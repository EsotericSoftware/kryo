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

package com.esotericsoftware.kryo;

import static com.esotericsoftware.kryo.ReflectionAssert.*;
import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.Kryo.Format;
import com.esotericsoftware.kryo.Kryo5TestData.TestData;
import com.esotericsoftware.kryo.Kryo5TestData.TestDataJava8;
import com.esotericsoftware.kryo.SerializerFactory.TaggedFieldSerializerFactory;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.DateSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.TimestampSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.UUIDSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.ImmutableCollectionsSerializers;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializer;
import com.esotericsoftware.kryo.serializers.RecordSerializer;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.util.HashMapReferenceResolver;
import com.esotericsoftware.kryo.util.ListReferenceResolver;
import com.esotericsoftware.kryo.util.MapReferenceResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import com.esotericsoftware.kryo.util.StdInstantiatorStrategy;

/** Reads data written by Kryo 5 with {@link Format#KRYO5}: the data of {@link SerializationCompatTest} in Kryo 5 and the types
 * that have new default serializers in Kryo 6. */
class Kryo5CompatibilityTest {
	@Test
	void testKryo5TestData () throws IOException {
		for (TestData expected : testData()) {
			for (String variant : new String[] {"standard", "bytebuffer"})
				assertReflectionEquals(read(newKryo(true), expected.getClass(), variant), expected);
		}
	}

	@Test
	void testKryo5TestDataWithoutCompatibility () throws IOException {
		// Kryo 6 writes maps differently, so the Kryo 5 test data can't be read without the Kryo 5 settings.
		assertThrows(Throwable.class, () -> {
			TestData expected = new TestData();
			assertReflectionEquals(read(newKryo(false), TestData.class, "standard"), expected);
		});
	}

	@Test
	void testNewDefaultSerializers () throws IOException {
		NewDefaults actual = read(newKryo(true), NewDefaults.class, "standard");
		assertEquals(1234567890123L, actual.timestamp.getTime());
		assertEquals(123000000, actual.timestamp.getNanos()); // Kryo 5 used DateSerializer, which drops the nanoseconds.
		assertTrue(actual.atomicBoolean.get());
		assertEquals(42, actual.atomicInteger.get());
		assertEquals(4242, actual.atomicLong.get());
		assertEquals("ref", actual.atomicReference.get());

		assertThrows(Throwable.class, () -> read(newKryo(false), NewDefaults.class, "standard").timestamp.getTime());
	}

	@Test
	void testEnumsWithConstantBodies () throws IOException {
		// Kryo 5 wrote the class of each value, because enums with constant bodies weren't final.
		BodyEnums actual = read(newKryo(true), BodyEnums.class, "standard");
		assertSame(BodyOp.MINUS, actual.op);
		assertEquals(List.of(BodyOp.PLUS, BodyOp.MINUS), actual.list);
		assertArrayEquals(new BodyOp[] {BodyOp.MINUS, BodyOp.PLUS}, actual.array);
		assertSame(BodyOp.PLUS, actual.object);

		assertNotReadable(() -> {
			BodyEnums read = read(newKryo(false), BodyEnums.class, "standard");
			return read.op == BodyOp.MINUS && read.list.equals(List.of(BodyOp.PLUS, BodyOp.MINUS))
				&& Arrays.equals(read.array, new BodyOp[] {BodyOp.MINUS, BodyOp.PLUS}) && read.object == BodyOp.PLUS;
		});
	}

	// A record with a generic component and a generic field, written with CompatibleFieldSerializer set as a class.
	@Test
	void testGenericsWithCompatibleFieldSerializer () throws IOException {
		Consumer<Kryo> setup = kryo -> kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
		Generics actual = read(newKryo(true, setup), Generics.class, "standard");
		assertEquals(List.of("a", "b"), actual.record.values());
		assertEquals(List.of("c", "d"), actual.list);

		assertNotReadable(() -> {
			Generics generics = read(newKryo(false, setup), Generics.class, "standard");
			return generics.record.values().equals(List.of("a", "b")) && generics.list.equals(List.of("c", "d"));
		});
	}

