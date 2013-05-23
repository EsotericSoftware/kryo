
package com.esotericsoftware.kryo.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import sun.misc.Unsafe;

import com.esotericsoftware.kryo.KryoException;

/** Same as Input, but does not use variable length encoding for integer types.
 * @author Roman Levenstein <romxilev@gmail.com> */
public final class FastInput extends Input {

	/** Creates an uninitialized Output. {@link #setBuffer(byte[], int, int)} must be called before the Output is used. */
	public FastInput () {
	}

	/** Creates a new Output for writing to a byte array.
	 * @param bufferSize The initial and maximum size of the buffer. An exception is thrown if this size is exceeded. */
	public FastInput (int bufferSize) {
		super(bufferSize);
	}

	/** Creates a new Output for writing to a byte array.
	 * @see #setBuffer(byte[]) */
	public FastInput (byte[] buffer) {
		super(buffer);
	}

	/** Creates a new Output for writing to a byte array.
	 * @see #setBuffer(byte[], int, int) */
	public FastInput (byte[] buffer, int offset, int count) {
		super(buffer, offset, count);
	}

	/** Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used. */
	public FastInput (InputStream outputStream) {
		super(outputStream);
	}

	/** Creates a new Output for writing to an OutputStream. */
	public FastInput (InputStream outputStream, int bufferSize) {
		super(outputStream, bufferSize);
	}

	public int readInt (boolean optimizePositive) throws KryoException {
		return readInt();
	}

	public long readLong (boolean optimizePositive) throws KryoException {
		return readLong();
	}
}
