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

import com.esotericsoftware.kryo.KryoException;

import static com.esotericsoftware.minlog.Log.*;

/** An InputStream that reads lengths and chunks of data from another OutputStream, allowing chunks to be skipped.
 * @author Nathan Sweet <misc@n4te.com> */
public class InputChunked extends Input {
	private int chunkSize = -1;

	/** Creates an uninitialized InputChunked with a buffer size of 2048. The InputStream must be set before it can be used. */
	public InputChunked () {
		super(2048);
	}

	/** Creates an uninitialized InputChunked. The InputStream must be set before it can be used. */
	public InputChunked (int bufferSize) {
		super(bufferSize);
	}

	/** Creates an InputChunked with a buffer size of 2048. */
	public InputChunked (InputStream inputStream) {
		super(inputStream, 2048);
	}

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

	public void rewind () {
		super.rewind();
		chunkSize = -1;
	}

	protected int fill (byte[] buffer, int offset, int count) throws KryoException {
		if (chunkSize == -1) // No current chunk, expect a new chunk.
			readChunkSize();
		else if (chunkSize == 0) // End of chunks.
			return -1;
		int actual = super.fill(buffer, offset, Math.min(chunkSize, count));
		chunkSize -= actual;
		if (chunkSize == 0) readChunkSize(); // Read next chunk size.
		return actual;
	}

	private void readChunkSize () {
		try {
			InputStream inputStream = getInputStream();
			for (int offset = 0, result = 0; offset < 32; offset += 7) {
				int b = inputStream.read();
				if (b == -1) throw new KryoException("Buffer underflow.");
				result |= (b & 0x7F) << offset;
				if ((b & 0x80) == 0) {
					chunkSize = result;
					if (TRACE) trace("kryo", "Read chunk: " + chunkSize);
					return;
				}
			}
		} catch (IOException ex) {
			throw new KryoException(ex);
		}
		throw new KryoException("Malformed integer.");
	}

	/** Advances the stream to the next set of chunks. InputChunked will appear to hit the end of the data until this method is
	 * called. */
	public void nextChunks () {
		if (chunkSize == -1) readChunkSize(); // No current chunk, expect a new chunk.
		while (chunkSize > 0)
			skip(chunkSize);
		chunkSize = -1;
		if (TRACE) trace("kryo", "Next chunks.");
	}
}
