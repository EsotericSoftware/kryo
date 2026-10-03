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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SynchronizedCollectionSerializersTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@BeforeEach
	public void setUp () throws Exception {
		super.setUp();

		SynchronizedCollectionSerializers.register(kryo);

		kryo.register(ArrayList.class);
		kryo.register(LinkedList.class);
		kryo.register(HashMap.class);
		kryo.register(TreeMap.class);
		kryo.register(HashSet.class);
		kryo.register(TreeSet.class);
		kryo.register(Arrays.asList("").getClass());
	}

	@Test
	void testSerializer () {
		roundTrip(3, Collections.synchronizedMap(new HashMap<>()));
		roundTrip(4, Collections.synchronizedMap(new TreeMap<>()));
		roundTrip(3, Collections.synchronizedList(new ArrayList<>()));
		roundTrip(3, Collections.synchronizedList(new LinkedList<>()));
		roundTrip(3, Collections.synchronizedSet(new HashSet<>()));
		roundTrip(4, Collections.synchronizedSet(new TreeSet<>()));
		roundTrip(6, Collections.synchronizedCollection(Arrays.asList("")));
	}

	@Test
	void testNavigable () {
		roundTrip(4, Collections.synchronizedNavigableSet(new TreeSet<>()));
		roundTrip(4, Collections.synchronizedNavigableMap(new TreeMap<>()));
	}

	@Test
	void testRegistrationOrder () {
		Kryo kryo = new Kryo();
		int firstId = kryo.getNextRegistrationId();
		SynchronizedCollectionSerializers.register(kryo);
		List<Class> types = Arrays.asList(Collections.synchronizedCollection(Arrays.asList("")).getClass(),
			Collections.synchronizedList(new ArrayList<>()).getClass(), Collections.synchronizedList(new LinkedList<>()).getClass(),
			Collections.synchronizedSet(new HashSet<>()).getClass(), Collections.synchronizedSortedSet(new TreeSet<>()).getClass(),
			Collections.synchronizedMap(new HashMap<>()).getClass(), Collections.synchronizedSortedMap(new TreeMap<>()).getClass(),
			Collections.synchronizedNavigableSet(new TreeSet<>()).getClass(),
			Collections.synchronizedNavigableMap(new TreeMap<>()).getClass());
		for (int i = 0; i < types.size(); i++)
			Assertions.assertEquals(firstId + i, kryo.getRegistration(types.get(i)).getId());
	}

	@Test
	void testDefaultSerializers () {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		SynchronizedCollectionSerializers.addDefaultSerializers(kryo);
		TreeMap<String, String> map = new TreeMap<>();
		map.put("a", "b");
		assertDefaultRoundTrip(kryo, Collections.synchronizedList(new ArrayList<>(Arrays.asList("a"))));
		assertDefaultRoundTrip(kryo, Collections.synchronizedSortedSet(new TreeSet<>(Arrays.asList("a"))));
		assertDefaultRoundTrip(kryo, Collections.synchronizedNavigableSet(new TreeSet<>(Arrays.asList("a"))));
		assertDefaultRoundTrip(kryo, Collections.synchronizedSortedMap(map));
		assertDefaultRoundTrip(kryo, Collections.synchronizedNavigableMap(map));
	}

	private void assertDefaultRoundTrip (Kryo kryo, Object object) {
		Output output = new Output(1024, -1);
		kryo.writeClassAndObject(output, object);
		Object result = kryo.readClassAndObject(new Input(output.toBytes()));
		Assertions.assertEquals(object.getClass(), result.getClass());
		Assertions.assertEquals(object.toString(), result.toString());
	}

	protected void doAssertEquals (Object object1, Object object2) {
		if (object1 instanceof Iterable<?> && object2 instanceof Iterable<?>) {
			Assertions.assertEquals(object1.getClass(), object2.getClass());
			Assertions.assertIterableEquals((Iterable<?>)object1, (Iterable<?>)object2);
		} else {
			Assertions.assertEquals(object1, object2);
		}
	}
}
