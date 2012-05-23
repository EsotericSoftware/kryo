
package com.esotericsoftware.kryo;

import java.util.HashMap;
import java.util.LinkedHashMap;

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
}
