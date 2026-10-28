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

import static com.esotericsoftware.kryo.serializers.ChunkedEncodingTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Kryo.Format;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.SerializerFactory.CompatibleFieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.TaggedFieldSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.ChunkedEncodingTest.Entity;
import com.esotericsoftware.kryo.serializers.ChunkedEncodingTest.Entity2;
import com.esotericsoftware.kryo.serializers.ChunkedEncodingTest.Entity3;
import com.esotericsoftware.kryo.serializers.ChunkedEncodingTest.Entity3Serializer;
import com.esotericsoftware.kryo.serializers.ChunkedEncodingTest.FailingEntity;
import com.esotericsoftware.kryo.serializers.ChunkedEncodingTest.Outer1;
import com.esotericsoftware.kryo.serializers.ChunkedEncodingTest.Outer2;
import com.esotericsoftware.kryo.serializers.ChunkedEncodingTest.RecordEntity;
import com.esotericsoftware.kryo.serializers.ChunkedEncodingTest.TaggedEntity;
import com.esotericsoftware.kryo.serializers.ChunkedEncodingTest.TaggedEntity2;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer.CompatibleFieldSerializerConfig;
import com.esotericsoftware.kryo.serializers.FieldSerializerTest.BindWithNotNull;
import com.esotericsoftware.kryo.serializers.FieldSerializerTest.FieldAccessData;
import com.esotericsoftware.kryo.serializers.SyntheticFieldsTest.Outer;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.TaggedFieldSerializerConfig;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/** Kryo's serializers with the Kryo 5 format, {@link Format#KRYO5}: MapSerializer, the chunked encoding, the generic types of
 * fields, code generation and synthetic fields. {@link com.esotericsoftware.kryo.Kryo5FormatTest} covers Kryo, the default
 * serializers and the reference resolvers. */
class Kryo5SerializersTest extends KryoTestCase {
	@Test
	void testMapSerializer () {
		// The class of each key and value is written, Kryo 6 writes it once.
		kryo.setFormat(Format.KRYO5);
		kryo.register(LinkedHashMap.class);
		LinkedHashMap map = new LinkedHashMap();
		map.put("a", 1);
		map.put("b", 2);
		roundTrip(12, map);
	}

	@Test
	void testNegativeCollectionAndMapSize () {
		// A corrupt negative size throws a KryoException. The size of a map has no flag in the Kryo 5 format.
		kryo.setFormat(Format.KRYO5);
		kryo.register(ArrayList.class);
		kryo.register(TreeSet.class);
		kryo.register(HashMap.class);
		kryo.register(TreeMap.class);
		for (int length : new int[] {-1, Integer.MIN_VALUE}) { // Size + 1.
			for (Class type : new Class[] {ArrayList.class, TreeSet.class, HashMap.class, TreeMap.class}) {
				Output output = new Output(16);
				if (Map.class.isAssignableFrom(type))
					output.writeVarInt(length, true);
				else
					output.writeVarIntFlag(false, length, true);
				byte[] bytes = output.toBytes();
				for (Input input : new Input[] {new Input(bytes), new Input(new ByteArrayInputStream(bytes))}) {
					KryoException ex = assertThrows(KryoException.class, () -> kryo.readObject(input, type));
					assertTrue(ex.getMessage().startsWith("Declared size is negative: " + (length - 1L)), ex.getMessage());
				}
			}
		}
	}

	@Test
	void testMapNullMarkers () {
		// The key and value serializers are known from the generic type of the field. Keys and values are written without a null
		// marker if the map contains no null key or value, which is written with the size.
		for (boolean references : new boolean[] {false, true}) {
			for (int size : new int[] {3, 100, 200}) {
				TypedMap object = new TypedMap();
				for (int i = 0; i < size; i++)
					object.map.put("key" + i, i);
				byte[] bytes = writeTypedMap(object, references, Format.KRYO6);
				// The format of Kryo 5 has a null marker for each value, and for each key with references.
				int markers = references ? size * 2 : size;
				// The flag takes one bit of the first byte of the size, which then needs a second byte from 63 entries instead of
				// from 127 entries.
				int flag = size >= 63 && size < 127 ? 1 : 0;
				assertEquals(writeTypedMap(object, references, Format.KRYO5).length - markers + flag, bytes.length);

				for (Format format : Format.values()) {
					object.map.put("null", null);
					writeTypedMap(object, references, format);
					object.map.remove("null");
					object.map.put(null, 5);
					writeTypedMap(object, references, format);
					object.map.remove(null);
				}
			}
		}
	}

