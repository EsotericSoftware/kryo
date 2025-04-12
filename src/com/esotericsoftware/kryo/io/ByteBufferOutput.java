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
import com.esotericsoftware.kryo.util.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** An {@link Output} that uses a ByteBuffer rather than a byte[].
 * <p>
 * Note that the byte[] {@link #getBuffer() buffer} is not used. Code taking an Output and expecting the byte[] to be used may not
 * work correctly.
 * @author Roman Levenstein <romixlev@gmail.com>
 * @author Nathan Sweet */
public class ByteBufferOutput extends Output {
	private static final ByteOrder nativeOrder = ByteOrder.nativeOrder();

	protected ByteBuffer byteBuffer;

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

	/** @see Output#Output(OutputStream) */
	public ByteBufferOutput (OutputStream outputStream) {
		this(4096, 4096);
		if (outputStream == null) throw new IllegalArgumentException("outputStream cannot be null.");
		this.outputStream = outputStream;
	}

	/** @see Output#Output(OutputStream, int) */
	public ByteBufferOutput (OutputStream outputStream, int bufferSize) {
		this(bufferSize, bufferSize);
		if (outputStream == null) throw new IllegalArgumentException("outputStream cannot be null.");
		this.outputStream = outputStream;
	}

	public OutputStream getOutputStream () {
		return outputStream;
	}

	/** Throws {@link UnsupportedOperationException} because this output uses a ByteBuffer, not a byte[].
	 * @deprecated
	 * @see #getByteBuffer() */
	public byte[] getBuffer () {
		throw new UnsupportedOperationException("This buffer does not used a byte[], see #getByteBuffer().");
	}

	/** Throws {@link UnsupportedOperationException} because this output uses a ByteBuffer, not a byte[].
	 * @deprecated
	 * @see #getByteBuffer() */
	public void setBuffer (byte[] buffer) {
		throw new UnsupportedOperationException("This buffer does not used a byte[], see #setByteBuffer(ByteBuffer).");
	}

	/** Throws {@link UnsupportedOperationException} because this output uses a ByteBuffer, not a byte[].
	 * @deprecated
	 * @see #getByteBuffer() */
	public void setBuffer (byte[] buffer, int maxBufferSize) {
		throw new UnsupportedOperationException("This buffer does not used a byte[], see #setByteBuffer(ByteBuffer).");
	}

	/** Allocates a new direct ByteBuffer with the specified bytes and sets it as the new buffer.
	 * @see #setBuffer(ByteBuffer) */
	public void setBuffer (byte[] bytes, int offset, int count) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
		buffer.put(bytes, offset, count);
		setBufferPosition(buffer, 0);
		setBufferLimit(buffer, bytes.length);
		setBuffer(buffer);
	}

	/** Sets a new buffer to write to. The max size is the buffer's length.
	 * @see #setBuffer(ByteBuffer, int) */
	public void setBuffer (ByteBuffer buffer) {
		setBuffer(buffer, buffer.capacity());
	}

	/** Sets a new buffer to write to. The bytes are not copied, the old buffer is discarded and the new buffer used in its place.
	 * The position and capacity are set to match the specified buffer. The total is reset. The
	 * {@link #setOutputStream(OutputStream) OutputStream} is set to null.
	 * @param maxBufferSize If {@link #flush()} does not empty the buffer, the buffer is doubled as needed until it exceeds
	 *           maxBufferSize and an exception is thrown. Can be -1 for no maximum. */
	public void setBuffer (ByteBuffer buffer, int maxBufferSize) {
		if (buffer == null) throw new IllegalArgumentException("buffer cannot be null.");
		if (maxBufferSize < -1) throw new IllegalArgumentException("maxBufferSize cannot be < -1: " + maxBufferSize);
		this.byteBuffer = buffer;
		this.maxCapacity = maxBufferSize == -1 ? Util.maxArraySize : maxBufferSize;
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
		setBufferPosition(byteBuffer, 0);
		byteBuffer.get(newBuffer, 0, position);
		return newBuffer;
	}

	public void setPosition (int position) {
		this.position = position;
		setBufferPosition(byteBuffer, position);
	}

	public void reset () {
		super.reset();
		setBufferPosition(byteBuffer, 0);
	}

	private int getBufferPosition (Buffer buffer) {
		return buffer.position();
	}

	private void setBufferPosition (Buffer buffer, int newPosition) {
		buffer.position(newPosition);
	}

	private void setBufferLimit (Buffer buffer, int length) {
		buffer.limit(length);
	}

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
		ByteBuffer newBuffer = !byteBuffer.isDirect() ? ByteBuffer.allocate(capacity) : ByteBuffer.allocateDirect(capacity);
		setBufferPosition(byteBuffer, 0);
		setBufferLimit(byteBuffer, position);
		newBuffer.put(byteBuffer);
		newBuffer.order(byteBuffer.order());
		byteBuffer = newBuffer;
		return true;
	}

	// OutputStream:

	public void flush () throws KryoException {
		if (outputStream == null) return;
		try {
			byte[] tmp = new byte[position];
			setBufferPosition(byteBuffer, 0);
			byteBuffer.get(tmp);
			setBufferPosition(byteBuffer, 0);
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

	// byte:

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

	public void writeInt (int bytes, int count) {
		if (count < 0 || count > 4) throw new IllegalArgumentException("count must be >= 0 and <= 4: " + count);
		require(count);
		position += count;
		ByteBuffer byteBuffer = this.byteBuffer;
		switch (count) {
			case 1:
				byteBuffer.put((byte)bytes);
				break;
			case 2:
				byteBuffer.put((byte)(bytes >> 8));
				byteBuffer.put((byte)bytes);
				break;
			case 3:
				byteBuffer.put((byte)(bytes >> 16));
				byteBuffer.put((byte)(bytes >> 8));
				byteBuffer.put((byte)bytes);
				break;
			case 4:
				byteBuffer.put((byte)(bytes >> 24));
				byteBuffer.put((byte)(bytes >> 16));
				byteBuffer.put((byte)(bytes >> 8));
				byteBuffer.put((byte)bytes);
				break;
		}
	}

	// int:

	public void writeInt (int value) throws KryoException {
		require(4);
		position += 4;
		ByteBuffer byteBuffer = this.byteBuffer;
		byteBuffer.put((byte)value);
		byteBuffer.put((byte)(value >> 8));
		byteBuffer.put((byte)(value >> 16));
		byteBuffer.put((byte)(value >> 24));
	}

	public int writeVarInt (int value, boolean optimizePositive) throws KryoException {
		if (!optimizePositive) value = (value << 1) ^ (value >> 31);
		if (value >>> 7 == 0) {
			if (position == capacity) require(1);
			position++;
			byteBuffer.put((byte)value);
			return 1;
		}
		if (value >>> 14 == 0) {
			require(2);
			position += 2;
			byteBuffer.put((byte)((value & 0x7F) | 0x80));
			byteBuffer.put((byte)(value >>> 7));
			return 2;
		}
		if (value >>> 21 == 0) {
			require(3);
			position += 3;
			ByteBuffer byteBuffer = this.byteBuffer;
			byteBuffer.put((byte)((value & 0x7F) | 0x80));
			byteBuffer.put((byte)(value >>> 7 | 0x80));
			byteBuffer.put((byte)(value >>> 14));
			return 3;
		}
		if (value >>> 28 == 0) {
			require(4);
			position += 4;
			ByteBuffer byteBuffer = this.byteBuffer;
			byteBuffer.put((byte)((value & 0x7F) | 0x80));
			byteBuffer.put((byte)(value >>> 7 | 0x80));
			byteBuffer.put((byte)(value >>> 14 | 0x80));
			byteBuffer.put((byte)(value >>> 21));
			return 4;
		}
		require(5);
		position += 5;
		ByteBuffer byteBuffer = this.byteBuffer;
		byteBuffer.put((byte)((value & 0x7F) | 0x80));
		byteBuffer.put((byte)(value >>> 7 | 0x80));
		byteBuffer.put((byte)(value >>> 14 | 0x80));
		byteBuffer.put((byte)(value >>> 21 | 0x80));
		byteBuffer.put((byte)(value >>> 28));
		return 5;
	}

	public int writeVarIntFlag (boolean flag, int value, boolean optimizePositive) throws KryoException {
		if (!optimizePositive) value = (value << 1) ^ (value >> 31);
		int first = (value & 0x3F) | (flag ? 0x80 : 0); // Mask first 6 bits, bit 8 is the flag.
		if (value >>> 6 == 0) {
			if (position == capacity) require(1);
			byteBuffer.put((byte)first);
			position++;
			return 1;
		}
		if (value >>> 13 == 0) {
			require(2);
			position += 2;
			byteBuffer.put((byte)(first | 0x40)); // Set bit 7.
			byteBuffer.put((byte)(value >>> 6));
			return 2;
		}
		if (value >>> 20 == 0) {
			require(3);
			position += 3;
			ByteBuffer byteBuffer = this.byteBuffer;
			byteBuffer.put((byte)(first | 0x40)); // Set bit 7.
			byteBuffer.put((byte)((value >>> 6) | 0x80)); // Set bit 8.
			byteBuffer.put((byte)(value >>> 13));
			return 3;
		}
		if (value >>> 27 == 0) {
			require(4);
			position += 4;
			ByteBuffer byteBuffer = this.byteBuffer;
			byteBuffer.put((byte)(first | 0x40)); // Set bit 7.
			byteBuffer.put((byte)((value >>> 6) | 0x80)); // Set bit 8.
			byteBuffer.put((byte)((value >>> 13) | 0x80)); // Set bit 8.
			byteBuffer.put((byte)(value >>> 20));
			return 4;
		}
		require(5);
		position += 5;
		ByteBuffer byteBuffer = this.byteBuffer;
		byteBuffer.put((byte)(first | 0x40)); // Set bit 7.
		byteBuffer.put((byte)((value >>> 6) | 0x80)); // Set bit 8.
		byteBuffer.put((byte)((value >>> 13) | 0x80)); // Set bit 8.
		byteBuffer.put((byte)((value >>> 20) | 0x80)); // Set bit 8.
		byteBuffer.put((byte)(value >>> 27));
		return 5;
	}

	// long:

	public void writeLong (long value) throws KryoException {
		require(8);
		position += 8;
		ByteBuffer byteBuffer = this.byteBuffer;
		byteBuffer.put((byte)value);
		byteBuffer.put((byte)(value >>> 8));
		byteBuffer.put((byte)(value >>> 16));
		byteBuffer.put((byte)(value >>> 24));
		byteBuffer.put((byte)(value >>> 32));
		byteBuffer.put((byte)(value >>> 40));
		byteBuffer.put((byte)(value >>> 48));
		byteBuffer.put((byte)(value >>> 56));
	}

	public int writeVarLong (long value, boolean optimizePositive) throws KryoException {
		if (!optimizePositive) value = (value << 1) ^ (value >> 63);
		if (value >>> 7 == 0) {
			if (position == capacity) require(1);
			position++;
			byteBuffer.put((byte)value);
			return 1;
		}
		if (value >>> 14 == 0) {
			require(2);
			position += 2;
			byteBuffer.put((byte)((value & 0x7F) | 0x80));
			byteBuffer.put((byte)(value >>> 7));
			return 2;
		}
		if (value >>> 21 == 0) {
			require(3);
			position += 3;
			ByteBuffer byteBuffer = this.byteBuffer;
			byteBuffer.put((byte)((value & 0x7F) | 0x80));
			byteBuffer.put((byte)(value >>> 7 | 0x80));
			byteBuffer.put((byte)(value >>> 14));
			return 3;
		}
		if (value >>> 28 == 0) {
			require(4);
			position += 4;
			ByteBuffer byteBuffer = this.byteBuffer;
			byteBuffer.put((byte)((value & 0x7F) | 0x80));
			byteBuffer.put((byte)(value >>> 7 | 0x80));
			byteBuffer.put((byte)(value >>> 14 | 0x80));
			byteBuffer.put((byte)(value >>> 21));
			return 4;
		}
		if (value >>> 35 == 0) {
			require(5);
			position += 5;
			ByteBuffer byteBuffer = this.byteBuffer;
			byteBuffer.put((byte)((value & 0x7F) | 0x80));
			byteBuffer.put((byte)(value >>> 7 | 0x80));
			byteBuffer.put((byte)(value >>> 14 | 0x80));
			byteBuffer.put((byte)(value >>> 21 | 0x80));
			byteBuffer.put((byte)(value >>> 28));
			return 5;
		}
		if (value >>> 42 == 0) {
			require(6);
			position += 6;
			ByteBuffer byteBuffer = this.byteBuffer;
			byteBuffer.put((byte)((value & 0x7F) | 0x80));
			byteBuffer.put((byte)(value >>> 7 | 0x80));
			byteBuffer.put((byte)(value >>> 14 | 0x80));
			byteBuffer.put((byte)(value >>> 21 | 0x80));
			byteBuffer.put((byte)(value >>> 28 | 0x80));
			byteBuffer.put((byte)(value >>> 35));
			return 6;
		}
		if (value >>> 49 == 0) {
			require(7);
			position += 7;
			ByteBuffer byteBuffer = this.byteBuffer;
			byteBuffer.put((byte)((value & 0x7F) | 0x80));
			byteBuffer.put((byte)(value >>> 7 | 0x80));
			byteBuffer.put((byte)(value >>> 14 | 0x80));
			byteBuffer.put((byte)(value >>> 21 | 0x80));
			byteBuffer.put((byte)(value >>> 28 | 0x80));
			byteBuffer.put((byte)(value >>> 35 | 0x80));
			byteBuffer.put((byte)(value >>> 42));
			return 7;
		}
		if (value >>> 56 == 0) {
			require(8);
			position += 8;
			ByteBuffer byteBuffer = this.byteBuffer;
			byteBuffer.put((byte)((value & 0x7F) | 0x80));
			byteBuffer.put((byte)(value >>> 7 | 0x80));
			byteBuffer.put((byte)(value >>> 14 | 0x80));
			byteBuffer.put((byte)(value >>> 21 | 0x80));
			byteBuffer.put((byte)(value >>> 28 | 0x80));
			byteBuffer.put((byte)(value >>> 35 | 0x80));
			byteBuffer.put((byte)(value >>> 42 | 0x80));
			byteBuffer.put((byte)(value >>> 49));
			return 8;
		}
		require(9);
		position += 9;
		ByteBuffer byteBuffer = this.byteBuffer;
		byteBuffer.put((byte)((value & 0x7F) | 0x80));
		byteBuffer.put((byte)(value >>> 7 | 0x80));
		byteBuffer.put((byte)(value >>> 14 | 0x80));
		byteBuffer.put((byte)(value >>> 21 | 0x80));
		byteBuffer.put((byte)(value >>> 28 | 0x80));
		byteBuffer.put((byte)(value >>> 35 | 0x80));
		byteBuffer.put((byte)(value >>> 42 | 0x80));
		byteBuffer.put((byte)(value >>> 49 | 0x80));
		byteBuffer.put((byte)(value >>> 56));
		return 9;
	}

	// float:

	public void writeFloat (float value) throws KryoException {
		require(4);
		ByteBuffer byteBuffer = this.byteBuffer;
		position += 4;
		int intValue = Float.floatToIntBits(value);
		byteBuffer.put((byte)intValue);
		byteBuffer.put((byte)(intValue >> 8));
		byteBuffer.put((byte)(intValue >> 16));
		byteBuffer.put((byte)(intValue >> 24));
	}

	// double:

	public void writeDouble (double value) throws KryoException {
		require(8);
		position += 8;
		ByteBuffer byteBuffer = this.byteBuffer;
		long longValue = Double.doubleToLongBits(value);
		byteBuffer.put((byte)longValue);
		byteBuffer.put((byte)(longValue >>> 8));
		byteBuffer.put((byte)(longValue >>> 16));
		byteBuffer.put((byte)(longValue >>> 24));
		byteBuffer.put((byte)(longValue >>> 32));
		byteBuffer.put((byte)(longValue >>> 40));
		byteBuffer.put((byte)(longValue >>> 48));
		byteBuffer.put((byte)(longValue >>> 56));
	}

	// short:

	public void writeShort (int value) throws KryoException {
		require(2);
		position += 2;
		byteBuffer.put((byte)value);
		byteBuffer.put((byte)(value >>> 8));
	}

	// char:

	public void writeChar (char value) throws KryoException {
		require(2);
		position += 2;
		byteBuffer.put((byte)value);
		byteBuffer.put((byte)(value >>> 8));
	}

	// boolean:

	public void writeBoolean (boolean value) throws KryoException {
		if (position == capacity) require(1);
		byteBuffer.put((byte)(value ? 1 : 0));
		position++;
	}

	// String:

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
				for (int i = 0, n = value.length(); i < n; ++i)
					byteBuffer.put((byte)value.charAt(i));
				position += charCount;
			}
			byteBuffer.put(position - 1, (byte)(byteBuffer.get(position - 1) | 0x80));
			return;
		}
		writeVarIntFlag(true, charCount + 1, true);
		int charIndex = 0;
		if (capacity - position >= charCount) {
			// Try to write 7 bit chars.
			ByteBuffer byteBuffer = this.byteBuffer;
			while (true) {
				int c = value.charAt(charIndex);
				if (c > 127) break;
				byteBuffer.put((byte)c);
				charIndex++;
				if (charIndex == charCount) {
					position = getBufferPosition(byteBuffer);
					return;
				}
			}
			position = getBufferPosition(byteBuffer);
		}
		if (charIndex < charCount) writeUtf8_slow(value, charCount, charIndex);
	}

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
			byteBuffer.put((byte)(2 | 0x80)); // 2 is string length + 1, bit 8 means UTF8.
			byteBuffer.put((byte)value.charAt(0));
			position += 2;
			return;
		}
		if (capacity - position < charCount)
			writeAscii_slow(value, charCount);
		else {
			ByteBuffer byteBuffer = this.byteBuffer;
			for (int i = 0, n = value.length(); i < n; ++i)
				byteBuffer.put((byte)value.charAt(i));
			position += charCount;
		}
		byteBuffer.put(position - 1, (byte)(byteBuffer.get(position - 1) | 0x80)); // Bit 8 means end of ASCII.
	}

	private void writeUtf8_slow (String value, int charCount, int charIndex) {
		for (; charIndex < charCount; charIndex++) {
			if (position == capacity) require(Math.min(capacity, charCount - charIndex));
			position++;
			int c = value.charAt(charIndex);
			if (c <= 0x007F)
				byteBuffer.put((byte)c);
			else if (c > 0x07FF) {
				byteBuffer.put((byte)(0xE0 | c >> 12 & 0x0F));
				require(2);
				position += 2;
				byteBuffer.put((byte)(0x80 | c >> 6 & 0x3F));
				byteBuffer.put((byte)(0x80 | c & 0x3F));
			} else {
				byteBuffer.put((byte)(0xC0 | c >> 6 & 0x1F));
				if (position == capacity) require(1);
				position++;
				byteBuffer.put((byte)(0x80 | c & 0x3F));
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

	// Primitive arrays:

	public void writeInts (int[] array, int offset, int count) throws KryoException {
		if (capacity >= count << 2) {
			require(count << 2);
			ByteBuffer byteBuffer = this.byteBuffer;
			for (int n = offset + count; offset < n; offset++) {
				int value = array[offset];
				byteBuffer.put((byte)value);
				byteBuffer.put((byte)(value >> 8));
				byteBuffer.put((byte)(value >> 16));
				byteBuffer.put((byte)(value >> 24));
			}
			position = getBufferPosition(byteBuffer);
		} else {
			for (int n = offset + count; offset < n; offset++)
				writeInt(array[offset]);
		}
	}

	public void writeLongs (long[] array, int offset, int count) throws KryoException {
		if (capacity >= count << 3) {
			require(count << 3);
			ByteBuffer byteBuffer = this.byteBuffer;
			for (int n = offset + count; offset < n; offset++) {
				long value = array[offset];
				byteBuffer.put((byte)value);
				byteBuffer.put((byte)(value >>> 8));
				byteBuffer.put((byte)(value >>> 16));
				byteBuffer.put((byte)(value >>> 24));
				byteBuffer.put((byte)(value >>> 32));
				byteBuffer.put((byte)(value >>> 40));
				byteBuffer.put((byte)(value >>> 48));
				byteBuffer.put((byte)(value >>> 56));
			}
			position = getBufferPosition(byteBuffer);
		} else {
			for (int n = offset + count; offset < n; offset++)
				writeLong(array[offset]);
		}
	}

	public void writeFloats (float[] array, int offset, int count) throws KryoException {
		if (capacity >= count << 2) {
			require(count << 2);
			ByteBuffer byteBuffer = this.byteBuffer;
			for (int n = offset + count; offset < n; offset++) {
				int value = Float.floatToIntBits(array[offset]);
				byteBuffer.put((byte)value);
				byteBuffer.put((byte)(value >> 8));
				byteBuffer.put((byte)(value >> 16));
				byteBuffer.put((byte)(value >> 24));
			}
			position = getBufferPosition(byteBuffer);
		} else {
			for (int n = offset + count; offset < n; offset++)
				writeFloat(array[offset]);
		}
	}

	public void writeDoubles (double[] array, int offset, int count) throws KryoException {
		if (capacity >= count << 3) {
			require(count << 3);
			ByteBuffer byteBuffer = this.byteBuffer;
			for (int n = offset + count; offset < n; offset++) {
				long value = Double.doubleToLongBits(array[offset]);
				byteBuffer.put((byte)value);
				byteBuffer.put((byte)(value >>> 8));
				byteBuffer.put((byte)(value >>> 16));
				byteBuffer.put((byte)(value >>> 24));
				byteBuffer.put((byte)(value >>> 32));
				byteBuffer.put((byte)(value >>> 40));
				byteBuffer.put((byte)(value >>> 48));
				byteBuffer.put((byte)(value >>> 56));
			}
			position = getBufferPosition(byteBuffer);
		} else {
			for (int n = offset + count; offset < n; offset++)
				writeDouble(array[offset]);
		}
	}

	public void writeShorts (short[] array, int offset, int count) throws KryoException {
		if (capacity >= count << 1) {
			require(count << 1);
			for (int n = offset + count; offset < n; offset++) {
				int value = array[offset];
				byteBuffer.put((byte)value);
				byteBuffer.put((byte)(value >>> 8));
			}
			position = getBufferPosition(byteBuffer);
		} else {
			for (int n = offset + count; offset < n; offset++)
				writeShort(array[offset]);
		}
	}

	public void writeChars (char[] array, int offset, int count) throws KryoException {
		if (capacity >= count << 1) {
			require(count << 1);
			for (int n = offset + count; offset < n; offset++) {
				int value = array[offset];
				byteBuffer.put((byte)value);
				byteBuffer.put((byte)(value >>> 8));
			}
			position = getBufferPosition(byteBuffer);
		} else {
			for (int n = offset + count; offset < n; offset++)
				writeChar(array[offset]);
		}
	}

	public void writeBooleans (boolean[] array, int offset, int count) throws KryoException {
		if (capacity >= count) {
			require(count);
			for (int n = offset + count; offset < n; offset++)
				byteBuffer.put(array[offset] ? (byte)1 : 0);
			position = getBufferPosition(byteBuffer);
		} else {
			for (int n = offset + count; offset < n; offset++)
				writeBoolean(array[offset]);
		}
	}
}
