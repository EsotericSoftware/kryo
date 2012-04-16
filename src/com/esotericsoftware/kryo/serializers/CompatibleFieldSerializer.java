
package com.esotericsoftware.kryo.serializers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.Util;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.InputChunked;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.OutputChunked;
import com.esotericsoftware.kryo.serializers.FieldSerializer.Optional;
import com.esotericsoftware.kryo.util.ObjectMap;
import com.esotericsoftware.reflectasm.FieldAccess;

import static com.esotericsoftware.minlog.Log.*;

/** Serializes objects using direct field assignment, with limited support for forward and backward compatibility. Fields can be
 * added or removed without invalidating previously serialized bytes. Note that changing the type of a field is not supported.
 * <p>
 * There is additional overhead compared to {@link FieldSerializer}. A header is output the first time an object of a given type
 * is serialized. The header consists of an int for the number of fields, then a String for each field name. Also, to support
 * skipping the bytes for a field that no longer exists, for each field value an int is written that is the length of the value in
 * bytes.
 * @author Nathan Sweet <misc@n4te.com> */
public class CompatibleFieldSerializer extends Serializer {
	final Kryo kryo;
	final Class type;
	private CachedField[] fields;
	Object access;
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

		ObjectMap context = kryo.getContext();

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
			if (Modifier.isStatic(modifiers)) continue;
			if (field.isSynthetic()) continue;

			if (!field.isAccessible()) {
				if (!setFieldsAsAccessible) continue;
				try {
					field.setAccessible(true);
				} catch (AccessControlException ex) {
					continue;
				}
			}

			Optional optional = field.getAnnotation(Optional.class);
			if (optional != null && !context.containsKey(optional.value())) continue;

			Class fieldClass = field.getType();

			CachedField cachedField = new CachedField();
			cachedField.field = field;
			if (fieldsCanBeNull)
				cachedField.canBeNull = !fieldClass.isPrimitive() && !field.isAnnotationPresent(NotNull.class);
			else
				cachedField.canBeNull = false;

			// Always use the same serializer for this field if the field's class is final.
			if (kryo.isFinal(fieldClass)) cachedField.fieldClass = fieldClass;

