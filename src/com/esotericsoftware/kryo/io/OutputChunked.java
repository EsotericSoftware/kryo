
package com.esotericsoftware.kryo.io;

import java.io.IOException;
import java.io.OutputStream;

import com.esotericsoftware.kryo.KryoException;

public class OutputChunked extends Output {
	public OutputChunked () {
		super(1024);
	}

	public OutputChunked (int bufferSize) {
		super(bufferSize);
	}

	public OutputChunked (OutputStream outputStream) {
		super(outputStream);
	}

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

	public void endChunks () {
		flush(); // Flush any partial chunk.
		try {
			getOutputStream().write(0); // Zero length chunk.
		} catch (IOException ex) {
			throw new KryoException(ex);
		}
	}
}
