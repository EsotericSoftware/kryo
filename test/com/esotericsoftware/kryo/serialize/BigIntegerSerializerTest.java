
package com.esotericsoftware.kryo.serialize;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;

import junit.framework.TestCase;

/**
 * @author Joe Jensen (joe.m.jensen@gmail.com)
 */
public class BigIntegerSerializerTest extends TestCase {
	public void testReadObjectData () {
		BigInteger in = BigInteger.probablePrime(128, new Random(System.currentTimeMillis()));
		BigIntegerSerializer serializer = new BigIntegerSerializer();
		ByteBuffer buffer = ByteBuffer.allocate(256);

		serializer.writeObjectData(buffer, in);
		buffer.rewind();
		BigInteger out = serializer.readObjectData(buffer, BigInteger.class);

		assertEquals(in, out);
	}

	public void testWriteObjectData () {
		BigInteger in = BigInteger.valueOf(987654321L);
		BigIntegerSerializer serializer = new BigIntegerSerializer();
		ByteBuffer buffer = ByteBuffer.allocate(256);

		serializer.writeObjectData(buffer, in);
		buffer.rewind();
		BigInteger out = serializer.readObjectData(buffer, BigInteger.class);

		assertEquals(in, out);
	}
}
