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

import java.lang.reflect.Field;
import java.util.Arrays;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;
import com.esotericsoftware.kryo.util.ListReferenceResolver;
import com.esotericsoftware.kryo.util.MapReferenceResolver;

public class FailureContainmentTest extends KryoTestCase {
	public void testInvalidMapReferences () {
		assertInvalidReferences(new MapReferenceResolver());
	}

	public void testInvalidListReferences () {
		assertInvalidReferences(new ListReferenceResolver());
	}

	private void assertInvalidReferences (ReferenceResolver resolver) {
		kryo.setReferenceResolver(resolver);
		for (boolean nullable : new boolean[] {false, true}) {
			for (int encodedId : new int[] {0, 2, 39, Integer.MAX_VALUE}) {
				if (nullable && encodedId == Kryo.NULL) continue;
				Output output = new Output(16);
				output.writeVarInt(encodedId, true);
				Input input = new Input(output.toBytes());
				try {
					if (nullable)
						kryo.readObjectOrNull(input, String.class);
					else
						kryo.readObject(input, String.class);
					fail("Expected an invalid reference to fail.");
				} catch (KryoException ex) {
					assertTrue(ex.getCause() instanceof IndexOutOfBoundsException);
					assertTrue(ex.getMessage().contains("reference ID " + (encodedId - 2)));
					assertTrue(ex.getMessage().contains(String.class.getName()));
				}
			}
		}
	}

	public void testReferenceExceptionCause () {
		assertReferenceFailure(new IllegalStateException("synthetic resolver failure"));
	}

	public void testExistingReferenceKryoException () {
		assertReferenceFailure(new KryoException("existing resolver failure"));
	}

	public void testReferenceErrorsAreNotWrapped () {
		assertReferenceFailure(new OutOfMemoryError("synthetic resolver allocation failure"));
	}

	private void assertReferenceFailure (final Throwable failure) {
		kryo.setReferenceResolver(new MapReferenceResolver() {
			public Object getReadObject (Class type, int id) {
				throwFailure(failure);
				return null;
			}
		});
		try {
			kryo.readObject(new Input(new byte[] {2}), String.class);
			fail("Expected a resolver failure.");
		} catch (KryoException ex) {
			assertFalse("Resolver errors must not be wrapped.", failure instanceof Error);
			if (failure instanceof KryoException)
				assertSame(failure, ex);
			else
				assertSame(failure, ex.getCause());
		} catch (OutOfMemoryError ex) {
			assertSame(failure, ex);
		}
	}

	public void testFieldReadAllocationFailures () throws Exception {
		assertFieldFailures(new OutOfMemoryError("synthetic read allocation failure"), true);
	}

	public void testFieldWriteAllocationFailures () throws Exception {
		assertFieldFailures(new OutOfMemoryError("synthetic write allocation failure"), false);
	}

	public void testFieldReadExceptionCauses () throws Exception {
		assertFieldFailures(new IllegalStateException("synthetic field read failure"), true);
	}

	public void testFieldWriteExceptionCauses () throws Exception {
		assertFieldFailures(new IllegalStateException("synthetic field write failure"), false);
	}

	public void testExistingFieldReadKryoExceptions () throws Exception {
		assertFieldFailures(new KryoException("existing field read failure"), true);
	}

	public void testExistingFieldWriteKryoExceptions () throws Exception {
		assertFieldFailures(new KryoException("existing field write failure"), false);
	}

	public void testOtherFieldErrorsAreNotWrapped () throws Exception {
		assertFieldFailures(new AssertionError("synthetic field error"), true);
		assertFieldFailures(new AssertionError("synthetic field error"), false);
	}

	private void assertFieldFailures (final Throwable failure, boolean read) throws Exception {
		String originalMessage = failure.getMessage();
		for (boolean transientField : new boolean[] {false, true}) {
			FieldSerializer<FieldData> serializer = new FieldSerializer<FieldData>(kryo, FieldData.class);
			serializer.setSerializeTransient(true);
			kryo.register(FieldData.class, serializer);
			if (transientField) serializer.removeField("value");
			final Field field = FieldData.class.getField(transientField ? "transientValue" : "value");
			CachedField[] fields = transientField ? serializer.getTransientFields() : serializer.getFields();
			fields[0] = new CachedField() {
				public Field getField () {
					return field;
				}

				public void read (Input input, Object object) {
					input.readByte();
					throwFailure(failure);
				}

				public void write (Output output, Object object) {
					output.writeByte(0);
					throwFailure(failure);
				}

				public void copy (Object original, Object copy) {
					throw new AssertionError("Copy should not be called.");
				}
			};
			Input input = new Input(new byte[16]);
			Output output = new Output(16);
			input.setPosition(7);
			output.setPosition(7);
			try {
				if (read)
					serializer.read(kryo, input, FieldData.class);
				else
					serializer.write(kryo, output, new FieldData());
				fail("Expected a field failure.");
			} catch (KryoException ex) {
				assertFalse("Other field errors must not be wrapped.", failure instanceof AssertionError);
				if (failure instanceof KryoException) {
					assertSame(failure, ex);
					assertEquals(originalMessage, ex.getMessage());
				} else {
					assertSame(failure, ex.getCause());
					assertTrue(ex.getMessage().contains(field.getName()));
					assertTrue(ex.getMessage().contains(FieldData.class.getName()));
					assertTrue(ex.getMessage().contains(read ? "reading" : "writing"));
					assertTrue(ex.getMessage().contains((read ? "input" : "output") + " position 8"));
				}
			} catch (AssertionError ex) {
				assertSame(failure, ex);
			}
		}
	}

