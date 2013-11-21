package com.esotericsoftware.kryo.continuations.write;

import java.util.Arrays;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializationContinuation;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.UnsafeCacheFields;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.kryo.serializers.ObjectField;
import com.esotericsoftware.kryo.serializers.UnsafeCacheFields.UnsafeObjectField;


/***
 * This is a continuations based serializer for a single field of an object, 
 * 
 * @author Roman Levenstein <romxilev@gmail.com>
 *
 */
public class CachedFieldSerializationContinuation extends SerializationContinuation {

	// Base object
	final private Object o;
	// Field to be serialized
	private CachedField field;
	final private Class[] generics;

	public CachedFieldSerializationContinuation(Output out, Object o,
	        Class[] generics, CachedField field, boolean elementsCanBeNull) {
		super(out);
		this.field = field;
		this.o = o;
		this.generics = generics;
	}

	public void setField(CachedField field) {
		this.field = field;
	}
	
	@Override
	public SerializationContinuation processWrite(Kryo kryo, Output out, boolean popCont) {
		if(popCont) kryo.popContinuation();
		if(field instanceof ObjectField)
			((ObjectField)field).generics = generics;
		else if(field instanceof UnsafeObjectField)
			((UnsafeObjectField)field).generics = generics;
		field.write(out, o);
		return null;
	}

	@Override
    public String toString() {
	    return "CachedFieldSerializationContinuation [o=" + o + ", field="
	            + field + ", generics=" + Arrays.toString(generics) + "]";
    }

}
