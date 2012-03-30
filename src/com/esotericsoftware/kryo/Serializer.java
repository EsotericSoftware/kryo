
package com.esotericsoftware.kryo;

/** Any serializer that uses {@link Kryo} to serialize another object must be reentrant. */
public abstract class Serializer<T> {
	public abstract void write (Kryo kryo, KryoOutput output, T object);

	public abstract T read (Kryo kryo, KryoInput input, Class<T> type);

	public T newInstance (Kryo kryo, KryoInput input, Class<T> type) {
		return kryo.newInstance(type);
	}
}
