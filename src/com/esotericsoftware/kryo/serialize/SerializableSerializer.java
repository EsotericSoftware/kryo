
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.log.Log.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.CustomSerialization;
import com.esotericsoftware.kryo.SerializationException;
import com.esotericsoftware.kryo.Serializer;

/**
 * Serializes objects using Java's built in serialization mechanism. Note that this is very inefficient and should be avoided if
 * possible.
 * @see Serializer
 * @see FieldSerializer
 * @see CustomSerialization
 * @author Nathan Sweet <misc@n4te.com>
 */
public class SerializableSerializer extends Serializer {
	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		T object = get(buffer, type);
		if (level <= TRACE) trace("kryo", "Read object: " + object);
		return object;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		put(buffer, object);
		if (level <= TRACE) trace("kryo", "Wrote object: " + object);
	}

	static public void put (ByteBuffer buffer, Object object) {
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
			objectStream.writeObject(object);
			objectStream.close();
			byte[] array = byteStream.toByteArray();
			IntSerializer.put(buffer, array.length, true);
			buffer.put(array);
		} catch (BufferOverflowException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new SerializationException("Error during Java serialization.", ex);
		}
	}

	static public <T> T get (ByteBuffer buffer, Class<T> type) {
		int length = IntSerializer.get(buffer, true);
		byte[] array = new byte[length];
		buffer.get(array);
		try {
			return (T)new ObjectInputStream(new ByteArrayInputStream(array)).readObject();
		} catch (Exception ex) {
			throw new SerializationException("Error during Java deserialization.", ex);
		}
	}
}
