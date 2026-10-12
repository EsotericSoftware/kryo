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
import com.esotericsoftware.kryo.SerializationCompatTestData.TestData;
import com.esotericsoftware.kryo.SerializationCompatTestData.TestDataJava8;
import com.esotericsoftware.kryo.SerializerFactory.CompatibleFieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.TaggedFieldSerializerFactory;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.util.StdInstantiatorStrategy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests {@link Format#KRYO5} against the released Kryo 5, the kryo5 artifact relocated to com.esotericsoftware.kryo.kryo5: Kryo
 * 6 reads what Kryo 5 writes, Kryo 5 reads what Kryo 6 writes, and both write the same bytes. Kryo 5 needs packages of java.base
 * open, so the tagged tests run in the kryo5 surefire execution, which opens them. */
@org.junit.jupiter.api.Tag("kryo5")
class Kryo5RoundTripTest {
	/** The fields of the test data that Kryo 5 can't round trip itself, so they are null in the tests: its FieldSerializer can't
	 * write the unmodifiable and synchronized collections, the set from map, the key set view and EnumMap, and loses data of
	 * Timestamp (nanos), URI, Pattern (flags), InetAddress and InetSocketAddress. {@link #testKryo5Broken(Class)} checks the
	 * list. */
	static final Set<String> kryo5Broken = Set.of("_timestamp", "_uri", "_pattern", "_keySetView", "_inetAddress",
		"_inetSocketAddress", "_enumMap", "_emptyEnumMap", "_setFromMap", "_unmodifiableList", "_unmodifiableLinkedList",
		"_unmodifiableSet", "_unmodifiableSortedSet", "_unmodifiableNavigableSet", "_unmodifiableSortedMap",
		"_unmodifiableNavigableMap", "_synchronizedList", "_synchronizedSet", "_synchronizedSortedSet", "_synchronizedNavigableSet",
		"_synchronizedMap", "_synchronizedSortedMap", "_synchronizedNavigableMap");

	/** The fields whose class names differ, also with the Kryo 5 format: Kryo 6 writes the name of the registration's type (see
	 * DefaultClassResolver#writeName), so a time zone as java.util.TimeZone, each charset as java.nio.charset.Charset and an enum
	 * constant with a body as its enum. Kryo 5 writes the implementation class, eg sun.util.calendar.ZoneInfo, sun.nio.cs.UTF_8 or
	 * BodyEnum$2. Both versions read both names. */
	static final Set<String> classNameDiffers = Set.of("_timeZone", "_charsets", "_bodyEnum");

	@ParameterizedTest
	@ValueSource(classes = {TestData.class, TestDataJava8.class, TestDataJava11.class, TestDataJava17.class})
	void testKryo5Broken (Class<?> type) throws Exception {
		Set<String> broken = new TreeSet<>();
		for (Field field : fields(type)) {
			Object data = only(type, field);
			try {
				byte[] bytes = writeKryo5(data, false);
				assertReflectionEquals(newKryo5().readObject(new com.esotericsoftware.kryo.kryo5.io.Input(bytes), type), data);
			} catch (Throwable ex) {
				broken.add(field.getName());
			}
		}
		assertEquals(new TreeSet<>(kryo5Broken), broken);
	}

	@ParameterizedTest
	@ValueSource(classes = {TestData.class, TestDataJava8.class, TestDataJava11.class, TestDataJava17.class})
	void testRoundTrip (Class<?> type) throws Exception {
		Object data = type.getDeclaredConstructor().newInstance();
		for (Field field : fields(type))
			if (kryo5Broken.contains(field.getName())) field.set(data, null);
		for (boolean byteBuffer : new boolean[] {false, true}) {
			Input input = byteBuffer ? new ByteBufferInput(writeKryo5(data, byteBuffer)) : new Input(writeKryo5(data, byteBuffer));
			assertReflectionEquals(newKryo6().readObject(input, type), data);
			byte[] bytes = writeKryo6(data, byteBuffer);
			assertReflectionEquals(newKryo5().readObject(byteBuffer ? new com.esotericsoftware.kryo.kryo5.io.ByteBufferInput(bytes)
				: new com.esotericsoftware.kryo.kryo5.io.Input(bytes), type), data);
		}

		// Without the fields whose class names differ, the whole object is written to the same bytes.
		for (Field field : fields(type))
			if (classNameDiffers.contains(field.getName())) field.set(data, null);
		for (boolean byteBuffer : new boolean[] {false, true})
			assertArrayEquals(writeKryo5(data, byteBuffer), writeKryo6(data, byteBuffer));
	}

	/** The strict check: with all other fields null, each field is written to the same bytes. */
	@ParameterizedTest
	@ValueSource(classes = {TestData.class, TestDataJava8.class, TestDataJava11.class, TestDataJava17.class})
	void testSameBytes (Class<?> type) throws Exception {
		List<String> different = new ArrayList<>();
		for (Field field : fields(type)) {
			if (kryo5Broken.contains(field.getName())) continue;
			Object data = only(type, field);
			byte[] kryo5Bytes = writeKryo5(data, false), kryo6Bytes = writeKryo6(data, false);
			if (Arrays.equals(kryo5Bytes, kryo6Bytes) == classNameDiffers.contains(field.getName()))
				different.add(
					field.getName() + "\n  Kryo 5: " + Arrays.toString(kryo5Bytes) + "\n  Kryo 6: " + Arrays.toString(kryo6Bytes));
		}
		assertTrue(different.isEmpty(), "Same bytes not as expected:\n" + String.join("\n", different));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void testCompatibleFieldSerializer (boolean chunked) {
		roundTrip(kryo -> {
			CompatibleFieldSerializerFactory factory = new CompatibleFieldSerializerFactory();
			factory.getConfig().setChunkedEncoding(chunked);
			kryo.setDefaultSerializer(factory);
		}, kryo -> {
			var factory = new com.esotericsoftware.kryo.kryo5.SerializerFactory.CompatibleFieldSerializerFactory();
			factory.getConfig().setChunkedEncoding(chunked);
			kryo.setDefaultSerializer(factory);
		});
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void testTaggedFieldSerializer (boolean chunked) {
		roundTrip(kryo -> {
			TaggedFieldSerializerFactory factory = new TaggedFieldSerializerFactory();
			factory.getConfig().setChunkedEncoding(chunked);
			factory.getConfig().setReadUnknownTagData(true);
			kryo.setDefaultSerializer(factory);
		}, kryo -> {
			var factory = new com.esotericsoftware.kryo.kryo5.SerializerFactory.TaggedFieldSerializerFactory();
			factory.getConfig().setChunkedEncoding(chunked);
			factory.getConfig().setReadUnknownTagData(true);
			kryo.setDefaultSerializer(factory);
		});
	}

	/** Both directions and the same bytes, with the default serializer configured on each side. */
	private void roundTrip (Consumer<Kryo> kryo6Setup, Consumer<com.esotericsoftware.kryo.kryo5.Kryo> kryo5Setup) {
		Entity entity = new Entity();
		entity.child = new Entity();
		entity.child.name = "child";
		entity.same = entity.child; // A reference.
		entity.list = new ArrayList<>(List.of("a", "b"));
		entity.number = 5;
		com.esotericsoftware.kryo.kryo5.Kryo kryo5 = newKryo5();
		kryo5Setup.accept(kryo5);
		com.esotericsoftware.kryo.kryo5.io.Output kryo5Output = new com.esotericsoftware.kryo.kryo5.io.Output(1024, -1);
		kryo5.writeObject(kryo5Output, entity);
		Kryo kryo6 = newKryo6();
		kryo6Setup.accept(kryo6);
		Output kryo6Output = new Output(1024, -1);
		kryo6.writeObject(kryo6Output, entity);

		Entity read = kryo6.readObject(new Input(kryo5Output.toBytes()), Entity.class);
		assertReflectionEquals(read, entity);
		assertSame(read.child, read.same);
		read = kryo5.readObject(new com.esotericsoftware.kryo.kryo5.io.Input(kryo6Output.toBytes()), Entity.class);
		assertReflectionEquals(read, entity);
		assertSame(read.child, read.same);
		assertArrayEquals(kryo5Output.toBytes(), kryo6Output.toBytes());
	}

	/** The test data with only the field set and the other non-primitive fields null. */
	private Object only (Class<?> type, Field field) throws Exception {
		Object data = type.getDeclaredConstructor().newInstance();
		for (Field other : fields(type))
			if (!other.equals(field)) other.set(data, null);
		return data;
	}

	/** The non-static, non-primitive fields of the type and its superclasses. */
	private List<Field> fields (Class<?> type) {
		List<Field> fields = new ArrayList<>();
		for (Class<?> c = type; c != Object.class; c = c.getSuperclass()) {
			for (Field field : c.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
				field.setAccessible(true);
				fields.add(field);
			}
		}
		return fields;
	}

	private byte[] writeKryo5 (Object data, boolean byteBuffer) {
		com.esotericsoftware.kryo.kryo5.io.Output output = byteBuffer
			? new com.esotericsoftware.kryo.kryo5.io.ByteBufferOutput(4096, -1)
			: new com.esotericsoftware.kryo.kryo5.io.Output(4096, -1);
		newKryo5().writeObject(output, data);
		return output.toBytes();
	}

	private byte[] writeKryo6 (Object data, boolean byteBuffer) {
		Output output = byteBuffer ? new ByteBufferOutput(4096, -1) : new Output(4096, -1);
		newKryo6().writeObject(output, data);
		return output.toBytes();
	}

	/** Same configuration as {@link SerializationCompatTest}. */
	private com.esotericsoftware.kryo.kryo5.Kryo newKryo5 () {
		com.esotericsoftware.kryo.kryo5.Kryo kryo = new com.esotericsoftware.kryo.kryo5.Kryo();
		kryo.setInstantiatorStrategy(new com.esotericsoftware.kryo.kryo5.util.DefaultInstantiatorStrategy(
			new com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy()));
		kryo.setReferences(true);
		kryo.setRegistrationRequired(false);
		kryo.register(EnumSet.class);
		return kryo;
	}

	private Kryo newKryo6 () {
		Kryo kryo = new Kryo();
		kryo.setFormat(Format.KRYO5); // Before registering, which creates the serializers.
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		kryo.setReferences(true);
		kryo.setRegistrationRequired(false);
		kryo.register(EnumSet.class);
		return kryo;
	}

	/** Tagged for the TaggedFieldSerializer of both versions. */
	public static class Entity {
		@Tag(1)
		@com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag(1) public String name;
		@Tag(2)
		@com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag(2) public Entity child;
		@Tag(3)
		@com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag(3) public Entity same;
		@Tag(4)
		@com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag(4) public List<String> list;
		@Tag(5)
		@com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag(5) public int number;
	}
}
