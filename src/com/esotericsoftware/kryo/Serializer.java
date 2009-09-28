
package com.esotericsoftware.kryo;

import static com.esotericsoftware.minlog.Log.*;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serializes objects to and from a {@link ByteBuffer}.
 * @see Kryo#register(Class, Serializer)
 * @author Nathan Sweet <misc@n4te.com>
 */
abstract public class Serializer {
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
				buffer.put((byte)0);
				return;
			}
			buffer.put((byte)1);
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
		if (canBeNull && buffer.get() == 0) {
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
	 * Returns an instance of the specified class.
	 * @throws SerializationException if the class could not be constructed.
	 */
	static public <T> T newInstance (Class<T> type) {
		try {
			return type.newInstance();
		} catch (Exception ex) {
			if (ex instanceof InstantiationException) {
				Constructor[] constructors = type.getConstructors();
				boolean hasZeroArgConstructor = false;
				for (int i = 0, n = constructors.length; i < n; i++) {
					Constructor constructor = constructors[i];
					if (constructor.getParameterTypes().length == 0) {
						hasZeroArgConstructor = true;
						break;
					}
				}
				if (!hasZeroArgConstructor)
					throw new SerializationException("Class cannot be created (missing no-arg constructor): " + type.getName(), ex);
			}
			throw new SerializationException("Error constructing instance of class: " + type.getName(), ex);
		}
	}
}
