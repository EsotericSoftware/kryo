
package com.esotericsoftware.kryo.io;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.util.UnsafeUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/** An InputStream that reads data from a byte array and optionally fills the byte array from another InputStream as needed.
 * Utility methods are provided for efficiently reading primitive types and strings.
 * 
 * @author Roman Levenstein <romixlev@gmail.com> */
public class ByteBufferInput extends Input {
	protected ByteBuffer niobuffer;

	protected boolean varIntsEnabled = true;

	/* Default byte order is BIG_ENDIAN to be compatible to the base class */
	ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

	protected final static ByteOrder nativeOrder = ByteOrder.nativeOrder();

	/** Creates an uninitialized Input. A buffer must be set before the Input is used.
	 * @see #setBuffer(ByteBuffer) */
	public ByteBufferInput () {
	}

	/** Creates a new Input for reading from a byte array.
	 * @param bufferSize The size of the buffer. An exception is thrown if more bytes than this are read. */
	public ByteBufferInput (int bufferSize) {
		this.capacity = bufferSize;
		niobuffer = ByteBuffer.allocateDirect(bufferSize);
		niobuffer.order(byteOrder);
	}

	public ByteBufferInput (byte[] buffer) {
		setBuffer(buffer);
	}

	/** Creates a new Input for reading from a ByteBuffer. */
	public ByteBufferInput (ByteBuffer buffer) {
		setBuffer(buffer);
	}

	/** Creates a new Input for reading from an InputStream with a buffer size of 4096. */
	public ByteBufferInput (InputStream inputStream) {
		this(4096);
		if (inputStream == null) throw new IllegalArgumentException("inputStream cannot be null.");
		this.inputStream = inputStream;
	}

	/** Creates a new Input for reading from an InputStream. */
	public ByteBufferInput (InputStream inputStream, int bufferSize) {
		this(bufferSize);
		if (inputStream == null) throw new IllegalArgumentException("inputStream cannot be null.");
		this.inputStream = inputStream;
	}

	public ByteOrder order () {
		return byteOrder;
	}

	public void order (ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
	}

	/** Sets a new buffer, discarding any previous buffer. The position and total are reset. */
	public void setBuffer (byte[] bytes) {
		ByteBuffer directBuffer = ByteBuffer.allocateDirect(bytes.length);
		directBuffer.put(bytes);
		directBuffer.position(0);
		directBuffer.limit(bytes.length);
		directBuffer.order(byteOrder);
		setBuffer(directBuffer);
	}

	/** Sets a new buffer, discarding any previous buffer. The byte order, position, limit and capacity are set to match the
	 * specified buffer. The total is reset. The {@link #setInputStream(InputStream) InputStream} is set to null. */
	public void setBuffer (ByteBuffer buffer) {
		if (buffer == null) throw new IllegalArgumentException("buffer cannot be null.");
		niobuffer = buffer;
		position = buffer.position();
		limit = buffer.limit();
		capacity = buffer.capacity();
		total = 0;
		inputStream = null;
	}

	/** Releases a direct buffer. {@link #setBuffer(ByteBuffer)} must be called before any write operations can be performed. */
	public void release () {
		close();
		UnsafeUtil.releaseBuffer(niobuffer);
		niobuffer = null;
	}

	/** This constructor allows for creation of a direct ByteBuffer of a given size at a given address.
	 * 
	 * <p>
	 * Typical usage could look like this snippet:
	 * 
	 * <pre>
	 * // Explicitly allocate memory
	 * long bufAddress = UnsafeUtil.unsafe().allocateMemory(4096);
	 * // Create a ByteBufferInput using the allocated memory region
	 * ByteBufferInput buffer = new ByteBufferInput(bufAddress, 4096);
	 * 
	 * // Do some operations on this buffer here
	 * 
	 * // Say that ByteBuffer won't be used anymore
	 * buffer.release();
	 * // Release the allocated region
	 * UnsafeUtil.unsafe().freeMemory(bufAddress);
	 * </pre>
	 * 
	 * @param address starting address of a memory region pre-allocated using Unsafe.allocateMemory() */
	public ByteBufferInput (long address, int size) {
		setBuffer(UnsafeUtil.getDirectBufferAt(address, size));
	}

