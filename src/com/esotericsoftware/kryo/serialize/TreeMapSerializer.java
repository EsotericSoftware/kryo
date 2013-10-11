package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.trace;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

/**
 * {@link TreeMap} serializer is a specialized {@link CollectionSerializer} that handles
 * TreeMaps containing elements that don't implement {@link Comparable}.
 * 
 * @author Martin Grotzke <martin.grotzke@googlemail.com>
 */
public class TreeMapSerializer extends MapSerializer {

	private final Kryo kryo;
	
	public TreeMapSerializer (Kryo kryo) {
		super(kryo);
		this.kryo = kryo;
	}

	@Override
	public void writeObjectData (ByteBuffer buffer, Object object) {
		kryo.writeClassAndObject(buffer, ((TreeMap)object).comparator());
		super.writeObjectData(buffer, object);
	}

	@Override
	protected <T> T newInstance(ByteBuffer buffer, Class<T> type) {
		return (T) new TreeMap((Comparator)kryo.readClassAndObject(buffer));
	}
	
}
