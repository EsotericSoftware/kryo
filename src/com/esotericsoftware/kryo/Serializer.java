
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Reads and writes objects to and from bytes.
 * <p>
 * Any serializer that uses {@link Kryo} to serialize another object must be reentrant. */
public abstract class Serializer<T> {
	public abstract void write (Kryo kryo, Output output, T object);

	public abstract T read (Kryo kryo, Input input, Class<T> type);

	/** Generally serializers should use this method to create new instances via reflection. This allows instance creation to be
	 * customized by overridding this method. The default implementaion calls {@link Kryo#newInstance(Class)}. */
	public T newInstance (Kryo kryo, Input input, Class<T> type) {
		return kryo.newInstance(type);
	}
}
