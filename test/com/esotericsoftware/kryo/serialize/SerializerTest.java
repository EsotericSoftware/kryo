
package com.esotericsoftware.kryo.serialize;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import junit.framework.TestCase;

import org.junit.Assert;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.Serializer;

// TODO - Write tests for all serializers.

public class SerializerTest extends TestCase {
	private ByteBuffer buffer = ByteBuffer.allocateDirect(500);

	private int intValues[] = {
	//
		0, 1, 2, 3, //
		-1, -2, -3, //
		32, -32, //
		127, 128, 129, //
		-125, -126, -127, -128, //
		252, 253, 254, 255, 256, //
		-252, -253, -254, -255, -256, //
		32767, 32768, 32769, //
		-32767, -32768, -32769, //
		65535, 65536, 65537, //
		Integer.MAX_VALUE, Integer.MIN_VALUE, //
	};

	public void testCollection () {
		Kryo kryo = new Kryo();
		CollectionSerializer serializer = new CollectionSerializer(kryo);
		roundTrip(serializer, 11, new ArrayList(Arrays.asList("1", "2", "3")));
		roundTrip(serializer, 13, new ArrayList(Arrays.asList("1", "2", null, 1, 2)));
		roundTrip(serializer, 15, new ArrayList(Arrays.asList("1", "2", null, 1, 2, 5)));
		roundTrip(serializer, 11, new ArrayList(Arrays.asList("1", "2", "3")));
		roundTrip(serializer, 11, new ArrayList(Arrays.asList("1", "2", "3")));
		serializer.setElementClass(String.class);
		roundTrip(serializer, 11, new ArrayList(Arrays.asList("1", "2", "3")));
		serializer.setElementsCanBeNull(false);
		roundTrip(serializer, 8, new ArrayList(Arrays.asList("1", "2", "3")));
	}

	public void testArray () {
		Kryo kryo = new Kryo();
		ArraySerializer serializer = new ArraySerializer(kryo);
		roundTrip(serializer, 7, new int[] {1, 2, 3, 4});
		roundTrip(serializer, 11, new int[] {1, 2, -100, 4});
		roundTrip(serializer, 13, new int[] {1, 2, -100, 40000});
		roundTrip(serializer, 10, new int[][] { {1, 2}, {100, 4}});
		roundTrip(serializer, 12, new int[][] { {1}, {2}, {100}, {4}});
		roundTrip(serializer, 15, new int[][][] { { {1}, {2}}, { {100}, {4}}});
		roundTrip(serializer, 19, new String[] {"11", "2222", "3", "4"});
		roundTrip(serializer, 17, new String[] {"11", "2222", null, "4"});
		serializer.setDimensionCount(1);
		serializer.setElementsAreSameType(true);
		roundTrip(serializer, 16, new String[] {"11", "2222", null, "4"});
		serializer.setElementsAreSameType(false);
		roundTrip(serializer, 16, new String[] {"11", "2222", null, "4"});
		roundTrip(new ArraySerializer(kryo), 6, new String[] {null, null, null});
		roundTrip(new ArraySerializer(kryo), 3, new String[] {});
		serializer.setElementsCanBeNull(true);
		serializer.setElementsAreSameType(true);
		roundTrip(serializer, 18, new String[] {"11", "2222", "3", "4"});
		serializer.setElementsCanBeNull(false);
		roundTrip(serializer, 14, new String[] {"11", "2222", "3", "4"});
	}

	public void testMap () {
		Kryo kryo = new Kryo();
		HashMap map = new HashMap();
		map.put("123", "456");
		map.put("789", "abc");
		MapSerializer serializer = new MapSerializer(kryo);
		roundTrip(serializer, 22, map);
		serializer.setKeyClass(String.class);
		serializer.setKeysCanBeNull(false);
		serializer.setValueClass(String.class);
		roundTrip(serializer, 20, map);
		serializer.setValuesCanBeNull(false);
		roundTrip(serializer, 18, map);
	}

	public void testShort () {
		roundTrip(new ShortSerializer(true), 2, (short)123);
		roundTrip(new ShortSerializer(false), 2, (short)123);

		buffer.clear();
		ShortSerializer.put(buffer, (short)250, false);
		buffer.flip();
		assertEquals(3, buffer.limit());
		assertEquals((short)250, ShortSerializer.get(buffer, false));

		buffer.clear();
		ShortSerializer.put(buffer, (short)250, true);
		buffer.flip();
		assertEquals(1, buffer.limit());
		assertEquals((short)250, ShortSerializer.get(buffer, true));

		buffer.clear();
		ShortSerializer.put(buffer, (short)123, true);
		buffer.flip();
		assertEquals(1, buffer.limit());
		assertEquals((short)123, ShortSerializer.get(buffer, true));
	}

	public void testNumbers () {
		for (int value : intValues) {
			roundTrip(value, false);
			roundTrip(value, true);
		}
	}

	private void roundTrip (int value, boolean optimizePositive) {
		buffer.clear();
		IntSerializer.put(buffer, value, optimizePositive);
		buffer.flip();
		int result = IntSerializer.get(buffer, optimizePositive);
		System.out.println(value + " int bytes, " + optimizePositive + ": " + buffer.limit());
		assertEquals(result, value);

		short shortValue = (short)value;
		buffer.clear();
		ShortSerializer.put(buffer, shortValue, optimizePositive);
		buffer.flip();
		short shortResult = ShortSerializer.get(buffer, optimizePositive);
		System.out.println(shortValue + " short bytes, " + optimizePositive + ": " + buffer.limit());
		assertEquals(shortResult, shortValue);
	}

	private void roundTrip (Serializer serializer, int length, Object object1) {
		buffer.clear();
		serializer.writeObject(buffer, object1);
		buffer.flip();
		System.out.println(object1 + " bytes: " + buffer.remaining());
		assertEquals("Incorrect length.", length, buffer.remaining());

		Object object2 = serializer.readObject(buffer, object1.getClass());
		if (object1.getClass().isArray()) {
			if (object1 instanceof int[])
				Assert.assertArrayEquals((int[])object1, (int[])object2);
			else if (object1 instanceof int[][])
				Assert.assertArrayEquals((int[][])object1, (int[][])object2);
			else if (object1 instanceof int[][][])
				Assert.assertArrayEquals((int[][][])object1, (int[][][])object2);
			else if (object1 instanceof String[])
				Assert.assertArrayEquals((String[])object1, (String[])object2);
			else
				fail();
		} else
			assertEquals(object1, object2);
	}

	public void testNonNull () {
		Kryo kryo = new Kryo();
		FieldSerializer fieldSerializer = new FieldSerializer(kryo);

		TestClass value = new TestClass();
		value.text = "moo";
		NonNullTestClass nonNullValue = new NonNullTestClass();
		nonNullValue.nonNullText = "moo";

		buffer.clear();
		fieldSerializer.writeObjectData(buffer, value);
		buffer.flip();
		assertEquals("Incorrect length.", 5, buffer.remaining());

		buffer.clear();
		fieldSerializer.writeObjectData(buffer, nonNullValue);
		buffer.flip();
		assertEquals("Incorrect length.", 4, buffer.remaining());
	}

	static public class NonNullTestClass {
		@NotNull
		public String nonNullText;
	}

	static public class TestClass {
		public String text;
	}
}
