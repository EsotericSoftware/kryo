
package com.esotericsoftware.kryo.io;

import java.io.IOException;
import java.io.InputStream;

import com.esotericsoftware.kryo.KryoException;

/** An InputStream that reads data from a byte array and optionally fills the byte array from another OutputStream as needed.
 * Utility methods are provided for efficiently reading primitive types and strings.
 * @author Nathan Sweet <misc@n4te.com> */
public class Input extends InputStream {
	private byte[] buffer;
	private int capacity, position, limit, total;
	private char[] chars = new char[32];
	private InputStream inputStream;

	/** Creates an uninitialized Input. {@link #setBuffer(byte[])} must be called before the Input is used. */
	public Input () {
	}

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

	/** Returns the limit for the buffer. */
	public int limit () {
		return limit;
	}

	/** Sets the limit in the buffer. */
	public void setLimit (int limit) {
		this.limit = limit;
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
			require(skipCount);
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
	 * @return the number of bytes remaining.
	 * @throws KryoException if EOS is reached before required bytes are read (buffer underflow). */
	private int require (int required) throws KryoException {
		int remaining = limit - position;
		if (remaining >= required) return remaining;
		if (required > capacity) throw new KryoException("Buffer too small: capacity: " + capacity + ", required: " + required);

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
		return remaining;
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
		require(1);
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
		require(1);
		return buffer[position++];
	}

	/** Reads a byte as an int from 0 to 255. */
	public int readByteUnsigned () throws KryoException {
		require(1);
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
			require(copyCount);
		}
	}

	// int

	/** Reads a 4 byte int. */
	public int readInt () throws KryoException {
		require(4);
		byte[] buffer = this.buffer;
		int position = this.position;
		this.position = position + 4;
		return (buffer[position] & 0xFF) << 24 //
			| (buffer[position + 1] & 0xFF) << 16 //
			| (buffer[position + 2] & 0xFF) << 8 //
			| buffer[position + 3] & 0xFF;
	}

	/** Reads a 1-5 byte int. */
	public int readInt (boolean optimizePositive) throws KryoException {
		if (require(1) < 5) return readInt_slow(optimizePositive);
		byte[] buffer = this.buffer;
		int position = this.position;
		int b = buffer[position++];
		int result;
		if ((b & 0x80) == 0)
			result = b & 0x7F;
		else {
			result = b & 0x1F | (buffer[position++] & 0xFF) << 5; // mask 1st 5 bits, combine with 2nd byte
			switch (b >>> 5 & 3) { // shift to bits 6,7, mask 1st 2 bits
			case 1:
				result |= (buffer[position++] & 0xFF) << 13;
				break;
			case 2:
				result |= (buffer[position++] & 0xFF) << 13;
				result |= (buffer[position++] & 0xFF) << 21;
				break;
			case 3:
				result |= (buffer[position++] & 0xFF) << 13;
				result |= (buffer[position++] & 0xFF) << 21;
				result |= (buffer[position++] & 0xFF) << 29;
				break;
			}
		}
		this.position = position;
		return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
	}

	private int readInt_slow (boolean optimizePositive) {
		// The buffer is guaranteed to have at least 1 byte.
		int b = buffer[position++];
		int result;
		if ((b & 0x80) == 0)
			result = b & 0x7F;
		else {
			int additional = b >>> 5 & 3; // shift to bits 6,7, mask 1st 2 bits
			require(additional + 1);
			byte[] buffer = this.buffer;
			int position = this.position;
			result = b & 0x1F | (buffer[position++] & 0xFF) << 5; // mask 1st 5 bits, combine with 2nd byte
			switch (additional) {
			case 1:
				result |= (buffer[position++] & 0xFF) << 13;
				break;
			case 2:
				result |= (buffer[position++] & 0xFF) << 13;
				result |= (buffer[position++] & 0xFF) << 21;
				break;
			case 3:
				result |= (buffer[position++] & 0xFF) << 13;
				result |= (buffer[position++] & 0xFF) << 21;
				result |= (buffer[position++] & 0xFF) << 29;
				break;
			}
			this.position = position;
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

	/** Reads the length and string of UTF8 characters, or null.
	 * @return May be null. */
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
		int b, charIndex = 0;
		int count = Math.min(require(1), charCount);
		int position = this.position;
		while (charIndex < count) {
			b = buffer[position++];
			if (b < 0) {
				position--;
				break;
			}
			chars[charIndex++] = (char)b;
		}
		this.position = position;
		// If buffer couldn't hold all chars or any were not 8 bit, use slow path for remainder.
		if (charIndex < charCount) return readString_slow(charCount, charIndex);
		return new String(chars, 0, charCount);
	}

	private String readString_slow (int charCount, int charIndex) {
		char[] chars = this.chars;
		byte[] buffer = this.buffer;
		while (charIndex < charCount) {
			if (position == limit) require(1);
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
				require(1);
				chars[charIndex] = (char)((b & 0x1F) << 6 | buffer[position++] & 0x3F);
				break;
			case 14:
				require(2);
				chars[charIndex] = (char)((b & 0x0F) << 12 | (buffer[position++] & 0x3F) << 6 | buffer[position++] & 0x3F);
				break;
			}
			charIndex++;
		}
		return new String(chars, 0, charCount);
	}