	/** Returns the bytes after checking that they are read as the same map. */
	private byte[] writeTypedMap (TypedMap object, boolean references, Format format) {
		Kryo kryo = new Kryo();
		kryo.setFormat(format);
		kryo.setReferences(references);
		kryo.register(TypedMap.class);
		kryo.register(HashMap.class);
		Output output = new Output(64, -1);
		kryo.writeObject(output, object);
		assertEquals(object.map, kryo.readObject(new Input(output.toBytes()), TypedMap.class).map);
		return output.toBytes();
	}

	public static class TypedMap {
		public HashMap<String, Integer> map = new HashMap();
	}

	@Test
	void testChunkedChangedFieldType () {
		// Without readUnknownFieldData, the class of a field with a final type is not written. The field names are identified by
		// their position, so the field is read as the new type.
		Kryo kryo = new Kryo();
		kryo.setFormat(Format.KRYO5);
		CompatibleFieldSerializerConfig config = new CompatibleFieldSerializerConfig();
		config.setChunkedEncoding(true);
		config.setReadUnknownFieldData(false);
		kryo.setDefaultSerializer(new CompatibleFieldSerializerFactory(config));
		kryo.setRegistrationRequired(false);
		byte[] bytes = write(kryo, new Outer1());
		assertEquals(5, kryo.readObject(new Input(bytes), Outer2.class).inner.a);
	}

	@Test
	void testTaggedRemovedField () {
		// The tags are written outside the chunks.
		TaggedEntity entity = new TaggedEntity();
		entity.b = new TaggedEntity2();
		entity.b.x = new Entity3(5);
		entity.c = new Entity3(10);
		byte[] bytes = write(taggedKryo(), entity);
		TaggedEntity read = taggedKryo().readObject(new Input(bytes), TaggedEntity.class);
		assertEquals(5, read.b.x.a);
		assertEquals(10, read.c.a);
		assertNull(read.d);

		Kryo reader = taggedKryo();
		((TaggedFieldSerializer)reader.getSerializer(TaggedEntity.class)).removeField("b");
		read = reader.readObject(new Input(bytes), TaggedEntity.class);
		assertNull(read.b);
		assertEquals(10, read.c.a);
	}

	@Test
	void testChunkedCreate () {
		// A subclass writes data for create before the object, which comes before the first chunk.
		Kryo kryo = compatibleKryo(true, false);
		kryo.register(Entity3.class, new Entity3Serializer(kryo), 21);
		byte[] bytes = write(kryo, new Entity3(5));
		Kryo reader = compatibleKryo(true, false);
		reader.register(Entity3.class, new Entity3Serializer(reader), 21);
		Entity3 read = reader.readObject(new Input(bytes), Entity3.class);
		assertEquals(5, read.a);
		assertEquals("s5", read.s);
	}

	@Test
	void testSkippedChunk () {
		// With the chunked encoding of Kryo 5, the class name of Entity3 is lost with the skipped chunk.
		byte[] bytes = removeEntity2(write(compatibleKryo(false, false), entity(false)));
		Entity read = compatibleKryo(false, false).readObject(new Input(bytes), Entity.class);
		assertNull(read.c);
	}

	@Test
	void testChunkedReferences () {
		// The chunked encoding of Kryo 5 keeps the references of the fields.
		Kryo writer = new Kryo();
		writer.setFormat(Format.KRYO5);
		CompatibleFieldSerializerConfig config = new CompatibleFieldSerializerConfig();
		config.setChunkedEncoding(true);
		writer.setDefaultSerializer(new CompatibleFieldSerializerFactory(config));
		writer.setReferences(true);
		writer.register(Entity.class, 20);
		writer.register(Entity3.class, 21);
		writer.register(Entity2.class, 22);
		Entity entity = entity(true);
		byte[] bytes = write(writer, entity);

		Kryo reader = new Kryo();
		reader.setFormat(Format.KRYO5); // Before registering, which creates the serializers.
		config = new CompatibleFieldSerializerConfig();
		config.setChunkedEncoding(true);
		reader.setDefaultSerializer(new CompatibleFieldSerializerFactory(config));
		reader.setReferences(true);
		reader.register(Entity.class, 20);
		reader.register(Entity3.class, 21);
		reader.register(Entity2.class, 22);
		Entity read = reader.readObject(new Input(bytes), Entity.class);
		assertEquals(5, read.b.x.a);
		assertSame(read.b.x, read.d);
	}

