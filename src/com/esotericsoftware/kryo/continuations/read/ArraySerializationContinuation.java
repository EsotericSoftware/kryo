package com.esotericsoftware.kryo.continuations.read;

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
final public class ArraySerializationContinuation extends SerializationContinuation {

	// Container to keep objects read so far
	final private Object[] container;
	// Number of elements to read
	final private int size;
	// Index of current element
	private int idx;
	// Continuation to be used for reading a single element
	final private ObjectSerializationContinuation cont;
	// Helper for storing a single element at the right place
	final private ContainerStore store;

	public ArraySerializationContinuation(Input in, Object[] array, int size,
	        ObjectArraySerializer colSerializer, Class elemClass,
	        Serializer elemSerializer, boolean elementsCanBeNull) {
		super(in);
		this.size = size;
		this.container = array;

		if (size > 0) {
			Serializer ser = elemSerializer;
			if (ser == null) {
				elemClass = null;
			}
			store = new ContainerStore(container, 0);
			cont = new ObjectSerializationContinuation(in, elemClass, store,
			        elemSerializer, elementsCanBeNull);
		} else {
			store = null;
			cont = null;
		}
	}

	@Override
	public Object processRead(Kryo kryo, Input in, boolean popCount) {
		SerializationContinuation oldCont = kryo.peekContinuation();
		while (idx < size) {
			// As long as there are still elements to process,
			// prepare the continuation for the next element
			int cur = idx++;

			store.setIdx(cur);

			// push a task to serialize current element
//			kryo.pushContinuation(cont);
			cont.processRead(kryo, in, false);
			if(kryo.peekContinuation() != oldCont) 
				return container;
		}  
		
		{
			// All elements were processed
			// Current continuation is finished
			kryo.popContinuation();
		}

		return container;
	}

	@Override
    public String toString() {
	    return "ArraySerializationContinuation [container="
	            + Arrays.toString(container) + ", size=" + size + ", idx="
	            + idx + "]";
    }
}
