
package com.esotericsoftware.kryo.compress;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Compressor;
import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

/**
 * Convenience class to compress and decompress using byte arrays.
 * @author Nathan Sweet <misc@n4te.com>
 */
public abstract class ByteArrayCompressor extends Compressor {
	public ByteArrayCompressor (Serializer serializer) {
		super(serializer);
	}

	public ByteArrayCompressor (Serializer serializer, int bufferSize) {
		super(serializer, bufferSize);
	}

	public void compress (ByteBuffer inputBuffer, Object object, ByteBuffer outputBuffer) {
		Context context = Kryo.getContext();
		byte[] inputBytes = context.getBuffer(bufferSize).array();
		int inputLength = inputBuffer.remaining();
		inputBuffer.get(inputBytes, 0, inputLength);
		compress(inputBytes, inputLength, outputBuffer);
	}

	/**
	 * Implementations should read the specified number of input bytes and write compressed data to the output buffer.
	 * @param outputBuffer A non-direct buffer.
	 */
	abstract public void compress (byte[] inputBytes, int inputLength, ByteBuffer outputBuffer);

	public void decompress (ByteBuffer inputBuffer, Class type, ByteBuffer outputBuffer) {
		Context context = Kryo.getContext();
		byte[] inputBytes = context.getBuffer(bufferSize).array();
		int inputLength = inputBuffer.remaining();
		inputBuffer.get(inputBytes, 0, inputLength);
		decompress(inputBytes, inputLength, outputBuffer);
	}

	/**
	 * Implementations should read the specified number of input bytes and write decompressed data to the output bytes.
	 * @param outputBuffer A non-direct buffer.
	 */
	abstract public void decompress (byte[] inputBytes, int inputLength, ByteBuffer outputBuffer);
}
