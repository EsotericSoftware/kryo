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

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test for java 8 Optional* serializers. */
class OptionalSerializersTest extends KryoTestCase {

	{
		supportsCopy = true;
	}

	@BeforeEach
	public void setUp () throws Exception {
		super.setUp();
		kryo.register(Optional.class);
		kryo.register(OptionalInt.class);
		kryo.register(OptionalLong.class);
		kryo.register(OptionalDouble.class);
		kryo.register(TestClass.class);
	}

	@Test
	void testOptional () {
		roundTrip(2, new TestClass(null));
		roundTrip(3, new TestClass(Optional.<String> empty()));
		roundTrip(6, new TestClass(Optional.of("foo")));
	}

	@Test
	void testOptionalInt () {
		roundTrip(2, OptionalInt.empty());
		roundTrip(6, OptionalInt.of(Integer.MIN_VALUE));
		roundTrip(6, OptionalInt.of(Integer.MAX_VALUE));
	}

	@Test
	void testOptionalLong () {
		roundTrip(2, OptionalLong.empty());
		roundTrip(10, OptionalLong.of(Long.MIN_VALUE));
		roundTrip(10, OptionalLong.of(Long.MAX_VALUE));
	}

	@Test
	void testOptionalDouble () {
		roundTrip(2, OptionalDouble.empty());
		roundTrip(10, OptionalDouble.of(Double.MIN_VALUE));
		roundTrip(10, OptionalDouble.of(Double.MAX_VALUE));
	}

	public static class TestClass {
		Optional<String> maybe;

		public TestClass () {
		}

		public TestClass (Optional<String> maybe) {
			this.maybe = maybe;
		}

		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TestClass testClass = (TestClass)o;
			return Objects.equals(maybe, testClass.maybe);

		}

		public int hashCode () {
			return Objects.hashCode(maybe);
		}
	}

}
