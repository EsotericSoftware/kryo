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

import static com.esotericsoftware.kryo.util.Util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer.Since;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.serializers.FieldSerializer.FieldAccessType;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;

/** Final fields of serializable classes are set with the method handles of the JDK if available, also if final field mutation
 * is denied, eg with {@code --illegal-final-field-mutation=deny}. */
class FinalFieldsTest {
	@Test
	void testFinalFields () {
		List<BiFunction<Kryo, Class, Serializer>> serializers = List.of(FieldSerializer::new, CompatibleFieldSerializer::new,
			TaggedFieldSerializer::new, VersionFieldSerializer::new);
		// The detected mode, and the method handles also if setting final fields with reflection is allowed. Without the method
		// handles (Java < 24), final fields are always set with reflection.
		boolean detected = FinalFieldSetter.mutationDenied();
		try {
			for (boolean denied : detected ? new boolean[] {true} : new boolean[] {false, true}) {
				FinalFieldSetter.denied = denied;
				testFinalFields(serializers);
			}
		} finally {
			FinalFieldSetter.denied = detected;
		}
	}

	@Test
	void testMissingAndNullFields () {
		boolean detected = FinalFieldSetter.mutationDenied();
		try {
			for (boolean denied : detected ? new boolean[] {true} : new boolean[] {false, true}) {
				FinalFieldSetter.denied = denied;
				// A final field that is not in the data keeps the value set by the constructor.
				Kryo writer = new Kryo();
				CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(writer, Defaults.class);
				serializer.removeField("name");
				serializer.removeField("number");
				writer.register(Defaults.class, serializer);
				Output output = new Output(1024, -1);
				writer.writeObject(output, new Defaults("written", 1));
				Kryo reader = new Kryo();
				reader.register(Defaults.class, new CompatibleFieldSerializer(reader, Defaults.class));
				Defaults read = reader.readObject(new Input(output.toBytes()), Defaults.class);
				assertEquals("default", read.name);
				assertEquals(7, read.number);

				// A final field written as null is read as null.
				output.reset();
				reader.writeObject(output, new Defaults(null, 1));
				read = reader.readObject(new Input(output.toBytes()), Defaults.class);
				assertNull(read.name);
				assertEquals(1, read.number);
			}
		} finally {
			FinalFieldSetter.denied = detected;
		}
	}

	@Test
	void testHashSetCycle () {
		// A final field is set right after it is read, like with reflection, so the hash code of an object that is added to a
		// HashSet while its other fields are read uses the value.
		boolean detected = FinalFieldSetter.mutationDenied();
		try {
			for (boolean denied : detected ? new boolean[] {true} : new boolean[] {false, true}) {
				FinalFieldSetter.denied = denied;
				Kryo kryo = new Kryo();
				kryo.setReferences(true);
				kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
				kryo.register(HashNode.class);
				kryo.register(HashSet.class);
				HashNode a = new HashNode("a"), b = new HashNode("b");
				a.neighbors.add(b);
				b.neighbors.add(a);
				Output output = new Output(1024, -1);
				kryo.writeObject(output, a);
				for (HashNode read : List.of(kryo.readObject(new Input(output.toBytes()), HashNode.class), kryo.copy(a))) {
					HashNode readB = read.neighbors.iterator().next();
					assertEquals("b", readB.id);
					assertTrue(readB.neighbors.contains(read));
				}
			}
		} finally {
			FinalFieldSetter.denied = detected;
		}
	}

	@Test
	void testNotSerializable () {
		// Final fields that are not serializable fields of a serializable class are set with reflection.
		Boolean denied = FinalFieldSetter.denied;
		try {
			FinalFieldSetter.denied = true;
			Kryo kryo = new Kryo();
			for (Class type : new Class[] {NotSerializable.class, NotSerializableSuperclass.class, TransientField.class}) {
				FieldSerializer serializer = new FieldSerializer(kryo, type);
				serializer.getFieldSerializerConfig().setSerializeTransient(true);
				serializer.updateFields();
				assertNull(serializer.finalFieldSetter, type.getSimpleName());
			}
			// The method handles are available since Java 24.
			if (Runtime.version().feature() >= 24 && !isAndroid)
				assertNotNull(new FieldSerializer(kryo, Defaults.class).finalFieldSetter);
		} finally {
			FinalFieldSetter.denied = denied;
		}
	}

