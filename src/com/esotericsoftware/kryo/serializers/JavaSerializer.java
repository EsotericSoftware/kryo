
package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.ObjectMap;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/** Serializes objects using Java's built in serialization mechanism. Note that this is very inefficient and should be avoided if
 * possible.
 * @see Serializer
 * @see FieldSerializer
 * @see KryoSerializable
 * @author Nathan Sweet <misc@n4te.com> */
public class JavaSerializer extends Serializer {
	public void write (Kryo kryo, Output output, Object object) {
		try {
			ObjectMap graphContext = kryo.getGraphContext();
			ObjectOutputStream objectStream = (ObjectOutputStream)graphContext.get(this);
			if (objectStream == null) {
				objectStream = new ObjectOutputStream(output);
				graphContext.put(this, objectStream);
			}
			objectStream.writeObject(object);
			objectStream.flush();
		} catch (Exception ex) {
			throw new KryoException("Error during Java serialization.", ex);
		}
	}

	public Object read (Kryo kryo, Input input, Class type) {
		try {
			ObjectMap graphContext = kryo.getGraphContext();
			ObjectInputStream objectStream = (ObjectInputStream)graphContext.get(this);
			if (objectStream == null) {
				objectStream = new ObjectInputStream(input);
				graphContext.put(this, objectStream);
			}
			return objectStream.readObject();
		} catch (Exception ex) {
			throw new KryoException("Error during Java deserialization.", ex);
		}
	}
}
