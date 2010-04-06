
package com.esotericsoftware.kryo;

import static com.esotericsoftware.minlog.Log.*;

import java.nio.ByteBuffer;

/**
 * Serializes objects to and from a {@link ByteBuffer}.
 * @see Kryo#register(Class, Serializer)
 * @author Nathan Sweet <misc@n4te.com>
 */
abstract public class Serializer {
	static private final byte NULL_OBJECT = 0;
	static private final byte NOT_NULL_OBJECT = 1;

	private boolean canBeNull = true;

	/**
	 * When true, a byte will not be used to denote if the object is null. This is useful for primitives and objects that are known
	 * to never be null. Defaults to true.
	 */
	public void setCanBeNull (boolean canBeNull) {
		this.canBeNull = canBeNull;
	}

	/**
	 * Writes the object to the buffer.
	 * @param object Can be null (writes a special class ID for a null object instead).
	 */
	public final void writeObject (ByteBuffer buffer, Object object) {
		if (canBeNull) {
			if (object == null) {
				if (TRACE) trace("kryo", "Wrote object: null");
				buffer.put(NULL_OBJECT);
				return;
			}
			buffer.put(NOT_NULL_OBJECT);
		}
		writeObjectData(buffer, object);
	}

	/**
	 * Writes the object to the buffer.
	 * @param object Cannot be null.
	 */
	public abstract void writeObjectData (ByteBuffer buffer, Object object);

	/**
	 * Reads an object from the buffer.
	 * @return The deserialized object, or null if the object read from the buffer was a null.
	 */
	public final <T> T readObject (ByteBuffer buffer, Class<T> type) {
		if (canBeNull && buffer.get() == NULL_OBJECT) {
			if (TRACE) trace("kryo", "Read object: null");
			return null;
		}
		return readObjectData(buffer, type);
	}

	/**
	 * Reads an object from the buffer.
	 * @return The deserialized object, never null.
	 */
	abstract public <T> T readObjectData (ByteBuffer buffer, Class<T> type);

	/**
	 * Returns an instance of the specified class. The default implementation calls {@link Kryo#newInstance(Class)}.
	 * @throws SerializationException if the class could not be constructed.
	 */
	public <T> T newInstance (Kryo kryo, Class<T> type) {
		return kryo.newInstance(type);
	}
}
