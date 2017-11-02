/* Copyright (c) 2008-2017, Nathan Sweet
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.util.Util;

/** An {@link Output} that uses a ByteBuffer rather than a byte[].
 * <p>
 * Note that the byte[] {@link #getBuffer() buffer} is not used. Code taking an Output and expecting the byte[] to be used may not
 * work correctly.
 * @author Roman Levenstein <romixlev@gmail.com> */
public class ByteBufferOutput extends Output {
	static private final ByteOrder nativeOrder = ByteOrder.nativeOrder();

	protected ByteBuffer byteBuffer;
	ByteOrder byteOrder = ByteOrder.BIG_ENDIAN; // Compatible with Output by default.

	/** Creates an uninitialized Output, {@link #setBuffer(ByteBuffer)} must be called before the Output is used. */
	public ByteBufferOutput () {
	}

	/** Creates a new Output for writing to a direct {@link ByteBuffer}.
	 * @param bufferSize The size of the buffer. An exception is thrown if more bytes than this are written and {@link #flush()}
	 *           does not empty the buffer. */
	public ByteBufferOutput (int bufferSize) {
		this(bufferSize, bufferSize);
	}

	/** Creates a new Output for writing to a direct ByteBuffer.
	 * @param bufferSize The initial size of the buffer.
	 * @param maxBufferSize If {@link #flush()} does not empty the buffer, the buffer is doubled as needed until it exceeds
	 *           maxBufferSize and an exception is thrown. Can be -1 for no maximum. */
	public ByteBufferOutput (int bufferSize, int maxBufferSize) {
		if (maxBufferSize < -1) throw new IllegalArgumentException("maxBufferSize cannot be < -1: " + maxBufferSize);
		this.capacity = bufferSize;
		this.maxCapacity = maxBufferSize == -1 ? Util.maxArraySize : maxBufferSize;
		byteBuffer = ByteBuffer.allocateDirect(bufferSize);
		byteBuffer.order(byteOrder);
	}

	/** Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used. */
	public ByteBufferOutput (OutputStream outputStream) {
		this(4096, 4096);
		if (outputStream == null) throw new IllegalArgumentException("outputStream cannot be null.");
		this.outputStream = outputStream;
	}

	/** Creates a new Output for writing to an OutputStream with the specified buffer size. */
	public ByteBufferOutput (OutputStream outputStream, int bufferSize) {
		this(bufferSize, bufferSize);
		if (outputStream == null) throw new IllegalArgumentException("outputStream cannot be null.");
		this.outputStream = outputStream;
	}

	/** Creates a new Output for writing to a ByteBuffer. */
	public ByteBufferOutput (ByteBuffer buffer) {
		setBuffer(buffer);
	}

	/** Creates a new Output for writing to a ByteBuffer.
	 * @param maxBufferSize If {@link #flush()} does not empty the buffer, the buffer is doubled as needed until it exceeds
	 *           maxBufferSize and an exception is thrown. Can be -1 for no maximum. */
	public ByteBufferOutput (ByteBuffer buffer, int maxBufferSize) {
		setBuffer(buffer, maxBufferSize);
	}

	public ByteOrder order () {
		return byteOrder;
	}

	/** Sets the byte order. Default is big endian. */
	public void order (ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
		this.byteBuffer.order(byteOrder);
	}

	public OutputStream getOutputStream () {
		return outputStream;
	}

	/** Sets a new OutputStream. The position and total are reset, discarding any buffered bytes.
	 * @param outputStream May be null. */
	public void setOutputStream (OutputStream outputStream) {
		this.outputStream = outputStream;
		position = 0;
		total = 0;
	}

	/** Sets a new buffer to write to. The max size is the buffer's length.
	 * @see #setBuffer(ByteBuffer, int) */
	public void setBuffer (ByteBuffer buffer) {
		setBuffer(buffer, buffer.capacity());
	}

