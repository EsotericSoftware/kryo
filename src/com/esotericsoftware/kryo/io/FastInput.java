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

import java.io.InputStream;

import com.esotericsoftware.kryo.KryoException;

/** An {@link Input} that does not use variable length encoding for int or long, which can be faster for some data.
 * @author Roman Levenstein <romxilev@gmail.com> */
public final class FastInput extends Input {
	/** @see Input#Input() */
	public FastInput () {
	}

	/** @see Input#Input(int) */
	public FastInput (int bufferSize) {
		super(bufferSize);
	}

	/** @see Input#Input(byte[]) */
	public FastInput (byte[] buffer) {
		super(buffer);
	}

	/** @see Input#Input(byte[], int, int) */
	public FastInput (byte[] buffer, int offset, int count) {
		super(buffer, offset, count);
	}

	/** @see Input#Input(InputStream) */
	public FastInput (InputStream outputStream) {
		super(outputStream);
	}

	/** @see Input#Input(InputStream, int) */
	public FastInput (InputStream outputStream, int bufferSize) {
		super(outputStream, bufferSize);
	}

	public int readVarInt (boolean optimizePositive) throws KryoException {
		return readInt();
	}

	public boolean canReadVarInt () throws KryoException {
		return limit - position >= 4;
	}

	public long readVarLong (boolean optimizePositive) throws KryoException {
		return readLong();
	}

	public boolean canReadVarLong () throws KryoException {
		return limit - position >= 8;
	}
}