	@Test
	void testUnknownGenericField () {
		// The class of the elements is not written, so the data of a removed field can't be read. It is skipped.
		for (boolean references : new boolean[] {false, true}) {
			Kryo writer = kryo5(references);
			writer.register(GenericEntity.class, 30);
			GenericEntity entity = new GenericEntity();
			entity.a = new ArrayList<>(List.of("a", "b"));
			entity.b = new HashMap<>(Map.of(1, "b"));
			entity.z = "z";
			byte[] bytes = write(writer, entity);

			Kryo reader = kryo5(references);
			reader.register(GenericEntity2.class, 30);
			assertEquals("z", reader.readObject(new Input(bytes), GenericEntity2.class).z);

			TaggedFieldSerializerConfig config = new TaggedFieldSerializerConfig();
			config.setChunkedEncoding(true);
			config.setReadUnknownTagData(true);
			writer.setDefaultSerializer(new TaggedFieldSerializerFactory(config));
			writer.register(TaggedGenericEntity.class, 33);
			TaggedGenericEntity tagged = new TaggedGenericEntity();
			tagged.a = entity.a;
			tagged.b = entity.b;
			tagged.z = "z";
			byte[] taggedBytes = write(writer, tagged);

			reader.setDefaultSerializer(new TaggedFieldSerializerFactory(config));
			reader.register(TaggedGenericEntity2.class, 33);
			assertEquals("z", reader.readObject(new Input(taggedBytes), TaggedGenericEntity2.class).z);
		}
	}

	private Kryo kryo5 (boolean references) {
		Kryo kryo = compatibleKryo(true, references);
		kryo.register(ArrayList.class, 31);
		kryo.register(HashMap.class, 32);
		return kryo;
	}

	@Test
	void testTimeZoneWithUnknownId () {
		// Written like Kryo 5 did, and read as GMT, instead of throwing.
		kryo.setFormat(Format.KRYO5);
		kryo.register(TimeZone.class);
		kryo.register(SimpleTimeZone.class);
		TimeZone unknown = new SimpleTimeZone(3600000, "Custom/Zone");
		Output output = new Output(64);
		kryo.writeObject(output, unknown);
		assertEquals("GMT", kryo.readObject(new Input(output.toBytes()), TimeZone.class).getID());
		kryo.setRegistrationRequired(false);
		output = new Output(64);
		kryo.writeClassAndObject(output, Calendar.getInstance(unknown));
		assertEquals("GMT", ((Calendar)kryo.readClassAndObject(new Input(output.toBytes()))).getTimeZone().getID());
	}

	@Test
	void testChunkedMaxArraySize () {
		// The chunked input the field values are read from has the maxArraySize of the outer input.
		kryo.setFormat(Format.KRYO5);
		kryo.register(byte[].class);
		CompatibleFieldSerializerConfig compatible = new CompatibleFieldSerializerConfig();
		compatible.setChunkedEncoding(true);
		kryo.register(ArrayHolder.class, new CompatibleFieldSerializer(kryo, ArrayHolder.class, compatible));
		TaggedFieldSerializerConfig tagged = new TaggedFieldSerializerConfig();
		tagged.setChunkedEncoding(true);
		kryo.register(TaggedArrayHolder.class, new TaggedFieldSerializer(kryo, TaggedArrayHolder.class, tagged));
		ArrayHolder holder = new ArrayHolder();
		holder.value = new byte[11];
		TaggedArrayHolder taggedHolder = new TaggedArrayHolder();
		taggedHolder.value = new byte[11];
		for (Object object : new Object[] {holder, taggedHolder}) {
			Output output = new Output(64);
			kryo.writeObject(output, object);
			Input input = new Input(output.toBytes());
			input.setMaxArraySize(11);
			kryo.readObject(input, object.getClass());
			Input limited = new Input(output.toBytes());
			limited.setMaxArraySize(10);
			KryoException ex = assertThrows(KryoException.class, () -> kryo.readObject(limited, object.getClass()));
			assertTrue(ex.getMessage().contains("maxArraySize"), ex.getMessage());
		}
	}

	@Test
	void testNoGeneratedCodeWithChunks () {
		// The chunked encoding of Kryo 5 uses the cached fields, without chunks the generated code is used.
		assumeTrue(CachedFields.codeGeneration, "Code generation is not supported on this platform.");
		for (boolean chunked : new boolean[] {true, false}) {
			Kryo kryo = new Kryo();
			kryo.setFormat(Format.KRYO5);
			CompatibleFieldSerializerFactory compatible = new CompatibleFieldSerializerFactory();
			compatible.getConfig().setCodeGeneration(true);
			compatible.getConfig().setChunkedEncoding(chunked);
			kryo.register(Entity3.class, new CompatibleFieldSerializer(kryo, Entity3.class, compatible.getConfig()));
			TaggedFieldSerializerFactory tagged = new TaggedFieldSerializerFactory();
			tagged.getConfig().setCodeGeneration(true);
			tagged.getConfig().setChunkedEncoding(chunked);
			kryo.register(TaggedEntity.class, new TaggedFieldSerializer(kryo, TaggedEntity.class, tagged.getConfig()));
			for (Class type : new Class[] {Entity3.class, TaggedEntity.class}) {
				GeneratedFields generated = ((FieldSerializer)kryo.getSerializer(type)).generated();
				if (chunked)
					assertNull(generated, type.getName());
				else
					assertNotNull(generated, type.getName());
			}
		}
	}

