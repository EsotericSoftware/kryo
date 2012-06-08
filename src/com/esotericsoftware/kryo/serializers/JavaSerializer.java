
package com.esotericsoftware.kryo.serializers;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Serializes objects using Java's built in serialization mechanism. Note that this is very inefficient and should be avoided if
 * possible.
 * @see Serializer
 * @see FieldSerializer
 * @see KryoSerializable
 * @author Nathan Sweet <misc@n4te.com> */
public class JavaSerializer extends Serializer {
	private ObjectOutputStream objectStream;
	private Output lastOutput;

	public void write (Kryo kryo, Output output, Object object) {
		try {
			if (output != lastOutput) {
				objectStream = new ObjectOutputStream(output);
				lastOutput = output;
			} else
				objectStream.reset();
			objectStream.writeObject(object);
			objectStream.flush();
		} catch (Exception ex) {
			throw new KryoException("Error during Java serialization.", ex);
		}
	}

	public Object read (Kryo kryo, Input input, Class type) {
		try {
			return new ObjectInputStream(input).readObject();
		} catch (Exception ex) {
			throw new KryoException("Error during Java deserialization.", ex);
		}
	}
}
