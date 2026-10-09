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
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.SerializerFactory.CompatibleFieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.FieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.TaggedFieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.VersionFieldSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer.Bind;
import com.esotericsoftware.kryo.serializers.FieldSerializer.FieldAccessType;
import com.esotericsoftware.kryo.serializers.FieldSerializer.NotNull;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer.Since;
import com.esotericsoftware.kryo.util.MapReferenceResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests {@link CodeGeneration}, which needs Java 24+. */
class CodeGenerationTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@BeforeEach
	public void setUp () throws Exception {
		assumeTrue(CachedFields.codeGeneration, "Code generation is not supported on this platform.");
		super.setUp();
		kryo.setDefaultSerializer(codeGeneration());
	}

	static private FieldSerializerFactory codeGeneration () {
		FieldSerializerFactory factory = new FieldSerializerFactory();
		factory.getConfig().setCodeGeneration(true);
		return factory;
	}

	@Test
	void testAllFieldKinds () {
		kryo.register(AllKinds.class);
		kryo.register(Nested.class);
		kryo.register(Color.class);
		kryo.register(ArrayList.class);
		kryo.register(HashMap.class);
		kryo.register(int[].class);
		AllKinds object = AllKinds.create();
		assertGenerated(AllKinds.class);
		roundTrip(83, object);

		object.nested = null;
		object.string = null;
		object.list = null;
		object.integer = null;
		roundTrip(57, object);
	}

	@Test
	void testSameBytesAsCachedFields () {
		Kryo cachedFields = new Kryo();
		FieldSerializerFactory factory = new FieldSerializerFactory();
		factory.getConfig().setCodeGeneration(false); // In case the system property enables it.
		cachedFields.setDefaultSerializer(factory);
		for (Kryo k : new Kryo[] {kryo, cachedFields}) {
			k.register(AllKinds.class);
			k.register(Nested.class);
			k.register(Color.class);
			k.register(ArrayList.class);
			k.register(HashMap.class);
			k.register(int[].class);
		}
		assertNull(((FieldSerializer)cachedFields.getSerializer(AllKinds.class)).generated);
		assertGenerated(AllKinds.class);

		AllKinds object = AllKinds.create();
		assertArrayEquals(write(cachedFields, object), write(kryo, object));

		// Read with each, write with the other.
		assertEquals(object, read(kryo, write(cachedFields, object), AllKinds.class));
		assertEquals(object, read(cachedFields, write(kryo, object), AllKinds.class));

		// With references, which the object graph has (nested is in the list and the map).
		kryo.setReferences(true);
		cachedFields.setReferences(true);
		byte[] bytes = write(kryo, object);
		assertArrayEquals(write(cachedFields, object), bytes);
		assertEquals(object, read(cachedFields, bytes, AllKinds.class));
		AllKinds read = read(kryo, bytes, AllKinds.class);
		assertEquals(object, read);
		assertSame(read.nested, read.list.get(1));
		assertSame(read.nested, read.map.get("key"));
	}

	@Test
	void testWithoutVarEncodingAndReferences () {
		kryo.setReferences(false);
		FieldSerializerFactory factory = codeGeneration();
		factory.getConfig().setVariableLengthEncoding(false);
		kryo.setDefaultSerializer(factory);
		kryo.register(AllKinds.class);
		kryo.register(Nested.class);
		kryo.register(Color.class);
		kryo.register(ArrayList.class);
		kryo.register(HashMap.class);
		kryo.register(int[].class);
		assertGenerated(AllKinds.class);
		roundTrip(102, AllKinds.create());
	}

	@Test
	void testStringFieldWithReferences () {
		kryo.register(Strings.class);
		assertGenerated(Strings.class);
		assertFalse(((FieldSerializer)kryo.getSerializer(Strings.class)).getField("a") instanceof ReflectField);
		Strings object = new Strings();
		object.a = "same";
		object.b = "same";
		object.c = "other";
		roundTrip(14, object);

		// With references for strings, a String field is an object field: written with the StringSerializer and references. The
		// resolver must be set before registering classes with String fields, so a new Kryo instance is used.
		kryo = new Kryo();
		kryo.setDefaultSerializer(codeGeneration());
		kryo.setReferenceResolver(new MapReferenceResolver() {
			public boolean useReferences (Class type) {
				return type == String.class || super.useReferences(type);
			}
		});
		kryo.register(Strings.class);
		assertGenerated(Strings.class);
		assertTrue(((FieldSerializer)kryo.getSerializer(Strings.class)).getField("a") instanceof ReflectField);
		roundTrip(14, object); // 1 class, 1 not null, 5 + 1 (reference) + 6 strings.
	}

	@Test
	void testSharedByKryoInstances () {
		Kryo kryo2 = new Kryo();
		kryo2.setDefaultSerializer(codeGeneration());
		kryo.register(Nested.class);
		kryo2.register(Nested.class);
		GeneratedFields generated1 = assertGenerated(Nested.class);
		GeneratedFields generated2 = ((FieldSerializer)kryo2.getSerializer(Nested.class)).generated;
		assertNotNull(generated2);
		assertNotSame(generated1, generated2);
		assertSame(generated1.getClass(), generated2.getClass());
		assertTrue(generated1.getClass().isHidden());
	}

	@Test
	void testRemoveFieldAndUpdateFields () {
		kryo.register(Nested.class);
		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(Nested.class);
		GeneratedFields generated = serializer.generated;
		assertNotNull(generated);

		// Field settings apply without updateFields, like for the cached fields.
		Nested object = new Nested();
		object.value = 123;
		object.name = "name";
		int length = write(kryo, object).length;
		serializer.getField("value").setVariableLengthEncoding(false);
		assertEquals(length + 2, write(kryo, object).length); // 4 instead of 2 bytes.
		assertEquals(123, read(kryo, write(kryo, object), Nested.class).value);
		serializer.getField("value").setVariableLengthEncoding(true);

		serializer.removeField("value");
		assertNotNull(serializer.generated);
		assertNotSame(generated, serializer.generated);
		assertNotSame(generated.getClass(), serializer.generated.getClass());
		Nested read = read(kryo, write(kryo, object), Nested.class);
		assertEquals(0, read.value);
		assertEquals("name", read.name);

		// The removed field stays removed.
		serializer.getFieldSerializerConfig().setVariableLengthEncoding(false);
		serializer.updateFields();
		assertNotNull(serializer.generated);
		assertEquals(4, write(kryo, object).length);

		serializer.getFieldSerializerConfig().setCodeGeneration(false);
		serializer.updateFields();
		assertNull(serializer.generated);
		assertEquals(4, write(kryo, object).length);
	}

	@Test
	void testFinalField () {
		kryo.register(FinalField.class);
		assertGenerated(FinalField.class);
		roundTrip(6, new FinalField(7, "name"));
	}

	@Test
	void testFinalFieldDenied () {
		// If setting final fields with reflection is denied, the generated code sets them with the FinalFieldSetter of the cached
		// field. The class is only used here, because the setter of a field is resolved once per JVM.
		assumeTrue(Runtime.version().feature() >= 24, "FinalFieldSetter needs Java 24+.");
		try {
			FinalFieldSetter.force = true;
			FieldSerializer serializer = new FieldSerializer(kryo, DeniedFinalField.class);
			serializer.getFieldSerializerConfig().setCodeGeneration(true);
			serializer.getFieldSerializerConfig().setFieldAccess(FieldAccessType.VARHANDLE); // Unsafe sets final fields itself.
			serializer.updateFields();
			kryo.register(DeniedFinalField.class, serializer);
			assertGenerated(DeniedFinalField.class);
			roundTrip(6, new DeniedFinalField(7, "name"));
			assertNotNull(serializer.getField("value").finalSetter);
			assertFalse(serializer.getField("name").finalUnresolved);
		} finally {
			FinalFieldSetter.force = false;
		}
	}

	@Test
	void testFinalFieldDeniedUnsafe () {
		// With Unsafe field access, the generated code sets a final field with Unsafe if setting it with reflection is denied.
		assumeTrue(com.esotericsoftware.kryo.util.Util.unsafe);
		try {
			FinalFieldSetter.force = true;
			FieldSerializer serializer = new FieldSerializer(kryo, DeniedFinalField.class);
			serializer.getFieldSerializerConfig().setCodeGeneration(true);
			serializer.getFieldSerializerConfig().setFieldAccess(FieldAccessType.UNSAFE);
			serializer.updateFields();
			kryo.register(DeniedFinalField.class, serializer);
			assertGenerated(DeniedFinalField.class);
			roundTrip(6, new DeniedFinalField(7, "name"));
			assertNull(serializer.getField("value").finalSetter);
			assertNull(serializer.getField("name").finalSetter);
		} finally {
			FinalFieldSetter.force = false;
		}
	}

	@Test
	void testFinalFieldDeniedUnsafeIncompatibleClass () {
		// The Unsafe fallback checks the type like UnsafeField, so a class in the data that isn't assignable to the field type throws
		// instead of setting the field to a value of the wrong type.
		assumeTrue(com.esotericsoftware.kryo.util.Util.unsafe);
		Kryo writer = new Kryo();
		writer.register(ObjectValue.class, 100);
		Output output = new Output(64);
		writer.writeObject(output, new ObjectValue());
		try {
			FinalFieldSetter.force = true;
			FieldSerializer serializer = new FieldSerializer(kryo, FinalNumberValue.class);
			serializer.getFieldSerializerConfig().setCodeGeneration(true);
			serializer.getFieldSerializerConfig().setFieldAccess(FieldAccessType.UNSAFE);
			serializer.updateFields();
			kryo.register(FinalNumberValue.class, serializer, 100);
			assertGenerated(FinalNumberValue.class);
			KryoException ex = assertThrows(KryoException.class,
				() -> kryo.readObject(new Input(output.toBytes()), FinalNumberValue.class));
			Throwable cause = ex;
			while (cause.getCause() != null)
				cause = cause.getCause();
			assertTrue(cause.getMessage().startsWith("Can not set java.lang.Number field value to java.lang.String"), cause.getMessage());
		} finally {
			FinalFieldSetter.force = false;
		}
	}

	@Test
	void testFinalFieldSecondSerializer () {
		// The call site of a final field is shared by all serializers of the class, it is resolved by the first that sets the field.
		// The cached field of another serializer resolves its own setter when it is first set outside the generated code, eg by
		// copy, so the generated code doesn't change the cached fields of other serializers.
		kryo.register(FinalField.class);
		roundTrip(6, new FinalField(7, "name"));
		Kryo other = new Kryo();
		FieldSerializer serializer = new FieldSerializer(other, FinalField.class);
		serializer.getFieldSerializerConfig().setCodeGeneration(true);
		serializer.getFieldSerializerConfig().setFieldAccess(FieldAccessType.VARHANDLE); // Unsafe fields have no setter.
		serializer.updateFields();
		other.register(FinalField.class, serializer);
		assertNotNull(serializer.generated);
		assertTrue(serializer.getField("value").finalUnresolved);
		FinalField copy = other.copy(new FinalField(8, "copy"));
		assertEquals(8, copy.value);
		assertEquals("copy", copy.name);
		assertFalse(serializer.getField("value").finalUnresolved);
		assertFalse(serializer.getField("name").finalUnresolved);
	}

	@Test
	void testNotSupported () {
		// Records use the cached fields.
		kryo.register(Point.class);
		assertNull(((FieldSerializer)kryo.getSerializer(Point.class)).generated);
		roundTrip(3, new Point(1, 2));

	}

	@Test
	void testManyFields () {
		// The fields are written and read in batches of private methods, which the JIT can compile.
		kryo.register(Wide.class);
		GeneratedFields generated = assertGenerated(Wide.class);
		int batches = 0;
		for (java.lang.reflect.Method method : generated.getClass().getDeclaredMethods())
			if (method.getName().startsWith("write") && method.getName().length() > 5) batches++;
		assertEquals(2 * ((Wide.count + CodeGeneration.batchSize - 1) / CodeGeneration.batchSize), batches);
		Wide object = new Wide();
		object.f0 = 1;
		object.f99 = 99;
		object.f199 = 199;
		Wide read = read(kryo, write(kryo, object), Wide.class);
		assertEquals(1, read.f0);
		assertEquals(99, read.f99);
		assertEquals(199, read.f199);
		assertEquals(0, read.f100);
	}

	@Test
	void testTypeVariableField () {
		// T resolved to String: a String field with the field type Object.
		kryo.register(StringBox.class);
		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(StringBox.class);
		assertNotNull(serializer.generated);
		assertFalse(serializer.getField("value") instanceof ReflectField);
		StringBox object = new StringBox();
		object.value = "value";
		roundTrip(6, object);
	}

	@Test
	void testFinalTypeVariableField () {
		// A final T resolved to String: the setter takes the field type Object, the generated code passes a String.
		kryo.register(FinalStringBox.class);
		assertGenerated(FinalStringBox.class);
		roundTrip(6, new FinalStringBox("value"));
	}

	@Test
	void testTypeVariableFieldWithOtherClass () {
		// With unknown field data, the String field for a T resolved to String can only read a String, with and without generated
		// code. Other data was read as a String, which corrupted the stream.
		for (boolean generated : new boolean[] {true, false}) {
			Kryo kryo = new Kryo();
			CompatibleFieldSerializerFactory factory = compatible(true, false);
			factory.getConfig().setCodeGeneration(generated);
			kryo.setDefaultSerializer(factory);
			kryo.register(StringBox.class);
			assertEquals(generated, ((FieldSerializer)kryo.getSerializer(StringBox.class)).generated != null);
			Output output = new Output(64);
			output.writeVarInt(1, true);
			output.writeString("value");
			kryo.writeClass(output, Integer.class);
			output.writeVarInt(5, false);
			byte[] bytes = output.toBytes();
			KryoException ex = assertThrows(KryoException.class, () -> read(kryo, bytes, StringBox.class));
			assertTrue(ex.getMessage().contains("Read type is incompatible with the field type: int -> String"), ex.getMessage());
		}
	}

	@Test
	void testFieldNameInError () {
		// The generated code names the field that failed, like the loop over the cached fields.
		kryo.register(Nested.class);
		GeneratedFields generated = assertGenerated(Nested.class);
		KryoException ex = assertThrows(KryoException.class, () -> generated.write(new Output(64), null));
		assertTrue(ex.getMessage().startsWith("Error writing name at position"), ex.getMessage());
		Nested nested = new Nested();
		nested.name = "name";
		byte[] bytes = write(kryo, nested);
		ex = assertThrows(KryoException.class, () -> generated.read(new Input(bytes), null));
		assertTrue(ex.getMessage().startsWith("Error reading name at position"), ex.getMessage());
	}

	@Test
	void testVersionFieldSerializer () {
		VersionFieldSerializerFactory factory = new VersionFieldSerializerFactory();
		factory.getConfig().setCodeGeneration(true);
		kryo.setDefaultSerializer(factory);
		kryo.register(Versioned.class);
		kryo.register(Nested.class);
		assertGenerated(Versioned.class);
		Versioned object = new Versioned();
		object.value = 5;
		object.name = "name";
		object.nested = new Nested();
		object.nested.value = 7;
		roundTrip(11, object);

		// Data of version 0, without the field added since version 1, is read with the cached fields.
		Output output = new Output(64);
		output.writeVarInt(0 + 1, true);
		output.writeString("name");
		output.writeVarInt(5, false);
		Versioned read = read(kryo, output.toBytes(), Versioned.class);
		assertEquals(5, read.value);
		assertEquals("name", read.name);
		assertNull(read.nested);
	}

	@Test
	void testCompatibleFieldSerializer () {
		testCompatibleFieldSerializer(false, false, 161);
	}

	@Test
	void testCompatibleFieldSerializerUnknownFieldData () {
		testCompatibleFieldSerializer(true, false, 182);
	}

	@Test
	void testCompatibleFieldSerializerChunked () {
		testCompatibleFieldSerializer(false, true, 0);
	}

	@Test
	void testCompatibleFieldSerializerChunkedUnknownFieldData () {
		testCompatibleFieldSerializer(true, true, 0);
	}

	@SuppressWarnings("deprecation") // Kryo 5 chunks.
	private void testCompatibleFieldSerializer (boolean readUnknownFieldData, boolean chunked, int length) {
		CompatibleFieldSerializerFactory factory = compatible(readUnknownFieldData, chunked);
		factory.getConfig().setCodeGeneration(true);
		kryo.setDefaultSerializer(factory);
		kryo.register(AllKinds.class);
		kryo.register(Nested.class);
		kryo.register(Color.class);
		kryo.register(ArrayList.class);
		kryo.register(HashMap.class);
		kryo.register(int[].class);
		assertGenerated(AllKinds.class);
		assertGenerated(Nested.class);
		AllKinds object = AllKinds.create();
		roundTrip(length == 0 ? Integer.MIN_VALUE : length, object);
		object.nested = null;
		object.string = null;
		object.list = null;
		object.integer = null;
		roundTrip(Integer.MIN_VALUE, object);

		// The same bytes as the cached fields, which can read the generated bytes and vice versa.
		Kryo cachedFields = new Kryo();
		CompatibleFieldSerializerFactory cachedFieldsFactory = compatible(readUnknownFieldData, chunked);
		cachedFieldsFactory.getConfig().setCodeGeneration(false); // In case the system property enables it.
		cachedFields.setDefaultSerializer(cachedFieldsFactory);
		cachedFields.register(AllKinds.class);
		cachedFields.register(Nested.class);
		cachedFields.register(Color.class);
		cachedFields.register(ArrayList.class);
		cachedFields.register(HashMap.class);
		cachedFields.register(int[].class);
		assertNull(((FieldSerializer)cachedFields.getSerializer(AllKinds.class)).generated);
		object = AllKinds.create();
		byte[] bytes = write(kryo, object);
		assertArrayEquals(write(cachedFields, object), bytes);
		assertEquals(object, read(cachedFields, bytes, AllKinds.class));
		assertEquals(object, read(kryo, write(cachedFields, object), AllKinds.class));

		// Data with other field names (a removed field) is read with the cached fields.
		Kryo other = new Kryo();
		other.setDefaultSerializer(compatible(readUnknownFieldData, chunked));
		other.register(Nested.class);
		((FieldSerializer)other.getSerializer(Nested.class)).removeField("value");
		Nested nested = new Nested();
		nested.value = 5;
		nested.name = "name";
		Nested read = read(kryo, write(other, nested), Nested.class);
		assertEquals(0, read.value);
		assertEquals("name", read.name);
		if (chunked) { // The unknown field is skipped.
			read = read(other, write(kryo, nested), Nested.class);
			assertEquals(0, read.value);
			assertEquals("name", read.name);
		}

		if (readUnknownFieldData && !chunked) {
			// A null value of a primitive field keeps the field value, an incompatible class is an error.
			Output output = new Output(64);
			writeNestedFieldNames(output); // name, value
			kryo.writeClass(output, String.class);
			output.writeString("name");
			kryo.writeClass(output, null);
			read = read(kryo, output.toBytes(), Nested.class);
			assertEquals(0, read.value);
			assertEquals("name", read.name);

			output.reset();
			writeNestedFieldNames(output);
			kryo.writeClass(output, String.class);
			output.writeString("name");
			kryo.writeClass(output, String.class);
			output.writeString("not an int");
			byte[] wrong = output.toBytes();
			KryoException ex = assertThrows(KryoException.class, () -> read(kryo, wrong, Nested.class));
			assertTrue(ex.getMessage().contains("Read type is incompatible with the field type"), ex.getMessage());
		}

		if (readUnknownFieldData && chunked) {
			// Incompatible classes of known fields are skipped in the chunks, the fields keep their values (also a final field).
			Kryo wrongType = new Kryo();
			wrongType.setDefaultSerializer(compatible(true, true));
			wrongType.register(FinalFieldOther.class);
			FinalFieldOther wrongObject = new FinalFieldOther();
			wrongObject.value = "not an int";
			wrongObject.name = 5;
			byte[] otherBytes = write(wrongType, wrongObject);
			kryo.register(FinalField.class);
			kryo.register(FinalFieldOther.class, new CompatibleFieldSerializer<FinalField>(kryo, FinalField.class, factory.getConfig()) {
				protected FinalField create (Kryo kryo, Input input, Class type) {
					return new FinalField(7, "name");
				}
			});
			assertGenerated(FinalField.class);
			FinalField skipped = (FinalField)kryo.readObject(new Input(otherBytes), (Class)FinalFieldOther.class);
			assertEquals(7, skipped.value);
			assertEquals("name", skipped.name);
		}

		// readUnknownFieldData can be changed without updateFields: the code is regenerated.
		CompatibleFieldSerializer serializer = (CompatibleFieldSerializer)kryo.getSerializer(Nested.class);
		assertEquals(readUnknownFieldData, serializer.generated().writesClasses);
		serializer.getCompatibleFieldSerializerConfig().setReadUnknownFieldData(!readUnknownFieldData);
		assertEquals(!readUnknownFieldData, serializer.generated().writesClasses);
		cachedFieldsFactory.getConfig().setReadUnknownFieldData(!readUnknownFieldData);
		cachedFields.register(Nested.class, new CompatibleFieldSerializer(cachedFields, Nested.class, cachedFieldsFactory.getConfig()));
		assertArrayEquals(write(cachedFields, nested), write(kryo, nested));
		serializer.getCompatibleFieldSerializerConfig().setReadUnknownFieldData(readUnknownFieldData);

		// The chunked encoding of Kryo 5 uses the cached fields.
		factory.getConfig().setChunkedEncoding(true);
		factory.getConfig().setLegacyChunks(true);
		kryo.register(Nested.class, new CompatibleFieldSerializer(kryo, Nested.class, factory.getConfig()));
		assertNull(((FieldSerializer)kryo.getSerializer(Nested.class)).generated);
	}

	@Test
	void testTaggedFieldSerializer () {
		testTaggedFieldSerializer(false, false, 13);
	}

	@Test
	void testTaggedFieldSerializerUnknownTagData () {
		testTaggedFieldSerializer(true, false, 15);
	}

	@Test
	void testTaggedFieldSerializerChunked () {
		testTaggedFieldSerializer(false, true, 0);
	}

	@Test
	void testTaggedFieldSerializerChunkedUnknownTagData () {
		testTaggedFieldSerializer(true, true, 0);
	}

	@SuppressWarnings("deprecation") // Kryo 5 chunks.
	private void testTaggedFieldSerializer (boolean readUnknownTagData, boolean chunked, int length) {
		TaggedFieldSerializerFactory factory = tagged(readUnknownTagData, chunked);
		factory.getConfig().setCodeGeneration(true);
		kryo.setDefaultSerializer(factory);
		kryo.register(Tagged.class);
		kryo.register(Nested.class, new FieldSerializer(kryo, Nested.class)); // Nested has no tags.
		assertGenerated(Tagged.class);
		Tagged object = Tagged.create();
		roundTrip(length == 0 ? Integer.MIN_VALUE : length, object);
		object.nested = null;
		object.name = null;
		roundTrip(Integer.MIN_VALUE, object);

		// The same bytes as the cached fields, which can read the generated bytes and vice versa.
		Kryo cachedFields = new Kryo();
		TaggedFieldSerializerFactory cachedFieldsFactory = tagged(readUnknownTagData, chunked);
		cachedFieldsFactory.getConfig().setCodeGeneration(false);
		cachedFields.setDefaultSerializer(cachedFieldsFactory);
		cachedFields.register(Tagged.class);
		cachedFields.register(Nested.class, new FieldSerializer(cachedFields, Nested.class));
		assertNull(((FieldSerializer)cachedFields.getSerializer(Tagged.class)).generated);
		object = Tagged.create();
		byte[] bytes = write(kryo, object);
		assertArrayEquals(write(cachedFields, object), bytes);
		assertEquals(object, read(cachedFields, bytes, Tagged.class));
		assertEquals(object, read(kryo, write(cachedFields, object), Tagged.class));

		// The tags in another order, with a deprecated tag instead of a written one: read with readTag.
		Tagged read;
		if (!chunked) {
			Output output = new Output(64);
			output.writeVarInt(3 + 1, true);
			writeTagged(kryo, output, 3, readUnknownTagData, "name");
			writeTagged(kryo, output, 1, readUnknownTagData, 5);
			writeTagged(kryo, output, 2, readUnknownTagData, 7); // The deprecated field.
			read = read(kryo, output.toBytes(), Tagged.class);
			assertEquals(5, read.value);
			assertEquals(7, read.old);
			assertEquals("name", read.name);
			assertNull(read.nested);

			// An unknown tag.
			output.reset();
			output.writeVarInt(3 + 1, true);
			writeTagged(kryo, output, 1, readUnknownTagData, 5);
			writeTagged(kryo, output, 99, readUnknownTagData, 8);
			writeTagged(kryo, output, 3, readUnknownTagData, "name");
			byte[] unknown = output.toBytes();
			if (readUnknownTagData) {
				read = read(kryo, unknown, Tagged.class);
				assertEquals(5, read.value);
				assertEquals("name", read.name);
			} else {
				KryoException ex = assertThrows(KryoException.class, () -> read(kryo, unknown, Tagged.class));
				assertTrue(ex.getMessage().contains("Unknown field tag: 99"), ex.getMessage());
			}
		}

		// Data of a class with other tags: the written ones in another order, one removed, one unknown (skipped if chunked).
		Kryo other = new Kryo();
		other.setDefaultSerializer(tagged(readUnknownTagData, chunked));
		other.register(OtherTagged.class);
		other.register(Nested.class, new FieldSerializer(other, Nested.class));
		// Read the other class's data as Tagged, with Tagged's generated code.
		kryo.register(OtherTagged.class, new TaggedFieldSerializer<Tagged>(kryo, Tagged.class, factory.getConfig()) {
			protected Tagged create (Kryo kryo, Input input, Class type) {
				return new Tagged();
			}
		});
		OtherTagged otherObject = new OtherTagged();
		otherObject.name = "name";
		otherObject.value = 5;
		otherObject.unknown = 8;
		byte[] otherBytes = write(other, otherObject);
		if (chunked || readUnknownTagData) {
			read = (Tagged)kryo.readObject(new Input(otherBytes), (Class)OtherTagged.class);
			assertEquals(5, read.value);
			assertEquals("name", read.name);
			assertNull(read.nested);
		} else {
			KryoException ex = assertThrows(KryoException.class, () -> kryo.readObject(new Input(otherBytes), (Class)OtherTagged.class));
			assertTrue(ex.getMessage().contains("Unknown field tag: 99"), ex.getMessage());
		}

		// readUnknownTagData can be changed without updateFields: the code is regenerated.
		TaggedFieldSerializer serializer = (TaggedFieldSerializer)kryo.getSerializer(Tagged.class);
		assertEquals(readUnknownTagData, serializer.generated().writesClasses);
		serializer.getTaggedFieldSerializerConfig().setReadUnknownTagData(!readUnknownTagData);
		assertEquals(!readUnknownTagData, serializer.generated().writesClasses);
		serializer.getTaggedFieldSerializerConfig().setReadUnknownTagData(readUnknownTagData);

		if (readUnknownTagData) {
			// A null class for a mismatched tag sets the field to null (readTag), an incompatible class of an expected tag is an
			// error without chunks and skipped with chunks, in both cases the fields keep their values.
			TaggedFieldSerializer<Tagged> nulls = new TaggedFieldSerializer<Tagged>(kryo, Tagged.class, factory.getConfig()) {
				protected Tagged create (Kryo kryo, Input input, Class type) {
					return Tagged.create();
				}
			};
			kryo.register(OtherTagged.class, nulls);
			Kryo nullsKryo = new Kryo();
			nullsKryo.setDefaultSerializer(tagged(true, chunked));
			nullsKryo.register(OtherTagged.class);
			OtherTagged nullsObject = new OtherTagged(); // name null, value 0, unknown 0
			Tagged read2 = (Tagged)kryo.readObject(new Input(write(nullsKryo, nullsObject)), (Class)OtherTagged.class);
			assertNull(read2.name); // Tag 3 is mismatched (expected 1) and read by readTag, which sets null.
			assertEquals(0, read2.value);
			assertNotNull(read2.nested); // Not in the data, keeps the created value.

			Kryo wrongType = new Kryo();
			wrongType.setDefaultSerializer(tagged(true, chunked));
			wrongType.register(WrongTypeTagged.class);
			WrongTypeTagged wrongObject = new WrongTypeTagged();
			wrongObject.value = "not an int";
			wrongObject.name = "name";
			byte[] wrongBytes = write(wrongType, wrongObject);
			kryo.register(WrongTypeTagged.class, nulls);
			if (chunked) {
				read2 = (Tagged)kryo.readObject(new Input(wrongBytes), (Class)WrongTypeTagged.class);
				assertEquals(5, read2.value); // Skipped, keeps the created value.
				assertEquals("name", read2.name);
			} else {
				KryoException ex = assertThrows(KryoException.class,
					() -> kryo.readObject(new Input(wrongBytes), (Class)WrongTypeTagged.class));
				assertTrue(ex.getMessage().contains("Read type is incompatible with the field type"), ex.getMessage());
			}
		}

		// The chunked encoding of Kryo 5 uses the cached fields.
		factory.getConfig().setChunkedEncoding(true);
		factory.getConfig().setLegacyChunks(true);
		kryo.register(Tagged.class, new TaggedFieldSerializer(kryo, Tagged.class, factory.getConfig()));
		assertNull(((FieldSerializer)kryo.getSerializer(Tagged.class)).generated);
	}

	static private void writeTagged (Kryo kryo, Output output, int tag, boolean writeClass, Object value) {
		output.writeVarInt(tag, true);
		if (writeClass) kryo.writeClass(output, value.getClass());
		if (value instanceof Integer)
			output.writeVarInt((Integer)value, false);
		else
			output.writeString((String)value);
	}

	static private TaggedFieldSerializerFactory tagged (boolean readUnknownTagData, boolean chunked) {
		TaggedFieldSerializerFactory factory = new TaggedFieldSerializerFactory();
		factory.getConfig().setReadUnknownTagData(readUnknownTagData);
		factory.getConfig().setChunkedEncoding(chunked);
		return factory;
	}

	/** The field names header CompatibleFieldSerializer writes for Nested. */
	static private void writeNestedFieldNames (Output output) {
		output.writeVarInt(2, true);
		output.writeString("name");
		output.writeString("value");
	}

	static private CompatibleFieldSerializerFactory compatible (boolean readUnknownFieldData, boolean chunked) {
		CompatibleFieldSerializerFactory factory = new CompatibleFieldSerializerFactory();
		factory.getConfig().setReadUnknownFieldData(readUnknownFieldData);
		factory.getConfig().setChunkedEncoding(chunked);
		return factory;
	}

	private GeneratedFields assertGenerated (Class type) {
		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(type);
		assertNotNull(serializer.generated, "No code generated for: " + type);
		return serializer.generated;
	}

	static private byte[] write (Kryo kryo, Object object) {
		Output output = new Output(1024, -1);
		kryo.writeObject(output, object);
		return output.toBytes();
	}

	static private <T> T read (Kryo kryo, byte[] bytes, Class<T> type) {
		return kryo.readObject(new Input(bytes), type);
	}

	static public class AllKinds {
		int i;
		long l;
		double d;
		float f;
		boolean z;
		short s;
		char c;
		byte b;
		String string;
		Integer integer;
		Color color;
		int[] ints;
		Nested nested;
		Object polymorphic;
		List<Nested> list;
		Map<String, Nested> map;
		@Bind(serializer = DefaultSerializers.StringSerializer.class) String bound;

		static AllKinds create () {
			AllKinds o = new AllKinds();
			o.i = -1234;
			o.l = 1L << 40;
			o.d = 1.5;
			o.f = -2.5f;
			o.z = true;
			o.s = -3;
			o.c = 'x';
			o.b = 5;
			o.string = "string";
			o.integer = 99;
			o.color = Color.green;
			o.ints = new int[] {1, 2, 3};
			o.nested = new Nested();
			o.nested.value = 7;
			o.nested.name = "nested";
			o.polymorphic = new Nested();
			o.list = new ArrayList<>(Arrays.asList(new Nested(), o.nested));
			o.map = new HashMap<>();
			o.map.put("key", o.nested);
			o.bound = "bound";
			return o;
		}

		public boolean equals (Object obj) {
			AllKinds other = (AllKinds)obj;
			return i == other.i && l == other.l && d == other.d && f == other.f && z == other.z && s == other.s && c == other.c
				&& b == other.b && Objects.equals(string, other.string) && Objects.equals(integer, other.integer) && color == other.color
				&& Arrays.equals(ints, other.ints) && Objects.equals(nested, other.nested) && Objects.equals(polymorphic, other.polymorphic)
				&& Objects.equals(list, other.list) && Objects.equals(map, other.map) && Objects.equals(bound, other.bound);
		}
	}

	static public class Nested {
		int value;
		String name;

		public boolean equals (Object obj) {
			Nested other = (Nested)obj;
			return value == other.value && Objects.equals(name, other.name);
		}

		public int hashCode () {
			return value;
		}
	}

	static public class Strings {
		String a, b, c;

		public boolean equals (Object obj) {
			Strings other = (Strings)obj;
			return Objects.equals(a, other.a) && Objects.equals(b, other.b) && Objects.equals(c, other.c);
		}
	}

	static public class NotNullField {
		Nested nested;
		@NotNull String required;
	}

	static public class FinalField {
		final int value;
		final String name;

		public FinalField () {
			this(0, null);
		}

		public FinalField (int value, String name) {
			this.value = value;
			this.name = name;
		}

		public boolean equals (Object obj) {
			return value == ((FinalField)obj).value && Objects.equals(name, ((FinalField)obj).name);
		}
	}

	static public class ObjectValue {
		public Object value = "string";
	}

	static public class FinalNumberValue {
		public final Number value = null;
	}

	static public class DeniedFinalField implements java.io.Serializable {
		final int value;
		final String name;

		public DeniedFinalField () {
			this(0, null);
		}

		public DeniedFinalField (int value, String name) {
			this.value = value;
			this.name = name;
		}

		public boolean equals (Object obj) {
			return obj instanceof DeniedFinalField other && other.value == value && Objects.equals(other.name, name);
		}
	}

	static public class Tagged {
		@Tag(1) int value;
		@Tag(2) @Deprecated int old;
		@Tag(3) String name;
		@Tag(4) Nested nested;

		static Tagged create () {
			Tagged o = new Tagged();
			o.value = 5;
			o.old = 6;
			o.name = "name";
			o.nested = new Nested();
			o.nested.value = 7;
			return o;
		}

		public boolean equals (Object obj) {
			Tagged other = (Tagged)obj;
			return value == other.value && Objects.equals(name, other.name) && Objects.equals(nested, other.nested);
		}
	}

	/** Written as Tagged by another version: the same number of tags as Tagged writes, another order, one unknown. */
	static public class OtherTagged {
		@Tag(3) String name;
		@Tag(1) int value;
		@Tag(99) int unknown;
	}

	static public class Versioned {
		String name;
		@Since(1) Nested nested;
		int value;

		public boolean equals (Object obj) {
			Versioned other = (Versioned)obj;
			return value == other.value && Objects.equals(name, other.name) && Objects.equals(nested, other.nested);
		}
	}

	/** Written by another version of Tagged: the same tags with other types. */
	static public class WrongTypeTagged {
		@Tag(1) String value;
		@Tag(3) String name;
		@Tag(4) Nested nested;
	}

	/** Written by another version of FinalField: the same field names with other types. */
	static public class FinalFieldOther {
		String value;
		int name;
	}

	static public class Box<T> {
		T value;
	}

	static public class StringBox extends Box<String> {
		public boolean equals (Object obj) {
			return Objects.equals(value, ((StringBox)obj).value);
		}
	}

	static public class FinalBox<T> {
		final T value;

		public FinalBox (T value) {
			this.value = value;
		}
	}

	static public class FinalStringBox extends FinalBox<String> {
		public FinalStringBox () {
			this(null);
		}

		public FinalStringBox (String value) {
			super(value);
		}

		public boolean equals (Object obj) {
			return Objects.equals(value, ((FinalStringBox)obj).value);
		}
	}

	/** More fields than a generated method has. */
	static public class Wide {
		static final int count = 200;
		int f0, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19, f20, f21, f22, f23, f24, f25, f26, f27, f28, f29, f30, f31, f32, f33, f34, f35, f36, f37, f38, f39, f40, f41, f42, f43, f44, f45, f46, f47, f48, f49, f50, f51, f52, f53, f54, f55, f56, f57, f58, f59, f60, f61, f62, f63, f64, f65, f66, f67, f68, f69, f70, f71, f72, f73, f74, f75, f76, f77, f78, f79, f80, f81, f82, f83, f84, f85, f86, f87, f88, f89, f90, f91, f92, f93, f94, f95, f96, f97, f98, f99, f100, f101, f102, f103, f104, f105, f106, f107, f108, f109, f110, f111, f112, f113, f114, f115, f116, f117, f118, f119, f120, f121, f122, f123, f124, f125, f126, f127, f128, f129, f130, f131, f132, f133, f134, f135, f136, f137, f138, f139, f140, f141, f142, f143, f144, f145, f146, f147, f148, f149, f150, f151, f152, f153, f154, f155, f156, f157, f158, f159, f160, f161, f162, f163, f164, f165, f166, f167, f168, f169, f170, f171, f172, f173, f174, f175, f176, f177, f178, f179, f180, f181, f182, f183, f184, f185, f186, f187, f188, f189, f190, f191, f192, f193, f194, f195, f196, f197, f198, f199;
	}

	public record Point (int x, int y) {
	}

	public enum Color {
		red, green, blue
	}
}
