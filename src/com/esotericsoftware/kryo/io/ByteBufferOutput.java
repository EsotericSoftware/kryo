
package com.esotericsoftware.kryo.io;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.util.UnsafeUtil;

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

/** An OutputStream that buffers data in a byte array and optionally flushes to another OutputStream. Utility methods are provided
 * for efficiently writing primitive types and strings.
 * 
 * @author Roman Levenstein <romixlev@gmail.com> */
public class ByteBufferOutput extends Output {
	protected ByteBuffer niobuffer;

	protected boolean varIntsEnabled = true;

	// Default byte order is BIG_ENDIAN to be compatible to the base class
	ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

	protected final static ByteOrder nativeOrder = ByteOrder.nativeOrder();

	/** Creates an uninitialized Output. A buffer must be set before the Output is used.
	 * @see #setBuffer(ByteBuffer, int) */
	public ByteBufferOutput () {
	}

	/** Creates a new Output for writing to a direct ByteBuffer.
	 * @param bufferSize The initial and maximum size of the buffer. An exception is thrown if this size is exceeded. */
	public ByteBufferOutput (int bufferSize) {
		this(bufferSize, bufferSize);
	}

	/** Creates a new Output for writing to a direct ByteBuffer.
	 * @param bufferSize The initial size of the buffer.
	 * @param maxBufferSize The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown. */
	public ByteBufferOutput (int bufferSize, int maxBufferSize) {
		if (maxBufferSize < -1) throw new IllegalArgumentException("maxBufferSize cannot be < -1: " + maxBufferSize);
		this.capacity = bufferSize;
		this.maxCapacity = maxBufferSize == -1 ? Integer.MAX_VALUE : maxBufferSize;
		niobuffer = ByteBuffer.allocateDirect(bufferSize);
		niobuffer.order(byteOrder);
	}

	/** Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used. */
	public ByteBufferOutput (OutputStream outputStream) {
		this(4096, 4096);
		if (outputStream == null) throw new IllegalArgumentException("outputStream cannot be null.");
		this.outputStream = outputStream;
	}

	/** Creates a new Output for writing to an OutputStream. */
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
	 * @param maxBufferSize The buffer is doubled as needed until it exceeds maxCapacity and an exception is thrown. */
	public ByteBufferOutput (ByteBuffer buffer, int maxBufferSize) {
		setBuffer(buffer, maxBufferSize);
	}

	/** Creates a direct ByteBuffer of a given size at a given address.
	 * <p>
	 * Typical usage could look like this snippet:
	 * 
	 * <pre>
	 * // Explicitly allocate memory
	 * long bufAddress = UnsafeUtil.unsafe().allocateMemory(4096);
	 * // Create a ByteBufferOutput using the allocated memory region
	 * ByteBufferOutput buffer = new ByteBufferOutput(bufAddress, 4096);
	 * 
	 * // Do some operations on this buffer here
	 * 
	 * // Say that ByteBuffer won't be used anymore
	 * buffer.release();
	 * // Release the allocated region
	 * UnsafeUtil.unsafe().freeMemory(bufAddress);
	 * </pre>
	 * @param address starting address of a memory region pre-allocated using Unsafe.allocateMemory()
	 * @param maxBufferSize */
	public ByteBufferOutput (long address, int maxBufferSize) {
		niobuffer = UnsafeUtil.getDirectBufferAt(address, maxBufferSize);
		setBuffer(niobuffer, maxBufferSize);
	}

	/** Release a direct buffer. {@link #setBuffer(ByteBuffer, int)} should be called before next write operations can be called.
	 * 
	 * NOTE: If Cleaner is not accessible due to SecurityManager restrictions, reflection could be used to obtain the "clean"
	 * method and then invoke it. */
	public void release () {
		clear();
		UnsafeUtil.releaseBuffer(niobuffer);
		niobuffer = null;
	}

	public ByteOrder order () {
		return byteOrder;
	}