	public String readString_new () {
		int b = buffer[position++];
		if ((b & (1 << 7)) == 0) {
			// ascii
			byte[] buffer = this.buffer;
			int end = position;
			int start = end - 1;
			int limit = this.limit;
			do {
				if (end == limit) return readAscii_slow();
				b = buffer[end++];
			} while ((b & 0x80) == 0);
			buffer[start] &= 0x7F; // Mask ascii/utf8 bit.
			buffer[end - 1] &= 0x7F; // Mask end of ascii bit.
			String value = new String(buffer, 0, start, end - start);
			buffer[start] |= 0x80;
			buffer[end - 1] |= 0x80;
			position = end;
			return value;
		}
		// utf8
		int charCount = readStringLength(b);
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
		int charIndex = 0;
		int count = Math.min(require(1), charCount);
		int position = this.position;
		while (charIndex < count) {
			b = buffer[position++];
			if (b < 0) {
				position--;
				break;
			}
			chars[charIndex++] = (char)b;
		}
		this.position = position;
		// If buffer couldn't hold all chars or any were not 8 bit, use slow path for remainder.
		if (charIndex < charCount) return readString_slow(charCount, charIndex);
		return new String(chars, 0, charCount);
	}

	private int readStringLength (int b) {
		int result = b & 0x3F; // only take first 6 bits
		if ((b & 0x40) != 0) { // if 6th bit
			b = buffer[position++];
			result |= (b & 0x7F) << 6;
			if ((b & 0x80) != 0) {
				b = buffer[position++];
				result |= (b & 0x7F) << 13;
				if ((b & 0x80) != 0) {
					b = buffer[position++];
					result |= (b & 0x7F) << 20;
					if ((b & 0x80) != 0) {
						b = buffer[position++];
						result |= (b & 0x7F) << 27;
					}
				}
			}
		}
		return result;
	}

	/** Reads a string of ASCII characters.
	 * @return May be null.
	 * @see Output#writeAscii(String) */
	public String readAscii () throws KryoException {
		int b = readByte();
		switch (b) {
		case 0:
			return null;
		case 1:
			return "";
		case 2:
			b = readByte();
		}
		byte[] buffer = this.buffer;
		int end = position;
		int start = end - 1;
		int limit = this.limit;
		while ((b & 0x80) == 0) {
			if (end == limit) return readAscii_slow();
			b = buffer[end++];
		}
		buffer[end - 1] = (byte)(b & 0x7F);
		String value = new String(buffer, 0, start, end - start);
		buffer[end - 1] = (byte)b;
		position = end;
		return value;
	}

	private String readAscii_slow () {
		position--; // Reread the first char.
		int charCount = 0;
		char[] chars = this.chars;
		byte[] buffer = this.buffer;
		while (true) {
			require(1);
			int b = buffer[position++];
			if (charCount == chars.length) {
				char[] newChars = new char[charCount * 2];
				System.arraycopy(chars, 0, newChars, 0, charCount);
				chars = newChars;
				this.chars = newChars;
			}
			chars[charCount++] = (char)(b & 0x7F);
			if ((b & 0x80) == 0x80) break;
		}
		System.out.println(new String(chars, 0, charCount));
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
		require(2);
		return (short)(((buffer[position++] & 0xFF) << 8) | (buffer[position++] & 0xFF));
	}

	/** Reads a 2 byte short as an int from 0 to 65535. */
	public int readShortUnsigned () throws KryoException {
		require(2);
		return ((buffer[position++] & 0xFF) << 8) | (buffer[position++] & 0xFF);
	}

	/** Reads a 1-3 byte short. */
	public short readShort (boolean optimizePositive) throws KryoException {
		int available = require(1);
		byte value = buffer[position++];
		if (optimizePositive) {
			if (value != -1) return (short)(value & 0xFF);
		} else {
			if (value != -128) return value;
		}
		if (available < 3) require(2);
		return (short)(((buffer[position++] & 0xFF) << 8) | (buffer[position++] & 0xFF));
	}

	// long

	/** Reads an 8 byte long. */
	public long readLong () throws KryoException {
		require(8);
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

	/** Reads a 1-9 byte long. */
	public long readLong (boolean optimizePositive) throws KryoException {
		byte[] buffer = this.buffer;
		if (require(1) < 9) return readLong_slow(optimizePositive);
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
										result |= (long)b << 56;
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
			require(1);
			b = buffer[position++];
			result |= (long)(b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				require(1);
				b = buffer[position++];
				result |= (long)(b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					require(1);
					b = buffer[position++];
					result |= (long)(b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						require(1);
						b = buffer[position++];
						result |= (long)(b & 0x7F) << 28;
						if ((b & 0x80) != 0) {
							require(1);
							b = buffer[position++];
							result |= (long)(b & 0x7F) << 35;
							if ((b & 0x80) != 0) {
								require(1);
								b = buffer[position++];
								result |= (long)(b & 0x7F) << 42;
								if ((b & 0x80) != 0) {
									require(1);
									b = buffer[position++];
									result |= (long)(b & 0x7F) << 49;
									if ((b & 0x80) != 0) {
										require(1);
										b = buffer[position++];
										result |= (long)b << 56;
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
		require(1);
		return buffer[position++] == 1;
	}

	// char

	/** Reads a 2 byte char. */
	public char readChar () throws KryoException {
		require(2);
		return (char)(((buffer[position++] & 0xFF) << 8) | (buffer[position++] & 0xFF));
	}

	// double

	/** Reads an 8 bytes double. */
	public double readDouble () throws KryoException {
		return Double.longBitsToDouble(readLong());
	}

	/** Reads a 1-9 byte double with reduced precision. */
	public double readDouble (double precision, boolean optimizePositive) throws KryoException {
		return readLong(optimizePositive) / (double)precision;
	}
}
