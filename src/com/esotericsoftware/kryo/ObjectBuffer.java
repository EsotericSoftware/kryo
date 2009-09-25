
package com.esotericsoftware.kryo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Serializes objects to and from byte arrays and streams.<br>
 * <br>
 * This class uses a buffer internally and is not thread safe.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class ObjectBuffer {
	private final Kryo kryo;
	private final ByteBuffer buffer;
	private final byte[] bytes;

	/**
	 * Creates an ObjectStream with a buffer size of 2048.
	 */
	public ObjectBuffer (Kryo kryo) {
		this(kryo, 2048);
	}

	/**
	 * @param bufferSize The maximum size in bytes of an object that can be read or written.
	 */
	public ObjectBuffer (Kryo kryo, int bufferSize) {
		this.kryo = kryo;
		buffer = ByteBuffer.allocate(bufferSize);
		bytes = buffer.array();
	}

	/**
	 * Reads the specified number of bytes from the stream and returns a deserialized object.
	 * @param contentLength The number of bytes to read, or -1 to read to the end of the stream.
	 */
	private void readToBuffer (InputStream input, int contentLength) {
		if (contentLength == -1) contentLength = Integer.MAX_VALUE;
		try {
			int position = 0;
			while (position < contentLength) {
				int count = input.read(bytes, position, bytes.length - position);
				if (count == -1) break;
				position += count;
			}
			buffer.position(0);
			buffer.limit(position);
		} catch (IOException ex) {
			throw new SerializationException("Error reading object bytes.", ex);
		}
	}

	/**
	 * @see Kryo#readClassAndObject(ByteBuffer)
	 */
	public Object readClassAndObject (InputStream input, int contentLength) {
		readToBuffer(input, contentLength);
		return kryo.readClassAndObject(buffer);
	}

	/**
	 * @see Kryo#readObject(ByteBuffer, Class)
	 */
	public <T> T readObject (InputStream input, int contentLength, Class<T> type) {
		readToBuffer(input, contentLength);
		return kryo.readObject(buffer, type);
	}

	/**
	 * @see Kryo#readObjectData(ByteBuffer, Class)
	 */
	public <T> T readObjectData (InputStream input, int contentLength, Class<T> type) {
		readToBuffer(input, contentLength);
		return kryo.readObjectData(buffer, type);
	}

	/**
	 * @see Kryo#writeClassAndObject(ByteBuffer, Object)
	 */
	public void writeClassAndObject (OutputStream output, Object object) {
		buffer.clear();
		kryo.writeClassAndObject(buffer, object);
		writeToStream(output);
	}

	/**
	 * @see Kryo#writeObject(ByteBuffer, Object)
	 */
	public void writeObject (OutputStream output, Object object) {
		buffer.clear();
		kryo.writeObject(buffer, object);
		writeToStream(output);
	}

	/**
	 * @see Kryo#writeObjectData(ByteBuffer, Object)
	 */
	public void writeObjectData (OutputStream output, Object object) {
		buffer.clear();
		kryo.writeObjectData(buffer, object);
		writeToStream(output);
	}

	private void writeToStream (OutputStream output) {
		try {
			output.write(bytes, 0, buffer.position());
		} catch (IOException ex) {
			throw new SerializationException("Error writing object bytes.", ex);
		}
	}

	/**
	 * @see Kryo#readClassAndObject(ByteBuffer)
	 */
	public Object readClassAndObject (byte[] objectBytes) {
		return kryo.readClassAndObject(ByteBuffer.wrap(objectBytes));
	}

	/**
	 * @see Kryo#readObject(ByteBuffer, Class)
	 */
	public <T> T readObject (byte[] objectBytes, Class<T> type) {
		return kryo.readObject(ByteBuffer.wrap(objectBytes), type);
	}

	/**
	 * @see Kryo#readObjectData(ByteBuffer, Class)
	 */
	public <T> T readObjectData (byte[] objectBytes, Class<T> type) {
		return kryo.readObjectData(ByteBuffer.wrap(objectBytes), type);
	}

	/**
	 * @see Kryo#writeClassAndObject(ByteBuffer, Object)
	 */
	public byte[] writeClassAndObject (Object object) {
		buffer.clear();
		kryo.writeClassAndObject(buffer, object);
		return writeToBytes();
	}

	/**
	 * @see Kryo#writeObject(ByteBuffer, Object)
	 */
	public byte[] writeObject (Object object) {
		buffer.clear();
		kryo.writeObject(buffer, object);
		return writeToBytes();
	}

	/**
	 * @see Kryo#writeObjectData(ByteBuffer, Object)
	 */
	public byte[] writeObjectData (Object object) {
		buffer.clear();
		kryo.writeObjectData(buffer, object);
		return writeToBytes();
	}

	private byte[] writeToBytes () {
		byte[] objectBytes = new byte[buffer.position()];
		System.arraycopy(bytes, 0, objectBytes, 0, objectBytes.length);
		return objectBytes;
	}
}
