package com.esotericsoftware.kryo.continuations.write;

import java.util.Arrays;
import java.util.Iterator;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializationContinuation;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.ObjectArraySerializer;

/***
 * This is a continuations based serializer for arrays of objects.
 * @author Roman Levenstein <romxilev@gmail.com>
 *
 */
final public class ArraySerializationContinuation extends
        SerializationContinuation {
	// Array to be serialized
	final private Object[] array;
	// Index of the element to be serialized
	private int idx;
	// Continuation to be used for serializing a single element
	final private ObjectSerializationContinuation cont;

	public ArraySerializationContinuation(Output out, Object[] array, int idx, ObjectArraySerializer colSerializer, Serializer elemSerializer, boolean elementsCanBeNull) {
	    super(out);
	    this.idx = idx;
	    this.array = array;
	    if(array.length > 0) {
				cont = new ObjectSerializationContinuation(out, null, elemSerializer, elementsCanBeNull);	    	
	    } else
	    	cont = null;
    }

	@Override
	public SerializationContinuation processWrite(Kryo kryo, Output out, boolean popCont) {
		SerializationContinuation oldCont = kryo.peekContinuation();
		while (idx < array.length) {
			// As long as there are still elements to process,
			// prepare the continuation for the next element
			Object elem = array[idx++];
			
		    cont.setElem(elem);
			
			// push a task to serialize the current element
//			kryo.pushContinuation(cont);
			cont.processWrite(kryo, out, false);
			if(kryo.peekContinuation() != oldCont) 
				return null;
		}
		
		{
			// All elements were processed
			// Current continuation is finished
			kryo.popContinuation();
		}
		return null;
	}

	@Override
    public String toString() {
	    return "ArraySerializationContinuation [array="
	            + Arrays.toString(array) + ", idx=" + idx + "]";
    }
}
