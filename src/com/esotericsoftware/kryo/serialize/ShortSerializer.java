
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.log.Log.*;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Serializer;

/**
 * Writes a 1-3 byte short.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class ShortSerializer extends Serializer {
	private boolean optimizePositive = true;

	public ShortSerializer () {
	}

	/**
	 * @param optimizePositive If true, writes 1 byte if 0 <= value <= 254 and 3 bytes otherwise (default). If false, writes 1 byte
	 *           if -127 <= value <= 127 and 3 bytes otherwise. If null, 2 bytes are always written.
	 */
	public ShortSerializer (Boolean optimizePositive) {
		this.optimizePositive = optimizePositive;
	}

	public Short readObjectData (ByteBuffer buffer, Class type) {
		short s = get(buffer, optimizePositive);
		if (level <= TRACE) trace("kryo", "Read short: " + s);
		return s;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		put(buffer, (Short)object, optimizePositive);
		if (level <= TRACE) trace("kryo", "Wrote short: " + object);
	}

	static private final byte SHORT = -128;
	static private final byte SHORT_POSITIVE = -1;

	/**
	 * Writes the specified short to the buffer as a byte or short depending on the size of the number.
	 * @param optimizePositive If true, writes 1 byte if 0 <= value <= 254 and 3 bytes otherwise. If false, writes 1 byte if -127
	 *           <= value <= 127 and 3 bytes otherwise.
	 * @return the number of bytes written.
	 */
	static public short put (ByteBuffer buffer, short value, boolean optimizePositive) {
		if (optimizePositive) {
			if (value >= 0 && value <= 254) {
				buffer.put((byte)value);
				return 1;
			}
			buffer.put(SHORT_POSITIVE);
			buffer.putShort(value);
		} else {
			if (value >= -127 && value <= 127) {
				buffer.put((byte)value);
				return 1;
			}
			buffer.put(SHORT);
			buffer.putShort(value);
		}
		return 3;
	}

	/**
	 * Reads a short from the buffer that was written with {@link #put(ByteBuffer, short, boolean)}.
	 */
	static public short get (ByteBuffer buffer, boolean optimizePositive) {
		byte value = buffer.get();
		if (optimizePositive) {
			if (value == SHORT_POSITIVE) return buffer.getShort();
			if (value < 0) return (short)(value + 256);
		} else {
			if (value == SHORT) return buffer.getShort();
		}
		return value;
	}
}
