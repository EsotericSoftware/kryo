
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Reads and writes objects to and from bytes.
 * <p>
 * Any serializer that uses {@link Kryo} to serialize another object must be reentrant. */
public abstract interface Serializer<T> {
	public void write (Kryo kryo, Output output, T object);

	public T read (Kryo kryo, Input input, Class<T> type);
}
