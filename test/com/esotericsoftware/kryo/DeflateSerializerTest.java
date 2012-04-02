
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.kryo.serializers.DeflateSerializer;

public class DeflateSerializerTest extends KryoTestCase {
	public void testZip () {
		kryo.register(String.class, new DeflateSerializer(new StringSerializer()));
		roundTrip(12, "abcdefabcdefabcdefabcdefabcdefabcdefabcdef");
	}
}
