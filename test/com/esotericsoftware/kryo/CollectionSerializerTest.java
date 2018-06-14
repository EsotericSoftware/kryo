/* Copyright (c) 2008, Nathan Sweet
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

package com.esotericsoftware.kryo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.Test;

import com.esotericsoftware.kryo.MapSerializerTest.KeyComparator;
import com.esotericsoftware.kryo.MapSerializerTest.KeyThatIsntComparable;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;

import static com.esotericsoftware.kryo.KryoTestUtil.list;

/** @author Nathan Sweet <misc@n4te.com> */
public class CollectionSerializerTest {

	private final Kryo kryo = new TestKryoFactory().create();
	private final KryoTestSupport support = new KryoTestSupport(kryo, true);

	@Test
	public void testCollections () {
		kryo.register(ArrayList.class);
		kryo.register(LinkedList.class);
		kryo.register(CopyOnWriteArrayList.class);
		support.roundTrip(11, 11, list("1", "2", "3"));
		support.roundTrip(13, 19, list("1", "2", null, 1, 2));
		support.roundTrip(15, 24, list("1", "2", null, 1, 2, 5));
		support.roundTrip(11, 11, list("1", "2", "3"));
		support.roundTrip(11, 11, list("1", "2", "3"));
		support.roundTrip(13, 13, list("1", "2", list("3")));
		support.roundTrip(13, 13, new LinkedList<>(list("1", "2", list("3"))));
		support.roundTrip(13, 13, new CopyOnWriteArrayList<>(list("1", "2", list("3"))));

		CollectionSerializer serializer = new CollectionSerializer();
		kryo.register(ArrayList.class, serializer);
		kryo.register(LinkedList.class, serializer);
		kryo.register(CopyOnWriteArrayList.class, serializer);
		serializer.setElementClass(String.class, kryo.getSerializer(String.class));
		support.roundTrip(8, 8, list("1", "2", "3"));
		serializer.setElementClass(String.class, new StringSerializer());
		support.roundTrip(8, 8, list("1", "2", "3"));
		serializer.setElementsCanBeNull(false);
		support.roundTrip(8, 8, list("1", "2", "3"));

		kryo.register(TreeSet.class);
		TreeSet set = new TreeSet();
		set.add("1");
		set.add("2");
		support.roundTrip(9, 9, set);

		kryo.register(KeyThatIsntComparable.class);
		kryo.register(KeyComparator.class);
		set = new TreeSet(new KeyComparator());
		set.add(new KeyThatIsntComparable("1"));
		set.add(new KeyThatIsntComparable("2"));
		support.roundTrip(9, 9, set);

		kryo.register(TreeSetSubclass.class);
		set = new TreeSetSubclass<Integer>();
		set.add(12);
		set.add(63);
		set.add(34);
		set.add(45);
		support.roundTrip(11, 23, set);
	}

	static public class TreeSetSubclass<E> extends TreeSet<E> {
		public TreeSetSubclass () {
		}

		public TreeSetSubclass (Comparator<? super E> comparator) {
			super(comparator);
		}
	}
}