	public void order (ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
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

	/** Sets the buffer that will be written to. maxCapacity is set to the specified buffer's capacity.
	 * @see #setBuffer(ByteBuffer, int) */
	public void setBuffer (ByteBuffer buffer) {
		setBuffer(buffer, buffer.capacity());
	}

	/** Sets the buffer that will be written to. The byte order, position and capacity are set to match the specified buffer. The
	 * total is set to 0. The {@link #setOutputStream(OutputStream) OutputStream} is set to null.
	 * @param maxBufferSize The buffer is doubled as needed until it exceeds maxCapacity and an exception is thrown. */
	public void setBuffer (ByteBuffer buffer, int maxBufferSize) {
		if (buffer == null) throw new IllegalArgumentException("buffer cannot be null.");
		if (maxBufferSize < -1) throw new IllegalArgumentException("maxBufferSize cannot be < -1: " + maxBufferSize);
		this.niobuffer = buffer;
		this.maxCapacity = maxBufferSize == -1 ? Integer.MAX_VALUE : maxBufferSize;
		byteOrder = buffer.order();
		capacity = buffer.capacity();
		position = buffer.position();
		total = 0;
		outputStream = null;
	}

	/** Returns the buffer. The bytes between zero and {@link #position()} are the data that has been written. */
	public ByteBuffer getByteBuffer () {
		niobuffer.position(position);
		return niobuffer;
	}

	/** Returns a new byte array containing the bytes currently in the buffer between zero and {@link #position()}. */
	public byte[] toBytes () {
		byte[] newBuffer = new byte[position];
		niobuffer.position(position);
		niobuffer.position(0);
		niobuffer.get(newBuffer, 0, position);
		return newBuffer;
	}

	/** Sets the current position in the buffer. */
	public void setPosition (int position) {
		this.position = position;
	}

	/** Sets the position and total to zero. */
	public void clear () {
		niobuffer.clear();
		position = 0;
		total = 0;
	}

	/** @return true if the buffer has been resized. */
	protected boolean require (int required) throws KryoException {
		if (capacity - position >= required) return false;
		if (required > maxCapacity)
			throw new KryoException("Buffer overflow. Max capacity: " + maxCapacity + ", required: " + required);
		flush();
		while (capacity - position < required) {
			if (capacity == maxCapacity)
				throw new KryoException("Buffer overflow. Available: " + (capacity - position) + ", required: " + required);
			// Grow buffer.
			if (capacity == 0) capacity = 1;
			capacity = Math.min(capacity * 2, maxCapacity);
			if (capacity < 0) capacity = maxCapacity;
			ByteBuffer newBuffer = (niobuffer != null && !niobuffer.isDirect()) ? ByteBuffer.allocate(capacity) : ByteBuffer
				.allocateDirect(capacity);
			// Copy the whole buffer
			niobuffer.position(0);
			newBuffer.put(niobuffer);
			newBuffer.order(byteOrder);
			niobuffer = newBuffer;
		}
		return true;
	}

	// OutputStream

	/** Writes the buffered bytes to the underlying OutputStream, if any. */
	public void flush () throws KryoException {
		if (outputStream == null) return;
		try {
			byte[] tmp = new byte[position];
			niobuffer.position(0);
			niobuffer.get(tmp);
			niobuffer.position(0);
			outputStream.write(tmp, 0, position);
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
		niobuffer.put((byte)value);
		position++;
	}

	/** Writes the bytes. Note the byte[] length is not written. */
	public void write (byte[] bytes) throws KryoException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		writeBytes(bytes, 0, bytes.length);
	}

	/** Writes the bytes. Note the byte[] length is not written. */
	public void write (byte[] bytes, int offset, int length) throws KryoException {
		writeBytes(bytes, offset, length);
	}

	// byte

	public void writeByte (byte value) throws KryoException {
		if (position == capacity) require(1);
		niobuffer.put(value);
		position++;
	}

	public void writeByte (int value) throws KryoException {
		if (position == capacity) require(1);
		niobuffer.put((byte)value);
		position++;
	}

	/** Writes the bytes. Note the byte[] length is not written. */
	public void writeBytes (byte[] bytes) throws KryoException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		writeBytes(bytes, 0, bytes.length);
	}

	/** Writes the bytes. Note the byte[] length is not written. */
	public void writeBytes (byte[] bytes, int offset, int count) throws KryoException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		int copyCount = Math.min(capacity - position, count);
		while (true) {
			niobuffer.put(bytes, offset, copyCount);
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
		niobuffer.putInt(value);
		position += 4;
	}

