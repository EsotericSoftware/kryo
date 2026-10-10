/* Copyright (c) 2008-2026, Nathan Sweet
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
import static com.esotericsoftware.kryo.util.Log.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Generics;
import com.esotericsoftware.kryo.util.Generics.GenericType;
import com.esotericsoftware.kryo.util.Generics.GenericsHierarchy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;

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
 * @author Roman Levenstein {@literal <romixlev@gmail.com>} */
public class FieldSerializer<T> extends Serializer<T> {
	final Kryo kryo;
	final Class type;
	final FieldSerializerConfig config;
	final CachedFields cachedFields;
	private final GenericsHierarchy genericsHierarchy;

	// For records.
	final Constructor recordConstructor;

	/** Generated code that writes and reads the fields, or null if code generation is disabled or not possible for the type. */
	GeneratedFields generated;
	private final Object[] recordDefaults;

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

		if (isRecord(type)) {
			RecordComponent[] components = type.getRecordComponents();
			Class[] componentTypes = new Class[components.length];
			recordDefaults = new Object[components.length];
			for (int i = 0; i < components.length; i++) {
				componentTypes[i] = components[i].getType();
				if (componentTypes[i].isPrimitive()) recordDefaults[i] = Array.get(Array.newInstance(componentTypes[i], 1), 0);
			}
			try {
				recordConstructor = type.getDeclaredConstructor(componentTypes);
			} catch (NoSuchMethodException ex) {
				throw new KryoException("Unable to find canonical constructor: " + className(type), ex);
			}
			try {
				recordConstructor.setAccessible(true);
			} catch (RuntimeException ex) {
				if (DEBUG) debug("kryo", "Unable to set canonical constructor as accessible: " + className(type), ex);
			}
		} else {
			recordConstructor = null;
			recordDefaults = null;
		}

