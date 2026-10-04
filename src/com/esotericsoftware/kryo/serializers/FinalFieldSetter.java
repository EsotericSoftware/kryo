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
import static com.esotericsoftware.minlog.Log.*;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/** Sets the final fields of serializable classes if setting final fields with reflection is denied (JEP 500, eg with
 * {@code --illegal-final-field-mutation=deny}). Otherwise final fields are set with reflection like other fields, and this class
 * is not used. Whether it is denied is checked once, by setting a final field of {@link Probe}.
 * <p>
 * Java serialization is allowed to set final fields. Since Java 24, {@code ReflectionFactory.defaultReadObjectForSerialization}
 * provides a method handle that does what {@link ObjectInputStream#defaultReadObject()} does for one class: it sets all
 * serializable fields declared by that class, including final fields, to the values it gets from
 * {@link ObjectInputStream#readFields()}. This class calls that method handle with its own {@link ObjectInputStream}, which
 * doesn't read Java serialization data, but returns the field values read by Kryo:
 * <ul>
 * <li>While FieldSerializer and its subclasses read or copy an object, the values of final fields are collected in an array,
 * indexed by {@link CachedField#index}, like for records. All other fields are set as usual.
 * <li>After all fields are read, {@link #set(Object, Object[])} calls the method handle of each class with final fields.
 * {@link FieldsInput} returns the collected value for each final field, and the current value of the object for each other
 * serializable field, so those keep their values. A final field that was not read, eg because it was removed, keeps its value
 * too.
 * </ul>
 * The object is still created by the serializer before its fields are read, so references to it work as before. Only final fields
 * of serializable classes that are not transient can be set this way. If a class has other final fields, eg in a superclass that
 * isn't serializable, it is not used and setting these fields fails as before. */
final class FinalFieldSetter {
	static private final Object reflectionFactory;
	static private final Method defaultReadObject;
	/** A final field that was not read keeps its value. */
	static private final Object unset = new Object();
	/** Null until a class with final fields is used. Can be set by tests. */
	static Boolean denied;

	static {
		Object factory = null;
		Method method = null;
		if (!isAndroid) {
			try { // Java 24+.
				Class factoryClass = Class.forName("sun.reflect.ReflectionFactory");
				factory = factoryClass.getMethod("getReflectionFactory").invoke(null);
				method = factoryClass.getMethod("defaultReadObjectForSerialization", Class.class);
			} catch (Throwable ex) {
				if (TRACE) trace("kryo", "Final fields are set with reflection.", ex);
			}
		}
		reflectionFactory = factory;
		defaultReadObject = method;
	}

	private final ClassFields[] classes;
	private final int count;
	private final FieldsInput input;

	private FinalFieldSetter (ClassFields[] classes, int count) throws IOException {
		this.classes = classes;
		this.count = count;
		input = new FieldsInput();
	}

	/** Called if the class has final fields. Returns null if final fields are set with reflection: setting them with reflection is
	 * allowed, the method handles are not available, or a final field is not a serializable field of a serializable class.
	 * Otherwise sets {@link CachedField#index} for each final field. */
	static FinalFieldSetter create (Class type, CachedField[] fields, CachedField[] copyFields) {
		if (defaultReadObject == null || isRecord(type) || !mutationDenied()) return null;
		// Both arrays have the same CachedField for a field that is read and copied.
		LinkedHashSet<CachedField> finalFields = new LinkedHashSet<>();
		for (CachedField[] array : new CachedField[][] {fields, copyFields})
			for (CachedField cachedField : array)
				if (Modifier.isFinal(cachedField.field.getModifiers())) finalFields.add(cachedField);
		try {
			LinkedHashMap<Class, ClassFields> classes = new LinkedHashMap<>();
			int index = 0;
			for (CachedField cachedField : finalFields) {
				Class declaringClass = cachedField.field.getDeclaringClass();
				ClassFields classFields = classes.get(declaringClass);
				if (classFields == null) classes.put(declaringClass, classFields = new ClassFields(declaringClass));
				classFields.setValueIndex(cachedField.field.getName(), index++);
			}
			FinalFieldSetter setter = new FinalFieldSetter(classes.values().toArray(new ClassFields[classes.size()]), index);
			index = 0;
			for (CachedField cachedField : finalFields)
				cachedField.index = index++;
			return setter;
		} catch (Throwable ex) {
			if (DEBUG) debug("kryo", "Final fields are set with reflection: " + className(type), ex);
			return null;
		}
	}

	/** Returns true if setting final fields with reflection is denied (JEP 500), eg with
	 * {@code --illegal-final-field-mutation=deny}. Java has no API for this, so a final field of a class in Kryo's module is set
	 * once. If it is allowed with a warning, the warning is shown then. */
	static boolean mutationDenied () {
		if (denied == null) {
			try {
				Field field = Probe.class.getDeclaredField("value");
				field.setAccessible(true);
				field.set(new Probe(), null);
				denied = false;
			} catch (IllegalAccessException ex) {
				denied = true;
			} catch (Throwable ex) {
				if (DEBUG) debug("kryo", "Unable to check if final fields can be set.", ex);
				denied = false;
			}
			if (DEBUG && denied) debug("kryo", "Final fields of serializable classes are set with method handles.");
		}
		return denied;
	}