	@Test
	void testGenericsWithTaggedFieldSerializer () throws IOException {
		Consumer<Kryo> setup = kryo -> {
			TaggedFieldSerializerFactory factory = new TaggedFieldSerializerFactory();
			factory.getConfig().setReadUnknownTagData(true);
			kryo.setDefaultSerializer(factory);
		};
		assertEquals(List.of("e", "f"), read(newKryo(true, setup), TaggedGenerics.class, "standard").list);

		assertNotReadable(() -> read(newKryo(false, setup), TaggedGenerics.class, "standard").list.equals(List.of("e", "f")));
	}

	// The serializers of the new default types were available in Kryo 5. If they were registered, they must still be used.
	@Test
	void testRegisteredSerializerOfNewDefaultType () {
		Kryo kryo = new Kryo();
		kryo.setFormat(Format.KRYO5);
		TimestampSerializer serializer = new TimestampSerializer();
		kryo.register(Timestamp.class, serializer);
		assertSame(serializer, kryo.getSerializer(Timestamp.class));

		Timestamp timestamp = new Timestamp(1234567890123L);
		timestamp.setNanos(123456789);
		Output output = new Output(64);
		kryo.writeObject(output, timestamp);
		assertEquals(timestamp, kryo.readObject(new Input(output.toBytes()), Timestamp.class)); // The nanoseconds are kept.
	}

	@Test
	void testFormatBeforeRegistration () {
		// The serializer of a class is chosen for the format when the class is registered, so the format can't change afterward.
		Kryo kryo = new Kryo();
		kryo.setFormat(Format.KRYO5);
		kryo.setFormat(Format.KRYO5);
		kryo.register(ArrayList.class);
		kryo.setFormat(Format.KRYO5); // Unchanged.
		assertThrows(KryoException.class, () -> kryo.setFormat(Format.KRYO6));
		assertEquals(Format.KRYO5, kryo.getFormat());

		Kryo kryo6 = new Kryo();
		kryo6.getDefaultSerializer(ArrayList.class);
		assertThrows(KryoException.class, () -> kryo6.setFormat(Format.KRYO5));
		assertEquals(Format.KRYO6, kryo6.getFormat());

		// A serializer created directly decides when it is created whether to write the class of an enum field value.
		Kryo kryo7 = new Kryo();
		kryo7.register(WithEnum.class, new FieldSerializer(kryo7, WithEnum.class));
		assertThrows(KryoException.class, () -> kryo7.setFormat(Format.KRYO5));
		Kryo kryo8 = new Kryo();
		new FieldSerializer(kryo8, WithEnum.class);
		assertThrows(KryoException.class, () -> kryo8.setFormat(Format.KRYO5));
	}

	static public class WithEnum {
		BodyOp op;
	}

	@Test
	void testDefaultSerializers () {
		// The types that have new default serializers in Kryo 6 use the serializers Kryo 5 used by default.
		Kryo kryo = new Kryo();
		kryo.setFormat(Format.KRYO5);
		kryo.setDefaultSerializer(JavaSerializer.class); // FieldSerializer can't access the fields of UUID without --add-opens.
		assertSame(JavaSerializer.class, kryo.getDefaultSerializer(UUID.class).getClass());
		assertSame(DateSerializer.class, kryo.getDefaultSerializer(Timestamp.class).getClass());
		assertSame(CollectionSerializer.class, kryo.getDefaultSerializer(LinkedBlockingQueue.class).getClass());
		assertSame(RecordSerializer.class, kryo.getDefaultSerializer(Point.class).getClass());

		kryo = new Kryo();
		assertSame(UUIDSerializer.class, kryo.getDefaultSerializer(UUID.class).getClass());
		assertSame(TimestampSerializer.class, kryo.getDefaultSerializer(Timestamp.class).getClass());
		assertSame(FieldSerializer.class, kryo.getDefaultSerializer(Point.class).getClass());
	}

	record Point (int x, int y) {
	}

