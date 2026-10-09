/* Copyright (c) 2008-2026, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryo.serializers;

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializerTest.KeyComparator;
import com.esotericsoftware.kryo.serializers.MapSerializerTest.KeyThatIsntComparable;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

/** @author Nathan Sweet */
class CollectionSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@Test
	void testSizeChanged () {
		// The size doesn't match the elements, eg because the collection was modified concurrently (#1181).
		kryo.register(WrongSizeList.class);
		for (int sizeDelta : new int[] {-1, 1}) {
			for (List<Object> elements : List.<List<Object>> of(List.of("a", "b"), Arrays.asList("a", null), List.of("a", 1),
				Arrays.asList(null, null))) {
				WrongSizeList list = new WrongSizeList(sizeDelta);
				list.addAll(elements);
				KryoException ex = assertThrows(KryoException.class, () -> kryo.writeObject(new Output(1024), list));
				assertTrue(ex.getMessage().contains("changed while it was written"), ex.getMessage());
			}
		}
		// With a known element serializer.
		CollectionSerializer serializer = new CollectionSerializer();
		serializer.setElementClass(String.class, new StringSerializer());
		serializer.setElementsCanBeNull(false);
		kryo.register(WrongSizeList.class, serializer);
		WrongSizeList list = new WrongSizeList(1);
		list.add("a");
		assertThrows(KryoException.class, () -> kryo.writeObject(new Output(1024), list));
	}

	public static class WrongSizeList extends ArrayList<Object> {
		final int sizeDelta;

		public WrongSizeList () {
			this(0);
		}

		public WrongSizeList (int sizeDelta) {
			this.sizeDelta = sizeDelta;
		}

		public int size () {
			return super.size() + sizeDelta;
		}
	}

	@Test
	void testWriteSameClassOnce () {
		kryo.register(ArrayList.class);
		ArrayList<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
		roundTrip(10, list); // The class of the elements is written once.

		CollectionSerializer serializer = new CollectionSerializer();
		assertTrue(serializer.getWriteSameClassOnce());
		serializer.setWriteSameClassOnce(false);
		assertFalse(serializer.getWriteSameClassOnce());
		kryo.register(ArrayList.class, serializer);
		Output output = new Output(64);
		kryo.writeClassAndObject(output, list);
		assertEquals(11, output.position()); // The class of each element is written.

		// The data can be read without the setting.
		Kryo reader = new Kryo();
		reader.register(ArrayList.class);
		assertEquals(list, reader.readClassAndObject(new Input(output.toBytes())));
	}

	// The class of an element can change while it is written, eg if the element is replaced (#943).
	@Test
	void testReplacedElements () {
		Kryo kryo = new Kryo() {
			public void writeClassAndObject (Output output, Object object) {
				super.writeClassAndObject(output, object instanceof StringBuilder ? object.toString() : object);
			}
		};
		CollectionSerializer serializer = new CollectionSerializer();
		serializer.setWriteSameClassOnce(false);
		kryo.register(ArrayList.class, serializer);
		kryo.register(StringBuilder.class);
		ArrayList list = new ArrayList(Arrays.asList(new StringBuilder("a"), new StringBuilder("b")));
		Output output = new Output(64);
		kryo.writeClassAndObject(output, list);
		assertEquals(Arrays.asList("a", "b"), kryo.readClassAndObject(new Input(output.toBytes())));
	}

	@Test
	void testMaliciousCollectionSize () {
		kryo.setReferences(false);
		kryo.register(ArrayList.class);
		Output declared = new Output(8);
		kryo.writeClass(declared, ArrayList.class);
		declared.writeVarIntFlag(false, 2000000001, true);
		declared.flush();
		assertThrows(KryoException.class, () -> kryo.readClassAndObject(new Input(declared.toBytes())));
	}

	@Test
	void testMaxCollectionSize () {
		kryo.setReferences(false);
		kryo.register(ArrayList.class);
		Output declared = new Output(8);
		kryo.writeClass(declared, ArrayList.class);
		declared.writeVarIntFlag(false, 2000000001, true);
		declared.flush();
		Input stream = new Input(new ByteArrayInputStream(declared.toBytes()));
		stream.setMaxArraySize(1024);
		assertThrows(KryoException.class, () -> kryo.readClassAndObject(stream));
	}

	@Test
	void testCollections () {
		kryo.register(ArrayList.class);
		kryo.register(LinkedList.class);
		kryo.register(CopyOnWriteArrayList.class);
		roundTrip(2, list());
		roundTrip(10, list("1", "2", "3"));
		roundTrip(9, list("1", "2", null));
		roundTrip(13, list("1", "2", null, 1, 2));
		roundTrip(15, list("1", "2", null, 1, 2, 5));

		roundTrip(16, list("11", "22", "33", "44", "55", "66"));
		roundTrip(19, list("11", "22", 33, "44", "55", "66"));
		roundTrip(15, list("11", "22", null, "44", "55", "66"));
		roundTrip(17, list("11", "22", 33, null, "55", "66"));
		roundTrip(3, list(null, null, null, null, null, null));

		roundTrip(10, list("1", "2", "3"));
		roundTrip(10, list("1", "2", "3"));
		roundTrip(14, list("1", "2", list("3")));
		roundTrip(14, new LinkedList(list("1", "2", list("3"))));
		roundTrip(14, new CopyOnWriteArrayList(list("1", "2", list("3"))));

		CollectionSerializer serializer = new CollectionSerializer();
		kryo.register(ArrayList.class, serializer);
		kryo.register(LinkedList.class, serializer);
		kryo.register(CopyOnWriteArrayList.class, serializer);
		serializer.setElementClass(Integer.class, kryo.getSerializer(Integer.class));
		roundTrip(5, list(1, 2, 3));
		roundTrip(7, list(1, 2, null));
		serializer.setElementClass(String.class, kryo.getSerializer(String.class));
		roundTrip(8, list("1", "2", "3"));
		serializer.setElementClass(String.class, new StringSerializer());
		roundTrip(8, list("1", "2", "3"));
		roundTrip(7, list("1", "2", null));
		serializer.setElementsCanBeNull(false);
		roundTrip(8, list("1", "2", "3"));

		kryo.register(TreeSet.class);
		TreeSet set = new TreeSet();
		set.add("1");
		set.add("2");
		roundTrip(9, set);

		kryo.register(KeyThatIsntComparable.class);
		kryo.register(KeyComparator.class);
		set = new TreeSet(new KeyComparator());
		set.add(new KeyThatIsntComparable("1"));
		set.add(new KeyThatIsntComparable("2"));
		roundTrip(9, set);

		kryo.register(TreeSetSubclass.class);
		set = new TreeSetSubclass();
		set.add(12);
		set.add(63);
		set.add(34);
		set.add(45);
		roundTrip(9, set);
	}

	@Test
	void testCopy () {
		List objects1 = Collections.singletonList(new Object());
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		List objects2 = kryo.copy(objects1);
		assertNotSame(objects1.get(0), objects2.get(0));
	}

	public static class TreeSetSubclass<E> extends TreeSet<E> {
		public TreeSetSubclass () {
		}

		public TreeSetSubclass (Comparator<? super E> comparator) {
			super(comparator);
		}
	}
}
