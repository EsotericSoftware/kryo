
package com.esotericsoftware.kryo2;

import java.nio.ByteBuffer;

public class NIOReadBuffer extends ReadBuffer {
	private final ByteBuffer buffer;

	public NIOReadBuffer (ByteBuffer buffer) {
		this.buffer = buffer;
	}

	public NIOReadBuffer (ByteBuffer buffer, int size) {
		super(size);
		this.buffer = buffer;
	}

	public NIOReadBuffer (ByteBuffer buffer, int bufferSize, int maxBuffers) {
		super(bufferSize, maxBuffers);
		this.buffer = buffer;
	}

	public NIOReadBuffer (ByteBuffer buffer, int bufferSize, int coreBuffers, int maxBuffers) {
		super(bufferSize, coreBuffers, maxBuffers);
		this.buffer = buffer;
	}

	protected int input (byte[] bytes) {
		int count = Math.min(buffer.remaining(), bytes.length);
		buffer.get(bytes, 0, count);
		return count;
	}

	public ByteBuffer getByteBuffer () {
		return buffer;
	}
}
