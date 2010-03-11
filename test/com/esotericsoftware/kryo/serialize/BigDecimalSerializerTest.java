
package com.esotericsoftware.kryo.serialize;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import junit.framework.TestCase;

/**
 * @author Joe Jensen (joe.m.jensen@gmail.com)
 */
public class BigDecimalSerializerTest extends TestCase {
	public void testReadObjectData () {
		BigDecimal in = BigDecimal.valueOf(12345L, 2);
		BigDecimalSerializer serializer = new BigDecimalSerializer();
		ByteBuffer buffer = ByteBuffer.allocate(256);

		serializer.writeObjectData(buffer, in);
		buffer.rewind();
		BigDecimal out = serializer.readObjectData(buffer, BigDecimal.class);

		assertEquals(in, out);
	}

	public void testWriteObjectData () {
		BigDecimal in = BigDecimal.valueOf(-54321L, -1);
		BigDecimalSerializer serializer = new BigDecimalSerializer();
		ByteBuffer buffer = ByteBuffer.allocate(256);

		serializer.writeObjectData(buffer, in);
		buffer.rewind();
		BigDecimal out = serializer.readObjectData(buffer, BigDecimal.class);

		assertEquals(in, out);
	}
}
