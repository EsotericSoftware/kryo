
package com.esotericsoftware.kryo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
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
		roundTrip(18, 21, map);
		roundTrip(2, 5, new LinkedHashMap());
		roundTrip(18, 21, new LinkedHashMap(map));

		MapSerializer serializer = new MapSerializer();
		kryo.register(HashMap.class, serializer);
		kryo.register(LinkedHashMap.class, serializer);
		serializer.setKeyClass(String.class, kryo.getSerializer(String.class));
		serializer.setKeysCanBeNull(false);
		serializer.setValueClass(String.class, kryo.getSerializer(String.class));
		roundTrip(14, 17, map);
		serializer.setValuesCanBeNull(false);
		roundTrip(14, 17, map);
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

	public void testTreeMap () {
		kryo.register(TreeMap.class);
		TreeMap map = new TreeMap();
		map.put("123", "456");
		map.put("789", "abc");
		roundTrip(19, 22, map);

		kryo.register(KeyThatIsntComparable.class);
		kryo.register(KeyComparator.class);
		map = new TreeMap(new KeyComparator());
		KeyThatIsntComparable key1 = new KeyThatIsntComparable();
		KeyThatIsntComparable key2 = new KeyThatIsntComparable();
		key1.value = "123";
		map.put(key1, "456");
		key2.value = "1234";
		map.put(key2, "4567");
		roundTrip(21, 24, map);

		kryo.register(TreeMapSubclass.class);
		map = new TreeMapSubclass<String, Integer>();
		map.put("1", 47);
		map.put("2", 34);
		map.put("3", 65);
		map.put("4", 44);
		roundTrip(24, 38, map);
	}

	public void testTreeMapWithReferences () {
		kryo.setReferences(true);
		kryo.register(TreeMap.class);
		TreeMap map = new TreeMap();
		map.put("123", "456");
		map.put("789", "abc");
		roundTrip(24, 27, map);

		kryo.register(KeyThatIsntComparable.class);
		kryo.register(KeyComparator.class);
		map = new TreeMap(new KeyComparator());
		KeyThatIsntComparable key1 = new KeyThatIsntComparable();
		KeyThatIsntComparable key2 = new KeyThatIsntComparable();
		key1.value = "123";
		map.put(key1, "456");
		key2.value = "1234";
		map.put(key2, "4567");
		roundTrip(29, 32, map);

		kryo.register(TreeMapSubclass.class);
		map = new TreeMapSubclass<String, Integer>();
		map.put("1", 47);
		map.put("2", 34);
		map.put("3", 65);
		map.put("4", 44);
		roundTrip(29, 43, map);
	}
	
	static public class HasGenerics {
		public HashMap<String, Integer[]> map = new HashMap();
		public HashMap<String, ?> map2 = new HashMap();
	}

	static public class KeyComparator implements Comparator<KeyThatIsntComparable> {
		public int compare (KeyThatIsntComparable o1, KeyThatIsntComparable o2) {
			return o1.value.compareTo(o2.value);
		}
	}

	static public class KeyThatIsntComparable {
		public String value;
		public KeyThatIsntComparable () {
		}
		public KeyThatIsntComparable (String value) {
			this.value = value;
		}
	}

	static public class TreeMapSubclass<K,V> extends TreeMap<K,V> {
		public TreeMapSubclass() {
		}
		public TreeMapSubclass(Comparator<? super K> comparator) {
			super(comparator);
		}
	}
}
