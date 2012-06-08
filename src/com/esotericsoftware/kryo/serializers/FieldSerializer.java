
package com.esotericsoftware.kryo.serializers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.Util;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.ObjectMap;
import com.esotericsoftware.reflectasm.FieldAccess;

import static com.esotericsoftware.minlog.Log.*;

/** Serializes objects using direct field assignment. No header or schema data is stored, only the data for each field. This
 * reduces output size but means if any field is added or removed, previously serialized bytes are invalidated. If fields are
 * public, bytecode generation will be used instead of reflection.
 * @see Serializer
 * @see Kryo#register(Class, Serializer)
 * @author Nathan Sweet <misc@n4te.com> */
public class FieldSerializer<T> extends Serializer<T> implements Comparator<FieldSerializer.CachedField> {
	private final Kryo kryo;
	private final Class type;
	private CachedField[] fields = new CachedField[0];
	Object access;
	private boolean fieldsCanBeNull = true, setFieldsAsAccessible = true;
	private boolean ignoreSyntheticFields = true;
	private boolean finalFieldTypes;

	public FieldSerializer (Kryo kryo, Class type) {
		this.kryo = kryo;
		this.type = type;
		rebuildCachedFields();
	}

	/** Called when the list of cached fields must be rebuilt. This is done any time settings are changed that affect which fields
	 * will be used. It is called from the constructor for FieldSerializer, but not for subclasses. Subclasses must call this from
	 * their constructor. */
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

		ArrayList<CachedField> asmFields = new ArrayList();
		ArrayList<CachedField> cachedFields = new ArrayList(allFields.size());
		for (int i = 0, n = allFields.size(); i < n; i++) {
			Field field = allFields.get(i);

			int modifiers = field.getModifiers();
			if (Modifier.isTransient(modifiers)) continue;
			if (Modifier.isStatic(modifiers)) continue;
			if (field.isSynthetic() && ignoreSyntheticFields) continue;

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

			CachedField cachedField = newCachedField(field);
			if (cachedField == null) continue;
			if (fieldsCanBeNull)
				cachedField.canBeNull = !fieldClass.isPrimitive() && !field.isAnnotationPresent(NotNull.class);
			else
				cachedField.canBeNull = false;

			// Always use the same serializer for this field if the field's class is final.
			if (kryo.isFinal(fieldClass) || finalFieldTypes) cachedField.valueClass = fieldClass;

			cachedFields.add(cachedField);
			if (!Modifier.isFinal(modifiers) && Modifier.isPublic(modifiers) && Modifier.isPublic(fieldClass.getModifiers()))
				asmFields.add(cachedField);
		}

		if (!Util.isAndroid && Modifier.isPublic(type.getModifiers()) && !asmFields.isEmpty()) {
			// Use ReflectASM for any public fields.
			try {
				access = FieldAccess.get(type);
				for (int i = 0, n = asmFields.size(); i < n; i++) {
					CachedField cachedField = asmFields.get(i);
					cachedField.accessIndex = ((FieldAccess)access).getIndex(cachedField.field.getName());
				}
			} catch (RuntimeException ignored) {
			}
		}

