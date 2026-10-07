/* Copyright (c) 2008, Nathan Sweet
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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.esotericsoftware.kryo.io.UnsafeMemoryInput;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;

public class DeclaredSizeBoundsTest extends KryoTestCase {
	public void testBufferBackedPrimitiveAndStringBounds () {
		for (Input input : new Input[] {new Input(new byte[8]), new ByteBufferInput(new byte[8]),
			new UnsafeInput(new byte[8]), new UnsafeMemoryInput(new byte[8])}) {
			try {
				input.readInts(8);
				fail("Eight ints cannot fit in eight bytes.");
			} catch (KryoException expected) {
			}
			try {
				input.readBytes(Integer.MAX_VALUE);
				fail("Huge byte array was accepted.");
			} catch (KryoException expected) {
			}
		}

		// UTF-8 length prefix declaring 2^31-1 characters with no character data.
		byte[] malformed = {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x07};
		for (Input input : new Input[] {new Input(malformed), new ByteBufferInput(malformed)}) {
			try {
				input.readString();
				fail("Huge string was accepted.");
			} catch (KryoException expected) {
			}
		}
	}

	public void testStreamLimitAndOverflow () {
		Input input = new Input(new ByteArrayInputStream(new byte[16]));
		input.setMaxArraySize(10);
		assertEquals(10, input.getMaxArraySize());
		try {
			input.readInts(11);
			fail("Declared size exceeded the configured limit.");
		} catch (KryoException expected) {
		}
		Input unsafe = new UnsafeInput(new ByteArrayInputStream(new byte[16]));
		try {
			unsafe.readLongs(300000000);
			fail("Byte count overflow was accepted.");
		} catch (KryoException expected) {
		}
		try {
			input.setMaxArraySize(-1);
			fail("Negative limit was accepted.");
		} catch (IllegalArgumentException expected) {
		}
	}

	public void testSerializerDeclaredSizes () {
		Output output = new Output(16);
		output.writeVarInt(2000000001, true);
		byte[] malformed = output.toBytes();
		assertRejected(malformed, int[].class);
		assertRejected(malformed, boolean[].class);
		assertRejected(malformed, String[].class);
		assertRejected(malformed, ArrayList.class);
		assertRejected(malformed, HashMap.class);
		assertRejected(malformed, CompatibleData.class);
		Input objectArrayInput = new Input(malformed);
		objectArrayInput.setMaxArraySize(10);
		kryo.register(Object[].class);
		try {
			kryo.readObject(objectArrayInput, Object[].class);
			fail("Object array exceeded configured limit.");
		} catch (KryoException expected) {
		}
	}

	private void assertRejected (byte[] malformed, Class type) {
		if (type == CompatibleData.class)
			kryo.register(type, new CompatibleFieldSerializer(kryo, type));
		else
			kryo.register(type);
		try {
			kryo.readObject(new Input(malformed), type);
			fail("Malformed declared size was accepted: " + type);
		} catch (KryoException expected) {
		}
	}

	public void testValidArraysAndStrings () {
		Output output = new Output(64);
		output.writeInts(new int[] {1, 2, 3});
		output.writeString("hello \u00e9");
		Input input = new Input(output.toBytes());
		assertTrue(java.util.Arrays.equals(new int[] {1, 2, 3}, input.readInts(3)));
		assertEquals("hello \u00e9", input.readString());
	}

	public static class CompatibleData {
		public int value;
	}
}
