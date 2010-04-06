
package com.esotericsoftware.kryo.serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.junit.Assert;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.ObjectBuffer;
import com.esotericsoftware.kryo.Serializer;

/**
 * Convenience methods for round tripping objects.
 */
abstract public class KryoTestCase extends TestCase {
	protected ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

	protected <T> T roundTrip (Serializer serializer, int length, T object1) {
		buffer.clear();
		serializer.setCanBeNull(true);
		roundTripSerializer(serializer, length, object1);
		buffer.clear();
		for (int i = 0; i < 103; i++)
			buffer.put((byte)i);
		roundTripSerializer(serializer, length, object1);
		buffer.clear();
		serializer.setCanBeNull(false);
		return roundTripSerializer(serializer, length - 1, object1);
	}

	private <T> T roundTripSerializer (Serializer serializer, int length, T object1) {
		int start = buffer.position();
		Kryo.getContext().reset();
		serializer.writeObject(buffer, object1);
		assertEquals("Incorrect length.", length, buffer.position() - start);
		buffer.position(start);
		Kryo.getContext().reset();
		Object object2 = serializer.readObject(buffer, object1.getClass());
		assertEquals("Incorrect number of bytes read.", start + length, buffer.position());
		assertEquals(object1, object2);

		buffer.position(start);
		Kryo.getContext().reset();
		serializer.writeObjectData(buffer, object1);
		buffer.position(start);
		Kryo.getContext().reset();
		object2 = serializer.readObjectData(buffer, object1.getClass());
		assertEquals(object1, object2);

		return (T)object2;
	}

	protected <T> T roundTrip (Kryo kryo, int length, T object1) {
		buffer.clear();
		roundTripKryo(kryo, length, object1);
		buffer.clear();
		for (int i = 0; i < 96; i++)
			buffer.put((byte)i);
		roundTripKryo(kryo, length, object1);
		roundTripObjectBuffer(kryo, length, object1, 1024);
		return roundTripObjectBuffer(kryo, length, object1, 2);
	}

	private <T> T roundTripKryo (Kryo kryo, int length, T object1) {
		int start = buffer.position();
		kryo.writeClassAndObject(buffer, object1);
		assertEquals("Incorrect length.", length, buffer.position() - start);
		buffer.position(start);
		byte[] bytes = new byte[length];
		buffer.get(bytes);
		buffer.position(start);
		Object object2 = kryo.readClassAndObject(buffer);
		assertEquals("Incorrect number of bytes read.", start + length, buffer.position());
		assertEquals(object1, object2);

		buffer.position(start);
		kryo.writeObject(buffer, object1);
		buffer.position(start);
		object2 = kryo.readObject(buffer, object1.getClass());
		assertEquals(object1, object2);

		buffer.position(start);
		kryo.writeObjectData(buffer, object1);
		buffer.position(start);
		object2 = kryo.readObjectData(buffer, object1.getClass());
		assertEquals(object1, object2);

		// Leave buffer with serialized bytes.
		buffer.clear();
		buffer.put(bytes);
		buffer.flip();

		return (T)object2;
	}

	private <T> T roundTripObjectBuffer (Kryo kryo, int length, T object1, int initialSize) {
		ObjectBuffer buffer = new ObjectBuffer(kryo, initialSize, 1024);

		// Bytes.

		byte[] bytes = buffer.writeClassAndObject(object1);
		assertEquals("Incorrect length.", length, bytes.length);
		Object object2 = buffer.readClassAndObject(bytes);
		assertEquals(object1, object2);
		object2 = buffer.readClassAndObject(bytes);
		assertEquals(object1, object2);

		bytes = buffer.writeObject(object1);
		object2 = buffer.readObject(bytes, object1.getClass());
		assertEquals(object1, object2);
		object2 = buffer.readObject(bytes, object1.getClass());
		assertEquals(object1, object2);

		bytes = buffer.writeObjectData(object1);
		object2 = buffer.readObjectData(bytes, object1.getClass());
		assertEquals(object1, object2);
		object2 = buffer.readObjectData(bytes, object1.getClass());
		assertEquals(object1, object2);

		// Streams.

		ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
		buffer.writeClassAndObject(output, object1);
		assertEquals("Incorrect length.", length, output.size());
		ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
		object2 = buffer.readClassAndObject(input);
		assertEquals(object1, object2);
		input.reset();
		object2 = buffer.readClassAndObject(input);
		assertEquals(object1, object2);

		output.reset();
		buffer.writeObject(output, object1);
		input = new ByteArrayInputStream(output.toByteArray());
		object2 = buffer.readObject(input, object1.getClass());
		assertEquals(object1, object2);
		input.reset();
		object2 = buffer.readObject(input, object1.getClass());
		assertEquals(object1, object2);

		output.reset();
		buffer.writeObjectData(output, object1);
		input = new ByteArrayInputStream(output.toByteArray());
		object2 = buffer.readObjectData(input, object1.getClass());
		assertEquals(object1, object2);
		input.reset();
		object2 = buffer.readObjectData(input, object1.getClass());
		assertEquals(object1, object2);

		return (T)object2;
	}

	static public void assertEquals (Object object1, Object object2) {
		Assert.assertEquals(arrayToList(object1), arrayToList(object2));
	}

	static private Object arrayToList (Object array) {
		if (array == null || !array.getClass().isArray()) return array;
		ArrayList list = new ArrayList(Array.getLength(array));
		for (int i = 0, n = Array.getLength(array); i < n; i++)
			list.add(arrayToList(Array.get(array, i)));
		return list;
	}

	static public ArrayList toList (Object... items) {
		ArrayList list = new ArrayList();
		for (Object item : items)
			list.add(item);
		return list;
	}
}
