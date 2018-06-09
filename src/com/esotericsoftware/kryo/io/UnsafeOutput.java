/* Copyright (c) 2008, Nathan Sweet
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

import static com.esotericsoftware.kryo.util.UnsafeUtil.*;

import java.io.OutputStream;
import java.nio.ByteOrder;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.util.Util;

/** An optimized OutputStream that buffers data in a byte array and optionally flushes to another OutputStream. Utility methods
 * are provided for efficiently writing primitive types, arrays of primitive types and strings. It uses @link{sun.misc.Unsafe} to
 * achieve a very good performance.
 * 
 * <p>
 * Important notes:<br/>
 * <li>This class increases performance, but may result in bigger size of serialized representation.</li>
 * <li>Bulk operations, e.g. on arrays of primitive types, are always using native byte order.</li>
 * <li>Fixed-size char, int, long, short, float and double elements are always written using native byte order.</li>
 * <li>Best performance is achieved if no variable length encoding for integers is used.</li>
 * <li>Output serialized using this class should always be deserilized using @link{UnsafeInput}</li>
 * 
 * </p>
 * @author Roman Levenstein <romixlev@gmail.com> */
public final class UnsafeOutput extends Output {

	/** If set, variable length encoding will be set for integer types if it is required */
	private boolean supportVarInts = false;

