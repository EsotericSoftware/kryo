
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.log.Log.*;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Serializer;

/**
 * Writes a 1-5 byte integer.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class IntSerializer extends Serializer {
	private boolean optimizePositive = true;

	public IntSerializer () {
	}

	/**
	 * @param optimizePositive If true, writes 1 byte if 0 <= value <= 253, 3 bytes if 0 <= value <= 65536, and 5 bytes otherwise
	 *           (default). If false, writes 1 byte if -126 <= value <= 127, 3 bytes if -32768 <= value <= 32767, and 5 bytes
	 *           otherwise. If null, 4 bytes are always written.
	 */
	public IntSerializer (Boolean optimizePositive) {
		this.optimizePositive = optimizePositive;
	}

	public Integer readObjectData (ByteBuffer buffer, Class type) {
		int i = get(buffer, optimizePositive);
		if (level <= TRACE) trace("kryo", "Read int: " + i);
		return i;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		put(buffer, (Integer)object, optimizePositive);
		if (level <= TRACE) trace("kryo", "Wrote int: " + object);
	}

	static private final byte SHORT = -128;
	static private final byte INT = -127;
	static private final byte SHORT_POSITIVE = -1;
	static private final byte INT_POSITIVE = -2;

	/**
	 * Writes the specified int to the buffer as a byte, short, or int depending on the size of the number.
	 * @param optimizePositive If true, writes 1 byte if 0 <= value <= 253, 3 bytes if 0 <= value <= 65536, and 5 bytes otherwise.
	 *           If false, writes 1 byte if -126 <= value <= 127, 3 bytes if -32768 <= value <= 32767, and 5 bytes otherwise.
	 * @return the number of bytes written.
	 */
	static public int put (ByteBuffer buffer, int value, boolean optimizePositive) {
		if (optimizePositive) {
			if (value >= 0 && value <= 253) {
				buffer.put((byte)value);
				return 1;
			} else if (value >= 0 && value < 65536) {
				buffer.put(SHORT_POSITIVE);
				buffer.putShort((short)value);
				return 3;
			}
			buffer.put(INT_POSITIVE);
			buffer.putInt(value);
		} else {
			if (value >= -126 && value <= 127) {
				buffer.put((byte)value);
				return 1;
			} else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
				buffer.put(SHORT);
				buffer.putShort((short)value);
				return 3;
			}
			buffer.put(INT);
			buffer.putInt(value);
		}
		return 5;
	}

	/**
	 * Reads an int from the buffer that was written with {@link #put(ByteBuffer, int, boolean)}.
	 */
	static public int get (ByteBuffer buffer, boolean optimizePositive) {
		byte value = buffer.get();
		if (optimizePositive) {
			switch (value) {
			case SHORT_POSITIVE:
				int shortValue = buffer.getShort();
				if (shortValue < 0) return shortValue + 65536;
				return shortValue;
			case INT_POSITIVE:
				return buffer.getInt();
			}
			if (value < 0) return value + 256;
		} else {
			switch (value) {
			case SHORT:
				return buffer.getShort();
			case INT:
				return buffer.getInt();
			}
		}
		return value;
	}

	/**
	 * Reads true if the buffer contains enough data to read an int that was written with {@link #put(ByteBuffer, int, boolean)}.
	 */
	static public boolean canRead (ByteBuffer buffer, boolean optimizePositive) {
		int position = buffer.position();
		byte value = buffer.get();
		buffer.position(position);
		if (optimizePositive) {
			switch (value) {
			case SHORT_POSITIVE:
				return buffer.remaining() >= 2;
			case INT_POSITIVE:
				return buffer.remaining() >= 4;
			}
		} else {
			switch (value) {
			case SHORT:
				return buffer.remaining() >= 2;
			case INT:
				return buffer.remaining() >= 4;
			}
		}
		return true;
	}
}
