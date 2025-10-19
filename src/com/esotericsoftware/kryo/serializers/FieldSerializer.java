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

import static com.esotericsoftware.kryo.util.Util.*;
import static com.esotericsoftware.minlog.Log.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Generics;
import com.esotericsoftware.kryo.util.Generics.GenericType;
import com.esotericsoftware.kryo.util.Generics.GenericsHierarchy;
import com.esotericsoftware.reflectasm.FieldAccess;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

/** Serializes objects using direct field assignment. FieldSerializer is generic and can serialize most classes without any
 * configuration. All non-public fields are written and read by default, so it is important to evaluate each class that will be
 * serialized. If fields are public, serialization may be faster.
 * <p>
 * FieldSerializer is efficient by writing only the field data, without any schema information, using the Java class files as the
 * schema. It does not support adding, removing, or changing the type of fields without invalidating previously serialized bytes.
 * Renaming fields is allowed only if it doesn't change the alphabetical order of the fields.
 * <p>
 * FieldSerializer's compatibility drawbacks can be acceptable in many situations, such as when sending data over a network, but
 * may not be a good choice for long term data storage because the Java classes cannot evolve. Subclasses provided more flexible
 * compatibility.
 * @see Serializer
 * @see Kryo#register(Class, Serializer)
 * @see VersionFieldSerializer
 * @see TaggedFieldSerializer
 * @see CompatibleFieldSerializer
 * @author Nathan Sweet
 * @author Roman Levenstein <romixlev@gmail.com> */
public class FieldSerializer<T> extends Serializer<T> {
	final Kryo kryo;
	final Class type;
	final FieldSerializerConfig config;
	final CachedFields cachedFields;
	private final GenericsHierarchy genericsHierarchy;

	public FieldSerializer (Kryo kryo, Class type) {
		this(kryo, type, new FieldSerializerConfig());
	}

	public FieldSerializer (Kryo kryo, Class type, FieldSerializerConfig config) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (type.isPrimitive()) throw new IllegalArgumentException("type cannot be a primitive class: " + type);
		if (config == null) throw new IllegalArgumentException("config cannot be null.");
		this.kryo = kryo;
		this.type = type;
		this.config = config;

		final Generics generics = kryo.getGenerics();
		genericsHierarchy = generics.buildHierarchy(type);

