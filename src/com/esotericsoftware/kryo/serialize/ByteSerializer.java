
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Serializer;

/**
 * Writes 1 byte.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class ByteSerializer extends Serializer {
	public Byte readObjectData (ByteBuffer buffer, Class type) {
		byte b = buffer.get();
		if (TRACE) trace("kryo", "Read byte: " + b);
		return b;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		buffer.put((Byte)object);
		if (TRACE) trace("kryo", "Wrote byte: " + object);
	}

	/**
	 * Writes the specified non-negative int to the buffer, cast as a byte.
	 */
	static public void putUnsigned (ByteBuffer buffer, int value) {
		if (value < 0) throw new IllegalArgumentException("value cannot be less than zero: " + value);
		buffer.put((byte)value);
	}

	/**
	 * Reads a non-negative byte from the buffer that was written with {@link #putUnsigned(ByteBuffer, int)}.
	 */
	static public int getUnsigned (ByteBuffer buffer) {
		byte value = buffer.get();
		if (value < 0) return value + 256;
		return value;
	}
}
