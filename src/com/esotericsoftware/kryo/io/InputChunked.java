
package com.esotericsoftware.kryo.io;

import java.io.IOException;
import java.io.InputStream;

import com.esotericsoftware.kryo.KryoException;

public class InputChunked extends Input {
	private int chunkSize = -1;

	public InputChunked () {
		super(1024);
	}

	public InputChunked (int bufferSize) {
		super(bufferSize);
	}

	public InputChunked (InputStream inputStream) {
		super(inputStream);
	}

	public InputChunked (InputStream inputStream, int bufferSize) {
		super(inputStream, bufferSize);
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
					return;
				}
			}
		} catch (IOException ex) {
			throw new KryoException(ex);
		}
		throw new KryoException("Malformed integer.");
	}

	public void nextChunks () {
		if (chunkSize == -1) readChunkSize(); // No current chunk, expect a new chunk.
		while (chunkSize > 0)
			skip(chunkSize);
		chunkSize = -1;
	}
}
