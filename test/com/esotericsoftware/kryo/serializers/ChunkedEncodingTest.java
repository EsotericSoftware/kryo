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
import com.esotericsoftware.kryo.SerializerFactory.CompatibleFieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.TaggedFieldSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer.CompatibleFieldSerializerConfig;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.TaggedFieldSerializerConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/** Skipping a chunk, eg because the class of a removed field no longer exists, must not break the rest of the object graph
 * (#1247). */
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
		Entity read = compatibleKryo(false, false, false).readObject(new Input(bytes), Entity.class);
		assertEquals(5, read.b.x.a);
		assertEquals(10, read.c.a);
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
		Kryo kryo = new Kryo();
		TaggedFieldSerializerConfig config = new TaggedFieldSerializerConfig();
		config.setChunkedEncoding(true);
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

	public static class TaggedEntity {
		@Tag(1) public TaggedEntity2 b;
		@Tag(2) public Entity3 c;
		@Tag(3) public Entity3 d;
		@Tag(4) public Entity3 e;
	}

	public static class TaggedEntity2 {
		@Tag(1) public Entity3 x;
	}
}
