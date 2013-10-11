
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.SerializationException;
import com.esotericsoftware.kryo.Serializer;

/**
 * Writes a 1-10 byte long.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class LongSerializer extends Serializer {
	private boolean optimizePositive;

	public LongSerializer () {
	}

	public LongSerializer (boolean optimizePositive) {
		this.optimizePositive = optimizePositive;
	}

	public Long readObjectData (ByteBuffer buffer, Class type) {
		long i = get(buffer, optimizePositive);
		if (TRACE) trace("kryo", "Read long: " + i);
		return i;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		put(buffer, (Long)object, optimizePositive);
		if (TRACE) trace("kryo", "Wrote long: " + object);
	}

	/**
	 * Writes the specified long to the buffer using 1 to 10 bytes, depending on the size of the number.
	 * @param optimizePositive See {@link #LongSerializer(boolean)}.
	 * @return the number of bytes written.
	 */
	static public int put (ByteBuffer buffer, long value, boolean optimizePositive) {
		if (!optimizePositive) value = (value << 1) ^ (value >> 63);
		if ((value & ~0x7FL) == 0) {
			buffer.put((byte)value);
			return 1;
		}
		buffer.put((byte)(((int)value & 0x7F) | 0x80));
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			buffer.put((byte)value);
			return 2;
		}
		buffer.put((byte)(((int)value & 0x7F) | 0x80));
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			buffer.put((byte)value);
			return 3;
		}
		buffer.put((byte)(((int)value & 0x7F) | 0x80));
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			buffer.put((byte)value);
			return 4;
		}
		buffer.put((byte)(((int)value & 0x7F) | 0x80));
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			buffer.put((byte)value);
			return 5;
		}
		buffer.put((byte)(((int)value & 0x7F) | 0x80));
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			buffer.put((byte)value);
			return 6;
		}
		buffer.put((byte)(((int)value & 0x7F) | 0x80));
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			buffer.put((byte)value);
			return 7;
		}
		buffer.put((byte)(((int)value & 0x7F) | 0x80));
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			buffer.put((byte)value);
			return 8;
		}
		buffer.put((byte)(((int)value & 0x7F) | 0x80));
		value >>>= 7;
		if ((value & ~0x7FL) == 0) {
			buffer.put((byte)value);
			return 9;
		}
		buffer.put((byte)(((int)value & 0x7F) | 0x80));
		value >>>= 7;
		buffer.put((byte)value);
		return 10;
	}

	/**
	 * Reads a long from the buffer that was written with {@link #put(ByteBuffer, long, boolean)}.
	 */
	static public long get (ByteBuffer buffer, boolean optimizePositive) {
		long result = 0;
		for (int offset = 0; offset < 64; offset += 7) {
			byte b = buffer.get();
			result |= (long)(b & 0x7F) << offset;
			if ((b & 0x80) == 0) {
				if (!optimizePositive) result = (result >>> 1) ^ -(result & 1);
				return result;
			}
		}
		throw new SerializationException("Malformed long.");
	}
}
