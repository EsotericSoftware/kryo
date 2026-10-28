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
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer.Since;

import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

/** {@link FieldSerializer#initializeCachedFields()} can remove fields, as its javadoc says, with all field serializers. */
class InitializeCachedFieldsTest {
	@Test
	void testRemoveFieldInInitializeCachedFields () {
		// A subclass that removes a field in initializeCachedFields, and the same serializer with the field removed afterwards.
		test("FieldSerializer", (kryo, remove) -> new FieldSerializer<Data>(kryo, Data.class) {
			protected void initializeCachedFields () {
				super.initializeCachedFields();
				if (remove) removeField("b");
			}
		});
		test("CompatibleFieldSerializer", (kryo, remove) -> new CompatibleFieldSerializer<Data>(kryo, Data.class) {
			protected void initializeCachedFields () {
				super.initializeCachedFields();
				if (remove) removeField("b");
			}
		});
		test("TaggedFieldSerializer", (kryo, remove) -> new TaggedFieldSerializer<Data>(kryo, Data.class) {
			protected void initializeCachedFields () {
				super.initializeCachedFields();
				if (remove) removeField("b");
			}
		});
		test("VersionFieldSerializer", (kryo, remove) -> new VersionFieldSerializer<Data>(kryo, Data.class) {
			protected void initializeCachedFields () {
				super.initializeCachedFields();
				if (remove) removeField("b");
			}
		});
	}

	private void test (String name, BiFunction<Kryo, Boolean, FieldSerializer<Data>> factory) {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		FieldSerializer<Data> serializer = factory.apply(kryo, true);
		assertEquals("a,c", names(serializer), name);

		FieldSerializer<Data> expected = factory.apply(kryo, false);
		expected.removeField("b");
		assertArrayEquals(write(kryo, expected), write(kryo, serializer), name);
		assertRead(kryo, serializer, 5, 2, "c5", name);

		// Removing another field doesn't call initializeCachedFields again, which would not find the field it removes.
		serializer.removeField("c");
		assertEquals("a", names(serializer), name);
		expected.removeField("c");
		assertArrayEquals(write(kryo, expected), write(kryo, serializer), name);
		assertRead(kryo, serializer, 5, 2, "c", name);

		// The fields are found again and initializeCachedFields removes the field again. Fields removed by the caller stay removed.
		serializer.updateFields();
		assertEquals("a", names(serializer), name);
		assertArrayEquals(write(kryo, expected), write(kryo, serializer), name);
		assertRead(kryo, serializer, 5, 2, "c", name);
	}

	@Test
	void testRemoveDuplicateFieldName () {
		// CompatibleFieldSerializer checks for duplicate field names after initializeCachedFields removed fields.
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		CompatibleFieldSerializer<Sub> serializer = new CompatibleFieldSerializer<Sub>(kryo, Sub.class) {
			protected void initializeCachedFields () {
				super.initializeCachedFields();
				removeField("value"); // One of the two fields with this name.
			}
		};
		assertEquals(1, serializer.getFields().length);
		Output output = new Output(64, -1);
		kryo.writeObject(output, new Sub(), serializer);
		kryo.readObject(new Input(output.toBytes()), Sub.class, serializer);
	}

	private String names (FieldSerializer serializer) {
		StringBuilder buffer = new StringBuilder();
		for (CachedField field : serializer.getFields())
			buffer.append(buffer.length() == 0 ? "" : ",").append(field.getName());
		return buffer.toString();
	}

	private byte[] write (Kryo kryo, FieldSerializer<Data> serializer) {
		Data data = new Data();
		data.a = 5;
		data.b = 5;
		data.c = "c5";
		Output output = new Output(64, -1);
		kryo.writeObject(output, data, serializer);
		return output.toBytes();
	}

	private void assertRead (Kryo kryo, FieldSerializer<Data> serializer, int a, int b, String c, String name) {
		Data data = kryo.readObject(new Input(write(kryo, serializer)), Data.class, serializer);
		assertEquals(a, data.a, name);
		assertEquals(b, data.b, name);
		assertEquals(c, data.c, name);
	}

	public static class Data {
		@Tag(1) @Since(1) public int a = 1;
		@Tag(2) public int b = 2;
		@Tag(3) @Since(2) public String c = "c";
	}

	public static class Base {
		public int value = 1;
	}

	public static class Sub extends Base {
		public int value = 2;
	}
}
