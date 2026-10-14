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
import static org.junit.jupiter.api.Assumptions.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Checks that the mode the tests run in with {@code -Dkryo.codeGeneration} or {@code -Dkryo.fieldAccess}, as in CI, is the one
 * actually used, so a silent fallback can't let a run pass without testing what it claims. Skipped if the property isn't set. */
class TestModeTest {
	@Test
	void testCodeGeneration () {
		assumeTrue("true".equals(System.getProperty("kryo.codeGeneration")), "Not run with -Dkryo.codeGeneration=true.");
		assertTrue(CachedFields.codeGeneration, CachedFields.codeGenerationUnavailable());
		FieldSerializer serializer = roundTrip();
		// generated() falls back to the cached fields if generating the code fails, and logs that only at DEBUG.
		assertNotNull(serializer.generated, "No code was generated.");
		assertNotNull(serializer.generatedCopy, "No code was generated for copying.");
		if ("asm".equals(System.getProperty("kryo.codeGeneration.backend"))) {
			assertFalse(Bytecode.classFileApi, "The Class-File API is used instead of ASM.");
			assertTrue(Bytecode.asm, "ASM is not on the classpath.");
		} else
			assertEquals(Runtime.version().feature() >= 24, Bytecode.classFileApi, "The Class-File API is used on Java 24+.");
	}

	@Test
	void testFieldAccess () {
		String configured = System.getProperty("kryo.fieldAccess");
		assumeTrue(configured != null, "Not run with -Dkryo.fieldAccess.");
		FieldSerializer serializer = roundTrip();
		// Unsafe falls back to VarHandles if it isn't available, VarHandles to reflection if a field can't be accessed.
		String name = switch (configured) {
		case "UNSAFE" -> "Unsafe";
		case "VARHANDLE" -> "VarHandle";
		case "REFLECTION" -> "Reflect";
		default -> throw new IllegalArgumentException(configured);
		};
		assertEquals(name + "Field", fieldClassName(serializer.getField("object")));
		assertEquals("Int" + name + "Field", fieldClassName(serializer.getField("value")));
		assertEquals("String" + name + "Field", fieldClassName(serializer.getField("text")));
	}

	/** Serializes and copies an object with the default serializer, a FieldSerializer, and returns the serializer. */
	static private FieldSerializer roundTrip () {
		Kryo kryo = new Kryo();
		kryo.register(Simple.class);
		kryo.register(ArrayList.class);
		Simple object = new Simple();
		object.value = 1;
		object.text = "a";
		object.object = new ArrayList(List.of(2));
		Output output = new Output(64, -1);
		kryo.writeObject(output, object);
		Simple read = kryo.readObject(new Input(output.toBytes()), Simple.class);
		assertEquals(object, read);
		assertEquals(object, kryo.copy(object));
		return (FieldSerializer)kryo.getSerializer(Simple.class);
	}

	static private String fieldClassName (CachedField field) {
		return field.getClass().getSimpleName();
	}

	public static class Simple {
		public int value;
		public String text;
		public Object object;

		public boolean equals (Object o) {
			return o instanceof Simple other && value == other.value && text.equals(other.text) && object.equals(other.object);
		}

		public int hashCode () {
			return value;
		}
	}
}
