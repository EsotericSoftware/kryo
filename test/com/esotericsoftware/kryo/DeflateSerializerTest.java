
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.kryo.serializers.DeflateSerializer;

/** @author Nathan Sweet <misc@n4te.com> */
public class DeflateSerializerTest extends KryoTestCase {
	public void testZip () {
		kryo.register(String.class, new DeflateSerializer(new StringSerializer()));
		roundTrip(13, 13,  "abcdefabcdefabcdefabcdefabcdefabcdefabcdef");
	}
}
