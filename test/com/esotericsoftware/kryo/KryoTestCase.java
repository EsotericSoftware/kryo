
package com.esotericsoftware.kryo;

import java.lang.reflect.Array;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.junit.Assert;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoInput;
import com.esotericsoftware.kryo.KryoOutput;

/** Convenience methods for round tripping objects. */
abstract public class KryoTestCase extends TestCase {
	protected Kryo kryo;

	protected void setUp () throws Exception {
		kryo = new Kryo();
		kryo.setReferences(false);
		kryo.setRegistrationRequired(true);
	}

	public <T> T roundTrip (int length, T object1) {
		KryoOutput output = new KryoOutput(length * 2, -1);
		kryo.writeClassAndObject(output, object1);

		KryoInput input = new KryoInput(output.toBytes());
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
		assertEquals("Incorrect length.", length, output.total());
		assertEquals("Incorrect number of bytes read.", length, input.total());

		return (T)object2;
	}

	static public void assertEquals (Object object1, Object object2) {
		Assert.assertEquals(arrayToList(object1), arrayToList(object2));
	}

	static public Object arrayToList (Object array) {
		if (array == null || !array.getClass().isArray()) return array;
		ArrayList list = new ArrayList(Array.getLength(array));
		for (int i = 0, n = Array.getLength(array); i < n; i++)
			list.add(arrayToList(Array.get(array, i)));
		return list;
	}

	static public ArrayList list (Object... items) {
		ArrayList list = new ArrayList();
		for (Object item : items)
			list.add(item);
		return list;
	}
}
