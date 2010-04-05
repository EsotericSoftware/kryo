
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.SerializationException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.Kryo.RegisteredClass;
import com.esotericsoftware.reflectasm.FieldAccess;

/**
 * @see FieldSerializer
 * @author Nathan Sweet <misc@n4te.com>
 */
public class CompatibleFieldSerializer extends Serializer {
	final Kryo kryo;
	final Class type;
	private CachedField[] fields;
	private boolean fieldsCanBeNull = true, setFieldsAsAccessible = true;

	public CompatibleFieldSerializer (Kryo kryo, Class type) {
		this.kryo = kryo;
		this.type = type;
		rebuildCachedFields();
	}

	private void rebuildCachedFields () {
		if (type.isInterface()) {
			fields = new CachedField[0]; // No fields to serialize.
			return;
		}

		// Collect all fields.
		ArrayList<Field> allFields = new ArrayList();
		Class nextClass = type;
		while (nextClass != Object.class) {
			Collections.addAll(allFields, nextClass.getDeclaredFields());
			nextClass = nextClass.getSuperclass();
		}

		ArrayList<CachedField> publicFields = new ArrayList();
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
			if (Modifier.isFinal(fieldClass.getModifiers())) cachedField.fieldClass = fieldClass;

			cachedFields.add(cachedField);
			if (Modifier.isPublic(modifiers)) publicFields.add(cachedField);
		}

		if (!publicFields.isEmpty()) {
			// Use ReflectASM for any public fields.
			try {
				FieldAccess access = FieldAccess.get(type);
				for (int i = 0, n = publicFields.size(); i < n; i++) {
					CachedField cachedField = publicFields.get(i);
					cachedField.access = access;
					cachedField.accessIndex = access.getIndex(cachedField.field.getName());
				}
			} catch (Throwable ignored) {
				// ReflectASM is not available on Android.
			}
		}

