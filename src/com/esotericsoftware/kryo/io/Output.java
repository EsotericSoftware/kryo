
package com.esotericsoftware.kryo.io;

import java.io.IOException;
import java.io.OutputStream;

import com.esotericsoftware.kryo.KryoException;

/** An OutputStream that buffers data in a byte array and optionally flushes to another OutputStream. Utility methods are provided
 * for efficiently writing primitive types and strings. */
public class Output extends OutputStream {
	private final int maxCapacity;
	private int capacity, position, total;
	private byte[] buffer;
	private OutputStream outputStream;

	/** Creates a new Output for writing to a byte array.
	 * @param bufferSize The initial and maximum size of the buffer. An exception is thrown if this size is exceeded. */
	public Output (int bufferSize) {
		this(bufferSize, bufferSize);
	}

	/** Creates a new Output for writing to a byte array.
	 * @param bufferSize The initial size of the buffer.
	 * @param maxBufferSize The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown. */
	public Output (int bufferSize, int maxBufferSize) {
		this.capacity = bufferSize;
		this.maxCapacity = maxBufferSize == -1 ? Integer.MAX_VALUE : maxBufferSize;
		buffer = new byte[bufferSize];
	}

	/** Creates a new Output for writing to a byte array.
	 * @param buffer An exception is thrown if more bytes are written than the length of this buffer. */
	public Output (byte[] buffer) {
		this(buffer, buffer.length);
	}

	/** Creates a new Output for writing to a byte array.
	 * @param maxBufferSize The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown. */
	public Output (byte[] buffer, int maxBufferSize) {
		if (buffer == null) throw new IllegalArgumentException("buffer cannot be null.");
		this.buffer = buffer;
		this.maxCapacity = maxBufferSize;
		capacity = buffer.length;
	}

	/** Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used. */
	public Output (OutputStream outputStream) {
		this(4096, 4096);
		if (outputStream == null) throw new IllegalArgumentException("outputStream cannot be null.");
		this.outputStream = outputStream;
	}

	/** Creates a new Output for writing to an OutputStream. */
	public Output (OutputStream outputStream, int bufferSize) {
		this(bufferSize, bufferSize);
		if (outputStream == null) throw new IllegalArgumentException("outputStream cannot be null.");
		this.outputStream = outputStream;
	}

	public OutputStream getOutputStream () {
		return outputStream;
	}

	/** Sets a new OutputStream. The position and total are reset, discarding any buffered bytes. */
	public void setOutputStream (OutputStream outputStream) {
		if (outputStream == null) throw new IllegalArgumentException("outputStream cannot be null.");
		this.outputStream = outputStream;
		position = 0;
		total = 0;
	}

	public byte[] getBuffer () {
		return buffer;
	}

	/** Returns a new byte array containing the bytes currently in the buffer. */
	public byte[] toBytes () {
		byte[] newBuffer = new byte[position];
		System.arraycopy(buffer, 0, newBuffer, 0, position);
		return newBuffer;
	}

	/** Returns the current position in the buffer. This is the number of bytes that have not been flushed. */
	public int position () {
		return position;
	}

	/** Returns the total number of bytes written. This may include bytes that have not been flushed. */
	public int total () {
		return total + position;
	}

	/** Sets the position and total to zero. */
	public void clear () {
		position = 0;
		total = 0;
	}

	/** @return true if the buffer has been resized. */
	private boolean require (int required) throws KryoException {
		if (capacity - position >= required) return false;
		if (required > maxCapacity)
			throw new KryoException("Buffer overflow. Max capacity: " + maxCapacity + ", required: " + required);
		flush();
		while (capacity - position < required) {
			if (capacity == maxCapacity)
				throw new KryoException("Buffer overflow. Available: " + (capacity - position) + ", required: " + required);
			// Grow buffer.
			capacity = Math.min(capacity * 2, maxCapacity);
			byte[] newBuffer = new byte[capacity];
			System.arraycopy(buffer, 0, newBuffer, 0, position);
			buffer = newBuffer;
		}
		return true;
	}

	// OutputStream

