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
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Kryo5Compatibility;
import com.esotericsoftware.kryo.SerializerFactory.BaseSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.CompatibleFieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.TaggedFieldSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer.CompatibleFieldSerializerConfig;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.TaggedFieldSerializerConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/** Skipping a chunk, eg because the class of a removed field no longer exists, must not break the rest of the object graph
 * (#1247). */
@SuppressWarnings("deprecation") // legacyChunks
class ChunkedEncodingTest {
	@Test
	void testUnregisteredClassNameInSkippedChunk () {
		Entity entity = entity(false);
		byte[] bytes = removeEntity2(write(compatibleKryo(false, false, false), entity));
		Entity read = compatibleKryo(false, false, false).readObject(new Input(bytes), Entity.class);
		assertEquals(10, read.c.a);
		assertEquals(15, read.d.a);
	}

	@Test
	void testFieldNamesInSkippedChunk () {
		Entity entity = entity(false);
		byte[] bytes = write(compatibleKryo(true, false, false), entity);
		Kryo reader = compatibleKryo(true, false, false);
		reader.getClassResolver().unregister(22); // Entity2 was removed.
		Entity read = reader.readObject(new Input(bytes), Entity.class);
		assertEquals(10, read.c.a);
		assertEquals("s10", read.c.s);
		assertEquals(15, read.d.a);
	}

	@Test
	void testReferencesInSkippedChunk () {
		Entity entity = entity(true);
		byte[] bytes = write(compatibleKryo(true, true, false), entity);
		Kryo reader = compatibleKryo(true, true, false);
		reader.getClassResolver().unregister(22);
		Entity read = reader.readObject(new Input(bytes), Entity.class);
		assertEquals(10, read.c.a);
		assertNull(read.d); // A reference to an object in the skipped chunk.
		assertSame(read.c, read.e); // A reference after the skipped chunk.
	}

	@Test
	void testTagged () {
		TaggedEntity entity = new TaggedEntity();
		entity.b = new TaggedEntity2();
		entity.b.x = new Entity3(5);
		entity.c = new Entity3(10);
		entity.d = entity.b.x;
		entity.e = entity.c;
		byte[] bytes = removeEntity2(write(taggedKryo(), entity), "TaggedEntity2", "TaggedEntityZ");
		TaggedEntity read = taggedKryo().readObject(new Input(bytes), TaggedEntity.class);
		assertEquals(10, read.c.a);
		assertNull(read.d);
		assertSame(read.c, read.e);
	}

	@Test
	void testNestedStream () {
		// DeflateSerializer writes Entity2 to its own stream, which starts a new scope with its own class names and field names.
		Entity entity = entity(true);
		Kryo writer = compatibleKryo(true, true, false);
		writer.register(Entity2.class, new DeflateSerializer(writer.getDefaultSerializer(Entity2.class)), 22);
		byte[] bytes = write(writer, entity);

		Kryo reader = compatibleKryo(true, true, false);
		reader.register(Entity2.class, new DeflateSerializer(reader.getDefaultSerializer(Entity2.class)), 22);
		Entity read = reader.readObject(new Input(bytes), Entity.class);
		assertEquals(5, read.b.x.a);
		assertSame(read.b.x, read.d);
		assertSame(read.c, read.e);

		reader = compatibleKryo(true, true, false);
		reader.getClassResolver().unregister(22);
		read = reader.readObject(new Input(bytes), Entity.class);
		assertEquals(10, read.c.a);
		assertNull(read.d);
		assertSame(read.c, read.e);
	}

	@Test
	void testNestedStreamClassName () {
		// The class name first written in the field names of a nested scope is deferred to the outer scope, which writes it before
		// the nested scope.
		for (boolean references : new boolean[] {false, true}) {
			NestedOuter outer = new NestedOuter();
			outer.inner = new NestedInner();
			outer.inner.a = 42;
			outer.after = "after";
			byte[] bytes = write(nestedStreamKryo(references), outer);
			String text = new String(bytes, StandardCharsets.ISO_8859_1);
			assertTrue(text.contains("NestedInne"), "Class name not in the outer definitions."); // The last char has the high bit.
			NestedOuter read = nestedStreamKryo(references).readObject(new Input(bytes), NestedOuter.class);
			assertEquals(42, read.inner.a);
			assertEquals("after", read.after);
		}
	}