		cachedFields = new CachedFields(this);
		cachedFields.rebuild();
	}

	/** Called when {@link #getFields()} and {@link #getCopyFields()} have been repopulated. Subclasses can override this method to
	 * configure or remove cached fields. */
	protected void initializeCachedFields () {
	}

	/** If the returned config settings are modified, {@link #updateFields()} must be called. */
	public FieldSerializerConfig getFieldSerializerConfig () {
		return config;
	}

	/** Must be called after {@link #getFieldSerializerConfig()} settings are changed to repopulate the cached fields. */
	public void updateFields () {
		if (TRACE) trace("kryo", "Update fields: " + className(type));
		cachedFields.rebuild();
	}

	public void write (Kryo kryo, Output output, T object) {
		int pop = pushTypeVariables();

		CachedField[] fields = cachedFields.fields;
		for (int i = 0, n = fields.length; i < n; i++) {
			if (TRACE) log("Write", fields[i], output.position());
			try {
				fields[i].write(output, object);
			} catch (KryoException e) {
				throw e;
			} catch (OutOfMemoryError | Exception e) {
				throw new KryoException("Error writing " + fields[i] + " at position " + output.position(), e);
			}
		}

		popTypeVariables(pop);
	}

	public T read (Kryo kryo, Input input, Class<? extends T> type) {
		int pop = pushTypeVariables();

		T object = create(kryo, input, type);
		kryo.reference(object);

		CachedField[] fields = cachedFields.fields;
		for (int i = 0, n = fields.length; i < n; i++) {
			if (TRACE) log("Read", fields[i], input.position());
			try {
				fields[i].read(input, object);
			} catch (KryoException e) {
				throw e;
			} catch (OutOfMemoryError | Exception e) {
				throw new KryoException("Error reading " + fields[i] + " at position " + input.position(), e);
			}
		}

		popTypeVariables(pop);
		return object;
	}

	/** Prepares the type variables for the serialized type. Must be balanced with {@link #popTypeVariables(int)} if >0 is
	 * returned. */
	protected int pushTypeVariables () {
		GenericType[] genericTypes = kryo.getGenerics().nextGenericTypes();
		if (genericTypes == null) return 0;

		int pop = kryo.getGenerics().pushTypeVariables(genericsHierarchy, genericTypes);
		if (TRACE && pop > 0) trace("kryo", "Generics: " + kryo.getGenerics());
		return pop;
	}

	protected void popTypeVariables (int pop) {
		Generics generics = kryo.getGenerics();
		if (pop > 0) {
			generics.popTypeVariables(pop);
		}
		generics.popGenericType();
	}

	/** Used by {@link #read(Kryo, Input, Class)} to create the new object. This can be overridden to customize object creation, eg
	 * to call a constructor with arguments. The default implementation uses {@link Kryo#newInstance(Class)}. */
	protected T create (Kryo kryo, Input input, Class<? extends T> type) {
		return kryo.newInstance(type);
	}

	protected void log (String prefix, CachedField cachedField, int position) {
		String fieldClassName;
		if (cachedField instanceof ReflectField) {
			ReflectField reflectField = (ReflectField)cachedField;
			Class fieldClass = reflectField.resolveFieldClass();
			if (fieldClass == null) fieldClass = cachedField.field.getType();
			fieldClassName = simpleName(fieldClass, reflectField.genericType);
		} else {
			if (cachedField.valueClass != null)
				fieldClassName = cachedField.valueClass.getSimpleName();
			else
				fieldClassName = cachedField.field.getType().getSimpleName();
		}
		trace("kryo", prefix + " field " + fieldClassName + ": " + cachedField.name + " ("
			+ className(cachedField.field.getDeclaringClass()) + ')' + pos(position));
	}

	/** Returns the field with the specified name, allowing field specific settings to be configured. */
	public CachedField getField (String fieldName) {
		for (CachedField cachedField : cachedFields.fields)
			if (cachedField.name.equals(fieldName)) return cachedField;
		throw new IllegalArgumentException("Field \"" + fieldName + "\" not found on class: " + type.getName());
	}

	/** Removes a field so that it won't be serialized. */
	public void removeField (String fieldName) {
		cachedFields.removeField(fieldName);
	}

	/** Removes a field so that it won't be serialized. */
	public void removeField (CachedField field) {
		cachedFields.removeField(field);
	}

	/** Returns the fields used for serialization. */
	public CachedField[] getFields () {
		return cachedFields.fields;
	}

	/** Returns the fields used for copying. */
	public CachedField[] getCopyFields () {
		return cachedFields.copyFields;
	}

	public Class getType () {
		return type;
	}

	public Kryo getKryo () {
		return kryo;
	}

	/** Used by {@link #copy(Kryo, Object)} to create a new object. This can be overridden to customize object creation, eg to call
	 * a constructor with arguments. The default implementation uses {@link Kryo#newInstance(Class)}. */
	protected T createCopy (Kryo kryo, T original) {
		return (T)kryo.newInstance(original.getClass());
	}

	public T copy (Kryo kryo, T original) {
		T copy = createCopy(kryo, original);
		kryo.reference(copy);

		for (int i = 0, n = cachedFields.copyFields.length; i < n; i++)
			cachedFields.copyFields[i].copy(original, copy);

		return copy;
	}

	/** Settings for serializing a field. */
	public abstract static class CachedField {
		final Field field;
		String name;
		Class valueClass;
		Serializer serializer;
		boolean canBeNull, varEncoding = true, optimizePositive, reuseSerializer = true;

		// For AsmField.
		FieldAccess access;
		int accessIndex = -1;

		// For UnsafeField.
		long offset;

		// For TaggedFieldSerializer.
		int tag;

		public CachedField (Field field) {
			this.field = field;
		}

		/** The concrete class of the values for this field, or null if it is not known. This saves 1-2 bytes. Only set to a
		 * non-null value if the values for this field are known to be of the specified type (or null). Default is the field type if
		 * it is a primitive, primitive wrapper, or final or if {@link FieldSerializerConfig#setFixedFieldTypes(boolean)} is
		 * true. */
		public void setValueClass (Class valueClass) {
			this.valueClass = valueClass;
		}

		/** @return May be null. */
		public Class getValueClass () {
			return valueClass;
		}

		/** Sets both {@link #setValueClass(Class)} and {@link #setSerializer(Serializer)}. */
		public void setValueClass (Class valueClass, Serializer serializer) {
			this.valueClass = valueClass;
			this.serializer = serializer;
		}

		/** The serializer to be used for this field, or null to use the serializer registered with {@link Kryo} for the type. Some
		 * serializers require the {@link #setValueClass(Class) value class} to also be set. Default is null. */
		public void setSerializer (Serializer serializer) {
			this.serializer = serializer;
		}

		/** @return May be null. */
		public Serializer getSerializer () {
			return this.serializer;
		}

		/** When false, it is assumed the field value can never be null. This saves 0-1 bytes. Default is false for primitives,
		 * otherwise {@link FieldSerializerConfig#setFieldsCanBeNull(boolean)} is used unless the field has the {@link NotNull}
		 * annotation.
		 * <p>
		 * If the field type is a type variable, the default value is used. */
		public void setCanBeNull (boolean canBeNull) {
			this.canBeNull = canBeNull;
		}

		public boolean getCanBeNull () {
			return canBeNull;
		}

		/** When true, variable length encoding is used for int or long fields. Default is true.
		 * @see FieldSerializerConfig#setVariableLengthEncoding(boolean)
		 * @see Output#setVariableLengthEncoding(boolean)
		 * @see Input#setVariableLengthEncoding(boolean) */
		public void setVariableLengthEncoding (boolean varEncoding) {
			this.varEncoding = varEncoding;
		}

		public boolean getVariableLengthEncoding () {
			return varEncoding;
		}

		/** When true, variable length int and long values are written with fewer bytes for positive values and more bytes for
		 * negative values. Default is false. */
		public void setOptimizePositive (boolean optimizePositive) {
			this.optimizePositive = optimizePositive;
		}

		public boolean getOptimizePositive () {
			return optimizePositive;
		}

		/** When true, serializers are re-used for all instances of the field if the {@link #valueClass} is known. Re-using
		 * serializers is significantly faster than looking them up for every read/write. However, this only works reliably when the
		 * {@link #valueClass} of the field never changes. Serializers that do not guarantee this must set the flag to false. */
		void setReuseSerializer (boolean reuseSerializer) {
			this.reuseSerializer = reuseSerializer;
		}

		boolean getReuseSerializer () {
			return reuseSerializer;
		}

		public String getName () {
			return name;
		}

		public Field getField () {
			return field;
		}

		public String toString () {
			return name;
		}

		public abstract void write (Output output, Object object);

		public abstract void read (Input input, Object object);

		public abstract void copy (Object original, Object copy);

	}

	/** Indicates a field should be ignored when its declaring class is registered unless the {@link Kryo#getContext() context} has
	 * a value set for the specified key. This can be useful when a field must be serialized for one purpose, but not for another.
	 * Eg, a class for a networked application could have a field that should not be serialized and sent to clients, but should be
	 * serialized when stored on the server.
	 * @author Nathan Sweet */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Optional {
		public String value();
	}

	/** Used to annotate fields with a specific Kryo serializer.
	 * @see CachedField#setSerializer(Serializer) */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Bind {
		/** @see CachedField#setValueClass(Class) */
		Class valueClass() default Object.class;

		/** The serializer class to serialize the annotated field, which will be created by the {@link #serializerFactory()}. Can be
		 * omitted if the serializer factory knows what type of serializer to create.
		 * @see CachedField#setSerializer(Serializer) */
		Class<? extends Serializer> serializer() default Serializer.class;

		/** The factory used to create the serializer. */
		Class<? extends SerializerFactory> serializerFactory() default SerializerFactory.class;

		/** @see CachedField#setCanBeNull(boolean) */
		boolean canBeNull() default true;

		/** @see CachedField#setVariableLengthEncoding(boolean) */
		boolean variableLengthEncoding() default true;

		/** @see CachedField#setOptimizePositive(boolean) */
		boolean optimizePositive() default false;
	}

	/** Indicates a field can never be null when it is being serialized and deserialized. Some serializers use this to save space.
	 * Eg, {@link FieldSerializer} may save 1 byte per field.
	 * @author Nathan Sweet */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface NotNull {
	}

	/** Configuration for FieldSerializer instances. */
	public static class FieldSerializerConfig implements Cloneable {
		boolean fieldsCanBeNull = true;
		boolean setFieldsAsAccessible = true;
		boolean ignoreSyntheticFields = true;
		boolean fixedFieldTypes;
		boolean copyTransient = true;
		boolean serializeTransient;
		boolean varEncoding = true;
		boolean extendedFieldNames;

		public FieldSerializerConfig clone () {
			try {
				return (FieldSerializerConfig)super.clone(); // Clone is ok as we have only primitive fields.
			} catch (CloneNotSupportedException ex) {
				throw new KryoException(ex);
			}
		}

		/** Sets the default value for {@link FieldSerializer.CachedField#setCanBeNull(boolean)}.
		 * @param fieldsCanBeNull False if none of the fields are null. Saves 0-1 byte per field. True if it is not known
		 *           (default). */
		public void setFieldsCanBeNull (boolean fieldsCanBeNull) {
			this.fieldsCanBeNull = fieldsCanBeNull;
			if (TRACE) trace("kryo", "FieldSerializerConfig fieldsCanBeNull: " + fieldsCanBeNull);
		}

		public boolean getFieldsCanBeNull () {
			return fieldsCanBeNull;
		}

		/** Controls which fields are serialized.
		 * @param setFieldsAsAccessible If true, all non-transient fields (inlcuding private fields) will be serialized and
		 *           {@link java.lang.reflect.Field#setAccessible(boolean) set as accessible} if necessary (default). If false, only
		 *           fields in the public API will be serialized. */
		public void setFieldsAsAccessible (boolean setFieldsAsAccessible) {
			this.setFieldsAsAccessible = setFieldsAsAccessible;
			if (TRACE) trace("kryo", "FieldSerializerConfig setFieldsAsAccessible: " + setFieldsAsAccessible);
		}

		public boolean getSetFieldsAsAccessible () {
			return setFieldsAsAccessible;
		}

		/** Controls if synthetic fields are serialized. Default is true.
		 * @param ignoreSyntheticFields If true, only non-synthetic fields will be serialized. */
		public void setIgnoreSyntheticFields (boolean ignoreSyntheticFields) {
			this.ignoreSyntheticFields = ignoreSyntheticFields;
			if (TRACE) trace("kryo", "FieldSerializerConfig ignoreSyntheticFields: " + ignoreSyntheticFields);
		}

		public boolean getIgnoreSyntheticFields () {
			return ignoreSyntheticFields;
		}

		/** Sets the default value for {@link FieldSerializer.CachedField#setValueClass(Class)} to the field's declared type. This
		 * allows FieldSerializer to be more efficient, since it knows field values will not be a subclass of their declared type.
		 * Default is false. */
		public void setFixedFieldTypes (boolean fixedFieldTypes) {
			this.fixedFieldTypes = fixedFieldTypes;
			if (TRACE) trace("kryo", "FieldSerializerConfig fixedFieldTypes: " + fixedFieldTypes);
		}

		public boolean getFixedFieldTypes () {
			return fixedFieldTypes;
		}

		/** If false, when {@link Kryo#copy(Object)} is called all transient fields that are accessible will be ignored from being
		 * copied. Default is true. */
		public void setCopyTransient (boolean copyTransient) {
			this.copyTransient = copyTransient;
			if (TRACE) trace("kryo", "FieldSerializerConfig copyTransient: " + copyTransient);
		}

		public boolean getCopyTransient () {
			return copyTransient;
		}

		/** If set, transient fields will be serialized. Default is false. */
		public void setSerializeTransient (boolean serializeTransient) {
			this.serializeTransient = serializeTransient;
			if (TRACE) trace("kryo", "FieldSerializerConfig serializeTransient: " + serializeTransient);
		}

		public boolean getSerializeTransient () {
			return serializeTransient;
		}

		/** When true, variable length values are used for int and long fields. Default is true.
		 * @see CachedField#setVariableLengthEncoding(boolean)
		 * @see Output#setVariableLengthEncoding(boolean)
		 * @see Input#setVariableLengthEncoding(boolean) */
		public void setVariableLengthEncoding (boolean varEncoding) {
			this.varEncoding = varEncoding;
			if (TRACE) trace("kryo", "FieldSerializerConfig variable length encoding: " + varEncoding);
		}

		public boolean getVariableLengthEncoding () {
			return varEncoding;
		}

		/** When true, field names are prefixed by their declaring class. This can avoid conflicts when a subclass has a field with
		 * the same name as a super class. Default is false. */
		public void setExtendedFieldNames (boolean extendedFieldNames) {
			this.extendedFieldNames = extendedFieldNames;
			if (TRACE) trace("kryo", "FieldSerializerConfig extendedFieldNames: " + extendedFieldNames);
		}

		public boolean getExtendedFieldNames () {
			return extendedFieldNames;
		}
	}
}
