
package com.esotericsoftware.kryo2;

import java.io.IOException;
import java.io.OutputStream;

import com.esotericsoftware.kryo.SerializationException;

public class OutputStreamBuffer extends WriteBuffer {
	private final OutputStream output;

	public OutputStreamBuffer (OutputStream output) {
		this.output = output;
	}

	public OutputStreamBuffer (OutputStream output, int size) {
		super(size);
		this.output = output;
	}

	public OutputStreamBuffer (OutputStream output, int bufferSize, int maxBuffers) {
		super(bufferSize, maxBuffers);
		this.output = output;
	}

	public OutputStreamBuffer (OutputStream output, int bufferSize, int coreBuffers, int maxBuffers) {
		super(bufferSize, coreBuffers, maxBuffers);
		this.output = output;
	}

	protected boolean output (byte[] bytes, int count) {
		try {
			output.write(bytes, 0, count);
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
		return true;
	}

	public void close () {
		flush();
		try {
			output.close();
		} catch (IOException ignored) {
		}
	}

	public OutputStream getOutputStream () {
		return output;
	}
}
