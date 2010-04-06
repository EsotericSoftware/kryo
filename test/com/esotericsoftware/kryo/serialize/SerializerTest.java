
package com.esotericsoftware.kryo.serialize;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;

/**
 * Tests all the simple serializers.
 */
public class SerializerTest extends KryoTestCase {
	public void testStringSerializer () {
		roundTrip(new StringSerializer(), 3, "a");
		roundTrip(new StringSerializer(), 3, "\n");
		roundTrip(new StringSerializer(), 2, "");
		roundTrip(new StringSerializer(), 99,
			"ABCDEFGHIJKLMNOPQRSTUVWXYZ\rabcdefghijklmnopqrstuvwxyz\n1234567890\t\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*");
		roundTrip(new StringSerializer(), 21, "abcdef\u00E1\u00E9\u00ED\u00F3\u00FA\u1234");
	}

	public void testCollectionSerializer () {
		Kryo kryo = new Kryo();
		CollectionSerializer serializer = new CollectionSerializer(kryo);
		kryo.register(ArrayList.class, serializer);
		kryo.register(LinkedList.class, serializer);
		kryo.register(CopyOnWriteArrayList.class, serializer);
		roundTrip(kryo, 11, toList("1", "2", "3"));
		roundTrip(kryo, 13, toList("1", "2", null, 1, 2));
		roundTrip(kryo, 15, toList("1", "2", null, 1, 2, 5));
		roundTrip(kryo, 11, toList("1", "2", "3"));
		roundTrip(kryo, 11, toList("1", "2", "3"));
		roundTrip(kryo, 13, toList("1", "2", toList("3")));
		roundTrip(kryo, 13, new LinkedList(toList("1", "2", toList("3"))));
		roundTrip(kryo, 13, new CopyOnWriteArrayList(toList("1", "2", toList("3"))));
		serializer.setElementClass(String.class);
		roundTrip(kryo, 11, toList("1", "2", "3"));
		serializer.setElementClass(String.class, new StringSerializer());
		roundTrip(kryo, 11, toList("1", "2", "3"));
		serializer.setElementsCanBeNull(false);
		roundTrip(kryo, 8, toList("1", "2", "3"));
		serializer.setLength(3);
		roundTrip(kryo, 7, toList("1", "2", "3"));
	}

	public void testArraySerializer () {
		Kryo kryo = new Kryo();
		ArraySerializer serializer = new ArraySerializer(kryo);
		kryo.register(int[].class, serializer);
		kryo.register(int[][].class, serializer);
		kryo.register(int[][][].class, serializer);
		kryo.register(String[].class, serializer);
		kryo.register(Object[].class, serializer);
		roundTrip(kryo, 7, new int[] {1, 2, 3, 4});
		roundTrip(kryo, 11, new int[] {1, 2, -100, 4});
		roundTrip(kryo, 13, new int[] {1, 2, -100, 40000});
		roundTrip(kryo, 10, new int[][] { {1, 2}, {100, 4}});
		roundTrip(kryo, 12, new int[][] { {1}, {2}, {100}, {4}});
		roundTrip(kryo, 15, new int[][][] { { {1}, {2}}, { {100}, {4}}});
		roundTrip(kryo, 19, new String[] {"11", "2222", "3", "4"});
		roundTrip(kryo, 17, new String[] {"11", "2222", null, "4"});
		roundTrip(kryo, 37, new Object[] {new String[] {"11", "2222", null, "4"}, new int[] {1, 2, 3, 4},
			new int[][] { {1, 2}, {100, 4}}});
		serializer.setDimensionCount(1);
		serializer.setElementsAreSameType(true);
		roundTrip(kryo, 16, new String[] {"11", "2222", null, "4"});
		serializer.setElementsAreSameType(false);
		roundTrip(kryo, 16, new String[] {"11", "2222", null, "4"});
		roundTrip(new ArraySerializer(kryo), 6, new String[] {null, null, null});
		roundTrip(new ArraySerializer(kryo), 3, new String[] {});
		serializer.setElementsAreSameType(true);
		roundTrip(kryo, 18, new String[] {"11", "2222", "3", "4"});
		serializer.setElementsCanBeNull(false);
		roundTrip(kryo, 14, new String[] {"11", "2222", "3", "4"});
		serializer.setLength(4);
		roundTrip(kryo, 13, new String[] {"11", "2222", "3", "4"});
	}

