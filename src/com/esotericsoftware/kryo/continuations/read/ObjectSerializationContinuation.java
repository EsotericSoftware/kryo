package com.esotericsoftware.kryo.continuations.read;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializationContinuation;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/***
 * This is a continuations based serializer for a single object. This serializer
 * does not produce any continuations on its own. But Kryo#readXXX methods may
 * indirectly result in new continuations being produced.
 * 
 * @author Roman Levenstein <romxilev@gmail.com>
 * 
 */
public class ObjectSerializationContinuation extends SerializationContinuation {
	// Type of the object
	final private Class type;
	// Serialized to be used
	final private Serializer ser;
	// Where to store the deserialized object
	final private Store store;
	final private ReadMode mode;

	static enum ReadMode {
		CLASS_AND_OBJECT, OBJECT_OR_NULL, OBJECT_OR_NULL_WITH_SERIALIZER, OBJECT, OBJECT_WITH_SERIALIZER
	}

	public ObjectSerializationContinuation(Input in, Class type, Store store,
	        Serializer ser, boolean elementsCanBeNull) {
		super(in);
		this.ser = ser;
		this.store = store;
		this.type = type;
		if (type == null)
			mode = ReadMode.CLASS_AND_OBJECT;
		else if (ser == null) {
			if (elementsCanBeNull)
				mode = ReadMode.OBJECT_OR_NULL;
			else
				mode = ReadMode.OBJECT;
		} else {
			if (elementsCanBeNull)
				mode = ReadMode.OBJECT_OR_NULL_WITH_SERIALIZER;
			else
				mode = ReadMode.OBJECT_WITH_SERIALIZER;
		}
	}

	@Override
	public Object processRead(Kryo kryo, Input in, boolean popCont) {
		if(popCont) kryo.popContinuation();
		Serializer s = ser;
		Object obj = null;

		switch (mode) {
		case CLASS_AND_OBJECT:
			obj = kryo.readClassAndObject(in);
			break;
		case OBJECT_OR_NULL:
			obj = kryo.readObjectOrNull(in, type);
			break;
		case OBJECT_WITH_SERIALIZER:
			obj = kryo.readObject(in, type, s);
			break;
		case OBJECT_OR_NULL_WITH_SERIALIZER:
			obj = kryo.readObjectOrNull(in, type, s);
			break;
		case OBJECT:
			obj = kryo.readObject(in, type);
			break;
		}

		store.store(obj);
		return obj;
	}

	@Override
	public String toString() {
		return "ObjectSerializationContinuation [type=" + type + ", ser=" + ser
		         + store + "]";
	}

}