	public int writeInt (int value, boolean optimizePositive) throws KryoException {
		if (!varIntsEnabled) {
			writeInt(value);
			return 4;
		} else
			return writeVarInt(value, optimizePositive);
	}

	public int writeVarInt (int val, boolean optimizePositive) throws KryoException {
		niobuffer.position(position);

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
			niobuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeInt(varInt);
			niobuffer.order(byteOrder);
			position -= 2;
			niobuffer.position(position);
			return 2;
		}

		varInt |= (0x80 << 8);
		varInt |= ((value & 0x7F) << 16);

		value >>>= 7;

		if (value == 0) {
			niobuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeInt(varInt);
			niobuffer.order(byteOrder);
			position -= 1;
			niobuffer.position(position);
			return 3;
		}

		varInt |= (0x80 << 16);
		varInt |= ((value & 0x7F) << 24);

		value >>>= 7;

		if (value == 0) {
			niobuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeInt(varInt);
			niobuffer.order(byteOrder);
			position -= 0;
			return 4;
		}

		varInt |= (0x80 << 24);
		long varLong = (varInt & 0xFFFFFFFFL) | (((long)value) << 32);

		niobuffer.order(ByteOrder.LITTLE_ENDIAN);
		writeLong(varLong);
		niobuffer.order(byteOrder);

