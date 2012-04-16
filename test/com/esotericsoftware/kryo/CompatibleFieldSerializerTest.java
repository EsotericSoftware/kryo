
package com.esotericsoftware.kryo;

import java.io.FileNotFoundException;

import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;

/** @author Nathan Sweet <misc@n4te.com> */
public class CompatibleFieldSerializerTest extends KryoTestCase {
	public void testAddedField () throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, TestClass.class);
		serializer.removeField("text");
		kryo.register(TestClass.class, serializer);
		roundTrip(55, object1);

		kryo.register(TestClass.class, new CompatibleFieldSerializer(kryo, TestClass.class));
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}

	public void testRemovedField () throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();

		kryo.register(TestClass.class, new CompatibleFieldSerializer(kryo, TestClass.class));
		roundTrip(84, object1);

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, TestClass.class);
		serializer.removeField("text");
		kryo.register(TestClass.class, serializer);
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}

	static public class TestClass {
		public String text = "something";
		public int moo = 120;
		public long moo2 = 1234120;
		public TestClass child;
		public int zzz = 123;

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
}