	public void testSubclassFieldFailures () throws Exception {
		for (boolean read : new boolean[] {false, true}) {
			for (Throwable failure : new Throwable[] {new IllegalStateException("synthetic field failure"),
				new OutOfMemoryError("synthetic allocation failure"), new KryoException("existing field failure")}) {
				assertSubclassFieldFailure(new VersionFieldSerializer<FieldData>(kryo, FieldData.class), FieldData.class, failure, read);
				assertSubclassFieldFailure(new CompatibleFieldSerializer<FieldData>(kryo, FieldData.class), FieldData.class,
					failure, read);
				assertSubclassFieldFailure(new TaggedFieldSerializer<TaggedData>(kryo, TaggedData.class), TaggedData.class,
					failure, read);
				assertSubclassFieldFailure(new TaggedFieldSerializer<AnnexedData>(kryo, AnnexedData.class), AnnexedData.class,
					failure, read);
			}
		}
	}

	private <T> void assertSubclassFieldFailure (FieldSerializer<T> serializer, Class<T> type, final Throwable failure,
		boolean read) throws Exception {
		kryo.register(type, serializer);
		final Field field = type.getField("value");
		Output data = new Output(32);
		if (read) {
			serializer.write(kryo, data, type.newInstance());
			kryo.reset();
		}
		CachedField[] fields = serializer.getFields();
		CachedField failingField = new CachedField() {
			public Field getField () {
				return field;
			}

			public void read (Input input, Object object) {
				input.readByte();
				throwFailure(failure);
			}

			public void write (Output output, Object object) {
				output.writeByte(0);
				throwFailure(failure);
			}

			public void copy (Object original, Object copy) {
				throw new AssertionError("Copy should not be called.");
			}
		};
		Field cachedFieldField = CachedField.class.getDeclaredField("field");
		cachedFieldField.setAccessible(true);
		cachedFieldField.set(failingField, field);
		fields[0] = failingField;
		try {
			if (read)
				serializer.read(kryo, new Input(data.toBytes()), type);
			else
				serializer.write(kryo, new Output(32), type.newInstance());
			fail("Expected a field failure.");
		} catch (KryoException ex) {
			if (failure instanceof KryoException) {
				assertSame(failure, ex);
			} else {
				assertSame(ex.getMessage(), failure, ex.getCause());
				assertTrue(ex.getMessage().contains(field.getName()));
				assertTrue(ex.getMessage().contains(type.getName()));
				assertTrue(ex.getMessage().contains(read ? "input position" : "output position"));
			}
		}
	}

	public void testNestedSerializerReadAllocationFailures () {
		assertNestedAllocationFailures(true);
	}

	public void testNestedSerializerWriteAllocationFailures () {
		assertNestedAllocationFailures(false);
	}

	private void assertNestedAllocationFailures (boolean read) {
		final OutOfMemoryError failure = new OutOfMemoryError("synthetic nested allocation failure");
		Serializer<byte[]> failingSerializer = new Serializer<byte[]>() {
			public byte[] read (Kryo kryo, Input input, Class<byte[]> type) {
				input.readByte();
				throw failure;
			}

			public void write (Kryo kryo, Output output, byte[] object) {
				output.writeByte(0);
				throw failure;
			}
		};
		for (boolean transientField : new boolean[] {false, true}) {
			FieldSerializer<ByteArrayFields> serializer = new FieldSerializer<ByteArrayFields>(kryo, ByteArrayFields.class);
			serializer.setSerializeTransient(true);
			kryo.register(ByteArrayFields.class, serializer);
			if (transientField) serializer.removeField("value");
			CachedField field = transientField ? serializer.getTransientFields()[0] : serializer.getFields()[0];
			field.setClass(byte[].class, failingSerializer);
			field.setCanBeNull(false);
			Input input = new Input(new byte[16]);
			Output output = new Output(16);
			input.setPosition(7);
			output.setPosition(7);
			try {
				if (read)
					serializer.read(kryo, input, ByteArrayFields.class);
				else
					serializer.write(kryo, output, new ByteArrayFields());
				fail("Expected a nested allocation failure.");
			} catch (KryoException ex) {
				assertSame(failure, ex.getCause());
				assertTrue(ex.getMessage().contains(field.getField().getName()));
				assertTrue(ex.getMessage().contains(read ? "reading" : "writing"));
				assertTrue(ex.getMessage().contains((read ? "input" : "output") + " position 8"));
			}
		}
	}

	public void testValidFieldBytes () {
		FieldSerializer<FieldData> serializer = new FieldSerializer<FieldData>(kryo, FieldData.class);
		serializer.setSerializeTransient(true);
		kryo.register(FieldData.class, serializer);
		FieldData data = new FieldData();
		data.value = 42;
		data.transientValue = 7;
		Output output = new Output(16);
		serializer.write(kryo, output, data);
		assertTrue(Arrays.equals(new byte[] {84, 14}, output.toBytes()));
		FieldData restored = serializer.read(kryo, new Input(output.toBytes()), FieldData.class);
		assertEquals(data.value, restored.value);
		assertEquals(data.transientValue, restored.transientValue);
	}

	private static void throwFailure (Throwable failure) {
		if (failure instanceof RuntimeException) throw (RuntimeException)failure;
		throw (Error)failure;
	}

	static public class ByteArrayFields {
		public byte[] value = new byte[0];
		public transient byte[] transientValue = new byte[0];
	}

	static public class FieldData {
		public int value;
		public transient int transientValue;
	}

	static public class TaggedData {
		@Tag(1)
		public int value;
	}

	static public class AnnexedData {
		@Tag(value = 1, annexed = true)
		public int value;
	}
}
