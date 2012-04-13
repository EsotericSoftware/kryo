
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Reads and writes objects to and from bytes. */
public abstract class Serializer<T> {
	private boolean acceptsNull;

	/** Writes the bytes for the object to the output.
	 * @param object May be null if {@link #getAcceptsNull()} is true. */
	abstract public void write (Kryo kryo, Output output, T object);

	/** Creates a new object of the specified type. The object may be uninitialized. This method may read from input to populate the
	 * object, but it must not call {@link Kryo} methods to deserialize nested objects. That must be done in
	 * {@link #read(Kryo, Input, Object)}. The default implementation calls {@link Kryo#newInstance(Class)}.
	 * @return May be null if {@link #getAcceptsNull()} is true. */
	public T create (Kryo kryo, Input input, Class<T> type) {
		return kryo.newInstance(type);
	}

	/** Populates the object. This method may call {@link Kryo} methods to deserialize nested objects, unlike
	 * {@link #create(Kryo, Input, Class)}. The default implementation is empty.
	 * <p>
	 * Any serializer that uses {@link Kryo} to serialize another object may need to be reentrant. */
	public void read (Kryo kryo, Input input, T object) {
	}

	public boolean getAcceptsNull () {
		return acceptsNull;
	}

	/** If true, this serializer will handle writing and reading null values. If false, the Kryo framework handles null values. */
	public void setAcceptsNull (boolean acceptsNull) {
		this.acceptsNull = acceptsNull;
	}
}
