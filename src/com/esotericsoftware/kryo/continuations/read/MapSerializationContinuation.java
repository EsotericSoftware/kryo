package com.esotericsoftware.kryo.continuations.read;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializationContinuation;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.MapSerializer;

/***
 * This is a continuations based serializer for maps of objects.
 * 
 * @author Roman Levenstein <romxilev@gmail.com>
 * 
 */
public class MapSerializationContinuation extends SerializationContinuation {
//	final private Class type;
	final private int size;
	private int idx;
	final private Object[] keys;
	final private Object[] values;
	final private MapSerializer collSerializer;
	private Map collection;
	// Continuations to be used for serializing a single element
	final private ObjectSerializationContinuation keyCont;
	final private ObjectSerializationContinuation valueCont;
	final private ContainerStore keyStore;
	final private ContainerStore valueStore;

	public MapSerializationContinuation(Kryo kryo, Input in, Class type, Map collection,
	        int size, MapSerializer colSerializer, Class keyClass,
	        Serializer keySerializer, Class valueClass,
	        Serializer valueSerializer, boolean keysCanBeNull,
	        boolean valuesCanBeNull) {
		super(in);
		this.collSerializer = colSerializer;
//		this.type = type;
		this.collection = collection;
		this.size = size;
		this.keys = new Object[size];
		this.values = new Object[size];
		if (size > 0) {
			keyStore = new ContainerStore(keys, 0);
			valueStore = new ContainerStore(values, 0);
			keyCont = new ObjectSerializationContinuation(in, keyClass,
			        keyStore, keySerializer, keysCanBeNull);
			valueCont = new ObjectSerializationContinuation(in, valueClass,
			        valueStore, valueSerializer, valuesCanBeNull);
		} else {
			keyStore = null;
			valueStore = null;
			keyCont = null;
			valueCont = null;
		}
		if (collection == null)
			collection = collSerializer.create(kryo, in, type);
	}

	@Override
	public Object processRead(Kryo kryo, Input in, boolean popCont) {
		if (idx < size) {
			// As long as there are still elements to process,
			// prepare the continuation for the next element
			final int cur = idx++;

			keyStore.setIdx(cur);
			valueStore.setIdx(cur);

			kryo.pushContinuation(valueCont);
			kryo.pushContinuation(keyCont);
		} else {
			// All elements were processed.
			// Current continuation is finished
			kryo.popContinuation();
			// All sub-elements were read
			// Now we can create a proper data structure
			for (int i = 0; i < size; i++)
				collection.put(keys[i], values[i]);
		}
		return collection;
	}

	@Override
	public String toString() {
		return "MapSerializationContinuation [size=" + size
		        + ", idx=" + idx + ", keys=" + Arrays.toString(keys)
		        + ", values=" + Arrays.toString(values) + ", collSerializer="
		        + collSerializer + ", collection=" + collection + "]";
	}
}
