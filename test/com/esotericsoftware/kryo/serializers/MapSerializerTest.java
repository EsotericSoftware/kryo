/* Copyright (c) 2008-2026, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryo.serializers;

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.Test;

/** @author Nathan Sweet */
@SuppressWarnings("synthetic-access")
class MapSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	// https://github.com/EsotericSoftware/kryo/issues/1157
	@Test
	void testLinkedHashMapAccessOrder () {
		kryo.register(LinkedHashMap.class, new LinkedHashMapSerializer());
		kryo.register(LruCache.class, new LinkedHashMapSerializer());
		for (boolean accessOrder : new boolean[] {true, false}) {
			LinkedHashMap<String, Integer> map = new LinkedHashMap<>(16, 0.75f, accessOrder);
			map.put("a", 1);
			map.put("b", 2);
			map.put("c", 3);
			map.get("a"); // With access order, "a" is moved to the end.
			for (LinkedHashMap<String, Integer> result : List.of(writeRead(map), kryo.copy(map))) {
				assertEquals(new ArrayList<>(map.keySet()), new ArrayList<>(result.keySet()));
				result.get("b");
				assertEquals(accessOrder ? List.of("c", "a", "b") : List.of("a", "b", "c"), new ArrayList<>(result.keySet()));
			}

			LinkedHashMap<String, Integer> empty = writeRead(new LinkedHashMap<>(16, 0.75f, accessOrder));
			empty.put("x", 1);
			empty.put("y", 2);
			empty.get("x");
			assertEquals(accessOrder ? List.of("y", "x") : List.of("x", "y"), new ArrayList<>(empty.keySet()));
		}

		// A subclass sets the access order in its constructor.
		LruCache cache = new LruCache();
		cache.put("a", 1);
		cache.put("b", 2);
		LruCache read = writeRead(cache);
		read.get("a");
		assertEquals(List.of("b", "a"), new ArrayList<>(read.keySet()));
	}

	private <T> T writeRead (T object) {
		Output output = new Output(1024);
		kryo.writeObject(output, object);
		return (T)kryo.readObject(new Input(output.toBytes()), object.getClass());
	}

	public static class LruCache extends LinkedHashMap<String, Integer> {
		public LruCache () {
			super(16, 0.75f, true);
		}
	}

	@Test
	void testSizeChanged () {
		// The size doesn't match the entries, eg because the map was modified concurrently (#1181).
		kryo.register(WrongSizeMap.class);
		for (int sizeDelta : new int[] {-1, 1}) {
			WrongSizeMap map = new WrongSizeMap(sizeDelta);
			map.put("a", 1);
			map.put("b", "c");
			KryoException ex = assertThrows(KryoException.class, () -> kryo.writeObject(new Output(1024), map));
			assertTrue(ex.getMessage().contains("changed while it was written"), ex.getMessage());
		}
	}

	public static class WrongSizeMap extends HashMap<Object, Object> {
		final int sizeDelta;

		public WrongSizeMap () {
			this(0);
		}

		public WrongSizeMap (int sizeDelta) {
			this.sizeDelta = sizeDelta;
		}

		public int size () {
			return super.size() + sizeDelta;
		}
	}

	@Test
	void testMaliciousMapSize () {
		kryo.setReferences(false);
		kryo.register(HashMap.class);
		Output declared = new Output(8);
		kryo.writeClass(declared, HashMap.class);
		declared.writeVarIntFlag(false, 2000000001, true);
		declared.flush();
		assertThrows(KryoException.class, () -> kryo.readClassAndObject(new Input(declared.toBytes())));
	}

	@Test
	void testMaxMapSize () {
		kryo.setReferences(false);
		kryo.register(HashMap.class);
		Output declared = new Output(8);
		kryo.writeClass(declared, HashMap.class);
		declared.writeVarIntFlag(false, 2000000001, true);
		declared.flush();
		Input stream = new Input(new ByteArrayInputStream(declared.toBytes()));
		stream.setMaxArraySize(1024);
		assertThrows(KryoException.class, () -> kryo.readClassAndObject(stream));
	}

	@Test
	void testNullMarkers () {
		// The key and value serializers are known from the generic type of the field. Keys and values are written without a null
		// marker if the map contains no null key or value, which is written with the size.
		for (boolean references : new boolean[] {false, true}) {
			for (int size : new int[] {3, 100, 200}) {
				TypedMap object = new TypedMap();
				for (int i = 0; i < size; i++)
					object.map.put("key" + i, i);
				byte[] bytes = writeTypedMap(object, references, true);
				// The format of Kryo 5 has a null marker for each value, and for each key with references.
				int markers = references ? size * 2 : size;
				// The flag takes one bit of the first byte of the size, which then needs a second byte from 63 entries instead of
				// from 127 entries.
				int flag = size >= 63 && size < 127 ? 1 : 0;
				assertEquals(writeTypedMap(object, references, false).length - markers + flag, bytes.length);

				object.map.put("null", null);
				writeTypedMap(object, references, true);
				object.map.remove("null");
				object.map.put(null, 5);
				writeTypedMap(object, references, true);
			}
		}
	}

	/** Returns the bytes after checking that they are read as the same map. */
	private byte[] writeTypedMap (TypedMap object, boolean references, boolean writeSameClassOnce) {
		Kryo kryo = new Kryo();
		kryo.setReferences(references);
		kryo.register(TypedMap.class);
		MapSerializer serializer = new MapSerializer();
		serializer.setWriteSameClassOnce(writeSameClassOnce);
		kryo.register(HashMap.class, serializer);
		Output output = new Output(64, -1);
		kryo.writeObject(output, object);
		assertEquals(object.map, kryo.readObject(new Input(output.toBytes()), TypedMap.class).map);
		return output.toBytes();
	}

	public static class TypedMap {
		public HashMap<String, Integer> map = new HashMap();
	}

	@Test
	void testKeyAndValueClassesWrittenOnce () {
		kryo.register(LinkedHashMap.class);
		// The class of the keys and values is written once if they all have the same class.
		LinkedHashMap map = new LinkedHashMap();
		map.put("a", 1);
		map.put("b", 2);
		roundTrip(10, map);
		// Otherwise each key or value is written with its class.
		map.put("c", "3");
		roundTrip(17, map);
		map.put("c", null);
		roundTrip(15, map);
		map.put(null, 3);
		roundTrip(21, map);
		LinkedHashMap nullValues = new LinkedHashMap();
		nullValues.put("a", null);
		roundTrip(7, nullValues);
	}

	@Test
	void testWriteSameClassOnceDisabled () {
		MapSerializer serializer = new MapSerializer();
		assertTrue(serializer.getWriteSameClassOnce());
		serializer.setWriteSameClassOnce(false);
		assertFalse(serializer.getWriteSameClassOnce());
		kryo.register(LinkedHashMap.class, serializer);
		LinkedHashMap map = new LinkedHashMap();
		map.put("a", 1);
		map.put("b", 2);
		roundTrip(12, map);
	}

	@Test
	void testMaps () {
		kryo.register(HashMap.class);
		kryo.register(LinkedHashMap.class);
		HashMap map = new HashMap();
		map.put("123", "456");
		map.put("789", "abc");
		roundTrip(16, map);
		roundTrip(2, new LinkedHashMap());
		roundTrip(16, new LinkedHashMap(map));

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

	@Test
	void testEnumMap () {
		kryo.register(SomeEnum.class);
		kryo.register(EnumMap.class, new EnumMapSerializer(SomeEnum.class));

		roundTrip(2, new EnumMap(SomeEnum.class));

		EnumMap map = new EnumMap(SomeEnum.class);
		map.put(SomeEnum.b, "b");
		roundTrip(7, map);
	}

	@Test
	void testEmptyHashMap () {
		execute(new HashMap(), 0);
	}

	@Test
	void testNotEmptyHashMap () {
		execute(new HashMap(), 1000);
	}

	@Test
	void testEmptyConcurrentHashMap () {
		execute(new ConcurrentHashMap(), 0);
	}

	@Test
	void testNotEmptyConcurrentHashMap () {
		execute(new ConcurrentHashMap(), 1000);
	}

	@Test
	void testGenerics () {
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
		assertArrayEquals(test.map.get("moo"), test2.map.get("moo"));
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

		assertEquals(map, deserialized);
	}

    @Test
    void testConcurrentSkipListMapSerializer() {
        ConcurrentSkipListMap map = new ConcurrentSkipListMap();
        kryo.register(ConcurrentSkipListMap.class);
        map.put(9, "456");
        map.put(3, "abc");
        map.put(1, 122);
        roundTrip(19, map);

        kryo.register(KeyThatIsntComparable.class);
        kryo.register(KeyComparator.class);
        ConcurrentSkipListMap cMap = new ConcurrentSkipListMap<>(new KeyComparator());
        KeyThatIsntComparable key1 = new KeyThatIsntComparable();
        KeyThatIsntComparable key2 = new KeyThatIsntComparable();
        key1.value = "311";
        cMap.put(key1, "257");
        key2.value = "213";
        cMap.put(key2, "455");
        roundTrip(17, cMap);

        kryo.register(ConcurrentSkipListMapSubclass.class);
        ConcurrentSkipListMapSubclass cSubMap = new ConcurrentSkipListMapSubclass();
        cSubMap.put("1", 77);
        cSubMap.put("2", 68);
        cSubMap.put("3", 63);
        cSubMap.put("4", 22);
        roundTrip(19, cSubMap);
    }

	@Test
	void testTreeMap () {
		kryo.register(TreeMap.class);
		TreeMap map = new TreeMap();
		map.put("123", "456");
		map.put("789", "abc");
		roundTrip(17, map);

		kryo.register(KeyThatIsntComparable.class);
		kryo.register(KeyComparator.class);
		map = new TreeMap(new KeyComparator());
		KeyThatIsntComparable key1 = new KeyThatIsntComparable();
		KeyThatIsntComparable key2 = new KeyThatIsntComparable();
		key1.value = "123";
		map.put(key1, "456");
		key2.value = "1234";
		map.put(key2, "4567");
		roundTrip(19, map);

		kryo.register(TreeMapSubclass.class);
		map = new TreeMapSubclass();
		map.put("1", 47);
		map.put("2", 34);
		map.put("3", 65);
		map.put("4", 44);
		roundTrip(18, map);
	}

	@Test
	void testTreeMapWithReferences () {
		kryo.setReferences(true);
		kryo.register(TreeMap.class);
		TreeMap map = new TreeMap();
		map.put("123", "456");
		map.put("789", "abc");
		roundTrip(18, map);

		kryo.register(KeyThatIsntComparable.class);
		kryo.register(KeyComparator.class);
		map = new TreeMap(new KeyComparator());
		KeyThatIsntComparable key1 = new KeyThatIsntComparable();
		KeyThatIsntComparable key2 = new KeyThatIsntComparable();
		key1.value = "123";
		map.put(key1, "456");
		key2.value = "1234";
		map.put(key2, "4567");
		roundTrip(23, map);

		kryo.register(TreeMapSubclass.class);
		map = new TreeMapSubclass();
		map.put("1", 47);
		map.put("2", 34);
		map.put("3", 65);
		map.put("4", 44);
		roundTrip(19, map);
	}

	@Test
	void testSerializingMapAfterDeserializingMultipleReferencesToSameMap () {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		Output output = new Output(4096);

		kryo.writeClassAndObject(output, new HasMultipleReferenceToSameMap());
		kryo.readClassAndObject(new Input(new ByteArrayInputStream(output.getBuffer())));
		output.reset();

		Map<Integer, List<String>> mapOfLists = new HashMap();
		mapOfLists.put(1, new ArrayList());
		kryo.writeClassAndObject(output, mapOfLists);

		Map<Integer, List<String>> deserializedMap = (Map<Integer, List<String>>)kryo
			.readClassAndObject(new Input(new ByteArrayInputStream(output.getBuffer())));
		assertEquals(1, deserializedMap.size());
	}

	@Test
	void testArrayListKeys () {
		CollectionSerializer collectionSerializer = new CollectionSerializer();
		// Increase generics savings so difference is more easily seen.
		collectionSerializer.setElementsCanBeNull(false);

		kryo.register(ArrayListKeys.class);
		kryo.register(HashMap.class);
		kryo.register(ArrayList.class, collectionSerializer);

		ArrayListKeys object = new ArrayListKeys();
		object.map = new HashMap();
		object.map.put(new ArrayList(Arrays.asList(1, 2, 3)), new ArrayList(Arrays.asList("a", "b", "c")));
		roundTrip(16, object);
	}

	static class ArrayListKeys {
		private HashMap<ArrayList<Integer>, ArrayList<String>> map = new HashMap();

		public boolean equals (Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
	}

	static class HasMultipleReferenceToSameMap {
		private Map<Integer, String> mapOne = new HashMap();
		private Map<Integer, String> mapTwo = this.mapOne;
	}

	public static class HasGenerics {
		public HashMap<String, Integer[]> map = new HashMap();
		public HashMap<String, ?> map2 = new HashMap();
	}

	public static class KeyComparator implements Comparator<KeyThatIsntComparable> {
		public int compare (KeyThatIsntComparable o1, KeyThatIsntComparable o2) {
			return o1.value.compareTo(o2.value);
		}
	}

	public static class KeyThatIsntComparable {
		public String value;

		public KeyThatIsntComparable () {
		}

		public KeyThatIsntComparable (String value) {
			this.value = value;
		}
	}

	public static class TreeMapSubclass<K, V> extends TreeMap<K, V> {
		public TreeMapSubclass () {
		}

		public TreeMapSubclass (Comparator<? super K> comparator) {
			super(comparator);
		}
	}

    public static class ConcurrentSkipListMapSubclass<K, V> extends ConcurrentSkipListMap<K, V> {
        public ConcurrentSkipListMapSubclass() {}

        public ConcurrentSkipListMapSubclass(Comparator<? super K> comparator) {
            super(comparator);
        }
    }

	public static enum SomeEnum {
		a, b, c
	}
}
