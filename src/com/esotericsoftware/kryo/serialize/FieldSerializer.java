
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.log.Log.*;

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
	private boolean fieldsAreNotNull, setFieldsAsAccessible = true;

	public FieldSerializer (Kryo kryo) {
		this.kryo = kryo;
	}

	/**
	 * @param fieldsAreNotNull True if none of the fields are null. Saves 1 byte per field. False if it is not known (default).
	 * @param setFieldsAsAccessible If true, all fields (inlcuding private fields) will be serialized and
	 *           {@link Field#setAccessible(boolean) set as accessible} (default). If false, only fields in the public API will be
	 *           serialized.
	 */
	public FieldSerializer (Kryo kryo, boolean fieldsAreNotNull, boolean setFieldsAsAccessible) {
		this.kryo = kryo;
		this.fieldsAreNotNull = fieldsAreNotNull;
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
			if (fieldsAreNotNull)
				cachedField.canBeNull = false;
			else
				cachedField.canBeNull = !field.isAnnotationPresent(NotNull.class);

			// Always use the same serializer for this field if the field's class is final.
			Class fieldClass = field.getType();
			if (Modifier.isFinal(fieldClass.getModifiers())) {
				cachedField.concreteType = fieldClass;
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
				if (level <= TRACE) trace("kryo", "Writing field: " + cachedField + " (" + type.getName() + ")");
				Object value = cachedField.field.get(object);

				Serializer serializer = cachedField.serializer;
				if (cachedField.concreteType == null) {
					RegisteredClass registeredClass = kryo.writeClass(buffer, value.getClass());
					if (serializer == null) serializer = registeredClass.serializer;
				}

				if (!cachedField.canBeNull)
					serializer.writeObjectData(buffer, value);
				else
					serializer.writeObject(buffer, value);
			}
		} catch (IllegalAccessException ex) {
			throw new SerializationException("Error accessing field in class: " + type.getName(), ex);
		}
		if (level <= TRACE) trace("kryo", "Wrote object: " + object);
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		T object = newInstance(type);
		try {
			CachedField[] fields = fieldCache.get(type);
			if (fields == null) fields = cache(type);
			for (int i = 0, n = fields.length; i < n; i++) {
				CachedField cachedField = fields[i];
				if (level <= TRACE) trace("kryo", "Reading field: " + cachedField + " (" + type.getName() + ")");
				Object value;
				Field field = cachedField.field;

				Class concreteType = cachedField.concreteType;
				Serializer serializer = cachedField.serializer;
				if (concreteType == null) {
					RegisteredClass registeredClass = kryo.readClass(buffer);
					concreteType = registeredClass.type;
					if (serializer == null) serializer = registeredClass.serializer;
				}

				if (!cachedField.canBeNull)
					value = serializer.readObjectData(buffer, concreteType);
				else
					value = serializer.readObject(buffer, concreteType);
				field.set(object, value);
			}
		} catch (IllegalAccessException ex) {
			throw new SerializationException("Error accessing field in class: " + type.getName(), ex);
		}
		if (level <= TRACE) trace("kryo", "Read object: " + object);
		return object;
	}

	/**
	 * Allows options to be set for a specific field.
	 * @param serializer The serializer to use for the field's value.
	 * @param canBeNull False if the field can never be null when it is serialized.
	 * @return
	 */
	public CachedField getField (Class type, String fieldName) {
		CachedField[] fields = fieldCache.get(type);
		if (fields == null) fields = cache(type);
		for (CachedField cachedField : fields)
			if (cachedField.field.getName().equals(fieldName)) return cachedField;
		throw new IllegalArgumentException("Field \"" + fieldName + "\" not found on class: " + type.getName());
	}

	// If a concreteType is set, a serializer should be set.
	static public class CachedField {
		// BOZO - Use methods.
		public Field field;
		public Class concreteType;
		public Serializer serializer;
		public boolean canBeNull;

		public String toString () {
			return field.getName();
		}
	}
}
