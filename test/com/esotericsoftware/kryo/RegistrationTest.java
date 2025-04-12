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

package com.esotericsoftware.kryo;

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.LongSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class RegistrationTest {
	@Test
	void testDefaultSerializerOrder () {
		Kryo kryo = new Kryo();
		kryo.addDefaultSerializer(Fruit.class, new FieldSerializer(kryo, Fruit.class));
		FieldSerializer appleSerializer = new FieldSerializer(kryo, Apple.class);
		kryo.addDefaultSerializer(Apple.class, appleSerializer);
		assertSame(appleSerializer, kryo.getDefaultSerializer(Apple.class));
	}

	@Test
	void testReplaceRegistration () throws IOException {
		Kryo kryo = new Kryo();
		kryo.register(double[].class, 7); // Replace long with double[].
		kryo.register(Some.class);
		kryo.register(long.class, new LongSerializer());

		{
			Some<Integer> s = new Some(2);
			Output output = new Output(1024);
			kryo.writeObject(output, s);
			output.flush();

			Input input = new Input(output.getBuffer());
			Some r = kryo.readObject(input, Some.class); // OK!
			assertEquals(r.value, 2);
		}

		{
			Some<Long> s = new Some(2L);
			Output output = new Output(1024);
			kryo.writeObject(output, s);
			output.flush();

			Input input = new Input(output.getBuffer());
			Some r = kryo.readObject(input, Some.class);
			assertEquals(r.value, 2L);
		}
	}

	public static class Some<T> {
		public T value;

		public Some () {
		}

		public Some (T t) {
			this.value = t;
		}
	}

	public static class Fruit {
	}

	public static class Apple extends Fruit {
	}
}
