
package com.esotericsoftware.kryo;

import java.io.Serializable;

import com.esotericsoftware.kryo.serializers.JavaSerializer;

/** @author Nathan Sweet <misc@n4te.com> */
public class JavaSerializerTest extends KryoTestCase {
	public void testJavaSerializer () {
		kryo.register(String.class, new JavaSerializer());
		roundTrip(50, 50,  "abcdefabcdefabcdefabcdefabcdefabcdefabcdef");
		roundTrip(12, 12, "meow");

		kryo.register(TestClass.class, new JavaSerializer());
		TestClass test = new TestClass();
		test.stringField = "fubar";
		test.intField = 54321;
		roundTrip(134, 134, test);
		roundTrip(134, 134, test);
		roundTrip(134, 134, test);
	}

	static public class TestClass implements Serializable {
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
