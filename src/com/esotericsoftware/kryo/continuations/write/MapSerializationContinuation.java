package com.esotericsoftware.kryo.continuations.write;

import java.util.Iterator;
import java.util.Map.Entry;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializationContinuation;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.MapSerializer;

/***
 * This is a continuations based serializer for maps of objects.
 * @author Roman Levenstein <romxilev@gmail.com>
 *
 */
public class MapSerializationContinuation extends SerializationContinuation {
	// Iterator over the map to be serialized
	final private Iterator it;
	// Continuations to be used for serializing a single element
	final private ObjectSerializationContinuation keyCont;
	final private ObjectSerializationContinuation valueCont;


	public MapSerializationContinuation(Output out, Iterator iterator, MapSerializer colSerializer, Serializer keySerializer, Serializer valueSerializer, boolean keysCanBeNull, boolean valuesCanBeNull) {
	    super(out);
	    this.it = iterator;
	    if(it.hasNext()) {
				keyCont = new ObjectSerializationContinuation(out, null, keySerializer, keysCanBeNull);	    	
				valueCont = new ObjectSerializationContinuation(out, null, valueSerializer, valuesCanBeNull);	    	
	    } else {
	    	keyCont = null;
	    	valueCont = null;
	    }
    }


	@Override
	public SerializationContinuation processWrite(Kryo kryo, Output out, boolean popCont) {
		if (it.hasNext()) {
			// As long as there are still elements to serialize,
			// prepare the continuation for the next element
			Entry elem = (Entry)it.next();
						
			keyCont.setElem(elem.getKey());
			valueCont.setElem(elem.getValue());
			
			kryo.pushContinuation(valueCont);
			kryo.pushContinuation(keyCont);
		} else {
			// All elements were serialized
			// Current continuation is finished
			kryo.popContinuation();
		}
		return null;
	}


	@Override
    public String toString() {
	    return "MapSerializationContinuation [it=" + it + "]";
    }
}
