
package com.esotericsoftware.kryo.serializers;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Serializes objects that implement the {@link Collection} interface.
 * <p>
 * With the default constructor, a collection requires a 1-3 byte header and an extra 2-3 bytes is written for each element in the
 * collection. The alternate constructor can be used to improve efficiency to match that of using an array instead of a
 * collection.
 * @author Nathan Sweet <misc@n4te.com> */
public class CollectionSerializer extends Serializer<Collection> {
	private boolean elementsCanBeNull = true;
	private Serializer serializer;
	private Class elementClass;
	private Class genericType;

	public CollectionSerializer () {
	}

	/** @see #setElementClass(Class, Serializer) */
	public CollectionSerializer (Class elementClass, Serializer serializer) {
		setElementClass(elementClass, serializer);
	}

	/** @see #setElementClass(Class, Serializer)
	 * @see #setElementsCanBeNull(boolean) */
	public CollectionSerializer (Class elementClass, Serializer serializer, boolean elementsCanBeNull) {
		setElementClass(elementClass, serializer);
		this.elementsCanBeNull = elementsCanBeNull;
	}

	/** @param elementsCanBeNull False if all elements are not null. This saves 1 byte per element if elementClass is set. True if it
	 *           is not known (default). */
	public void setElementsCanBeNull (boolean elementsCanBeNull) {
		this.elementsCanBeNull = elementsCanBeNull;
	}

	/** @param elementClass The concrete class of each element. This saves 1-2 bytes per element. Set to null if the class is not
	 *           known or varies per element (default).
	 * @param serializer The serializer to use for each element. */
	public void setElementClass (Class elementClass, Serializer serializer) {
		this.elementClass = elementClass;
		this.serializer = serializer;
	}

	public void setGenerics (Kryo kryo, Type[] generics) {
		if (generics == null)
			genericType = null;
		else {
			Class type = (Class)generics[0];
			if (kryo.isFinal(type)) genericType = type;
		}
	}

	public void write (Kryo kryo, Output output, Collection object) {
		Collection collection = (Collection)object;
		int length = collection.size();
		output.writeInt(length, true);
		if (length == 0) return;
		Serializer serializer = this.serializer;
		if (genericType != null) {
			if (serializer == null) serializer = kryo.getSerializer(genericType);
			genericType = null;
		}
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
	}

	public Collection read (Kryo kryo, Input input, Class<Collection> type) {
		Collection collection = kryo.newInstance(type);
		kryo.reference(collection);
		int length = input.readInt(true);
		if (collection instanceof ArrayList) ((ArrayList)collection).ensureCapacity(length);
		Class elementClass = this.elementClass;
		Serializer serializer = this.serializer;
		if (genericType != null) {
			if (serializer == null) {
				elementClass = genericType;
				serializer = kryo.getSerializer(genericType);
			}
			genericType = null;
		}
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
		return collection;
	}

	public Collection copy (Kryo kryo, Collection original) {
		Collection copy = kryo.newInstance(original.getClass());
		kryo.reference(copy);
		for (Object element : original)
			copy.add(kryo.copy(element));
		return copy;
	}
}