	/** Unregistered classes, NestedInner is written with DeflateSerializer and without its class. */
	private Kryo nestedStreamKryo (boolean references) {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setReferences(references);
		CompatibleFieldSerializerConfig config = new CompatibleFieldSerializerConfig();
		config.setChunkedEncoding(true);
		config.setReadUnknownFieldData(false);
		kryo.setDefaultSerializer(new CompatibleFieldSerializerFactory(config));
		kryo.addDefaultSerializer(NestedInner.class, new BaseSerializerFactory() {
			public Serializer newSerializer (Kryo kryo, Class type) {
				return new DeflateSerializer(new CompatibleFieldSerializer(kryo, type, config));
			}
		});
		return kryo;
	}

	@Test
	void testChangedFieldType () {
		// Without readUnknownFieldData, the class of a field with a final type is not written. If the type changed, the field
		// names are not found for the new class.
		for (boolean legacyChunks : new boolean[] {false, true}) {
			Kryo kryo = new Kryo();
			CompatibleFieldSerializerConfig config = new CompatibleFieldSerializerConfig();
			config.setChunkedEncoding(true);
			config.setLegacyChunks(legacyChunks);
			config.setReadUnknownFieldData(false);
			kryo.setDefaultSerializer(new CompatibleFieldSerializerFactory(config));
			kryo.setRegistrationRequired(false);
			byte[] bytes = write(kryo, new Outer1());
			if (legacyChunks) // The field names are identified by their position.
				assertEquals(5, kryo.readObject(new Input(bytes), Outer2.class).inner.a);
			else {
				KryoException ex = assertThrows(KryoException.class, () -> kryo.readObject(new Input(bytes), Outer2.class));
				assertTrue(ex.getMessage().contains("readUnknownFieldData"));
			}
		}
	}

	@Test
	void testTaggedLegacyChunks () {
		// The tags are written outside the chunks.
		for (boolean legacyChunks : new boolean[] {false, true}) {
			TaggedEntity entity = new TaggedEntity();
			entity.b = new TaggedEntity2();
			entity.b.x = new Entity3(5);
			entity.c = new Entity3(10);
			byte[] bytes = write(taggedKryo(legacyChunks), entity);
			TaggedEntity read = taggedKryo(legacyChunks).readObject(new Input(bytes), TaggedEntity.class);
			assertEquals(5, read.b.x.a);
			assertEquals(10, read.c.a);
			assertNull(read.d);

			Kryo reader = taggedKryo(legacyChunks);
			((TaggedFieldSerializer)reader.getSerializer(TaggedEntity.class)).removeField("b");
			read = reader.readObject(new Input(bytes), TaggedEntity.class);
			assertNull(read.b);
			assertEquals(10, read.c.a);
		}
	}

	@Test
	void testOtherSerializerInField () {
		// The class names and references in data written by other serializers in a skipped field are not lost.
		for (boolean deflate : new boolean[] {false, true}) {
			Kryo writer = compatibleKryo(false, true, false);
			Serializer serializer = new FieldSerializer(writer, Entity2.class);
			writer.register(Entity2.class, deflate ? new DeflateSerializer(serializer) : serializer, 22);
			Entity entity = entity(true); // Entity3 is unregistered and first written by FieldSerializer in the skipped field.
			byte[] bytes = write(writer, entity);

			Kryo reader = compatibleKryo(false, true, false); // Entity2 is not registered, so the field is skipped.
			Entity read = reader.readObject(new Input(bytes), Entity.class);
			assertNull(read.b);
			assertEquals(10, read.c.a);
			assertNull(read.d); // A reference to the object in the skipped field.
			assertSame(read.c, read.e);
		}
	}

	@Test
	void testCreate () {
		// A subclass writes data for create before the object, as described in the README. For the outermost object, this data
		// comes before the class names and field names of the scope.
		for (boolean legacyChunks : new boolean[] {false, true}) {
			Kryo kryo = compatibleKryo(true, false, legacyChunks);
			kryo.register(Entity3.class, new Entity3Serializer(kryo, legacyChunks), 21);
			byte[] bytes = write(kryo, new Entity3(5));
			Kryo reader = compatibleKryo(true, false, legacyChunks);
			reader.register(Entity3.class, new Entity3Serializer(reader, legacyChunks), 21);
			Entity3 read = reader.readObject(new Input(bytes), Entity3.class);
			assertEquals(5, read.a);
			assertEquals("s5", read.s);
		}
	}