	/** Sets a new buffer to write to. The bytes are not copied, the old buffer is discarded and the new buffer used in its place.
	 * The byte order, position and capacity are set to match the specified buffer. The total is reset. The
	 * {@link #setOutputStream(OutputStream) OutputStream} is set to null.
	 * @param maxBufferSize If {@link #flush()} does not empty the buffer, the buffer is doubled as needed until it exceeds
	 *           maxBufferSize and an exception is thrown. Can be -1 for no maximum. */
	public void setBuffer (ByteBuffer buffer, int maxBufferSize) {
		if (buffer == null) throw new IllegalArgumentException("buffer cannot be null.");
		if (maxBufferSize < -1) throw new IllegalArgumentException("maxBufferSize cannot be < -1: " + maxBufferSize);
		this.byteBuffer = buffer;
		this.maxCapacity = maxBufferSize == -1 ? Util.maxArraySize : maxBufferSize;
		byteOrder = buffer.order();
		capacity = buffer.capacity();
		position = buffer.position();
		total = 0;
		outputStream = null;
	}

	/** Returns the buffer. The bytes between zero and {@link #position()} are the data that has been written. */
	public ByteBuffer getByteBuffer () {
		return byteBuffer;
	}

	public byte[] toBytes () {
		byte[] newBuffer = new byte[position];
		byteBuffer.position(0);
		byteBuffer.get(newBuffer, 0, position);
		return newBuffer;
	}

	public void setPosition (int position) {
		this.position = position;
		this.byteBuffer.position(position);
	}

	public void clear () {
		byteBuffer.clear();
		position = 0;
		total = 0;
	}

	protected boolean require (int required) throws KryoException {
		if (capacity - position >= required) return false;
		if (required > maxCapacity) {
			byteBuffer.order(byteOrder);
			throw new KryoException("Buffer overflow. Max capacity: " + maxCapacity + ", required: " + required);
		}
		flush();
		while (capacity - position < required) {
			if (capacity == maxCapacity) {
				byteBuffer.order(byteOrder);
				throw new KryoException("Buffer overflow. Available: " + (capacity - position) + ", required: " + required);
			}
			// Grow buffer.
			if (capacity == 0) capacity = 1;
			capacity = Math.min(capacity * 2, maxCapacity);
			if (capacity < 0) capacity = maxCapacity;
			ByteBuffer newBuffer = !byteBuffer.isDirect() ? ByteBuffer.allocate(capacity) : ByteBuffer.allocateDirect(capacity);
			// Copy the whole buffer
			byteBuffer.position(0);
			byteBuffer.limit(position);
			newBuffer.put(byteBuffer);
			newBuffer.order(byteBuffer.order());

			// writeInt & writeLong mess with the byte order, need to keep track of the current byte order when growing.
			final ByteOrder currentByteOrder = byteOrder;
			setBuffer(newBuffer, maxCapacity);
			byteOrder = currentByteOrder;
		}
		return true;
	}

	// OutputStream

	public void flush () throws KryoException {
		if (outputStream == null) return;
		try {
			byte[] tmp = new byte[position];
			byteBuffer.position(0);
			byteBuffer.get(tmp);
			byteBuffer.position(0);
			outputStream.write(tmp, 0, position);
		} catch (IOException ex) {
			throw new KryoException(ex);
		}
		total += position;
		position = 0;
	}

