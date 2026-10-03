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

import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Output;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests immutable lists with null elements, which are created by Stream.toList. */
class ImmutableListNullsTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@BeforeEach
	public void setUp () throws Exception {
		super.setUp();
		ImmutableCollectionsSerializers.registerSerializers(kryo);
		kryo.register(String.class);
	}

	@Test
	void testDifferentClassesWithNull () {
		assertImmutableWithNull(roundTrip(8, Stream.of(1, "a", null).toList()));
		assertImmutableWithNull(roundTrip(3, Stream.of(null, null).toList()));
		assertImmutableWithNull(roundTrip(3, Stream.of((Object)null).toList()));
	}

	@Test
	void testSameClassWithNull () {
		// The default format doesn't support null elements of the same class.
		assertThrows(IllegalArgumentException.class, () -> kryo.writeClassAndObject(new Output(64), Stream.of(null, 1, null).toList()));

		((CollectionSerializer)kryo.getSerializer(List.of().getClass())).setElementsCanBeNull(true);
		assertImmutableWithNull(roundTrip(8, Stream.of(null, 1, null).toList()));
		assertImmutableWithNull(roundTrip(8, Stream.of(1, "a", null).toList()));
		assertImmutableWithNull(roundTrip(7, Stream.of(1, null).toList()));
		roundTrip(7, List.of(1, 2, 3));
	}

	@Test
	void testCopy () {
		assertImmutableWithNull(kryo.copy(Stream.of(null, 1, null).toList()));
	}

	private void assertImmutableWithNull (List list) {
		assertEquals(Stream.of(1, null).toList().getClass(), list.getClass());
		assertTrue(list.contains(null));
		assertThrows(UnsupportedOperationException.class, () -> list.add(1));
	}
}
