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

import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/** An InputStream whose source is a {@link ByteBuffer}.
 * @author Nathan Sweet */
public class ByteBufferInputStream extends InputStream {
	private ByteBuffer byteBuffer;

	/** Creates an uninitialized stream that cannot be used until {@link #setByteBuffer(ByteBuffer)} is called. */
	public ByteBufferInputStream () {
	}

	/** Creates a stream with a new non-direct buffer of the specified size. The position and limit of the buffer is zero. */
	public ByteBufferInputStream (int bufferSize) {
		this(ByteBuffer.allocate(bufferSize));
		flipBuffer(byteBuffer);
	}

	public ByteBufferInputStream (ByteBuffer byteBuffer) {
		this.byteBuffer = byteBuffer;
	}

	public ByteBuffer getByteBuffer () {
		return byteBuffer;
	}

	public void setByteBuffer (ByteBuffer byteBuffer) {
		this.byteBuffer = byteBuffer;
	}

	public int read () throws IOException {
		if (!byteBuffer.hasRemaining()) return -1;
		return byteBuffer.get() & 0xFF;
	}

	public int read (byte[] bytes, int offset, int length) throws IOException {
		if (length == 0) return 0;
		int count = Math.min(byteBuffer.remaining(), length);
		if (count == 0) return -1;
		byteBuffer.get(bytes, offset, count);
		return count;
	}

	public int available () throws IOException {
		return byteBuffer.remaining();
	}

	private void flipBuffer (Buffer buffer) {
		buffer.flip();
	}

}
