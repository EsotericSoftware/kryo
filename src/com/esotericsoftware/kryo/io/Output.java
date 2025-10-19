/* Copyright (c) 2008-2025, Nathan Sweet
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

package com.esotericsoftware.kryo.io;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.util.Pool.Poolable;
import com.esotericsoftware.kryo.util.Util;

import java.io.IOException;
import java.io.OutputStream;

/** An OutputStream that writes data to a byte[] and optionally flushes to another OutputStream. Utility methods are provided for
 * efficiently writing primitive types and strings using big endian.
 * @author Nathan Sweet */
public class Output extends OutputStream implements AutoCloseable, Poolable {
	protected int maxCapacity;
	protected long total;
	protected int position;
	protected int capacity;
	protected byte[] buffer;
	protected OutputStream outputStream;
	protected boolean varEncoding = true;

	/** Creates an uninitialized Output, {@link #setBuffer(byte[], int)} must be called before the Output is used. */
	public Output () {
	}

	/** Creates a new Output for writing to a byte[].
	 * @param bufferSize The size of the buffer. An exception is thrown if more bytes than this are written and {@link #flush()}
	 *           does not empty the buffer. */
	public Output (int bufferSize) {
		this(bufferSize, bufferSize);
	}

	/** Creates a new Output for writing to a byte[].
	 * @param bufferSize The initial size of the buffer.
	 * @param maxBufferSize If {@link #flush()} does not empty the buffer, the buffer is doubled as needed until it exceeds
	 *           maxBufferSize and an exception is thrown. Can be -1 for no maximum. */
	public Output (int bufferSize, int maxBufferSize) {
		if (bufferSize > maxBufferSize && maxBufferSize != -1) throw new IllegalArgumentException(
			"bufferSize: " + bufferSize + " cannot be greater than maxBufferSize: " + maxBufferSize);
		if (maxBufferSize < -1) throw new IllegalArgumentException("maxBufferSize cannot be < -1: " + maxBufferSize);
		this.capacity = bufferSize;
		this.maxCapacity = maxBufferSize == -1 ? Util.maxArraySize : maxBufferSize;
		buffer = new byte[bufferSize];
	}

	/** Creates a new Output for writing to a byte[].
	 * @see #setBuffer(byte[]) */
	public Output (byte[] buffer) {
		this(buffer, buffer.length);
	}

	/** Creates a new Output for writing to a byte[].
	 * @see #setBuffer(byte[], int) */
	public Output (byte[] buffer, int maxBufferSize) {
		if (buffer == null) throw new IllegalArgumentException("buffer cannot be null.");
		setBuffer(buffer, maxBufferSize);
	}

	/** Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used. */
	public Output (OutputStream outputStream) {
		this(4096, 4096);
		if (outputStream == null) throw new IllegalArgumentException("outputStream cannot be null.");
		this.outputStream = outputStream;
	}

	/** Creates a new Output for writing to an OutputStream with the specified buffer size. */
	public Output (OutputStream outputStream, int bufferSize) {
		this(bufferSize, bufferSize);
		if (outputStream == null) throw new IllegalArgumentException("outputStream cannot be null.");
		this.outputStream = outputStream;
	}

	public OutputStream getOutputStream () {
		return outputStream;
	}

	/** Sets a new OutputStream to flush data to when the buffer is full. The position and total are reset, discarding any buffered
	 * bytes.
	 * @param outputStream May be null. */
	public void setOutputStream (OutputStream outputStream) {
		this.outputStream = outputStream;
		reset();
	}

	/** Sets a new buffer to write to. The max size is the buffer's length.
	 * @see #setBuffer(byte[], int) */
	public void setBuffer (byte[] buffer) {
		setBuffer(buffer, buffer.length);
	}

	/** Sets a new buffer to write to. The bytes are not copied, the old buffer is discarded and the new buffer used in its place.
	 * The position and total are reset. The {@link #setOutputStream(OutputStream) OutputStream} is set to null.
	 * @param maxBufferSize If {@link #flush()} does not empty the buffer, the buffer is doubled as needed until it exceeds
	 *           maxBufferSize and an exception is thrown. Can be -1 for no maximum. */
	public void setBuffer (byte[] buffer, int maxBufferSize) {
		if (buffer == null) throw new IllegalArgumentException("buffer cannot be null.");
		if (buffer.length > maxBufferSize && maxBufferSize != -1) throw new IllegalArgumentException(
			"buffer has length: " + buffer.length + " cannot be greater than maxBufferSize: " + maxBufferSize);
		if (maxBufferSize < -1) throw new IllegalArgumentException("maxBufferSize cannot be < -1: " + maxBufferSize);
		this.buffer = buffer;
		this.maxCapacity = maxBufferSize == -1 ? Util.maxArraySize : maxBufferSize;
		capacity = buffer.length;
		position = 0;
		total = 0;
		outputStream = null;
	}

