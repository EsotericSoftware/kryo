
package com.esotericsoftware.kryo.serialize;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Serializer;

/**
 * Convenience class that provides simpler methods for reading and writing object data.
 */
public abstract class SimpleSerializer<T> extends Serializer {
	public <E> E readObjectData (ByteBuffer buffer, Class<E> type) {
		return (E)read(buffer);
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		write(buffer, (T)object);
	}

	/**
	 * Convenience method that can be used for cleanliness when the extra parameters provided by
	 * {@link #readObjectData(ByteBuffer, Class)} are not needed.
	 */
	abstract public T read (ByteBuffer buffer);

	/**
	 * Convenience method that can be used for cleanliness when the extra parameters provided by
	 * {@link #writeObjectData(ByteBuffer, Object)} are not needed.
	 */
	abstract public void write (ByteBuffer buffer, T object);
}
