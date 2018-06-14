
package com.esotericsoftware.kryo;

import java.lang.reflect.Array;
import java.util.ArrayList;

import org.junit.Assert;

/**
 * <pre>
 * Kryo Test Utility Class.
 * This class should consisted of Kryo Project specific assertions,
 * boiler factory code.
 * </pre>
 */
public class KryoTestUtil {

	static public ArrayList list (Object... items) {
		ArrayList list = new ArrayList();
		for (Object item : items)
			list.add(item);
		return list;
	}

	static Object arrayToList (Object array) {
		if (array == null || !array.getClass().isArray()) return array;
		ArrayList list = new ArrayList(Array.getLength(array));
		for (int i = 0, n = Array.getLength(array); i < n; i++)
			list.add(arrayToList(Array.get(array, i)));
		return list;
	}

	static void assertDoubleEquals (double expected, double actual) {
		Assert.assertEquals(expected, actual, 0);
	}

	static void assertFloatEquals (float expected, float actual) {
		Assert.assertEquals(expected, actual, 0);
	}
}
