/* Copyright (c) 2008, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryo.unsafe;

import static com.esotericsoftware.kryo.unsafe.UnsafeUtil.*;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import sun.nio.ch.DirectBuffer;

/** A {@link ByteBufferOutput} that writes data to a direct ByteBuffer (off-heap memory) using sun.misc.Unsafe. Multi-byte
 * primitive types use native byte order, so the native byte order on different computers which read and write the data must be
 * the same. Variable length encoding is not used for int or long to maximize performance.
 * @author Roman Levenstein <romixlev@gmail.com> */
public class UnsafeByteBufferOutput extends ByteBufferOutput {
	/** Start address of the memory buffer. It must be non-movable, which normally means that is is allocated off-heap. */
	private long bufferAddress;

	/** Creates an uninitialized Output, {@link #setBuffer(ByteBuffer)} must be called before the Output is used. */
	public UnsafeByteBufferOutput () {
	}

	/** Creates a new Output for writing to a direct {@link ByteBuffer}.
	 * @param bufferSize The size of the buffer. An exception is thrown if more bytes than this are written and {@link #flush()}
	 *           does not empty the buffer. */
	public UnsafeByteBufferOutput (int bufferSize) {
		super(bufferSize);
		updateBufferAddress();
	}

	/** Creates a new Output for writing to a direct ByteBuffer.
	 * @param bufferSize The initial size of the buffer.
	 * @param maxBufferSize If {@link #flush()} does not empty the buffer, the buffer is doubled as needed until it exceeds
	 *           maxBufferSize and an exception is thrown. Can be -1 for no maximum. */
	public UnsafeByteBufferOutput (int bufferSize, int maxBufferSize) {
		super(bufferSize, maxBufferSize);
		updateBufferAddress();
	}

	/** Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used. */
	public UnsafeByteBufferOutput (OutputStream outputStream) {
		super(outputStream);
		updateBufferAddress();
	}

	/** Creates a new Output for writing to an OutputStream with the specified buffer size. */
	public UnsafeByteBufferOutput (OutputStream outputStream, int bufferSize) {
		super(outputStream, bufferSize);
		updateBufferAddress();
	}

	/** Creates a new Output for writing to a ByteBuffer representing the memory region at the specified address and size. */
	public UnsafeByteBufferOutput (long address, int size) {
		super(newDirectBuffer(address, size));
		updateBufferAddress();
	}

	public void setBuffer (ByteBuffer buffer, int maxBufferSize) {
		if (!(buffer instanceof DirectBuffer)) throw new IllegalArgumentException("buffer must be direct.");
		super.setBuffer(buffer, maxBufferSize);
		updateBufferAddress();
	}

	private void updateBufferAddress () {
		bufferAddress = ((DirectBuffer)byteBuffer).address();
	}

	public void writeInt (int value) throws KryoException {
		require(4);
		unsafe.putInt(bufferAddress + position, value);
		position += 4;
	}

	public int writeInt (int value, boolean optimizePositive) throws KryoException {
		writeInt(value);
		return 4;
	}

	public void writeFloat (float value) throws KryoException {
		require(4);
		unsafe.putFloat(bufferAddress + position, value);
		position += 4;
	}

	public void writeShort (int value) throws KryoException {
		require(2);
		unsafe.putShort(bufferAddress + position, (short)value);
		position += 2;
	}

	public void writeLong (long value) throws KryoException {
		require(8);
		unsafe.putLong(bufferAddress + position, value);
		position += 8;
	}

	public int writeLong (long value, boolean optimizePositive) throws KryoException {
		writeLong(value);
		return 8;
	}

	public void writeChar (char value) throws KryoException {
		require(2);
		unsafe.putChar(bufferAddress + position, value);
		position += 2;
	}

	public void writeDouble (double value) throws KryoException {
		require(8);
		unsafe.putDouble(bufferAddress + position, value);
		position += 8;
	}

	public void writeInts (int[] object, boolean optimizePositive) throws KryoException {
		writeBytes(object, intArrayBaseOffset, 0, object.length << 2);
	}

	public void writeLongs (long[] object, boolean optimizePositive) throws KryoException {
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

	public void writeBytes (byte[] bytes) throws KryoException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		writeBytes(bytes, 0, bytes.length);
	}

	/*** Output count bytes from a memory region starting at the given offset inside the in-memory representation of the object. */
	public void writeBytes (Object object, long offset, long count) throws KryoException {
		writeBytes(object, byteArrayBaseOffset, offset, count);
	}

	private void writeBytes (Object fromArray, long fromArrayTypeOffset, long srcOffset, long count) throws KryoException {
		int copyCount = Math.min(capacity - position, (int)count);
		while (true) {
			unsafe.copyMemory(fromArray, fromArrayTypeOffset + srcOffset, null, bufferAddress + position, copyCount);
			position += copyCount;
			count -= copyCount;
			if (count == 0) return;
			srcOffset += copyCount;
			copyCount = Math.min(capacity, (int)count);
			require(copyCount);
		}
	}
}
