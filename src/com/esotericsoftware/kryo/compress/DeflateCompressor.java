
package com.esotericsoftware.kryo.compress;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.esotericsoftware.kryo.SerializationException;
import com.esotericsoftware.kryo.Serializer;

/**
 * Compresses and decompresses using the "deflate" algorithm.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class DeflateCompressor extends ByteArrayCompressor {
	private Deflater deflater;
	private Inflater inflater;

	public DeflateCompressor (Serializer serializer) {
		this(serializer, 2048);
	}

	public DeflateCompressor (Serializer serializer, int bufferSize) {
		super(serializer, bufferSize);
		this.deflater = new Deflater();
		this.inflater = new Inflater();
	}

	public void compress (byte[] inputBytes, int inputLength, ByteBuffer outputBuffer) {
		deflater.reset();
		deflater.setInput(inputBytes, 0, inputLength);
		deflater.finish();
		outputBuffer.position(deflater.deflate(outputBuffer.array()));
	}

	public void decompress (byte[] inputBytes, int inputLength, ByteBuffer outputBuffer) {
		inflater.reset();
		inflater.setInput(inputBytes, 0, inputLength);
		try {
			outputBuffer.position(inflater.inflate(outputBuffer.array()));
		} catch (DataFormatException ex) {
			throw new SerializationException("Error inflating data.", ex);
		}
	}
}
