
package com.esotericsoftware.kryo.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/** An InputStream whose source is a {@link ByteBuffer}. */
public class ByteBufferInputStream extends InputStream {
	private ByteBuffer byteBuffer;

	public ByteBufferInputStream () {
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
		return byteBuffer.get();
	}

	public int read (byte[] bytes, int offset, int length) throws IOException {
		int count = Math.min(byteBuffer.remaining(), length);
		if (count == 0) return -1;
		byteBuffer.get(bytes, offset, length);
		return count;
	}
}
