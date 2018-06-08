/* Copyright (c) 2008-2018, Nathan Sweet
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
import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.ReflectionSerializerFactory;
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
 * configuration. It is efficient by writing only the field data, without any schema information, using the Java class files as
 * the schema. It does not support adding, removing, or changing the type of fields without invalidating previously serialized
 * bytes. This can be acceptable in many situations, such as when sending data over a network, but may not be a good choice for
 * long term data storage because the Java classes cannot evolve. Because FieldSerializer attempts to read and write all
 * non-public fields by default, it is important to evaluate each class that will be serialized. If fields are public, faster
 * bytecode generation will be used instead of reflection.
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
		if (config == null) throw new IllegalArgumentException("config cannot be null.");
		this.kryo = kryo;
		this.type = type;
		this.config = config;

		genericsHierarchy = new GenericsHierarchy(type);

		cachedFields = new CachedFields(this);
		cachedFields.rebuild();
	}

	/** Called when {@link #getFields()} and {@link #getCopyFields()} have been repopulated. Subclasses can override this method to
	 * configure or remove cached fields. */
	protected void initializeCachedFields () {
	}

	/** If the returned config settings are modified, {@link #updateConfig()} must be called. */
	public FieldSerializerConfig getFieldSerializerConfig () {
		return config;
	}

	/** Must be called after {@link #getFieldSerializerConfig()} settings are changed to repopulate the cached fields. */
	public void updateConfig () {
		if (TRACE) trace("kryo", "Update FieldSerializerConfig: " + className(type));
		cachedFields.rebuild();
	}

	public void write (Kryo kryo, Output output, T object) {
		int pop = pushTypeVariables();

		CachedField[] fields = cachedFields.fields;
		for (int i = 0, n = fields.length; i < n; i++) {
			if (TRACE) log("Write", fields[i], output.position());
			fields[i].write(output, object);
		}

		if (pop > 0) popTypeVariables(pop);
	}

	public T read (Kryo kryo, Input input, Class<? extends T> type) {
		int pop = pushTypeVariables();

		T object = create(kryo, input, type);
		kryo.reference(object);

		CachedField[] fields = cachedFields.fields;
		for (int i = 0, n = fields.length; i < n; i++) {
			if (TRACE) log("Read", fields[i], input.position());
			fields[i].read(input, object);
		}

		if (pop > 0) popTypeVariables(pop);
		return object;
	}

	private int pushTypeVariables () {
		GenericType[] genericTypes = kryo.getGenerics().nextGenericTypes();
		if (genericTypes == null) return 0;

		int pop = kryo.getGenerics().pushTypeVariables(genericsHierarchy, genericTypes);
		if (TRACE && pop > 0) trace("kryo", "Generics: " + kryo.getGenerics());
		return pop;
	}

	private void popTypeVariables (int pop) {
		Generics generics = kryo.getGenerics();
		generics.popTypeVariables(pop);
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
	static public abstract class CachedField {
		final Field field;
		String name;
		Class valueClass;
		Serializer serializer;
		boolean canBeNull, varEncoding = true, optimizePositive;

		// For AsmField.
		FieldAccess access;
		int accessIndex = -1;

		// For UnsafeField.
		long offset;

		public CachedField (Field field) {
			this.field = field;
		}

		/** The concrete class of the values for this field, or null if it is not known. This saves 1-2 bytes. Only set to a
		 * non-null value if the field type in the class definition is final or the values for this field are known to be of the
		 * specified type. Default is true if the field type is final or {@link FieldSerializerConfig#setFixedFieldTypes(boolean)}
		 * is true.
		 * <p>
		 * If the field type is a type variable, the default value is used. */
		public void setClass (Class valueClass) {
			this.valueClass = valueClass;
		}

		/** Sets both {@link #setClass(Class)} and {@link #setSerializer(Serializer)}. */
		public void setClass (Class valueClass, Serializer serializer) {
			this.valueClass = valueClass;
			this.serializer = serializer;
		}

		/** The serializer to be used for this field when {@link #setClass(Class)} is not null, or null to use the serializer
		 * registered with {@link Kryo} for the type. Default is null. */
		public void setSerializer (Serializer serializer) {
			this.serializer = serializer;
		}

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

		public String getName () {
			return name;
		}

		public Field getField () {
			return field;
		}

		public String toString () {
			return name;
		}

		abstract public void write (Output output, Object object);

		abstract public void read (Input input, Object object);

		abstract public void copy (Object original, Object copy);
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
	 * @see com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField#setSerializer(Serializer) */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Bind {
		/** The serializer class to serialize the annotated field, which will be created by the {@link #serializerFactory()}. Can be
		 * omitted if the serializer factory knows what type of serializer to create. */
		Class<? extends Serializer> value() default Serializer.class;

		/** The factory used to create the serializer. */
		Class<? extends SerializerFactory> serializerFactory() default ReflectionSerializerFactory.class;
	}

	/** Used to annotate that a field uses a specific class.
	 * @see com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField#setClass(Class) */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface BindClass {
		/** The concrete class to use for all values of this field. */
		Class value();
	}
}
