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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.ObjectArraySerializer;

import org.junit.jupiter.api.Test;

/** @author Nathan Sweet */
class ArraySerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@Test
	void testArrays () {
		kryo.register(int[].class);
		kryo.register(int[][].class);
		kryo.register(int[][][].class);
		kryo.register(String[].class);
		kryo.register(Object[].class);
		roundTrip(4, new Object[] {null, null});
		roundTrip(6, new Object[] {null, "2"});
		roundTrip(6, new int[] {1, 2, 3, 4});
		roundTrip(7, new int[] {1, 2, -100, 4});
		roundTrip(9, new int[] {1, 2, -100, 40000});
		roundTrip(9, new int[][] {{1, 2}, {100, 4}});
		roundTrip(11, new int[][] {{1}, {2}, {100}, {4}});
		roundTrip(13, new int[][][] {{{1}, {2}}, {{100}, {4}}});
		roundTrip(12, new String[] {"11", "2222", "3", "4"});
		roundTrip(11, new String[] {"11", "2222", null, "4"});
		roundTrip(28,
			new Object[] {new String[] {"11", "2222", null, "4"}, new int[] {1, 2, 3, 4}, new int[][] {{1, 2}, {100, 4}}});

		ObjectArraySerializer serializer = new ObjectArraySerializer(kryo, String[].class);
		kryo.register(String[].class, serializer);
		serializer.setElementsAreSameType(true);
		roundTrip(11, new String[] {"11", "2222", null, "4"});
		serializer.setElementsAreSameType(false);
		roundTrip(11, new String[] {"11", "2222", null, "4"});
		roundTrip(5, new String[] {null, null, null});
		roundTrip(2, new String[] {});
		serializer.setElementsAreSameType(true);
		roundTrip(12, new String[] {"11", "2222", "3", "4"});
		serializer.setElementsCanBeNull(false);
		roundTrip(12, new String[] {"11", "2222", "3", "4"});

		serializer = new ObjectArraySerializer(kryo, Float[].class);
		kryo.register(Float[][].class, serializer);
		kryo.register(Float[].class, serializer);
		Float[][] array = new Float[4][];
		array[0] = new Float[] {0.0f, 1.0f};
		array[1] = null;
		array[2] = new Float[] {2.0f, 3.0f};
		array[3] = new Float[] {3.0f};
		roundTrip(31, array);
	}

	@Test
	void testRecursiveArray () {
		Object[] array = new Object[1];
		array[0] = array;
		kryo.register(Object[].class);
		Object[] copy = kryo.copy(array);
		assertTrue(copy == copy[0]);
	}

	@Test
	void testStringArray () {
		String moo = "moooooooooooooooooo";
		String[] array = {moo, "dog", moo};
		kryo.register(String[].class);
		roundTrip(43, array);

		kryo.setReferences(true);
		roundTrip(28, array);
	}

	// boolean[] and Object[] check the length against maxArraySize like the other arrays.
	@Test
	void testMaxArraySize () {
		kryo.register(boolean[].class);
		kryo.register(Object[].class);
		kryo.register(Empty.class);
		kryo.register(Empty[].class);
		for (Object array : new Object[] {new boolean[11], new Object[11], new Empty[11]}) {
			Output output = new Output(64);
			kryo.writeObject(output, array);
			Input input = new Input(output.toBytes());
			input.setMaxArraySize(10);
			KryoException ex = assertThrows(KryoException.class, () -> kryo.readObject(input, array.getClass()));
			assertTrue(ex.getMessage().contains("maxArraySize"), ex.getMessage());
		}

		// Elements can take 0 bytes, so the length of an Object[] isn't checked against the bytes remaining: without a null
		// marker, and with a serializer that accepts null and writes nothing.
		ObjectArraySerializer serializer = new ObjectArraySerializer(kryo, Empty[].class);
		serializer.setElementsCanBeNull(false);
		kryo.register(Empty[].class, serializer);
		Output output = new Output(64);
		kryo.writeObject(output, new Empty[] {new Empty(), new Empty(), new Empty()});
		assertEquals(1, output.position());
		assertEquals(3, kryo.readObject(new Input(output.toBytes()), Empty[].class).length);

		kryo.register(Empty.class, new EmptySerializer());
		kryo.register(Empty[].class, new ObjectArraySerializer(kryo, Empty[].class));
		output = new Output(64);
		kryo.writeObject(output, new Empty[] {new Empty(), new Empty(), new Empty()});
		assertEquals(1, output.position());
		assertEquals(3, kryo.readObject(new Input(output.toBytes()), Empty[].class).length);
	}

	public static final class Empty {
	}

	static class EmptySerializer extends Serializer<Empty> {
		{
			setAcceptsNull(true);
		}

		public void write (Kryo kryo, Output output, Empty object) {
		}

		public Empty read (Kryo kryo, Input input, Class<? extends Empty> type) {
			return new Empty();
		}
	}
}
