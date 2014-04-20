
package com.esotericsoftware.kryo.serializers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Serializes objects that implement the {@link Map} interface.
 * <p>
 * With the default constructor, a map requires a 1-3 byte header and an extra 4 bytes is written for each key/value pair.
 * @author Nathan Sweet <misc@n4te.com> */
public class MapSerializer extends Serializer<Map> {
	private Class keyClass, valueClass;
	private Serializer keySerializer, valueSerializer;
	private boolean keysCanBeNull = true, valuesCanBeNull = true;
	private Class keyGenericType, valueGenericType;

	/** @param keysCanBeNull False if all keys are not null. This saves 1 byte per key if keyClass is set. True if it is not known
	 *           (default). */
	public void setKeysCanBeNull (boolean keysCanBeNull) {
		this.keysCanBeNull = keysCanBeNull;
	}

	/** @param keyClass The concrete class of each key. This saves 1 byte per key. Set to null if the class is not known or varies
	 *           per key (default).
	 * @param keySerializer The serializer to use for each key. */
	public void setKeyClass (Class keyClass, Serializer keySerializer) {
		this.keyClass = keyClass;
		this.keySerializer = keySerializer;
	}

	/** @param valueClass The concrete class of each value. This saves 1 byte per value. Set to null if the class is not known or
	 *           varies per value (default).
	 * @param valueSerializer The serializer to use for each value. */
	public void setValueClass (Class valueClass, Serializer valueSerializer) {
		this.valueClass = valueClass;
		this.valueSerializer = valueSerializer;
	}

	/** @param valuesCanBeNull True if values are not null. This saves 1 byte per value if keyClass is set. False if it is not known
	 *           (default). */
	public void setValuesCanBeNull (boolean valuesCanBeNull) {
		this.valuesCanBeNull = valuesCanBeNull;
	}

	public void setGenerics (Kryo kryo, Class[] generics) {
		keyGenericType = null;
		valueGenericType = null;
		
		if (generics != null && generics.length > 0) {
			if (generics[0] != null && kryo.isFinal(generics[0])) keyGenericType = generics[0];
			if (generics.length > 1 && generics[1] != null && kryo.isFinal(generics[1])) valueGenericType = generics[1];
		}
	}

	public void write (Kryo kryo, Output output, Map map) {
		int length = map.size();
		output.writeInt(length, true);

		Serializer keySerializer = this.keySerializer;
		if (keyGenericType != null) {
			if (keySerializer == null) keySerializer = kryo.getSerializer(keyGenericType);
			keyGenericType = null;
		}
		Serializer valueSerializer = this.valueSerializer;
		if (valueGenericType != null) {
			if (valueSerializer == null) valueSerializer = kryo.getSerializer(valueGenericType);
			valueGenericType = null;
		}

		for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
			Entry entry = (Entry)iter.next();
			if (keySerializer != null) {
				if (keysCanBeNull)
					kryo.writeObjectOrNull(output, entry.getKey(), keySerializer);
				else
					kryo.writeObject(output, entry.getKey(), keySerializer);
			} else
				kryo.writeClassAndObject(output, entry.getKey());
			if (valueSerializer != null) {
				if (valuesCanBeNull)
					kryo.writeObjectOrNull(output, entry.getValue(), valueSerializer);
				else
					kryo.writeObject(output, entry.getValue(), valueSerializer);
			} else
				kryo.writeClassAndObject(output, entry.getValue());
		}
	}

	/** Used by {@link #read(Kryo, Input, Class)} to create the new object. This can be overridden to customize object creation, eg
	 * to call a constructor with arguments. The default implementation uses {@link Kryo#newInstance(Class)}. */
	protected Map create (Kryo kryo, Input input, Class<Map> type) {
		return kryo.newInstance(type);
	}

	public Map read (Kryo kryo, Input input, Class<Map> type) {
		Map map = create(kryo, input, type);
		int length = input.readInt(true);

		Class keyClass = this.keyClass;
		Class valueClass = this.valueClass;

		Serializer keySerializer = this.keySerializer;
		if (keyGenericType != null) {
			keyClass = keyGenericType;
			if (keySerializer == null) keySerializer = kryo.getSerializer(keyClass);
			keyGenericType = null;
		}
		Serializer valueSerializer = this.valueSerializer;
		if (valueGenericType != null) {
			valueClass = valueGenericType;
			if (valueSerializer == null) valueSerializer = kryo.getSerializer(valueClass);
			valueGenericType = null;
		}

		kryo.reference(map);

		for (int i = 0; i < length; i++) {
			Object key;
			if (keySerializer != null) {
				if (keysCanBeNull)
					key = kryo.readObjectOrNull(input, keyClass, keySerializer);
				else
					key = kryo.readObject(input, keyClass, keySerializer);
			} else
				key = kryo.readClassAndObject(input);
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
		return map;
	}

	protected Map createCopy (Kryo kryo, Map original) {
		return kryo.newInstance(original.getClass());
	}

	public Map copy (Kryo kryo, Map original) {
		Map copy = createCopy(kryo, original);
		for (Iterator iter = original.entrySet().iterator(); iter.hasNext();) {
			Entry entry = (Entry)iter.next();
			copy.put(kryo.copy(entry.getKey()), kryo.copy(entry.getValue()));
		}
		return copy;
	}

	/** 
	 * Used to annotate fields that are maps with specific Kryo serializers for 
	 * their keys or values. 
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface BindMap {

		/** 
		 * Serializer to be used for keys
		 * 
		 * @return the class<? extends serializer> used for keys serialization
		 */
		@SuppressWarnings("rawtypes")
		Class<? extends Serializer> keySerializer() default Serializer.class;

		/** 
		 * Serializer to be used for values
		 * 
		 * @return the class<? extends serializer> used for values serialization 
		 */
		@SuppressWarnings("rawtypes")
		Class<? extends Serializer> valueSerializer() default Serializer.class;

		/** 
		 * Class used for keys
		 * 
		 * @return the class used for keys 
		 */
		Class<?> keyClass() default Object.class;

		/** 
		 * Class used for values
		 * 
		 * @return the class used for values 
		 */
		Class<?> valueClass() default Object.class;

		/** 
		 * Indicates if keys can be null
		 * 
		 * @return true, if keys can be null 
		 */
		boolean keysCanBeNull() default true;

		/** 
		 * Indicates if values can be null
		 * 
		 * @return true, if values can be null 
		 */
		boolean valuesCanBeNull() default true;
	}
}
