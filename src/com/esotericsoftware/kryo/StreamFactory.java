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

package com.esotericsoftware.kryo;

import java.io.InputStream;
import java.io.OutputStream;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Provides input and output streams based on system settings.
 * @author Roman Levenstein <romixlev@gmail.com> */
public interface StreamFactory {

	/** Creates an uninitialized Input. */
	public Input getInput ();

	/** Creates a new Input for reading from a byte array.
	 * @param bufferSize The size of the buffer. An exception is thrown if more bytes than this are read. */
	public Input getInput(int bufferSize);

	/** Creates a new Input for reading from a byte array.
	 * @param buffer An exception is thrown if more bytes than this are read. */
	public Input getInput(byte[] buffer) ;

	/** Creates a new Input for reading from a byte array.
	 * @param buffer An exception is thrown if more bytes than this are read. */
	public Input getInput(byte[] buffer, int offset, int count) ;

	/** Creates a new Input for reading from an InputStream with a buffer size of 4096. */
	public Input getInput(InputStream inputStream);
	
	/** Creates a new Input for reading from an InputStream. */
	public Input getInput(InputStream inputStream, int bufferSize);
	
	/** Creates an uninitialized Output. {@link Output#setBuffer(byte[], int)} must be called before the Output is used. */
	public Output getOutput();

	/** Creates a new Output for writing to a byte array.
	 * @param bufferSize The initial and maximum size of the buffer. An exception is thrown if this size is exceeded. */
	public Output getOutput(int bufferSize);

	/** Creates a new Output for writing to a byte array.
	 * @param bufferSize The initial size of the buffer.
	 * @param maxBufferSize The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown. Can be -1
	 *           for no maximum. */
	public Output getOutput(int bufferSize, int maxBufferSize);
	
	/** Creates a new Output for writing to a byte array.
	 * @see Output#setBuffer(byte[]) */
	public Output getOutput(byte[] buffer);

	/** Creates a new Output for writing to a byte array.
	 * @see Output#setBuffer(byte[], int) */
	public Output getOutput(byte[] buffer, int maxBufferSize);

	/** Creates a new Output for writing to an OutputStream. A buffer size of 4096 is used. */
	public Output getOutput(OutputStream outputStream);
	
	/** Creates a new Output for writing to an OutputStream. */
	public Output getOutput(OutputStream outputStream, int bufferSize);

	public void setKryo(Kryo kryo);
	
}