		position -= 3;
		niobuffer.position(position);
		return 5;
	}

	// string

	/** Writes the length and string, or null. Short strings are checked and if ASCII they are written more efficiently, else they
	 * are written as UTF8. If a string is known to be ASCII, {@link #writeAscii(String)} may be used. The string can be read using
	 * {@link Input#readString()} or {@link Input#readStringBuilder()}.
	 * @param value May be null. */
	public void writeString (String value) throws KryoException {
		niobuffer.position(position);
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
				niobuffer.put(tmp, 0, tmp.length);
				position += charCount;
			}
			niobuffer.put(position - 1, (byte)(niobuffer.get(position - 1) | 0x80));
		} else {
			writeUtf8Length(charCount + 1);
			int charIndex = 0;
			if (capacity - position >= charCount) {
				// Try to write 8 bit chars.
				int position = this.position;
				for (; charIndex < charCount; charIndex++) {
					int c = value.charAt(charIndex);
					if (c > 127) break;
					niobuffer.put(position++, (byte)c);
				}
				this.position = position;
				niobuffer.position(position);
			}
			if (charIndex < charCount) writeString_slow(value, charCount, charIndex);
			niobuffer.position(position);
		}
	}

	/** Writes the length and CharSequence as UTF8, or null. The string can be read using {@link Input#readString()} or
	 * {@link Input#readStringBuilder()}.
	 * @param value May be null. */
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
				niobuffer.put(position++, (byte)c);
			}
			this.position = position;
			niobuffer.position(position);
		}
		if (charIndex < charCount) writeString_slow(value, charCount, charIndex);
		niobuffer.position(position);
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
		if (charCount == 0) {
			writeByte(1 | 0x80); // 1 means empty string, bit 8 means UTF8.
			return;
		}
		if (capacity - position < charCount)
			writeAscii_slow(value, charCount);
		else {
			byte[] tmp = value.getBytes();
			niobuffer.put(tmp, 0, tmp.length);
			position += charCount;
		}
		niobuffer.put(position - 1, (byte)(niobuffer.get(position - 1) | 0x80)); // Bit 8 means end of ASCII.
	}

	/** Writes the length of a string, which is a variable length encoded int except the first byte uses bit 8 to denote UTF8 and
	 * bit 7 to denote if another byte is present. */
	private void writeUtf8Length (int value) {
		if (value >>> 6 == 0) {
			require(1);
			niobuffer.put((byte)(value | 0x80)); // Set bit 8.
			position += 1;
		} else if (value >>> 13 == 0) {
			require(2);
			niobuffer.put((byte)(value | 0x40 | 0x80)); // Set bit 7 and 8.
			niobuffer.put((byte)(value >>> 6));
			position += 2;
		} else if (value >>> 20 == 0) {
			require(3);
			niobuffer.put((byte)(value | 0x40 | 0x80)); // Set bit 7 and 8.
			niobuffer.put((byte)((value >>> 6) | 0x80)); // Set bit 8.
			niobuffer.put((byte)(value >>> 13));
			position += 3;
		} else if (value >>> 27 == 0) {
			require(4);
			niobuffer.put((byte)(value | 0x40 | 0x80)); // Set bit 7 and 8.
			niobuffer.put((byte)((value >>> 6) | 0x80)); // Set bit 8.
			niobuffer.put((byte)((value >>> 13) | 0x80)); // Set bit 8.
			niobuffer.put((byte)(value >>> 20));
			position += 4;
		} else {
			require(5);
			niobuffer.put((byte)(value | 0x40 | 0x80)); // Set bit 7 and 8.
			niobuffer.put((byte)((value >>> 6) | 0x80)); // Set bit 8.
			niobuffer.put((byte)((value >>> 13) | 0x80)); // Set bit 8.
			niobuffer.put((byte)((value >>> 20) | 0x80)); // Set bit 8.
			niobuffer.put((byte)(value >>> 27));
			position += 5;
		}
	}

	private void writeString_slow (CharSequence value, int charCount, int charIndex) {
		for (; charIndex < charCount; charIndex++) {
			if (position == capacity) require(Math.min(capacity, charCount - charIndex));
			int c = value.charAt(charIndex);
			if (c <= 0x007F) {
				niobuffer.put(position++, (byte)c);
			} else if (c > 0x07FF) {
				niobuffer.put(position++, (byte)(0xE0 | c >> 12 & 0x0F));
				require(2);
				niobuffer.put(position++, (byte)(0x80 | c >> 6 & 0x3F));
				niobuffer.put(position++, (byte)(0x80 | c & 0x3F));
			} else {
				niobuffer.put(position++, (byte)(0xC0 | c >> 6 & 0x1F));
				require(1);
				niobuffer.put(position++, (byte)(0x80 | c & 0x3F));
			}
		}
	}

	private void writeAscii_slow (String value, int charCount) throws KryoException {
		ByteBuffer buffer = this.niobuffer;
		int charIndex = 0;
		int charsToWrite = Math.min(charCount, capacity - position);
		while (charIndex < charCount) {
			byte[] tmp = new byte[charCount];
			value.getBytes(charIndex, charIndex + charsToWrite, tmp, 0);
			buffer.put(tmp, 0, charsToWrite);
// value.getBytes(charIndex, charIndex + charsToWrite, buffer, position);
			charIndex += charsToWrite;
			position += charsToWrite;
			charsToWrite = Math.min(charCount - charIndex, capacity);
			if (require(charsToWrite)) buffer = this.niobuffer;
		}
	}

	// float

	/** Writes a 4 byte float. */
	public void writeFloat (float value) throws KryoException {
		require(4);
		niobuffer.putFloat(value);
		position += 4;
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
		niobuffer.putShort((short)value);
		position += 2;
	}

	// long

	/** Writes an 8 byte long. */
	public void writeLong (long value) throws KryoException {
		require(8);
		niobuffer.putLong(value);
		position += 8;
	}

	public int writeLong (long value, boolean optimizePositive) throws KryoException {
		if (!varIntsEnabled) {
			writeLong(value);
			return 8;
		} else
			return writeVarLong(value, optimizePositive);
	}

	public int writeVarLong (long value, boolean optimizePositive) throws KryoException {
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
			niobuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeInt(varInt);
			niobuffer.order(byteOrder);
			position -= 2;
			niobuffer.position(position);
			return 2;
		}

		varInt |= (0x80 << 8);
		varInt |= ((value & 0x7F) << 16);

		value >>>= 7;

		if (value == 0) {
			niobuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeInt(varInt);
			niobuffer.order(byteOrder);
			position -= 1;
			niobuffer.position(position);
			return 3;
		}

		varInt |= (0x80 << 16);
		varInt |= ((value & 0x7F) << 24);

		value >>>= 7;

		if (value == 0) {
			niobuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeInt(varInt);
			niobuffer.order(byteOrder);
			position -= 0;
			return 4;
		}

		varInt |= (0x80 << 24);
		long varLong = (varInt & 0xFFFFFFFFL);
		varLong |= (((long)(value & 0x7F)) << 32);

		value >>>= 7;

		if (value == 0) {
			niobuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeLong(varLong);
			niobuffer.order(byteOrder);
			position -= 3;
			niobuffer.position(position);
			return 5;
		}

		varLong |= (0x80L << 32);
		varLong |= (((long)(value & 0x7F)) << 40);

		value >>>= 7;

		if (value == 0) {
			niobuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeLong(varLong);
			niobuffer.order(byteOrder);
			position -= 2;
			niobuffer.position(position);
			return 6;
		}

		varLong |= (0x80L << 40);
		varLong |= (((long)(value & 0x7F)) << 48);

		value >>>= 7;

		if (value == 0) {
			niobuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeLong(varLong);
			niobuffer.order(byteOrder);
			position -= 1;
			niobuffer.position(position);
			return 7;
		}

		varLong |= (0x80L << 48);
		varLong |= (((long)(value & 0x7F)) << 56);

		value >>>= 7;

		if (value == 0) {
			niobuffer.order(ByteOrder.LITTLE_ENDIAN);
			writeLong(varLong);
			niobuffer.order(byteOrder);
			return 8;
		}

		varLong |= (0x80L << 56);
		niobuffer.order(ByteOrder.LITTLE_ENDIAN);
		writeLong(varLong);
		niobuffer.order(byteOrder);
		write((byte)(value));
		return 9;
	}

	/** Writes a 1-9 byte long.
	 * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
	 *           inefficient (9 bytes). */
	public int writeLongS (long value, boolean optimizePositive) throws KryoException {
		if (!optimizePositive) value = (value << 1) ^ (value >> 63);
		if (value >>> 7 == 0) {
			require(1);
			niobuffer.put((byte)value);
			position += 1;
			return 1;
		}
		if (value >>> 14 == 0) {
			require(2);
			niobuffer.put((byte)((value & 0x7F) | 0x80));
			niobuffer.put((byte)(value >>> 7));
			position += 2;
			return 2;
		}
		if (value >>> 21 == 0) {
			require(3);
			niobuffer.put((byte)((value & 0x7F) | 0x80));
			niobuffer.put((byte)(value >>> 7 | 0x80));
			niobuffer.put((byte)(value >>> 14));
			position += 3;
			return 3;
		}
		if (value >>> 28 == 0) {
			require(4);
			niobuffer.put((byte)((value & 0x7F) | 0x80));
			niobuffer.put((byte)(value >>> 7 | 0x80));
			niobuffer.put((byte)(value >>> 14 | 0x80));
			niobuffer.put((byte)(value >>> 21));
			position += 4;
			return 4;
		}
		if (value >>> 35 == 0) {
			require(5);
			niobuffer.put((byte)((value & 0x7F) | 0x80));
			niobuffer.put((byte)(value >>> 7 | 0x80));
			niobuffer.put((byte)(value >>> 14 | 0x80));
			niobuffer.put((byte)(value >>> 21 | 0x80));
			niobuffer.put((byte)(value >>> 28));
			position += 5;
			return 5;
		}
		if (value >>> 42 == 0) {
			require(6);
			niobuffer.put((byte)((value & 0x7F) | 0x80));
			niobuffer.put((byte)(value >>> 7 | 0x80));
			niobuffer.put((byte)(value >>> 14 | 0x80));
			niobuffer.put((byte)(value >>> 21 | 0x80));
			niobuffer.put((byte)(value >>> 28 | 0x80));
			niobuffer.put((byte)(value >>> 35));
			position += 6;
			return 6;
		}
		if (value >>> 49 == 0) {
			require(7);
			niobuffer.put((byte)((value & 0x7F) | 0x80));
			niobuffer.put((byte)(value >>> 7 | 0x80));
			niobuffer.put((byte)(value >>> 14 | 0x80));
			niobuffer.put((byte)(value >>> 21 | 0x80));
			niobuffer.put((byte)(value >>> 28 | 0x80));
			niobuffer.put((byte)(value >>> 35 | 0x80));
			niobuffer.put((byte)(value >>> 42));
			position += 7;
			return 7;
		}
		if (value >>> 56 == 0) {
			require(8);
			niobuffer.put((byte)((value & 0x7F) | 0x80));
			niobuffer.put((byte)(value >>> 7 | 0x80));
			niobuffer.put((byte)(value >>> 14 | 0x80));
			niobuffer.put((byte)(value >>> 21 | 0x80));
			niobuffer.put((byte)(value >>> 28 | 0x80));
			niobuffer.put((byte)(value >>> 35 | 0x80));
			niobuffer.put((byte)(value >>> 42 | 0x80));
			niobuffer.put((byte)(value >>> 49));
			position += 8;
			return 8;
		}
		require(9);
		niobuffer.put((byte)((value & 0x7F) | 0x80));
		niobuffer.put((byte)(value >>> 7 | 0x80));
		niobuffer.put((byte)(value >>> 14 | 0x80));
		niobuffer.put((byte)(value >>> 21 | 0x80));
		niobuffer.put((byte)(value >>> 28 | 0x80));
		niobuffer.put((byte)(value >>> 35 | 0x80));
		niobuffer.put((byte)(value >>> 42 | 0x80));
		niobuffer.put((byte)(value >>> 49 | 0x80));
		niobuffer.put((byte)(value >>> 56));
		position += 9;
		return 9;
	}

	// boolean

	/** Writes a 1 byte boolean. */
	public void writeBoolean (boolean value) throws KryoException {
		require(1);
		niobuffer.put((byte)(value ? 1 : 0));
		position++;
	}

	// char

	/** Writes a 2 byte char. */
	public void writeChar (char value) throws KryoException {
		require(2);
		niobuffer.putChar(value);
		position += 2;
	}

	// double

	/** Writes an 8 byte double. */
	public void writeDouble (double value) throws KryoException {
		require(8);
		niobuffer.putDouble(value);
		position += 8;
	}

	/** Writes a 1-9 byte double with reduced precision.
	 * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
	 *           inefficient (9 bytes). */
	public int writeDouble (double value, double precision, boolean optimizePositive) throws KryoException {
		return writeLong((long)(value * precision), optimizePositive);
	}

	// Methods implementing bulk operations on arrays of primitive types

	/** Bulk output of an int array. */
	public void writeInts (int[] object) throws KryoException {
		if (capacity - position >= object.length * 4 && isNativeOrder()) {
			IntBuffer buf = niobuffer.asIntBuffer();
			buf.put(object);
			position += object.length * 4;
		} else
			super.writeInts(object);
	}

	/** Bulk output of an long array. */
	public void writeLongs (long[] object) throws KryoException {
		if (capacity - position >= object.length * 8 && isNativeOrder()) {
			LongBuffer buf = niobuffer.asLongBuffer();
			buf.put(object);
			position += object.length * 8;
		} else
			super.writeLongs(object);
	}

	/** Bulk output of a float array. */
	public void writeFloats (float[] object) throws KryoException {
		if (capacity - position >= object.length * 4 && isNativeOrder()) {
			FloatBuffer buf = niobuffer.asFloatBuffer();
			buf.put(object);
			position += object.length * 4;
		} else
			super.writeFloats(object);
	}

	/** Bulk output of a short array. */
	public void writeShorts (short[] object) throws KryoException {
		if (capacity - position >= object.length * 2 && isNativeOrder()) {
			ShortBuffer buf = niobuffer.asShortBuffer();
			buf.put(object);
			position += object.length * 2;
		} else
			super.writeShorts(object);
	}

	/** Bulk output of a char array. */
	public void writeChars (char[] object) throws KryoException {
		if (capacity - position >= object.length * 2 && isNativeOrder()) {
			CharBuffer buf = niobuffer.asCharBuffer();
			buf.put(object);
			position += object.length * 2;
		} else
			super.writeChars(object);
	}

	/** Bulk output of a double array. */
	public void writeDoubles (double[] object) throws KryoException {
		if (capacity - position >= object.length * 8 && isNativeOrder()) {
			DoubleBuffer buf = niobuffer.asDoubleBuffer();
			buf.put(object);
			position += object.length * 8;
		} else
			super.writeDoubles(object);
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
