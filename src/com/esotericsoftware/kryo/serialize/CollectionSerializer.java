
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.nio.ByteBuffer;
import java.util.Collection;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

/**
 * Serializes objects that implement the {@link Collection} interface.
 * <p>
 * With the default constructor, a collection requires a 1-3 byte header and an extra 2-3 bytes is written for each element in the
 * collection. The alternate constructor can be used to improve efficiency to match that of using an array instead of a
 * collection.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class CollectionSerializer extends Serializer {
	private final Kryo kryo;
	private boolean elementsCanBeNull = true;
	private Serializer serializer;
	private Class elementClass;

	public CollectionSerializer (Kryo kryo) {
		this.kryo = kryo;
	}

	/**
	 * @param elementsCanBeNull False if all elements are not null. This saves 1 byte per element if elementClass is set. True if
	 *           it is not known (default).
	 */
	public void setElementsCanBeNull (boolean elementsCanBeNull) {
		this.elementsCanBeNull = elementsCanBeNull;
	}

	/**
	 * @param elementClass The concrete class of each element. This saves 1-2 bytes per element. The serializer registered for the
	 *           specified class will be used. Set to null if the class is not known or varies per element (default).
	 */
	public void setElementClass (Class elementClass) {
		this.elementClass = elementClass;
		this.serializer = elementClass == null ? null : kryo.getRegisteredClass(elementClass).serializer;
	}

	/**
	 * @param elementClass The concrete class of each element. This saves 1-2 bytes per element. Set to null if the class is not
	 *           known or varies per element (default).
	 * @param serializer The serializer to use for each element.
	 */
	public void setElementClass (Class elementClass, Serializer serializer) {
		this.elementClass = elementClass;
		this.serializer = serializer;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		Collection collection = (Collection)object;
		int length = collection.size();
		IntSerializer.put(buffer, length, true);
		if (length == 0) return;
		if (serializer != null) {
			if (elementsCanBeNull) {
				for (Object element : collection)
					serializer.writeObject(buffer, element);
			} else {
				for (Object element : collection)
					serializer.writeObjectData(buffer, element);
			}
		} else {
			for (Object element : collection)
				kryo.writeClassAndObject(buffer, element);
		}
		if (TRACE) trace("kryo", "Wrote collection: " + object);
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		int length = IntSerializer.get(buffer, true);
		Collection collection = (Collection)newInstance(type);
		if (length == 0) return (T)collection;
		if (serializer != null) {
			if (elementsCanBeNull) {
				for (int i = 0; i < length; i++)
					collection.add(serializer.readObject(buffer, elementClass));
			} else {
				for (int i = 0; i < length; i++)
					collection.add(serializer.readObjectData(buffer, elementClass));
			}
		} else {
			for (int i = 0; i < length; i++)
				collection.add(kryo.readClassAndObject(buffer));
		}
		if (TRACE) trace("kryo", "Read collection: " + collection);
		return (T)collection;
	}
}
