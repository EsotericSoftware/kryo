/* Copyright (c) 2008-2023, Nathan Sweet
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

import com.esotericsoftware.kryo.KryoTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UnmodifiableCollectionsSerializersTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@BeforeEach
	public void setUp () throws Exception {
		super.setUp();

		UnmodifiableCollectionSerializers.registerSerializers(kryo);

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
		roundTrip(3, Collections.unmodifiableMap(new HashMap<>()));
		roundTrip(4, Collections.unmodifiableMap(new TreeMap<>()));
		roundTrip(3, Collections.unmodifiableList(new ArrayList<>()));
		roundTrip(3, Collections.unmodifiableList(new LinkedList<>()));
		roundTrip(3, Collections.unmodifiableSet(new HashSet<>()));
		roundTrip(4, Collections.unmodifiableSet(new TreeSet<>()));
		roundTrip(6, Collections.unmodifiableCollection(Arrays.asList("")));
	}

	protected void doAssertEquals (Object object1, Object object2) {
		if (object1 instanceof Iterable<?> && object2 instanceof Iterable<?>) {
			Assertions.assertInstanceOf(object1.getClass(), object2);
			Assertions.assertInstanceOf(object2.getClass(), object1);
			Assertions.assertIterableEquals((Iterable<?>)object1, (Iterable<?>)object2);
		} else {
			Assertions.assertEquals(object1, object2);
		}
	}
}
