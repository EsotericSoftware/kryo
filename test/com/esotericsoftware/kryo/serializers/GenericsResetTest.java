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
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Verifies that an exception thrown while (de)serializing a generic field does not leak the generics stack and poison a reused
 * Kryo instance.
 * <p>
 * See <a href="https://github.com/EsotericSoftware/kryo/issues/1281">#1281</a>: when a serializer threw between
 * {@link com.esotericsoftware.kryo.util.Generics#pushGenericType pushGenericType} and the balancing
 * {@link com.esotericsoftware.kryo.util.Generics#popGenericType popGenericType}, the leaked entry survived {@link Kryo#reset()}
 * and caused a later, unrelated deserialization to fail with an {@link ArrayIndexOutOfBoundsException}. */
class GenericsResetTest extends KryoTestCase {

	@Test
	void testGenericsStackIsResetAfterDeserializationException () {
		kryo.setReferences(false);
		kryo.setRegistrationRequired(false);
		kryo.register(Holder.class);
		kryo.register(Item.class);
		kryo.register(Payload.class, new ThrowOnReadSerializer());

		// A Holder whose List<Item> element holds a Payload. Reading the Payload throws, after the generic type for the
		// List<Item> field was pushed, leaking it onto the generics stack.
		Holder holder = new Holder();
		holder.items = new ArrayList();
		Item item = new Item();
		item.payload = new Payload();
		holder.items.add(item);

		Output output = new Output(512);
		kryo.writeClassAndObject(output, holder);
		byte[] failingBytes = output.toBytes();

		// An unrelated, well-formed HashMap containing two non-empty nested maps. On a poisoned instance, reading the second
		// nested map mistakes the leaked List generic for a Map generic and indexes [1], throwing AIOOBE.
		byte[] validBytes = writeNestedMaps();

		// The first deserialization fails inside the generic field.
		assertThrows(KryoException.class, () -> kryo.readClassAndObject(new Input(failingBytes)));

		// Kryo.reset() must have discarded the leaked generic type.
		assertEquals(0, kryo.getGenerics().getGenericTypesSize());

		// A subsequent unrelated deserialization on the same instance must succeed.
		Object result = kryo.readClassAndObject(new Input(validBytes));
		assertTrue(result instanceof Map);
		assertEquals(2, ((Map)result).size());
	}

	@Test
	void testGenericsStackIsResetAfterSerializationException () {
		kryo.setReferences(false);
		kryo.setRegistrationRequired(false);
		kryo.register(Holder.class);
		kryo.register(Item.class);
		kryo.register(Payload.class, new ThrowOnWriteSerializer());

		Holder holder = new Holder();
		holder.items = new ArrayList();
		Item item = new Item();
		item.payload = new Payload();
		holder.items.add(item);

		// Writing the Payload throws, after the generic type for the List<Item> field was pushed.
		assertThrows(KryoException.class, () -> kryo.writeClassAndObject(new Output(512), holder));

		// Kryo.reset() must have discarded the leaked generic type.
		assertEquals(0, kryo.getGenerics().getGenericTypesSize());

		// A subsequent unrelated serialization/deserialization on the same instance must succeed.
		byte[] validBytes = writeNestedMaps();
		Object result = kryo.readClassAndObject(new Input(validBytes));
		assertTrue(result instanceof Map);
		assertEquals(2, ((Map)result).size());
	}

	/** A bare HashMap with two non-empty nested maps, mirroring the payload that triggers the AIOOBE on a poisoned instance. */
	private byte[] writeNestedMaps () {
		Map<String, Object> map = new HashMap();
		Map<String, String> nested1 = new HashMap();
		nested1.put("a", "1");
		Map<String, String> nested2 = new HashMap();
		nested2.put("b", "2");
		map.put("k1", nested1);
		map.put("k2", nested2);

		Output output = new Output(512);
		kryo.writeClassAndObject(output, map);
		return output.toBytes();
	}

	public static class Holder {
		public List<Item> items;
	}

	public static class Item {
		public Object payload;
	}

	public static class Payload {
	}

	/** Writes nothing and always throws on read, simulating any read-time failure (eg a missing class). */
	static class ThrowOnReadSerializer extends Serializer<Payload> {
		public void write (Kryo kryo, Output output, Payload object) {
		}

		public Payload read (Kryo kryo, Input input, Class<? extends Payload> type) {
			throw new RuntimeException("simulated read failure");
		}
	}

	/** Always throws on write, simulating any write-time failure. */
	static class ThrowOnWriteSerializer extends Serializer<Payload> {
		public void write (Kryo kryo, Output output, Payload object) {
			throw new RuntimeException("simulated write failure");
		}

		public Payload read (Kryo kryo, Input input, Class<? extends Payload> type) {
			return new Payload();
		}
	}
}
