
package com.esotericsoftware.kryo.serialize;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.junit.Assert;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.ObjectBuffer;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.minlog.Log;

// TODO - Write tests for all serializers.
// TODO - Break this monolithic test into smaller tests.

public class CompatibleFieldSerializerTest extends TestCase {
	private ByteBuffer buffer = ByteBuffer.allocateDirect(500);

	private <T> T roundTrip (Serializer serializer, int length, T object1) {
		Kryo.reset();
		buffer.clear();
		serializer.writeObject(buffer, object1);
		buffer.flip();
		System.out.println(object1 + " bytes: " + buffer.remaining());
		assertEquals("Incorrect length.", length, buffer.remaining());

		Kryo.reset();
		Object object2 = serializer.readObject(buffer, object1.getClass());
		assertEquals(object1, object2);
		return (T)object2;
	}

	public void testCompatibleFieldSerializer () throws FileNotFoundException {
		Log.TRACE();

		TestClass value = new TestClass();
		value.child = new TestClass();

		Kryo kryo = new Kryo();
		ObjectBuffer objectBuffer = new ObjectBuffer(kryo);

		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(kryo, TestClass.class);
		// value.optional = 123;
		kryo.register(TestClass.class, serializer);

		// objectBuffer.writeClassAndObject(new FileOutputStream("test.bin"), value);

		// Object value3 = objectBuffer.readClassAndObject(new FileInputStream("test.bin"));
		// assertEquals(value, value3);

		TestClass value2 = roundTrip(serializer, 71, value);
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
