
package com.esotericsoftware.kryo.serializers;

import java.util.ArrayList;
import java.util.Collection;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import static com.esotericsoftware.minlog.Log.*;

/** Serializes objects that implement the {@link Collection} interface.
 * <p>
 * With the default constructor, a collection requires a 1-3 byte header and an extra 2-3 bytes is written for each element in the
 * collection. The alternate constructor can be used to improve efficiency to match that of using an array instead of a
 * collection.
 * @author Nathan Sweet <misc@n4te.com> */
public class CollectionSerializer implements Serializer<Collection> {
	private final Kryo kryo;
	private boolean elementsCanBeNull = true;
	private Serializer serializer;
	private Class elementClass;
	private Integer length;

	public CollectionSerializer (Kryo kryo) {
		this.kryo = kryo;
	}

	/** @param elementsCanBeNull False if all elements are not null. This saves 1 byte per element if elementClass is set. True if it
	 *           is not known (default). */
	public void setElementsCanBeNull (boolean elementsCanBeNull) {
		this.elementsCanBeNull = elementsCanBeNull;
	}

	/** @param elementClass The concrete class of each element. This saves 1-2 bytes per element. The serializer registered for the
	 *           specified class will be used. Set to null if the class is not known or varies per element (default). */
	public void setElementClass (Class elementClass) {
		this.elementClass = elementClass;
		this.serializer = elementClass == null ? null : kryo.getRegistration(elementClass).getSerializer();
	}

	/** Sets the number of objects in the collection. Saves 1-2 bytes. */
	public void setLength (int length) {
		this.length = length;
	}

	/** @param elementClass The concrete class of each element. This saves 1-2 bytes per element. Set to null if the class is not
	 *           known or varies per element (default).
	 * @param serializer The serializer to use for each element. */
	public void setElementClass (Class elementClass, Serializer serializer) {
		this.elementClass = elementClass;
		this.serializer = serializer;
	}

	public void write (Kryo kryo, Output output, Collection object) {
		Collection collection = (Collection)object;
		int length;
		if (this.length != null)
			length = this.length;
		else {
			length = collection.size();
			output.writeInt(length, true);
		}
		if (length == 0) return;
		if (serializer != null) {
			if (elementsCanBeNull) {
				for (Object element : collection)
					kryo.writeObjectOrNull(output, element, serializer);
			} else {
				for (Object element : collection)
					kryo.writeObject(output, element, serializer);
			}
		} else {
			for (Object element : collection)
				kryo.writeClassAndObject(output, element);
		}
		if (TRACE) trace("kryo", "Wrote collection: " + object);
	}

	public Collection read (Kryo kryo, Input input, Class<Collection> type) {
		int length;
		if (this.length != null)
			length = this.length;
		else
			length = input.readInt(true);
		Collection collection;
		if ((Class)type == ArrayList.class)
			collection = new ArrayList(length);
		else
			collection = (Collection)newInstance(kryo, input, type);
		if (length == 0) return collection;
		if (serializer != null) {
			if (elementsCanBeNull) {
				for (int i = 0; i < length; i++)
					collection.add(kryo.readObjectOrNull(input, elementClass, serializer));
			} else {
				for (int i = 0; i < length; i++)
					collection.add(kryo.readObject(input, elementClass, serializer));
			}
		} else {
			for (int i = 0; i < length; i++)
				collection.add(kryo.readClassAndObject(input));
		}
		if (TRACE) trace("kryo", "Read collection: " + collection);
		return collection;
	}

	/** Instance creation can be customized by overridding this method. The default implementaion calls
	 * {@link Kryo#newInstance(Class)}. */
	public <T> T newInstance (Kryo kryo, Input input, Class<T> type) {
		return kryo.newInstance(type);
	}
}
