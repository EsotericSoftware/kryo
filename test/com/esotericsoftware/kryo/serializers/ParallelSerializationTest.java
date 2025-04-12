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
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.util.Pool;

import java.util.Objects;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;

class ParallelSerializationTest {

	private final Pool<Kryo> pool = new Pool<Kryo>(true, false, 8) {
		@Override
		protected Kryo create () {
			Kryo kryo = new Kryo();
			kryo.setRegistrationRequired(false);
			kryo.setReferences(true);
			kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
			return kryo;
		}
	};

	@Test
	void serializeAndDeserialize () {
		IntStream.range(0, 100)
			.parallel()
			.forEach(it -> {
				for (int i = 0; i < 10; i++) {
					roundTrip(new TestClass(new GenericClass<>("test-" + it + "-" + i)));
				}
			});
	}

	private void roundTrip (Object target) {
		byte[] serialized = serialize(target);
		Object result = deserialize(serialized);
		assertEquals(result, target);
	}

	private byte[] serialize (Object sample) {
		Kryo kryo = pool.obtain();
		Output out = new Output(4096, -1);
		kryo.writeClassAndObject(out, sample);
		byte[] serialized = out.toBytes();
		out.close();
		pool.free(kryo);
		return serialized;
	}

	private Object deserialize (byte[] serialized) {
		Kryo kryo = pool.obtain();
		Input in = new Input(serialized);
		Object result = kryo.readClassAndObject(in);
		in.close();
		pool.free(kryo);
		return result;
	}

	static class TestClass {

		private final GenericSuperClass<String> value;

		public TestClass(GenericSuperClass<String> value) {
			this.value = value;
		}

		public GenericSuperClass<String> value () {
			return value;
		}

		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TestClass that = (TestClass)o;
			return Objects.equals(value, that.value);
		}

		@Override
		public int hashCode () {
			return Objects.hash(value);
		}
	}

	static abstract class GenericSuperClass<T> {
	}

	static class GenericClass<T> extends GenericSuperClass<T> {
		private final T value;

		public GenericClass(T value) {
			this.value = value;
		}

		public T value () {
			return value;
		}

		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			GenericClass<?> genericClass = (GenericClass<?>)o;
			return Objects.equals(value, genericClass.value);
		}

		@Override
		public int hashCode () {
			return value.hashCode();
		}
	}
	
}