	/** Returns the buffer. The bytes between 0 and {@link #position()} are the data that has been written. */
	public byte[] getBuffer () {
		return buffer;
	}

	/** Allocates and returns a new byte[] containing the bytes currently in the buffer between 0 and {@link #position()}. */
	public byte[] toBytes () {
		byte[] newBuffer = new byte[position];
		System.arraycopy(buffer, 0, newBuffer, 0, position);
		return newBuffer;
	}

	public boolean getVariableLengthEncoding () {
		return varEncoding;
	}

	/** If false, {@link #writeInt(int, boolean)}, {@link #writeLong(long, boolean)}, {@link #writeInts(int[], int, int, boolean)},
	 * and {@link #writeLongs(long[], int, int, boolean)} will use fixed length encoding, which may be faster for some data.
	 * Default is true. */
	public void setVariableLengthEncoding (boolean varEncoding) {
		this.varEncoding = varEncoding;
	}

	/** Returns the current position in the buffer. This is the number of bytes that have not been flushed. */
	public int position () {
		return position;
	}

	/** Sets the current position in the buffer. */
	public void setPosition (int position) {
		this.position = position;
	}

	/** Returns the total number of bytes written. This may include bytes that have not been flushed. */
	public long total () {
		return total + position;
	}

	/** The maximum buffer size, or -1 for no maximum.
	 * @see Output#Output(int, int) */
	public int getMaxCapacity () {
		return maxCapacity;
	}

	/** Sets the position and total to 0. */
	public void reset () {
		position = 0;
		total = 0;
	}

	/** Ensures the buffer is large enough to read the specified number of bytes.
	 * @return true if the buffer has been resized. */
	protected boolean require (int required) throws KryoException {
		if (capacity - position >= required) return false;
		flush();
		if (capacity - position >= required) return true;
		if (required > maxCapacity - position) {
			if (required > maxCapacity)
				throw new KryoBufferOverflowException("Buffer overflow. Max capacity: " + maxCapacity + ", required: " + required);
			throw new KryoBufferOverflowException(
				"Buffer overflow. Available: " + (maxCapacity - position) + ", required: " + required);
		}
		if (capacity == 0) capacity = 16;
		do {
			capacity = Math.min(capacity * 2, maxCapacity);
		} while (capacity - position < required);
		byte[] newBuffer = new byte[capacity];
		System.arraycopy(buffer, 0, newBuffer, 0, position);
		buffer = newBuffer;
		return true;
	}

	// OutputStream:

	/** Flushes the buffered bytes. The default implementation writes the buffered bytes to the {@link #getOutputStream()
	 * OutputStream}, if any, and sets the position to 0. Can be overridden to flush the bytes somewhere else. */
	public void flush () throws KryoException {
		if (outputStream == null) return;
		try {
			outputStream.write(buffer, 0, position);
			outputStream.flush();
		} catch (IOException ex) {
			throw new KryoException(ex);
		}
		total += position;
		position = 0;
	}