	public ByteBuffer getByteBuffer () {
		return niobuffer;
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

	public void rewind () {
		super.rewind();
		niobuffer.position(0);
	}

	/** Fills the buffer with more bytes. Can be overridden to fill the bytes from a source other than the InputStream. */
	protected int fill (ByteBuffer buffer, int offset, int count) throws KryoException {
		if (inputStream == null) return -1;
		try {
			byte[] tmp = new byte[count];
			int result = inputStream.read(tmp, 0, count);
			buffer.position(offset);
			if (result >= 0) {
				buffer.put(tmp, 0, result);
				buffer.position(offset);
			}
			return result;
		} catch (IOException ex) {
			throw new KryoException(ex);
		}
	}

	/** @param required Must be > 0. The buffer is filled until it has at least this many bytes.
	 * @return the number of bytes remaining.
	 * @throws KryoException if EOS is reached before required bytes are read (buffer underflow). */
	final protected int require (int required) throws KryoException {
		int remaining = limit - position;
		if (remaining >= required) return remaining;
		if (required > capacity) throw new KryoException("Buffer too small: capacity: " + capacity + ", required: " + required);

		int count;
		// Try to fill the buffer.
		if (remaining > 0) {
			count = fill(niobuffer, limit, capacity - limit);
			if (count == -1) throw new KryoException("Buffer underflow.");
			remaining += count;
			if (remaining >= required) {
				limit += count;
				return remaining;
			}
		}

		// Compact. Position after compaction can be non-zero
		niobuffer.position(position);
		niobuffer.compact();
		total += position;
		position = 0;

		while (true) {
			count = fill(niobuffer, remaining, capacity - remaining);
			if (count == -1) {
				if (remaining >= required) break;
				throw new KryoException("Buffer underflow.");
			}
			remaining += count;
			if (remaining >= required) break; // Enough has been read.
		}
		limit = remaining;
		niobuffer.position(0);
		return remaining;
	}

	/** @param optional Try to fill the buffer with this many bytes.
	 * @return the number of bytes remaining, but not more than optional, or -1 if the EOS was reached and the buffer is empty. */
	private int optional (int optional) throws KryoException {
		int remaining = limit - position;
		if (remaining >= optional) return optional;
		optional = Math.min(optional, capacity);

		// Try to fill the buffer.
		int count = fill(niobuffer, limit, capacity - limit);
		if (count == -1) return remaining == 0 ? -1 : Math.min(remaining, optional);
		remaining += count;
		if (remaining >= optional) {
			limit += count;
			return optional;
		}

		// Compact.
		niobuffer.compact();
		total += position;
		position = 0;

		while (true) {
			count = fill(niobuffer, remaining, capacity - remaining);
			if (count == -1) break;
			remaining += count;
			if (remaining >= optional) break; // Enough has been read.
		}
		limit = remaining;
		niobuffer.position(position);
		return remaining == 0 ? -1 : Math.min(remaining, optional);
	}

	// InputStream

	/** Reads a single byte as an int from 0 to 255, or -1 if there are no more bytes are available. */
	public int read () throws KryoException {
		if (optional(1) <= 0) return -1;
		niobuffer.position(position);
		position++;
		return niobuffer.get() & 0xFF;
	}

	/** Reads bytes.length bytes or less and writes them to the specified byte[], starting at 0, and returns the number of bytes
	 * read. */
	public int read (byte[] bytes) throws KryoException {
		niobuffer.position(position);
		return read(bytes, 0, bytes.length);
	}

	/** Reads count bytes or less and writes them to the specified byte[], starting at offset, and returns the number of bytes read
	 * or -1 if no more bytes are available. */
	public int read (byte[] bytes, int offset, int count) throws KryoException {
		niobuffer.position(position);
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		int startingCount = count;
		int copyCount = Math.min(limit - position, count);
		while (true) {
			niobuffer.get(bytes, offset, copyCount);
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
		niobuffer.position(position);
		require(1);
		position++;
		return niobuffer.get();
	}

	/** Reads a byte as an int from 0 to 255. */
	public int readByteUnsigned () throws KryoException {
		// buffer.position(position);
		require(1);
		position++;
		return niobuffer.get() & 0xFF;
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
			niobuffer.get(bytes, offset, copyCount);
			position += copyCount;
			count -= copyCount;
			if (count == 0) break;
			offset += copyCount;
			copyCount = Math.min(count, capacity);
			require(copyCount);
		}
	}

	public int readInt () throws KryoException {
		require(4);
		position += 4;
		return niobuffer.getInt();
	}

	public int readInt (boolean optimizePositive) throws KryoException {
		if (varIntsEnabled)
			return readVarInt(optimizePositive);
		else
			return readInt();
	}

	public int readVarInt (boolean optimizePositive) throws KryoException {
		niobuffer.position(position);
		if (require(1) < 5) return readInt_slow(optimizePositive);
		position++;
		int b = niobuffer.get();
		int result = b & 0x7F;
		if ((b & 0x80) != 0) {
			position++;
			b = niobuffer.get();
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				position++;
				b = niobuffer.get();
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					position++;
					b = niobuffer.get();
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						position++;
						b = niobuffer.get();
						result |= (b & 0x7F) << 28;
					}
				}
			}
		}
		return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
	}

	private int readInt_slow (boolean optimizePositive) {
		// The buffer is guaranteed to have at least 1 byte.
		position++;
		int b = niobuffer.get();
		int result = b & 0x7F;
		if ((b & 0x80) != 0) {
			require(1);
			position++;
			b = niobuffer.get();
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				require(1);
				position++;
				b = niobuffer.get();
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					require(1);
					position++;
					b = niobuffer.get();
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						require(1);
						position++;
						b = niobuffer.get();
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
		if ((niobuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((niobuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((niobuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((niobuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		return true;
	}

	/** Returns true if enough bytes are available to read a long with {@link #readLong(boolean)}. */
	public boolean canReadLong () throws KryoException {
		if (limit - position >= 9) return true;
		if (optional(5) <= 0) return false;
		int p = position;
		if ((niobuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((niobuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((niobuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((niobuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((niobuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((niobuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((niobuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((niobuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		return true;
	}

	/** Reads the length and string of UTF8 characters, or null. This can read strings written by {@link Output#writeString(String)}
	 * , {@link Output#writeString(CharSequence)}, and {@link Output#writeAscii(String)}.
	 * @return May be null. */
	public String readString () {
		niobuffer.position(position);
		int available = require(1);
		position++;
		int b = niobuffer.get();
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
			position++;
			b = niobuffer.get();
			result |= (b & 0x7F) << 6;
			if ((b & 0x80) != 0) {
				position++;
				b = niobuffer.get();
				result |= (b & 0x7F) << 13;
				if ((b & 0x80) != 0) {
					position++;
					b = niobuffer.get();
					result |= (b & 0x7F) << 20;
					if ((b & 0x80) != 0) {
						position++;
						b = niobuffer.get();
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
			position++;
			b = niobuffer.get();
			result |= (b & 0x7F) << 6;
			if ((b & 0x80) != 0) {
				require(1);
				position++;
				b = niobuffer.get();
				result |= (b & 0x7F) << 13;
				if ((b & 0x80) != 0) {
					require(1);
					position++;
					b = niobuffer.get();
					result |= (b & 0x7F) << 20;
					if ((b & 0x80) != 0) {
						require(1);
						position++;
						b = niobuffer.get();
						result |= (b & 0x7F) << 27;
					}
				}
			}
		}
		return result;
	}

	private void readUtf8 (int charCount) {
		char[] chars = this.chars;
		// Try to read 7 bit ASCII chars.
		int charIndex = 0;
		int count = Math.min(require(1), charCount);
		int position = this.position;
		int b;
		while (charIndex < count) {
			position++;
			b = niobuffer.get();
			if (b < 0) {
				position--;
				break;
			}
			chars[charIndex++] = (char)b;
		}
		this.position = position;
		// If buffer didn't hold all chars or any were not ASCII, use slow path for remainder.
		if (charIndex < charCount) {
			niobuffer.position(position);
			readUtf8_slow(charCount, charIndex);
		}
	}

	private void readUtf8_slow (int charCount, int charIndex) {
		char[] chars = this.chars;
		while (charIndex < charCount) {
			if (position == limit) require(1);
			position++;
			int b = niobuffer.get() & 0xFF;
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
				position++;
				chars[charIndex] = (char)((b & 0x1F) << 6 | niobuffer.get() & 0x3F);
				break;
			case 14:
				require(2);
				position += 2;
				int b2 = niobuffer.get();
				int b3 = niobuffer.get();
				chars[charIndex] = (char)((b & 0x0F) << 12 | (b2 & 0x3F) << 6 | b3 & 0x3F);
				break;
			}
			charIndex++;
		}
	}

	private String readAscii () {
		int end = position;
		int start = end - 1;
		int limit = this.limit;
		int b;
		do {
			if (end == limit) return readAscii_slow();
			end++;
			b = niobuffer.get();
		} while ((b & 0x80) == 0);
		niobuffer.put(end - 1, (byte)(niobuffer.get(end - 1) & 0x7F)); // Mask end of ascii bit.
		byte[] tmp = new byte[end - start];
		niobuffer.position(start);
		niobuffer.get(tmp);
		String value = new String(tmp, 0, 0, end - start);
		niobuffer.put(end - 1, (byte)(niobuffer.get(end - 1) | 0x80));
		position = end;
		niobuffer.position(position);
		return value;
	}

	private String readAscii_slow () {
		position--; // Re-read the first byte.
		// Copy chars currently in buffer.
		int charCount = limit - position;
		if (charCount > chars.length) chars = new char[charCount * 2];
		char[] chars = this.chars;
		for (int i = position, ii = 0, n = limit; i < n; i++, ii++)
			chars[ii] = (char)niobuffer.get(i);
		position = limit;
		// Copy additional chars one by one.
		while (true) {
			require(1);
			position++;
			int b = niobuffer.get();
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
		niobuffer.position(position);
		int available = require(1);
		position++;
		int b = niobuffer.get();
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

	/** Reads a 4 byte float. */
	public float readFloat () throws KryoException {
		require(4);
		position += 4;
		return niobuffer.getFloat();
	}

	/** Reads a 1-5 byte float with reduced precision. */
	public float readFloat (float precision, boolean optimizePositive) throws KryoException {
		return readInt(optimizePositive) / (float)precision;
	}

	/** Reads a 2 byte short. */
	public short readShort () throws KryoException {
		require(2);
		position += 2;
		return niobuffer.getShort();
	}

	/** Reads a 2 byte short as an int from 0 to 65535. */
	public int readShortUnsigned () throws KryoException {
		require(2);
		position += 2;
		return niobuffer.getShort();
	}

	/** Reads an 8 byte long. */
	public long readLong () throws KryoException {
		require(8);
		position += 8;
		return niobuffer.getLong();
	}

	/** {@inheritDoc} */
	public long readLong (boolean optimizePositive) throws KryoException {
		if (varIntsEnabled)
			return readVarLong(optimizePositive);
		else
			return readLong();
	}

	/** {@inheritDoc} */
	public long readVarLong (boolean optimizePositive) throws KryoException {
		niobuffer.position(position);
		if (require(1) < 9) return readLong_slow(optimizePositive);
		position++;
		int b = niobuffer.get();
		long result = b & 0x7F;
		if ((b & 0x80) != 0) {
			position++;
			b = niobuffer.get();
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				position++;
				b = niobuffer.get();
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					position++;
					b = niobuffer.get();
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						position++;
						b = niobuffer.get();
						result |= (long)(b & 0x7F) << 28;
						if ((b & 0x80) != 0) {
							position++;
							b = niobuffer.get();
							result |= (long)(b & 0x7F) << 35;
							if ((b & 0x80) != 0) {
								position++;
								b = niobuffer.get();
								result |= (long)(b & 0x7F) << 42;
								if ((b & 0x80) != 0) {
									position++;
									b = niobuffer.get();
									result |= (long)(b & 0x7F) << 49;
									if ((b & 0x80) != 0) {
										position++;
										b = niobuffer.get();
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
		position++;
		int b = niobuffer.get();
		long result = b & 0x7F;
		if ((b & 0x80) != 0) {
			require(1);
			position++;
			b = niobuffer.get();
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				require(1);
				position++;
				b = niobuffer.get();
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					require(1);
					position++;
					b = niobuffer.get();
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						require(1);
						position++;
						b = niobuffer.get();
						result |= (long)(b & 0x7F) << 28;
						if ((b & 0x80) != 0) {
							require(1);
							position++;
							b = niobuffer.get();
							result |= (long)(b & 0x7F) << 35;
							if ((b & 0x80) != 0) {
								require(1);
								position++;
								b = niobuffer.get();
								result |= (long)(b & 0x7F) << 42;
								if ((b & 0x80) != 0) {
									require(1);
									position++;
									b = niobuffer.get();
									result |= (long)(b & 0x7F) << 49;
									if ((b & 0x80) != 0) {
										require(1);
										position++;
										b = niobuffer.get();
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

	/** Reads a 1 byte boolean. */
	public boolean readBoolean () throws KryoException {
		require(1);
		position++;
		return niobuffer.get() == 1 ? true : false;
	}

	/** Reads a 2 byte char. */
	public char readChar () throws KryoException {
		require(2);
		position += 2;
		return niobuffer.getChar();
	}

	/** Reads an 8 bytes double. */
	public double readDouble () throws KryoException {
		require(8);
		position += 8;
		return niobuffer.getDouble();
	}

	/** Reads a 1-9 byte double with reduced precision. */
	public double readDouble (double precision, boolean optimizePositive) throws KryoException {
		return readLong(optimizePositive) / (double)precision;
	}

	// Methods implementing bulk operations on arrays of primitive types

	/** Bulk input of an int array. */
	public int[] readInts (int length) throws KryoException {
		if (capacity - position >= length * 4 && isNativeOrder()) {
			int[] array = new int[length];
			IntBuffer buf = niobuffer.asIntBuffer();
			buf.get(array);
			position += length * 4;
			niobuffer.position(position);
			return array;
		} else
			return super.readInts(length);
	}

	/** Bulk input of a long array. */
	public long[] readLongs (int length) throws KryoException {
		if (capacity - position >= length * 8 && isNativeOrder()) {
			long[] array = new long[length];
			LongBuffer buf = niobuffer.asLongBuffer();
			buf.get(array);
			position += length * 8;
			niobuffer.position(position);
			return array;
		} else
			return super.readLongs(length);
	}

	/** Bulk input of a float array. */
	public float[] readFloats (int length) throws KryoException {
		if (capacity - position >= length * 4 && isNativeOrder()) {
			float[] array = new float[length];
			FloatBuffer buf = niobuffer.asFloatBuffer();
			buf.get(array);
			position += length * 4;
			niobuffer.position(position);
			return array;
		} else
			return super.readFloats(length);
	}

	/** Bulk input of a short array. */
	public short[] readShorts (int length) throws KryoException {
		if (capacity - position >= length * 2 && isNativeOrder()) {
			short[] array = new short[length];
			ShortBuffer buf = niobuffer.asShortBuffer();
			buf.get(array);
			position += length * 2;
			niobuffer.position(position);
			return array;
		} else
			return super.readShorts(length);
	}

	/** Bulk input of a char array. */
	public char[] readChars (int length) throws KryoException {
		if (capacity - position >= length * 2 && isNativeOrder()) {
			char[] array = new char[length];
			CharBuffer buf = niobuffer.asCharBuffer();
			buf.get(array);
			position += length * 2;
			niobuffer.position(position);
			return array;
		} else
			return super.readChars(length);
	}

	/** Bulk input of a double array. */
	public double[] readDoubles (int length) throws KryoException {
		if (capacity - position >= length * 8 && isNativeOrder()) {
			double[] array = new double[length];
			DoubleBuffer buf = niobuffer.asDoubleBuffer();
			buf.get(array);
			position += length * 8;
			niobuffer.position(position);
			return array;
		} else
			return super.readDoubles(length);
	}

	private boolean isNativeOrder () {
		return byteOrder == nativeOrder;
	}

	/** Return current setting for variable length encoding of integers
	 * @return current setting for variable length encoding of integers */
	public boolean getVarIntsEnabled () {
		return varIntsEnabled;
	}

	/** Controls if a variable length encoding for integer types should be used when serializers suggest it.
	 * 
	 * @param varIntsEnabled */
	public void setVarIntsEnabled (boolean varIntsEnabled) {
		this.varIntsEnabled = varIntsEnabled;
	}
}
