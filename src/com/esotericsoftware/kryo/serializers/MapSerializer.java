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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Generics.GenericType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/** Serializes objects that implement the {@link Map} interface.
 * <p>
 * With the default constructor, a map requires a 1-3 byte header and an extra 4 bytes is written for each key/value pair.
 * @author Nathan Sweet */
public class MapSerializer<T extends Map> extends Serializer<T> {
	private Class keyClass, valueClass;
	private Serializer keySerializer, valueSerializer;
	private boolean keysCanBeNull = true, valuesCanBeNull = true;

	public MapSerializer () {
		setAcceptsNull(true);
	}

	/** @param keysCanBeNull False if all keys are not null. This saves 1 byte per key if keyClass is set. True if it is not known
	 *           (default). */
	public void setKeysCanBeNull (boolean keysCanBeNull) {
		this.keysCanBeNull = keysCanBeNull;
	}

	/** The concrete class of the keys for this map, or null if it is not known. This saves 1-2 bytes. Only set to a non-null value
	 * if the keys for this map are known to be of the specified type (or null). */
	public void setKeyClass (Class keyClass) {
		this.keyClass = keyClass;
	}

	public Class getKeyClass () {
		return keyClass;
	}

	/** Sets both {@link #setKeyClass(Class)} and {@link #setKeySerializer(Serializer)}. */
	public void setKeyClass (Class keyClass, Serializer keySerializer) {
		this.keyClass = keyClass;
		this.keySerializer = keySerializer;
	}

	/** The serializer to be used for the keys in this map, or null to use the serializer registered with {@link Kryo} for the
	 * type. Default is null. */
	public void setKeySerializer (Serializer keySerializer) {
		this.keySerializer = keySerializer;
	}

	public Serializer getKeySerializer () {
		return this.keySerializer;
	}

	/** The concrete class of the values for this map, or null if it is not known. This saves 1-2 bytes. Only set to a non-null
	 * value if the values for this map are known to be of the specified type (or null). */
	public void setValueClass (Class valueClass) {
		this.valueClass = valueClass;
	}

	public Class getValueClass () {
		return valueClass;
	}

	/** Sets both {@link #setValueClass(Class)} and {@link #setValueSerializer(Serializer)}. */
	public void setValueClass (Class valueClass, Serializer valueSerializer) {
		this.valueClass = valueClass;
		this.valueSerializer = valueSerializer;
	}

	/** The serializer to be used for this field, or null to use the serializer registered with {@link Kryo} for the type. Default
	 * is null. */
	public void setValueSerializer (Serializer valueSerializer) {
		this.valueSerializer = valueSerializer;
	}

	public Serializer getValueSerializer () {
		return this.valueSerializer;
	}

	/** @param valuesCanBeNull True if values are not null. This saves 1 byte per value if keyClass is set. False if it is not
	 *           known (default). */
	public void setValuesCanBeNull (boolean valuesCanBeNull) {
		this.valuesCanBeNull = valuesCanBeNull;
	}

