
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

import com.esotericsoftware.kryo.CustomSerialization;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.SerializationException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.Kryo.RegisteredClass;

// BOZO - Rework getField method. Javadoc.
// BOZO - Change passing class to Serializer constructors to serializer, or both.
// BOZO - Move constructor args to setters for cleanliness?

/**
 * Serializes objects using direct field assignment. This is the most efficient mechanism for serializing objects, often as good
 * as {@link CustomSerialization}. FieldSerializer is many times smaller and faster than Java serialization.
 * <p>
 * FieldSerializer does not write header data, only the object data is stored. If the type of a field is not final (note
 * primitives are final) then an extra byte is written for that field.
 * @see Serializer
 * @see Kryo#register(Class, Serializer)
 * @author Nathan Sweet <misc@n4te.com>
 */
public class FieldSerializer extends Serializer {
	private final Kryo kryo;
	private final HashMap<Class, CachedField[]> fieldCache = new HashMap();
	private boolean fieldsCanBeNull = true, setFieldsAsAccessible = true;

	public FieldSerializer (Kryo kryo) {
		this.kryo = kryo;
	}

	/**
	 * Sets the default value for {@link CachedField#setCanBeNull(boolean)}. Should not be called after any objects are serialized
	 * or {@link #getField(Class, String)} is used.
	 * @param fieldsCanBeNull False if none of the fields are null. Saves 1 byte per field. True if it is not known (default).
	 */
	public void setFieldsCanBeNull (boolean fieldsCanBeNull) {
		this.fieldsCanBeNull = fieldsCanBeNull;
	}

	/**
	 * Controls which fields are accessed. Should not be called after any objects are serialized or
	 * {@link #getField(Class, String)} is used.
	 * @param setFieldsAsAccessible If true, all non-transient fields (inlcuding private fields) will be serialized and
	 *           {@link Field#setAccessible(boolean) set as accessible} (default). If false, only fields in the public API will be
	 *           serialized.
	 */
	public void setFieldsAsAccessible (boolean setFieldsAsAccessible) {
		this.setFieldsAsAccessible = setFieldsAsAccessible;
	}

