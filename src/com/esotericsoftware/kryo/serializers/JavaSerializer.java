
package com.esotericsoftware.kryo.serializers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.BufferOverflowException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import static com.esotericsoftware.minlog.Log.*;

/** Serializes objects using Java's built in serialization mechanism. Note that this is very inefficient and should be avoided if
 * possible.
 * @see Serializer
 * @see FieldSerializer
 * @see CustomSerialization
 * @author Nathan Sweet <misc@n4te.com> */
public class JavaSerializer extends Serializer {
	public void write (Kryo kryo, Output output, Object object) {
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream(256);
			ObjectOutputStream objectStream = new ObjectOutputStream(output);
			objectStream.writeObject(object);
			objectStream.close();
			byte[] array = byteStream.toByteArray();
			output.writeInt(array.length, true);
			output.writeBytes(array);
		} catch (BufferOverflowException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new KryoException("Error during Java serialization.", ex);
		}
		if (TRACE) trace("kryo", "Wrote object: " + object);
	}

	public Object read (Kryo kryo, Input input, Class type) {
		byte[] array = input.readBytes(input.readInt(true));
		try {
			Object object = new ObjectInputStream(new ByteArrayInputStream(array)).readObject();
			if (TRACE) trace("kryo", "Read object: " + object);
			return object;
		} catch (Exception ex) {
			throw new KryoException("Error during Java deserialization.", ex);
		}
	}
}