	public void close () throws KryoException {
		flush();
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException ignored) {
			}
		}
	}

	public void write (int value) throws KryoException {
		if (position == capacity) require(1);
		byteBuffer.put((byte)value);
		position++;
	}

	public void write (byte[] bytes) throws KryoException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		writeBytes(bytes, 0, bytes.length);
	}

	public void write (byte[] bytes, int offset, int length) throws KryoException {
		writeBytes(bytes, offset, length);
	}

	// byte

	public void writeByte (byte value) throws KryoException {
		if (position == capacity) require(1);
		byteBuffer.put(value);
		position++;
	}

	public void writeByte (int value) throws KryoException {
		if (position == capacity) require(1);
		byteBuffer.put((byte)value);
		position++;
	}

	public void writeBytes (byte[] bytes) throws KryoException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		writeBytes(bytes, 0, bytes.length);
	}

	public void writeBytes (byte[] bytes, int offset, int count) throws KryoException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		int copyCount = Math.min(capacity - position, count);
		while (true) {
			byteBuffer.put(bytes, offset, copyCount);
			position += copyCount;
			count -= copyCount;
			if (count == 0) return;
			offset += copyCount;
			copyCount = Math.min(capacity, count);
			require(copyCount);
		}
	}

	// int

	public void writeInt (int value) throws KryoException {
		require(4);
		byteBuffer.putInt(value);
		position += 4;
	}

	public int writeInt (int val, boolean optimizePositive) throws KryoException {
		byteBuffer.position(position);

		int value = val;
		if (!optimizePositive) value = (value << 1) ^ (value >> 31);
		int varInt = 0;

		varInt = (value & 0x7F);

		value >>>= 7;

		if (value == 0) {
			writeByte(varInt);
			return 1;
		}

		varInt |= 0x80;
		varInt |= ((value & 0x7F) << 8);

		value >>>= 7;

		if (value == 0) {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeInt(varInt);
			byteBuffer.order(byteOrder);
			position -= 2;
			byteBuffer.position(position);
			return 2;
		}

		varInt |= (0x80 << 8);
		varInt |= ((value & 0x7F) << 16);

		value >>>= 7;

		if (value == 0) {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeInt(varInt);
			byteBuffer.order(byteOrder);
			position -= 1;
			byteBuffer.position(position);
			return 3;
		}

		varInt |= (0x80 << 16);
		varInt |= ((value & 0x7F) << 24);

		value >>>= 7;

		if (value == 0) {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeInt(varInt);
			byteBuffer.order(byteOrder);
			position -= 0;
			return 4;
		}

		varInt |= (0x80 << 24);
		long varLong = (varInt & 0xFFFFFFFFL) | (((long)value) << 32);

		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		writeLong(varLong);
		byteBuffer.order(byteOrder);

		position -= 3;
		byteBuffer.position(position);
		return 5;
	}

	// string

	public void writeString (String value) throws KryoException {
		byteBuffer.position(position);
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
		boolean ascii = false;
		if (charCount > 1 && charCount < 64) {
			ascii = true;
			for (int i = 0; i < charCount; i++) {
				int c = value.charAt(i);
				if (c > 127) {
					ascii = false;
					break;
				}
			}
		}
		if (ascii) {
			if (capacity - position < charCount)
				writeAscii_slow(value, charCount);
			else {
				byte[] tmp = value.getBytes();
				byteBuffer.put(tmp, 0, tmp.length);
				position += charCount;
			}
			byteBuffer.put(position - 1, (byte)(byteBuffer.get(position - 1) | 0x80));
		} else {
			writeUtf8Length(charCount + 1);
			int charIndex = 0;
			if (capacity - position >= charCount) {
				// Try to write 8 bit chars.
				int position = this.position;
				for (; charIndex < charCount; charIndex++) {
					int c = value.charAt(charIndex);
					if (c > 127) break;
					byteBuffer.put(position++, (byte)c);
				}
				this.position = position;
				byteBuffer.position(position);
			}
			if (charIndex < charCount) writeString_slow(value, charCount, charIndex);
			byteBuffer.position(position);
		}
	}

	public void writeString (CharSequence value) throws KryoException {
		if (value == null) {
			writeByte(0x80); // 0 means null, bit 8 means UTF8.
			return;
		}
		int charCount = value.length();
		if (charCount == 0) {
			writeByte(1 | 0x80); // 1 means empty string, bit 8 means UTF8.
			return;
		}
		writeUtf8Length(charCount + 1);
		int charIndex = 0;
		if (capacity - position >= charCount) {
			// Try to write 8 bit chars.
			int position = this.position;
			for (; charIndex < charCount; charIndex++) {
				int c = value.charAt(charIndex);
				if (c > 127) break;
				byteBuffer.put(position++, (byte)c);
			}
			this.position = position;
			byteBuffer.position(position);
		}
		if (charIndex < charCount) writeString_slow(value, charCount, charIndex);
		byteBuffer.position(position);
	}

	public void writeAscii (String value) throws KryoException {
		if (value == null) {
			writeByte(0x80); // 0 means null, bit 8 means UTF8.
			return;
		}
		int charCount = value.length();
		if (charCount == 0) {
			writeByte(1 | 0x80); // 1 means empty string, bit 8 means UTF8.
			return;
		}
		if (capacity - position < charCount)
			writeAscii_slow(value, charCount);
		else {
			byte[] tmp = value.getBytes();
			byteBuffer.put(tmp, 0, tmp.length);
			position += charCount;
		}
		byteBuffer.put(position - 1, (byte)(byteBuffer.get(position - 1) | 0x80)); // Bit 8 means end of ASCII.
	}

	/** Writes the length of a string, which is a variable length encoded int except the first byte uses bit 8 to denote UTF8 and
	 * bit 7 to denote if another byte is present. */
	private void writeUtf8Length (int value) {
		if (value >>> 6 == 0) {
			require(1);
			byteBuffer.put((byte)(value | 0x80)); // Set bit 8.
			position += 1;
		} else if (value >>> 13 == 0) {
			require(2);
			byteBuffer.put((byte)(value | 0x40 | 0x80)); // Set bit 7 and 8.
			byteBuffer.put((byte)(value >>> 6));
			position += 2;
		} else if (value >>> 20 == 0) {
			require(3);
			byteBuffer.put((byte)(value | 0x40 | 0x80)); // Set bit 7 and 8.
			byteBuffer.put((byte)((value >>> 6) | 0x80)); // Set bit 8.
			byteBuffer.put((byte)(value >>> 13));
			position += 3;
		} else if (value >>> 27 == 0) {
			require(4);
			byteBuffer.put((byte)(value | 0x40 | 0x80)); // Set bit 7 and 8.
			byteBuffer.put((byte)((value >>> 6) | 0x80)); // Set bit 8.
			byteBuffer.put((byte)((value >>> 13) | 0x80)); // Set bit 8.
			byteBuffer.put((byte)(value >>> 20));
			position += 4;
		} else {
			require(5);
			byteBuffer.put((byte)(value | 0x40 | 0x80)); // Set bit 7 and 8.
			byteBuffer.put((byte)((value >>> 6) | 0x80)); // Set bit 8.
			byteBuffer.put((byte)((value >>> 13) | 0x80)); // Set bit 8.
			byteBuffer.put((byte)((value >>> 20) | 0x80)); // Set bit 8.
			byteBuffer.put((byte)(value >>> 27));
			position += 5;
		}
	}

	private void writeString_slow (CharSequence value, int charCount, int charIndex) {
		for (; charIndex < charCount; charIndex++) {
			if (position == capacity) require(Math.min(capacity, charCount - charIndex));
			int c = value.charAt(charIndex);
			if (c <= 0x007F) {
				byteBuffer.put(position++, (byte)c);
			} else if (c > 0x07FF) {
				byteBuffer.put(position++, (byte)(0xE0 | c >> 12 & 0x0F));
				require(2);
				byteBuffer.put(position++, (byte)(0x80 | c >> 6 & 0x3F));
				byteBuffer.put(position++, (byte)(0x80 | c & 0x3F));
			} else {
				byteBuffer.put(position++, (byte)(0xC0 | c >> 6 & 0x1F));
				require(1);
				byteBuffer.put(position++, (byte)(0x80 | c & 0x3F));
			}
		}
	}

	private void writeAscii_slow (String value, int charCount) throws KryoException {
		ByteBuffer buffer = this.byteBuffer;
		int charIndex = 0;
		int charsToWrite = Math.min(charCount, capacity - position);
		while (charIndex < charCount) {
			byte[] tmp = new byte[charCount];
			value.getBytes(charIndex, charIndex + charsToWrite, tmp, 0);
			buffer.put(tmp, 0, charsToWrite);
			charIndex += charsToWrite;
			position += charsToWrite;
			charsToWrite = Math.min(charCount - charIndex, capacity);
			if (require(charsToWrite)) buffer = this.byteBuffer;
		}
	}

	// float

	public void writeFloat (float value) throws KryoException {
		require(4);
		byteBuffer.putFloat(value);
		position += 4;
	}

	public int writeFloat (float value, float precision, boolean optimizePositive) throws KryoException {
		return writeInt((int)(value * precision), optimizePositive);
	}

	// short

	public void writeShort (int value) throws KryoException {
		require(2);
		byteBuffer.putShort((short)value);
		position += 2;
	}

	// long

	public void writeLong (long value) throws KryoException {
		require(8);
		byteBuffer.putLong(value);
		position += 8;
	}

	public int writeLong (long value, boolean optimizePositive) throws KryoException {
		if (!optimizePositive) value = (value << 1) ^ (value >> 63);
		int varInt = 0;

		varInt = (int)(value & 0x7F);

		value >>>= 7;

		if (value == 0) {
			writeByte(varInt);
			return 1;
		}

		varInt |= 0x80;
		varInt |= ((value & 0x7F) << 8);

		value >>>= 7;

		if (value == 0) {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeInt(varInt);
			byteBuffer.order(byteOrder);
			position -= 2;
			byteBuffer.position(position);
			return 2;
		}

		varInt |= (0x80 << 8);
		varInt |= ((value & 0x7F) << 16);

		value >>>= 7;

		if (value == 0) {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeInt(varInt);
			byteBuffer.order(byteOrder);
			position -= 1;
			byteBuffer.position(position);
			return 3;
		}

		varInt |= (0x80 << 16);
		varInt |= ((value & 0x7F) << 24);

		value >>>= 7;

		if (value == 0) {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeInt(varInt);
			byteBuffer.order(byteOrder);
			position -= 0;
			return 4;
		}

		varInt |= (0x80 << 24);
		long varLong = (varInt & 0xFFFFFFFFL);
		varLong |= (((long)(value & 0x7F)) << 32);

		value >>>= 7;

		if (value == 0) {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeLong(varLong);
			byteBuffer.order(byteOrder);
			position -= 3;
			byteBuffer.position(position);
			return 5;
		}

		varLong |= (0x80L << 32);
		varLong |= (((long)(value & 0x7F)) << 40);

		value >>>= 7;

		if (value == 0) {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeLong(varLong);
			byteBuffer.order(byteOrder);
			position -= 2;
			byteBuffer.position(position);
			return 6;
		}

		varLong |= (0x80L << 40);
		varLong |= (((long)(value & 0x7F)) << 48);

		value >>>= 7;

		if (value == 0) {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeLong(varLong);
			byteBuffer.order(byteOrder);
			position -= 1;
			byteBuffer.position(position);
			return 7;
		}

		varLong |= (0x80L << 48);
		varLong |= (((long)(value & 0x7F)) << 56);

		value >>>= 7;

		if (value == 0) {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeLong(varLong);
			byteBuffer.order(byteOrder);
			return 8;
		}

		varLong |= (0x80L << 56);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		writeLong(varLong);
		byteBuffer.order(byteOrder);
		write((byte)(value));
		return 9;
	}

	// boolean

	public void writeBoolean (boolean value) throws KryoException {
		require(1);
		byteBuffer.put((byte)(value ? 1 : 0));
		position++;
	}

	// char

	public void writeChar (char value) throws KryoException {
		require(2);
		byteBuffer.putChar(value);
		position += 2;
	}

	// double

	public void writeDouble (double value) throws KryoException {
		require(8);
		byteBuffer.putDouble(value);
		position += 8;
	}

	public int writeDouble (double value, double precision, boolean optimizePositive) throws KryoException {
		return writeLong((long)(value * precision), optimizePositive);
	}

	// Methods implementing bulk operations on arrays of primitive types

	public void writeInts (int[] object) throws KryoException {
		if (capacity - position >= object.length * 4 && byteOrder == nativeOrder) {
			IntBuffer buf = byteBuffer.asIntBuffer();
			buf.put(object);
			position += object.length * 4;
		} else
			super.writeInts(object);
	}

	public void writeLongs (long[] object) throws KryoException {
		if (capacity - position >= object.length * 8 && byteOrder == nativeOrder) {
			LongBuffer buf = byteBuffer.asLongBuffer();
			buf.put(object);
			position += object.length * 8;
		} else
			super.writeLongs(object);
	}

	public void writeFloats (float[] object) throws KryoException {
		if (capacity - position >= object.length * 4 && byteOrder == nativeOrder) {
			FloatBuffer buf = byteBuffer.asFloatBuffer();
			buf.put(object);
			position += object.length * 4;
		} else
			super.writeFloats(object);
	}

	public void writeShorts (short[] object) throws KryoException {
		if (capacity - position >= object.length * 2 && byteOrder == nativeOrder) {
			ShortBuffer buf = byteBuffer.asShortBuffer();
			buf.put(object);
			position += object.length * 2;
		} else
			super.writeShorts(object);
	}

	public void writeChars (char[] object) throws KryoException {
		if (capacity - position >= object.length * 2 && byteOrder == nativeOrder) {
			CharBuffer buf = byteBuffer.asCharBuffer();
			buf.put(object);
			position += object.length * 2;
		} else
			super.writeChars(object);
	}

	public void writeDoubles (double[] object) throws KryoException {
		if (capacity - position >= object.length * 8 && byteOrder == nativeOrder) {
			DoubleBuffer buf = byteBuffer.asDoubleBuffer();
			buf.put(object);
			position += object.length * 8;
		} else
			super.writeDoubles(object);
	}
}
