/* Copyright (c) 2008-2025, Nathan Sweet
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
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializerTest.KeyComparator;
import com.esotericsoftware.kryo.serializers.MapSerializerTest.KeyThatIsntComparable;

import java.util.ArrayList;
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
