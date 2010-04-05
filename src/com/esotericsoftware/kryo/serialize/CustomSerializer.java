
package com.esotericsoftware.kryo.serialize;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.CustomSerialization;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

/**
 * @see CustomSerialization
 * @author Nathan Sweet <misc@n4te.com>
 */
public class CustomSerializer extends Serializer {
	private final Kryo kryo;

	public CustomSerializer (Kryo kryo) {
		this.kryo = kryo;
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		T object = newInstance(kryo, type);
		((CustomSerialization)object).readObjectData(kryo, buffer);
		return object;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		((CustomSerialization)object).writeObjectData(kryo, buffer);
	}
}
