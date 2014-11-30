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
import java.io.OutputStream;

import com.esotericsoftware.kryo.KryoException;

import static com.esotericsoftware.minlog.Log.*;

/** An OutputStream that buffers data in a byte array and flushes to another OutputStream, writing the length before each flush.
 * The length allows the chunks to be skipped when reading.
 * @author Nathan Sweet <misc@n4te.com> */
public class OutputChunked extends Output {
	/** Creates an uninitialized OutputChunked with a maximum chunk size of 2048. The OutputStream must be set before it can be
	 * used. */
	public OutputChunked () {
		super(2048);
	}

	/** Creates an uninitialized OutputChunked. The OutputStream must be set before it can be used.
	 * @param bufferSize The maximum size of a chunk. */
	public OutputChunked (int bufferSize) {
		super(bufferSize);
	}

	/** Creates an OutputChunked with a maximum chunk size of 2048. */
	public OutputChunked (OutputStream outputStream) {
		super(outputStream, 2048);
	}

	/** @param bufferSize The maximum size of a chunk. */
	public OutputChunked (OutputStream outputStream, int bufferSize) {
		super(outputStream, bufferSize);
	}

	public void flush () throws KryoException {
		if (position() > 0) {
			try {
				writeChunkSize();
			} catch (IOException ex) {
				throw new KryoException(ex);
			}
		}
		super.flush();
	}

	private void writeChunkSize () throws IOException {
		int size = position();
		if (TRACE) trace("kryo", "Write chunk: " + size);
		OutputStream outputStream = getOutputStream();
		if ((size & ~0x7F) == 0) {
			outputStream.write(size);
			return;
		}
		outputStream.write((size & 0x7F) | 0x80);
		size >>>= 7;
		if ((size & ~0x7F) == 0) {
			outputStream.write(size);
			return;
		}
		outputStream.write((size & 0x7F) | 0x80);
		size >>>= 7;
		if ((size & ~0x7F) == 0) {
			outputStream.write(size);
			return;
		}
		outputStream.write((size & 0x7F) | 0x80);
		size >>>= 7;
		if ((size & ~0x7F) == 0) {
			outputStream.write(size);
			return;
		}
		outputStream.write((size & 0x7F) | 0x80);
		size >>>= 7;
		outputStream.write(size);
	}

	/** Marks the end of some data that may have been written by any number of chunks. These chunks can then be skipped when
	 * reading. */
	public void endChunks () {
		flush(); // Flush any partial chunk.
		if (TRACE) trace("kryo", "End chunks.");
		try {
			getOutputStream().write(0); // Zero length chunk.
		} catch (IOException ex) {
			throw new KryoException(ex);
		}
	}
}
