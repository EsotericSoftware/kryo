
package com.esotericsoftware.kryo;

import static com.esotericsoftware.minlog.Log.*;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

// BOZO - Grow the buffer size automatically?

/**
 * Wraps another serializer in order to modify the bytes after they are serialized and before they are deserialized.
 * @see Kryo#register(Class, Serializer)
 * @author Nathan Sweet <misc@n4te.com>
 */
abstract public class Compressor extends Serializer {
	private Serializer serializer;
	private boolean compress = true;
	private boolean decompress = true;
	protected final int bufferSize;

	/**
	 * Creates a compressor with compress and decompress set to true and bufferSize set to 2048.
	 * @param serializer
	 */
	public Compressor (Serializer serializer) {
		this(serializer, 2048);
	}

	/**
	 * @param bufferSize The maximum size in bytes of an object that can be read or written.
	 */
	public Compressor (Serializer serializer, int bufferSize) {
		this.serializer = serializer;
		this.bufferSize = bufferSize;
	}

	/**
	 * Sets whether the compressor will compress data after serialization.
	 */
	public void setCompress (boolean compress) {
		this.compress = compress;
	}

	/**
	 * Sets whether the compressor will decompress data before serialization.
	 */
	public void setDecompress (boolean decompress) {
		this.decompress = decompress;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		if (!compress) {
			serializer.writeObjectData(buffer, object);
			return;
		}

		int start = buffer.position() + 2;
		try {
			buffer.position(start);
		} catch (IllegalArgumentException ex) {
			new BufferOverflowException();
		}

		serializer.writeObjectData(buffer, object);
		int end = buffer.position();

		buffer.position(start);
		buffer.limit(end);

		Context context = Kryo.getContext();
		ByteBuffer outputBuffer = context.getBuffer(bufferSize);

		compress(buffer, object, outputBuffer);
		outputBuffer.flip();
		buffer.position(start - 2);
		buffer.limit(buffer.capacity());
		buffer.putShort((short)(outputBuffer.limit()));
		buffer.put(outputBuffer);

		if (TRACE) {
			trace("kryo", "Compressed to " + ((int)(outputBuffer.limit() / (float)(end - start) * 10000) / 100f) + "% using: "
				+ getClass().getName());
		}
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		if (!decompress) return serializer.readObjectData(buffer, type);

		int oldLimit = buffer.limit();
		int length = buffer.getShort();
		try {
			buffer.limit(buffer.position() + length);
		} catch (IllegalArgumentException ex) {
			throw new SerializationException("Compressed data length exceeds buffer capacity: " + buffer.position() + length, ex);
		}

		Context context = Kryo.getContext();
		ByteBuffer outputBuffer = context.getBuffer(bufferSize);

		decompress(buffer, type, outputBuffer);
		outputBuffer.flip();
		buffer.limit(oldLimit);

		if (TRACE) trace("kryo", "Decompressed using: " + getClass().getName());
		return serializer.readObjectData(outputBuffer, type);
	}

	/**
	 * The compressor should read the input buffer from the current position to the limit, compress the data, and put the result in
	 * the output buffer.
	 * @param outputBuffer A non-direct buffer.
	 */
	abstract public void compress (ByteBuffer inputBuffer, Object object, ByteBuffer outputBuffer);

	/**
	 * The compressor should read the input buffer from the current position to the limit, decompress the data, and put the result
	 * in the output buffer.
	 * @param outputBuffer A non-direct buffer.
	 */
	abstract public void decompress (ByteBuffer inputBuffer, Class type, ByteBuffer outputBuffer);
}
