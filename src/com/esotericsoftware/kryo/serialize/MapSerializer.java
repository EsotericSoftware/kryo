
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

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
	private boolean keysCanBeNull = true, valuesCanBeNull = true;

	public MapSerializer (Kryo kryo) {
		this.kryo = kryo;
	}

	/**
	 * @param keysCanBeNull False if all keys are not null. This saves 1 byte per key if keyClass is set. True if it is not known
	 *           (default).
	 */
	public void setKeysCanBeNull (boolean keysCanBeNull) {
		this.keysCanBeNull = keysCanBeNull;
	}

	/**
	 * @param keyClass The concrete class of each key. This saves 1 byte per key. The serializer registered for the specified class
	 *           will be used. Set to null if the class is not known or varies per key (default).
	 */
	public void setKeyClass (Class keyClass) {
		this.keyClass = keyClass;
		keySerializer = keyClass == null ? null : kryo.getRegisteredClass(keyClass).serializer;
	}

	/**
	 * @param keyClass The concrete class of each key. This saves 1 byte per key. Set to null if the class is not known or varies
	 *           per key (default).
	 * @param keySerializer The serializer to use for each key.
	 */
	public void setKeyClass (Class keyClass, Serializer keySerializer) {
		this.keyClass = keyClass;
		this.keySerializer = keySerializer;
	}

	/**
	 * @param valueClass The concrete class of each value. This saves 1 byte per value. The serializer registered for the specified
	 *           class will be used. Set to null if the class is not known or varies per value (default).
	 */
	public void setValueClass (Class valueClass) {
		this.valueClass = valueClass;
		valueSerializer = valueClass == null ? null : kryo.getRegisteredClass(valueClass).serializer;
	}

	/**
	 * @param valueClass The concrete class of each value. This saves 1 byte per value. Set to null if the class is not known or
	 *           varies per value (default).
	 * @param valueSerializer The serializer to use for each value.
	 */
	public void setValueClass (Class valueClass, Serializer valueSerializer) {
		this.valueClass = valueClass;
		this.valueSerializer = valueSerializer;
	}

	/**
	 * @param valuesCanBeNull True if values are not null. This saves 1 byte per value if keyClass is set. False if it is not known
	 *           (default).
	 */
	public void setValuesCanBeNull (boolean valuesCanBeNull) {
		this.valuesCanBeNull = valuesCanBeNull;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		Map<Object, Object> map = (Map)object;
		int length = map.size();
		IntSerializer.put(buffer, length, true);
		if (length == 0) return;
		for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
			Entry entry = (Entry)iter.next();
			if (keySerializer != null) {
				if (keysCanBeNull)
					keySerializer.writeObject(buffer, entry.getKey());
				else
					keySerializer.writeObjectData(buffer, entry.getKey());
			} else
				kryo.writeClassAndObject(buffer, entry.getKey());
			if (valueSerializer != null) {
				if (valuesCanBeNull)
					valueSerializer.writeObject(buffer, entry.getValue());
				else
					valueSerializer.writeObjectData(buffer, entry.getValue());
			} else
				kryo.writeClassAndObject(buffer, entry.getValue());
		}
		if (TRACE) trace("kryo", "Wrote map: " + object);
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		Map map = (Map)newInstance(type);
		int length = IntSerializer.get(buffer, true);
		if (length == 0) return (T)map;
		for (int i = 0; i < length; i++) {
			Object key;
			if (keySerializer != null) {
				if (keysCanBeNull)
					key = keySerializer.readObject(buffer, keyClass);
				else
					key = keySerializer.readObjectData(buffer, keyClass);
			} else
				key = kryo.readClassAndObject(buffer);
			Object value;
			if (valueSerializer != null) {
				if (valuesCanBeNull)
					value = valueSerializer.readObject(buffer, valueClass);
				else
					value = valueSerializer.readObjectData(buffer, valueClass);
			} else
				value = kryo.readClassAndObject(buffer);
			map.put(key, value);
		}
		if (TRACE) trace("kryo", "Read map: " + map);
		return (T)map;
	}
}
