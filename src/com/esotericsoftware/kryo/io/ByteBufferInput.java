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
import com.esotericsoftware.kryo.io.KryoBufferUnderflowException;
import com.esotericsoftware.kryo.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** An {@link Input} that uses a ByteBuffer rather than a byte[].
 * <p>
 * Note that the byte[] {@link #getBuffer() buffer} is not used. Code taking an Input and expecting the byte[] to be used may not
 * work correctly.
 * @author Roman Levenstein <romixlev@gmail.com>
 * @author Nathan Sweet */
public class ByteBufferInput extends Input {
	private static final ByteOrder nativeOrder = ByteOrder.nativeOrder();

	protected ByteBuffer byteBuffer;
	private byte[] tempBuffer;

	/** Creates an uninitialized Input, {@link #setBuffer(ByteBuffer)} must be called before the Input is used. */
	public ByteBufferInput () {
	}

	/** Creates a new Input for reading from a direct {@link ByteBuffer}.
	 * @param bufferSize The size of the buffer. An exception is thrown if more bytes than this are read and
	 *           {@link #fill(ByteBuffer, int, int)} does not supply more bytes. */
	public ByteBufferInput (int bufferSize) {
		this.capacity = bufferSize;
		byteBuffer = ByteBuffer.allocateDirect(bufferSize);
	}

	/** Creates a new Input for reading from a {@link ByteBuffer} which is filled with the specified bytes. */
	public ByteBufferInput (byte[] bytes) {
		this(bytes, 0, bytes.length);
	}

	/** Creates a new Input for reading from a {@link ByteBuffer} which is filled with the specified bytes.
	 * @see #setBuffer(byte[], int, int) */
	public ByteBufferInput (byte[] bytes, int offset, int count) {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
		buffer.put(bytes);
		flipBuffer(buffer);
		setBuffer(buffer);
	}

	/** Creates a new Input for reading from a ByteBuffer. */
	public ByteBufferInput (ByteBuffer buffer) {
		setBuffer(buffer);
	}

	/** @see Input#Input(InputStream) */
	public ByteBufferInput (InputStream inputStream) {
		this(4096);
		if (inputStream == null) throw new IllegalArgumentException("inputStream cannot be null.");
		this.inputStream = inputStream;
	}

	/** @see Input#Input(InputStream, int) */
	public ByteBufferInput (InputStream inputStream, int bufferSize) {
		this(bufferSize);
		if (inputStream == null) throw new IllegalArgumentException("inputStream cannot be null.");
		this.inputStream = inputStream;
	}

	/** Throws {@link UnsupportedOperationException} because this input uses a ByteBuffer, not a byte[].
	 * @deprecated
	 * @see #getByteBuffer() */
	public byte[] getBuffer () {
		throw new UnsupportedOperationException("This input does not used a byte[], see #getByteBuffer().");
	}

	/** Throws {@link UnsupportedOperationException} because this input uses a ByteBuffer, not a byte[].
	 * @deprecated
	 * @see #setBuffer(ByteBuffer) */
	public void setBuffer (byte[] bytes) {
		throw new UnsupportedOperationException("This input does not used a byte[], see #setByteBuffer(ByteBuffer).");
	}

	/** Throws {@link UnsupportedOperationException} because this input uses a ByteBuffer, not a byte[].
	 * @deprecated
	 * @see #setBuffer(ByteBuffer) */
	public void setBuffer (byte[] bytes, int offset, int count) {
		throw new UnsupportedOperationException("This input does not used a byte[], see #setByteBufferByteBuffer().");
	}

	/** Sets a new buffer to read from. The bytes are not copied, the old buffer is discarded and the new buffer used in its place.
	 * The position, limit, and capacity are set to match the specified buffer. The total is reset. The
	 * {@link #setInputStream(InputStream) InputStream} is set to null. */
	public void setBuffer (ByteBuffer buffer) {
		if (buffer == null) throw new IllegalArgumentException("buffer cannot be null.");
		byteBuffer = buffer;
		position = buffer.position();
		limit = buffer.limit();
		capacity = buffer.capacity();
		total = 0;
		inputStream = null;
	}

	public ByteBuffer getByteBuffer () {
		return byteBuffer;
	}

	public void setInputStream (InputStream inputStream) {
		this.inputStream = inputStream;
		limit = 0;
		reset();
	}

	public void reset () {
		super.reset();
		setBufferPosition(byteBuffer, 0);
	}

	/** Fills the buffer with more bytes. May leave the buffer position changed. The default implementation reads from the
	 * {@link #getInputStream() InputStream}, if set. Can be overridden to fill the bytes from another source. */
	protected int fill (ByteBuffer buffer, int offset, int count) throws KryoException {
		if (inputStream == null) return -1;
		try {
			if (tempBuffer == null) tempBuffer = new byte[2048];
			setBufferPosition(buffer, offset);
			int total = 0;
			while (count > 0) {
				int read = inputStream.read(tempBuffer, 0, Math.min(tempBuffer.length, count));
				if (read == -1) {
					if (total == 0) return -1;
					break;
				}
				buffer.put(tempBuffer, 0, read);
				count -= read;
				total += read;
			}
			return total;
		} catch (IOException ex) {
			throw new KryoException(ex);
		}
	}

	protected int require (int required) throws KryoException {
		int remaining = limit - position;
		if (remaining >= required) return remaining;
		if (required > capacity) throw new KryoException("Buffer too small: capacity: " + capacity + ", required: " + required);

		int count;

		// Try to fill the buffer.
		if (remaining > 0) {
			count = fill(byteBuffer, limit, capacity - limit);
			if (count == -1) throw new KryoBufferUnderflowException("Buffer underflow.");
			setBufferPosition(byteBuffer, position);
			remaining += count;
			if (remaining >= required) {
				limit += count;
				return remaining;
			}
		}

		// Compact.
		byteBuffer.compact(); // Buffer's position is at end of compacted bytes.
		total += position;
		position = 0;

		// Try to fill again.
		while (true) {
			count = fill(byteBuffer, remaining, capacity - remaining);
			if (count == -1) {
				if (remaining >= required) break;
				throw new KryoBufferUnderflowException("Buffer underflow.");
			}
			remaining += count;
			if (remaining >= required) break; // Enough has been read.
		}
		limit = remaining;
		setBufferPosition(byteBuffer, 0);
		return remaining;
	}

	/** Fills the buffer with at least the number of bytes specified, if possible.
	 * @param optional Must be > 0.
	 * @return the number of bytes remaining, but not more than optional, or -1 if {@link #fill(ByteBuffer, int, int)} is unable to
	 *         provide more bytes. */
	protected int optional (int optional) throws KryoException {
		int remaining = limit - position;
		if (remaining >= optional) return optional;
		optional = Math.min(optional, capacity);

		// Try to fill the buffer.
		int count = fill(byteBuffer, limit, capacity - limit);
		setBufferPosition(byteBuffer, position);
		if (count == -1) return remaining == 0 ? -1 : Math.min(remaining, optional);
		remaining += count;
		if (remaining >= optional) {
			limit += count;
			return optional;
		}

		// Compact.
		byteBuffer.compact(); // Buffer's position is at end of compacted bytes.
		total += position;
		position = 0;

		// Try to fill again.
		while (true) {
			count = fill(byteBuffer, remaining, capacity - remaining);
			if (count == -1) break;
			remaining += count;
			if (remaining >= optional) break; // Enough has been read.
		}
		limit = remaining;
		setBufferPosition(byteBuffer, 0);
		return remaining == 0 ? -1 : Math.min(remaining, optional);
	}

	// InputStream:

	public int read () throws KryoException {
		if (optional(1) <= 0) return -1;
		position++;
		return byteBuffer.get() & 0xFF;
	}

	public int read (byte[] bytes) throws KryoException {
		return read(bytes, 0, bytes.length);
	}

	public int read (byte[] bytes, int offset, int count) throws KryoException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		int startingCount = count;
		int copyCount = Math.min(limit - position, count);
		while (true) {
			byteBuffer.get(bytes, offset, copyCount);
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

	public void setPosition (int position) {
		this.position = position;
		setBufferPosition(byteBuffer, position);
	}

	public void setLimit (int limit) {
		this.limit = limit;
		setBufferLimit(byteBuffer, limit);
	}

	public void skip (int count) throws KryoException {
		super.skip(count);
		setBufferPosition(byteBuffer, position);
	}

	public long skip (long count) throws KryoException {
		long remaining = count;
		while (remaining > 0) {
			int skip = (int)Math.min(Util.maxArraySize, remaining);
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

	private int getBufferPosition (Buffer buffer) {
		return buffer.position();
	}

	private void setBufferPosition (Buffer buffer, int position) {
		buffer.position(position);
	}

	private void setBufferLimit (Buffer buffer, int limit) {
		buffer.limit(limit);
	}

	private void flipBuffer (Buffer buffer) {
		buffer.flip();
	}

	// byte:

	public byte readByte () throws KryoException {
		if (position == limit) require(1);
		position++;
		return byteBuffer.get();
	}

	public int readByteUnsigned () throws KryoException {
		if (position == limit) require(1);
		position++;
		return byteBuffer.get() & 0xFF;
	}

	public byte[] readBytes (int length) throws KryoException {
		byte[] bytes = new byte[length];
		readBytes(bytes, 0, length);
		return bytes;
	}

	public void readBytes (byte[] bytes, int offset, int count) throws KryoException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		int copyCount = Math.min(limit - position, count);
		while (true) {
			byteBuffer.get(bytes, offset, copyCount);
			position += copyCount;
			count -= copyCount;
			if (count == 0) break;
			offset += copyCount;
			copyCount = Math.min(count, capacity);
			require(copyCount);
		}
	}

	public int readInt (int count) {
		if (count < 0 || count > 4) throw new IllegalArgumentException("count must be >= 0 and <= 4: " + count);
		require(count);
		position += count;
		ByteBuffer byteBuffer = this.byteBuffer;
		switch (count) {
			case 1:
				return byteBuffer.get();
			case 2:
				return byteBuffer.get() << 8
					| byteBuffer.get() & 0xFF;
			case 3:
				return byteBuffer.get() << 16
					| (byteBuffer.get() & 0xFF) << 8
					| byteBuffer.get() & 0xFF;
			case 4:
				return byteBuffer.get() << 24
					| (byteBuffer.get() & 0xFF) << 16
					| (byteBuffer.get() & 0xFF) << 8
					| byteBuffer.get() & 0xFF;
		}
		throw new IllegalStateException(); // impossible
	}

	// int:

	public int readInt () throws KryoException {
		require(4);
		position += 4;
		ByteBuffer byteBuffer = this.byteBuffer;
		return byteBuffer.get() & 0xFF //
			| (byteBuffer.get() & 0xFF) << 8 //
			| (byteBuffer.get() & 0xFF) << 16 //
			| (byteBuffer.get() & 0xFF) << 24;
	}

	public int readVarInt (boolean optimizePositive) throws KryoException {
		if (require(1) < 5) return readVarInt_slow(optimizePositive);
		int b = byteBuffer.get();
		int result = b & 0x7F;
		if ((b & 0x80) != 0) {
			ByteBuffer byteBuffer = this.byteBuffer;
			b = byteBuffer.get();
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				b = byteBuffer.get();
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					b = byteBuffer.get();
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						b = byteBuffer.get();
						result |= (b & 0x7F) << 28;
					}
				}
			}
		}
		position = getBufferPosition(byteBuffer);
		return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
	}

	private int readVarInt_slow (boolean optimizePositive) {
		// The buffer is guaranteed to have at least 1 byte.
		position++;
		int b = byteBuffer.get();
		int result = b & 0x7F;
		if ((b & 0x80) != 0) {
			if (position == limit) require(1);
			ByteBuffer byteBuffer = this.byteBuffer;
			position++;
			b = byteBuffer.get();
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				if (position == limit) require(1);
				position++;
				b = byteBuffer.get();
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					if (position == limit) require(1);
					position++;
					b = byteBuffer.get();
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						if (position == limit) require(1);
						position++;
						b = byteBuffer.get();
						result |= (b & 0x7F) << 28;
					}
				}
			}
		}
		return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
	}

	public boolean canReadVarInt () throws KryoException {
		if (limit - position >= 5) return true;
		if (optional(5) <= 0) return false;
		int p = position, limit = this.limit;
		ByteBuffer byteBuffer = this.byteBuffer;
		if ((byteBuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((byteBuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((byteBuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((byteBuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		return true;
	}

	/** Reads the boolean part of a varint flag. The position is not advanced, {@link #readVarIntFlag(boolean)} should be used to
	 * advance the position. */
	public boolean readVarIntFlag () {
		if (position == limit) require(1);
		return (byteBuffer.get(position) & 0x80) != 0;
	}

	/** Reads the 1-5 byte int part of a varint flag. The position is advanced so if the boolean part is needed it should be read
	 * first with {@link #readVarIntFlag()}. */
	public int readVarIntFlag (boolean optimizePositive) {
		if (require(1) < 5) return readVarIntFlag_slow(optimizePositive);
		int b = byteBuffer.get();
		int result = b & 0x3F; // Mask first 6 bits.
		if ((b & 0x40) != 0) { // Bit 7 means another byte, bit 8 means UTF8.
			ByteBuffer byteBuffer = this.byteBuffer;
			b = byteBuffer.get();
			result |= (b & 0x7F) << 6;
			if ((b & 0x80) != 0) {
				b = byteBuffer.get();
				result |= (b & 0x7F) << 13;
				if ((b & 0x80) != 0) {
					b = byteBuffer.get();
					result |= (b & 0x7F) << 20;
					if ((b & 0x80) != 0) {
						b = byteBuffer.get();
						result |= (b & 0x7F) << 27;
					}
				}
			}
		}
		position = getBufferPosition(byteBuffer);
		return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
	}

	private int readVarIntFlag_slow (boolean optimizePositive) {
		// The buffer is guaranteed to have at least 1 byte.
		position++;
		int b = byteBuffer.get();
		int result = b & 0x3F;
		if ((b & 0x40) != 0) {
			if (position == limit) require(1);
			position++;
			ByteBuffer byteBuffer = this.byteBuffer;
			b = byteBuffer.get();
			result |= (b & 0x7F) << 6;
			if ((b & 0x80) != 0) {
				if (position == limit) require(1);
				position++;
				b = byteBuffer.get();
				result |= (b & 0x7F) << 13;
				if ((b & 0x80) != 0) {
					if (position == limit) require(1);
					position++;
					b = byteBuffer.get();
					result |= (b & 0x7F) << 20;
					if ((b & 0x80) != 0) {
						if (position == limit) require(1);
						position++;
						b = byteBuffer.get();
						result |= (b & 0x7F) << 27;
					}
				}
			}
		}
		return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
	}

	// long:

	public long readLong () throws KryoException {
		require(8);
		position += 8;
		ByteBuffer byteBuffer = this.byteBuffer;
		return byteBuffer.get() & 0xFF //
			| (byteBuffer.get() & 0xFF) << 8 //
			| (byteBuffer.get() & 0xFF) << 16 //
			| (long)(byteBuffer.get() & 0xFF) << 24 //
			| (long)(byteBuffer.get() & 0xFF) << 32 //
			| (long)(byteBuffer.get() & 0xFF) << 40 //
			| (long)(byteBuffer.get() & 0xFF) << 48 //
			| (long)byteBuffer.get() << 56;
	}

	public long readVarLong (boolean optimizePositive) throws KryoException {
		if (require(1) < 9) return readVarLong_slow(optimizePositive);
		int b = byteBuffer.get();
		long result = b & 0x7F;
		if ((b & 0x80) != 0) {
			ByteBuffer byteBuffer = this.byteBuffer;
			b = byteBuffer.get();
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				b = byteBuffer.get();
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					b = byteBuffer.get();
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						b = byteBuffer.get();
						result |= (long)(b & 0x7F) << 28;
						if ((b & 0x80) != 0) {
							b = byteBuffer.get();
							result |= (long)(b & 0x7F) << 35;
							if ((b & 0x80) != 0) {
								b = byteBuffer.get();
								result |= (long)(b & 0x7F) << 42;
								if ((b & 0x80) != 0) {
									b = byteBuffer.get();
									result |= (long)(b & 0x7F) << 49;
									if ((b & 0x80) != 0) {
										b = byteBuffer.get();
										result |= (long)b << 56;
									}
								}
							}
						}
					}
				}
			}
		}
		position = getBufferPosition(byteBuffer);
		return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
	}

	private long readVarLong_slow (boolean optimizePositive) {
		// The buffer is guaranteed to have at least 1 byte.
		position++;
		int b = byteBuffer.get();
		long result = b & 0x7F;
		if ((b & 0x80) != 0) {
			if (position == limit) require(1);
			ByteBuffer byteBuffer = this.byteBuffer;
			position++;
			b = byteBuffer.get();
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				if (position == limit) require(1);
				position++;
				b = byteBuffer.get();
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					if (position == limit) require(1);
					position++;
					b = byteBuffer.get();
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						if (position == limit) require(1);
						position++;
						b = byteBuffer.get();
						result |= (long)(b & 0x7F) << 28;
						if ((b & 0x80) != 0) {
							if (position == limit) require(1);
							position++;
							b = byteBuffer.get();
							result |= (long)(b & 0x7F) << 35;
							if ((b & 0x80) != 0) {
								if (position == limit) require(1);
								position++;
								b = byteBuffer.get();
								result |= (long)(b & 0x7F) << 42;
								if ((b & 0x80) != 0) {
									if (position == limit) require(1);
									position++;
									b = byteBuffer.get();
									result |= (long)(b & 0x7F) << 49;
									if ((b & 0x80) != 0) {
										if (position == limit) require(1);
										position++;
										b = byteBuffer.get();
										result |= (long)b << 56;
									}
								}
							}
						}
					}
				}
			}
		}
		return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
	}

	public boolean canReadVarLong () throws KryoException {
		if (limit - position >= 9) return true;
		if (optional(5) <= 0) return false;
		int p = position, limit = this.limit;
		ByteBuffer byteBuffer = this.byteBuffer;
		if ((byteBuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((byteBuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((byteBuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((byteBuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((byteBuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((byteBuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((byteBuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		if ((byteBuffer.get(p++) & 0x80) == 0) return true;
		if (p == limit) return false;
		return true;
	}

	// float:

	public float readFloat () throws KryoException {
		require(4);
		ByteBuffer byteBuffer = this.byteBuffer;
		int p = this.position;
		this.position = p + 4;
		return Float.intBitsToFloat(byteBuffer.get() & 0xFF //
			| (byteBuffer.get() & 0xFF) << 8 //
			| (byteBuffer.get() & 0xFF) << 16 //
			| (byteBuffer.get() & 0xFF) << 24);
	}

	// double:

	public double readDouble () throws KryoException {
		require(8);
		ByteBuffer byteBuffer = this.byteBuffer;
		int p = position;
		position = p + 8;
		return Double.longBitsToDouble(byteBuffer.get() & 0xFF //
			| (byteBuffer.get() & 0xFF) << 8 //
			| (byteBuffer.get() & 0xFF) << 16 //
			| (long)(byteBuffer.get() & 0xFF) << 24 //
			| (long)(byteBuffer.get() & 0xFF) << 32 //
			| (long)(byteBuffer.get() & 0xFF) << 40 //
			| (long)(byteBuffer.get() & 0xFF) << 48 //
			| (long)byteBuffer.get() << 56);
	}

	// boolean:

	public boolean readBoolean () throws KryoException {
		if (position == limit) require(1);
		position++;
		return byteBuffer.get() == 1 ? true : false;
	}

	// short:

	public short readShort () throws KryoException {
		require(2);
		position += 2;
		return (short)((byteBuffer.get() & 0xFF) | ((byteBuffer.get() & 0xFF) << 8));
	}

	public int readShortUnsigned () throws KryoException {
		require(2);
		position += 2;
		return (byteBuffer.get() & 0xFF) | ((byteBuffer.get() & 0xFF) << 8);
	}

	// char:

	public char readChar () throws KryoException {
		require(2);
		position += 2;
		return (char)((byteBuffer.get() & 0xFF) | ((byteBuffer.get() & 0xFF) << 8));
	}

	// String:

	public String readString () {
		if (!readVarIntFlag()) return readAsciiString(); // ASCII.
		// Null, empty, or UTF8.
		int charCount = readVarIntFlag(true);
		switch (charCount) {
		case 0:
			return null;
		case 1:
			return "";
		}
		charCount--;
		readUtf8Chars(charCount);
		return new String(chars, 0, charCount);
	}

	public StringBuilder readStringBuilder () {
		if (!readVarIntFlag()) return new StringBuilder(readAsciiString()); // ASCII.
		// Null, empty, or UTF8.
		int charCount = readVarIntFlag(true);
		switch (charCount) {
		case 0:
			return null;
		case 1:
			return new StringBuilder("");
		}
		charCount--;
		readUtf8Chars(charCount);
		StringBuilder builder = new StringBuilder(charCount);
		builder.append(chars, 0, charCount);
		return builder;
	}

	private void readUtf8Chars (int charCount) {
		if (chars.length < charCount) chars = new char[charCount];
		char[] chars = this.chars;
		// Try to read 7 bit ASCII chars.
		ByteBuffer byteBuffer = this.byteBuffer;
		int charIndex = 0;
		int count = Math.min(require(1), charCount);
		while (charIndex < count) {
			int b = byteBuffer.get();
			if (b < 0) break;
			chars[charIndex++] = (char)b;
		}
		position += charIndex;
		// If buffer didn't hold all chars or any were not ASCII, use slow path for remainder.
		if (charIndex < charCount) {
			setBufferPosition(byteBuffer, position);
			readUtf8Chars_slow(charCount, charIndex);
		}
	}

	private void readUtf8Chars_slow (int charCount, int charIndex) {
		ByteBuffer byteBuffer = this.byteBuffer;
		char[] chars = this.chars;
		while (charIndex < charCount) {
			if (position == limit) require(1);
			position++;
			int b = byteBuffer.get() & 0xFF;
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
				chars[charIndex] = (char)((b & 0x1F) << 6 | byteBuffer.get() & 0x3F);
				break;
			case 14:
				require(2);
				position += 2;
				int b2 = byteBuffer.get();
				int b3 = byteBuffer.get();
				chars[charIndex] = (char)((b & 0x0F) << 12 | (b2 & 0x3F) << 6 | b3 & 0x3F);
				break;
			}
			charIndex++;
		}
	}

	private String readAsciiString () {
		char[] chars = this.chars;
		ByteBuffer byteBuffer = this.byteBuffer;
		int charCount = 0;
		for (int n = Math.min(chars.length, limit - position); charCount < n; charCount++) {
			int b = byteBuffer.get();
			if ((b & 0x80) == 0x80) {
				position = getBufferPosition(byteBuffer);
				chars[charCount] = (char)(b & 0x7F);
				return new String(chars, 0, charCount + 1);
			}
			chars[charCount] = (char)b;
		}
		position = getBufferPosition(byteBuffer);
		return readAscii_slow(charCount);
	}

	private String readAscii_slow (int charCount) {
		char[] chars = this.chars;
		ByteBuffer byteBuffer = this.byteBuffer;
		while (true) {
			if (position == limit) require(1);
			position++;
			int b = byteBuffer.get();
			if (charCount == chars.length) {
				char[] newChars = new char[charCount * 2];
				System.arraycopy(chars, 0, newChars, 0, charCount);
				chars = newChars;
				this.chars = newChars;
			}
			if ((b & 0x80) == 0x80) {
				chars[charCount] = (char)(b & 0x7F);
				return new String(chars, 0, charCount + 1);
			}
			chars[charCount++] = (char)b;
		}
	}

	// Primitive arrays:

	public int[] readInts (int length) throws KryoException {
		int[] array = new int[length];
		if (optional(length << 2) == length << 2) {
			ByteBuffer byteBuffer = this.byteBuffer;
			for (int i = 0; i < length; i++) {
				array[i] = byteBuffer.get() & 0xFF //
					| (byteBuffer.get() & 0xFF) << 8 //
					| (byteBuffer.get() & 0xFF) << 16 //
					| (byteBuffer.get() & 0xFF) << 24;
			}
			position = getBufferPosition(byteBuffer);
		} else {
			for (int i = 0; i < length; i++)
				array[i] = readInt();
		}
		return array;
	}

	public long[] readLongs (int length) throws KryoException {
		long[] array = new long[length];
		if (optional(length << 3) == length << 3) {
			ByteBuffer byteBuffer = this.byteBuffer;
			for (int i = 0; i < length; i++) {
				array[i] = byteBuffer.get() & 0xFF//
					| (byteBuffer.get() & 0xFF) << 8 //
					| (byteBuffer.get() & 0xFF) << 16 //
					| (long)(byteBuffer.get() & 0xFF) << 24 //
					| (long)(byteBuffer.get() & 0xFF) << 32 //
					| (long)(byteBuffer.get() & 0xFF) << 40 //
					| (long)(byteBuffer.get() & 0xFF) << 48 //
					| (long)byteBuffer.get() << 56;
			}
			position = getBufferPosition(byteBuffer);
		} else {
			for (int i = 0; i < length; i++)
				array[i] = readLong();
		}
		return array;
	}

	public float[] readFloats (int length) throws KryoException {
		float[] array = new float[length];
		if (optional(length << 2) == length << 2) {
			ByteBuffer byteBuffer = this.byteBuffer;
			for (int i = 0; i < length; i++) {
				array[i] = Float.intBitsToFloat(byteBuffer.get() & 0xFF //
					| (byteBuffer.get() & 0xFF) << 8 //
					| (byteBuffer.get() & 0xFF) << 16 //
					| (byteBuffer.get() & 0xFF) << 24);
			}
			position = getBufferPosition(byteBuffer);
		} else {
			for (int i = 0; i < length; i++)
				array[i] = readFloat();
		}
		return array;
	}

	public double[] readDoubles (int length) throws KryoException {
		double[] array = new double[length];
		if (optional(length << 3) == length << 3) {
			ByteBuffer byteBuffer = this.byteBuffer;
			for (int i = 0; i < length; i++) {
				array[i] = Double.longBitsToDouble(byteBuffer.get() & 0xFF //
					| (byteBuffer.get() & 0xFF) << 8 //
					| (byteBuffer.get() & 0xFF) << 16 //
					| (long)(byteBuffer.get() & 0xFF) << 24 //
					| (long)(byteBuffer.get() & 0xFF) << 32 //
					| (long)(byteBuffer.get() & 0xFF) << 40 //
					| (long)(byteBuffer.get() & 0xFF) << 48 //
					| (long)byteBuffer.get() << 56);
			}
			position = getBufferPosition(byteBuffer);
		} else {
			for (int i = 0; i < length; i++)
				array[i] = readDouble();
		}
		return array;
	}

	public short[] readShorts (int length) throws KryoException {
		short[] array = new short[length];
		if (optional(length << 1) == length << 1) {
			ByteBuffer byteBuffer = this.byteBuffer;
			for (int i = 0; i < length; i++)
				array[i] = (short)((byteBuffer.get() & 0xFF) | ((byteBuffer.get() & 0xFF) << 8));
			position = getBufferPosition(byteBuffer);
		} else {
			for (int i = 0; i < length; i++)
				array[i] = readShort();
		}
		return array;
	}

	public char[] readChars (int length) throws KryoException {
		char[] array = new char[length];
		if (optional(length << 1) == length << 1) {
			ByteBuffer byteBuffer = this.byteBuffer;
			for (int i = 0; i < length; i++)
				array[i] = (char)((byteBuffer.get() & 0xFF) | ((byteBuffer.get() & 0xFF) << 8));
			position = getBufferPosition(byteBuffer);
		} else {
			for (int i = 0; i < length; i++)
				array[i] = readChar();
		}
		return array;
	}

	public boolean[] readBooleans (int length) throws KryoException {
		boolean[] array = new boolean[length];
		if (optional(length) == length) {
			ByteBuffer byteBuffer = this.byteBuffer;
			for (int i = 0; i < length; i++)
				array[i] = byteBuffer.get() != 0;
			position = getBufferPosition(byteBuffer);
		} else {
			for (int i = 0; i < length; i++)
				array[i] = readBoolean();
		}
		return array;
	}
}
