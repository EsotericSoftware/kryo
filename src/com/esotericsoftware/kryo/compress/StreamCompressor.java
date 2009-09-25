
package com.esotericsoftware.kryo.compress;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.SerializationException;
import com.esotericsoftware.kryo.Serializer;

/**
 * Convenience class to compress and decompress using streams.
 * @author Nathan Sweet <misc@n4te.com>
 */
public abstract class StreamCompressor extends ByteArrayCompressor {
	public StreamCompressor (Serializer serializer) {
		super(serializer, 2048);
	}

	public StreamCompressor (Serializer serializer, int bufferSize) {
		super(serializer, bufferSize);
	}

	public void compress (byte[] inputBytes, int inputLength, ByteBuffer outputBuffer) {
		ByteArrayOutputStream outputBufferStream = new ByteArrayOutputStream(outputBuffer.array());
		try {
			FilterOutputStream output = getCompressionStream(outputBufferStream);
			try {
				output.write(inputBytes, 0, inputLength);
			} finally {
				output.close();
			}
			outputBuffer.position(outputBufferStream.size());
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
	}

	abstract public FilterOutputStream getCompressionStream (OutputStream output) throws IOException;

	public void decompress (byte[] inputBytes, int inputLength, ByteBuffer outputBuffer) {
		ByteArrayInputStream inputBufferStream = new ByteArrayInputStream(inputBytes);
		inputBufferStream.setCount(inputLength);
		try {
			FilterInputStream input = getDecompressionStream(inputBufferStream);
			try {
				outputBuffer.position(input.read(outputBuffer.array(), 0, outputBuffer.capacity()));
			} finally {
				input.close();
			}
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
	}

	abstract public FilterInputStream getDecompressionStream (InputStream input) throws IOException;

	static private class ByteArrayOutputStream extends java.io.ByteArrayOutputStream {
		public ByteArrayOutputStream (byte[] buf) {
			super(0);
			this.buf = buf;
		}
	}

	static private class ByteArrayInputStream extends java.io.ByteArrayInputStream {
		public ByteArrayInputStream (byte[] buf) {
			super(buf);
		}

		public void setCount (int count) {
			this.count = count;
		}
	}
}
