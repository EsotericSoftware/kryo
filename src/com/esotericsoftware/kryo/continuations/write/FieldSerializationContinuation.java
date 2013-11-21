package com.esotericsoftware.kryo.continuations.write;

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
	// Base object
	final private Object o;
	final private Class[] generics;
	// Set of fields belonging to this object
	final private CachedField[] fields;
	// Current field number
	private int fieldNum;
	// FieldSerializer object belonging to the base object
	final private FieldSerializer fieldSerializer;
	final private CachedFieldSerializationContinuation fieldContinuation;

	public FieldSerializationContinuation(Output out, Object o, Class[] generics, int fieldNum,
	        FieldSerializer fieldSerializer, Serializer elemSerializer,
	        boolean elementsCanBeNull) {
		super(out);
		this.o = o;
		this.fieldNum = fieldNum;
		this.fieldSerializer = fieldSerializer;
		this.generics = generics;
		this.fields = fieldSerializer.getFields();
//		if(fields.length > 0) {
//				fieldContinuation = new CachedFieldSerializationContinuation(out,
//				        o, generics, fields[0], elementsCanBeNull);
//		} else
			fieldContinuation = null;
	}

	@Override
	public SerializationContinuation processWrite(Kryo kryo, Output out, boolean popCont) {
		SerializationContinuation oldCont = kryo.peekContinuation();
		while (fieldNum < fields.length) {
			// As long as there are still fields to serialize,
			// prepare the continuation for the next element

//			int curField = fieldNum++;

//			fieldContinuation.setField(fields[curField]);
//			CachedField field = fields[curField];
//			field.write(out, o);
			fields[fieldNum++].write(out, o);
			// TODO: May be if we can check that this field has a non-continuation based
			// serializer, we do not need to return and can continue processing 
			// for the next field?
			if(kryo.peekContinuation() != oldCont) 
				return null;
//			if(field.getField().getType().isPrimitive()) {
//				// Small optimization to avoid creation of useless continuations
//				field.write(out, o);
//			} else {
//				// push a task to serialize elem
//				kryo.pushContinuation(fieldContinuation);
//			}
		} 
		
		{
			// All elements were serialized
			// Current continuation is finished
			kryo.popContinuation();
			if(fieldSerializer.getGenericsScope() != null) {
				// Pop the scope for generics
				kryo.popGenericsScope();
			}			
			return null;
		}
	}

	@Override
    public String toString() {
	    return "FieldSerializationContinuation [o=" + o + ", generics="
	            + Arrays.toString(generics) + ", fields="
	            + Arrays.toString(fields) + ", fieldNum=" + fieldNum
	            + ", fieldSerializer=" + fieldSerializer + "]";
    }
}