	/** Writes the buffered bytes to the underlying OutputStream, if any. */
	public void flush () throws KryoException {
		if (outputStream == null) return;
		try {
			outputStream.write(buffer, 0, position);
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

	public void write (byte[] bytes) throws KryoException {
		writeBytes(bytes, 0, bytes.length);
	}

	public void write (byte[] bytes, int offset, int length) throws KryoException {
		writeBytes(bytes, offset, length);
	}

	// byte

	public void writeByte (int value) throws KryoException {
		if (position == capacity) require(1);
		buffer[position++] = (byte)value;
	}

	public void writeBytes (byte[] bytes) throws KryoException {
		writeBytes(bytes, 0, bytes.length);
	}

	public void writeBytes (byte[] bytes, int offset, int count) throws KryoException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		int copyCount = Math.min(capacity - position, count);
		while (true) {
			System.arraycopy(bytes, offset, buffer, position, copyCount);
			position += copyCount;
			count -= copyCount;
			if (count == 0) return;
			offset += copyCount;
			copyCount = Math.min(capacity, count);
			require(copyCount);
		}
	}

	// int

	/** Writes a 4 byte int. */
	public void writeInt (int value) throws KryoException {
		require(4);
		byte[] buffer = this.buffer;
		buffer[position++] = (byte)(value >> 24);
		buffer[position++] = (byte)(value >> 16);
		buffer[position++] = (byte)(value >> 8);
		buffer[position++] = (byte)value;
	}

	/** Writes a 1-5 byte int.
	 * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
	 *           inefficient (5 bytes). */
	public int writeInt (int value, boolean optimizePositive) throws KryoException {
		if (!optimizePositive) value = (value << 1) ^ (value >> 31);
		int length;
		if ((value & ~0x7F) == 0)
			length = 1;
		else if ((value >>> 7 & ~0x7F) == 0)
			length = 2;
		else if ((value >>> 14 & ~0x7F) == 0)
			length = 3;
		else if ((value >>> 21 & ~0x7F) == 0)
			length = 4;
		else
			length = 5;
		require(length);
		byte[] buffer = this.buffer;
		switch (length) {
		case 5:
			buffer[position++] = (byte)((value & 0x7F) | 0x80);
			value >>>= 7;
		case 4:
			buffer[position++] = (byte)((value & 0x7F) | 0x80);
			value >>>= 7;
		case 3:
			buffer[position++] = (byte)((value & 0x7F) | 0x80);
			value >>>= 7;
		case 2:
			buffer[position++] = (byte)((value & 0x7F) | 0x80);
			value >>>= 7;
		case 1:
			buffer[position++] = (byte)value;
		}
		return length;
	}

	// string

	/** Writes the length and string using 1 byte per character. */
	public void writeChars (String value) throws KryoException {
		if (value == null) {
			writeByte(0);
			return;
		}
		int charCount = value.length();
		writeInt(charCount + 1, true);
		if (capacity < charCount)
			writeChars_slow(value, charCount);
		else {
			require(charCount);
			byte[] buffer = this.buffer;
			for (int i = 0; i < charCount; i++)
				buffer[position++] = (byte)value.charAt(i);
		}
	}

	private void writeChars_slow (String value, int charCount) throws KryoException {
		byte[] buffer = this.buffer;
		int charsToWrite = capacity - position;
		int charIndex = 0;
		while (charIndex < charCount) {
			for (int n = charIndex + charsToWrite; charIndex < n; charIndex++)
				buffer[position++] = (byte)value.charAt(charIndex);
			charsToWrite = Math.min(charCount - charIndex, capacity);
			if (require(charsToWrite)) buffer = this.buffer;
		}
	}

	/** Writes the length and string using UTF8, or null.
	 * @param value May be null. */
	public void writeString (String value) throws KryoException {
		if (value == null) {
			writeByte(0);
			return;
		}
		int charCount = value.length();
		writeInt(charCount + 1, true);
		int charIndex = 0;
		if (capacity >= charCount) {
			// Try to write 8 bit chars.
			require(charCount);
			for (; charIndex < charCount; charIndex++) {
				int c = value.charAt(charIndex);
				if (c > 127) break;
				buffer[position++] = (byte)c;
			}
		}
		if (charIndex < charCount) writeString_slow(value, charCount, charIndex);
	}

	private void writeString_slow (String value, int charCount, int charIndex) {
		for (; charIndex < charCount; charIndex++) {
			if (position == capacity) require(Math.min(capacity, charCount - charIndex));
			int c = value.charAt(charIndex);
			if (c <= 0x007F) {
				buffer[position++] = (byte)c;
			} else if (c > 0x07FF) {
				buffer[position++] = (byte)(0xE0 | c >> 12 & 0x0F);
				require(2);
				buffer[position++] = (byte)(0x80 | c >> 6 & 0x3F);
				buffer[position++] = (byte)(0x80 | c & 0x3F);
			} else {
				buffer[position++] = (byte)(0xC0 | c >> 6 & 0x1F);
				require(1);
				buffer[position++] = (byte)(0x80 | c & 0x3F);
			}
		}
	}

	// float

	/** Writes a 4 byte float. */
	public void writeFloat (float value) throws KryoException {
		writeInt(Float.floatToIntBits(value));
	}

	/** Writes a 1-5 byte float with reduced precision.
	 * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
	 *           inefficient (5 bytes). */
	public int writeFloat (float value, float precision, boolean optimizePositive) throws KryoException {
		return writeInt((int)(value * precision), optimizePositive);
	}

	// short

	/** Writes a 2 byte short. */
	public void writeShort (int value) throws KryoException {
		require(2);
		buffer[position++] = (byte)(value >>> 8);
		buffer[position++] = (byte)value;
	}

	/** Writes a 1-3 byte short.
	 * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
	 *           inefficient (3 bytes). */
	public int writeShort (int value, boolean optimizePositive) throws KryoException {
		if (optimizePositive) {
			if (value >= 0 && value <= 254) {
				require(1);
				buffer[position++] = (byte)value;
				return 1;
			}
			require(3);
			buffer[position++] = -1; // short positive
		} else {
			if (value >= -127 && value <= 127) {
				require(1);
				buffer[position++] = (byte)value;
				return 1;
			}
			require(3);
			buffer[position++] = -128; // short
		}
		buffer[position++] = (byte)(value >>> 8);
		buffer[position++] = (byte)value;
		return 3;
	}

	// long

	/** Writes an 8 byte long. */
	public void writeLong (long value) throws KryoException {
		require(8);
		byte[] buffer = this.buffer;
		buffer[position++] = (byte)(value >>> 56);
		buffer[position++] = (byte)(value >>> 48);
		buffer[position++] = (byte)(value >>> 40);
		buffer[position++] = (byte)(value >>> 32);
		buffer[position++] = (byte)(value >>> 24);
		buffer[position++] = (byte)(value >>> 16);
		buffer[position++] = (byte)(value >>> 8);
		buffer[position++] = (byte)value;
	}

	/** Writes a 1-10 byte long.
	 * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
	 *           inefficient (10 bytes). */
	public int writeLong (long value, boolean optimizePositive) throws KryoException {
		if (!optimizePositive) value = (value << 1) ^ (value >> 63);
		int length;
		if ((value & ~0x7Fl) == 0)
			length = 1;
		else if ((value >>> 7 & ~0x7Fl) == 0)
			length = 2;
		else if ((value >>> 14 & ~0x7Fl) == 0)
			length = 3;
		else if ((value >>> 21 & ~0x7Fl) == 0)
			length = 4;
		else if ((value >>> 28 & ~0x7Fl) == 0)
			length = 5;
		else if ((value >>> 35 & ~0x7Fl) == 0)
			length = 6;
		else if ((value >>> 42 & ~0x7Fl) == 0)
			length = 7;
		else if ((value >>> 49 & ~0x7Fl) == 0)
			length = 8;
		else if ((value >>> 56 & ~0x7Fl) == 0)
			length = 9;
		else
			length = 10;
		require(length);
		byte[] buffer = this.buffer;
		switch (length) {
		case 10:
			buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
			value >>>= 7;
		case 9:
			buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
			value >>>= 7;
		case 8:
			buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
			value >>>= 7;
		case 7:
			buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
			value >>>= 7;
		case 6:
			buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
			value >>>= 7;
		case 5:
			buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
			value >>>= 7;
		case 4:
			buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
			value >>>= 7;
		case 3:
			buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
			value >>>= 7;
		case 2:
			buffer[position++] = (byte)(((int)value & 0x7F) | 0x80);
			value >>>= 7;
		case 1:
			buffer[position++] = (byte)value;
		}
		return length;
	}

	// boolean

	/** Writes a 1 byte boolean. */
	public void writeBoolean (boolean value) throws KryoException {
		require(1);
		buffer[position++] = (byte)(value ? 1 : 0);
	}

	// char

	/** Writes a 2 byte char. */
	public void writeChar (char value) throws KryoException {
		require(2);
		buffer[position++] = (byte)(value >>> 8);
		buffer[position++] = (byte)value;
	}

	// double

	/** Writes an 8 byte double. */
	public void writeDouble (double value) throws KryoException {
		writeLong(Double.doubleToLongBits(value));
	}

	/** Writes a 1-10 byte double with reduced precision.
	 * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
	 *           inefficient (10 bytes). */
	public int writeDouble (double value, double precision, boolean optimizePositive) throws KryoException {
		return writeLong((long)(value * precision), optimizePositive);
	}
}
