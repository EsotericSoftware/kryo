/* Copyright (c) 2008-2025, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryo.serializers;

import static com.esotericsoftware.kryo.Kryo.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Serializes objects that implement the {@link Collection} interface.
 * <p>
 * With the default constructor, a collection requires a 1-3 byte header and an extra 2-3 bytes is written for each element in the
 * collection.
 * @author Nathan Sweet */
public class CollectionSerializer<T extends Collection> extends Serializer<T> {
	private boolean elementsCanBeNull = true;
	private Serializer elementSerializer;
	private Class elementClass;

	public CollectionSerializer () {
		setAcceptsNull(true);
	}

	/** @param elementsCanBeNull False if all elements are not null. This saves 1 byte per element if elementClass is set. True if
	 *           it is not known (default). */
	public void setElementsCanBeNull (boolean elementsCanBeNull) {
		this.elementsCanBeNull = elementsCanBeNull;
	}

	/** The concrete class of the collection elements, or null if it is not known. This saves 1-2 bytes per element. Only set to a
	 * non-null value if the elements in the collection are known to all be instances of this class (or null). */
	public void setElementClass (Class elementClass) {
		this.elementClass = elementClass;
	}

	public Class getElementClass () {
		return elementClass;
	}

	/** Sets both {@link #setElementClass(Class)} and {@link #setElementSerializer(Serializer)}. */
	public void setElementClass (Class elementClass, Serializer serializer) {
		this.elementClass = elementClass;
		this.elementSerializer = serializer;
	}

	/** The serializer to be used for elements in collection, or null to use the serializer registered with {@link Kryo} for each
	 * element's type. Default is null. */
	public void setElementSerializer (Serializer elementSerializer) {
		this.elementSerializer = elementSerializer;
	}

	public Serializer getElementSerializer () {
		return this.elementSerializer;
	}

	public void write (Kryo kryo, Output output, T collection) {
		if (collection == null) {
			output.writeByte(NULL);
			return;
		}

		int length = collection.size();
		if (length == 0) {
			output.writeByte(1);
			writeHeader(kryo, output, collection);
			return;
		}

		boolean elementsCanBeNull = this.elementsCanBeNull;
		Serializer elementSerializer = this.elementSerializer;
		if (elementSerializer == null) {
			Class genericClass = kryo.getGenerics().nextGenericClass();
			if (genericClass != null && kryo.isFinal(genericClass)) elementSerializer = kryo.getSerializer(genericClass);
		}
		try {
			outer:
			if (elementSerializer != null) {
				inner:
				if (elementsCanBeNull) {
					for (Object element : collection) {
						if (element == null) {
							output.writeVarIntFlag(true, length + 1, true);
							break inner;
						}
					}
					output.writeVarIntFlag(false, length + 1, true);
					elementsCanBeNull = false;
				} else
					output.writeVarInt(length + 1, true);
				writeHeader(kryo, output, collection);
			} else { // Serializer is unknown, check if all elements are the same type.
				Class elementType = null;
				boolean hasNull = false;
				for (Object element : collection) {
					if (element == null)
						hasNull = true;
					else if (elementType == null)
						elementType = element.getClass();
					else if (element.getClass() != elementType) { // Elements are different types.
						output.writeVarIntFlag(false, length + 1, true);
						writeHeader(kryo, output, collection);
						break outer;
					}
				}
				output.writeVarIntFlag(true, length + 1, true);
				writeHeader(kryo, output, collection);
				if (elementType == null) { // All elements are null.
					output.writeByte(NULL);
					return;
				}
				// All elements are the same class.
				kryo.writeClass(output, elementType);
				elementSerializer = kryo.getSerializer(elementType);
				if (elementsCanBeNull) {
					output.writeBoolean(hasNull);
					elementsCanBeNull = hasNull;
				}
			}

			if (elementSerializer != null) {
				if (elementsCanBeNull) {
					for (Object element : collection)
						kryo.writeObjectOrNull(output, element, elementSerializer);
				} else {
					for (Object element : collection)
						kryo.writeObject(output, element, elementSerializer);
				}
			} else {
				for (Object element : collection)
					kryo.writeClassAndObject(output, element);
			}
		} finally {
			kryo.getGenerics().popGenericType();
		}
	}

	/** Can be overidden to write data needed for {@link #create(Kryo, Input, Class, int)}. The default implementation does
	 * nothing. */
	protected void writeHeader (Kryo kryo, Output output, T collection) {
	}

