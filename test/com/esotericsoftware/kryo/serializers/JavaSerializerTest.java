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

import java.util.Objects;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer.CompatibleFieldSerializerConfig;
import com.esotericsoftware.kryo.SerializerFactory.CompatibleFieldSerializerFactory;
import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** @author Nathan Sweet */
class JavaSerializerTest extends KryoTestCase {
	@Test
	void testJavaSerializer () {
		kryo.register(String.class, new JavaSerializer());
		roundTrip(50, "abcdefabcdefabcdefabcdefabcdefabcdefabcdef");
		roundTrip(12, "meow");

		kryo.register(TestClass.class, new JavaSerializer());
		TestClass test = new TestClass();
		test.stringField = "fubar";
		test.intField = 54321;
		roundTrip(146, test);
		roundTrip(146, test);
		roundTrip(146, test);
	}

	@Test
	void testJavaSerializerFallbackToDefaultClassLoader () {
		kryo.setClassLoader(new URLClassLoader(new URL[]{}, null));
		
		kryo.register(TestClass.class, new JavaSerializer());
		
		TestClass test = new TestClass();
		test.intField = 54321;
		roundTrip(139, test);
	}

	@Test
	void testReadThenWriteWithoutReset () {
		// With autoReset false, the object stream used for reading must not be used for writing.
		kryo.register(TestClass.class, new JavaSerializer());
		kryo.setAutoReset(false);
		TestClass test = new TestClass();
		test.stringField = "fubar";
		Output output = new Output(1024);
		kryo.writeObject(output, test);
		kryo.reset();
		assertEquals(test, kryo.readObject(new Input(output.toBytes()), TestClass.class));

		output = new Output(1024);
		kryo.writeObject(output, test);
		Kryo reader = new Kryo();
		reader.register(TestClass.class, new JavaSerializer());
		assertEquals(test, reader.readObject(new Input(output.toBytes()), TestClass.class));
	}

	@Test
	void testObjectInputFilterRejectsClass () {
		JavaSerializer serializer = new JavaSerializer();
		serializer.setObjectInputFilter(
			info -> info.serialClass() == TestClass.class ? ObjectInputFilter.Status.REJECTED : ObjectInputFilter.Status.UNDECIDED);
		kryo.register(TestClass.class, serializer);
		Output output = new Output(1024);
		kryo.writeObject(output, newTestClass());

		KryoException ex = assertThrows(KryoException.class,
			() -> kryo.readObject(new Input(output.toBytes()), TestClass.class));
		assertTrue(ex.getCause() instanceof InvalidClassException);
	}

	@Test
	void testObjectInputFilterAllowsClass () {
		JavaSerializer serializer = new JavaSerializer();
		serializer.setObjectInputFilter(info -> ObjectInputFilter.Status.ALLOWED);
		kryo.register(TestClass.class, serializer);
		TestClass test = newTestClass();
		Output output = new Output(1024);
		kryo.writeObject(output, test);

		assertEquals(test, kryo.readObject(new Input(output.toBytes()), TestClass.class));
	}

	@Test
	void testObjectInputFilterRejectsNestedClass () {
		// The filter decides about every class in the Java serialization data, not only the outermost one.
		List<Class<?>> seen = new ArrayList<>();
		JavaSerializer serializer = new JavaSerializer();
		serializer.setObjectInputFilter(info -> {
			if (info.serialClass() != null) seen.add(info.serialClass());
			return info.serialClass() == TestClass.class ? ObjectInputFilter.Status.REJECTED : ObjectInputFilter.Status.UNDECIDED;
		});
		kryo.register(Holder.class, serializer);
		Holder holder = new Holder();
		holder.payload = newTestClass();
		Output output = new Output(1024);
		kryo.writeObject(output, holder);

		KryoException ex = assertThrows(KryoException.class, () -> kryo.readObject(new Input(output.toBytes()), Holder.class));
		assertTrue(ex.getCause() instanceof InvalidClassException);
		assertTrue(seen.contains(Holder.class));
		assertTrue(seen.contains(TestClass.class));
	}