	static private final class Probe {
		final Object value;

		Probe () {
			value = null;
		}
	}

	Object[] newValues () {
		Object[] values = new Object[count];
		Arrays.fill(values, unset);
		return values;
	}

	/** Sets the final fields to the values, indexed by {@link CachedField#index}. */
	void set (Object object, Object[] values) {
		FieldsInput input = this.input;
		for (ClassFields classFields : classes) {
			input.classFields = classFields;
			input.object = object;
			input.values = values;
			input.next = 0;
			try {
				classFields.handle.invokeExact(object, (ObjectInputStream)input);
			} catch (Throwable ex) {
				throw new KryoException("Error setting final fields: " + className(classFields.type), ex);
			} finally {
				input.object = null;
				input.values = null;
			}
		}
	}

	/** The serializable fields of a class, which the method handle sets. */
	static final class ClassFields {
		final Class type;
		final MethodHandle handle;
		final ObjectStreamClass streamClass;
		final String[] names;
		final Field[] fields;
		final int[] valueIndexes; // -1 if the field keeps its current value.

		ClassFields (Class type) throws Exception {
			this.type = type;
			if (!Serializable.class.isAssignableFrom(type)) throw new KryoException("Class is not serializable.");
			MethodHandle handle = (MethodHandle)defaultReadObject.invoke(reflectionFactory, type);
			if (handle == null) throw new KryoException("No method handle, eg because of serialPersistentFields.");
			this.handle = handle.asType(MethodType.methodType(void.class, Object.class, ObjectInputStream.class));
			streamClass = ObjectStreamClass.lookup(type);
			ObjectStreamField[] streamFields = streamClass.getFields();
			names = new String[streamFields.length];
			fields = new Field[streamFields.length];
			valueIndexes = new int[streamFields.length];
			for (int i = 0; i < streamFields.length; i++) {
				names[i] = streamFields[i].getName();
				fields[i] = type.getDeclaredField(names[i]);
				fields[i].setAccessible(true);
				valueIndexes[i] = -1;
			}
		}

		/** @throws KryoException If the field is not serializable, eg because it is transient. */
		void setValueIndex (String name, int valueIndex) {
			int i = indexOf(name);
			if (i == -1) throw new KryoException("Field is not serializable: " + name);
			valueIndexes[i] = valueIndex;
		}

		int indexOf (String name) {
			for (int i = 0; i < names.length; i++)
				if (names[i].equals(name)) return i;
			return -1;
		}
	}

	/** Provides the field values to the method handle, instead of reading them with Java serialization. */
	static final class FieldsInput extends ObjectInputStream {
		ClassFields classFields;
		Object object;
		Object[] values;
		int next; // The handle gets the fields in the order of the ObjectStreamClass.
		private final GetField getField = new GetField() {
			public ObjectStreamClass getObjectStreamClass () {
				return classFields.streamClass;
			}

			public boolean defaulted (String name) {
				return false;
			}

			private Object value (String name) {
				ClassFields classFields = FieldsInput.this.classFields;
				int i = next;
				if (i < classFields.names.length && classFields.names[i].equals(name))
					next++;
				else {
					i = classFields.indexOf(name);
					if (i == -1) throw new IllegalArgumentException(name);
				}
				int valueIndex = classFields.valueIndexes[i];
				if (valueIndex != -1 && values[valueIndex] != unset) return values[valueIndex];
				try {
					return classFields.fields[i].get(object);
				} catch (IllegalAccessException ex) {
					throw new KryoException(ex);
				}
			}

			public boolean get (String name, boolean val) {
				Object value = value(name);
				return value == null ? val : (Boolean)value;
			}

			public byte get (String name, byte val) {
				Object value = value(name);
				return value == null ? val : (Byte)value;
			}

			public char get (String name, char val) {
				Object value = value(name);
				return value == null ? val : (Character)value;
			}

			public short get (String name, short val) {
				Object value = value(name);
				return value == null ? val : (Short)value;
			}

			public int get (String name, int val) {
				Object value = value(name);
				return value == null ? val : (Integer)value;
			}

			public long get (String name, long val) {
				Object value = value(name);
				return value == null ? val : (Long)value;
			}

			public float get (String name, float val) {
				Object value = value(name);
				return value == null ? val : (Float)value;
			}

			public double get (String name, double val) {
				Object value = value(name);
				return value == null ? val : (Double)value;
			}

			public Object get (String name, Object val) {
				return value(name);
			}
		};

		FieldsInput () throws IOException {
		}

		public GetField readFields () {
			return getField;
		}
	}
}
