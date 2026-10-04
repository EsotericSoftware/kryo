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

package com.esotericsoftware.kryo.util;

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.ClassResolver;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class DefaultClassResolverTest {
	@Test
	void testUnknownClassName () {
		// A later reference to the name ID of an unknown class must not be read as a class name.
		Output output = new Output(64, -1);
		output.writeVarInt(1, true); // NAME + 2
		output.writeVarInt(0, true); // Name ID.
		output.writeString("does.not.Exist");
		output.writeVarInt(1, true);
		output.writeVarInt(0, true); // Reference to the name ID.
		Kryo kryo = kryo();
		Input input = new Input(output.toBytes());
		assertTrue(assertThrows(KryoException.class, () -> kryo.readClass(input)).getMessage().contains("does.not.Exist"));
		assertTrue(assertThrows(KryoException.class, () -> kryo.readClass(input)).getMessage().contains("does.not.Exist"));
		assertEquals(output.position(), input.position());
	}

	@Test
	void testDeferredNames () {
		Kryo kryo = kryo();
		ClassResolver resolver = kryo.getClassResolver();
		Output data = new Output(256, -1), names = new Output(256, -1);
		int mark = resolver.beginDeferredNames();
		kryo.writeClass(data, A.class);
		kryo.writeClass(data, B.class);
		kryo.writeClass(data, A.class);
		resolver.endDeferredNames(names, mark);
		assertFalse(contains(data, "$A"), "Only name IDs are written while deferring.");

		Kryo reader = kryo();
		reader.getClassResolver().readDeferredNames(new Input(names.toBytes()));
		Input input = new Input(data.toBytes());
		assertSame(A.class, reader.readClass(input).getType());
		assertSame(B.class, reader.readClass(input).getType());
		assertSame(A.class, reader.readClass(input).getType());

		// After deferring ended, names are written as usual.
		kryo.writeClass(data, C.class);
		assertTrue(contains(data, "$C"));
	}

	@Test
	void testNestedDeferredNames () {
		Kryo kryo = kryo();
		ClassResolver resolver = kryo.getClassResolver();
		Output data = new Output(256, -1), innerNames = new Output(256, -1), outerNames = new Output(256, -1);
		int outer = resolver.beginDeferredNames();
		kryo.writeClass(data, A.class);
		int inner = resolver.beginDeferredNames();
		kryo.writeClass(data, B.class);
		resolver.endDeferredNames(innerNames, inner);
		kryo.writeClass(data, C.class); // Still deferred by the outer level.
		resolver.endDeferredNames(outerNames, outer);
		assertFalse(contains(data, "$C"));
		assertEquals(1, new Input(innerNames.toBytes()).readVarInt(true));
		assertEquals(3, new Input(outerNames.toBytes()).readVarInt(true)); // Including the inner names.

		Kryo reader = kryo();
		reader.getClassResolver().readDeferredNames(new Input(innerNames.toBytes()));
		reader.getClassResolver().readDeferredNames(new Input(outerNames.toBytes())); // Known name IDs are skipped.
		Input input = new Input(data.toBytes());
		assertSame(A.class, reader.readClass(input).getType());
		assertSame(B.class, reader.readClass(input).getType());
		assertSame(C.class, reader.readClass(input).getType());
	}

	@Test
	void testDeferredUnknownClassName () {
		Output names = new Output(64, -1);
		names.writeVarInt(1, true);
		names.writeVarInt(0, true);
		names.writeString("does.not.Exist");
		Kryo reader = kryo();
		reader.getClassResolver().readDeferredNames(new Input(names.toBytes())); // Doesn't throw.

		Output data = new Output(16, -1);
		data.writeVarInt(1, true);
		data.writeVarInt(0, true);
		KryoException ex = assertThrows(KryoException.class, () -> reader.readClass(new Input(data.toBytes())));
		assertTrue(ex.getMessage().contains("does.not.Exist"));
	}

	@Test
	void testResetEndsDeferredNames () {
		// Eg after an exception while deferring.
		Kryo kryo = kryo();
		kryo.getClassResolver().beginDeferredNames();
		kryo.reset();
		Output data = new Output(256, -1);
		kryo.writeClass(data, A.class);
		assertTrue(contains(data, "$A"));
	}

	/** Without auto reset, because writeClass and readClass reset the object graph at the top level. */
	private Kryo kryo () {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setAutoReset(false);
		return kryo;
	}

	/** Returns true if the output contains the ASCII text, ignoring the high bit that marks the end of an ASCII string. */
	private boolean contains (Output output, String text) {
		byte[] bytes = output.toBytes();
		for (int i = 0; i < bytes.length; i++)
			bytes[i] &= 0x7f;
		return new String(bytes, StandardCharsets.US_ASCII).contains(text);
	}

	static class A {
	}

	static class B {
	}

	static class C {
	}
}
