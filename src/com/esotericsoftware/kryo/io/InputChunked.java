
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