	private static final boolean isLittleEndian = ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN);

	/** Creates an uninitialized Output. {@link #setBuffer(byte[], int)} must be called before the Output is used. */
	public UnsafeOutput () {
	}

	/** Creates a new Output for writing to a byte array.
	 * 
	 * @param bufferSize The initial and maximum size of the buffer. An exception is thrown if this size is exceeded. */
	public UnsafeOutput (int bufferSize) {
		this(bufferSize, bufferSize);
	}

	/** Creates a new Output for writing to a byte array.
	 * 
	 * @param bufferSize The initial size of the buffer.
	 * @param maxBufferSize The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown. */
	public UnsafeOutput (int bufferSize, int maxBufferSize) {
		super(bufferSize, maxBufferSize);
	}

	/** Creates a new Output for writing to a byte array.
	 * 
	 * @see #setBuffer(byte[]) */
	public UnsafeOutput (byte[] buffer) {
		this(buffer, buffer.length);
	}

	/** Creates a new Output for writing to a byte array.
	 * 
	 * @see #setBuffer(byte[], int) */
	public UnsafeOutput (byte[] buffer, int maxBufferSize) {
		super(buffer, maxBufferSize);
	}

	/** Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used. */
	public UnsafeOutput (OutputStream outputStream) {
		super(outputStream);
	}

	/** Creates a new Output for writing to an OutputStream. */
	public UnsafeOutput (OutputStream outputStream, int bufferSize) {
		super(outputStream, bufferSize);
	}

	/** Writes a single byte. */
	final public void writeByte (byte value) throws KryoException {
		require(1);
		unsafe().putByte(buffer, byteArrayBaseOffset + position, value);
		position++;
	}

	final public void writeByte (int value) throws KryoException {
		require(1);
		unsafe().putByte(buffer, byteArrayBaseOffset + position, (byte)value);
		position++;
	}

	/** Writes a 1 byte boolean.*/
	final public void writeBoolean (boolean value) throws KryoException {
		require(1);
		unsafe().putBoolean(buffer, byteArrayBaseOffset + position, value);
		position++;
	}

	/** Writes a 4 byte int. */
	final public void writeInt (int value) throws KryoException {
		require(4);
		unsafe().putInt(buffer, byteArrayBaseOffset + position, value);
		position += 4;
	}

	final private void writeLittleEndianInt (int val) {
		if (isLittleEndian)
			writeInt(val);
		else
			writeInt(Util.swapInt(val));
	}

	/** Writes a 4 byte float. */
	final public void writeFloat (float value) throws KryoException {
		require(4);
		unsafe().putFloat(buffer, byteArrayBaseOffset + position, value);
		position += 4;
	}

	/** Writes a 2 byte short. */
	final public void writeShort (int value) throws KryoException {
		require(2);
		unsafe().putShort(buffer, byteArrayBaseOffset + position, (short)value);
		position += 2;
	}

	/** Writes an 8 byte long. */
	final public void writeLong (long value) throws KryoException {
		require(8);
		unsafe().putLong(buffer, byteArrayBaseOffset + position, value);
		position += 8;
	}

	final private void writeLittleEndianLong (long val) {
		if (isLittleEndian)
			writeLong(val);
		else
			writeLong(Util.swapLong(val));
	}

	/** Writes an 8 byte double. */
	final public void writeDouble (double value) throws KryoException {
		require(8);
		unsafe().putDouble(buffer, byteArrayBaseOffset + position, value);
		position += 8;
	}

	/** Writes a 2 byte char. */
	final public void writeChar (char value) throws KryoException {
		require(2);
		unsafe().putChar(buffer, byteArrayBaseOffset + position, value);
		position += 2;
	}

	final public int writeInt (int value, boolean optimizePositive) throws KryoException {
		if (!supportVarInts) {
			writeInt(value);
			return 4;
		} else
			return writeVarInt(value, optimizePositive);
	}

	final public int writeLong (long value, boolean optimizePositive) throws KryoException {
		if (!supportVarInts) {
			writeLong(value);
			return 8;
		} else
			return writeVarLong(value, optimizePositive);
	}

	final public int writeVarInt (int val, boolean optimizePositive) throws KryoException {
		int value = val;
		if (!optimizePositive) value = (value << 1) ^ (value >> 31);
		int varInt = 0;

		varInt = (value & 0x7F);

		value >>>= 7;

		if (value == 0) {
			write(varInt);
			return 1;
		}

		varInt |= 0x80;
		varInt |= ((value & 0x7F) << 8);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianInt(varInt);
			position -= 2;
			return 2;
		}

		varInt |= 0x80 << 8;
		varInt |= ((value & 0x7F) << 16);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianInt(varInt);
			position -= 1;
			return 3;
		}

		varInt |= 0x80 << 16;
		varInt |= ((value & 0x7F) << 24);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianInt(varInt);
			position -= 0;
			return 4;
		}

		long varLong = varInt;
		varLong |= (0x80L << 24);
		varLong |= ((long)(value & 0x7F) << 32);
		varLong &= 0xFFFFFFFFFL;
		writeLittleEndianLong(varLong);
		position -= 3;
		return 5;
	}

	// TODO: Make it work on little and big endian machines
	final public int writeVarLong (long value, boolean optimizePositive) throws KryoException {
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
			writeLittleEndianInt(varInt);
			position -= 2;
			return 2;
		}

		varInt |= (0x80 << 8);
		varInt |= ((value & 0x7F) << 16);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianInt(varInt);
			position -= 1;
			return 3;
		}

		varInt |= (0x80 << 16);
		varInt |= ((value & 0x7F) << 24);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianInt(varInt);
			position -= 0;
			return 4;
		}

		long varLong = (varInt & 0xFFFFFFFFL) | (0x80L << 24);
		varLong |= ((value & 0x7F) << 32);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianLong(varLong);
			position -= 3;
			return 5;
		}

		varLong |= (0x80L << 32);
		varLong |= ((value & 0x7F) << 40);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianLong(varLong);
			position -= 2;
			return 6;
		}

		varLong |= (0x80L << 40);
		varLong |= ((value & 0x7F) << 48);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianLong(varLong);
			position -= 1;
			return 7;
		}

		varLong |= (0x80L << 48);
		varLong |= ((value & 0x7F) << 56);

		value >>>= 7;

		if (value == 0) {
			writeLittleEndianLong(varLong);
			return 8;
		}

		varLong |= (0x80L << 56);
		writeLittleEndianLong(varLong);
		write((int)(value & 0xFF));
		return 9;
	}

	// Methods implementing bulk operations on arrays of primitive types

	final public void writeInts (int[] object, boolean optimizePositive) throws KryoException {
		if (!supportVarInts) {
			int bytesToCopy = object.length << 2;
			writeBytes(object, intArrayBaseOffset, 0, bytesToCopy);
		} else
			super.writeInts(object, optimizePositive);
	}

	final public void writeLongs (long[] object, boolean optimizePositive) throws KryoException {
		if (!supportVarInts) {
			int bytesToCopy = object.length << 3;
			writeBytes(object, longArrayBaseOffset, 0, bytesToCopy);
		} else
			super.writeLongs(object, optimizePositive);
	}

	final public void writeInts (int[] object) throws KryoException {
		int bytesToCopy = object.length << 2;
		writeBytes(object, intArrayBaseOffset, 0, bytesToCopy);
	}

	final public void writeLongs (long[] object) throws KryoException {
		int bytesToCopy = object.length << 3;
		writeBytes(object, longArrayBaseOffset, 0, bytesToCopy);
	}

	final public void writeFloats (float[] object) throws KryoException {
		int bytesToCopy = object.length << 2;
		writeBytes(object, floatArrayBaseOffset, 0, bytesToCopy);
	}

	final public void writeShorts (short[] object) throws KryoException {
		int bytesToCopy = object.length << 1;
		writeBytes(object, shortArrayBaseOffset, 0, bytesToCopy);
	}

	final public void writeChars (char[] object) throws KryoException {
		int bytesToCopy = object.length << 1;
		writeBytes(object, charArrayBaseOffset, 0, bytesToCopy);
	}

	final public void writeDoubles (double[] object) throws KryoException {
		int bytesToCopy = object.length << 3;
		writeBytes(object, doubleArrayBaseOffset, 0, bytesToCopy);
	}

	/*** Output count bytes from a memory region starting at the given #{offset} inside the in-memory representation of obj object.
	 * @param obj
	 * @param offset
	 * @param count */
	final public void writeBytes (Object obj, long offset, long count) throws KryoException {
		writeBytes(obj, 0, offset, count);
	}

	final private void writeBytes (Object srcArray, long srcArrayTypeOffset, long srcOffset, long count) throws KryoException {
		int copyCount = Math.min(capacity - position, (int)count);

		while (true) {
			unsafe().copyMemory(srcArray, srcArrayTypeOffset + srcOffset, buffer, byteArrayBaseOffset + position, copyCount);
			position += copyCount;
			count -= copyCount;
			if (count == 0) return;
			srcOffset += copyCount;
			copyCount = Math.min(capacity, (int)count);
			require(copyCount);
		}
	}

	/*** Return current setting for variable length encoding of integers
	 * @return current setting for variable length encoding of integers */
	public boolean supportVarInts () {
		return supportVarInts;
	}

	/*** Controls if a variable length encoding for integer types should be used when serializers suggest it.
	 * 
	 * @param supportVarInts */
	public void supportVarInts (boolean supportVarInts) {
		this.supportVarInts = supportVarInts;
	}
}
