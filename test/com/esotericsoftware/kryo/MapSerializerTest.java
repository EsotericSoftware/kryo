
package com.esotericsoftware.kryo;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.esotericsoftware.kryo.serializers.MapSerializer;

public class MapSerializerTest extends KryoTestCase {
	public void testMaps () {
		kryo.register(HashMap.class);
		kryo.register(LinkedHashMap.class);
		HashMap map = new HashMap();
		map.put("123", "456");
		map.put("789", "abc");
		roundTrip(22, map);
		roundTrip(2, new LinkedHashMap());
		roundTrip(22, new LinkedHashMap(map));

		MapSerializer serializer = new MapSerializer(kryo);
		kryo.register(HashMap.class, serializer);
		kryo.register(LinkedHashMap.class, serializer);
		serializer.setKeyClass(String.class);
		serializer.setKeysCanBeNull(false);
		serializer.setValueClass(String.class);
		roundTrip(20, map);
		serializer.setValuesCanBeNull(false);
		roundTrip(18, map);
	}
}
