
package com.esotericsoftware.kryo.unsafe;

import static com.esotericsoftware.kryo.unsafe.UnsafeUtil.*;

import java.io.InputStream;
import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.ByteBufferInput;

import sun.nio.ch.DirectBuffer;

/** An {@link ByteBufferInput} that reads data from direct ByteBuffer (off-heap memory) using sun.misc.Unsafe. Multi-byte
 * primitive types use native byte order, so the native byte order on different computers which read and write the data must be
 * the same. Variable length encoding is not used for int or long to maximize performance.
 * @author Roman Levenstein <romixlev@gmail.com> */
public class UnsafeMemoryInput extends ByteBufferInput {
	/** Start address of the memory buffer. It must be non-movable, which normally means that is is allocated off-heap. */
	private long bufferAddress;

	/** Creates an uninitialized Input, {@link #setBuffer(ByteBuffer)} must be called before the Input is used. */
	public UnsafeMemoryInput () {
	}

	/** Creates a new Input for reading from a direct {@link ByteBuffer}.
	 * @param bufferSize The size of the buffer. An exception is thrown if more bytes than this are read and
	 *           {@link #fill(ByteBuffer, int, int)} does not supply more bytes. */
	public UnsafeMemoryInput (int bufferSize) {
		super(bufferSize);
		updateBufferAddress();
	}

	/** Creates a new Input for reading from a {@link ByteBuffer} which is filled with the specified bytes. */
	public UnsafeMemoryInput (byte[] buffer) {
		super(buffer);
		updateBufferAddress();
	}

	/** Creates a new Input for reading from a ByteBuffer. */
	public UnsafeMemoryInput (ByteBuffer buffer) {
		super(buffer);
		updateBufferAddress();
	}

	/** Creates a new Input for reading from a ByteBuffer representing the memory region at the specified address and size. */
	public UnsafeMemoryInput (long address, int size) {
		super(newDirectBuffer(address, size));
		updateBufferAddress();
	}

	/** Creates a new Input for reading from an InputStream with a buffer size of 4096. */
	public UnsafeMemoryInput (InputStream inputStream) {
		super(inputStream);
		updateBufferAddress();
	}

	/** Creates a new Input for reading from an InputStream with the specified buffer size. */
	public UnsafeMemoryInput (InputStream inputStream, int bufferSize) {
		super(inputStream, bufferSize);
		updateBufferAddress();
	}

	public void setBuffer (ByteBuffer buffer) {
		super.setBuffer(buffer);
		updateBufferAddress();
	}

	private void updateBufferAddress () {
		bufferAddress = ((DirectBuffer)byteBuffer).address();
	}

	public int readInt () throws KryoException {
		require(4);
		int result = unsafe.getInt(bufferAddress + position);
		position += 4;
		return result;
	}

	public int readInt (boolean optimizePositive) throws KryoException {
		return readInt();
	}

	public boolean canReadInt () throws KryoException {
		return limit - position >= 4;
	}

	public float readFloat () throws KryoException {
		require(4);
		float result = unsafe.getFloat(bufferAddress + position);
		position += 4;
		return result;
	}

	public short readShort () throws KryoException {
		require(2);
		short result = unsafe.getShort(bufferAddress + position);
		position += 2;
		return result;
	}

	public long readLong () throws KryoException {
		require(8);
		long result = unsafe.getLong(bufferAddress + position);
		position += 8;
		return result;
	}

	public long readLong (boolean optimizePositive) throws KryoException {
		return readLong();
	}

	public boolean canReadLong () throws KryoException {
		return limit - position >= 8;
	}

	public boolean readBoolean () throws KryoException {
		byteBuffer.position(position);
		return super.readBoolean();
	}

	public byte readByte () throws KryoException {
		byteBuffer.position(position);
		return super.readByte();
	}

	public char readChar () throws KryoException {
		require(2);
		char result = unsafe.getChar(bufferAddress + position);
		position += 2;
		return result;
	}

	public double readDouble () throws KryoException {
		require(8);
		double result = unsafe.getDouble(bufferAddress + position);
		position += 8;
		return result;
	}

	public int[] readInts (int length, boolean optimizePositive) throws KryoException {
		int[] array = new int[length];
		readBytes(array, intArrayBaseOffset, 0, length << 2);
		return array;
	}

	public long[] readLongs (int length, boolean optimizePositive) throws KryoException {
		long[] array = new long[length];
		readBytes(array, longArrayBaseOffset, 0, length << 3);
		return array;
	}

	public float[] readFloats (int length) throws KryoException {
		float[] array = new float[length];
		readBytes(array, floatArrayBaseOffset, 0, length << 2);
		return array;
	}

	public short[] readShorts (int length) throws KryoException {
		short[] array = new short[length];
		readBytes(array, shortArrayBaseOffset, 0, length << 1);
		return array;
	}

	public char[] readChars (int length) throws KryoException {
		char[] array = new char[length];
		readBytes(array, charArrayBaseOffset, 0, length << 1);
		return array;
	}

	public double[] readDoubles (int length) throws KryoException {
		double[] array = new double[length];
		readBytes(array, doubleArrayBaseOffset, 0, length << 3);
		return array;
	}

	public byte[] readBytes (int length) throws KryoException {
		byte[] bytes = new byte[length];
		readBytes(bytes, 0, bytes.length);
		return bytes;
	}

	/** Reads bytes to the specified array. */
	public void readBytes (Object toArray, long offset, long count) throws KryoException {
		if (!toArray.getClass().isArray()) throw new KryoException("toArray must be an array.");
		readBytes(toArray, byteArrayBaseOffset, offset, (int)count);
	}

	private void readBytes (Object toArray, long toArrayTypeOffset, long offset, int count) throws KryoException {
		int copyCount = Math.min(limit - position, count);
		while (true) {
			unsafe.copyMemory(null, bufferAddress + position, toArray, toArrayTypeOffset + offset, copyCount);
			position += copyCount;
			count -= copyCount;
			if (count == 0) break;
			offset += copyCount;
			copyCount = Math.min(count, capacity);
			require(copyCount);
		}
	}
}