	@Test
	void testNoObjectInputFilterByDefault () {
		assertNull(new JavaSerializer().getObjectInputFilter());
	}

	private static TestClass newTestClass () {
		TestClass test = new TestClass();
		test.stringField = "fubar";
		test.intField = 54321;
		return test;
	}

	public static class Holder implements Serializable {
		Object payload;
	}

	// The same serializer writes the objects of a graph to different outputs and reads them from different inputs.
	@Test
	void testDeflateSerializer () {
		kryo.register(ArrayList.class);
		kryo.register(TestClass.class, new DeflateSerializer(new JavaSerializer()));

		ArrayList<TestClass> list = new ArrayList<>();
		for (int i = 0; i < 3; i++)
			list.add(testClass(i));
		roundTrip(Integer.MIN_VALUE, list);
	}

	@Test
	void testChunkedEncoding () {
		CompatibleFieldSerializerConfig config = new CompatibleFieldSerializerConfig();
		config.setChunkedEncoding(true);
		kryo.setDefaultSerializer(new CompatibleFieldSerializerFactory(config));
		kryo.register(ArrayList.class);
		kryo.register(ValueHolder.class);
		kryo.register(TestClass.class, new JavaSerializer());

		ArrayList<ValueHolder> list = new ArrayList<>();
		for (int i = 0; i < 3; i++)
			list.add(new ValueHolder(testClass(i)));
		roundTrip(Integer.MIN_VALUE, list);
	}

	@Test
	void testOutputsAlternate () {
		JavaSerializer serializer = new JavaSerializer();
		kryo.register(ArrayList.class);
		kryo.register(TestClass.class, new DeflateSerializer(serializer));
		kryo.register(ValueHolder.class, serializer);

		ArrayList<Object> list = new ArrayList<>();
		list.add(testClass(0));
		list.add(new ValueHolder(testClass(1)));
		list.add(testClass(2));
		list.add(new ValueHolder(testClass(3)));
		roundTrip(Integer.MIN_VALUE, list);
	}

	// All objects of a graph use one stream, so what they share is written once, also if they are written to different outputs.
	@Test
	void testSharedAcrossOutputs () {
		kryo.register(ArrayList.class);
		kryo.register(ValueHolder.class, new DeflateSerializer(new JavaSerializer()));

		TestClass shared = testClass(0);
		ArrayList<ValueHolder> list = new ArrayList<>();
		list.add(new ValueHolder(shared));
		list.add(new ValueHolder(shared));

		Output output = new Output(1024, -1);
		kryo.writeObject(output, list);
		output.writeString("after");

		Input input = new Input(output.toBytes());
		ArrayList<ValueHolder> read = kryo.readObject(input, ArrayList.class);
		assertEquals(list, read);
		assertSame(read.get(0).value, read.get(1).value);
		assertEquals("after", input.readString());
	}

	// A value written after others with the same serializer must end up in the output it is written to, here to be compressed.
	@Test
	void testDirectThenDeflate () {
		JavaSerializer serializer = new JavaSerializer();
		kryo.register(ArrayList.class);
		kryo.register(TestClass.class, new DeflateSerializer(serializer));
		kryo.register(ValueHolder.class, serializer);

		TestClass compressible = new TestClass();
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < 10000; i++)
			text.append('x');
		compressible.stringField = text.toString();

		ArrayList<Object> list = new ArrayList<>();
		list.add(new ValueHolder(testClass(0)));
		list.add(compressible);

		Output output = new Output(1024, -1);
		kryo.writeObject(output, list);
		assertTrue(output.position() < 1000, "Not compressed: " + output.position());
		output.writeString("after");