	@Test
	void testMapSerializerFormat () {
		// MapSerializer writes the Kryo 5 format, also if it is registered explicitly: the class of each key and value.
		Kryo kryo5 = new Kryo();
		kryo5.setFormat(Format.KRYO5);
		kryo5.register(HashMap.class, new MapSerializer());
		Kryo kryo6 = new Kryo();
		kryo6.register(HashMap.class, new MapSerializer());

		HashMap map = new HashMap(Map.of("a", 1, "b", 2));
		byte[] bytes = write(kryo5, map);
		assertEquals(write(kryo6, map).length + 2, bytes.length);
		assertEquals(map, kryo5.readObject(new Input(bytes), HashMap.class));
		assertEquals(map, kryo6.readObject(new Input(write(kryo6, map)), HashMap.class));
	}

	@Test
	void testRegisteredImmutableCollections () {
		Kryo kryo = new Kryo();
		kryo.setFormat(Format.KRYO5);
		ImmutableCollectionsSerializers.registerSerializers(kryo);
		// Like Kryo 5, null elements of the same class are not supported.
		assertThrows(IllegalArgumentException.class,
			() -> kryo.writeClassAndObject(new Output(64), Stream.of(null, 1, null).toList()));
	}

	@Test
	void testReferenceResolver () {
		// Kryo's reference resolvers use references for strings, without enabling references.
		Kryo kryo = new Kryo();
		kryo.setFormat(Format.KRYO5);
		assertFalse(kryo.getReferences());
		kryo.setReferences(true);
		assertTrue(kryo.getReferenceResolver().useReferences(String.class));
		assertFalse(new Kryo(new MapReferenceResolver()).getReferenceResolver().useReferences(String.class));

		// Strings shared by identity are written as references, with each of Kryo's reference resolvers and their subclasses.
		for (ReferenceResolver resolver : new ReferenceResolver[] {new MapReferenceResolver(), new ListReferenceResolver(),
			new HashMapReferenceResolver(), new MapReferenceResolver() {
			}}) {
			kryo = new Kryo(resolver);
			kryo.setFormat(Format.KRYO5);
			kryo.setRegistrationRequired(false);
			assertTrue(resolver.useReferences(String.class));
			String shared = new String("shared");
			ArrayList<String> list = new ArrayList<>(List.of(shared, shared));
			ArrayList<String> read = kryo.readObject(new Input(write(kryo, list)), ArrayList.class);
			assertEquals(list, read);
			assertSame(read.get(0), read.get(1), resolver.getClass().getName());
		}

	}

	@Test
	void testStringReferencesAfterStringField () {
		// A String field decides when it is created whether it uses references, so that can't change afterward.
		Kryo kryo = new Kryo();
		kryo.register(WithString.class);
		kryo.setReferences(true); // Kryo's reference resolvers don't use references for strings.
		kryo.setReferences(false);
		kryo.setReferenceResolver(new ListReferenceResolver());
		assertThrows(KryoException.class, () -> kryo.setFormat(Format.KRYO5)); // Also because a serializer was created.
		assertThrows(KryoException.class, () -> kryo.setReferenceResolver(new MapReferenceResolver() {
			public boolean useReferences (Class type) {
				return true;
			}
		}));

		Kryo kryo5 = new Kryo();
		kryo5.setFormat(Format.KRYO5);
		kryo5.setReferences(true);
		kryo5.register(WithString.class);
		kryo5.setReferences(true);
		assertThrows(KryoException.class, () -> kryo5.setReferences(false));
		assertThrows(KryoException.class, () -> kryo5.setReferenceResolver(new MapReferenceResolver() {
			public boolean useReferences (Class type) {
				return type != String.class;
			}
		}));

		// A String field of a serializer created directly, without references: the format can change, but then enabling
		// references would change whether the field uses references.
		Kryo kryo6 = new Kryo();
		new FieldSerializer(kryo6, WithString.class);
		kryo6.setFormat(Format.KRYO5);
		assertThrows(KryoException.class, () -> kryo6.setReferences(true));

		// With references, the format can't change after the String field was created.
		Kryo kryo7 = new Kryo(new MapReferenceResolver());
		new FieldSerializer(kryo7, WithString.class);
		assertThrows(KryoException.class, () -> kryo7.setFormat(Format.KRYO5));
		assertEquals(Format.KRYO6, kryo7.getFormat());

		// The resolver has its Kryo instance when useReferences is called for the check.
		Kryo kryo8 = new Kryo();
		kryo8.register(WithString.class);
		kryo8.setReferenceResolver(new MapReferenceResolver() {
			public boolean useReferences (Class type) {
				return !this.kryo.isFinal(type); // Needs the Kryo instance set by setKryo.
			}
		});
		assertTrue(kryo8.getReferences());
	}