		int fieldCount = cachedFields.size();
		fields = new CachedField[fieldCount];
		for (int i = 0; i < fieldCount; i++)
			fields[i] = cachedFields.poll();
	}

	/**
	 * Sets the default value for {@link CachedField#setCanBeNull(boolean)}.
	 * @param fieldsCanBeNull False if none of the fields are null. Saves 1 byte per field. True if it is not known (default).
	 */
	public void setFieldsCanBeNull (boolean fieldsCanBeNull) {
		this.fieldsCanBeNull = fieldsCanBeNull;
		rebuildCachedFields();
	}

	/**
	 * Controls which fields are accessed.
	 * @param setFieldsAsAccessible If true, all non-transient fields (inlcuding private fields) will be serialized and
	 *           {@link Field#setAccessible(boolean) set as accessible} (default). If false, only fields in the public API will be
	 *           serialized.
	 */
	public void setFieldsAsAccessible (boolean setFieldsAsAccessible) {
		this.setFieldsAsAccessible = setFieldsAsAccessible;
		rebuildCachedFields();
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		Context context = Kryo.getContext();
		if (context.getTemp(this, "schemaWritten") == null) {
			context.putTemp(this, "schemaWritten", Boolean.TRUE);
			if (TRACE) trace("kryo", "Writing " + fields.length + " field names.");
			IntSerializer.put(buffer, fields.length, true);
			for (int i = 0, n = fields.length; i < n; i++)
				StringSerializer.put(buffer, fields[i].field.getName());
		}

		Class type = object.getClass();
		try {
			for (int i = 0, n = fields.length; i < n; i++) {
				CachedField cachedField = fields[i];
				if (TRACE) trace("kryo", "Writing field: " + cachedField + " (" + type.getName() + ")");

				Object value = cachedField.get(object);
				if (value == null) {
					kryo.writeClass(buffer, null);
					continue;
				}

				int start = buffer.position();
				try {
					buffer.position(start + 1);
				} catch (IllegalArgumentException ex) {
					new BufferOverflowException();
				}

				Serializer serializer = cachedField.serializer;
				if (cachedField.fieldClass == null) {
					RegisteredClass registeredClass = kryo.writeClass(buffer, value.getClass());
					if (serializer == null) serializer = registeredClass.getSerializer();
					serializer.writeObjectData(buffer, value);
				} else {
					if (serializer == null)
						cachedField.serializer = serializer = kryo.getRegisteredClass(cachedField.fieldClass).getSerializer();
					if (!cachedField.canBeNull)
						serializer.writeObjectData(buffer, value);
					else
						serializer.writeObject(buffer, value);
				}

				int dataLength = buffer.position() - start - 1;
				if (dataLength <= 127) {
					// Ideally it fits in one byte.
					buffer.put(start, (byte)dataLength);
				} else {
					// Shift the data over to make room for the length.
					byte[] temp = context.getByteArray(dataLength);
					buffer.position(start + 1);
					buffer.get(temp);
					buffer.position(start);
					IntSerializer.put(buffer, dataLength, true);
					buffer.put(temp);
				}
			}
		} catch (IllegalAccessException ex) {
			throw new SerializationException("Error accessing field in class: " + type.getName(), ex);
		}
		if (TRACE) trace("kryo", "Wrote object: " + object);
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		return readObjectData(newInstance(type), buffer, type);
	}

	protected <T> T readObjectData (T object, ByteBuffer buffer, Class<T> type) {
		Context context = Kryo.getContext();
		CachedField[] fields = (CachedField[])context.getTemp(this, "schema");
		if (fields == null) {
			int length = IntSerializer.get(buffer, true);
			if (TRACE) trace("kryo", "Reading " + length + " field names.");
			String[] names = new String[length];
			for (int i = 0; i < length; i++)
				names[i] = StringSerializer.get(buffer);

			fields = new CachedField[length];
			CachedField[] allFields = this.fields;
			outer: //
			for (int i = 0, n = names.length; i < n; i++) {
				String schemaName = names[i];
				for (int ii = 0, nn = allFields.length; ii < nn; ii++) {
					if (allFields[ii].field.getName().equals(schemaName)) {
						fields[i] = allFields[ii];
						continue outer;
					}
				}
				if (TRACE) trace("kryo", "Ignoring obsolete field: " + schemaName);
			}
			context.putTemp(this, "schema", fields);
		}

		try {
			for (int i = 0, n = fields.length; i < n; i++) {
				int dataLength = IntSerializer.get(buffer, true);

				CachedField cachedField = fields[i];
				if (cachedField == null) {
					if (TRACE) trace("kryo", "Skipping obsolete field bytes: " + dataLength);
					try {
						buffer.position(buffer.position() + dataLength);
					} catch (IllegalArgumentException ex) {
						new BufferOverflowException();
					}
					continue;
				}

				if (TRACE) trace("kryo", "Reading field: " + cachedField + " (" + type.getName() + ")");

				Object value;

				if (dataLength == 0)
					value = null;
				else {
					Class concreteType = cachedField.fieldClass;
					Serializer serializer = cachedField.serializer;
					if (concreteType == null) {
						RegisteredClass registeredClass = kryo.readClass(buffer);
						if (registeredClass == null)
							value = null;
						else {
							concreteType = registeredClass.getType();
							if (serializer == null) serializer = registeredClass.getSerializer();
							value = serializer.readObjectData(buffer, concreteType);
						}
					} else {
						if (serializer == null)
							cachedField.serializer = serializer = kryo.getRegisteredClass(concreteType).getSerializer();
						if (!cachedField.canBeNull)
							value = serializer.readObjectData(buffer, concreteType);
						else
							value = serializer.readObject(buffer, concreteType);
					}
				}

				cachedField.set(object, value);
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
	public CachedField getField (String fieldName) {
		for (CachedField cachedField : fields)
			if (cachedField.field.getName().equals(fieldName)) return cachedField;
		throw new IllegalArgumentException("Field \"" + fieldName + "\" not found on class: " + type.getName());
	}

	/**
	 * Removes a field so that it won't be serialized.
	 */
	public void removeField (String fieldName) {
		for (int i = 0; i < fields.length; i++) {
			CachedField cachedField = fields[i];
			if (cachedField.field.getName().equals(fieldName)) {
				CachedField[] newFields = new CachedField[fields.length - 1];
				System.arraycopy(fields, 0, newFields, 0, i);
				System.arraycopy(fields, i + 1, newFields, i, newFields.length - i);
				fields = newFields;
				return;
			}
		}
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
		Object access;
		int accessIndex;

		/**
		 * @param fieldClass The concrete class of the values for this field. This saves 1-2 bytes. The serializer registered for
		 *           the specified class will be used. Only set to a non-null value if the field type in the class definition is
		 *           final or the values for this field will not vary.
		 */
		public void setClass (Class fieldClass) {
			this.fieldClass = fieldClass;
			this.serializer = null;
		}

		/**
		 * @param fieldClass The concrete class of the values for this field. This saves 1-2 bytes. Only set to a non-null value if
		 *           the field type in the class definition is final or the values for this field will not vary.
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

		Object get (Object object) throws IllegalAccessException {
			if (access != null) return ((FieldAccess)access).get(object, accessIndex);
			return field.get(object);
		}

		void set (Object object, Object value) throws IllegalAccessException {
			if (access != null)
				((FieldAccess)access).set(object, accessIndex, value);
			else
				field.set(object, value);
		}
	}
}