	static class Entity3Serializer extends CompatibleFieldSerializer<Entity3> {
		Entity3Serializer (Kryo kryo, boolean legacyChunks) {
			super(kryo, Entity3.class);
			getCompatibleFieldSerializerConfig().setChunkedEncoding(true);
			getCompatibleFieldSerializerConfig().setLegacyChunks(legacyChunks);
		}

		public void write (Kryo kryo, Output output, Entity3 object) {
			output.writeInt(object.a);
			super.write(kryo, output, object);
		}

		protected Entity3 create (Kryo kryo, Input input, Class<? extends Entity3> type) {
			return new Entity3(input.readInt());
		}
	}

	@Test
	void testLegacyChunks () {
		// With the chunked encoding of Kryo 5, the class name of Entity3 is lost with the skipped chunk.
		byte[] bytes = removeEntity2(write(compatibleKryo(false, false, true), entity(false)));
		Entity read = compatibleKryo(false, false, true).readObject(new Input(bytes), Entity.class);
		assertNull(read.c);
	}

	@Test
	void testLargeFields () {
		// Field lengths that need more than the reserved byte, nested in another large field.
		for (boolean references : new boolean[] {false, true}) {
			Entity entity = entity(references);
			entity.b.x.s = "x".repeat(300);
			entity.c.s = "c".repeat(20000);
			byte[] bytes = write(compatibleKryo(true, references, false), entity);
			Entity read = compatibleKryo(true, references, false).readObject(new Input(bytes), Entity.class);
			assertEquals(entity.b.x.s, read.b.x.s);
			assertEquals(entity.c.s, read.c.s);

			Kryo reader = compatibleKryo(true, references, false);
			reader.getClassResolver().unregister(22);
			read = reader.readObject(new Input(bytes), Entity.class);
			assertEquals(entity.c.s, read.c.s);
		}
	}

	@Test
	void testStreams () {
		// Small buffers, so field lengths and skipping span buffer boundaries.
		Entity entity = entity(true);
		entity.b.x.s = "x".repeat(300);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Output output = new Output(outputStream, 16);
		compatibleKryo(false, true, false).writeObject(output, entity);
		output.flush();
		byte[] bytes = removeEntity2(outputStream.toByteArray());

		Entity read = compatibleKryo(false, true, false).readObject(new Input(new ByteArrayInputStream(bytes), 16), Entity.class);
		assertEquals(10, read.c.a);
		assertNull(read.d);
		assertSame(read.c, read.e);
	}

	@Test
	void testException () {
		// An exception while writing must not affect the next object graph.
		Kryo kryo = compatibleKryo(false, false, false);
		kryo.register(Failing.class, new Serializer<Failing>() {
			public void write (Kryo kryo, Output output, Failing object) {
				throw new KryoException("failed");
			}

			public Failing read (Kryo kryo, Input input, Class<? extends Failing> type) {
				return null;
			}
		});
		FailingEntity failing = new FailingEntity();
		failing.c = new Entity3(1);
		failing.failing = new Failing();
		assertThrows(KryoException.class, () -> write(kryo, failing));

		byte[] bytes = write(kryo, entity(false));
		// The field names collected before the exception are not written.
		assertArrayEquals(write(compatibleKryo(false, false, false), entity(false)), bytes);
		Entity read = compatibleKryo(false, false, false).readObject(new Input(bytes), Entity.class);
		assertEquals(5, read.b.x.a);
		assertEquals(10, read.c.a);
	}

	@Test
	void testExceptionThenRead () throws Exception {
		// Reading in the same object graph must not keep the scope left over by an exception while writing.
		Kryo kryo = compatibleKryo(false, false, false);
		kryo.setAutoReset(false);
		kryo.register(Failing.class, new Serializer<Failing>() {
			public void write (Kryo kryo, Output output, Failing object) {
				throw new KryoException("failed");
			}

			public Failing read (Kryo kryo, Input input, Class<? extends Failing> type) {
				return null;
			}
		});
		FailingEntity failing = new FailingEntity();
		failing.c = new Entity3(1);
		failing.failing = new Failing();
		assertThrows(KryoException.class, () -> write(kryo, failing));
		kryo.reset();

		kryo.readObject(new Input(write(compatibleKryo(false, false, false), entity(false))), Entity.class);
		byte[] bytes = write(kryo, entity(false));
		Field writeDepth = DefaultChunkedEncoding.class.getDeclaredField("writeDepth");
		writeDepth.setAccessible(true);
		assertEquals(0, writeDepth.getInt(DefaultChunkedEncoding.get(kryo)));
		assertEquals(10, compatibleKryo(false, false, false).readObject(new Input(bytes), Entity.class).c.a);
	}

