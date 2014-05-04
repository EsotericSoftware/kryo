
package com.esotericsoftware.kryo.io;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import sun.nio.ch.DirectBuffer;

import static com.esotericsoftware.kryo.util.UnsafeUtil.*;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.util.Util;

/** An optimized OutputStream that writes data directly into the off-heap memory. Utility methods are provided for efficiently
 * writing primitive types, arrays of primitive types and strings. It uses @link{sun.misc.Unsafe} to achieve a very good
 * performance.
 * 
 * <p>
 * Important notes:<br/>
 * <li>This class increases performance, but may result in bigger size of serialized representation.</li>
 * <li>Bulk operations, e.g. on arrays of primitive types, are always using native byte order.</li>
 * <li>Fixed-size int, long, short, float and double elements are always written using native byte order.</li>
 * <li>Best performance is achieved if no variable length encoding for integers is used.</li>
 * <li>Output serialized using this class should always be deserilized using @link{UnsafeMemoryInput}</li>
 * 
 * </p>
 * @author Roman Levenstein <romixlev@gmail.com> */
public final class UnsafeMemoryOutput extends ByteBufferOutput {

	/** Start address of the memory buffer The memory buffer should be non-movable, which normally means that is is allocated
	 * off-heap */
	private long bufaddress;

	private final static boolean isLittleEndian = ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN);

	{
		varIntsEnabled = false;

	}

	/** Creates an uninitialized Output. {@link #setBuffer(byte[])} must be called before the Output is used. */
	public UnsafeMemoryOutput () {
	}

	/** Creates a new Output for writing to a byte array.
	 * 
	 * @param bufferSize The initial and maximum size of the buffer. An exception is thrown if this size is exceeded. */
	public UnsafeMemoryOutput (int bufferSize) {
		this(bufferSize, bufferSize);
	}

	/** Creates a new Output for writing to a byte array.
	 * 
	 * @param bufferSize The initial size of the buffer.
	 * @param maxBufferSize The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown. */
	public UnsafeMemoryOutput (int bufferSize, int maxBufferSize) {
		super(bufferSize, maxBufferSize);
		updateBufferAddress();
	}

	/** Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used. */
	public UnsafeMemoryOutput (OutputStream outputStream) {
		super(outputStream);
		updateBufferAddress();
	}

	/** Creates a new Output for writing to an OutputStream. */
	public UnsafeMemoryOutput (OutputStream outputStream, int bufferSize) {
		super(outputStream, bufferSize);
		updateBufferAddress();
	}

	public UnsafeMemoryOutput (long address, int maxBufferSize) {
		super(address, maxBufferSize);
		updateBufferAddress();
	}

	public void setBuffer (ByteBuffer buffer, int maxBufferSize) {
		super.setBuffer(buffer, maxBufferSize);
		updateBufferAddress();
	}

	private void updateBufferAddress () {
		bufaddress = ((DirectBuffer)super.niobuffer).address();
	}

	/** Writes a 4 byte int. */
	final public void writeInt (int value) throws KryoException {
		require(4);
		unsafe().putInt(bufaddress + position, value);
		position += 4;
	}

	/** Writes a 4 byte float. */
	final public void writeFloat (float value) throws KryoException {
		require(4);
		unsafe().putFloat(bufaddress + position, value);
		position += 4;
	}

	/** Writes a 2 byte short. */
	final public void writeShort (int value) throws KryoException {
		require(2);
		unsafe().putShort(bufaddress + position, (short)value);
		position += 2;
	}

	/** Writes an 8 byte long. */
	final public void writeLong (long value) throws KryoException {
		require(8);
		unsafe().putLong(bufaddress + position, value);
		position += 8;
	}

	final public void writeByte (int value) throws KryoException {
		super.niobuffer.position(position);
		super.writeByte(value);
	}

	public void writeByte (byte value) throws KryoException {
		super.niobuffer.position(position);
		super.writeByte(value);
	}

	/** Writes a 1 byte boolean. */
	final public void writeBoolean (boolean value) throws KryoException {
		super.niobuffer.position(position);
		super.writeBoolean(value);
	}

	/** Writes a 2 byte char. */
	final public void writeChar (char value) throws KryoException {
		super.niobuffer.position(position);
		super.writeChar(value);
	}

	/** Writes an 8 byte double. */
	final public void writeDouble (double value) throws KryoException {
		require(8);
		unsafe().putDouble(bufaddress + position, value);
		double check = unsafe().getDouble(bufaddress + position);
		position += 8;
	}

	final public int writeInt (int value, boolean optimizePositive) throws KryoException {
		if (!varIntsEnabled) {
			writeInt(value);
			return 4;
		} else
			return writeVarInt(value, optimizePositive);
	}

	final public int writeLong (long value, boolean optimizePositive) throws KryoException {
		if (!varIntsEnabled) {
			writeLong(value);
			return 8;
		} else
			return writeVarLong(value, optimizePositive);
	}

	final public int writeVarInt (int val, boolean optimizePositive) throws KryoException {
		long value = val;
		if (!optimizePositive) value = (value << 1) ^ (value >> 31);
		long varInt = 0;

		varInt = (value & 0x7F);

		value >>>= 7;

		if (value == 0) {
			writeByte((byte)varInt);
			return 1;
		}

		varInt |= 0x80;
		varInt |= ((value & 0x7F) << 8);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianInt((int)varInt);
			position -= 2;
			return 2;
		}

		varInt |= (0x80 << 8);
		varInt |= ((value & 0x7F) << 16);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianInt((int)varInt);
			position -= 1;
			return 3;
		}

		varInt |= (0x80 << 16);
		varInt |= ((value & 0x7F) << 24);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianInt((int)varInt);
			position -= 0;
			return 4;
		}

		varInt |= (0x80 << 24);
		varInt |= ((value & 0x7F) << 32);
		varInt &= 0xFFFFFFFFL;
		writeLittleEndianLong(varInt);
		position -= 3;
		return 5;
	}

	final public int writeVarLong (long value, boolean optimizePositive) throws KryoException {
		if (!optimizePositive) value = (value << 1) ^ (value >> 63);
		int varInt = 0;

		varInt = (int)(value & 0x7F);

		value >>>= 7;

		if (value == 0) {
			write(varInt);
			return 1;
		}

		varInt |= 0x80;
		varInt |= ((value & 0x7F) << 8);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianInt(varInt);
			position -= 2;
			return 2;
		}

		varInt |= (0x80 << 8);
		varInt |= ((value & 0x7F) << 16);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianInt(varInt);
			position -= 1;
			return 3;
		}

		varInt |= (0x80 << 16);
		varInt |= ((value & 0x7F) << 24);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianInt(varInt);
			position -= 0;
			return 4;
		}

		varInt |= (0x80 << 24);
		long varLong = (varInt & 0xFFFFFFFFL) | (((long)(value & 0x7F)) << 32);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianLong(varLong);
			position -= 3;
			return 5;
		}

		varLong |= (0x80L << 32);
		varLong |= (((long)(value & 0x7F)) << 40);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianLong(varLong);
			position -= 2;
			return 6;
		}

		varLong |= (0x80L << 40);
		varLong |= (((long)(value & 0x7F)) << 48);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianLong(varLong);
			position -= 1;
			return 7;
		}

		varLong |= (0x80L << 48);
		varLong |= (((long)(value & 0x7F)) << 56);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianLong(varLong);
			return 8;
		}

		varLong |= (0x80L << 56);
		writeLittleEndianLong(varLong);
		write((byte)((value & 0x7F)));
		return 9;
	}

	final private void writeLittleEndianInt (int val) {
		if (isLittleEndian)
			writeInt(val);
		else
			writeInt(Util.swapInt(val));
	}

	final private void writeLittleEndianLong (long val) {
		if (isLittleEndian)
			writeLong(val);
		else
			writeLong(Util.swapLong(val));
	}

	// Methods implementing bulk operations on arrays of primitive types

	final public void writeInts (int[] object, boolean optimizePositive) throws KryoException {
		if (!varIntsEnabled) {
			int bytesToCopy = object.length << 2;
			writeBytes(object, intArrayBaseOffset, 0, bytesToCopy);
		} else
			super.writeInts(object, optimizePositive);
	}

	final public void writeLongs (long[] object, boolean optimizePositive) throws KryoException {
		if (!varIntsEnabled) {
			int bytesToCopy = object.length << 3;
			writeBytes(object, longArrayBaseOffset, 0, bytesToCopy);
		} else
			super.writeLongs(object, optimizePositive);
	}

	final public void writeInts (int[] object) throws KryoException {
		int bytesToCopy = object.length << 2;
		writeBytes(object, intArrayBaseOffset, 0, bytesToCopy);
	}

	final public void writeLongs (long[] object) throws KryoException {
		int bytesToCopy = object.length << 3;
		writeBytes(object, longArrayBaseOffset, 0, bytesToCopy);
	}

	final public void writeFloats (float[] object) throws KryoException {
		int bytesToCopy = object.length << 2;
		writeBytes(object, floatArrayBaseOffset, 0, bytesToCopy);
	}

	final public void writeShorts (short[] object) throws KryoException {
		int bytesToCopy = object.length << 1;
		writeBytes(object, shortArrayBaseOffset, 0, bytesToCopy);
	}

	final public void writeChars (char[] object) throws KryoException {
		int bytesToCopy = object.length << 1;
		writeBytes(object, charArrayBaseOffset, 0, bytesToCopy);
	}

	final public void writeDoubles (double[] object) throws KryoException {
		int bytesToCopy = object.length << 3;
		writeBytes(object, doubleArrayBaseOffset, 0, bytesToCopy);
	}

	/*** Output count bytes from a memory region starting at the given #{offset} inside the in-memory representation of obj object.
	 * @param obj
	 * @param offset
	 * @param count */
	final public void writeBytes (Object obj, long offset, long count) throws KryoException {
		writeBytes(obj, 0, offset, count);
	}

	/*** Output count bytes from a memory region starting at the given #{offset} inside the in-memory representation of obj object.
	 * The destination is defined by its address */
	final private void writeBytes (Object srcArray, long srcArrayTypeOffset, long srcOffset, long count) throws KryoException {
		int copyCount = Math.min(capacity - position, (int)count);

		while (true) {
			unsafe().copyMemory(srcArray, srcArrayTypeOffset + srcOffset, null, bufaddress + position, copyCount);
			position += copyCount;
			count -= copyCount;
			if (count == 0) return;
			srcOffset += copyCount;
			copyCount = Math.min(capacity, (int)count);
			require(copyCount);
		}
	}
}
