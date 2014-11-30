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
