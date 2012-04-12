
package com.esotericsoftware.kryo.io;

import java.io.IOException;
import java.io.InputStream;

import com.esotericsoftware.kryo.KryoException;

/** An InputStream that reads data from a byte array and optionally fills the byte array from another OutputStream as needed.
 * Utility methods are provided for efficiently reading primitive types and strings. */
public class Input extends InputStream {
	private byte[] buffer;
	private int capacity, position, limit, total;
	private char[] chars = new char[0];
	private InputStream inputStream;

	/** Creates a new Input for reading from a byte array.
	 * @param bufferSize The size of the buffer. An exception is thrown if more bytes than this are read. */
	public Input (int bufferSize) {
		this.capacity = bufferSize;
		buffer = new byte[bufferSize];
	}

	/** Creates a new Input for reading from a byte array.
	 * @param buffer An exception is thrown if more bytes than this are read. */
	public Input (byte[] buffer) {
		setBuffer(buffer, 0, buffer.length);
	}

	/** Creates a new Input for reading from a byte array.
	 * @param buffer An exception is thrown if more bytes than this are read. */
	public Input (byte[] buffer, int offset, int count) {
		setBuffer(buffer, offset, count);
	}

	/** Creates a new Input for reading from an InputStream with a buffer size of 4096. */
	public Input (InputStream inputStream) {
		this(4096);
		if (inputStream == null) throw new IllegalArgumentException("inputStream cannot be null.");
		this.inputStream = inputStream;
	}

	/** Creates a new Input for reading from an InputStream. */
	public Input (InputStream inputStream, int bufferSize) {
		this(bufferSize);
		if (inputStream == null) throw new IllegalArgumentException("inputStream cannot be null.");
		this.inputStream = inputStream;
	}

	/** Sets a new buffer. The position and total are reset, discarding any buffered bytes. */
	public void setBuffer (byte[] bytes) {
		setBuffer(bytes, 0, bytes.length);
	}

	/** Sets a new buffer. The position and total are reset, discarding any buffered bytes. */
	public void setBuffer (byte[] bytes, int offset, int count) {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		buffer = bytes;
		position = offset;
		limit = count;
		capacity = bytes.length;
		total = 0;
		inputStream = null;
	}

	public InputStream getInputStream () {
		return inputStream;
	}

	/** Sets a new InputStream. The position and total are reset, discarding any buffered bytes. */
	public void setInputStream (InputStream inputStream) {
		if (inputStream == null) throw new IllegalArgumentException("inputStream cannot be null.");
		this.inputStream = inputStream;
		limit = 0;
		rewind();
	}

	/** Returns the number of bytes read. */
	public int total () {
		return total + position;
	}

	/** Returns the current position in the buffer. */
	public int position () {
		return position;
	}

	/** Sets the current position in the buffer. */
	public void setPosition (int position) {
		this.position = position;
	}

	/** Sets the position and total to zero. */
	public void rewind () {
		position = 0;
		total = 0;
	}

	/** Discards the specified number of bytes. */
	public void skip (int count) throws KryoException {
		int skipCount = Math.min(limit - position, count);
		while (true) {
			position += skipCount;
			count -= skipCount;
			if (count == 0) break;
			skipCount = Math.min(count, capacity);
			require(skipCount, skipCount);
		}
	}

	/** Fills the buffer with more bytes. Can be overridden to fill the bytes from a source other than the InputStream. */
	protected int fill (byte[] buffer, int offset, int count) throws KryoException {
		if (inputStream == null) return -1;
		try {
			return inputStream.read(buffer, offset, count);
		} catch (IOException ex) {
			throw new KryoException(ex);
		}
	}

	/** @param required Must be > 0. The buffer is filled until it has at least this many bytes.
	 * @param optional Try to fill at least this many bytes in buffer.
	 * @return the number of bytes remaining, but not more than optional.
	 * @throws KryoException if EOS is reached before required bytes are read (buffer underflow). */
	private int require (int required, int optional) throws KryoException {
		int remaining = limit - position;
		if (remaining >= required) return Math.min(remaining, optional);

		if (required > capacity) throw new KryoException("Buffer too small: capacity: " + capacity + ", required: " + required);
		optional = Math.min(optional, capacity);

		// Compact.
		System.arraycopy(buffer, position, buffer, 0, remaining);
		total += position;
		position = 0;

		while (true) {
			int count = fill(buffer, remaining, capacity - remaining);
			if (count == -1) {
				if (remaining >= required) break;
				throw new KryoException("Buffer underflow.");
			}
			remaining += count;
			if (remaining >= required) break; // Enough has been read.
		}
		limit = remaining;
		return Math.min(remaining, optional);
	}

