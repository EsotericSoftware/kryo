
package com.esotericsoftware.kryo.serialize;

import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serialize.FieldSerializerTest.HasStringField;
import com.esotericsoftware.kryo.serialize.FieldSerializerTest.TestClass;

public class ReferenceFieldSerializerTest extends KryoTestCase {
	public void testReferenceFieldSerializer () {
		Kryo kryo = new Kryo();
		kryo.register(TestClass.class, new ReferenceFieldSerializer(kryo, TestClass.class));
		kryo.register(HasStringField.class, new ReferenceFieldSerializer(kryo, HasStringField.class));

		HasStringField hasStringField = new HasStringField();
		hasStringField.text = "moo";
		roundTrip(kryo, 7, hasStringField);
		roundTrip(new ReferenceFieldSerializer(kryo, HasStringField.class), 7, hasStringField);

		TestClass test = new TestClass();
		test.optional = 12;
		test.nullField = "value";
		test.text = "123";
		test.child = new TestClass();
		roundTrip(kryo, 41, test);
		test.nullField = null;
		roundTrip(kryo, 35, test);

		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(TestClass.class);
		serializer.removeField("optional");
		roundTrip(kryo, 33, test);
	}

	public void testReference () {
		Kryo kryo = new Kryo();
		kryo.register(ArrayList.class);
		kryo.register(TestClass.class, new ReferenceFieldSerializer(kryo, TestClass.class));

		TestClass test = new TestClass();
		test.optional = 12;
		test.nullField = "value";
		test.text = "123";
		test.child = new TestClass();
		test.child2 = test.child;
		TestClass object2 = roundTrip(kryo, 42, test);
		assertTrue(object2.child == object2.child2);
	}

	public void testCyclicReference () {
		Kryo kryo = new Kryo();
		kryo.register(TestClass.class, new ReferenceFieldSerializer(kryo, TestClass.class));

		TestClass test = new TestClass();
		test.optional = 12;
		test.nullField = "value";
		test.text = "123";
		test.child = test;
		TestClass object2 = roundTrip(kryo, 22, test);
		assertTrue(object2.child == object2);
	}

	public void testNonStaticInnerClass () {
		Kryo kryo = new Kryo() {
			protected Serializer newDefaultSerializer (Class type) {
				return new ReferenceFieldSerializer(this, type);
			}
		};
		kryo.register(OuterClass.class);
		kryo.register(OuterClass.InnerClass.class);
		OuterClass outerClass = new OuterClass();
		outerClass.something = "meow";
		outerClass.innerClass.somethingElse = "moo";
		roundTrip(kryo, 18, outerClass);
		roundTrip(kryo, 21, outerClass.innerClass);
	}

	static public class OuterClass {
		public InnerClass innerClass = new InnerClass();
		public String something;

		public class InnerClass {
			public String somethingElse;

			public boolean equals (Object obj) {
				if (this == obj) return true;
				if (obj == null) return false;
				if (getClass() != obj.getClass()) return false;
				InnerClass other = (InnerClass)obj;
				if (somethingElse == null) {
					if (other.somethingElse != null) return false;
				} else if (!somethingElse.equals(other.somethingElse)) return false;
				return true;
			}
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			OuterClass other = (OuterClass)obj;
			if (innerClass == null) {
				if (other.innerClass != null) return false;
			} else if (!innerClass.equals(other.innerClass)) return false;
			if (something == null) {
				if (other.something != null) return false;
			} else if (!something.equals(other.something)) return false;
			return true;
		}
	}
}
