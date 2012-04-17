
package com.esotericsoftware.kryo.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/** An OutputStream whose target is a {@link ByteBuffer}. If bytes would be written that would overflow the buffer,
 * {@link #flush()} is called. Subclasses can override flush to empty the buffer.
 * @author Nathan Sweet <misc@n4te.com> */
public class ByteBufferOutputStream extends OutputStream {
	private ByteBuffer byteBuffer;

	/** Creates an uninitialized stream that cannot be used until {@link #setByteBuffer(ByteBuffer)} is called. */
	public ByteBufferOutputStream () {
	}

	/** Creates a stream with a new non-direct buffer of the specified size. */
	public ByteBufferOutputStream (int bufferSize) {
		this(ByteBuffer.allocate(bufferSize));
	}

	public ByteBufferOutputStream (ByteBuffer byteBuffer) {
		this.byteBuffer = byteBuffer;
	}

	public ByteBuffer getByteBuffer () {
		return byteBuffer;
	}

	public void setByteBuffer (ByteBuffer byteBuffer) {
		this.byteBuffer = byteBuffer;
	}

	public void write (int b) throws IOException {
		if (!byteBuffer.hasRemaining()) flush();
		byteBuffer.put((byte)b);
	}

	public void write (byte[] bytes, int offset, int length) throws IOException {
		if (byteBuffer.remaining() < length) flush();
		byteBuffer.put(bytes, offset, length);
	}
}
