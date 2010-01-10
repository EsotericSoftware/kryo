
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.trace;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.SerializationException;
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
	 * @param optimizePositive Determines how many bytes are written to serialize various ranges of integers:
	 *           <table cellpadding=3>
	 *           <tr>
	 *           <td><b>Bytes</td>
	 *           <td><b>true</td>
	 *           <td><b>false</td>
	 *           </tr>
	 *           <tr>
	 *           <td>1</td>
	 *           <td>0 <= value <= 127</td>
	 *           <td>-64 <= value <= 63</td>
	 *           </tr>
	 *           <tr>
	 *           <td>2</td>
	 *           <td>128 <= value <= 16383</td>
	 *           <td>-8192 <= value <= 8191</td>
	 *           </tr>
	 *           <tr>
	 *           <td>3</td>
	 *           <td>16384 <= value <= 2097151</td>
	 *           <td>-1048576 <= value <= 1048575</td>
	 *           </tr>
	 *           <tr>
	 *           <td>4</td>
	 *           <td>2097152 <= value <= 268435455</td>
	 *           <td>-134217728 <= value <= 134217727</td>
	 *           </tr>
	 *           <tr>
	 *           <td>5</td>
	 *           <td>value < 0 || value > 268435455</td>
	 *           <td>value < -134217728 || value > 134217727</td>
	 *           </tr>
	 *           </table>
	 */
	public IntSerializer (boolean optimizePositive) {
		this.optimizePositive = optimizePositive;
	}

	public Integer readObjectData (ByteBuffer buffer, Class type) {
		int i = get(buffer, optimizePositive);
		if (TRACE) trace("kryo", "Read int: " + i);
		return i;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		put(buffer, (Integer)object, optimizePositive);
		if (TRACE) trace("kryo", "Wrote int: " + object);
	}

	/**
	 * Writes the specified int to the buffer using 1 to 5 bytes, depending on the size of the number.
	 * @param optimizePositive See {@link #IntSerializer(boolean)}.
	 * @return the number of bytes written.
	 */
	static public int put (ByteBuffer buffer, int value, boolean optimizePositive) {
		if (!optimizePositive) value = (value << 1) ^ (value >> 31);
		if ((value & ~0x7F) == 0) {
			buffer.put((byte)value);
			return 1;
		}
		buffer.put((byte)((value & 0x7F) | 0x80));
		value >>>= 7;
		if ((value & ~0x7F) == 0) {
			buffer.put((byte)value);
			return 2;
		}
		buffer.put((byte)((value & 0x7F) | 0x80));
		value >>>= 7;
		if ((value & ~0x7F) == 0) {
			buffer.put((byte)value);
			return 3;
		}
		buffer.put((byte)((value & 0x7F) | 0x80));
		value >>>= 7;
		if ((value & ~0x7F) == 0) {
			buffer.put((byte)value);
			return 4;
		}
		buffer.put((byte)((value & 0x7F) | 0x80));
		value >>>= 7;
		buffer.put((byte)value);
		return 5;
	}

	/**
	 * Reads an int from the buffer that was written with {@link #put(ByteBuffer, int, boolean)}.
	 */
	static public int get (ByteBuffer buffer, boolean optimizePositive) {
		for (int offset = 0, result = 0; offset < 32; offset += 7) {
			int b = buffer.get();
			result |= (b & 0x7F) << offset;
			if ((b & 0x80) == 0) {
				if (!optimizePositive) result = (result >>> 1) ^ -(result & 1);
				return result;
			}
		}
		throw new SerializationException("Malformed integer.");
	}

	/**
	 * Reads true if the buffer contains enough data to read an int that was written with {@link #put(ByteBuffer, int, boolean)}.
	 */
	static public boolean canRead (ByteBuffer buffer, boolean optimizePositive) {
		int position = buffer.position();
		try {
			int remaining = buffer.remaining();
			int offset = 0, result = 0;
			for (; offset < 32 && remaining > 0; offset += 7, remaining--) {
				byte b = buffer.get();
				result |= (b & 0x7f) << offset;
				if ((b & 0x80) == 0) return true;
			}
			return false;
		} finally {
			buffer.position(position);
		}
	}
}