	public void testMapSerializer () {
		Kryo kryo = new Kryo();
		MapSerializer serializer = new MapSerializer(kryo);
		kryo.register(HashMap.class, serializer);
		kryo.register(LinkedHashMap.class, serializer);
		HashMap map = new HashMap();
		map.put("123", "456");
		map.put("789", "abc");
		roundTrip(kryo, 22, map);
		roundTrip(kryo, 2, new LinkedHashMap());
		roundTrip(kryo, 22, new LinkedHashMap(map));
		serializer.setKeyClass(String.class);
		serializer.setKeysCanBeNull(false);
		serializer.setValueClass(String.class);
		roundTrip(kryo, 20, map);
		serializer.setValuesCanBeNull(false);
		roundTrip(kryo, 18, map);
	}

	public void testDateSerializer () {
		Kryo kryo = new Kryo();
		kryo.register(Date.class, new DateSerializer());
		roundTrip(kryo, 2, new Date(0));
		roundTrip(kryo, 4, new Date(1234567));
		roundTrip(kryo, 11, new Date(-1234567));
	}

	public void testBigDecimalSerializer () {
		roundTrip(new BigDecimalSerializer(), 5, BigDecimal.valueOf(12345, 2));
	}

	public void testBigIntegerSerializer () {
		roundTrip(new BigIntegerSerializer(), 8, BigInteger.valueOf(1270507903945L));
	}

	public void testBooleanSerializer () {
		roundTrip(new BooleanSerializer(), 2, true);
		roundTrip(new BooleanSerializer(), 2, false);
	}

	public void testByteSerializer () {
		roundTrip(new ByteSerializer(), 2, (byte)1);
		roundTrip(new ByteSerializer(), 2, (byte)125);
		roundTrip(new ByteSerializer(), 2, (byte)-125);
	}

	public void testCharSerializer () {
		roundTrip(new CharSerializer(), 3, 'a');
		roundTrip(new CharSerializer(), 3, 'z');
	}

	public void testDoubleSerializer () {
		roundTrip(new DoubleSerializer(), 9, 0d);
		roundTrip(new DoubleSerializer(), 9, 1234d);
		roundTrip(new DoubleSerializer(), 9, 1234.5678d);
	}

	public enum TestEnum {
		a, b, c
	}

	public void testEnumSerializer () {
		roundTrip(new EnumSerializer(TestEnum.class), 2, TestEnum.a);
		roundTrip(new EnumSerializer(TestEnum.class), 2, TestEnum.b);
		roundTrip(new EnumSerializer(TestEnum.class), 2, TestEnum.c);
	}

	public void testFloatSerializer () {
		roundTrip(new FloatSerializer(), 5, 0f);
		roundTrip(new FloatSerializer(), 5, 123f);
		roundTrip(new FloatSerializer(), 5, 123.456f);
	}

	public void testIntSerializer () {
		roundTrip(new IntSerializer(true), 2, 0);
		roundTrip(new IntSerializer(false), 2, 0);
		roundTrip(new IntSerializer(true), 2, 63);
		roundTrip(new IntSerializer(false), 2, 63);
		roundTrip(new IntSerializer(true), 2, 64);
		roundTrip(new IntSerializer(false), 3, 64);
		roundTrip(new IntSerializer(true), 2, 127);
		roundTrip(new IntSerializer(false), 3, 127);
		roundTrip(new IntSerializer(true), 3, 128);
		roundTrip(new IntSerializer(false), 3, 128);
		roundTrip(new IntSerializer(true), 3, 8191);
		roundTrip(new IntSerializer(false), 3, 8191);
		roundTrip(new IntSerializer(true), 3, 8192);
		roundTrip(new IntSerializer(false), 4, 8192);
		roundTrip(new IntSerializer(true), 3, 16383);
		roundTrip(new IntSerializer(false), 4, 16383);
		roundTrip(new IntSerializer(true), 4, 16384);
		roundTrip(new IntSerializer(false), 4, 16384);
		roundTrip(new IntSerializer(true), 4, 2097151);
		roundTrip(new IntSerializer(false), 5, 2097151);
		roundTrip(new IntSerializer(true), 4, 1048575);
		roundTrip(new IntSerializer(false), 4, 1048575);
		roundTrip(new IntSerializer(true), 5, 134217727);
		roundTrip(new IntSerializer(false), 5, 134217727);
		roundTrip(new IntSerializer(true), 5, 268435455);
		roundTrip(new IntSerializer(false), 6, 268435455);
		roundTrip(new IntSerializer(true), 5, 134217728);
		roundTrip(new IntSerializer(false), 6, 134217728);
		roundTrip(new IntSerializer(true), 6, 268435456);
		roundTrip(new IntSerializer(false), 6, 268435456);
		roundTrip(new IntSerializer(false), 2, -64);
		roundTrip(new IntSerializer(true), 6, -64);
		roundTrip(new IntSerializer(false), 3, -65);
		roundTrip(new IntSerializer(true), 6, -65);
		roundTrip(new IntSerializer(false), 3, -8192);
		roundTrip(new IntSerializer(true), 6, -8192);
		roundTrip(new IntSerializer(false), 4, -1048576);
		roundTrip(new IntSerializer(true), 6, -1048576);
		roundTrip(new IntSerializer(false), 5, -134217728);
		roundTrip(new IntSerializer(true), 6, -134217728);
		roundTrip(new IntSerializer(false), 6, -134217729);
		roundTrip(new IntSerializer(true), 6, -134217729);
	}

