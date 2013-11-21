package com.esotericsoftware.kryo.continuations.read;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializationContinuation;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;

/***
 * This is a continuations based serializer for an object with fields, 
 * i.e. this is a continuation-based version of FieldSerialiter.
 * 
 * @author Roman Levenstein <romxilev@gmail.com>
 *
 */
public class FieldSerializationContinuation extends SerializationContinuation {

	final private Object o;
	final private CachedField[] fields;
	final private Class[] generics;
	private int fieldNum;
	final private FieldSerializer fieldSerializer;
//	final private CachedFieldSerializationContinuation fieldContinuation;

	public FieldSerializationContinuation(Input in, Object o, Class[] generics,
	        int fieldNum, FieldSerializer fieldSerializer,
	        Serializer elemSerializer, boolean elementsCanBeNull) {
		super(in);
		this.fieldNum = fieldNum;
		this.fieldSerializer = fieldSerializer;
		this.generics = generics;
		this.fields = fieldSerializer.getFields();
		this.o = o;
//		if (fields.length > 0) {
//			fieldContinuation = new CachedFieldSerializationContinuation(in, o,
//			        generics, fields[0], elementsCanBeNull);
//		} else
//			fieldContinuation = null;
	}

	@Override
	public Object processRead(Kryo kryo, Input in, boolean popCont) {
		SerializationContinuation oldCont = kryo.peekContinuation();
		while (fieldNum < fields.length) {
			// As long as there are still fields to process,
			// prepare the continuation for the next element

			int curField = fieldNum++;

//			fieldContinuation.setField(fields[curField]);
			
			CachedField field = fields[curField];
			field.read(in, o);
			// TODO: May be if we can check that this field has a non-continuation based
			// serializer, we do not need to return and can continue processing 
			// for the next field?
			if(kryo.peekContinuation() != oldCont) 
				return o;
//			if (field.getField().getType().isPrimitive())
//				field.read(in, o);
//			else
//				// push a task to serialize elem
//				kryo.pushContinuation(fieldContinuation);
		} 
		
		{
			// All elements were serialized
			// Current continuation is finished
			kryo.popContinuation();
			if (fieldSerializer.getGenericsScope() != null) {
				// Pop the scope for generics
				kryo.popGenericsScope();
			}
		}
		return o;
	}

	@Override
    public String toString() {
	    return "FieldSerializationContinuation [o=" + o + ", fields="
	            + Arrays.toString(fields) + ", generics="
	            + Arrays.toString(generics) + ", fieldNum=" + fieldNum
	            + ", fieldSerializer=" + fieldSerializer + "]";
    }
}