	public void write (Kryo kryo, Output output, T map) {
		if (map == null) {
			output.writeByte(0);
			return;
		}

		int size = map.size();
		if (size == 0) {
			output.writeByte(1);
			writeHeader(kryo, output, map);
			return;
		}

		output.writeVarInt(size + 1, true);
		writeHeader(kryo, output, map);

		Serializer keySerializer = this.keySerializer, valueSerializer = this.valueSerializer;

		GenericType[] genericTypes = kryo.getGenerics().nextGenericTypes();
		if (genericTypes != null) {
			if (keySerializer == null) {
				Class keyType = genericTypes[0].resolve(kryo.getGenerics());
				if (keyType != null && kryo.isFinal(keyType)) keySerializer = kryo.getSerializer(keyType);
			}
			if (valueSerializer == null) {
				Class valueType = genericTypes[1].resolve(kryo.getGenerics());
				if (valueType != null && kryo.isFinal(valueType)) valueSerializer = kryo.getSerializer(valueType);
			}
		}

		for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
			Entry entry = (Entry)iter.next();
			if (genericTypes != null) kryo.getGenerics().pushGenericType(genericTypes[0]);
			if (keySerializer != null) {
				if (keysCanBeNull)
					kryo.writeObjectOrNull(output, entry.getKey(), keySerializer);
				else
					kryo.writeObject(output, entry.getKey(), keySerializer);
			} else
				kryo.writeClassAndObject(output, entry.getKey());
			if (genericTypes != null) kryo.getGenerics().popGenericType();
			if (valueSerializer != null) {
				if (valuesCanBeNull)
					kryo.writeObjectOrNull(output, entry.getValue(), valueSerializer);
				else
					kryo.writeObject(output, entry.getValue(), valueSerializer);
			} else
				kryo.writeClassAndObject(output, entry.getValue());
		}
		kryo.getGenerics().popGenericType();
	}

	/** Can be overidden to write data needed for {@link #create(Kryo, Input, Class, int)}. The default implementation does
	 * nothing. */
	protected void writeHeader (Kryo kryo, Output output, T map) {
	}

	/** Used by {@link #read(Kryo, Input, Class)} to create the new object. This can be overridden to customize object creation, eg
	 * to call a constructor with arguments. The default implementation uses {@link Kryo#newInstance(Class)} with a special case
	 * for HashMap. */
	protected T create (Kryo kryo, Input input, Class<? extends T> type, int size) {
		if (type == HashMap.class) {
			if (size < 3)
				size++;
			else if (size < 1073741824) // Max POT.
				size = (int)(size / 0.75f + 1); // 0.75 is the default load factor.
			return (T)new HashMap(size);
		}
		return kryo.newInstance(type);
	}

	public T read (Kryo kryo, Input input, Class<? extends T> type) {
		int length = input.readVarInt(true);
		if (length == 0) return null;
		length--;

		T map = create(kryo, input, type, length);
		kryo.reference(map);
		if (length == 0) return map;

		Class keyClass = this.keyClass;
		Class valueClass = this.valueClass;
		Serializer keySerializer = this.keySerializer, valueSerializer = this.valueSerializer;

		GenericType[] genericTypes = kryo.getGenerics().nextGenericTypes();
		if (genericTypes != null) {
			if (keySerializer == null) {
				Class genericClass = genericTypes[0].resolve(kryo.getGenerics());
				if (genericClass != null && kryo.isFinal(genericClass)) {
					keySerializer = kryo.getSerializer(genericClass);
					keyClass = genericClass;
				}
			}
			if (valueSerializer == null) {
				Class genericClass = genericTypes[1].resolve(kryo.getGenerics());
				if (genericClass != null && kryo.isFinal(genericClass)) {
					valueSerializer = kryo.getSerializer(genericClass);
					valueClass = genericClass;
				}
			}
		}

		for (int i = 0; i < length; i++) {
			Object key;
			if (genericTypes != null) kryo.getGenerics().pushGenericType(genericTypes[0]);
			if (keySerializer != null) {
				if (keysCanBeNull)
					key = kryo.readObjectOrNull(input, keyClass, keySerializer);
				else
					key = kryo.readObject(input, keyClass, keySerializer);
			} else
				key = kryo.readClassAndObject(input);
			if (genericTypes != null) kryo.getGenerics().popGenericType();
			Object value;
			if (valueSerializer != null) {
				if (valuesCanBeNull)
					value = kryo.readObjectOrNull(input, valueClass, valueSerializer);
				else
					value = kryo.readObject(input, valueClass, valueSerializer);
			} else
				value = kryo.readClassAndObject(input);
			map.put(key, value);
		}
		kryo.getGenerics().popGenericType();
		return map;
	}

	protected T createCopy (Kryo kryo, T original) {
		return (T)kryo.newInstance(original.getClass());
	}

	public T copy (Kryo kryo, T original) {
		T copy = createCopy(kryo, original);
		for (Iterator iter = original.entrySet().iterator(); iter.hasNext();) {
			Entry entry = (Entry)iter.next();
			copy.put(kryo.copy(entry.getKey()), kryo.copy(entry.getValue()));
		}
		return copy;
	}

	/** Annotates a {@link Map} field with {@link MapSerializer} settings for {@link FieldSerializer}. */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface BindMap {
		/** @see MapSerializer#setKeyClass(Class) */
		Class keyClass() default Object.class;

		/** The key serializer class, which will be created using the {@link #keySerializerFactory()}. Can be omitted if the
		 * serializer factory knows what type of serializer to create.
		 * @see MapSerializer#setKeySerializer(Serializer) */
		Class<? extends Serializer> keySerializer() default Serializer.class;

		/** The factory used to create the key serializer. */
		Class<? extends SerializerFactory> keySerializerFactory() default SerializerFactory.class;

		/** @see MapSerializer#setValueClass(Class) */
		Class valueClass() default Object.class;

		/** The value serializer class, which will be created using the {@link #valueSerializerFactory()}. Can be omitted if the
		 * serializer factory knows what type of serializer to create.
		 * @see MapSerializer#setValueSerializer(Serializer) */
		Class<? extends Serializer> valueSerializer() default Serializer.class;

		/** The factory used to create the value serializer. */
		Class<? extends SerializerFactory> valueSerializerFactory() default SerializerFactory.class;

		/** Indicates if keys can be null
		 * @return true, if keys can be null */
		boolean keysCanBeNull() default true;

		/** Indicates if values can be null
		 * @return true, if values can be null */
		boolean valuesCanBeNull() default true;
	}
}
