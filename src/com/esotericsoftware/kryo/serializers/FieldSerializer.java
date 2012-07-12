
package com.esotericsoftware.kryo.serializers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.IntArray;
import com.esotericsoftware.kryo.util.ObjectMap;
import com.esotericsoftware.kryo.util.Util;
import com.esotericsoftware.reflectasm.FieldAccess;

import static com.esotericsoftware.minlog.Log.*;

// BOZO - Make primitive serialization with ReflectASM configurable?

/** Serializes objects using direct field assignment. No header or schema data is stored, only the data for each field. This
 * reduces output size but means if any field is added or removed, previously serialized bytes are invalidated. If fields are
 * public, bytecode generation will be used instead of reflection.
 * @see Serializer
 * @see Kryo#register(Class, Serializer)
 * @author Nathan Sweet <misc@n4te.com> */
public class FieldSerializer<T> extends Serializer<T> implements Comparator<FieldSerializer.CachedField> {
	final Kryo kryo;
	final Class type;
	private CachedField[] fields = new CachedField[0];
	Object access;
	private boolean fieldsCanBeNull = true, setFieldsAsAccessible = true;
	private boolean ignoreSyntheticFields = true;
	private boolean fixedFieldTypes;

	// BOZO - Get rid of kryo here?
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

		IntArray useAsm = new IntArray();
		ArrayList<Field> validFields = new ArrayList(allFields.size());
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

			validFields.add(field);