		cachedFields = new CachedFields(this);
		cachedFields.rebuild();
	}

	/** Called when {@link #getFields()} and {@link #getCopyFields()} have been repopulated. Subclasses can override this method to
	 * configure or remove cached fields. It is not called when a field is removed. */
	protected void initializeCachedFields () {
	}

	/** Called after the cached fields changed: after {@link #initializeCachedFields()}, which can remove fields, and after a field
	 * was removed. Subclasses in this package update what they derive from the fields here.
	 * @param fields The fields, before their hidden classes are defined on the first use, so this must not keep them. */
	void cachedFieldsChanged (CachedField[] fields) {
	}

	/** Called by {@link CachedFields} when the fields were built or a field was removed.
	 * @param fields The fields, before their hidden classes are defined on the first use. */
	final void fieldsChanged (CachedField[] fields) {
		cachedFieldsChanged(fields);
		regenerate();
	}

	/** Generates the code for the fields if {@link #codeGenerated()}. If that fails, the cached fields are used and rebuilt with
	 * their hidden classes, which {@link CachedFields} doesn't create when code is generated. */
	final void regenerate () {
		regenerate(true);
	}

	/** @param rebuild If false and no code could be generated, the cached fields are used as they are, eg while serializing. */
	final void regenerate (boolean rebuild) {
		generated = null;
		if (!codeGenerated()) return;
		try {
			generated = generateCode();
		} catch (KryoException ex) {
			if (DEBUG) debug("kryo", "Unable to generate code for the fields of: " + className(type), ex);
		}
		if (generated == null && rebuild && !codeGenerationFailed) {
			codeGenerationFailed = true;
			cachedFields.rebuild();
		}
	}

	/** True if code could not be generated for the fields, then the cached fields are used. Reset by {@link #updateFields()}. */
	private boolean codeGenerationFailed;

	/** Returns true if code is generated for the fields: {@link FieldSerializerConfig#setCodeGeneration(boolean)} is enabled, the
	 * platform supports it, the class is not a record and {@link #usesGeneratedCode()}. */
	final boolean codeGenerated () {
		return config.codeGeneration && CachedFields.codeGeneration && recordConstructor == null && !codeGenerationFailed
			&& usesGeneratedCode();
	}

	/** Returns true if the generated code can be used with the current config settings, which can be changed without
	 * {@link #updateFields()}. Subclasses with settings their generated code doesn't support return false. Called by the super
	 * constructor, so subclasses can only use {@link #config}. */
	boolean usesGeneratedCode () {
		return true;
	}

	/** Returns true if the class of each value is written before the value, like CompatibleFieldSerializer with unknown field
	 * data. Called by the super constructor, so subclasses can only use {@link #config}. */
	boolean writesClasses () {
		return false;
	}

	/** Returns the generated code for the fields, or null if it can't be generated. Subclasses pass their fields and options.
	 * Called by the super constructor, so subclasses can only use {@link #config}. */
	GeneratedFields generateCode () {
		return GeneratedFields.generate(this, cachedFields.fields(), writesClasses(), null);
	}

	/** Returns the generated code for the current config settings, or null if it isn't used. {@link #usesGeneratedCode()} and
	 * {@link #writesClasses()} can be changed without {@link #updateFields()}, so the code is regenerated if the setting it was
	 * generated for changed. */
	final GeneratedFields generated () {
		GeneratedFields generated = this.generated;
		if (generated == null || !usesGeneratedCode()) return null;
		if (generated.writesClasses != writesClasses()) {
			regenerate(false);
			return this.generated;
		}
		return generated;
	}

	/** Returns true if the generic type of a field is used to optimize the serialization of its value, eg to omit the class of
	 * collection elements. Then the value can only be read with the same generic type. */
	protected boolean optimizeGenerics () {
		return true;
	}

	/** If the returned config settings are modified, {@link #updateFields()} must be called. */
	public FieldSerializerConfig getFieldSerializerConfig () {
		return config;
	}

	/** Must be called after {@link #getFieldSerializerConfig()} settings are changed to repopulate the cached fields. */
	public void updateFields () {
		if (TRACE) trace("kryo", "Update fields: " + className(type));
		codeGenerationFailed = false;
		cachedFields.rebuild();
	}

	public void write (Kryo kryo, Output output, T object) {
		int pop = pushTypeVariables();

		if (generated() != null) {
			writeGenerated(output, object, null);
			popTypeVariables(pop);
			return;
		}

		CachedField[] fields = cachedFields.fields();
		for (int i = 0, n = fields.length; i < n; i++) {
			if (TRACE) log("Write", fields[i], output.position());
			try {
				fields[i].write(output, object);
			} catch (KryoException e) {
				throw e;
			} catch (Exception e) {
				throw new KryoException("Error writing " + fields[i] + " at position " + output.position(), e);
			}
		}

		popTypeVariables(pop);
	}

	public T read (Kryo kryo, Input input, Class<? extends T> type) {
		int pop = pushTypeVariables();

		T object = null;
		Object[] values = null;
		if (recordConstructor == null) {
			object = create(kryo, input, type);
			kryo.reference(object);
		} else
			values = newRecordValues();

		if (generated() != null) {
			readGenerated(input, object, null);
			popTypeVariables(pop);
			return object;
		}

		CachedField[] fields = cachedFields.fields();
		for (int i = 0, n = fields.length; i < n; i++) {
			if (TRACE) log("Read", fields[i], input.position());
			try {
				final CachedField field = fields[i];
				if (values == null)
					readField(field, input, object);
				else
					values[field.index] = field.read(input);
			} catch (KryoException e) {
				throw e;
			} catch (Exception e) {
				throw new KryoException("Error reading " + fields[i] + " at position " + input.position(), e);
			}
		}

		if (values != null) object = createRecord(values);

		popTypeVariables(pop);
		return object;
	}

	/** Writes all fields with {@link #generated}.
	 * @param chunks May be null. */
	void writeGenerated (Output output, Object object, ChunkedEncoding chunks) {
		try {
			if (chunks == null)
				generated.write(output, object);
			else
				generated.write(output, object, chunks);
		} catch (KryoException e) {
			throw e;
		} catch (Exception e) {
			throw new KryoException("Error writing " + className(type) + " at position " + output.position(), e);
		}
	}

	/** Reads all fields with {@link #generated}.
	 * @param chunks May be null. */
	void readGenerated (Input input, Object object, ChunkedEncoding chunks) {
		try {
			if (chunks == null)
				generated.read(input, object);
			else
				generated.read(input, object, chunks);
		} catch (KryoException e) {
			throw e;
		} catch (Exception e) {
			throw new KryoException("Error reading " + className(type) + " at position " + input.position(), e);
		}
	}

	/** Reads the value of a field and sets it, with {@link FinalFieldSetter} for a final field if needed. */
	void readField (CachedField field, Input input, Object object) {
		FinalFieldSetter setter = finalSetter(field);
		if (setter == null)
			field.read(input, object);
		else
			setter.set(object, field.read(input));
	}

	/** Copies the value of a field, with {@link FinalFieldSetter} for a final field if needed. */
	void copyField (Kryo kryo, CachedField field, Object original, Object copy) {
		FinalFieldSetter setter = finalSetter(field);
		if (setter == null) {
			field.copy(original, copy);
			return;
		}
		try {
			Object value = field.get(original);
			// Primitive values are immutable, all other values are copied like other field values.
			setter.set(copy, field.field.getType().isPrimitive() ? value : kryo.copy(value));
		} catch (IllegalAccessException ex) {
			throw new KryoException("Error accessing field: " + field.name + " (" + className(type) + ")", ex);
		} catch (KryoException ex) {
			ex.addTrace(field.name + " (" + className(type) + ")");
			throw ex;
		}
	}

	/** Returns a new array for the component values of a record, indexed by {@link CachedField#index}. */
	Object[] newRecordValues () {
		return new Object[recordDefaults.length];
	}

	/** Creates a record using its canonical constructor. Components without a value, for example because they were not present in
	 * the serialized data, are set to their default value. */
	T createRecord (Object[] values) {
		Object[] defaults = recordDefaults;
		for (int i = 0, n = values.length; i < n; i++)
			if (values[i] == null) values[i] = defaults[i];
		try {
			return (T)recordConstructor.newInstance(values);
		} catch (InvocationTargetException ex) {
			throw new KryoException("Error constructing record: " + className(type), ex.getCause());
		} catch (Exception ex) {
			throw new KryoException("Error constructing record: " + className(type), ex);
		}
	}

	/** Prepares the type variables for the serialized type. Must be balanced with {@link #popTypeVariables(int)} if {@code > 0} is
	 * returned. */
	protected int pushTypeVariables () {
		Generics generics = kryo.getGenerics();
		GenericType genericType = generics.nextGenericType();
		if (genericType == null) return 0;
		// nextGenericType pushes the last type argument for the values of a collection or map. It must not be used for the fields,
		// which don't push their own generic type if optimizeGenerics is false.
		generics.popGenericType();

		int pop = generics.pushTypeVariables(genericsHierarchy, genericType);
		if (TRACE && pop > 0) trace("kryo", "Generics: " + generics);
		return pop;
	}

	protected void popTypeVariables (int pop) {
		Generics generics = kryo.getGenerics();
		if (pop > 0) {
			generics.popTypeVariables(pop);
		}
		generics.popGenericType();
	}

	/** Returns the setter of a final field, resolving it on the first call, see {@link CachedField#finalUnresolved}. Null if the
	 * field is set with reflection. */
	static FinalFieldSetter finalSetter (CachedField field) {
		if (field.finalUnresolved) {
			field.finalUnresolved = false;
			field.finalSetter = FinalFieldSetter.create(field.field);
		}
		return field.finalSetter;
	}

	/** Sets a final field with its {@link FinalFieldSetter}, or with Unsafe for an Unsafe field. Called by the generated code if
	 * setting the field with reflection is denied. */
	static void setFinal (CachedField field, Object object, Object value) {
		FinalFieldSetter setter = finalSetter(field);
		if (setter != null)
			setter.set(object, value);
		else if (field.offset != 0)
			UnsafeField.put(field, object, value);
		else {
			throw ReflectField.accessError(field.field,
				new IllegalAccessException("Setting final fields with reflection is denied."));
		}
	}

	/** Sets a non-primitive field to null. */
	void setNull (CachedField cachedField, Object object) {
		if (cachedField.field.getType().isPrimitive()) return;
		FinalFieldSetter setter = finalSetter(cachedField);
		if (setter != null) {
			setter.set(object, null);
			return;
		}
		try {
			cachedField.field.set(object, null);
		} catch (IllegalAccessException ex) {
			throw new KryoException("Error setting field to null: " + cachedField, ex);
		}
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
		for (CachedField cachedField : cachedFields.fields())
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
		return cachedFields.fields();
	}

	/** Returns the fields used for copying. */
	public CachedField[] getCopyFields () {
		return cachedFields.copyFields();
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
		final CachedField[] copyFields = cachedFields.copyFields();
		if (recordConstructor == null) {
			T copy = createCopy(kryo, original);
			kryo.reference(copy);
			for (int i = 0, n = copyFields.length; i < n; i++)
				copyField(kryo, copyFields[i], original, copy);
			return copy;
		}

		Object[] values = newRecordValues();
		for (int i = 0, n = copyFields.length; i < n; i++) {
			CachedField field = copyFields[i];
			try {
				Object value = field.get(original);
				// Primitive values are immutable, all other values are copied like other field values.
				values[field.index] = field.field.getType().isPrimitive() ? value : kryo.copy(value);
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field: " + field.name + " (" + className(type) + ")", ex);
			} catch (KryoException ex) {
				ex.addTrace(field.name + " (" + className(type) + ")");
				throw ex;
			}
		}
		return createRecord(values);
	}

	/** Settings for serializing a field. */
	public abstract static class CachedField {
		final Field field;
		String name;
		Class valueClass;
		Serializer serializer;
		boolean canBeNull, varEncoding = true, optimizePositive, reuseSerializer = true;

		// For Records
		int index;

		/** Sets the field if it is final and setting it with reflection is denied, else null. */
		FinalFieldSetter finalSetter;
		/** True for a final field until it is first set, then {@link #finalSetter} is resolved. Resolving it obtains a method
		 * handle for setting the field, which Java 26+ warns about like setting the field with reflection, so it is only done when
		 * Kryo sets a final field, not when a serializer is created, eg to write objects. */
		boolean finalUnresolved;

		// For UnsafeField.
		long offset;

		// For TaggedFieldSerializer.
		int tag;

		public CachedField (Field field) {
			this.field = field;
		}

		/** Copies the settings of another cached field for the same field, which this field replaces. */
		void copySettings (CachedField from) {
			name = from.name;
			valueClass = from.valueClass;
			serializer = from.serializer;
			canBeNull = from.canBeNull;
			varEncoding = from.varEncoding;
			optimizePositive = from.optimizePositive;
			reuseSerializer = from.reuseSerializer;
			index = from.index;
			tag = from.tag;
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

		/** @deprecated Has no effect, variable length int and long values are always written optimized for both negative and
		 *             positive values. Will be removed in Kryo 7. */
		@Deprecated
		public void setOptimizePositive (boolean optimizePositive) {
			this.optimizePositive = optimizePositive;
		}

		/** @deprecated See {@link #setOptimizePositive(boolean)}. */
		@Deprecated
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

		public abstract Object read (Input input);

		public abstract void copy (Object original, Object copy);

		Object get (Object object) throws IllegalAccessException {
			return field.get(object);
		}
	}

	/** Indicates a field should be ignored when its declaring class is registered unless the {@link Kryo#getContext() context} has
	 * a value set for a key specified by at least one of the {@link Optional} annotations. */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Optionals {
		Optional[] value();
	}

	/** Indicates a field should be ignored when its declaring class is registered unless the {@link Kryo#getContext() context} has
	 * a value set for the specified key. This can be useful when a field must be serialized for one purpose, but not for another.
	 * Eg, a class for a networked application could have a field that should not be serialized and sent to clients, but should be
	 * serialized when stored on the server. If a field has multiple of this annotation, then the field is serialized if at least
	 * one of the keys is present in the context.
	 * @author Nathan Sweet */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@Repeatable(Optionals.class)
	public @interface Optional {
		String value();
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

		/** @deprecated Has no effect, see {@link CachedField#setOptimizePositive(boolean)}. */
		@Deprecated
		boolean optimizePositive() default false;
	}

	/** Indicates a field can never be null when it is being serialized and deserialized. Some serializers use this to save space.
	 * Eg, {@link FieldSerializer} may save 1 byte per field.
	 * @author Nathan Sweet */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface NotNull {
	}

	/** How {@link FieldSerializer} reads and writes fields. If a field can't be accessed this way, VarHandles are used, and
	 * reflection if they can't be used either, eg for final fields, which can't be written with VarHandles, or for records, which
	 * are never accessed with Unsafe. Reflection works for all fields. */
	public enum FieldAccessType {
		/** {@code sun.misc.Unsafe}, if available. Fastest, but deprecated for removal by Java. */
		UNSAFE,
		/** {@link java.lang.invoke.VarHandle} for non-final fields. Where hidden classes can be defined, which is not on Android or
		 * in a native image, each field is accessed by a hidden class that has the VarHandle as a constant, which is much faster
		 * and close to Unsafe. */
		VARHANDLE,
		/** {@link Field} reflection. */
		REFLECTION
	}

	/** Configuration for FieldSerializer instances. */
	public static class FieldSerializerConfig implements Cloneable {
		/** The value of the system property "kryo.fieldAccess" if it is set. Otherwise Unsafe where it can be used without a
		 * warning, otherwise VarHandles. Java warns about Unsafe memory access since Java 24, unless it is allowed with
		 * {@code --sun-misc-unsafe-memory-access=allow}. On Android, which has VarHandles only since API level 33, reflection. */
		static final FieldAccessType defaultFieldAccess;
		static {
			String memoryAccess = System.getProperty("sun.misc.unsafe.memory.access");
			String configured = System.getProperty("kryo.fieldAccess");
			if (configured != null) {
				try {
					defaultFieldAccess = FieldAccessType.valueOf(configured);
				} catch (IllegalArgumentException ex) {
					throw new KryoException("Invalid value of the system property kryo.fieldAccess: " + configured, ex);
				}
			} else if (isAndroid)
				defaultFieldAccess = FieldAccessType.REFLECTION;
			else if (!unsafe)
				defaultFieldAccess = FieldAccessType.VARHANDLE;
			else if (memoryAccess != null)
				defaultFieldAccess = memoryAccess.equals("allow") ? FieldAccessType.UNSAFE : FieldAccessType.VARHANDLE;
			else
				defaultFieldAccess = Runtime.version().feature() < 24 ? FieldAccessType.UNSAFE : FieldAccessType.VARHANDLE;
			if (DEBUG) {
				debug("kryo", "Default field access: " + defaultFieldAccess + (isAndroid ? " (Android)"
					: " (Java " + Runtime.version().feature() + ", Unsafe available: " + unsafe + ", Unsafe memory access: "
						+ (memoryAccess == null ? "default" : memoryAccess) + ")")
					+ ", hidden classes: " + CachedFields.hiddenFields + ", code generation available: "
					+ CachedFields.codeGeneration);
			}
		}

		/** True if the system property "kryo.codeGeneration" is "true". */
		static final boolean defaultCodeGeneration = "true".equals(System.getProperty("kryo.codeGeneration"));
		static {
			if (defaultCodeGeneration && !CachedFields.codeGeneration && WARN)
				warn("kryo", "The system property kryo.codeGeneration is true. " + CachedFields.codeGenerationUnavailable());
		}

		FieldAccessType fieldAccess = defaultFieldAccess;
		boolean codeGeneration = defaultCodeGeneration;
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
				return (FieldSerializerConfig)super.clone(); // Clone is ok as we have only primitive and immutable fields.
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
		 * @param setFieldsAsAccessible If true, all non-transient fields (including private fields) will be serialized and
		 *           {@link java.lang.reflect.Field#setAccessible(boolean) set as accessible} (default). If false, only public,
		 *           non-final fields of public classes will be serialized, which can be accessed without setAccessible. */
		public void setFieldsAsAccessible (boolean setFieldsAsAccessible) {
			this.setFieldsAsAccessible = setFieldsAsAccessible;
			if (TRACE) trace("kryo", "FieldSerializerConfig setFieldsAsAccessible: " + setFieldsAsAccessible);
		}

		public boolean getSetFieldsAsAccessible () {
			return setFieldsAsAccessible;
		}

		/** Controls if synthetic fields are serialized, which the compiler generates, eg the outer instance and the captured
		 * variables of anonymous classes, local classes and non-static member classes. Without them, these objects have a null
		 * outer instance after reading. The synthetic fields can refer to large or sensitive object graphs, and an inner object
		 * that is serialized with its outer instance often needs references, because the outer instance refers to the inner object.
		 * @param ignoreSyntheticFields True to never serialize synthetic fields (default), false to always serialize them. */
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

		/** Sets how fields are read and written. Default is the value of the system property "kryo.fieldAccess" if it is set,
		 * otherwise {@link FieldAccessType#UNSAFE} where Unsafe can be used without a warning, which is before Java 24 or with
		 * {@code --sun-misc-unsafe-memory-access=allow}, {@link FieldAccessType#REFLECTION} on Android, otherwise
		 * {@link FieldAccessType#VARHANDLE}. Changes take effect for new serializers or after
		 * {@link FieldSerializer#updateFields()}. */
		public void setFieldAccess (FieldAccessType fieldAccess) {
			if (fieldAccess == null) throw new IllegalArgumentException("fieldAccess cannot be null.");
			this.fieldAccess = fieldAccess;
			if (TRACE) trace("kryo", "FieldSerializerConfig fieldAccess: " + fieldAccess);
		}

		public FieldAccessType getFieldAccess () {
			return fieldAccess;
		}

		/** If true, the code that writes and reads the fields of a class is generated as a hidden class, which the JIT can optimize
		 * much better than the loop over the cached fields: there is no virtual call per field and the field accessors are
		 * constants. The generated code writes the same bytes. Used by FieldSerializer and its subclasses, except with the chunked
		 * encoding of Kryo 5. The class is written with the Class-File API on Java 24+, or with ASM on older Java versions, which
		 * is an optional dependency. Not available on Android or in a native image. The cached fields are used where code can't be
		 * generated, eg for records. Default is false, or true if the system property "kryo.codeGeneration" is "true". */
		public void setCodeGeneration (boolean codeGeneration) {
			this.codeGeneration = codeGeneration;
			if (TRACE) trace("kryo", "FieldSerializerConfig codeGeneration: " + codeGeneration);
			if (codeGeneration && !CachedFields.codeGeneration && WARN) warn("kryo", CachedFields.codeGenerationUnavailable());
		}

		public boolean getCodeGeneration () {
			return codeGeneration;
		}
	}
}