	@Test
	void testBindAndNotNull () {
		// Like Kryo 5 with Unsafe or ReflectASM, @Bind and @NotNull are ignored for String fields without references, and @NotNull
		// is ignored next to @Bind.
		Kryo kryo = new Kryo();
		kryo.setFormat(Format.KRYO5);
		kryo.setReferences(false);
		kryo.register(FieldAccessData.class);
		kryo.register(BindWithNotNull.class);
		kryo.register(ArrayList.class);
		FieldAccessData object = new FieldAccessData("string", 5);
		object.notNull = null;
		FieldAccessData read = roundTrip(kryo, object);
		assertEquals("string", read.bound); // The serializer is ignored.
		assertNull(read.notNull);

		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(BindWithNotNull.class);
		assertTrue(serializer.getField("notNull").getCanBeNull());
		BindWithNotNull bindWithNotNull = new BindWithNotNull();
		bindWithNotNull.notNull = null;
		assertEquals(bindWithNotNull, roundTrip(kryo, bindWithNotNull));

		// With references, Kryo 5 applied the annotations to String fields.
		Kryo references = new Kryo();
		references.setFormat(Format.KRYO5);
		references.setReferences(true);
		references.register(FieldAccessData.class);
		references.register(ArrayList.class);
		assertEquals("STRING", roundTrip(references, new FieldAccessData("string", 5)).bound);
		assertThrows(KryoException.class, () -> references.writeObject(new Output(64, -1), object));
	}

	private <T> T roundTrip (Kryo kryo, T object) {
		Output output = new Output(64, -1);
		kryo.writeObject(output, object);
		return (T)kryo.readObject(new Input(output.toBytes()), object.getClass());
	}

	@Test
	void testSyntheticFields () {
		// Like Kryo 5, the outer instance of an inner class is not serialized.
		Kryo kryo = SyntheticFieldsTest.newKryo(null);
		kryo.setFormat(Format.KRYO5);
		Outer outer = new Outer("outer");
		Outer.Member member = outer.new Member();
		assertNull(SyntheticFieldsTest.roundTrip(kryo, member).outer());
	}

	private Kryo compatibleKryo (boolean registration, boolean references) {
		Kryo kryo = new Kryo();
		kryo.setFormat(Format.KRYO5);
		CompatibleFieldSerializerConfig config = new CompatibleFieldSerializerConfig();
		config.setChunkedEncoding(true);
		kryo.setDefaultSerializer(new CompatibleFieldSerializerFactory(config));
		kryo.setReferences(references);
		kryo.setRegistrationRequired(registration);
		if (registration) {
			kryo.register(Entity.class, 20);
			kryo.register(Entity3.class, 21);
			kryo.register(Entity2.class, 22);
			kryo.register(RecordEntity.class, 23);
			kryo.register(FailingEntity.class, 24);
		}
		return kryo;
	}

	private Kryo taggedKryo () {
		Kryo kryo = new Kryo();
		kryo.setFormat(Format.KRYO5);
		TaggedFieldSerializerConfig config = new TaggedFieldSerializerConfig();
		config.setChunkedEncoding(true);
		config.setReadUnknownTagData(true);
		kryo.setDefaultSerializer(new TaggedFieldSerializerFactory(config));
		kryo.setReferences(true);
		kryo.setRegistrationRequired(false);
		return kryo;
	}

	public static class GenericEntity {
		public ArrayList<String> a;
		public HashMap<Integer, String> b;
		public String z;
	}

	public static class GenericEntity2 {
		public String z;
	}

	public static class TaggedGenericEntity {
		@Tag(1) public ArrayList<String> a;
		@Tag(2) public HashMap<Integer, String> b;
		@Tag(3) public String z;
	}

	public static class TaggedGenericEntity2 {
		@Tag(3) public String z;
	}

	public static class ArrayHolder {
		public Object value;
	}

	public static class TaggedArrayHolder {
		@Tag(1) public Object value;
	}
}