		Input input = new Input(output.toBytes());
		assertEquals(list, kryo.readObject(input, ArrayList.class));
		assertEquals("after", input.readString());
	}

	// The writeObject and readObject methods of a value can use the same serializer, over the object stream.
	@Test
	void testNested () {
		Nested.kryo = kryo;
		Nested.serializer = new JavaSerializer();
		kryo.register(Nested.class, Nested.serializer);
		kryo.register(ArrayList.class);

		ArrayList<Nested> list = new ArrayList<>();
		list.add(new Nested(testClass(0)));
		list.add(new Nested(testClass(1)));

		Output output = new Output(1024, -1);
		kryo.writeObject(output, list);
		output.writeString("after");

		Input input = new Input(output.toBytes());
		assertEquals(list, kryo.readObject(input, ArrayList.class));
		assertEquals("after", input.readString());
	}

	@Test
	void testFlushesOutput () {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		kryo.writeObject(new Output(stream, 1024), testClass(0), new JavaSerializer());
		assertTrue(stream.size() > 0);
	}

	@Test
	void testCloseInObject () {
		boolean[] closed = new boolean[2];
		JavaSerializer serializer = new JavaSerializer();
		ByteArrayOutputStream stream = new ByteArrayOutputStream() {
			public void close () {
				closed[0] = true;
			}
		};
		Output output = new Output(stream, 1024);
		kryo.writeObject(output, new Closes(), serializer);
		output.flush();
		assertTrue(closed[0]);

		kryo.readObject(new Input(new ByteArrayInputStream(stream.toByteArray()) {
			public void close () {
				closed[1] = true;
			}
		}), Closes.class, serializer);
		assertTrue(closed[1]);
	}

	@Test
	void testAvailable () {
		JavaSerializer serializer = new JavaSerializer();
		Output output = new Output(1024, -1);
		kryo.writeObject(output, new CustomData(), serializer);
		CustomData read = kryo.readObject(new Input(output.toBytes()), CustomData.class, serializer);
		assertEquals(4, read.available);
	}

	private TestClass testClass (int value) {
		TestClass test = new TestClass();
		test.stringField = "value " + value;
		test.intField = value;
		return test;
	}

	public static class Nested implements Serializable {
		static Kryo kryo;
		static JavaSerializer serializer;

		transient TestClass value;

		public Nested () {
		}

		public Nested (TestClass value) {
			this.value = value;
		}

		private void writeObject (ObjectOutputStream out) throws IOException {
			Output output = new Output(out);
			kryo.writeObject(output, value, serializer);
			output.flush();
			out.writeUTF("after");
		}

		private void readObject (ObjectInputStream in) throws IOException {
			value = kryo.readObject(new Input(in), TestClass.class, serializer);
			assertEquals("after", in.readUTF());
		}

		public boolean equals (Object obj) {
			return obj instanceof Nested && value.equals(((Nested)obj).value);
		}
	}

	public static class Closes implements Serializable {
		private void writeObject (ObjectOutputStream out) throws IOException {
			out.close();
		}

		private void readObject (ObjectInputStream in) throws IOException {
			in.close();
		}
	}

	public static class CustomData implements Serializable {
		transient int available;

		private void writeObject (ObjectOutputStream out) throws IOException {
			out.writeInt(1);
		}

		private void readObject (ObjectInputStream in) throws IOException {
			available = in.available();
			in.readInt();
		}
	}

	public static class ValueHolder implements Serializable {
		TestClass value;

		public ValueHolder () {
		}

		public ValueHolder (TestClass value) {
			this.value = value;
		}

		public boolean equals (Object obj) {
			return obj instanceof ValueHolder && Objects.equals(value, ((ValueHolder)obj).value);
		}
	}

	public static class TestClass implements Serializable {
		String stringField;
		int intField;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			TestClass other = (TestClass)obj;
			if (intField != other.intField) return false;
			if (stringField == null) {
				if (other.stringField != null) return false;
			} else if (!stringField.equals(other.stringField)) return false;
			return true;
		}
	}
}