	@Test
	void testMultipleObjects () {
		// Each element is written with the class names and field names first written in it.
		ArrayList<Entity> list = new ArrayList<>();
		list.add(entity(false));
		list.add(entity(false));
		Kryo writer = compatibleKryo(false, false, false);
		Output output = new Output(1024, -1);
		writer.writeClassAndObject(output, list);
		byte[] bytes = removeEntity2(output.toBytes());

		ArrayList<Entity> read = (ArrayList)compatibleKryo(false, false, false).readClassAndObject(new Input(bytes));
		for (Entity entity : read) {
			assertNull(entity.b);
			assertEquals(10, entity.c.a);
			assertEquals(15, entity.d.a);
		}
	}

	@Test
	void testRecord () {
		RecordEntity entity = new RecordEntity(new Entity2(), new Entity3(10));
		entity.b().x = new Entity3(5);
		byte[] bytes = write(compatibleKryo(true, false, false), entity);
		Kryo reader = compatibleKryo(true, false, false);
		reader.getClassResolver().unregister(22);
		RecordEntity read = reader.readObject(new Input(bytes), RecordEntity.class);
		assertNull(read.b());
		assertEquals(10, read.c().a);
		assertEquals("s10", read.c().s);
	}

	@Test
	void testKryo5Compatibility () {
		// Kryo5Compatibility enables the chunked encoding of Kryo 5.
		Kryo writer = compatibleKryo(true, true, true);
		CompatibleFieldSerializerConfig config = new CompatibleFieldSerializerConfig();
		config.setChunkedEncoding(true);
		config.setLegacyChunks(true);
		config.setOptimizeGenerics(true);
		writer.setDefaultSerializer(new CompatibleFieldSerializerFactory(config));
		Entity entity = entity(true);
		byte[] bytes = write(writer, entity);

		Kryo reader = new Kryo();
		config = new CompatibleFieldSerializerConfig();
		config.setChunkedEncoding(true);
		reader.setDefaultSerializer(new CompatibleFieldSerializerFactory(config));
		Kryo5Compatibility.configure(reader); // Before registering, which creates the serializers.
		reader.setReferences(true);
		reader.register(Entity.class, 20);
		reader.register(Entity3.class, 21);
		reader.register(Entity2.class, 22);
		Entity read = reader.readObject(new Input(bytes), Entity.class);
		assertEquals(5, read.b.x.a);
		assertSame(read.b.x, read.d);
	}

