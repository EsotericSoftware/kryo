package com.esotericsoftware.kryo.continuations.write;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializationContinuation;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Output;

/***
 * This is a continuations based serializer for a single object.
 * This serializer does not produce any continuations on its own.
 * But Kryo#writeXXX methods may indirectly result in new continuations being produced.
 * 
 * @author Roman Levenstein <romxilev@gmail.com>
 *
 */
public class ObjectSerializationContinuation extends SerializationContinuation {
	// Object to be serialized
	private Object o;
	// Serializer to be used
	final private Serializer ser;
	final private boolean elementsCanBeNull;

	public ObjectSerializationContinuation(Output out, Object o,
	        Serializer ser, boolean elementsCanBeNull) {
		super(out);
		this.elementsCanBeNull = elementsCanBeNull;
		this.ser = ser;
		this.o = o;
	}

	@Override
	public SerializationContinuation processWrite(Kryo kryo, Output out, boolean popCont) {
		if(popCont)
			kryo.popContinuation();
		Serializer s = ser;
		if (s == null) {
			kryo.writeClassAndObject(out, o);
		} else {
			if (elementsCanBeNull)
				kryo.writeObjectOrNull(out, o, ser);
			else
				kryo.writeObject(out, o, ser);
		}
		return null;
	}

	public void setElem(Object o) {
		this.o = o;
	}

	@Override
    public String toString() {
	    return "ObjectSerializationContinuation [o=" + o + ", ser=" + ser
	            + ", elementsCanBeNull=" + elementsCanBeNull + "]";
    }

}
