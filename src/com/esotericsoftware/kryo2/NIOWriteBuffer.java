
package com.esotericsoftware.kryo2;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.SerializationException;

public class NIOWriteBuffer extends WriteBuffer {
	private final ByteBuffer buffer;

	public NIOWriteBuffer (ByteBuffer buffer) {
		this.buffer = buffer;
	}

	public NIOWriteBuffer (ByteBuffer buffer, int size) {
		super(size);
		this.buffer = buffer;
	}

	public NIOWriteBuffer (ByteBuffer buffer, int bufferSize, int maxBuffers) {
		super(bufferSize, maxBuffers);
		this.buffer = buffer;
	}

	public NIOWriteBuffer (ByteBuffer buffer, int bufferSize, int coreBuffers, int maxBuffers) {
		super(bufferSize, coreBuffers, maxBuffers);
		this.buffer = buffer;
	}

	public void clear () {
		super.clear();
		buffer.clear();
	}

	protected boolean output (byte[] bytes, int count) {
		try {
			buffer.put(bytes, 0, count);
		} catch (BufferOverflowException ex) {
			throw new SerializationException(ex);
		}
		return true;
	}

	public ByteBuffer getByteBuffer () {
		return buffer;
	}
}
