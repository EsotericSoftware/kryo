/* Copyright (c) 2008-2025, Nathan Sweet
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
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.SerializerFactory.TaggedFieldSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Objects;

import org.junit.jupiter.api.Test;

@SuppressWarnings("synthetic-access")
class TaggedFieldSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@Test
	void testTaggedFieldSerializer () {
		TestClass object1 = new TestClass();
		object1.moo = 2;
		object1.child = new TestClass();
		object1.child.moo = 5;
		object1.other = new AnotherClass();
		object1.other.value = "meow";
		object1.ignored = 32;
		kryo.setDefaultSerializer(TaggedFieldSerializer.class);
		kryo.register(TestClass.class);
		kryo.register(AnotherClass.class);
		TestClass object2 = roundTrip(57, object1);
		assertEquals(0, object2.ignored);
	}

	@Test
	void testAddedField () {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();
		object1.other = new AnotherClass();
		object1.other.value = "meow";

		TaggedFieldSerializer serializer = new TaggedFieldSerializer(kryo, TestClass.class);
		serializer.removeField("text");
		kryo.register(TestClass.class, serializer);
		kryo.register(AnotherClass.class, new TaggedFieldSerializer(kryo, AnotherClass.class));
		roundTrip(39, object1);

		kryo.register(TestClass.class, new TaggedFieldSerializer(kryo, TestClass.class));
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}

	/** Serializes an array with a Class with two tagged fields. Then deserializes it using a serializer that has removed some
	 * fields to simulate a past version of the compiled application. An array is used to ensure subsequent bytes in the stream are
	 * unaffected. */
	@Test
	void testForwardCompatibility () {
		FutureClass futureObject = new FutureClass();
		futureObject.value = 3;
		futureObject.futureString = "future";
		futureObject.futureClass2 = new FutureClass2();
		futureObject.futureClass2.text = "futureText";
		futureObject.futureClass2.moo = 13;
		futureObject.futureClass2.moo2 = 9000L;
		futureObject.futureClass2.zzz = 15;
		futureObject.futureClass2.fc2 = new FutureClass2();
		futureObject.futureClass2.fc2.text = "inner futureText";
		futureObject.futureClass2.fc2.moo = 254;
		futureObject.futureClass2.fc2.moo2 = 1L;
		futureObject.futureClass2.fc2.zzz = 503;
		Object[] futureArray = new Object[2];
		futureArray[0] = futureObject;
		futureArray[1] = new TestClass();

		TaggedFieldSerializerFactory factory = new TaggedFieldSerializerFactory();
		factory.getConfig().setChunkedEncoding(true);
		kryo.setDefaultSerializer(factory);
		kryo.register(TestClass.class);
		kryo.register(Object[].class);
		TaggedFieldSerializer<FutureClass> futureSerializer = new TaggedFieldSerializer(kryo, FutureClass.class);
		futureSerializer.getTaggedFieldSerializerConfig().setChunkedEncoding(true);
		futureSerializer.updateFields();
		kryo.register(FutureClass.class, futureSerializer);
		TaggedFieldSerializer<FutureClass2> futureSerializer2 = new TaggedFieldSerializer(kryo, FutureClass2.class);
		futureSerializer2.getTaggedFieldSerializerConfig().setChunkedEncoding(true);
		futureSerializer2.updateFields();
		kryo.register(FutureClass2.class, futureSerializer2);

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		output = new Output(outStream);
		kryo.writeClassAndObject(output, futureArray);
		output.flush();
		byte[] futureArrayData = outStream.toByteArray();

		TaggedFieldSerializer<FutureClass> presentSerializer = new TaggedFieldSerializer(kryo, FutureClass.class);
		presentSerializer.getTaggedFieldSerializerConfig().setChunkedEncoding(true);
		presentSerializer.updateFields();
		presentSerializer.removeField("futureString"); // simulate past version of application
		kryo.register(FutureClass.class, presentSerializer);
		TaggedFieldSerializer<FutureClass2> presentSerializer2 = new TaggedFieldSerializer(kryo, FutureClass2.class);
		presentSerializer2.getTaggedFieldSerializerConfig().setChunkedEncoding(true);
		presentSerializer2.updateFields();
		presentSerializer2.removeField("zzz"); // simulate past version of application
		presentSerializer2.removeField("fc2"); // simulate past version of application
		kryo.register(FutureClass2.class, presentSerializer2);

		ByteArrayInputStream inStream = new ByteArrayInputStream(futureArrayData);
		input = new Input(inStream);
		Object[] presentArray = (Object[])kryo.readClassAndObject(input);
		FutureClass presentObject = (FutureClass)presentArray[0];
		assertNotEquals(futureObject, presentObject);
		assertTrue(presentObject.pastEquals(futureObject));
		assertEquals(futureArray[1], presentArray[1]);
	}

	/** Attempts to register a class with a field tagged with a value already used in its superclass. Should receive
	 * IllegalArgumentException. */
	@Test
	void testInvalidTagValue () {
		Kryo newKryo = new Kryo();
		newKryo.setReferences(true);
		TaggedFieldSerializerFactory factory = new TaggedFieldSerializerFactory();
		factory.getConfig().setChunkedEncoding(true);
		kryo.setDefaultSerializer(factory);
		newKryo.setDefaultSerializer(TaggedFieldSerializer.class);

		boolean receivedIAE = false;
		try {
			newKryo.register(IncompatibleClass.class);
		} catch (IllegalArgumentException ex) {
			receivedIAE = true;
		}
		assertTrue(receivedIAE);
	}

	// https://github.com/EsotericSoftware/kryo/issues/774
	@Test
	void testClassWithObjectField() {
		TaggedFieldSerializer<ClassWithObjectField> serializer = new TaggedFieldSerializer<>(kryo, ClassWithObjectField.class);
		final TaggedFieldSerializer.TaggedFieldSerializerConfig config = serializer.getTaggedFieldSerializerConfig();
		config.setChunkedEncoding(true);
		config.setReadUnknownTagData(true);
		kryo.register(ClassWithObjectField.class, serializer);

		roundTrip(8, new ClassWithObjectField(123));
		roundTrip(9, new ClassWithObjectField("foo"));
	}

	public static class ClassWithObjectField {
		@Tag(0)
		Object value;

		public ClassWithObjectField() { }

		public ClassWithObjectField(Object value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ClassWithObjectField wrapper = (ClassWithObjectField) o;
			return Objects.equals(value, wrapper.value);
		}
	}

	public static class TestClass {
		@Tag(0) public String text = "something";
		@Tag(1) public int moo = 120;
		@Tag(2) public long moo2 = 1234120;
		@Tag(3) public TestClass child;
		@Tag(4) public int zzz = 123;
		@Tag(5) public AnotherClass other;
		@Tag(6) @Deprecated public int ignored;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			TestClass other = (TestClass)obj;
			if (child == null) {
				if (other.child != null) return false;
			} else if (!child.equals(other.child)) return false;
			if (moo != other.moo) return false;
			if (moo2 != other.moo2) return false;
			if (text == null) {
				if (other.text != null) return false;
			} else if (!text.equals(other.text)) return false;
			if (zzz != other.zzz) return false;
			return true;
		}
	}

	public static class IncompatibleClass extends TestClass {
		@Tag(3) public int poorlyTaggedField = 5;
	}

	public static class AnotherClass {
		@Tag(1) String value;
	}

	private static class FutureClass {
		@Tag(0) public Integer value;
		@Tag(1) public FutureClass2 futureClass2;
		@Tag(value = 2) public String futureString = "unchanged";

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			FutureClass other = (FutureClass)obj;
			if (futureString == null) {
				if (other.futureString != null) return false;
			} else if (!futureString.equals(other.futureString)) return false;
			if (futureClass2 == null) {
				if (other.futureClass2 != null) return false;
			} else if (!futureClass2.equals(other.futureClass2)) return false;
			if (value == null) {
				if (other.value != null) return false;
			} else if (!value.equals(other.value)) return false;
			return true;
		}

		/** What equals(Object) would have been before the chunked fields were added to the class. */
		public boolean pastEquals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			FutureClass other = (FutureClass)obj;
			if (futureClass2 == null) {
				if (other.futureClass2 != null) return false;
			} else if (!futureClass2.pastEquals(other.futureClass2)) return false;
			if (value == null) {
				if (other.value != null) return false;
			} else if (!value.equals(other.value)) return false;
			return true;
		}
	}

	private static class FutureClass2 {
		@Tag(0) public String text = "something";
		@Tag(1) public int moo = 120;
		@Tag(2) public long moo2 = 1234120;
		@Tag(value = 3) public int zzz = 123;
		@Tag(value = 4) public FutureClass2 fc2;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			FutureClass2 other = (FutureClass2)obj;
			if (fc2 == null) {
				if (other.fc2 != null) return false;
			} else if (!fc2.equals(other.fc2)) return false;
			if (moo != other.moo) return false;
			if (moo2 != other.moo2) return false;
			if (text == null) {
				if (other.text != null) return false;
			} else if (!text.equals(other.text)) return false;
			if (zzz != other.zzz) return false;
			return true;
		}

		/** What equals(Object) would have been before the chunked fields were added to the class. */
		public boolean pastEquals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			FutureClass2 other = (FutureClass2)obj;
			if (moo != other.moo) return false;
			if (moo2 != other.moo2) return false;
			if (text == null) {
				if (other.text != null) return false;
			} else if (!text.equals(other.text)) return false;
			return true;
		}
	}
}
