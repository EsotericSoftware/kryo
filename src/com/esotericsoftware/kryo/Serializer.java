
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Any serializer that uses {@link Kryo} to serialize another object must be reentrant. */
public abstract class Serializer<T> {
	public abstract void write (Kryo kryo, Output output, T object);

	public abstract T read (Kryo kryo, Input input, Class<T> type);

	public T newInstance (Kryo kryo, Input input, Class<T> type) {
		return kryo.newInstance(type);
	}
}
