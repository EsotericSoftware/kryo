/* Copyright (c) 2008, Nathan Sweet
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

import java.io.FileNotFoundException;

import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;

/** @author Nathan Sweet <misc@n4te.com> */
public class CompatibleFieldSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	public void testCompatibleFieldSerializer () throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();
		object1.other = new AnotherClass();
		object1.other.value = "meow";
		kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
		kryo.register(TestClass.class);
		kryo.register(AnotherClass.class);
		roundTrip(107, 107, object1);
	}

	public void testAddedField () throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();
		object1.other = new AnotherClass();
		object1.other.value = "meow";

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, TestClass.class);
		serializer.removeField("text");
		kryo.register(TestClass.class, serializer);
		kryo.register(AnotherClass.class, new CompatibleFieldSerializer(kryo, AnotherClass.class));
		roundTrip(80, 80, object1);

		kryo.register(TestClass.class, new CompatibleFieldSerializer(kryo, TestClass.class));
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}

	public void testRemovedField () throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();

		kryo.register(TestClass.class, new CompatibleFieldSerializer(kryo, TestClass.class));
		roundTrip(94, 94, object1);

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, TestClass.class);
		serializer.removeField("text");
		kryo.register(TestClass.class, serializer);
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}

	public void testExtendedClass () throws FileNotFoundException {
		ExtendedTestClass extendedObject = new ExtendedTestClass();

		// this test would fail with DEFAULT field name strategy
		kryo.getFieldSerializerConfig().setCachedFieldNameStrategy(FieldSerializer.CachedFieldNameStrategy.EXTENDED);

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, ExtendedTestClass.class);
		kryo.register(ExtendedTestClass.class, serializer);
		roundTrip(286, 286, extendedObject);

		ExtendedTestClass object2 = (ExtendedTestClass)kryo.readClassAndObject(input);
		assertEquals(extendedObject, object2);
	}

	static public class TestClass {
		public String text = "something";
		public int moo = 120;
		public long moo2 = 1234120;
		public TestClass child;
		public int zzz = 123;
		public AnotherClass other;

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

	static public class ExtendedTestClass extends TestClass {
		// keep the same names of attributes like TestClass
		public String text = "extendedSomething";
		public int moo = 127;
		public long moo2 = 5555;
		public TestClass child;
		public int zzz = 222;
		public AnotherClass other;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ExtendedTestClass other = (ExtendedTestClass)obj;

			if (!super.equals(obj)) return false;
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

	static public class AnotherClass {
		String value;
	}
}
