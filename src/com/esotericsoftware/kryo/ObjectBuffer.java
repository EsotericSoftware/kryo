
package com.esotericsoftware.kryo;

import static com.esotericsoftware.minlog.Log.*;

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
	private final int maxCapacity;
	private ByteBuffer buffer;
	private byte[] bytes;

	/**
	 * Creates an ObjectStream with an initial buffer size of 2KB and a maximum size of 16KB.
	 */
	public ObjectBuffer (Kryo kryo) {
		this(kryo, 2 * 1024, 16 * 1024);
	}

	/**
	 * @param maxCapacity The initial and maximum size in bytes of an object that can be read or written.
	 * @see #ObjectBuffer(Kryo, int, int)
	 */
	public ObjectBuffer (Kryo kryo, int maxCapacity) {
		this(kryo, maxCapacity, maxCapacity);
	}

	/**
	 * @param initialCapacity The initial maximum size in bytes of an object that can be read or written.
	 * @param maxCapacity The maximum size in bytes of an object that can be read or written. The capacity is doubled until the
	 *           maxCapacity is exceeded, then SerializationException is thrown by the read and write methods.
	 */
	public ObjectBuffer (Kryo kryo, int initialCapacity, int maxCapacity) {
		this.kryo = kryo;
		buffer = ByteBuffer.allocate(initialCapacity);
		bytes = buffer.array();
		this.maxCapacity = maxCapacity;
	}

	/**
	 * Reads the specified number of bytes from the stream into the buffer.
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
				if (position == bytes.length && !resizeBuffer(true))
					throw new SerializationException("Buffer limit exceeded: " + maxCapacity);
			}
			buffer.position(0);
			buffer.limit(position);
		} catch (IOException ex) {
			throw new SerializationException("Error reading object bytes.", ex);
		}
	}

	/**
	 * Reads to the end of the stream and returns the deserialized object.
	 * @see Kryo#readClassAndObject(ByteBuffer)
	 */
	public Object readClassAndObject (InputStream input) {
		readToBuffer(input, -1);
		return kryo.readClassAndObject(buffer);
	}

	/**
	 * Reads the specified number of bytes and returns the deserialized object.
	 * @see Kryo#readClassAndObject(ByteBuffer)
	 */
	public Object readClassAndObject (InputStream input, int contentLength) {
		readToBuffer(input, contentLength);
		return kryo.readClassAndObject(buffer);
	}

	/**
	 * Reads to the end of the stream and returns the deserialized object.
	 * @see Kryo#readObject(ByteBuffer, Class)
	 */
	public <T> T readObject (InputStream input, Class<T> type) {
		readToBuffer(input, -1);
		return kryo.readObject(buffer, type);
	}

	/**
	 * Reads the specified number of bytes and returns the deserialized object.
	 * @see Kryo#readObject(ByteBuffer, Class)
	 */
	public <T> T readObject (InputStream input, int contentLength, Class<T> type) {
		readToBuffer(input, contentLength);
		return kryo.readObject(buffer, type);
	}

	/**
	 * Reads to the end of the stream and returns the deserialized object.
	 * @see Kryo#readObjectData(ByteBuffer, Class)
	 */
	public <T> T readObjectData (InputStream input, Class<T> type) {
		readToBuffer(input, -1);
		return kryo.readObjectData(buffer, type);
	}

	/**
	 * Reads the specified number of bytes and returns the deserialized object.
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
		while (true) {
			try {
				kryo.writeClassAndObject(buffer, object);
				break;
			} catch (SerializationException ex) {
				if (!ex.causedByBufferOverflow()) throw ex;
				if (!resizeBuffer(false)) {
					throw new SerializationException("Buffer limit exceeded serializing object of type: "
						+ object.getClass().getName(), ex);
				}
				Kryo.getContext().reset();
			}
		}
		writeToStream(output);
	}

	/**
	 * @see Kryo#writeObject(ByteBuffer, Object)
	 */
	public void writeObject (OutputStream output, Object object) {
		buffer.clear();
		while (true) {
			try {
				kryo.writeObject(buffer, object);
				break;
			} catch (SerializationException ex) {
				if (!ex.causedByBufferOverflow()) throw ex;
				if (!resizeBuffer(false)) {
					throw new SerializationException("Buffer limit exceeded serializing object of type: "
						+ object.getClass().getName(), ex);
				}
				Kryo.getContext().reset();
			}
		}
		writeToStream(output);
	}

	/**
	 * @see Kryo#writeObjectData(ByteBuffer, Object)
	 */
	public void writeObjectData (OutputStream output, Object object) {
		buffer.clear();
		while (true) {
			try {
				kryo.writeObjectData(buffer, object);
				break;
			} catch (SerializationException ex) {
				if (!ex.causedByBufferOverflow()) throw ex;
				if (!resizeBuffer(false)) {
					throw new SerializationException("Buffer limit exceeded serializing object of type: "
						+ object.getClass().getName(), ex);
				}
				Kryo.getContext().reset();
			}
		}
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
		while (true) {
			try {
				kryo.writeClassAndObject(buffer, object);
				break;
			} catch (SerializationException ex) {
				if (!ex.causedByBufferOverflow()) throw ex;
				if (!resizeBuffer(false)) {
					throw new SerializationException("Buffer limit exceeded serializing object of type: "
						+ object.getClass().getName(), ex);
				}
				Kryo.getContext().reset();
			}
		}
		return writeToBytes();
	}

	/**
	 * @see Kryo#writeObject(ByteBuffer, Object)
	 */
	public byte[] writeObject (Object object) {
		buffer.clear();
		while (true) {
			try {
				kryo.writeObject(buffer, object);
				break;
			} catch (SerializationException ex) {
				if (!ex.causedByBufferOverflow()) throw ex;
				if (!resizeBuffer(false)) {
					throw new SerializationException("Buffer limit exceeded serializing object of type: "
						+ object.getClass().getName(), ex);
				}
				Kryo.getContext().reset();
			}
		}
		return writeToBytes();
	}

	/**
	 * @see Kryo#writeObjectData(ByteBuffer, Object)
	 */
	public byte[] writeObjectData (Object object) {
		buffer.clear();
		while (true) {
			try {
				kryo.writeObjectData(buffer, object);
				break;
			} catch (SerializationException ex) {
				if (!ex.causedByBufferOverflow()) throw ex;
				if (!resizeBuffer(false)) {
					throw new SerializationException("Buffer limit exceeded serializing object of type: "
						+ object.getClass().getName(), ex);
				}
				Kryo.getContext().reset();
			}
		}
		return writeToBytes();
	}

	private byte[] writeToBytes () {
		byte[] objectBytes = new byte[buffer.position()];
		System.arraycopy(bytes, 0, objectBytes, 0, objectBytes.length);
		return objectBytes;
	}

	private boolean resizeBuffer (boolean preserveContents) {
		int capacity = buffer.capacity();
		if (capacity == maxCapacity) return false;
		int newCapacity = Math.min(maxCapacity, capacity * 2);

		ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
		byte[] newArray = newBuffer.array();
		if (preserveContents) System.arraycopy(bytes, 0, newArray, 0, bytes.length);
		buffer = newBuffer;
		bytes = newArray;

		if (DEBUG) debug("kryo", "Resized ObjectBuffer to: " + newCapacity);
		return true;
	}
}
