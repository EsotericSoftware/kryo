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

import com.esotericsoftware.kryo.KryoTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test for java >=9 ImmutableCollections serializers. */
class ImmutableCollectionsSerializersTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@BeforeEach
	public void setUp () throws Exception {
		super.setUp();
		
		ImmutableCollectionsSerializers.registerSerializers(kryo);
		kryo.register(HashMap.class);
		kryo.register(TestClass.class);
	}

	@Test
	void testImmutableCollections () {
		roundTrip(4, new TestClass(null, null, null));
		roundTrip(7, new TestClass(List.of(), Map.of(), Set.of()));
		roundTrip(11, new TestClass(List.of(1), Map.of(1, 2), Set.of(1)));
		roundTrip(17, new TestClass(List.of(1, 2, 3), Map.of(1, 2, 3, 4), Set.of(1, 2, 3)));
	}

	@Test
	void testImmutableList () {
		roundTrip(2, List.of());
		roundTrip(4, List.of(1));
		roundTrip(5, List.of(1, 2));
		roundTrip(6, List.of(1, 2, 3));
		roundTrip(4, List.of(1, 2, 3).subList(0, 1));
	}

	@Test
	void setImmutableMap () {
		roundTrip(2, Map.of());
		roundTrip(6, Map.of(1, 2));
		roundTrip(10, Map.of(1, 2, 3, 4));
	}

	@Test
	void testImmutableSet () {
		roundTrip(2, Set.of());
		roundTrip(4, Set.of(1));
		roundTrip(5, Set.of(1, 2));
		roundTrip(6, Set.of(1, 2, 3));
	}

	public static class TestClass {
		List<Integer> list;
		Map<Integer, Integer> map;
		Set<Integer> set;

		public TestClass () {
		}

		public TestClass (List<Integer> list, Map<Integer, Integer> map, Set<Integer> set) {
			this.list = list;
			this.map = map;
			this.set = set;
		}

		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final TestClass testClass = (TestClass)o;
			return Objects.equals(list, testClass.list) && Objects.equals(map, testClass.map)
				&& Objects.equals(set, testClass.set);
		}

		@Override
		public int hashCode () {
			return Objects.hash(list, map, set);
		}
	}

}
