package com.esotericsoftware.kryo.continuations.read;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializationContinuation;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;

/***
 * This is a continuations-based serializer for collections of objects.
 * @author Roman Levenstein <romxilev@gmail.com>
 *
 */
public class CollectionSerializationContinuation extends
        SerializationContinuation {
	// Container to keep objects read so far
	final Object[] container;
	// Number of elements to read
	final private int size;
	// Index of current element
	private int idx;
	// Type of the collection
//	private final Class type;
	// Serializer of the whole collection
//	final private CollectionSerializer collSerializer;
	// Target collection, where read elements to be stored
	private Collection collection;
	// Continuation to be used for reading a single element
	final private ObjectSerializationContinuation cont;
	// Helper for storing a single element at the right place
	final private ContainerStore store;

	public CollectionSerializationContinuation(Kryo kryo, Input in, Class type,
	        Collection collection, int size,
	        CollectionSerializer collSerializer, Class elemClass, Serializer elemSerializer,
	        boolean elementsCanBeNull) {
		super(in);
//		this.collSerializer = collSerializer;
//		this.type = type;
		this.size = size;
		this.collection = collection;
		this.container = new Object[size];
		if(size>0) {
			store = new ContainerStore(container, 0);
			cont = new ObjectSerializationContinuation(in,
			        elemClass, store, elemSerializer, elementsCanBeNull);
		} else {
			store = null;
			cont = null;
		}
		if (collection == null)
			collection = collSerializer.create(kryo, in, type);
	}

	@Override
	public Object processRead(Kryo kryo, Input in, boolean popCont) {
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
				return collection;
		} 
		
		{
			// All elements were processed
			// Current continuation is finished
			kryo.popContinuation();
			// All sub-elements were read
			// Now we can create a proper data structure
			for (int i = 0; i < size; i++)
				collection.add(container[i]);
		}
		return collection;
	}

	@Override
    public String toString() {
	    return "CollectionSerializationContinuation [container="
	            + Arrays.toString(container) + ", size=" + size + ", idx="
	            + idx + "]";
    }
}
