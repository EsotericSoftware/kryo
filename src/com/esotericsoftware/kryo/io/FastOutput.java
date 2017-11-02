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

import java.io.OutputStream;

import com.esotericsoftware.kryo.KryoException;

/** An {@link Output} that does not use variable length encoding for int or long, which can be faster for some data at the cost of
 * a larger serialized size.
 * @author Roman Levenstein <romxilev@gmail.com> */
public final class FastOutput extends Output {
	/** @see Output#Output() */
	public FastOutput () {
	}

	/** @see Output#Output(int) */
	public FastOutput (int bufferSize) {
		this(bufferSize, bufferSize);
	}

	/** @see Output#Output(int, int) */
	public FastOutput (int bufferSize, int maxBufferSize) {
		super(bufferSize, maxBufferSize);
	}

	/** @see Output#Output(byte[]) */
	public FastOutput (byte[] buffer) {
		this(buffer, buffer.length);
	}

	/** @see Output#Output(byte[], int) */
	public FastOutput (byte[] buffer, int maxBufferSize) {
		super(buffer, maxBufferSize);
	}

	/** @see Output#Output(OutputStream) */
	public FastOutput (OutputStream outputStream) {
		super(outputStream);
	}

	/** @see Output#Output(OutputStream, int) */
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
