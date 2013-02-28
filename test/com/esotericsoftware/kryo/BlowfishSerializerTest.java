
package com.esotericsoftware.kryo;

import javax.crypto.KeyGenerator;

import com.esotericsoftware.kryo.serializers.BlowfishSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;

/** @author Nathan Sweet <misc@n4te.com> */
public class BlowfishSerializerTest extends KryoTestCase {
	public void testZip () throws Exception {
		byte[] key = KeyGenerator.getInstance("Blowfish").generateKey().getEncoded();
		kryo.register(String.class, new BlowfishSerializer(new StringSerializer(), key));
		roundTrip(49, 49,  "abcdefabcdefabcdefabcdefabcdefabcdefabcdef");
	}
}
