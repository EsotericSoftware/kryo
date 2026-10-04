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


package com.esotericsoftware.kryo;

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Enums with constant bodies are serialized like other enums, so bodies can be added or removed. */
class EnumsFinalTest {
	@Test
	void testIsFinal () {
		Kryo kryo = new Kryo();
		assertTrue(kryo.isFinal(BodyOp.class));
		assertTrue(kryo.isFinal(BodyOp[].class));
		kryo.setEnumsFinal(false);
		assertFalse(kryo.isFinal(BodyOp.class));
		assertTrue(kryo.isFinal(PlainOp.class));
	}

	@Test
	void testAddAndRemoveBodies () {
		for (boolean enumsFinal : new boolean[] {true, false}) {
			byte[] plain = write(kryo(enumsFinal, PlainOp.class, PlainHolder.class), new PlainHolder());
			byte[] body = write(kryo(enumsFinal, BodyOp.class, BodyHolder.class), new BodyHolder());
			if (enumsFinal) {
				assertArrayEquals(plain, body);
				BodyHolder read = kryo(true, BodyOp.class, BodyHolder.class).readObject(new Input(plain), BodyHolder.class);
				assertSame(BodyOp.MINUS, read.op);
				assertEquals(List.of(BodyOp.PLUS, BodyOp.MINUS), read.list);
				assertArrayEquals(new BodyOp[] {BodyOp.MINUS}, read.array);
				PlainHolder readPlain = kryo(true, PlainOp.class, PlainHolder.class).readObject(new Input(body), PlainHolder.class);
				assertSame(PlainOp.MINUS, readPlain.op);
			} else
				assertTrue(body.length > plain.length); // The class of each value is written, like in Kryo 5.
		}
	}

	@Test
	void testEnumName () {
		// The name of an unregistered enum constant with a body is the name of its enum.
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		Output output = new Output(256);
		kryo.writeClassAndObject(output, BodyOp.MINUS);
		String bytes = new String(output.toBytes(), StandardCharsets.ISO_8859_1);
		assertFalse(bytes.contains("BodyOp$"), bytes);
		assertSame(BodyOp.MINUS, kryo.readClassAndObject(new Input(output.toBytes())));
	}

	private Kryo kryo (boolean enumsFinal, Class op, Class holder) {
		Kryo kryo = new Kryo();
		kryo.setEnumsFinal(enumsFinal);
		kryo.register(op, 100);
		kryo.register(holder, 101);
		kryo.register(java.util.ArrayList.class, 102);
		kryo.register(op.arrayType(), 103);
		return kryo;
	}

	private byte[] write (Kryo kryo, Object object) {
		Output output = new Output(256);
		kryo.writeObject(output, object);
		return output.toBytes();
	}

	public enum PlainOp {
		PLUS, MINUS
	}

	public enum BodyOp {
		PLUS {
			public String toString () {
				return "+";
			}
		},
		MINUS {
			public String toString () {
				return "-";
			}
		}
	}

	public static class PlainHolder {
		public PlainOp op = PlainOp.MINUS;
		public List<PlainOp> list = new java.util.ArrayList<>(Arrays.asList(PlainOp.PLUS, PlainOp.MINUS));
		public PlainOp[] array = {PlainOp.MINUS};
	}

	public static class BodyHolder {
		public BodyOp op = BodyOp.MINUS;
		public List<BodyOp> list = new java.util.ArrayList<>(Arrays.asList(BodyOp.PLUS, BodyOp.MINUS));
		public BodyOp[] array = {BodyOp.MINUS};
	}
}