	@Test
	void testUnknownDataOptimizedGenerics () {
		// With optimizeGenerics, the class of the elements is not written, so the data of a removed field can't be read. It is
		// skipped.
		for (boolean references : new boolean[] {false, true}) {
			Kryo writer = optimizedKryo(references);
			writer.register(GenericEntity.class, 30);
			GenericEntity entity = new GenericEntity();
			entity.a = new ArrayList<>(List.of("a", "b"));
			entity.b = new HashMap<>(Map.of(1, "b"));
			entity.z = "z";
			byte[] bytes = write(writer, entity);

			Kryo reader = optimizedKryo(references);
			reader.register(GenericEntity2.class, 30);
			assertEquals("z", reader.readObject(new Input(bytes), GenericEntity2.class).z);

			TaggedFieldSerializerConfig config = new TaggedFieldSerializerConfig();
			config.setChunkedEncoding(true);
			config.setReadUnknownTagData(true);
			config.setOptimizeGenerics(true);
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

	private Kryo optimizedKryo (boolean references) {
		Kryo kryo = compatibleKryo(true, references, false);
		CompatibleFieldSerializerConfig config = new CompatibleFieldSerializerConfig();
		config.setChunkedEncoding(true);
		config.setOptimizeGenerics(true);
		kryo.setDefaultSerializer(new CompatibleFieldSerializerFactory(config));
		kryo.register(ArrayList.class, 31);
		kryo.register(HashMap.class, 32);
		return kryo;
	}

	@Test
	void testMisreadUnknownData () {
		// The class of a removed field was replaced by an incompatible class. Reading its data past the field or reading more
		// objects than it contains throws, so the reference IDs after the field are never wrong.
		for (boolean references : new boolean[] {false, true}) {
			Kryo writer = compatibleKryo(true, references, false);
			writer.register(MisreadEntity.class, 30);
			writer.register(MisreadInner.class, new FieldSerializer(writer, MisreadInner.class), 31);
			MisreadEntity entity = new MisreadEntity();
			entity.a = new MisreadInner();
			entity.z = "z";
			byte[] bytes = write(writer, entity);

			Kryo reader = compatibleKryo(true, references, false);
			reader.register(MisreadEntity2.class, 30);
			reader.register(MisreadInner2.class, new FieldSerializer(reader, MisreadInner2.class), 31);
			KryoException ex = assertThrows(KryoException.class, () -> reader.readObject(new Input(bytes), MisreadEntity2.class));
			assertTrue(ex.getMessage().startsWith("More data was read than the field contains"));
		}

		Kryo writer = compatibleKryo(true, true, false);
		writer.register(MisreadEntity.class, 30);
		writer.register(MisreadInner3.class, new FieldSerializer(writer, MisreadInner3.class), 31);
		writer.register(ArrayList.class, 32);
		MisreadEntity entity = new MisreadEntity();
		entity.a = new MisreadInner3();
		entity.d = new ArrayList<>(List.of("d"));
		entity.e = entity.d;
		byte[] bytes = write(writer, entity);

		Kryo reader = compatibleKryo(true, true, false);
		reader.register(MisreadEntity2.class, 30);
		reader.register(MisreadInner4.class, new FieldSerializer(reader, MisreadInner4.class), 31);
		reader.register(ArrayList.class, 32);
		reader.register(Phantom.class, new FieldSerializer(reader, Phantom.class), 33);
		KryoException ex = assertThrows(KryoException.class, () -> reader.readObject(new Input(bytes), MisreadEntity2.class));
		assertTrue(ex.getMessage().startsWith("More objects were read than the field contains"));
	}

	@Test
	void testExceptionInUnknownData () {
		// An exception while reading the data of a removed field, inside a nested object, must not affect the next object.
		Kryo writer = compatibleKryo(true, false, false);
		writer.register(FailingEntity.class, 30);
		writer.register(Failing.class, new Serializer<Failing>() {
			public void write (Kryo kryo, Output output, Failing object) {
			}

			public Failing read (Kryo kryo, Input input, Class<? extends Failing> type) {
				return null;
			}
		}, 31);
		writer.register(ArrayList.class, 32);
		writer.register(NestedEntity.class, 33);
		ArrayList<NestedEntity> list = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			NestedEntity entity = new NestedEntity();
			entity.a = new FailingEntity();
			entity.a.c = new Entity3(i);
			entity.a.failing = new Failing();
			entity.z = "z" + i;
			list.add(entity);
		}
		Output output = new Output(1024, -1);
		writer.writeObject(output, list);

		Kryo reader = compatibleKryo(true, false, false);
		reader.register(FailingEntity.class, 30);
		reader.register(Failing.class, new Serializer<Failing>() {
			public void write (Kryo kryo, Output output, Failing object) {
			}

			public Failing read (Kryo kryo, Input input, Class<? extends Failing> type) {
				throw new KryoException("failed");
			}
		}, 31);
		reader.register(ArrayList.class, 32);
		reader.register(NestedEntity2.class, 33);
		ArrayList<NestedEntity2> read = reader.readObject(new Input(output.toBytes()), ArrayList.class);
		assertEquals("z0", read.get(0).z);
		assertEquals("z1", read.get(1).z);
	}

	private Entity entity (boolean references) {
		Entity entity = new Entity();
		entity.b = new Entity2();
		entity.b.x = new Entity3(5); // Entity3 is first written in the chunk that is skipped.
		entity.c = new Entity3(10);
		entity.d = references ? entity.b.x : new Entity3(15);
		if (references) entity.e = entity.c;
		return entity;
	}

	private Kryo compatibleKryo (boolean registration, boolean references, boolean legacyChunks) {
		Kryo kryo = new Kryo();
		CompatibleFieldSerializerConfig config = new CompatibleFieldSerializerConfig();
		config.setChunkedEncoding(true);
		config.setLegacyChunks(legacyChunks);
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
		return taggedKryo(false);
	}

	private Kryo taggedKryo (boolean legacyChunks) {
		Kryo kryo = new Kryo();
		TaggedFieldSerializerConfig config = new TaggedFieldSerializerConfig();
		config.setChunkedEncoding(true);
		config.setLegacyChunks(legacyChunks);
		config.setReadUnknownTagData(true);
		kryo.setDefaultSerializer(new TaggedFieldSerializerFactory(config));
		kryo.setReferences(true);
		kryo.setRegistrationRequired(false);
		return kryo;
	}

	private byte[] write (Kryo kryo, Object object) {
		Output output = new Output(1024, -1);
		kryo.writeObject(output, object);
		return output.toBytes();
	}

	private byte[] removeEntity2 (byte[] bytes) {
		return removeEntity2(bytes, "Entity2", "EntityZ");
	}

	/** Replaces the class name so the class is not found when reading. Short class names are written as ASCII with the high bit
	 * set on the last character, long ones as UTF-8 with their length. */
	private byte[] removeEntity2 (byte[] bytes, String name, String replacement) {
		int count = replace(bytes, "$" + name, "$" + replacement, true) + replace(bytes, "$" + name, "$" + replacement, false);
		assertTrue(count > 0);
		return bytes;
	}

	private int replace (byte[] bytes, String name, String replacement, boolean highBit) {
		byte[] search = name.getBytes(StandardCharsets.US_ASCII), replace = replacement.getBytes(StandardCharsets.US_ASCII);
		if (highBit) {
			search[search.length - 1] |= 0x80;
			replace[replace.length - 1] |= 0x80;
		}
		int count = 0;
		outer:
		for (int i = 0; i <= bytes.length - search.length; i++) {
			for (int ii = 0; ii < search.length; ii++)
				if (bytes[i + ii] != search[ii]) continue outer;
			// Without high bit, the name must end here, so Entity2 doesn't match TaggedEntity2 when replacing the other.
			System.arraycopy(replace, 0, bytes, i, replace.length);
			count++;
		}
		return count;
	}

	public static class Entity {
		public Entity2 b;
		public Entity3 c, d, e;
	}

	public static class Entity2 {
		public Entity3 x;
	}

	public static class Entity3 { // Tags are used by TaggedFieldSerializer and ignored by CompatibleFieldSerializer.
		@Tag(1) public int a;
		@Tag(2) public String s;

		public Entity3 () {
		}

		public Entity3 (int a) {
			this.a = a;
			s = "s" + a;
		}
	}

	public record RecordEntity(Entity2 b, Entity3 c) {
	}

	public static class Failing {
	}

	public static class FailingEntity {
		public Entity3 c;
		public Failing failing;
	}

	public static final class Inner1 {
		public int a = 5;
	}

	public static final class Inner2 {
		public int a;
	}

	public static class NestedOuter {
		public NestedInner inner;
		public String after;
	}

	public static final class NestedInner {
		public int a;
	}

	public static class Outer1 {
		public Inner1 inner = new Inner1();
	}

	public static class Outer2 {
		public Inner2 inner;
	}

	public static class TaggedEntity {
		@Tag(1) public TaggedEntity2 b;
		@Tag(2) public Entity3 c;
		@Tag(3) public Entity3 d;
		@Tag(4) public Entity3 e;
	}

	public static class TaggedEntity2 {
		@Tag(1) public Entity3 x;
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

	public static class MisreadEntity {
		public Object a, d, e;
		public String z;
	}

	public static class MisreadEntity2 {
		public Object d, e;
		public String z;
	}

	public static class MisreadInner {
		public String a = "a";
	}

	public static class MisreadInner2 {
		public String a, b, c;
	}

	public static class MisreadInner3 {
		public int a = -1;
	}

	public static class MisreadInner4 {
		public Phantom a;
	}

	public static final class Phantom {
	}

	public static class NestedEntity {
		public FailingEntity a;
		public String z;
	}

	public static class NestedEntity2 {
		public String z;
	}
}
