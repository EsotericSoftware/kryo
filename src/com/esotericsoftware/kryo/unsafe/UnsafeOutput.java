
package com.esotericsoftware.kryo.unsafe;

import static com.esotericsoftware.kryo.unsafe.UnsafeUtil.*;

import java.io.OutputStream;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Output;

/** An {@link Output} that reads data using sun.misc.Unsafe. Multi-byte primitive types use native byte order, so the native byte
 * order on different computers which read and write the data must be the same. Variable length encoding is not used for int or
 * long to maximize performance.
 * @author Roman Levenstein <romixlev@gmail.com> */
public class UnsafeOutput extends Output {
	/** Creates an uninitialized Output, {@link #setBuffer(byte[], int)} must be called before the Output is used. */
	public UnsafeOutput () {
	}

	/** Creates a new Output for writing to a byte[].
	 * @param bufferSize The size of the buffer. An exception is thrown if more bytes than this are written and {@link #flush()}
	 *           does not empty the buffer. */
	public UnsafeOutput (int bufferSize) {
		this(bufferSize, bufferSize);
	}

	/** Creates a new Output for writing to a byte[].
	 * @param bufferSize The initial size of the buffer.
	 * @param maxBufferSize If {@link #flush()} does not empty the buffer, the buffer is doubled as needed until it exceeds
	 *           maxBufferSize and an exception is thrown. Can be -1 for no maximum. */
	public UnsafeOutput (int bufferSize, int maxBufferSize) {
		super(bufferSize, maxBufferSize);
	}

	/** Creates a new Output for writing to a byte[].
	 * @see #setBuffer(byte[]) */
	public UnsafeOutput (byte[] buffer) {
		this(buffer, buffer.length);
	}

	/** Creates a new Output for writing to a byte[].
	 * @see #setBuffer(byte[], int) */
	public UnsafeOutput (byte[] buffer, int maxBufferSize) {
		super(buffer, maxBufferSize);
	}

	/** Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used. */
	public UnsafeOutput (OutputStream outputStream) {
		super(outputStream);
	}

	/** Creates a new Output for writing to an OutputStream with the specified buffer size. */
	public UnsafeOutput (OutputStream outputStream, int bufferSize) {
		super(outputStream, bufferSize);
	}

	public void writeInt (int value) throws KryoException {
		require(4);
		unsafe.putInt(buffer, byteArrayBaseOffset + position, value);
		position += 4;
	}

	public int writeVarInt (int value, boolean optimizePositive) throws KryoException {
		writeInt(value);
		return 4;
	}

	public void writeFloat (float value) throws KryoException {
		require(4);
		unsafe.putFloat(buffer, byteArrayBaseOffset + position, value);
		position += 4;
	}

	public void writeShort (int value) throws KryoException {
		require(2);
		unsafe.putShort(buffer, byteArrayBaseOffset + position, (short)value);
		position += 2;
	}

	public void writeLong (long value) throws KryoException {
		require(8);
		unsafe.putLong(buffer, byteArrayBaseOffset + position, value);
		position += 8;
	}

	public void writeDouble (double value) throws KryoException {
		require(8);
		unsafe.putDouble(buffer, byteArrayBaseOffset + position, value);
		position += 8;
	}

	public void writeChar (char value) throws KryoException {
		require(2);
		unsafe.putChar(buffer, byteArrayBaseOffset + position, value);
		position += 2;
	}

	public int writeVarLong (long value, boolean optimizePositive) throws KryoException {
		writeLong(value);
		return 8;
	}

	public void writeVarInts (int[] object, boolean optimizePositive) throws KryoException {
		writeBytes(object, intArrayBaseOffset, 0, object.length << 2);
	}

	public void writeVarLongs (long[] object, boolean optimizePositive) throws KryoException {
		writeBytes(object, longArrayBaseOffset, 0, object.length << 3);
	}

	public void writeInts (int[] object) throws KryoException {
		writeBytes(object, intArrayBaseOffset, 0, object.length << 2);
	}

	public void writeLongs (long[] object) throws KryoException {
		writeBytes(object, longArrayBaseOffset, 0, object.length << 3);
	}

	public void writeFloats (float[] object) throws KryoException {
		writeBytes(object, floatArrayBaseOffset, 0, object.length << 2);
	}

	public void writeShorts (short[] object) throws KryoException {
		writeBytes(object, shortArrayBaseOffset, 0, object.length << 1);
	}

	public void writeChars (char[] object) throws KryoException {
		writeBytes(object, charArrayBaseOffset, 0, object.length << 1);
	}

	public void writeDoubles (double[] object) throws KryoException {
		writeBytes(object, doubleArrayBaseOffset, 0, object.length << 3);
	}

	/*** Output count bytes from a memory region starting at the given offset inside the in-memory representation of the object. */
	public void writeBytes (Object object, long offset, long count) throws KryoException {
		writeBytes(object, 0, offset, count);
	}

	private void writeBytes (Object fromArray, long fromArrayTypeOffset, long srcOffset, long count) throws KryoException {
		int copyCount = Math.min(capacity - position, (int)count);
		while (true) {
			unsafe.copyMemory(fromArray, fromArrayTypeOffset + srcOffset, buffer, byteArrayBaseOffset + position, copyCount);
			position += copyCount;
			count -= copyCount;
			if (count == 0) return;
			srcOffset += copyCount;
			copyCount = Math.min(capacity, (int)count);
			require(copyCount);
		}
	}
}
