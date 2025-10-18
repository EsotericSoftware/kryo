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

import static com.esotericsoftware.minlog.Log.*;

import com.esotericsoftware.kryo.KryoException;

import java.io.IOException;
import java.io.InputStream;

/** An {@link Input} that reads lengths and chunks of data from another OutputStream, allowing chunks to be skipped.
 * @author Nathan Sweet */
public class InputChunked extends Input {
	private int chunkSize = -1;

	/** @see Input#Input() */
	public InputChunked () {
		super();
	}

	/** @see Input#Input(int) */
	public InputChunked (int bufferSize) {
		super(bufferSize);
	}

	/** @see Input#Input(InputStream) */
	public InputChunked (InputStream inputStream) {
		super(inputStream);
	}

	/** @see Input#Input(InputStream, int) */
	public InputChunked (InputStream inputStream, int bufferSize) {
		super(inputStream, bufferSize);
	}

	public void setInputStream (InputStream inputStream) {
		super.setInputStream(inputStream);
		chunkSize = -1;
	}

	public void setBuffer (byte[] bytes, int offset, int count) {
		super.setBuffer(bytes, offset, count);
		chunkSize = -1;
	}

	public void reset () {
		super.reset();
		chunkSize = -1;
	}

	protected int fill (byte[] buffer, int offset, int count) throws KryoException {
		if (chunkSize == -1) { // No current chunk, expect a new chunk.
			if (!readChunkSize()) return -1;
		} else if (chunkSize == 0) // End of chunk.
			return -1;
		int actual = super.fill(buffer, offset, Math.min(chunkSize, count));
		chunkSize -= actual;
		if (chunkSize == 0 && !readChunkSize()) return -1;
		return actual;
	}

	/** @return false if the end of the stream was reached. */
	private boolean readChunkSize () {
		try {
			InputStream inputStream = getInputStream();
			for (int offset = 0, result = 0; offset < 32; offset += 7) {
				int b = inputStream.read();
				if (b == -1) return false;
				result |= (b & 0x7F) << offset;
				if ((b & 0x80) == 0) {
					chunkSize = result;
					if (TRACE && chunkSize > 0) trace("kryo", "Read chunk: " + chunkSize);
					return true;
				}
			}
		} catch (IOException ex) {
			throw new KryoException("Unable to read chunk size.", ex);
		}
		throw new KryoException("Unable to read chunk size: malformed integer");
	}

	/** Advances the stream to the next chunk. InputChunked will appear to hit the end of the data until this method is called. */
	public void nextChunk () {
		position = limit; // Underflow resets the position to 0. Ensure we are at the end of the chunk.
		if (chunkSize == -1) readChunkSize(); // No current chunk, expect a new chunk.
		while (chunkSize > 0)
			skip(chunkSize);
		chunkSize = -1;
		if (TRACE) trace("kryo", "Next chunk.");
	}
}
