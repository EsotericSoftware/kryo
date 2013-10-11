
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Serializer;

/**
 * Serializes instances of {@link BigInteger}.
 * @author Joe Jensen (joe.m.jensen@gmail.com)
 */
public class BigIntegerSerializer extends Serializer {
	public BigInteger readObjectData (ByteBuffer buffer, Class type) {
		int length = IntSerializer.get(buffer, true);
		byte[] bytes = new byte[length];
		buffer.get(bytes, 0, length);
		BigInteger value = new BigInteger(bytes);
		if (TRACE) trace("kryo", "Read BigInteger: " + value);
		return value;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		BigInteger value = (BigInteger)object;
		byte[] bytes = value.toByteArray();
		IntSerializer.put(buffer, bytes.length, true);
		buffer.put(bytes);
		if (TRACE) trace("kryo", "Wrote BigInteger: " + value);
	}
}
