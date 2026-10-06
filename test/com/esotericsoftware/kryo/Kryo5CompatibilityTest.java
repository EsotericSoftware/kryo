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

import com.esotericsoftware.kryo.Kryo5TestData.TestData;
import com.esotericsoftware.kryo.Kryo5TestData.TestDataJava8;
import com.esotericsoftware.kryo.SerializerFactory.CompatibleFieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.TaggedFieldSerializerFactory;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.TimestampSerializer;
import com.esotericsoftware.kryo.serializers.ImmutableCollectionsSerializers;
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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;

/** Reads data written by Kryo 5 with {@link Kryo5Compatibility}: the data of {@link SerializationCompatTest} in Kryo 5 and the
 * types that have new default serializers in Kryo 6. */
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
		TimestampSerializer serializer = new TimestampSerializer();
		kryo.register(Timestamp.class, serializer);
		Kryo5Compatibility.configure(kryo);
		assertSame(serializer, kryo.getSerializer(Timestamp.class));

		Timestamp timestamp = new Timestamp(1234567890123L);
		timestamp.setNanos(123456789);
		Output output = new Output(64);
		kryo.writeObject(output, timestamp);
		assertEquals(timestamp, kryo.readObject(new Input(output.toBytes()), Timestamp.class)); // The nanoseconds are kept.
	}

	@Test
	void testOptimizeGenerics () {
		Kryo kryo = new Kryo();
		CompatibleFieldSerializerFactory compatible = new CompatibleFieldSerializerFactory();
		kryo.setDefaultSerializer(compatible);
		Kryo5Compatibility.configure(kryo);
		assertTrue(compatible.getConfig().getOptimizeGenerics());

		kryo = new Kryo();
		TaggedFieldSerializerFactory tagged = new TaggedFieldSerializerFactory();
		kryo.setDefaultSerializer(tagged);
		Kryo5Compatibility.configure(kryo);
		assertTrue(tagged.getConfig().getOptimizeGenerics());
	}

	@Test
	void testRegisteredImmutableCollections () {
		Kryo kryo = new Kryo();
		ImmutableCollectionsSerializers.registerSerializers(kryo);
		Kryo5Compatibility.configure(kryo);
		// Like Kryo 5, null elements of the same class are not supported.
		assertThrows(IllegalArgumentException.class,
			() -> kryo.writeClassAndObject(new Output(64), Stream.of(null, 1, null).toList()));
	}

	@Test
	void testReferenceResolver () {
		// Kryo's reference resolvers are replaced by ones that use references for strings, without enabling references.
		Kryo kryo = new Kryo();
		Kryo5Compatibility.configure(kryo);
		assertFalse(kryo.getReferences());
		kryo.setReferences(true);
		assertTrue(kryo.getReferenceResolver().useReferences(String.class));

		kryo = new Kryo(new MapReferenceResolver(100));
		Kryo5Compatibility.configure(kryo);
		assertTrue(kryo.getReferenceResolver().useReferences(String.class));
		assertEquals(100, ((MapReferenceResolver)kryo.getReferenceResolver()).getMaximumCapacity());

		// Strings shared by identity are written as references, with each of Kryo's reference resolvers.
		for (ReferenceResolver resolver : new ReferenceResolver[] {new MapReferenceResolver(), new ListReferenceResolver(),
			new HashMapReferenceResolver()}) {
			kryo = new Kryo(resolver);
			kryo.setRegistrationRequired(false);
			Kryo5Compatibility.configure(kryo);
			assertNotSame(resolver, kryo.getReferenceResolver());
			assertTrue(kryo.getReferenceResolver().useReferences(String.class));
			String shared = new String("shared");
			ArrayList<String> list = new ArrayList<>(List.of(shared, shared));
			Output output = new Output(64);
			kryo.writeObject(output, list);
			ArrayList<String> read = kryo.readObject(new Input(output.toBytes()), ArrayList.class);
			assertEquals(list, read);
			assertSame(read.get(0), read.get(1), resolver.getClass().getSimpleName());
		}

		// Subclasses decide themselves.
		MapReferenceResolver custom = new MapReferenceResolver() {
		};
		kryo = new Kryo(custom);
		Kryo5Compatibility.configure(kryo);
		assertSame(custom, kryo.getReferenceResolver());
		assertFalse(kryo.getReferenceResolver().useReferences(String.class));
	}

	@Test
	void testStringReferencesAfterStringField () {
		// A String field decides when it is created whether it uses references, so that can't change afterward.
		Kryo kryo = new Kryo();
		kryo.register(WithString.class);
		kryo.setReferences(true); // Kryo's reference resolvers don't use references for strings.
		kryo.setReferences(false);
		kryo.setReferenceResolver(new ListReferenceResolver());
		assertThrows(KryoException.class, () -> Kryo5Compatibility.configure(kryo));
		assertThrows(KryoException.class, () -> kryo.setReferenceResolver(new MapReferenceResolver() {
			public boolean useReferences (Class type) {
				return true;
			}
		}));

		Kryo kryo5 = new Kryo();
		Kryo5Compatibility.configure(kryo5);
		kryo5.setReferences(true);
		kryo5.register(WithString.class);
		kryo5.setReferences(true);
		assertThrows(KryoException.class, () -> kryo5.setReferences(false));
		assertThrows(KryoException.class, () -> kryo5.setReferenceResolver(new MapReferenceResolver()));

		// Without references, String fields are written directly, so the resolver can change.
		Kryo kryo6 = new Kryo();
		kryo6.register(WithString.class);
		Kryo5Compatibility.configure(kryo6);
		assertThrows(KryoException.class, () -> kryo6.setReferences(true));

		// The resolver has its Kryo instance when useReferences is called for the check.
		Kryo kryo7 = new Kryo();
		kryo7.register(WithString.class);
		kryo7.setReferenceResolver(new MapReferenceResolver() {
			public boolean useReferences (Class type) {
				return !kryo.isFinal(type); // Needs the Kryo instance.
			}
		});
		assertTrue(kryo7.getReferences());
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
		assertFalse(correct, "The Kryo 5 data was read correctly without Kryo5Compatibility.");
	}

	private Kryo newKryo (boolean kryo5) {
		return newKryo(kryo5, kryo -> {
		});
	}

	private Kryo newKryo (boolean kryo5, Consumer<Kryo> setup) {
		// Same configuration as SerializationCompatTest.
		Kryo kryo = new Kryo(new Kryo5ClassResolver(), new MapReferenceResolver());
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		kryo.setReferences(true);
		kryo.setRegistrationRequired(false);
		kryo.register(EnumSet.class);
		setup.accept(kryo);
		if (kryo5) Kryo5Compatibility.configure(kryo);
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
