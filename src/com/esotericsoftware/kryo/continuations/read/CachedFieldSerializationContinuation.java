package com.esotericsoftware.kryo.continuations.read;

import java.util.Arrays;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializationContinuation;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
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
	// Field to be processed
	private CachedField field;
	final private Class[] generics;

	public CachedFieldSerializationContinuation(Input in, Object o,
	        Class[] generics, CachedField field, boolean elementsCanBeNull) {
		super(in);
		this.field = field;
		this.o = o;
		this.generics = generics;
	}

	public void setField(CachedField field) {
		this.field = field;
	}

	@Override
	public Object processRead(Kryo kryo, Input in, boolean popCont) {
		if(popCont) kryo.popContinuation();
		if(field instanceof ObjectField)
			((ObjectField)field).generics = generics;
		else if(field instanceof UnsafeObjectField)
			((UnsafeObjectField)field).generics = generics;
		field.read(in, o);
		return o;
	}

	@Override
    public String toString() {
	    return "CachedFieldSerializationContinuation [o=" + o + ", field="
	            + field + ", generics=" + Arrays.toString(generics) + "]";
    }

}
