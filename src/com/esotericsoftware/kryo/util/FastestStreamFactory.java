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

package com.esotericsoftware.kryo.util;

import java.io.InputStream;
import java.io.OutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.StreamFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.esotericsoftware.kryo.io.UnsafeOutput;

/**
 * This StreamFactory tries to provide fastest possible Input/Output streams on a given platform.
 * It may return sun.misc.Unsafe based implementations of streams, which are
 * very fast, but not portable across platforms.
 * 
 * @author Roman Levenstein <romixlev@gmail.com>
 *
 */
public class FastestStreamFactory implements StreamFactory {
	
	static private boolean isUnsafe = UnsafeUtil.unsafe() != null;

	@Override
	public Input getInput() {
		return (isUnsafe)? new UnsafeInput() : new Input();
	}

	@Override
	public Input getInput(int bufferSize) {
		return (isUnsafe)? new UnsafeInput(bufferSize) : new Input(bufferSize);
	}

	@Override
	public Input getInput(byte[] buffer) {
		return (isUnsafe)? new UnsafeInput(buffer) : new Input(buffer);
	}

	@Override
	public Input getInput(byte[] buffer, int offset, int count) {
		return (isUnsafe)? new UnsafeInput(buffer, offset, count) : new Input(buffer, offset, count);
	}

	@Override
	public Input getInput(InputStream inputStream) {
		return (isUnsafe)? new UnsafeInput(inputStream) : new Input(inputStream);
	}

	@Override
	public Input getInput(InputStream inputStream, int bufferSize) {
		return (isUnsafe)? new UnsafeInput(inputStream, bufferSize) : new Input(inputStream, bufferSize);
	}

	@Override
	public Output getOutput() {
		return (isUnsafe)? new UnsafeOutput() : new Output();
	}

	@Override
	public Output getOutput(int bufferSize) {
		return (isUnsafe)? new UnsafeOutput(bufferSize) : new Output(bufferSize);
	}

	@Override
	public Output getOutput(int bufferSize, int maxBufferSize) {
		return (isUnsafe)? new UnsafeOutput(bufferSize, maxBufferSize) : new Output(bufferSize, maxBufferSize);
	}

	@Override
	public Output getOutput(byte[] buffer) {
		return (isUnsafe)? new UnsafeOutput(buffer) : new Output(buffer);
	}

	@Override
	public Output getOutput(byte[] buffer, int maxBufferSize) {
		return (isUnsafe)? new UnsafeOutput(buffer, maxBufferSize) : new Output(buffer, maxBufferSize);
	}

	@Override
	public Output getOutput(OutputStream outputStream) {
		return (isUnsafe)? new UnsafeOutput(outputStream) : new Output(outputStream);
	}

	@Override
	public Output getOutput(OutputStream outputStream, int bufferSize) {
		return (isUnsafe)? new UnsafeOutput(outputStream, bufferSize) : new Output(outputStream, bufferSize);
	}

	@Override
	public void setKryo(Kryo kryo) {
		// Only use Unsafe-based streams if this Kryo instance supports it
		//isUnsafe = UnsafeUtil.unsafe() != null && kryo.getUnsafe();
	}

}
