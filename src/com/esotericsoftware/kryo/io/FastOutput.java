
package com.esotericsoftware.kryo.io;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

import sun.misc.Unsafe;

import com.esotericsoftware.kryo.KryoException;

/** Same as Output, but does not use variable length encoding for integer types.
 * @author Roman Levenstein <romxilev@gmail.com> */
public final class FastOutput extends Output {

	/** Creates an uninitialized Output. {@link #setBuffer(byte[], int)} must be called before the Output is used. */
	public FastOutput () {
	}

	/** Creates a new Output for writing to a byte array.
	 * 
	 * @param bufferSize The initial and maximum size of the buffer. An exception is thrown if this size is exceeded. */
	public FastOutput (int bufferSize) {
		this(bufferSize, bufferSize);
	}

	/** Creates a new Output for writing to a byte array.
	 * 
	 * @param bufferSize The initial size of the buffer.
	 * @param maxBufferSize The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown. */
	public FastOutput (int bufferSize, int maxBufferSize) {
		super(bufferSize, maxBufferSize);
	}

	/** Creates a new Output for writing to a byte array.
	 * 
	 * @see #setBuffer(byte[]) */
	public FastOutput (byte[] buffer) {
		this(buffer, buffer.length);
	}

	/** Creates a new Output for writing to a byte array.
	 * 
	 * @see #setBuffer(byte[], int) */
	public FastOutput (byte[] buffer, int maxBufferSize) {
		super(buffer, maxBufferSize);
	}

	/** Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used. */
	public FastOutput (OutputStream outputStream) {
		super(outputStream);
	}

	/** Creates a new Output for writing to an OutputStream. */
	public FastOutput (OutputStream outputStream, int bufferSize) {
		super(outputStream, bufferSize);
	}

	public int writeInt (int value, boolean optimizePositive) throws KryoException {
		writeInt(value);
		return 4;
	}

	public int writeLong (long value, boolean optimizePositive) throws KryoException {
		writeLong(value);
		return 8;
	}

}