	private CachedField[] cache (Class type) {
		if (type.isInterface()) return new CachedField[0]; // No fields to serialize.
		ArrayList<Field> allFields = new ArrayList();
		Class nextClass = type;
		while (nextClass != Object.class) {
			Collections.addAll(allFields, nextClass.getDeclaredFields());
			nextClass = nextClass.getSuperclass();
		}
		PriorityQueue<CachedField> cachedFields = new PriorityQueue(Math.max(1, allFields.size()), new Comparator<CachedField>() {
			public int compare (CachedField o1, CachedField o2) {
				// Fields are sorted by alpha so the order of the data is known.
				return o1.field.getName().compareTo(o2.field.getName());
			}
		});
		for (int i = 0, n = allFields.size(); i < n; i++) {
			Field field = allFields.get(i);
			int modifiers = field.getModifiers();
			if (Modifier.isTransient(modifiers)) continue;
			if (Modifier.isFinal(modifiers)) continue;
			if (Modifier.isStatic(modifiers)) continue;
			if (field.isSynthetic()) continue;
			if (setFieldsAsAccessible)
				field.setAccessible(true);
			else if (Modifier.isPrivate(modifiers)) {
				continue;
			}

			CachedField cachedField = new CachedField();
			cachedField.field = field;
			if (fieldsCanBeNull)
				cachedField.canBeNull = !field.isAnnotationPresent(NotNull.class);
			else
				cachedField.canBeNull = false;

			// Always use the same serializer for this field if the field's class is final.
			Class fieldClass = field.getType();
			if (Modifier.isFinal(fieldClass.getModifiers())) {
				cachedField.fieldClass = fieldClass;
				cachedField.serializer = kryo.getRegisteredClass(fieldClass).serializer;
			}

			cachedFields.add(cachedField);
		}

		int n = cachedFields.size();
		CachedField[] cachedFieldArray = new CachedField[n];
		for (int i = 0; i < n; i++)
			cachedFieldArray[i] = cachedFields.poll();
		fieldCache.put(type, cachedFieldArray);
		return cachedFieldArray;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		Class type = object.getClass();
		try {
			CachedField[] fields = fieldCache.get(type);
			if (fields == null) fields = cache(type);
			for (int i = 0, n = fields.length; i < n; i++) {
				CachedField cachedField = fields[i];
				if (TRACE) trace("kryo", "Writing field: " + cachedField + " (" + type.getName() + ")");

				Object value = cachedField.field.get(object);

				Serializer serializer = cachedField.serializer;
				if (cachedField.fieldClass == null) {
					if (value == null) {
						kryo.writeClass(buffer, null);
						continue;
					}
					RegisteredClass registeredClass = kryo.writeClass(buffer, value.getClass());
					if (serializer == null) serializer = registeredClass.serializer;
					serializer.writeObjectData(buffer, value);
				} else {
					if (!cachedField.canBeNull)
						serializer.writeObjectData(buffer, value);
					else
						serializer.writeObject(buffer, value);
				}
			}
		} catch (IllegalAccessException ex) {
			throw new SerializationException("Error accessing field in class: " + type.getName(), ex);
		}
		if (TRACE) trace("kryo", "Wrote object: " + object);
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		T object = newInstance(type);
		try {
			CachedField[] fields = fieldCache.get(type);
			if (fields == null) fields = cache(type);
			for (int i = 0, n = fields.length; i < n; i++) {
				CachedField cachedField = fields[i];
				if (TRACE) trace("kryo", "Reading field: " + cachedField + " (" + type.getName() + ")");

				Object value;

				Class concreteType = cachedField.fieldClass;
				Serializer serializer = cachedField.serializer;
				if (concreteType == null) {
					RegisteredClass registeredClass = kryo.readClass(buffer);
					if (registeredClass == null)
						value = null;
					else {
						concreteType = registeredClass.type;
						if (serializer == null) serializer = registeredClass.serializer;
						value = serializer.readObjectData(buffer, concreteType);
					}
				} else {
					if (!cachedField.canBeNull)
						value = serializer.readObjectData(buffer, concreteType);
					else
						value = serializer.readObject(buffer, concreteType);
				}

				cachedField.field.set(object, value);
			}
		} catch (IllegalAccessException ex) {
			throw new SerializationException("Error accessing field in class: " + type.getName(), ex);
		}
		if (TRACE) trace("kryo", "Read object: " + object);
		return object;
	}

	/**
	 * Allows specific fields to be optimized.
	 */
	public CachedField getField (Class type, String fieldName) {
		CachedField[] fields = fieldCache.get(type);
		if (fields == null) fields = cache(type);
		for (CachedField cachedField : fields)
			if (cachedField.field.getName().equals(fieldName)) return cachedField;
		throw new IllegalArgumentException("Field \"" + fieldName + "\" not found on class: " + type.getName());
	}

	/**
	 * Controls how a field will be serialized.
	 */
	public class CachedField {
		Field field;
		Class fieldClass;
		Serializer serializer;
		boolean canBeNull;

		/**
		 * @param fieldClass The concrete class of the values for this field. This saves 1-2 bytes. The serializer registered for
		 *           the specified class will be used. Set to null if the field type in the class definition is final or the values
		 *           for this field vary (default).
		 */
		public void setClass (Class fieldClass) {
			this.fieldClass = fieldClass;
			this.serializer = fieldClass == null ? null : kryo.getRegisteredClass(fieldClass).serializer;
		}

		/**
		 * @param fieldClass The concrete class of the values for this field. This saves 1-2 bytes. The serializer registered for
		 *           the specified class will be used. Set to null if the field type in the class definition is final or the values
		 *           for this field vary (default).
		 */
		public void setClass (Class fieldClass, Serializer serializer) {
			this.fieldClass = fieldClass;
			this.serializer = serializer;
		}

		public void setCanBeNull (boolean canBeNull) {
			this.canBeNull = canBeNull;
		}

		public String toString () {
			return field.getName();
		}
	}
}
