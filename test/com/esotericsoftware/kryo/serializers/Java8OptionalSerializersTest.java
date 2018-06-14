/* Copyright (c) 2016, Martin Grotzke
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

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.Before;
import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestSupport;
import com.esotericsoftware.kryo.TestKryoFactory;

/** Test for java 8 Optional* serializers. Excluded from surefire tests via the "until-java8" profile in pom.xml which excludes
 * "Java8*Tests". */
public class Java8OptionalSerializersTest {

	private final Kryo kryo = new TestKryoFactory().create();
	private final KryoTestSupport support = new KryoTestSupport(kryo, true);

	@Before
	public void setUp () throws Exception {
		kryo.register(Optional.class);
		kryo.register(OptionalInt.class);
		kryo.register(OptionalLong.class);
		kryo.register(OptionalDouble.class);
		kryo.register(TestClass.class);
	}

	@Test
	public void testOptional () {
		support.roundTrip(2, 2, new TestClass(null));
		support.roundTrip(3, 3, new TestClass(Optional.<String> empty()));
		support.roundTrip(6, 6, new TestClass(Optional.of("foo")));
	}

	@Test
	public void testOptionalInt () {
		support.roundTrip(2, 2, OptionalInt.empty());
		support.roundTrip(6, 6, OptionalInt.of(Integer.MIN_VALUE));
		support.roundTrip(6, 6, OptionalInt.of(Integer.MAX_VALUE));
	}

	@Test
	public void testOptionalLong () {
		support.roundTrip(2, 2, OptionalLong.empty());
		support.roundTrip(10, 10, OptionalLong.of(Long.MIN_VALUE));
		support.roundTrip(10, 10, OptionalLong.of(Long.MAX_VALUE));
	}

	@Test
	public void testOptionalDouble () {
		support.roundTrip(2, 2, OptionalDouble.empty());
		support.roundTrip(10, 10, OptionalDouble.of(Double.MIN_VALUE));
		support.roundTrip(10, 10, OptionalDouble.of(Double.MAX_VALUE));
	}

	static class TestClass {
		Optional<String> maybe;

		public TestClass () {
		}

		public TestClass (Optional<String> maybe) {
			this.maybe = maybe;
		}

		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TestClass testClass = (TestClass)o;
			return Objects.equals(maybe, testClass.maybe);

		}

		@Override
		public int hashCode () {
			return Objects.hashCode(maybe);
		}
	}

}