			cachedFields.add(cachedField);
			if (Modifier.isPublic(modifiers) && Modifier.isPublic(fieldClass.getModifiers())) publicFields.add(cachedField);
		}

		if (!Util.isAndroid && Modifier.isPublic(type.getModifiers()) && !publicFields.isEmpty()) {
			// Use ReflectASM for any public fields.
			try {
				access = FieldAccess.get(type);
				for (int i = 0, n = publicFields.size(); i < n; i++) {
					CachedField cachedField = publicFields.get(i);
					cachedField.accessIndex = ((FieldAccess)access).getIndex(cachedField.field.getName());
				}
			} catch (RuntimeException ignored) {
			}
		}

		int fieldCount = cachedFields.size();
		fields = new CachedField[fieldCount];
		for (int i = 0; i < fieldCount; i++)
			fields[i] = cachedFields.poll();
	}

	/** Sets the default value for {@link CachedField#setCanBeNull(boolean)}.
	 * @param fieldsCanBeNull False if none of the fields are null. Saves 1 byte per field. True if it is not known (default). */
	public void setFieldsCanBeNull (boolean fieldsCanBeNull) {
		this.fieldsCanBeNull = fieldsCanBeNull;
		rebuildCachedFields();
	}

	/** Controls which fields are accessed.
	 * @param setFieldsAsAccessible If true, all non-transient fields (inlcuding private fields) will be serialized and
	 *           {@link Field#setAccessible(boolean) set as accessible} (default). If false, only fields in the public API will be
	 *           serialized. */
	public void setFieldsAsAccessible (boolean setFieldsAsAccessible) {
		this.setFieldsAsAccessible = setFieldsAsAccessible;
		rebuildCachedFields();
	}

	public void write (Kryo kryo, Output output, Object object) {
		ObjectMap context = kryo.getGraphContext();
		if (!context.containsKey("schemaWritten")) {
			context.put("schemaWritten", null);
			if (TRACE) trace("kryo", "Write " + fields.length + " field names.");
			output.writeInt(fields.length, true);
			for (int i = 0, n = fields.length; i < n; i++)
				output.writeString(fields[i].field.getName());
		}

		OutputChunked outputChunked = new OutputChunked(output, 1024);
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];
			try {
				if (TRACE) trace("kryo", "Write field: " + cachedField + " (" + object.getClass().getName() + ")");

				Object value = cachedField.get(object);
				if (value == null) {
					kryo.writeClass(outputChunked, null);
					outputChunked.endChunks();
					continue;
				}

				Serializer serializer = cachedField.serializer;
				if (cachedField.fieldClass == null) {
					Registration registration = kryo.writeClass(outputChunked, value.getClass());
					if (serializer == null) serializer = registration.getSerializer();
					kryo.writeObject(outputChunked, value, serializer);
				} else {
					if (serializer == null)
						cachedField.serializer = serializer = kryo.getRegistration(cachedField.fieldClass).getSerializer();
					if (!cachedField.canBeNull)
						kryo.writeObject(outputChunked, value, serializer);
					else
						kryo.writeObjectOrNull(outputChunked, value, serializer);
				}

				outputChunked.endChunks();
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field in class: " + object.getClass().getName(), ex);
			} catch (KryoException ex) {
				ex.addTrace(cachedField + " (" + object.getClass().getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(cachedField + " (" + object.getClass().getName() + ")");
				throw ex;
			}
		}
	}

	public void read (Kryo kryo, Input input, Object object) {
		ObjectMap context = kryo.getGraphContext();
		CachedField[] fields = (CachedField[])context.get("schema");
		if (fields == null) {
			int length = input.readInt(true);
			if (TRACE) trace("kryo", "Read " + length + " field names.");
			String[] names = new String[length];
			for (int i = 0; i < length; i++)
				names[i] = input.readString();

			fields = new CachedField[length];
			CachedField[] allFields = this.fields;
			outer:
			for (int i = 0, n = names.length; i < n; i++) {
				String schemaName = names[i];
				for (int ii = 0, nn = allFields.length; ii < nn; ii++) {
					if (allFields[ii].field.getName().equals(schemaName)) {
						fields[i] = allFields[ii];
						continue outer;
					}
				}
				if (TRACE) trace("kryo", "Ignore obsolete field: " + schemaName);
			}
			context.put("schema", fields);
		}

		InputChunked inputChunked = new InputChunked(input, 1024);
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];
			try {
				if (cachedField == null) {
					if (TRACE) trace("kryo", "Skip obsolete field.");
					inputChunked.nextChunks();
					continue;
				}

				if (TRACE) trace("kryo", "Read field: " + cachedField + " (" + type.getName() + ")");

				Object value;

				Class concreteType = cachedField.fieldClass;
				Serializer serializer = cachedField.serializer;
				if (concreteType == null) {
					Registration registration = kryo.readClass(inputChunked);
					if (registration == null)
						value = null;
					else {
						concreteType = registration.getType();
						if (serializer == null) serializer = registration.getSerializer();
						value = kryo.readObject(inputChunked, concreteType, serializer);
					}
				} else {
					if (serializer == null) cachedField.serializer = serializer = kryo.getRegistration(concreteType).getSerializer();
					if (!cachedField.canBeNull)
						value = kryo.readObject(inputChunked, concreteType, serializer);
					else
						value = kryo.readObjectOrNull(inputChunked, concreteType, serializer);
				}

				cachedField.set(object, value);

				inputChunked.nextChunks();
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field in class: " + type.getName(), ex);
			} catch (KryoException ex) {
				ex.addTrace(cachedField + " (" + type.getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(cachedField + " (" + type.getName() + ")");
				throw ex;
			}
		}
	}

	/** Allows specific fields to be optimized. */
	public CachedField getField (String fieldName) {
		for (CachedField cachedField : fields)
			if (cachedField.field.getName().equals(fieldName)) return cachedField;
		throw new IllegalArgumentException("Field \"" + fieldName + "\" not found on class: " + type.getName());
	}

	/** Removes a field so that it won't be serialized. */
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

	/** Controls how a field will be serialized. */
	public class CachedField {
		Field field;
		Class fieldClass;
		Serializer serializer;
		boolean canBeNull;
		int accessIndex = -1;

		/** @param fieldClass The concrete class of the values for this field. This saves 1-2 bytes. The serializer registered for the
		 *           specified class will be used. Only set to a non-null value if the field type in the class definition is final
		 *           or the values for this field will not vary. */
		public void setClass (Class fieldClass) {
			this.fieldClass = fieldClass;
			this.serializer = null;
		}

		/** @param fieldClass The concrete class of the values for this field. This saves 1-2 bytes. Only set to a non-null value if
		 *           the field type in the class definition is final or the values for this field will not vary. */
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
			if (accessIndex != -1) return ((FieldAccess)access).get(object, accessIndex);
			return field.get(object);
		}

		void set (Object object, Object value) throws IllegalAccessException {
			if (accessIndex != -1)
				((FieldAccess)access).set(object, accessIndex, value);
			else
				field.set(object, value);
		}
	}
}
