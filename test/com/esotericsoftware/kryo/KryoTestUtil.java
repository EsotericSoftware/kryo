
package com.esotericsoftware.kryo;

import java.lang.reflect.Array;
import java.util.ArrayList;

import org.junit.Assert;

public class KryoTestUtil {

	static public ArrayList list (Object... items) {
		ArrayList list = new ArrayList();
		for (Object item : items)
			list.add(item);
		return list;
	}

	static public Object arrayToList (Object array) {
		if (array == null || !array.getClass().isArray()) return array;
		ArrayList list = new ArrayList(Array.getLength(array));
		for (int i = 0, n = Array.getLength(array); i < n; i++)
			list.add(arrayToList(Array.get(array, i)));
		return list;
	}

	static public void assertEquals (Object object1, Object object2) {
		Assert.assertEquals(object1, object2);
	}
}
