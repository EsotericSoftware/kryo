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
import com.esotericsoftware.kryo.SerializerFactory.CompatibleFieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.TaggedFieldSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer.CompatibleFieldSerializerConfig;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.TaggedFieldSerializerConfig;

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
