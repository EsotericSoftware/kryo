package com.esotericsoftware.kryo.continuations.write;

import java.util.Iterator;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializationContinuation;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;

/***
 * This is a continuations based serializer for collections of objects.
 * @author Roman Levenstein <romxilev@gmail.com>
 *
 */
public class CollectionSerializationContinuation extends
        SerializationContinuation {
	
	// Iterator over the collection to be serialized
	final private Iterator it;
	// Continuation to be used for serializing a single element
	final private ObjectSerializationContinuation cont;


	public CollectionSerializationContinuation(Output out, Iterator iterator, CollectionSerializer colSerializer, Serializer elemSerializer, boolean elementsCanBeNull) {
	    super(out);
	    this.it = iterator;
	    if(it.hasNext()) {
				cont = new ObjectSerializationContinuation(out, null, elemSerializer, elementsCanBeNull);	    	
	    } else
	    	cont = null;
    }

	@Override
	public SerializationContinuation processWrite(Kryo kryo, Output out, boolean popCont) {
		SerializationContinuation oldCont = kryo.peekContinuation();
		if (it.hasNext()) {
			// As long as there are still elements to serialize,
			// prepare the continuation for the next element
			Object elem = it.next();
						
			cont.setElem(elem);
			
			// push a task to serialize current element
//			kryo.pushContinuation( cont );
			cont.processWrite(kryo, out, false);
			if(kryo.peekContinuation() != oldCont) 
				return null;
		} else {
			// All elements were serialized
			// Current continuation is finished
			kryo.popContinuation();
		}
		return null;
	}

	@Override
    public String toString() {
	    return "CollectionSerializationContinuation [it=" + it + "]";
    }	
}