	private byte[] write (Kryo kryo, Object object) {
		Output output = new Output(256, -1);
		kryo.writeObject(output, object);
		return output.toBytes();
	}

	static public class WithString {
		String value;
	}

	/** Fails if the data is read correctly without the Kryo 5 settings. */
	private void assertNotReadable (Callable<Boolean> read) {
		boolean correct;
		try {
			correct = read.call();
		} catch (Throwable ex) {
			correct = false;
		}
		assertFalse(correct, "The Kryo 5 data was read correctly with the Kryo 6 format.");
	}

	private Kryo newKryo (boolean kryo5) {
		return newKryo(kryo5, kryo -> {
		});
	}

	private Kryo newKryo (boolean kryo5, Consumer<Kryo> setup) {
		// Same configuration as SerializationCompatTest.
		Kryo kryo = new Kryo(new Kryo5ClassResolver(), new MapReferenceResolver());
		if (kryo5) kryo.setFormat(Format.KRYO5); // Before registering, which creates the serializers.
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		kryo.setReferences(true);
		kryo.setRegistrationRequired(false);
		kryo.register(EnumSet.class);
		setup.accept(kryo);
		return kryo;
	}

	private <T> T read (Kryo kryo, Class<T> type, String variant) throws IOException {
		File dir = new File("test");
		if (!dir.exists()) dir = new File("../test");
		String name = type.getSimpleName().replace("Kryo5", "");
		File file = new File(dir, "resources/kryo5/" + name + "-" + variant + ".ser");
		try (Input input = variant.equals("bytebuffer") ? new ByteBufferInput(new FileInputStream(file))
			: new Input(new FileInputStream(file))) {
			return kryo.readObject(input, type);
		}
	}

	private TestData[] testData () {
		return new TestData[] {new TestData(), new TestDataJava8(), new Kryo5TestDataJava11(), new Kryo5TestDataJava17()};
	}

	/** Maps the class names of the Kryo 5 test data to the copies in this package. */
	static class Kryo5ClassResolver extends DefaultClassResolver {
		protected Class getTypeByName (String className) {
			Class type = super.getTypeByName(className);
			if (type != null) return type;
			// The classes that wrote Generics-standard.ser and TaggedGenerics-standard.ser were in gen2.Gen2.
			String renamed = className.replace("com.esotericsoftware.kryo.SerializationCompatTestData", Kryo5TestData.class.getName())
				.replace("com.esotericsoftware.kryo.TestDataJava", Kryo5TestData.class.getName() + "Java")
				.replace("gen2.Gen2$", Kryo5CompatibilityTest.class.getName() + "$");
			if (renamed.equals(className)) return null;
			try {
				return Class.forName(renamed);
			} catch (ClassNotFoundException ex) {
				return null;
			}
		}
	}

	public record GenericRecord(List<String> values) {
	}

	/** Written by Kryo 5 with CompatibleFieldSerializer to Generics-standard.ser. */
	public static class Generics {
		public GenericRecord record;
		public List<String> list;
	}

	/** Written by Kryo 5 with TaggedFieldSerializer and readUnknownTagData to TaggedGenerics-standard.ser. */
	public static class TaggedGenerics {
		@Tag(1) public List<String> list;
	}

	public enum BodyOp {
		PLUS {
			public String toString () {
				return "+";
			}
		},
		MINUS {
			public String toString () {
				return "-";
			}
		}
	}

	/** Enums with constant bodies, written by Kryo 5.7.0 to BodyEnums-standard.ser. */
	static class BodyEnums {
		public BodyOp op;
		public List<BodyOp> list;
		public BodyOp[] array;
		public Object object;
	}

	/** Types that have new default serializers in Kryo 6, written by Kryo 5 to NewDefaults-standard.ser. */
	static class NewDefaults {
		public Timestamp timestamp;
		public AtomicBoolean atomicBoolean;
		public AtomicInteger atomicInteger;
		public AtomicLong atomicLong;
		public AtomicReference<String> atomicReference;
	}
}
