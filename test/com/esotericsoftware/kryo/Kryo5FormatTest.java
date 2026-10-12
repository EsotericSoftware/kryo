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

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.EnumsFinalTest.BodyHolder;
import com.esotericsoftware.kryo.EnumsFinalTest.BodyOp;
import com.esotericsoftware.kryo.EnumsFinalTest.PlainHolder;
import com.esotericsoftware.kryo.EnumsFinalTest.PlainOp;
import com.esotericsoftware.kryo.Kryo.Format;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.DateSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.InetAddressSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.InetSocketAddressSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.TimestampSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.UUIDSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.ImmutableCollectionsSerializers;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.KryoSerializableSerializer;
import com.esotericsoftware.kryo.serializers.RecordSerializer;
import com.esotericsoftware.kryo.util.HashMapReferenceResolver;
import com.esotericsoftware.kryo.util.ListReferenceResolver;
import com.esotericsoftware.kryo.util.MapReferenceResolver;

import java.io.File;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** The Kryo 5 format, {@link Format#KRYO5}: Kryo, its default serializers and its reference resolvers behave like Kryo 5.
 * {@link Kryo5CompatibilityTest} reads data written by Kryo 5,
 * {@link com.esotericsoftware.kryo.serializers.Kryo5SerializersTest} covers the serializers. */
class Kryo5FormatTest {
	@Test
	void testEnumsWithConstantBodies () {
		// Enums with constant bodies are not final, so the class of each value is written, like in Kryo 5.
		Kryo kryo = new Kryo();
		kryo.setFormat(Format.KRYO5);
		assertFalse(kryo.isFinal(BodyOp.class));
		assertTrue(kryo.isFinal(PlainOp.class));

		byte[] plain = write(kryo(PlainOp.class, PlainHolder.class), new PlainHolder());
		byte[] body = write(kryo(BodyOp.class, BodyHolder.class), new BodyHolder());
		assertTrue(body.length > plain.length);
		BodyHolder read = kryo(BodyOp.class, BodyHolder.class).readObject(new Input(body), BodyHolder.class);
		assertSame(BodyOp.MINUS, read.op);
		assertEquals(List.of(BodyOp.PLUS, BodyOp.MINUS), read.list);
		assertArrayEquals(new BodyOp[] {BodyOp.MINUS}, read.array);
	}

	private Kryo kryo (Class op, Class holder) {
		Kryo kryo = new Kryo();
		kryo.setFormat(Format.KRYO5);
		kryo.register(op, 100);
		kryo.register(holder, 101);
		kryo.register(ArrayList.class, 102);
		kryo.register(op.arrayType(), 103);
		return kryo;
	}

	@Test
	void testRegisteredSerializerOfNewDefaultType () {
		// The serializers of the new default types were available in Kryo 5. If they were registered, they must still be used.
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
		kryo.register(ArrayList.class);
		kryo.setFormat(Format.KRYO5); // Unchanged.
		assertThrows(KryoException.class, () -> kryo.setFormat(Format.KRYO6));
		assertEquals(Format.KRYO5, kryo.getFormat());

		Kryo kryo6 = new Kryo();
		kryo6.getDefaultSerializer(ArrayList.class);
		assertThrows(KryoException.class, () -> kryo6.setFormat(Format.KRYO5));
		assertEquals(Format.KRYO6, kryo6.getFormat());

		// A serializer created directly decides when it is created whether to write the class of an enum field value, which
		// depends on isFinal.
		Kryo kryo7 = new Kryo();
		new FieldSerializer(kryo7, WithEnum.class);
		assertThrows(KryoException.class, () -> kryo7.setFormat(Format.KRYO5));
		Kryo kryo8 = new Kryo();
		assertTrue(kryo8.isFinal(BodyOp.class));
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
		kryo.setDefaultSerializer(JavaSerializer.class); // FieldSerializer can't access the fields of JDK classes without --add-opens.
		for (Class type : new Class[] {UUID.class, File.class, ByteBuffer.allocate(0).getClass(),
			ByteBuffer.allocateDirect(0).getClass()})
			assertSame(JavaSerializer.class, kryo.getDefaultSerializer(type).getClass(), type.getName());
		assertSame(DateSerializer.class, kryo.getDefaultSerializer(Timestamp.class).getClass());
		// Kryo 5 couldn't read the data its FieldSerializer wrote for these types, so the Kryo 6 serializers are used.
		for (Class type : new Class[] {Inet4Address.class, Inet6Address.class})
			assertSame(InetAddressSerializer.class, kryo.getDefaultSerializer(type).getClass(), type.getName());
		assertSame(InetSocketAddressSerializer.class, kryo.getDefaultSerializer(InetSocketAddress.class).getClass());
		for (Class type : new Class[] {ConcurrentSkipListSet.class, PriorityBlockingQueue.class, LinkedBlockingQueue.class,
			LinkedBlockingDeque.class})
			assertSame(CollectionSerializer.class, kryo.getDefaultSerializer(type).getClass(), type.getName());
		assertSame(RecordSerializer.class, kryo.getDefaultSerializer(Point.class).getClass());
		// Like Kryo 5, records come before the built-in default serializers, but after the added ones.
		assertSame(RecordSerializer.class, kryo.getDefaultSerializer(SerializablePoint.class).getClass());
		kryo.addDefaultSerializer(SerializablePoint.class, JavaSerializer.class);
		assertSame(JavaSerializer.class, kryo.getDefaultSerializer(SerializablePoint.class).getClass());

		kryo = new Kryo();
		assertSame(UUIDSerializer.class, kryo.getDefaultSerializer(UUID.class).getClass());
		assertSame(TimestampSerializer.class, kryo.getDefaultSerializer(Timestamp.class).getClass());
		assertSame(FieldSerializer.class, kryo.getDefaultSerializer(Point.class).getClass());
		assertSame(KryoSerializableSerializer.class, kryo.getDefaultSerializer(SerializablePoint.class).getClass());
	}

	record Point (int x, int y) {
	}

	record SerializablePoint (int x, int y) implements KryoSerializable {
		public void write (Kryo kryo, Output output) {
		}

		public void read (Kryo kryo, Input input) {
		}
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
		assertThrows(KryoException.class, () -> kryo.setFormat(Format.KRYO5)); // A serializer was created.
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

		// A String field decides when it is created whether it uses references, which depends on the format.
		Kryo kryo6 = new Kryo();
		new FieldSerializer(kryo6, WithString.class);
		assertThrows(KryoException.class, () -> kryo6.setFormat(Format.KRYO5));
		assertEquals(Format.KRYO6, kryo6.getFormat());

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

}