		Collections.sort(cachedFields, this);
		fields = cachedFields.toArray(new CachedField[cachedFields.size()]);
	}

	/** Returns a new cached field for the specified field, or null if the field should not be used.
	 * @return May be null. */
	protected CachedField newCachedField (Field field) {
		return new CachedField(field);
	}

	public int compare (CachedField o1, CachedField o2) {
		// Fields are sorted by alpha so the order of the data is known.
		return o1.field.getName().compareTo(o2.field.getName());
	}

	/** Sets the default value for {@link CachedField#setCanBeNull(boolean)}. Calling this method resets the {@link #getFields()
	 * cached fields}.
	 * @param fieldsCanBeNull False if none of the fields are null. Saves 0-1 byte per field. True if it is not known (default). */
	public void setFieldsCanBeNull (boolean fieldsCanBeNull) {
		this.fieldsCanBeNull = fieldsCanBeNull;
		rebuildCachedFields();
	}

	/** Controls which fields are serialized. Calling this method resets the {@link #getFields() cached fields}.
	 * @param setFieldsAsAccessible If true, all non-transient fields (inlcuding private fields) will be serialized and
	 *           {@link Field#setAccessible(boolean) set as accessible} if necessary (default). If false, only fields in the public
	 *           API will be serialized. */
	public void setFieldsAsAccessible (boolean setFieldsAsAccessible) {
		this.setFieldsAsAccessible = setFieldsAsAccessible;
		rebuildCachedFields();
	}

	/** Controls if synthetic fields are serialized. Default is true. Calling this method resets the {@link #getFields() cached
	 * fields}.
	 * @param ignoreSyntheticFields If true, only non-synthetic fields will be serialized. */
	public void setIgnoreSyntheticFields (boolean ignoreSyntheticFields) {
		this.ignoreSyntheticFields = ignoreSyntheticFields;
		rebuildCachedFields();
	}

	/** Sets the default value for {@link CachedField#setClass(Class)} to the field's declared type. This allows FieldSerializer to
	 * be more efficient, since it knows field values will not be a subclass of their declared type. Default is false. Calling this
	 * method resets the {@link #getFields() cached fields}. */
	public void setFixedFieldTypes (boolean finalFieldTypes) {
		this.finalFieldTypes = finalFieldTypes;
		rebuildCachedFields();
	}

	public void write (Kryo kryo, Output output, T object) {
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];
			try {
				if (TRACE) trace("kryo", "Write field: " + cachedField + " (" + object.getClass().getName() + ")");

				Object value = cachedField.get(object);

				Serializer serializer = cachedField.serializer;
				if (cachedField.valueClass == null) {
					// The concrete type of the field is unknown, write the class first.
					if (value == null) {
						kryo.writeClass(output, null);
						continue;
					}
					Registration registration = kryo.writeClass(output, value.getClass());
					if (serializer == null) serializer = registration.getSerializer();
					if (cachedField.generics != null) serializer.setGenerics(kryo, cachedField.generics);
					kryo.writeObject(output, value, serializer);
				} else {
					// The concrete type of the field is known, always use the same serializer.
					if (serializer == null) cachedField.serializer = serializer = kryo.getSerializer(cachedField.valueClass);
					if (cachedField.generics != null) serializer.setGenerics(kryo, cachedField.generics);
					if (cachedField.canBeNull) {
						kryo.writeObjectOrNull(output, value, serializer);
					} else {
						if (value == null) {
							throw new KryoException("Field value is null but canBeNull is false: " + cachedField + " ("
								+ object.getClass().getName() + ")");
						}
						kryo.writeObject(output, value, serializer);
					}
				}
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field: " + cachedField + " (" + object.getClass().getName() + ")", ex);
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

	/** Used by {@link #read(Kryo, Input, Class)} to create the new object. This can be overridden to customize object creation, eg
	 * to call a constructor with arguments. The default implementation uses {@link Kryo#newInstance(Class)}. */
	protected T create (Kryo kryo, Input input, Class<T> type) {
		return kryo.newInstance(type);
	}

	public T read (Kryo kryo, Input input, Class<T> type) {
		T object = create(kryo, input, type);
		kryo.reference(object);
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];
			try {
				if (TRACE) trace("kryo", "Read field: " + cachedField + " (" + type.getName() + ")");

				Object value;

				Class concreteType = cachedField.valueClass;
				Serializer serializer = cachedField.serializer;
				if (concreteType == null) {
					Registration registration = kryo.readClass(input);
					if (registration == null)
						value = null;
					else {
						if (serializer == null) serializer = registration.getSerializer();
						if (cachedField.generics != null) serializer.setGenerics(kryo, cachedField.generics);
						value = kryo.readObject(input, registration.getType(), serializer);
					}
				} else {
					if (serializer == null) cachedField.serializer = serializer = kryo.getSerializer(cachedField.valueClass);
					if (cachedField.generics != null) serializer.setGenerics(kryo, cachedField.generics);
					if (cachedField.canBeNull)
						value = kryo.readObjectOrNull(input, concreteType, serializer);
					else
						value = kryo.readObject(input, concreteType, serializer);
				}

				cachedField.set(object, value);
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field: " + cachedField + " (" + type.getName() + ")", ex);
			} catch (KryoException ex) {
				ex.addTrace(cachedField + " (" + type.getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(cachedField + " (" + type.getName() + ")");
				throw ex;
			}
		}
		return object;
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

	public CachedField[] getFields () {
		return fields;
	}

	public Class getType () {
		return type;
	}

	/** Used by {@link #copy(Kryo, Object)} to create the new object. This can be overridden to customize object creation, eg to
	 * call a constructor with arguments. The default implementation uses {@link Kryo#newInstance(Class)}. */
	protected T createCopy (Kryo kryo, T original) {
		return (T)kryo.newInstance(original.getClass());
	}

	public T copy (Kryo kryo, T original) {
		T copy = createCopy(kryo, original);
		kryo.reference(copy);
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];
			try {
				Object value = cachedField.get(original);
				cachedField.set(copy, value);
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field: " + cachedField + " (" + type.getName() + ")", ex);
			} catch (KryoException ex) {
				ex.addTrace(cachedField + " (" + type.getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(cachedField + " (" + type.getName() + ")");
				throw ex;
			}
		}
		return copy;
	}

	/** Controls how a field will be serialized. */
	public class CachedField<X> {
		final Field field;
		Class valueClass;
		Serializer serializer;
		boolean canBeNull;
		int accessIndex = -1;
		Type[] generics;

		public CachedField (Field field) {
			this.field = field;

			Type genericType = field.getGenericType();
			if (genericType instanceof ParameterizedType) generics = ((ParameterizedType)genericType).getActualTypeArguments();
		}

		/** @param valueClass The concrete class of the values for this field. This saves 1-2 bytes. The serializer registered for the
		 *           specified class will be used. Only set to a non-null value if the field type in the class definition is final
		 *           or the values for this field will not vary. */
		public void setClass (Class valueClass) {
			this.valueClass = valueClass;
			this.serializer = null;
		}

		/** @param valueClass The concrete class of the values for this field. This saves 1-2 bytes. Only set to a non-null value if
		 *           the field type in the class definition is final or the values for this field will not vary. */
		public void setClass (Class valueClass, Serializer serializer) {
			this.valueClass = valueClass;
			this.serializer = serializer;
		}

		public void setSerializer (Serializer serializer) {
			this.serializer = serializer;
		}

		public void setCanBeNull (boolean canBeNull) {
			this.canBeNull = canBeNull;
		}

		public Field getField () {
			return field;
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

	/** Indicates a field should be ignored when its declaring class is registered unless the {@link Kryo#getContext() context} has
	 * a value set for specified key. This can be useful to useful when a field must be serialized for one purpose, but not for
	 * another. Eg, a class for a networked application might have a field that should not be serialized and sent to clients, but
	 * should be serialized when stored on the server.
	 * @author Nathan Sweet <misc@n4te.com> */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	static public @interface Optional {
		public String value();
	}
}
