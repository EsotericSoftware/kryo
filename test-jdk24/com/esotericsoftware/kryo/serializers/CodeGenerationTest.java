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

		// With references for strings, a String field is an object field: written with the StringSerializer and references.
		kryo.setReferenceResolver(new MapReferenceResolver() {
			public boolean useReferences (Class type) {
				return type == String.class || super.useReferences(type);
			}
		});
		kryo.register(Strings.class, new FieldSerializer(kryo, Strings.class, codeGeneration().getConfig()));
		assertGenerated(Strings.class);
		assertTrue(((FieldSerializer)kryo.getSerializer(Strings.class)).getField("a") instanceof ReflectField);
		roundTrip(14, object); // 1 class, 1 not null, 5 + 1 (reference) + 6 strings.
	}

	@Test
	void testNoHiddenFieldClasses () {
		// The generated code doesn't use the fields to write and read, so they don't get a hidden class each.
		kryo.register(AllKinds.class);
		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(AllKinds.class);
		assertNotNull(serializer.generated);
		if (CachedFields.hiddenFields && serializer.getFieldSerializerConfig().getFieldAccess() == FieldAccessType.VARHANDLE) {
			assertEquals("IntVarHandleField", serializer.getField("i").getClass().getSimpleName());
			assertEquals("VarHandleField", serializer.getField("nested").getClass().getSimpleName());
		}
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
	void testNotSupported () {
		// Records use the cached fields.
		kryo.register(Point.class);
		assertNull(((FieldSerializer)kryo.getSerializer(Point.class)).generated);
		roundTrip(3, new Point(1, 2));

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

	public record Point (int x, int y) {
	}

	public enum Color {
		red, green, blue
	}
}