	public void testLongSerializer () {
		roundTrip(new LongSerializer(true), 2, 0l);
		roundTrip(new LongSerializer(false), 2, 0l);
		roundTrip(new LongSerializer(true), 2, 63l);
		roundTrip(new LongSerializer(false), 2, 63l);
		roundTrip(new LongSerializer(true), 2, 64l);
		roundTrip(new LongSerializer(false), 3, 64l);
		roundTrip(new LongSerializer(true), 2, 127l);
		roundTrip(new LongSerializer(false), 3, 127l);
		roundTrip(new LongSerializer(true), 3, 128l);
		roundTrip(new LongSerializer(false), 3, 128l);
		roundTrip(new LongSerializer(true), 3, 8191l);
		roundTrip(new LongSerializer(false), 3, 8191l);
		roundTrip(new LongSerializer(true), 3, 8192l);
		roundTrip(new LongSerializer(false), 4, 8192l);
		roundTrip(new LongSerializer(true), 3, 16383l);
		roundTrip(new LongSerializer(false), 4, 16383l);
		roundTrip(new LongSerializer(true), 4, 16384l);
		roundTrip(new LongSerializer(false), 4, 16384l);
		roundTrip(new LongSerializer(true), 4, 2097151l);
		roundTrip(new LongSerializer(false), 5, 2097151l);
		roundTrip(new LongSerializer(true), 4, 1048575l);
		roundTrip(new LongSerializer(false), 4, 1048575l);
		roundTrip(new LongSerializer(true), 5, 134217727l);
		roundTrip(new LongSerializer(false), 5, 134217727l);
		roundTrip(new LongSerializer(true), 5, 268435455l);
		roundTrip(new LongSerializer(false), 6, 268435455l);
		roundTrip(new LongSerializer(true), 5, 134217728l);
		roundTrip(new LongSerializer(false), 6, 134217728l);
		roundTrip(new LongSerializer(true), 6, 268435456l);
		roundTrip(new LongSerializer(false), 6, 268435456l);
		roundTrip(new LongSerializer(false), 2, -64l);
		roundTrip(new LongSerializer(true), 11, -64l);
		roundTrip(new LongSerializer(false), 3, -65l);
		roundTrip(new LongSerializer(true), 11, -65l);
		roundTrip(new LongSerializer(false), 3, -8192l);
		roundTrip(new LongSerializer(true), 11, -8192l);
		roundTrip(new LongSerializer(false), 4, -1048576l);
		roundTrip(new LongSerializer(true), 11, -1048576l);
		roundTrip(new LongSerializer(false), 5, -134217728l);
		roundTrip(new LongSerializer(true), 11, -134217728l);
		roundTrip(new LongSerializer(false), 6, -134217729l);
		roundTrip(new LongSerializer(true), 11, -134217729l);
	}

	public void testShortSerializer () {
		roundTrip(new ShortSerializer(true), 2, (short)0);
		roundTrip(new ShortSerializer(true), 2, (short)123);
		roundTrip(new ShortSerializer(false), 2, (short)123);
		roundTrip(new ShortSerializer(true), 4, (short)-123);
		roundTrip(new ShortSerializer(false), 2, (short)-123);
		roundTrip(new ShortSerializer(false), 4, (short)250);
		roundTrip(new ShortSerializer(true), 2, (short)250);
		roundTrip(new ShortSerializer(true), 2, (short)123);
		roundTrip(new ShortSerializer(true), 4, (short)400);
	}
}