			// BOZO - Must be public?
			useAsm.add(!Modifier.isFinal(modifiers) && Modifier.isPublic(modifiers)
				&& Modifier.isPublic(field.getType().getModifiers()) ? 1 : 0);
		}

		// Use ReflectASM for any public fields.
		if (!Util.isAndroid && Modifier.isPublic(type.getModifiers()) && useAsm.indexOf(1) != -1) {
			try {
				access = FieldAccess.get(type);
			} catch (RuntimeException ignored) {
			}
		}

		ArrayList<CachedField> cachedFields = new ArrayList(validFields.size());
		for (int i = 0, n = validFields.size(); i < n; i++) {
			Field field = validFields.get(i);

			int accessIndex = -1;
			if (access != null && useAsm.get(i) == 1) accessIndex = ((FieldAccess)access).getIndex(field.getName());

			cachedFields.add(newCachedField(field, cachedFields.size(), accessIndex));
		}

		Collections.sort(cachedFields, this);
		fields = cachedFields.toArray(new CachedField[cachedFields.size()]);

		initializeCachedFields();
	}

	protected void initializeCachedFields () {
	}

	private CachedField newCachedField (Field field, int fieldIndex, int accessIndex) {
		Class fieldClass = field.getType();

		CachedField cachedField;
		if (accessIndex != -1) {
			if (fieldClass.isPrimitive()) {
				if (fieldClass == boolean.class)
					cachedField = new BooleanField();
				else if (fieldClass == byte.class)
					cachedField = new ByteField();
				else if (fieldClass == char.class)
					cachedField = new CharField();
				else if (fieldClass == short.class)
					cachedField = new ShortField();
				else if (fieldClass == int.class)
					cachedField = new IntField();
				else if (fieldClass == long.class)
					cachedField = new LongField();
				else if (fieldClass == float.class)
					cachedField = new FloatField();
				else if (fieldClass == double.class)
					cachedField = new DoubleField();
				else
					cachedField = new ObjectField();
			} else if (fieldClass == String.class
				&& (!kryo.getReferences() || !kryo.getReferenceResolver().useReferences(String.class))) {
				cachedField = new StringField();
			} else
				cachedField = new ObjectField();
		} else {
			cachedField = new ObjectField();
			((ObjectField)cachedField).generics = Kryo.getGenerics(field.getGenericType());
		}

		cachedField.field = field;
		cachedField.accessIndex = accessIndex;
		cachedField.canBeNull = fieldsCanBeNull && !fieldClass.isPrimitive() && !field.isAnnotationPresent(NotNull.class);

		// Always use the same serializer for this field if the field's class is final.
		if (kryo.isFinal(fieldClass) || fixedFieldTypes) cachedField.valueClass = fieldClass;

		return cachedField;
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
	public void setFixedFieldTypes (boolean fixedFieldTypes) {
		this.fixedFieldTypes = fixedFieldTypes;
		rebuildCachedFields();
	}

	public void write (Kryo kryo, Output output, T object) {
		CachedField[] fields = this.fields;
		for (int i = 0, n = fields.length; i < n; i++)
			fields[i].write(output, object);
	}

	public T read (Kryo kryo, Input input, Class<T> type) {
		T object = create(kryo, input, type);
		kryo.reference(object);
		CachedField[] fields = this.fields;
		for (int i = 0, n = fields.length; i < n; i++)
			fields[i].read(input, object);
		return object;
	}

	/** Used by {@link #read(Kryo, Input, Class)} to create the new object. This can be overridden to customize object creation, eg
	 * to call a constructor with arguments. The default implementation uses {@link Kryo#newInstance(Class)}. */
	protected T create (Kryo kryo, Input input, Class<T> type) {
		return kryo.newInstance(type);
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
		for (int i = 0, n = fields.length; i < n; i++)
			fields[i].copy(original, copy);
		return copy;
	}

	/** Controls how a field will be serialized. */
	public abstract class CachedField<X> {
		Field field;
		Class valueClass;
		Serializer serializer;
		boolean canBeNull;
		int accessIndex = -1;

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

		abstract public void write (Output output, Object object);

		abstract public void read (Input input, Object object);

		abstract public void copy (Object original, Object copy);
	}

	abstract class AsmCachedField extends CachedField {
		FieldAccess access = (FieldAccess)FieldSerializer.this.access;
	}

	class IntField extends AsmCachedField {
		public void write (Output output, Object object) {
			output.writeInt(access.getInt(object, accessIndex), false);
		}

		public void read (Input input, Object object) {
			access.setInt(object, accessIndex, input.readInt(false));
		}

		public void copy (Object original, Object copy) {
			access.setInt(copy, accessIndex, access.getInt(original, accessIndex));
		}
	}

	class FloatField extends AsmCachedField {
		public void write (Output output, Object object) {
			output.writeFloat(access.getFloat(object, accessIndex));
		}

		public void read (Input input, Object object) {
			access.setFloat(object, accessIndex, input.readFloat());
		}

		public void copy (Object original, Object copy) {
			access.setFloat(copy, accessIndex, access.getFloat(original, accessIndex));
		}
	}

	class ShortField extends AsmCachedField {
		public void write (Output output, Object object) {
			output.writeShort(access.getShort(object, accessIndex));
		}

		public void read (Input input, Object object) {
			access.setShort(object, accessIndex, input.readShort());
		}

		public void copy (Object original, Object copy) {
			access.setShort(copy, accessIndex, access.getShort(original, accessIndex));
		}
	}

	class ByteField extends AsmCachedField {
		public void write (Output output, Object object) {
			output.writeByte(access.getByte(object, accessIndex));
		}

		public void read (Input input, Object object) {
			access.setByte(object, accessIndex, input.readByte());
		}

		public void copy (Object original, Object copy) {
			access.setByte(copy, accessIndex, access.getByte(original, accessIndex));
		}
	}

	class BooleanField extends AsmCachedField {
		public void write (Output output, Object object) {
			output.writeBoolean(access.getBoolean(object, accessIndex));
		}

		public void read (Input input, Object object) {
			access.setBoolean(object, accessIndex, input.readBoolean());
		}

		public void copy (Object original, Object copy) {
			access.setBoolean(copy, accessIndex, access.getBoolean(original, accessIndex));
		}
	}

	class CharField extends AsmCachedField {
		public void write (Output output, Object object) {
			output.writeChar(access.getChar(object, accessIndex));
		}

		public void read (Input input, Object object) {
			access.setChar(object, accessIndex, input.readChar());
		}

		public void copy (Object original, Object copy) {
			access.setChar(copy, accessIndex, access.getChar(original, accessIndex));
		}
	}

	class LongField extends AsmCachedField {
		public void write (Output output, Object object) {
			output.writeLong(access.getLong(object, accessIndex), false);
		}

		public void read (Input input, Object object) {
			access.setLong(object, accessIndex, input.readLong(false));
		}

		public void copy (Object original, Object copy) {
			access.setLong(copy, accessIndex, access.getLong(original, accessIndex));
		}
	}

	class DoubleField extends AsmCachedField {
		public void write (Output output, Object object) {
			output.writeDouble(access.getDouble(object, accessIndex));
		}

		public void read (Input input, Object object) {
			access.setDouble(object, accessIndex, input.readDouble());
		}

		public void copy (Object original, Object copy) {
			access.setDouble(copy, accessIndex, access.getDouble(original, accessIndex));
		}
	}

	class StringField extends AsmCachedField {
		public void write (Output output, Object object) {
			output.writeString(access.getString(object, accessIndex));
		}

		public void read (Input input, Object object) {
			access.set(object, accessIndex, input.readString());
		}

		public void copy (Object original, Object copy) {
			access.set(copy, accessIndex, access.getString(original, accessIndex));
		}
	}

	class ObjectField extends CachedField {
		Class[] generics;

		public void write (Output output, Object object) {
			try {
				if (TRACE) trace("kryo", "Write field: " + this + " (" + object.getClass().getName() + ")");

				Object value;
				if (accessIndex != -1)
					value = ((FieldAccess)access).get(object, accessIndex);
				else
					value = field.get(object);

				Serializer serializer = this.serializer;
				if (valueClass == null) {
					// The concrete type of the field is unknown, write the class first.
					if (value == null) {
						kryo.writeClass(output, null);
						return;
					}
					Registration registration = kryo.writeClass(output, value.getClass());
					if (serializer == null) serializer = registration.getSerializer();
					if (generics != null) serializer.setGenerics(kryo, generics);
					kryo.writeObject(output, value, serializer);
				} else {
					// The concrete type of the field is known, always use the same serializer.
					if (serializer == null) this.serializer = serializer = kryo.getSerializer(valueClass);
					if (generics != null) serializer.setGenerics(kryo, generics);
					if (canBeNull) {
						kryo.writeObjectOrNull(output, value, serializer);
					} else {
						if (value == null) {
							throw new KryoException("Field value is null but canBeNull is false: " + this + " ("
								+ object.getClass().getName() + ")");
						}
						kryo.writeObject(output, value, serializer);
					}
				}
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field: " + this + " (" + object.getClass().getName() + ")", ex);
			} catch (KryoException ex) {
				ex.addTrace(this + " (" + object.getClass().getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(this + " (" + object.getClass().getName() + ")");
				throw ex;
			}
		}

		public void read (Input input, Object object) {
			try {
				if (TRACE) trace("kryo", "Read field: " + this + " (" + type.getName() + ")");
				Object value;

				Class concreteType = valueClass;
				Serializer serializer = this.serializer;
				if (concreteType == null) {
					Registration registration = kryo.readClass(input);
					if (registration == null)
						value = null;
					else {
						if (serializer == null) serializer = registration.getSerializer();
						if (generics != null) serializer.setGenerics(kryo, generics);
						value = kryo.readObject(input, registration.getType(), serializer);
					}
				} else {
					if (serializer == null) this.serializer = serializer = kryo.getSerializer(valueClass);
					if (generics != null) serializer.setGenerics(kryo, generics);
					if (canBeNull)
						value = kryo.readObjectOrNull(input, concreteType, serializer);
					else
						value = kryo.readObject(input, concreteType, serializer);
				}

				if (accessIndex != -1)
					((FieldAccess)access).set(object, accessIndex, value);
				else
					field.set(object, value);
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field: " + this + " (" + type.getName() + ")", ex);
			} catch (KryoException ex) {
				ex.addTrace(this + " (" + type.getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(this + " (" + type.getName() + ")");
				throw ex;
			}
		}

		public void copy (Object original, Object copy) {
			try {
				if (accessIndex != -1) {
					FieldAccess access = (FieldAccess)FieldSerializer.this.access;
					access.set(copy, accessIndex, kryo.copy(access.get(original, accessIndex)));
				} else
					field.set(copy, kryo.copy(field.get(original)));
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field: " + this + " (" + type.getName() + ")", ex);
			} catch (KryoException ex) {
				ex.addTrace(this + " (" + type.getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(this + " (" + type.getName() + ")");
				throw ex;
			}
		}
	}

	/** Indicates a field should be ignored when its declaring class is registered unless the {@link Kryo#getContext() context} has
	 * a value set for the specified key. This can be useful when a field must be serialized for one purpose, but not for another.
	 * Eg, a class for a networked application could have a field that should not be serialized and sent to clients, but should be
	 * serialized when stored on the server.
	 * @author Nathan Sweet <misc@n4te.com> */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	static public @interface Optional {
		public String value();
	}
}
