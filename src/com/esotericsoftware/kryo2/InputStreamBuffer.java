
package com.esotericsoftware.kryo2;

import java.io.IOException;
import java.io.InputStream;

import com.esotericsoftware.kryo.SerializationException;

public class InputStreamBuffer extends ReadBuffer {
	private final InputStream input;

	public InputStreamBuffer (InputStream input) {
		this.input = input;
	}

	public InputStreamBuffer (InputStream input, int size) {
		super(size);
		this.input = input;
	}

	public InputStreamBuffer (InputStream input, int bufferSize, int maxBuffers) {
		super(bufferSize, maxBuffers);
		this.input = input;
	}

	public InputStreamBuffer (InputStream input, int bufferSize, int coreBuffers, int maxBuffers) {
		super(bufferSize, coreBuffers, maxBuffers);
		this.input = input;
	}

	protected int input (byte[] buffer) {
		try {
			return input.read(buffer);
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
	}
}
