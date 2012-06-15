
package com.esotericsoftware.kryo;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import junit.framework.Assert;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.MapSerializer;

/** @author Nathan Sweet <misc@n4te.com> */
public class MapSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	public void testMaps () {
		kryo.register(HashMap.class);
		kryo.register(LinkedHashMap.class);
		HashMap map = new HashMap();
		map.put("123", "456");
		map.put("789", "abc");
		roundTrip(18, map);
		roundTrip(2, new LinkedHashMap());
		roundTrip(18, new LinkedHashMap(map));

		MapSerializer serializer = new MapSerializer();
		kryo.register(HashMap.class, serializer);
		kryo.register(LinkedHashMap.class, serializer);
		serializer.setKeyClass(String.class, kryo.getSerializer(String.class));
		serializer.setKeysCanBeNull(false);
		serializer.setValueClass(String.class, kryo.getSerializer(String.class));
		roundTrip(14, map);
		serializer.setValuesCanBeNull(false);
		roundTrip(14, map);
	}

	public void testEmptyHashMap () {
		execute(new HashMap<Object, Object>(), 0);
	}

	public void testNotEmptyHashMap () {
		execute(new HashMap<Object, Object>(), 1000);
	}

	public void testEmptyConcurrentHashMap () {
		execute(new ConcurrentHashMap<Object, Object>(), 0);
	}

	public void testNotEmptyConcurrentHashMap () {
		execute(new ConcurrentHashMap<Object, Object>(), 1000);
	}

	public void testGenerics () {
		kryo.register(HasGenerics.class);
		kryo.register(Integer[].class);
		kryo.register(HashMap.class);

		HasGenerics test = new HasGenerics();
		test.map.put("moo", new Integer[] {1, 2});

		output = new Output(4096);
		kryo.writeClassAndObject(output, test);
		output.flush();

		input = new Input(output.toBytes());
		HasGenerics test2 = (HasGenerics)kryo.readClassAndObject(input);
		assertEquals(test.map.get("moo"), test2.map.get("moo"));
	}

	private void execute (Map<Object, Object> map, int inserts) {
		Random random = new Random();
		for (int i = 0; i < inserts; i++)
			map.put(random.nextLong(), random.nextBoolean());

		Kryo kryo = new Kryo();
		kryo.register(HashMap.class, new MapSerializer());
		kryo.register(ConcurrentHashMap.class, new MapSerializer());

		Output output = new Output(2048, -1);
		kryo.writeClassAndObject(output, map);
		output.close();

		Input input = new Input(output.toBytes());
		Object deserialized = kryo.readClassAndObject(input);
		input.close();

		Assert.assertEquals(map, deserialized);
	}

	static public class HasGenerics {
		public HashMap<String, Integer[]> map = new HashMap();
		public HashMap<String, ?> map2 = new HashMap();
	}
}
