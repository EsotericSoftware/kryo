
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.log.Log.*;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

/**
 * Serializes objects that implement the {@link Map} interface.
 * <p>
 * With the default constructor, a map requires a 1-3 byte header and an extra 4 bytes is written for each key/value pair.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class MapSerializer extends Serializer {
	private final Kryo kryo;
	private Class keyClass, valueClass;
	private Serializer keySerializer, valueSerializer;
	private boolean keysAreNotNull, valuesAreNotNull;

	public MapSerializer (Kryo kryo) {
		this.kryo = kryo;
	}

	/**
	 * @param keyClass The concrete class of each key. This saves 1 byte per key. Set to null if the class is not known or varies
	 *           per key (default).
	 * @param keysAreNotNull True if keys are not null. This saves 1 byte per key if keyClass is set. False if it is not known
	 *           (default).
	 * @param valueClass The concrete class of each value. This saves 1 byte per value. Set to null if the class is not known or
	 *           varies per value (default).
	 * @param valuesAreNotNull True if values are not null. This saves 1 byte per value if keyClass is set. False if it is not
	 *           known (default).
	 */
	public MapSerializer (Kryo kryo, Class keyClass, boolean keysAreNotNull, Class valueClass, boolean valuesAreNotNull) {
		this.kryo = kryo;
		this.keyClass = keyClass;
		this.keysAreNotNull = keysAreNotNull;
		keySerializer = keyClass == null ? null : kryo.getRegisteredClass(keyClass).serializer;
		this.valueClass = valueClass;
		this.valuesAreNotNull = valuesAreNotNull;
		valueSerializer = valueClass == null ? null : kryo.getRegisteredClass(valueClass).serializer;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		Map<Object, Object> map = (Map)object;
		int length = map.size();
		IntSerializer.put(buffer, length, true);
		if (length == 0) return;
		for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
			Entry entry = (Entry)iter.next();
			if (keySerializer != null) {
				if (keysAreNotNull)
					keySerializer.writeObjectData(buffer, entry.getKey());
				else
					keySerializer.writeObject(buffer, entry.getKey());
			} else
				kryo.writeClassAndObject(buffer, entry.getKey());
			if (valueSerializer != null) {
				if (valuesAreNotNull)
					valueSerializer.writeObjectData(buffer, entry.getValue());
				else
					valueSerializer.writeObject(buffer, entry.getValue());
			} else
				kryo.writeClassAndObject(buffer, entry.getValue());
		}
		if (level <= TRACE) trace("kryo", "Wrote map: " + object);
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		Map map = (Map)newInstance(type);
		int length = IntSerializer.get(buffer, true);
		if (length == 0) return (T)map;
		for (int i = 0; i < length; i++) {
			Object key;
			if (keySerializer != null) {
				if (keysAreNotNull)
					key = keySerializer.readObjectData(buffer, keyClass);
				else
					key = keySerializer.readObject(buffer, keyClass);
			} else
				key = kryo.readClassAndObject(buffer);
			Object value;
			if (valueSerializer != null) {
				if (valuesAreNotNull)
					value = valueSerializer.readObjectData(buffer, valueClass);
				else
					value = valueSerializer.readObject(buffer, valueClass);
			} else
				value = kryo.readClassAndObject(buffer);
			map.put(key, value);
		}
		if (level <= TRACE) trace("kryo", "Read map: " + map);
		return (T)map;
	}
}