	@Test
	void testRemovedFields () {
		// The setter is created for the fields that remain after fields were removed, eg untagged fields by TaggedFieldSerializer.
		assumeTrue(Runtime.version().feature() >= 24 && !isAndroid);
		Boolean denied = FinalFieldSetter.denied;
		try {
			FinalFieldSetter.denied = true;
			Kryo kryo = new Kryo();
			assertNotNull(new TaggedFieldSerializer(kryo, TaggedSubclass.class).finalFieldSetter);
			FieldSerializer serializer = new FieldSerializer(kryo, NotSerializableSuperclass.class);
			assertNull(serializer.finalFieldSetter);
			serializer.removeField("a");
			assertNotNull(serializer.finalFieldSetter);
			serializer.removeField("b");
			assertNull(serializer.finalFieldSetter);
		} finally {
			FinalFieldSetter.denied = denied;
		}
	}

	@Test
	void testUnsafe () {
		// Unsafe sets final fields also if setting them with reflection is denied.
		assumeTrue(unsafe);
		Boolean denied = FinalFieldSetter.denied;
		try {
			FinalFieldSetter.denied = true;
			Kryo kryo = new Kryo();
			FieldSerializer serializer = new FieldSerializer(kryo, Defaults.class);
			serializer.getFieldSerializerConfig().setFieldAccess(FieldAccessType.UNSAFE);
			serializer.updateFields();
			assertNull(serializer.finalFieldSetter);
		} finally {
			FinalFieldSetter.denied = denied;
		}
	}

	private void testFinalFields (List<BiFunction<Kryo, Class, Serializer>> serializers) {
		for (BiFunction<Kryo, Class, Serializer> factory : serializers) {
			Kryo kryo = new Kryo();
			kryo.setReferences(true);
			kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
			kryo.register(Base.class, factory.apply(kryo, Base.class));
			kryo.register(Node.class, factory.apply(kryo, Node.class));
			Node node = new Node(42, "name", 7);
			node.mutable = "mutable";

			Output output = new Output(1024, -1);
			kryo.writeObject(output, node);
			Node read = kryo.readObject(new Input(output.toBytes()), Node.class);
			assertNode(read);

			assertNode(kryo.copy(node));
		}
	}

	private void assertNode (Node node) {
		assertEquals(42, node.number);
		assertEquals("name", node.name);
		assertEquals(7L, node.base);
		assertEquals("mutable", node.mutable);
		assertSame(node, node.self); // A final field that refers to the object itself.
	}

	public static class Defaults implements Serializable {
		final String name;
		final int number;

		Defaults () {
			name = "default";
			number = 7;
		}

		Defaults (String name, int number) {
			this.name = name;
			this.number = number;
		}
	}

	public static class HashNode implements Serializable {
		final String id;
		final HashSet<HashNode> neighbors = new HashSet<>();

		HashNode (String id) {
			this.id = id;
		}

		public int hashCode () {
			return id == null ? 0 : id.hashCode();
		}

		public boolean equals (Object object) {
			return object instanceof HashNode other && Objects.equals(other.id, id);
		}
	}

	public static class NotSerializable {
		final int a = 1;
	}

	public static class NotSerializableSuperclass extends NotSerializable implements Serializable {
		final int b = 2;
	}

	public static class TaggedSubclass extends NotSerializable implements Serializable {
		@Tag(1) final int tagged = 4;
	}

	public static class TransientField implements Serializable {
		final transient int c = 3;
	}

	public static class Base implements Serializable {
		@Tag(1) final long base;

		Base (long base) {
			this.base = base;
		}
	}

	public static class Node extends Base {
		@Tag(2) final int number;
		@Tag(3) final String name;
		@Tag(4) final Node self;
		@Tag(5) @Since(0) String mutable;

		Node (int number, String name, long base) {
			super(base);
			this.number = number;
			this.name = name;
			self = this;
		}
	}
}
