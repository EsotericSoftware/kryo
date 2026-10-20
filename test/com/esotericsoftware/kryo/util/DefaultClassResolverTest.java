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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.junit.jupiter.api.Test;

class DefaultClassResolverTest {
	@Test
	void testInvalidClassId () {
		// -1 is not written for any registration ID. It was read as the memoized class after a registration cleared the memo.
		Output output = new Output(16, -1);
		output.writeVarInt(-1, true);
		byte[] invalid = output.toBytes();
		Kryo kryo = kryo();
		assertThrows(KryoException.class, () -> kryo.readClass(new Input(invalid)));

		kryo.register(A.class, 10);
		Output a = new Output(16, -1);
		kryo.writeClass(a, A.class);
		assertSame(A.class, kryo.readClass(new Input(a.toBytes())).getType());
		kryo.register(B.class, 11);
		assertThrows(KryoException.class, () -> kryo.readClass(new Input(invalid)));
		assertSame(A.class, kryo.readClass(new Input(a.toBytes())).getType());
	}

	@Test
	void testNullClassName () {
		Output output = new Output(16, -1);
		output.writeVarInt(1, true); // NAME + 2
		output.writeVarInt(0, true); // Name ID.
		output.writeString(null);
		Kryo kryo = kryo();
		KryoException ex = assertThrows(KryoException.class, () -> kryo.readClass(new Input(output.toBytes())));
		assertTrue(ex.getMessage().contains("null"), ex.getMessage());
	}

	/** Without auto reset, because writeClass and readClass reset the object graph at the top level. */
	private Kryo kryo () {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setAutoReset(false);
		return kryo;
	}

	static class A {
	}

	static class B {
	}
}
