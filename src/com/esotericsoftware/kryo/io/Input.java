
package com.esotericsoftware.kryo.io;

import com.esotericsoftware.kryo.KryoException;

import java.io.IOException;
import java.io.InputStream;

/** An InputStream that reads data from a byte array and optionally fills the byte array from another InputStream as needed.
 * Utility methods are provided for efficiently reading primitive types and strings.
 * <p>
 * The byte[] buffer may be modified and then returned to its original state during some read operations, so the same byte[]
 * should not be used concurrently in separate threads.
 * @author Nathan Sweet <misc@n4te.com> */
public class Input extends InputStream {
	protected byte[] buffer;
	protected int position;
	protected int capacity;
	protected int limit;
	protected long total;
	protected char[] chars = new char[32];
	protected InputStream inputStream;

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
		limit = offset + count;
		capacity = bytes.length;
		total = 0;
		inputStream = null;
	}

	public byte[] getBuffer () {
		return buffer;
	}

	public InputStream getInputStream () {
		return inputStream;
	}

	/** Sets a new InputStream. The position and total are reset, discarding any buffered bytes.
	 * @param inputStream May be null. */
	public void setInputStream (InputStream inputStream) {
		this.inputStream = inputStream;
		limit = 0;
		rewind();
	}

	/** Returns the number of bytes read. */
	public long total () {
		return total + position;
	}

	/** Sets the number of bytes read. */
	public void setTotal (int total) {
		this.total = total;
	}

	/** Returns the current position in the buffer. */
	final public int position () {
		return position;
	}

	/** Sets the current position in the buffer. */
	public void setPosition (int position) {
		this.position = position;
	}

	/** Returns the limit for the buffer. */
	final public int limit () {
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

	/** Fills the buffer with more bytes. Can be overridden to fill the bytes from a source other than the InputStream.
	 * @return -1 if there are no more bytes. */
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
	protected int require (int required) throws KryoException {
		int remaining = limit - position;
		if (remaining >= required) return remaining;
		if (required > capacity) throw new KryoException("Buffer too small: capacity: " + capacity + ", required: " + required);

		int count;
		// Try to fill the buffer.
		if (remaining > 0) {
			count = fill(buffer, limit, capacity - limit);
			if (count == -1) throw new KryoException("Buffer underflow.");
			remaining += count;
			if (remaining >= required) {
				limit += count;
				return remaining;
			}
		}

		// Was not enough, compact and try again.
		System.arraycopy(buffer, position, buffer, 0, remaining);
		total += position;
		position = 0;

		while (true) {
			count = fill(buffer, remaining, capacity - remaining);
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

		int count;

		// Try to fill the buffer.
		count = fill(buffer, limit, capacity - limit);
		if (count == -1) return remaining == 0 ? -1 : Math.min(remaining, optional);
		remaining += count;
		if (remaining >= optional) {
			limit += count;
			return optional;
		}

		// Was not enough, compact and try again.
		System.arraycopy(buffer, position, buffer, 0, remaining);
		total += position;
		position = 0;

		while (true) {
			count = fill(buffer, remaining, capacity - remaining);
			if (count == -1) break;
			remaining += count;
			if (remaining >= optional) break; // Enough has been read.
		}

		limit = remaining;
		return remaining == 0 ? -1 : Math.min(remaining, optional);
	}

	public boolean eof () {
		return optional(1) <= 0;
	}

	// InputStream

	public int available () throws IOException {
		return limit - position + ((null != inputStream) ? inputStream.available() : 0);
	}

	/** Reads a single byte as an int from 0 to 255, or -1 if there are no more bytes are available. */
	public int read () throws KryoException {
		if (optional(1) <= 0) return -1;
		return buffer[position++] & 0xFF;
	}

	/** Reads bytes.length bytes or less and writes them to the specified byte[], starting at 0, and returns the number of bytes
	 * read. */
	public int read (byte[] bytes) throws KryoException {
		return read(bytes, 0, bytes.length);
	}

	/** Reads count bytes or less and writes them to the specified byte[], starting at offset, and returns the number of bytes read
	 * or -1 if no more bytes are available. */
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
			int skip = (int)Math.min(Integer.MAX_VALUE, remaining);
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

	/** Reads a single byte. */
	public byte readByte () throws KryoException {
		require(1);
		return buffer[position++];
	}

	/** Reads a byte as an int from 0 to 255. */
	public int readByteUnsigned () throws KryoException {
		require(1);
		return buffer[position++] & 0xFF;
	}

	/** Reads the specified number of bytes into a new byte[]. */
	public byte[] readBytes (int length) throws KryoException {
		byte[] bytes = new byte[length];
		readBytes(bytes, 0, length);
		return bytes;
	}

	/** Reads bytes.length bytes and writes them to the specified byte[], starting at index 0. */
	public void readBytes (byte[] bytes) throws KryoException {
		readBytes(bytes, 0, bytes.length);
	}

	/** Reads count bytes and writes them to the specified byte[], starting at offset. */
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

	/** Reads a 1-5 byte int. This stream may consider such a variable length encoding request as a hint. It is not guaranteed that
	 * a variable length encoding will be really used. The stream may decide to use native-sized integer representation for
	 * efficiency reasons. **/
	public int readInt (boolean optimizePositive) throws KryoException {
		return readVarInt(optimizePositive);
	}

	/** Reads a 1-5 byte int. It is guaranteed that a varible length encoding will be used. */
	public int readVarInt (boolean optimizePositive) throws KryoException {
		if (require(1) < 5) return readInt_slow(optimizePositive);
		int b = buffer[position++];
		int result = b & 0x7F;
		if ((b & 0x80) != 0) {
			byte[] buffer = this.buffer;
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
			require(1);
			byte[] buffer = this.buffer;
			b = buffer[position++];
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				require(1);
				b = buffer[position++];
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					require(1);
					b = buffer[position++];
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						require(1);
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

	/** Returns true if enough bytes are available to read a long with {@link #readLong(boolean)}. */
	public boolean canReadLong () throws KryoException {
		if (limit - position >= 9) return true;
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

	/** Reads the length and string of UTF8 characters, or null. This can read strings written by {@link Output#writeString(String)}
	 * , {@link Output#writeString(CharSequence)}, and {@link Output#writeAscii(String)}.
	 * @return May be null. */
	public String readString () {
		int available = require(1);
		int b = buffer[position++];
		if ((b & 0x80) == 0) return readAscii(); // ASCII.
		// Null, empty, or UTF8.
		int charCount = available >= 5 ? readUtf8Length(b) : readUtf8Length_slow(b);
		switch (charCount) {
		case 0:
			return null;
		case 1:
			return "";
		}
		charCount--;
		if (chars.length < charCount) chars = new char[charCount];
		readUtf8(charCount);
		return new String(chars, 0, charCount);
	}

	private int readUtf8Length (int b) {
		int result = b & 0x3F; // Mask all but first 6 bits.
		if ((b & 0x40) != 0) { // Bit 7 means another byte, bit 8 means UTF8.
			byte[] buffer = this.buffer;
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

	private int readUtf8Length_slow (int b) {
		int result = b & 0x3F; // Mask all but first 6 bits.
		if ((b & 0x40) != 0) { // Bit 7 means another byte, bit 8 means UTF8.
			require(1);
			byte[] buffer = this.buffer;
			b = buffer[position++];
			result |= (b & 0x7F) << 6;
			if ((b & 0x80) != 0) {
				require(1);
				b = buffer[position++];
				result |= (b & 0x7F) << 13;
				if ((b & 0x80) != 0) {
					require(1);
					b = buffer[position++];
					result |= (b & 0x7F) << 20;
					if ((b & 0x80) != 0) {
						require(1);
						b = buffer[position++];
						result |= (b & 0x7F) << 27;
					}
				}
			}
		}
		return result;
	}

	private void readUtf8 (int charCount) {
		byte[] buffer = this.buffer;
		char[] chars = this.chars;
		// Try to read 7 bit ASCII chars.
		int charIndex = 0;
		int count = Math.min(require(1), charCount);
		int position = this.position;
		int b;
		while (charIndex < count) {
			b = buffer[position++];
			if (b < 0) {
				position--;
				break;
			}
			chars[charIndex++] = (char)b;
		}
		this.position = position;
		// If buffer didn't hold all chars or any were not ASCII, use slow path for remainder.
		if (charIndex < charCount) readUtf8_slow(charCount, charIndex);
	}

	private void readUtf8_slow (int charCount, int charIndex) {
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
				if (position == limit) require(1);
				chars[charIndex] = (char)((b & 0x1F) << 6 | buffer[position++] & 0x3F);
				break;
			case 14:
				require(2);
				chars[charIndex] = (char)((b & 0x0F) << 12 | (buffer[position++] & 0x3F) << 6 | buffer[position++] & 0x3F);
				break;
			}
			charIndex++;
		}
	}

	private String readAscii () {
		byte[] buffer = this.buffer;
		int end = position;
		int start = end - 1;
		int limit = this.limit;
		int b;
		do {
			if (end == limit) return readAscii_slow();
			b = buffer[end++];
		} while ((b & 0x80) == 0);
		buffer[end - 1] &= 0x7F; // Mask end of ascii bit.
		String value = new String(buffer, 0, start, end - start);
		buffer[end - 1] |= 0x80;
		position = end;
		return value;
	}

	private String readAscii_slow () {
		position--; // Re-read the first byte.
		// Copy chars currently in buffer.
		int charCount = limit - position;
		if (charCount > chars.length) chars = new char[charCount * 2];
		char[] chars = this.chars;
		byte[] buffer = this.buffer;
		for (int i = position, ii = 0, n = limit; i < n; i++, ii++)
			chars[ii] = (char)buffer[i];
		position = limit;
		// Copy additional chars one by one.
		while (true) {
			require(1);
			int b = buffer[position++];
			if (charCount == chars.length) {
				char[] newChars = new char[charCount * 2];
				System.arraycopy(chars, 0, newChars, 0, charCount);
				chars = newChars;
				this.chars = newChars;
			}
			if ((b & 0x80) == 0x80) {
				chars[charCount++] = (char)(b & 0x7F);
				break;
			}
			chars[charCount++] = (char)b;
		}
		return new String(chars, 0, charCount);
	}

	/** Reads the length and string of UTF8 characters, or null. This can read strings written by {@link Output#writeString(String)}
	 * , {@link Output#writeString(CharSequence)}, and {@link Output#writeAscii(String)}.
	 * @return May be null. */
	public StringBuilder readStringBuilder () {
		int available = require(1);
		int b = buffer[position++];
		if ((b & 0x80) == 0) return new StringBuilder(readAscii()); // ASCII.
		// Null, empty, or UTF8.
		int charCount = available >= 5 ? readUtf8Length(b) : readUtf8Length_slow(b);
		switch (charCount) {
		case 0:
			return null;
		case 1:
			return new StringBuilder("");
		}
		charCount--;
		if (chars.length < charCount) chars = new char[charCount];
		readUtf8(charCount);
		StringBuilder builder = new StringBuilder(charCount);
		builder.append(chars, 0, charCount);
		return builder;
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

	/** Reads a 1-9 byte long. This stream may consider such a variable length encoding request as a hint. It is not guaranteed that
	 * a variable length encoding will be really used. The stream may decide to use native-sized integer representation for
	 * efficiency reasons. */
	public long readLong (boolean optimizePositive) throws KryoException {
		return readVarLong(optimizePositive);
	}

	/** Reads a 1-9 byte long. It is guaranteed that a varible length encoding will be used. */
	public long readVarLong (boolean optimizePositive) throws KryoException {
		if (require(1) < 9) return readLong_slow(optimizePositive);
		int b = buffer[position++];
		long result = b & 0x7F;
		if ((b & 0x80) != 0) {
			byte[] buffer = this.buffer;
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
			byte[] buffer = this.buffer;
			b = buffer[position++];
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				require(1);
				b = buffer[position++];
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					require(1);
					b = buffer[position++];
					result |= (b & 0x7F) << 21;
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

	// Methods implementing bulk operations on arrays of primitive types

	/** Bulk input of an int array. */
	public int[] readInts (int length, boolean optimizePositive) throws KryoException {
		int[] array = new int[length];
		for (int i = 0; i < length; i++)
			array[i] = readInt(optimizePositive);
		return array;
	}

	/** Bulk input of a long array. */
	public long[] readLongs (int length, boolean optimizePositive) throws KryoException {
		long[] array = new long[length];
		for (int i = 0; i < length; i++)
			array[i] = readLong(optimizePositive);
		return array;
	}

	/** Bulk input of an int array. */
	public int[] readInts (int length) throws KryoException {
		int[] array = new int[length];
		for (int i = 0; i < length; i++)
			array[i] = readInt();
		return array;
	}

	/** Bulk input of a long array. */
	public long[] readLongs (int length) throws KryoException {
		long[] array = new long[length];
		for (int i = 0; i < length; i++)
			array[i] = readLong();
		return array;
	}

	/** Bulk input of a float array. */
	public float[] readFloats (int length) throws KryoException {
		float[] array = new float[length];
		for (int i = 0; i < length; i++)
			array[i] = readFloat();
		return array;
	}

	/** Bulk input of a short array. */
	public short[] readShorts (int length) throws KryoException {
		short[] array = new short[length];
		for (int i = 0; i < length; i++)
			array[i] = readShort();
		return array;
	}

	/** Bulk input of a char array. */
	public char[] readChars (int length) throws KryoException {
		char[] array = new char[length];
		for (int i = 0; i < length; i++)
			array[i] = readChar();
		return array;
	}

	/** Bulk input of a double array. */
	public double[] readDoubles (int length) throws KryoException {
		double[] array = new double[length];
		for (int i = 0; i < length; i++)
			array[i] = readDouble();
		return array;
	}
}