	/** Flushes any buffered bytes and closes the underlying OutputStream, if any. */
	public void close () throws KryoException {
		flush();
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException ignored) {
			}
		}
	}

	/** Writes a byte. */
	public void write (int value) throws KryoException {
		if (position == capacity) require(1);
		buffer[position++] = (byte)value;
	}

	/** Writes the bytes. Note the number of bytes is not written. */
	public void write (byte[] bytes) throws KryoException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		writeBytes(bytes, 0, bytes.length);
	}

	/** Writes the bytes. Note the number of bytes is not written. */
	public void write (byte[] bytes, int offset, int length) throws KryoException {
		writeBytes(bytes, offset, length);
	}

	// byte:

	public void writeByte (byte value) throws KryoException {
		if (position == capacity) require(1);
		buffer[position++] = value;
	}

	public void writeByte (int value) throws KryoException {
		if (position == capacity) require(1);
		buffer[position++] = (byte)value;
	}

	/** Writes the bytes. Note the number of bytes is not written. */
	public void writeBytes (byte[] bytes) throws KryoException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		writeBytes(bytes, 0, bytes.length);
	}

	/** Writes the bytes. Note the number of bytes is not written. */
	public void writeBytes (byte[] bytes, int offset, int count) throws KryoException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		int copyCount = Math.min(capacity - position, count);
		while (true) {
			System.arraycopy(bytes, offset, buffer, position, copyCount);
			position += copyCount;
			count -= copyCount;
			if (count == 0) return;
			offset += copyCount;
			copyCount = Math.min(Math.max(capacity, 1), count);
			require(copyCount);
		}
	}

	/** Writes count bytes from long, the last byte written is the lowest byte from the long.
	 *  Note the number of bytes is not written. */
	public void writeInt (int bytes, int count) {
		if (count < 0 || count > 4) throw new IllegalArgumentException("count must be >= 0 and <= 4: " + count);
		require(count);
		int p = position;
		position = p + count;
		switch (count) {
			case 1:
				buffer[p] = (byte)bytes;
				break;
			case 2:
				buffer[p] = (byte)(bytes >> 8);
				buffer[p+1] = (byte)bytes;
				break;
			case 3:
				buffer[p] = (byte)(bytes >> 16);
				buffer[p+1] = (byte)(bytes >> 8);
				buffer[p+2] = (byte)bytes;
				break;
			case 4:
				buffer[p] = (byte)(bytes >> 24);
				buffer[p+1] = (byte)(bytes >> 16);
				buffer[p+2] = (byte)(bytes >> 8);
				buffer[p+3] = (byte)bytes;
				break;
		}
	}

	/** Writes count bytes from long, the last byte written is the lowest byte from the long.
	 *  Note the number of bytes is not written. */
	public void writeLong (long bytes, int count) {
		if (count < 0 || count > 8) throw new IllegalArgumentException("count must be >= 0 and <= 8: " + count);
		if (count <= 4) {
			writeInt((int) bytes, count);
		} else {
			require(count);
			writeInt((int) (bytes >> 32), count - 4);
			writeInt((int) bytes, 4);
		}
	}

	// int:

	/** Writes a 4 byte int. */
	public void writeInt (int value) throws KryoException {
		require(4);
		byte[] buffer = this.buffer;
		int p = position;
		position = p + 4;
		buffer[p] = (byte)value;
		buffer[p + 1] = (byte)(value >> 8);
		buffer[p + 2] = (byte)(value >> 16);
		buffer[p + 3] = (byte)(value >> 24);
	}

	/** Reads an int using fixed or variable length encoding, depending on {@link #setVariableLengthEncoding(boolean)}. Use
	 * {@link #writeVarInt(int, boolean)} explicitly when writing values that should always use variable length encoding (eg values
	 * that appear many times).
	 * @return The number of bytes written.
	 * @see #intLength(int, boolean) */
	public int writeInt (int value, boolean optimizePositive) throws KryoException {
		if (varEncoding) return writeVarInt(value, optimizePositive);
		writeInt(value);
		return 4;
	}

	/** Writes a 1-5 byte int.
	 * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
	 *           inefficient (5 bytes).
	 * @return The number of bytes written.
	 * @see #varIntLength(int, boolean) */
	public int writeVarInt (int value, boolean optimizePositive) throws KryoException {
		if (!optimizePositive) value = (value << 1) ^ (value >> 31);
		if (value >>> 7 == 0) {
			if (position == capacity) require(1);
			buffer[position++] = (byte)value;
			return 1;
		}
		if (value >>> 14 == 0) {
			require(2);
			int p = position;
			position = p + 2;
			buffer[p] = (byte)((value & 0x7F) | 0x80);
			buffer[p + 1] = (byte)(value >>> 7);
			return 2;
		}
		if (value >>> 21 == 0) {
			require(3);
			int p = position;
			position = p + 3;
			byte[] buffer = this.buffer;
			buffer[p] = (byte)((value & 0x7F) | 0x80);
			buffer[p + 1] = (byte)(value >>> 7 | 0x80);
			buffer[p + 2] = (byte)(value >>> 14);
			return 3;
		}
		if (value >>> 28 == 0) {
			require(4);
			int p = position;
			position = p + 4;
			byte[] buffer = this.buffer;
			buffer[p] = (byte)((value & 0x7F) | 0x80);
			buffer[p + 1] = (byte)(value >>> 7 | 0x80);
			buffer[p + 2] = (byte)(value >>> 14 | 0x80);
			buffer[p + 3] = (byte)(value >>> 21);
			return 4;
		}
		require(5);
		int p = position;
		position = p + 5;
		byte[] buffer = this.buffer;
		buffer[p] = (byte)((value & 0x7F) | 0x80);
		buffer[p + 1] = (byte)(value >>> 7 | 0x80);
		buffer[p + 2] = (byte)(value >>> 14 | 0x80);
		buffer[p + 3] = (byte)(value >>> 21 | 0x80);
		buffer[p + 4] = (byte)(value >>> 28);
		return 5;
	}

	/** Writes a 1-5 byte int, encoding the boolean value with a bit flag.
	 * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
	 *           inefficient (5 bytes).
	 * @return The number of bytes written. */
	public int writeVarIntFlag (boolean flag, int value, boolean optimizePositive) throws KryoException {
		if (!optimizePositive) value = (value << 1) ^ (value >> 31);
		int first = (value & 0x3F) | (flag ? 0x80 : 0); // Mask first 6 bits, bit 8 is the flag.
		if (value >>> 6 == 0) {
			if (position == capacity) require(1);
			buffer[position++] = (byte)first;
			return 1;
		}
		if (value >>> 13 == 0) {
			require(2);
			int p = position;
			position = p + 2;
			buffer[p] = (byte)(first | 0x40); // Set bit 7.
			buffer[p + 1] = (byte)(value >>> 6);
			return 2;
		}
		if (value >>> 20 == 0) {
			require(3);
			byte[] buffer = this.buffer;
			int p = position;
			position = p + 3;
			buffer[p] = (byte)(first | 0x40); // Set bit 7.
			buffer[p + 1] = (byte)((value >>> 6) | 0x80); // Set bit 8.
			buffer[p + 2] = (byte)(value >>> 13);
			return 3;
		}
		if (value >>> 27 == 0) {
			require(4);
			byte[] buffer = this.buffer;
			int p = position;
			position = p + 4;
			buffer[p] = (byte)(first | 0x40); // Set bit 7.
			buffer[p + 1] = (byte)((value >>> 6) | 0x80); // Set bit 8.
			buffer[p + 2] = (byte)((value >>> 13) | 0x80); // Set bit 8.
			buffer[p + 3] = (byte)(value >>> 20);
			return 4;
		}
		require(5);
		byte[] buffer = this.buffer;
		int p = position;
		position = p + 5;
		buffer[p] = (byte)(first | 0x40); // Set bit 7.
		buffer[p + 1] = (byte)((value >>> 6) | 0x80); // Set bit 8.
		buffer[p + 2] = (byte)((value >>> 13) | 0x80); // Set bit 8.
		buffer[p + 3] = (byte)((value >>> 20) | 0x80); // Set bit 8.
		buffer[p + 4] = (byte)(value >>> 27);
		return 5;
	}

	/** Returns the number of bytes that would be written with {@link #writeInt(int, boolean)}. */
	public int intLength (int value, boolean optimizePositive) {
		if (varEncoding) return varIntLength(value, optimizePositive);
		return 4;
	}

	// long:

	/** Writes an 8 byte long. */
	public void writeLong (long value) throws KryoException {
		require(8);
		byte[] buffer = this.buffer;
		int p = position;
		position = p + 8;
		buffer[p] = (byte)value;
		buffer[p + 1] = (byte)(value >>> 8);
		buffer[p + 2] = (byte)(value >>> 16);
		buffer[p + 3] = (byte)(value >>> 24);
		buffer[p + 4] = (byte)(value >>> 32);
		buffer[p + 5] = (byte)(value >>> 40);
		buffer[p + 6] = (byte)(value >>> 48);
		buffer[p + 7] = (byte)(value >>> 56);
	}

	/** Reads a long using fixed or variable length encoding, depending on {@link #setVariableLengthEncoding(boolean)}. Use
	 * {@link #writeVarLong(long, boolean)} explicitly when writing values that should always use variable length encoding (eg
	 * values that appear many times).
	 * @return The number of bytes written.
	 * @see #longLength(int, boolean) */
	public int writeLong (long value, boolean optimizePositive) throws KryoException {
		if (varEncoding) return writeVarLong(value, optimizePositive);
		writeLong(value);
		return 8;

	}

	/** Writes a 1-9 byte long.
	 * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
	 *           inefficient (9 bytes).
	 * @return The number of bytes written.
	 * @see #varLongLength(long, boolean) */
	public int writeVarLong (long value, boolean optimizePositive) throws KryoException {
		if (!optimizePositive) value = (value << 1) ^ (value >> 63);
		if (value >>> 7 == 0) {
			if (position == capacity) require(1);
			buffer[position++] = (byte)value;
			return 1;
		}
		if (value >>> 14 == 0) {
			require(2);
			int p = position;
			position = p + 2;
			buffer[p] = (byte)((value & 0x7F) | 0x80);
			buffer[p + 1] = (byte)(value >>> 7);
			return 2;
		}
		if (value >>> 21 == 0) {
			require(3);
			int p = position;
			position = p + 3;
			byte[] buffer = this.buffer;
			buffer[p] = (byte)((value & 0x7F) | 0x80);
			buffer[p + 1] = (byte)(value >>> 7 | 0x80);
			buffer[p + 2] = (byte)(value >>> 14);
			return 3;
		}
		if (value >>> 28 == 0) {
			require(4);
			int p = position;
			position = p + 4;
			byte[] buffer = this.buffer;
			buffer[p] = (byte)((value & 0x7F) | 0x80);
			buffer[p + 1] = (byte)(value >>> 7 | 0x80);
			buffer[p + 2] = (byte)(value >>> 14 | 0x80);
			buffer[p + 3] = (byte)(value >>> 21);
			return 4;
		}
		if (value >>> 35 == 0) {
			require(5);
			int p = position;
			position = p + 5;
			byte[] buffer = this.buffer;
			buffer[p] = (byte)((value & 0x7F) | 0x80);
			buffer[p + 1] = (byte)(value >>> 7 | 0x80);
			buffer[p + 2] = (byte)(value >>> 14 | 0x80);
			buffer[p + 3] = (byte)(value >>> 21 | 0x80);
			buffer[p + 4] = (byte)(value >>> 28);
			return 5;
		}
		if (value >>> 42 == 0) {
			require(6);
			int p = position;
			position = p + 6;
			byte[] buffer = this.buffer;
			buffer[p] = (byte)((value & 0x7F) | 0x80);
			buffer[p + 1] = (byte)(value >>> 7 | 0x80);
			buffer[p + 2] = (byte)(value >>> 14 | 0x80);
			buffer[p + 3] = (byte)(value >>> 21 | 0x80);
			buffer[p + 4] = (byte)(value >>> 28 | 0x80);
			buffer[p + 5] = (byte)(value >>> 35);
			return 6;
		}
		if (value >>> 49 == 0) {
			require(7);
			int p = position;
			position = p + 7;
			byte[] buffer = this.buffer;
			buffer[p] = (byte)((value & 0x7F) | 0x80);
			buffer[p + 1] = (byte)(value >>> 7 | 0x80);
			buffer[p + 2] = (byte)(value >>> 14 | 0x80);
			buffer[p + 3] = (byte)(value >>> 21 | 0x80);
			buffer[p + 4] = (byte)(value >>> 28 | 0x80);
			buffer[p + 5] = (byte)(value >>> 35 | 0x80);
			buffer[p + 6] = (byte)(value >>> 42);
			return 7;
		}
		if (value >>> 56 == 0) {
			require(8);
			int p = position;
			position = p + 8;
			byte[] buffer = this.buffer;
			buffer[p] = (byte)((value & 0x7F) | 0x80);
			buffer[p + 1] = (byte)(value >>> 7 | 0x80);
			buffer[p + 2] = (byte)(value >>> 14 | 0x80);
			buffer[p + 3] = (byte)(value >>> 21 | 0x80);
			buffer[p + 4] = (byte)(value >>> 28 | 0x80);
			buffer[p + 5] = (byte)(value >>> 35 | 0x80);
			buffer[p + 6] = (byte)(value >>> 42 | 0x80);
			buffer[p + 7] = (byte)(value >>> 49);
			return 8;
		}
		require(9);
		int p = position;
		position = p + 9;
		byte[] buffer = this.buffer;
		buffer[p] = (byte)((value & 0x7F) | 0x80);
		buffer[p + 1] = (byte)(value >>> 7 | 0x80);
		buffer[p + 2] = (byte)(value >>> 14 | 0x80);
		buffer[p + 3] = (byte)(value >>> 21 | 0x80);
		buffer[p + 4] = (byte)(value >>> 28 | 0x80);
		buffer[p + 5] = (byte)(value >>> 35 | 0x80);
		buffer[p + 6] = (byte)(value >>> 42 | 0x80);
		buffer[p + 7] = (byte)(value >>> 49 | 0x80);
		buffer[p + 8] = (byte)(value >>> 56);
		return 9;
	}

	/** Returns the number of bytes that would be written with {@link #writeLong(long, boolean)}. */
	public int longLength (int value, boolean optimizePositive) {
		if (varEncoding) return varLongLength(value, optimizePositive);
		return 8;
	}

	// float:

	/** Writes a 4 byte float. */
	public void writeFloat (float value) throws KryoException {
		require(4);
		byte[] buffer = this.buffer;
		int p = position;
		position = p + 4;
		int intValue = Float.floatToIntBits(value);
		buffer[p] = (byte)intValue;
		buffer[p + 1] = (byte)(intValue >> 8);
		buffer[p + 2] = (byte)(intValue >> 16);
		buffer[p + 3] = (byte)(intValue >> 24);
	}

	/** Writes a 1-5 byte float with reduced precision.
	 * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
	 *           inefficient (5 bytes).
	 * @return The number of bytes written. */
	public int writeVarFloat (float value, float precision, boolean optimizePositive) throws KryoException {
		return writeVarInt((int)(value * precision), optimizePositive);
	}

	// double:

	/** Writes an 8 byte double. */
	public void writeDouble (double value) throws KryoException {
		require(8);
		byte[] buffer = this.buffer;
		int p = position;
		position = p + 8;
		long longValue = Double.doubleToLongBits(value);
		buffer[p] = (byte)longValue;
		buffer[p + 1] = (byte)(longValue >>> 8);
		buffer[p + 2] = (byte)(longValue >>> 16);
		buffer[p + 3] = (byte)(longValue >>> 24);
		buffer[p + 4] = (byte)(longValue >>> 32);
		buffer[p + 5] = (byte)(longValue >>> 40);
		buffer[p + 6] = (byte)(longValue >>> 48);
		buffer[p + 7] = (byte)(longValue >>> 56);
	}

	/** Writes a 1-9 byte double with reduced precision.
	 * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
	 *           inefficient (9 bytes).
	 * @return The number of bytes written. */
	public int writeVarDouble (double value, double precision, boolean optimizePositive) throws KryoException {
		return writeVarLong((long)(value * precision), optimizePositive);
	}

	// short:

	/** Writes a 2 byte short. */
	public void writeShort (int value) throws KryoException {
		require(2);
		int p = position;
		position = p + 2;
		buffer[p] = (byte)value;
		buffer[p + 1] = (byte)(value >>> 8);
	}

	// char:

	/** Writes a 2 byte char. */
	public void writeChar (char value) throws KryoException {
		require(2);
		int p = position;
		position = p + 2;
		buffer[p] = (byte)value;
		buffer[p + 1] = (byte)(value >>> 8);
	}

	// boolean:

	/** Writes a 1 byte boolean. */
	public void writeBoolean (boolean value) throws KryoException {
		if (position == capacity) require(1);
		buffer[position++] = value ? (byte)1 : 0;
	}

	// String:

	/** Writes the length and string, or null. Short strings are checked and if ASCII they are written more efficiently, else they
	 * are written as UTF8. If a string is known to be ASCII, {@link #writeAscii(String)} may be used. The string can be read using
	 * {@link Input#readString()} or {@link Input#readStringBuilder()}.
	 * @param value May be null. */
	public void writeString (String value) throws KryoException {
		if (value == null) {
			writeByte(0x80); // 0 means null, bit 8 means UTF8.
			return;
		}
		int charCount = value.length();
		if (charCount == 0) {
			writeByte(1 | 0x80); // 1 means empty string, bit 8 means UTF8.
			return;
		}
		// Detect ASCII.
		outer:
		if (charCount > 1 && charCount <= 32) {
			for (int i = 0; i < charCount; i++)
				if (value.charAt(i) > 127) break outer;
			if (capacity - position < charCount)
				writeAscii_slow(value, charCount);
			else {
				value.getBytes(0, charCount, buffer, position);
				position += charCount;
			}
			buffer[position - 1] |= 0x80;
			return;
		}
		writeVarIntFlag(true, charCount + 1, true);
		int charIndex = 0;
		if (capacity - position >= charCount) {
			// Try to write 7 bit chars.
			byte[] buffer = this.buffer;
			int p = position;
			while (true) {
				int c = value.charAt(charIndex);
				if (c > 127) break;
				buffer[p++] = (byte)c;
				charIndex++;
				if (charIndex == charCount) {
					position = p;
					return;
				}
			}
			position = p;
		}
		if (charIndex < charCount) writeUtf8_slow(value, charCount, charIndex);
	}

	/** Writes a string that is known to contain only ASCII characters. Non-ASCII strings passed to this method will be corrupted.
	 * Each byte is a 7 bit character with the remaining byte denoting if another character is available. This is slightly more
	 * efficient than {@link #writeString(String)}. The string can be read using {@link Input#readString()} or
	 * {@link Input#readStringBuilder()}.
	 * @param value May be null. */
	public void writeAscii (String value) throws KryoException {
		if (value == null) {
			writeByte(0x80); // 0 means null, bit 8 means UTF8.
			return;
		}
		int charCount = value.length();
		switch (charCount) {
		case 0:
			writeByte(1 | 0x80); // 1 is string length + 1, bit 8 means UTF8.
			return;
		case 1:
			require(2);
			buffer[position++] = (byte)(2 | 0x80); // 2 is string length + 1, bit 8 means UTF8.
			buffer[position++] = (byte)value.charAt(0);
			return;
		}
		if (capacity - position < charCount)
			writeAscii_slow(value, charCount);
		else {
			value.getBytes(0, charCount, buffer, position);
			position += charCount;
		}
		buffer[position - 1] |= 0x80; // Bit 8 means end of ASCII.
	}

	private void writeUtf8_slow (String value, int charCount, int charIndex) {
		for (; charIndex < charCount; charIndex++) {
			if (position == capacity) require(Math.min(capacity, charCount - charIndex));
			int c = value.charAt(charIndex);
			if (c <= 0x007F)
				buffer[position++] = (byte)c;
			else if (c > 0x07FF) {
				buffer[position++] = (byte)(0xE0 | c >> 12 & 0x0F);
				require(2);
				buffer[position++] = (byte)(0x80 | c >> 6 & 0x3F);
				buffer[position++] = (byte)(0x80 | c & 0x3F);
			} else {
				buffer[position++] = (byte)(0xC0 | c >> 6 & 0x1F);
				if (position == capacity) require(1);
				buffer[position++] = (byte)(0x80 | c & 0x3F);
			}
		}
	}

	private void writeAscii_slow (String value, int charCount) throws KryoException {
		if (charCount == 0) return;
		if (position == capacity) require(1); // Must be able to write at least one character.
		int charIndex = 0;
		byte[] buffer = this.buffer;
		int charsToWrite = Math.min(charCount, capacity - position);
		while (charIndex < charCount) {
			value.getBytes(charIndex, charIndex + charsToWrite, buffer, position);
			charIndex += charsToWrite;
			position += charsToWrite;
			charsToWrite = Math.min(charCount - charIndex, capacity);
			if (require(charsToWrite)) buffer = this.buffer;
		}
	}

	// Primitive arrays:

	/** Writes an int array in bulk. This may be more efficient than writing them individually. */
	public void writeInts (int[] array, int offset, int count) throws KryoException {
		if (capacity >= count << 2) {
			require(count << 2);
			byte[] buffer = this.buffer;
			int p = position;
			for (int n = offset + count; offset < n; offset++, p += 4) {
				int value = array[offset];
				buffer[p] = (byte)value;
				buffer[p + 1] = (byte)(value >> 8);
				buffer[p + 2] = (byte)(value >> 16);
				buffer[p + 3] = (byte)(value >> 24);
			}
			position = p;
		} else {
			for (int n = offset + count; offset < n; offset++)
				writeInt(array[offset]);
		}
	}

	/** Writes an int array in bulk using fixed or variable length encoding, depending on
	 * {@link #setVariableLengthEncoding(boolean)}. This may be more efficient than writing them individually. */
	public void writeInts (int[] array, int offset, int count, boolean optimizePositive) throws KryoException {
		if (varEncoding) {
			for (int n = offset + count; offset < n; offset++)
				writeVarInt(array[offset], optimizePositive);
		} else
			writeInts(array, offset, count);
	}

	/** Writes a long array in bulk. This may be more efficient than writing them individually. */
	public void writeLongs (long[] array, int offset, int count) throws KryoException {
		if (capacity >= count << 3) {
			require(count << 3);
			byte[] buffer = this.buffer;
			int p = position;
			for (int n = offset + count; offset < n; offset++, p += 8) {
				long value = array[offset];
				buffer[p] = (byte)value;
				buffer[p + 1] = (byte)(value >>> 8);
				buffer[p + 2] = (byte)(value >>> 16);
				buffer[p + 3] = (byte)(value >>> 24);
				buffer[p + 4] = (byte)(value >>> 32);
				buffer[p + 5] = (byte)(value >>> 40);
				buffer[p + 6] = (byte)(value >>> 48);
				buffer[p + 7] = (byte)(value >>> 56);
			}
			position = p;
		} else {
			for (int n = offset + count; offset < n; offset++)
				writeLong(array[offset]);
		}
	}

	/** Writes a long array in bulk using fixed or variable length encoding, depending on
	 * {@link #setVariableLengthEncoding(boolean)}. This may be more efficient than writing them individually. */
	public void writeLongs (long[] array, int offset, int count, boolean optimizePositive) throws KryoException {
		if (varEncoding) {
			for (int n = offset + count; offset < n; offset++)
				writeVarLong(array[offset], optimizePositive);
		} else
			writeLongs(array, offset, count);
	}

	/** Writes a float array in bulk. This may be more efficient than writing them individually. */
	public void writeFloats (float[] array, int offset, int count) throws KryoException {
		if (capacity >= count << 2) {
			require(count << 2);
			byte[] buffer = this.buffer;
			int p = position;
			for (int n = offset + count; offset < n; offset++, p += 4) {
				int value = Float.floatToIntBits(array[offset]);
				buffer[p] = (byte)value;
				buffer[p + 1] = (byte)(value >> 8);
				buffer[p + 2] = (byte)(value >> 16);
				buffer[p + 3] = (byte)(value >> 24);
			}
			position = p;
		} else {
			for (int n = offset + count; offset < n; offset++)
				writeFloat(array[offset]);
		}
	}

	/** Writes a double array in bulk. This may be more efficient than writing them individually. */
	public void writeDoubles (double[] array, int offset, int count) throws KryoException {
		if (capacity >= count << 3) {
			require(count << 3);
			byte[] buffer = this.buffer;
			int p = position;
			for (int n = offset + count; offset < n; offset++, p += 8) {
				long value = Double.doubleToLongBits(array[offset]);
				buffer[p] = (byte)value;
				buffer[p + 1] = (byte)(value >>> 8);
				buffer[p + 2] = (byte)(value >>> 16);
				buffer[p + 3] = (byte)(value >>> 24);
				buffer[p + 4] = (byte)(value >>> 32);
				buffer[p + 5] = (byte)(value >>> 40);
				buffer[p + 6] = (byte)(value >>> 48);
				buffer[p + 7] = (byte)(value >>> 56);
			}
			position = p;
		} else {
			for (int n = offset + count; offset < n; offset++)
				writeDouble(array[offset]);
		}
	}

	/** Writes a short array in bulk. This may be more efficient than writing them individually. */
	public void writeShorts (short[] array, int offset, int count) throws KryoException {
		if (capacity >= count << 1) {
			require(count << 1);
			byte[] buffer = this.buffer;
			int p = position;
			for (int n = offset + count; offset < n; offset++, p += 2) {
				int value = array[offset];
				buffer[p] = (byte)value;
				buffer[p + 1] = (byte)(value >>> 8);
			}
			position = p;
		} else {
			for (int n = offset + count; offset < n; offset++)
				writeShort(array[offset]);
		}
	}

	/** Writes a char array in bulk. This may be more efficient than writing them individually. */
	public void writeChars (char[] array, int offset, int count) throws KryoException {
		if (capacity >= count << 1) {
			require(count << 1);
			byte[] buffer = this.buffer;
			int p = position;
			for (int n = offset + count; offset < n; offset++, p += 2) {
				int value = array[offset];
				buffer[p] = (byte)value;
				buffer[p + 1] = (byte)(value >>> 8);
			}
			position = p;
		} else {
			for (int n = offset + count; offset < n; offset++)
				writeChar(array[offset]);
		}
	}

	/** Writes a boolean array in bulk. This may be more efficient than writing them individually. */
	public void writeBooleans (boolean[] array, int offset, int count) throws KryoException {
		if (capacity >= count) {
			require(count);
			byte[] buffer = this.buffer;
			int p = position;
			for (int n = offset + count; offset < n; offset++, p++)
				buffer[p] = array[offset] ? (byte)1 : 0;
			position = p;
		} else {
			for (int n = offset + count; offset < n; offset++)
				writeBoolean(array[offset]);
		}
	}

	//

	/** Returns the number of bytes that would be written with {@link #writeVarInt(int, boolean)}. */
	public static int varIntLength (int value, boolean optimizePositive) {
		if (!optimizePositive) value = (value << 1) ^ (value >> 31);
		if (value >>> 7 == 0) return 1;
		if (value >>> 14 == 0) return 2;
		if (value >>> 21 == 0) return 3;
		if (value >>> 28 == 0) return 4;
		return 5;
	}

	/** Returns the number of bytes that would be written with {@link #writeVarLong(long, boolean)}. */
	public static int varLongLength (long value, boolean optimizePositive) {
		if (!optimizePositive) value = (value << 1) ^ (value >> 63);
		if (value >>> 7 == 0) return 1;
		if (value >>> 14 == 0) return 2;
		if (value >>> 21 == 0) return 3;
		if (value >>> 28 == 0) return 4;
		if (value >>> 35 == 0) return 5;
		if (value >>> 42 == 0) return 6;
		if (value >>> 49 == 0) return 7;
		if (value >>> 56 == 0) return 8;
		return 9;
	}
}