	/** Used by {@link #read(Kryo, Input, Class)} to create the new object. This can be overridden to customize object creation (eg
	 * to call a constructor with arguments), optionally reading bytes written in {@link #writeHeader(Kryo, Output, Collection)}.
	 * The default implementation uses {@link Kryo#newInstance(Class)} with special cases for ArrayList and HashSet. */
	protected T create (Kryo kryo, Input input, Class<? extends T> type, int size) {
		if (type == ArrayList.class) return (T)new ArrayList<>(size);
		if (type == HashSet.class) return (T)new HashSet<>(Math.max((int)(size / 0.75f) + 1, 16));
		T collection = kryo.newInstance(type);
		if (collection instanceof ArrayList) ((ArrayList)collection).ensureCapacity(size);
		return collection;
	}

	public T read (Kryo kryo, Input input, Class<? extends T> type) {
		Class elementClass = this.elementClass;
		Serializer elementSerializer = this.elementSerializer;
		if (elementSerializer == null) {
			Class genericClass = kryo.getGenerics().nextGenericClass();
			if (genericClass != null && kryo.isFinal(genericClass)) {
				elementSerializer = kryo.getSerializer(genericClass);
				elementClass = genericClass;
			}
		}

		try {
			T collection;
			int length;
			boolean elementsCanBeNull = this.elementsCanBeNull;
			if (elementSerializer != null) {
				if (elementsCanBeNull) {
					elementsCanBeNull = input.readVarIntFlag();
					length = input.readVarIntFlag(true);
				} else
					length = input.readVarInt(true);
				if (length == 0) return null;

				length--;
				collection = create(kryo, input, type, length);
				kryo.reference(collection);

				if (length == 0) return collection;
			} else {
				boolean sameType = input.readVarIntFlag();
				length = input.readVarIntFlag(true);
				if (length == 0) return null;

				length--;
				collection = create(kryo, input, type, length);
				kryo.reference(collection);

				if (length == 0) return collection;

				if (sameType) {
					Registration registration = kryo.readClass(input);
					if (registration == null) { // All elements are null.
						for (int i = 0; i < length; i++)
							collection.add(null);
						kryo.getGenerics().popGenericType();
						return collection;
					}
					elementClass = registration.getType();
					elementSerializer = kryo.getSerializer(elementClass);
					if (elementsCanBeNull) elementsCanBeNull = input.readBoolean();
				}
			}

			if (elementSerializer != null) {
				if (elementsCanBeNull) {
					for (int i = 0; i < length; i++)
						collection.add(kryo.readObjectOrNull(input, elementClass, elementSerializer));
				} else {
					for (int i = 0; i < length; i++)
						collection.add(kryo.readObject(input, elementClass, elementSerializer));
				}
			} else {
				for (int i = 0; i < length; i++)
					collection.add(kryo.readClassAndObject(input));
			}
			return collection;
		} finally {
			kryo.getGenerics().popGenericType();
		}
	}

	/** Used by {@link #copy(Kryo, Collection)} to create the new object. This can be overridden to customize object creation, eg
	 * to call a constructor with arguments. The default implementation uses {@link Kryo#newInstance(Class)}. */
	protected T createCopy (Kryo kryo, T original) {
		return (T)kryo.newInstance(original.getClass());
	}

	public T copy (Kryo kryo, T original) {
		T copy = createCopy(kryo, original);
		kryo.reference(copy);
		for (Object element : original)
			copy.add(kryo.copy(element));
		return copy;
	}

	/** Annotates a {@link Collection} field with {@link CollectionSerializer} settings for {@link FieldSerializer}. */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface BindCollection {
		/** @see CollectionSerializer#setElementClass(Class) */
		Class elementClass() default Object.class;

		/** The element serializer class, which will be created using the {@link #elementSerializerFactory()}. Can be omitted if the
		 * serializer factory knows what type of serializer to create.
		 * @see CollectionSerializer#setElementSerializer(Serializer) */
		Class<? extends Serializer> elementSerializer() default Serializer.class;

		/** The factory used to create the element serializer. */
		Class<? extends SerializerFactory> elementSerializerFactory() default SerializerFactory.class;

		/** @see CollectionSerializer#setElementsCanBeNull(boolean) */
		boolean elementsCanBeNull() default true;
	}
}
