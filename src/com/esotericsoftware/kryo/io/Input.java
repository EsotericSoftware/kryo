
package com.esotericsoftware.kryo.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.KryoException;

public class Input extends InputStream {
	private byte[] buffer;
	private int capacity, position, limit, total;
	private char[] chars = new char[0];
	private InputStream inputStream;

	public Input (int bufferSize) {
		this.capacity = bufferSize;
		buffer = new byte[bufferSize];
	}

	public Input (byte[] bytes) {
		setBytes(bytes, 0, bytes.length);
	}

	public Input (byte[] bytes, int offset, int count) {
		setBytes(bytes, offset, count);
	}

	public Input (InputStream inputStream) {
		this(4096);
		if (inputStream == null) throw new IllegalArgumentException("inputStream cannot be null.");
		this.inputStream = inputStream;
	}

	public Input (InputStream inputStream, int bufferSize) {
		this(bufferSize);
		if (inputStream == null) throw new IllegalArgumentException("inputStream cannot be null.");
		this.inputStream = inputStream;
	}

	public void setBytes (byte[] bytes) {
		setBytes(bytes, 0, bytes.length);
	}

	public void setBytes (byte[] bytes, int offset, int count) {
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

	public void setInputStream (InputStream inputStream) {
		if (inputStream == null) throw new IllegalArgumentException("inputStream cannot be null.");
		this.inputStream = inputStream;
		limit = 0;
		rewind();
	}

	public int total () {
		return total + position;
	}

	public void setPosition (int position) {
		this.position = position;
	}

	public void rewind () {
		position = 0;
		total = 0;
	}

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

	protected int fill (byte[] buffer, int offset, int count) throws KryoException {
		if (inputStream == null) return -1;
		try {
			return inputStream.read(buffer, offset, count);
		} catch (IOException ex) {
			throw new KryoException(ex);
		}
	}

	/** @param required Must have at least this many bytes in buffer.
	 * @param optional Try to fill at least this many bytes in buffer. */
	private void require (int required, int optional) throws KryoException {
		int remaining = limit - position;
		if (remaining >= optional) return;
		remaining = Math.max(0, remaining);
		System.arraycopy(buffer, position, buffer, 0, remaining);
		while (true) {
			int count = fill(buffer, remaining, capacity - remaining);
			if (count == -1) {
				if (remaining >= required) break;
				throw new KryoException("Buffer underflow.");
			}
			remaining += count;
			if (remaining >= optional) break;
		}
		limit = remaining;
		total += position;
		position = 0;
	}

	// InputStream

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
			copyCount = Math.min(count, capacity);
			require(0, copyCount);
			if (position == limit) break;
		}
		return startingCount - count;
	}

	public long skip (long count) throws KryoException {
		long remaining = count;
		while (remaining > 0) {
			int skip = Math.max(Integer.MAX_VALUE, (int)remaining);
			skip(skip);
			remaining -= skip;
		}
		return count;
	}

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

	public int readInt () throws KryoException {
		require(4, 4);
		byte[] buffer = this.buffer;
		return (buffer[position++] & 0xFF) << 24 //
			| (buffer[position++] & 0xFF) << 16 //
			| (buffer[position++] & 0xFF) << 8 //
			| buffer[position++] & 0xFF;
	}

	public int readInt (boolean optimizePositive) throws KryoException {
		require(1, 5);
		int b = buffer[position++];
		int result = b & 0x7F;
		if ((b & 0x80) != 0) {
			try {
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
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new KryoException("Buffer underflow.");
			}
		}
		return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
	}

	public boolean canReadInt () throws KryoException {
		if (limit - position >= 5) return true;
		require(0, 4);
		int p = position;
		if (p == limit) return false;
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

	public String readChars () throws KryoException {
		int charCount = readInt(true);
		if (chars.length < charCount) chars = new char[charCount];
		if (charCount > capacity) return readChars_slow(charCount);
		char[] chars = this.chars;
		byte[] buffer = this.buffer;
		require(charCount, charCount);
		int charIndex = 0;
		while (charIndex < charCount)
			chars[charIndex++] = (char)(buffer[position++] & 0xFF);
		return new String(chars, 0, charCount);
	}

	private String readChars_slow (int charCount) throws KryoException {
		char[] chars = this.chars;
		int charsToRead = limit - position;
		int charIndex = 0;
		while (charIndex < charCount) {
			for (int n = charIndex + charsToRead; charIndex < n; charIndex++)
				chars[charIndex++] = (char)(buffer[position++] & 0xFF);
			charsToRead = Math.min(charCount - charIndex, capacity);
			require(charsToRead, charsToRead);
		}
		return new String(chars, 0, charCount);
	}

	public String readString () throws KryoException {
		int charCount = readInt(true);
		if (chars.length < charCount) chars = new char[charCount];
		char[] chars = this.chars;
		byte[] buffer = this.buffer;
		require(charCount, charCount);
		int c = 0, charIndex = 0;
		while (charIndex < charCount) {
			c = buffer[position++] & 0xFF;
			if (c > 127) break;
			chars[charIndex++] = (char)c;
		}
		if (charIndex < charCount) return readString_slow(charCount, charIndex, c);
		return new String(chars, 0, charCount);
	}

	private String readString_slow (int charCount, int charIndex, int c) throws KryoException {
		char[] chars = this.chars;
		byte[] buffer = this.buffer;
		int charsPerBuffer = capacity / 3;
		int charsToRead = Math.min(charCount - charIndex, (limit - position) / 3);
		while (true) {
			int endIndex = charIndex + charsToRead;
			while (true) {
				switch (c >> 4) {
				case 0:
				case 1:
				case 2:
				case 3:
				case 4:
				case 5:
				case 6:
				case 7:
					chars[charIndex] = (char)c;
					break;
				case 12:
				case 13:
					chars[charIndex] = (char)((c & 0x1F) << 6 | buffer[position++] & 0x3F);
					break;
				case 14:
					chars[charIndex] = (char)((c & 0x0F) << 12 | (buffer[position++] & 0x3F) << 6 | buffer[position++] & 0x3F);
					break;
				}
				if (++charIndex == endIndex) break;
				c = buffer[position++] & 0xFF;
			}
			if (charIndex == charCount) break;
			charsToRead = Math.min(charCount - charIndex, capacity);
			require(charsToRead, charsToRead);
			c = buffer[position++] & 0xFF;
		}
		return new String(chars, 0, charCount);
	}

	// float

	public float readFloat () throws KryoException {
		return Float.intBitsToFloat(readInt());
	}

	public float readFloat (float precision, boolean optimizePositive) throws KryoException {
		return readInt(optimizePositive) / (float)precision;
	}

	// short

	public short readShort () throws KryoException {
		require(2, 2);
		return (short)(((buffer[position++] & 0xFF) << 8) | (buffer[position++] & 0xFF));
	}

	public int readShortUnsigned () throws KryoException {
		return ((buffer[position++] & 0xFF) << 8) | (buffer[position++] & 0xFF);
	}

	public short readShort (boolean optimizePositive) throws KryoException {
		require(1, 3);
		byte value = buffer[position++];
		try {
			if (optimizePositive) {
				if (value == -1) return (short)(((buffer[position++] & 0xFF) << 8) | (buffer[position++] & 0xFF)); // short positive
				return (short)(value & 0xFF);
			}
			if (value == -128) return (short)(((buffer[position++] & 0xFF) << 8) | (buffer[position++] & 0xFF)); // short
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new KryoException("Buffer underflow.");
		}
		return value;
	}

	// long

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

	public long readLong (boolean optimizePositive) throws KryoException {
		byte[] buffer = this.buffer;
		require(1, 10);
		int b = buffer[position++];
		long result = b & 0x7F;
		if ((b & 0x80) != 0) {
			try {
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
			} catch (ArrayIndexOutOfBoundsException ex) {
				throw new KryoException("Buffer underflow.");
			}
		}
		if (!optimizePositive) result = (result >>> 1) ^ -(result & 1);
		return result;
	}

	// boolean

	public boolean readBoolean () throws KryoException {
		require(1, 1);
		return buffer[position++] == 1;
	}

	// char

	public char readChar () throws KryoException {
		require(2, 2);
		return (char)(((buffer[position++] & 0xFF) << 8) | (buffer[position++] & 0xFF));
	}

	// double

	public double readDouble () throws KryoException {
		return Double.longBitsToDouble(readLong());
	}

	public double readDouble (double precision, boolean optimizePositive) throws KryoException {
		return readLong(optimizePositive) / (double)precision;
	}
}