	/** @param optional Try to fill the buffer with this many bytes.
	 * @return the number of bytes remaining, but not more than optional, or -1 if the EOS was reached and the buffer is empty. */
	private int optional (int optional) throws KryoException {
		int remaining = limit - position;
		if (remaining >= optional) return optional;
		optional = Math.min(optional, capacity);

		// Compact.
		System.arraycopy(buffer, position, buffer, 0, remaining);
		total += position;
		position = 0;

		while (true) {
			int count = fill(buffer, remaining, capacity - remaining);
			if (count == -1) break;
			remaining += count;
			if (remaining >= optional) break; // Enough has been read.
		}
		limit = remaining;
		return remaining == 0 ? -1 : Math.min(remaining, optional);
	}

	// InputStream

	/** Reads a byte. */
	public int read () throws KryoException {
		require(1, 1);
		return buffer[position++];
	}

	public int read (byte[] bytes) throws KryoException {
		return read(bytes, 0, bytes.length);
	}

	public int read (byte[] bytes, int offset, int count) throws KryoException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		int startingCount = count;
		int copyCount = Math.min(limit - position, count);
		while (true) {
			System.arraycopy(buffer, position, bytes, offset, copyCount);
			position += copyCount;
			count -= copyCount;
			if (count == 0) break;
			offset += copyCount;
			copyCount = optional(count);
			if (copyCount == -1) {
				// End of data.
				if (startingCount == count) return -1;
				break;
			}
			if (position == limit) break;
		}
		return startingCount - count;
	}

	/** Discards the specified number of bytes. */
	public long skip (long count) throws KryoException {
		long remaining = count;
		while (remaining > 0) {
			int skip = Math.max(Integer.MAX_VALUE, (int)remaining);
			skip(skip);
			remaining -= skip;
		}
		return count;
	}

	/** Closes the underlying InputStream, if any. */
	public void close () throws KryoException {
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException ignored) {
			}
		}
	}

	// byte

	public byte readByte () throws KryoException {
		require(1, 1);
		return buffer[position++];
	}

	/** Reads a byte as an int from 0 to 255. */
	public int readByteUnsigned () throws KryoException {
		require(1, 1);
		return buffer[position++] & 0xFF;
	}

	public byte[] readBytes (int length) throws KryoException {
		byte[] bytes = new byte[length];
		readBytes(bytes, 0, length);
		return bytes;
	}

	public void readBytes (byte[] bytes) throws KryoException {
		readBytes(bytes, 0, bytes.length);
	}

	public void readBytes (byte[] bytes, int offset, int count) throws KryoException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		int copyCount = Math.min(limit - position, count);
		while (true) {
			System.arraycopy(buffer, position, bytes, offset, copyCount);
			position += copyCount;
			count -= copyCount;
			if (count == 0) break;
			offset += copyCount;
			copyCount = Math.min(count, capacity);
			require(copyCount, copyCount);
		}
	}

	// int

	/** Reads a 4 byte int. */
	public int readInt () throws KryoException {
		require(4, 4);
		byte[] buffer = this.buffer;
		return (buffer[position++] & 0xFF) << 24 //
			| (buffer[position++] & 0xFF) << 16 //
			| (buffer[position++] & 0xFF) << 8 //
			| buffer[position++] & 0xFF;
	}

	/** Reads a 1-5 byte int. */
	public int readInt (boolean optimizePositive) throws KryoException {
		if (require(1, 5) < 5) return readInt_slow(optimizePositive);
		int b = buffer[position++];
		int result = b & 0x7F;
		if ((b & 0x80) != 0) {
			b = buffer[position++];
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				b = buffer[position++];
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					b = buffer[position++];
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						b = buffer[position++];
						result |= (b & 0x7F) << 28;
					}
				}
			}
		}
		return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
	}

	private int readInt_slow (boolean optimizePositive) {
		// The buffer is guaranteed to have at least 1 byte.
		int b = buffer[position++];
		int result = b & 0x7F;
		if ((b & 0x80) != 0) {
			require(1, 4);
			b = buffer[position++];
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				require(1, 3);
				b = buffer[position++];
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					require(1, 2);
					b = buffer[position++];
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						require(1, 1);
						b = buffer[position++];
						result |= (b & 0x7F) << 28;
					}
				}
			}
		}
		return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
	}

	/** Returns true if enough bytes are available to read an int with {@link #readInt(boolean)}. */
	public boolean canReadInt () throws KryoException {
		if (limit - position >= 5) return true;
		if (optional(5) <= 0) return false;
		int p = position;
		if ((buffer[p++] & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((buffer[p++] & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((buffer[p++] & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((buffer[p++] & 0x80) == 0) return true;
		if (p == limit) return false;
		return true;
	}

	// string

	/** Reads the length and string of 8 bit characters. */
	public String readChars () throws KryoException {
		int charCount = readInt(true);
		switch (charCount) {
		case 0:
			return null;
		case 1:
			return "";
		}
		charCount--;
		if (chars.length < charCount) chars = new char[charCount];
		if (charCount > require(1, charCount)) return readChars_slow(charCount);
		char[] chars = this.chars;
		byte[] buffer = this.buffer;
		for (int charIndex = 0; charIndex < charCount;)
			chars[charIndex++] = (char)(buffer[position++] & 0xFF);
		return new String(chars, 0, charCount);
	}

	private String readChars_slow (int charCount) {
		char[] chars = this.chars;
		byte[] buffer = this.buffer;
		for (int charIndex = 0; charIndex < charCount;) {
			int count = require(1, Math.min(charCount - charIndex, capacity));
			for (int n = charIndex + count; charIndex < n; charIndex++)
				chars[charIndex] = (char)(buffer[position++] & 0xFF);
		}
		return new String(chars, 0, charCount);
	}

	/** Reads the length and string of UTF8 characters. */
	public String readString () throws KryoException {
		int charCount = readInt(true);
		switch (charCount) {
		case 0:
			return null;
		case 1:
			return "";
		}
		charCount--;
		if (chars.length < charCount) chars = new char[charCount];
		char[] chars = this.chars;
		byte[] buffer = this.buffer;
		// Try to read 8 bit chars.
		int charIndex = 0, b;
		int count = require(1, charCount);
		while (charIndex < count) {
			b = buffer[position++] & 0xFF;
			if (b > 127) {
				position--;
				break;
			}
			chars[charIndex++] = (char)b;
		}
		// If buffer couldn't hold all chars or any were not 8 bit, use slow path.
		if (charIndex < charCount) return readString_slow(charCount, charIndex);
		return new String(chars, 0, charCount);
	}

	private String readString_slow (int charCount, int charIndex) {
		char[] chars = this.chars;
		byte[] buffer = this.buffer;
		while (charIndex < charCount) {
			if (position == limit) require(1, charCount - charIndex);
			int b = buffer[position++] & 0xFF;
			switch (b >> 4) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				chars[charIndex] = (char)b;
				break;
			case 12:
			case 13:
				require(1, 1);
				chars[charIndex] = (char)((b & 0x1F) << 6 | buffer[position++] & 0x3F);
				break;
			case 14:
				require(2, 2);
				chars[charIndex] = (char)((b & 0x0F) << 12 | (buffer[position++] & 0x3F) << 6 | buffer[position++] & 0x3F);
				break;
			}
			charIndex++;
		}
		return new String(chars, 0, charCount);
	}

	// float

	/** Reads a 4 byte float. */
	public float readFloat () throws KryoException {
		return Float.intBitsToFloat(readInt());
	}

	/** Reads a 1-5 byte float with reduced precision. */
	public float readFloat (float precision, boolean optimizePositive) throws KryoException {
		return readInt(optimizePositive) / (float)precision;
	}

	// short

	/** Reads a 2 byte short. */
	public short readShort () throws KryoException {
		require(2, 2);
		return (short)(((buffer[position++] & 0xFF) << 8) | (buffer[position++] & 0xFF));
	}

	/** Reads a 2 byte short as an int from 0 to 65535. */
	public int readShortUnsigned () throws KryoException {
		require(2, 2);
		return ((buffer[position++] & 0xFF) << 8) | (buffer[position++] & 0xFF);
	}

	/** Reads a 1-3 byte short. */
	public short readShort (boolean optimizePositive) throws KryoException {
		int available = require(1, 3);
		byte value = buffer[position++];
		if (optimizePositive) {
			if (value != -1) return (short)(value & 0xFF);
		} else {
			if (value != -128) return value;
		}
		if (available < 3) require(2, 2);
		return (short)(((buffer[position++] & 0xFF) << 8) | (buffer[position++] & 0xFF));
	}

	// long

	/** Reads an 8 byte long. */
	public long readLong () throws KryoException {
		require(8, 8);
		byte[] buffer = this.buffer;
		return (long)buffer[position++] << 56 //
			| (long)(buffer[position++] & 0xFF) << 48 //
			| (long)(buffer[position++] & 0xFF) << 40 //
			| (long)(buffer[position++] & 0xFF) << 32 //
			| (long)(buffer[position++] & 0xFF) << 24 //
			| (buffer[position++] & 0xFF) << 16 //
			| (buffer[position++] & 0xFF) << 8 //
			| buffer[position++] & 0xFF;

	}

	/** Reads a 1-10 byte long. */
	public long readLong (boolean optimizePositive) throws KryoException {
		byte[] buffer = this.buffer;
		if (require(1, 10) < 10) return readLong_slow(optimizePositive);
		int b = buffer[position++];
		long result = b & 0x7F;
		if ((b & 0x80) != 0) {
			b = buffer[position++];
			result |= (long)(b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				b = buffer[position++];
				result |= (long)(b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					b = buffer[position++];
					result |= (long)(b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						b = buffer[position++];
						result |= (long)(b & 0x7F) << 28;
						if ((b & 0x80) != 0) {
							b = buffer[position++];
							result |= (long)(b & 0x7F) << 35;
							if ((b & 0x80) != 0) {
								b = buffer[position++];
								result |= (long)(b & 0x7F) << 42;
								if ((b & 0x80) != 0) {
									b = buffer[position++];
									result |= (long)(b & 0x7F) << 49;
									if ((b & 0x80) != 0) {
										b = buffer[position++];
										result |= (long)(b & 0x7F) << 56;
										if ((b & 0x80) != 0) {
											b = buffer[position++];
											result |= (long)(b & 0x7F) << 63;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		if (!optimizePositive) result = (result >>> 1) ^ -(result & 1);
		return result;
	}

	private long readLong_slow (boolean optimizePositive) {
		// The buffer is guaranteed to have at least 1 byte.
		int b = buffer[position++];
		long result = b & 0x7F;
		if ((b & 0x80) != 0) {
			require(1, 9);
			b = buffer[position++];
			result |= (long)(b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				require(1, 8);
				b = buffer[position++];
				result |= (long)(b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					require(1, 7);
					b = buffer[position++];
					result |= (long)(b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						require(1, 6);
						b = buffer[position++];
						result |= (long)(b & 0x7F) << 28;
						if ((b & 0x80) != 0) {
							require(1, 5);
							b = buffer[position++];
							result |= (long)(b & 0x7F) << 35;
							if ((b & 0x80) != 0) {
								require(1, 4);
								b = buffer[position++];
								result |= (long)(b & 0x7F) << 42;
								if ((b & 0x80) != 0) {
									require(1, 3);
									b = buffer[position++];
									result |= (long)(b & 0x7F) << 49;
									if ((b & 0x80) != 0) {
										require(1, 2);
										b = buffer[position++];
										result |= (long)(b & 0x7F) << 56;
										if ((b & 0x80) != 0) {
											require(1, 1);
											b = buffer[position++];
											result |= (long)(b & 0x7F) << 63;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		if (!optimizePositive) result = (result >>> 1) ^ -(result & 1);
		return result;
	}

	// boolean

	/** Reads a 1 byte boolean. */
	public boolean readBoolean () throws KryoException {
		require(1, 1);
		return buffer[position++] == 1;
	}

	// char

	/** Reads a 2 byte char. */
	public char readChar () throws KryoException {
		require(2, 2);
		return (char)(((buffer[position++] & 0xFF) << 8) | (buffer[position++] & 0xFF));
	}

	// double

	/** Reads an 8 bytes double. */
	public double readDouble () throws KryoException {
		return Double.longBitsToDouble(readLong());
	}

	/** Reads a 1-10 byte double with reduced precision. */
	public double readDouble (double precision, boolean optimizePositive) throws KryoException {
		return readLong(optimizePositive) / (double)precision;
	}
}
