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
import java.util.LinkedHashMap;

/** Sets the final fields of serializable classes if setting final fields with reflection is denied (JEP 500, eg with
 * {@code --illegal-final-field-mutation=deny}). Otherwise final fields are set with reflection like other fields, and this class
 * is not used. Whether it is denied is checked once, by setting a final field of {@link Probe}. With Unsafe field access, final
 * fields are set with Unsafe, which is not affected, so this class is not used either.
 * <p>
 * Java serialization is allowed to set final fields. Since Java 24, {@code ReflectionFactory.defaultReadObjectForSerialization}
 * provides a method handle that does what {@link ObjectInputStream#defaultReadObject()} does for one class: it sets all
 * serializable fields declared by that class, including final fields, to the values it gets from
 * {@link ObjectInputStream#readFields()}. This class calls that method handle with its own {@link ObjectInputStream}, which
 * doesn't read Java serialization data. {@link FieldsInput} returns the new value for the field to set, and the current value of
 * the object for all other serializable fields of the class, so those keep their values.
 * <p>
 * FieldSerializer and its subclasses call {@link #set(Object, int, Object)} right after reading or copying the value of a final
 * field, with {@link CachedField#index} of the field. So the fields are set in the same order as with reflection, which matters
 * eg if the object is added to a HashSet while its other fields are read.
 * <p>
 * Only final fields of serializable classes that are not transient can be set this way. Other final fields, eg in a superclass
 * that isn't serializable or a transient final field that is copied, are set with reflection, which fails as before. */
final class FinalFieldSetter {
	static private final Object reflectionFactory;
	static private final Method defaultReadObject;
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

	/** For each final field, by {@link CachedField#index}: its class and its position in the serializable fields of the class. */
	private final ClassFields[] classes;
	private final int[] positions;
	private final FieldsInput input = new FieldsInput();

	private FinalFieldSetter (ClassFields[] classes, int[] positions) throws IOException {
		this.classes = classes;
		this.positions = positions;
	}

	/** Called if the class had final fields when its fields were cached. Returns null if final fields are set with reflection:
	 * setting them with reflection is allowed, the method handles are not available, or no final field is a serializable field of
	 * a serializable class. Otherwise sets {@link CachedField#index} for each final field that can be set. The other final fields
	 * are set with reflection, which fails, eg a transient final field that is only copied. */
	static FinalFieldSetter create (CachedField[] fields, CachedField[] copyFields) {
		if (defaultReadObject == null || !mutationDenied()) return null;
		LinkedHashMap<Class, ClassFields> classesByType = new LinkedHashMap<>();
		// Both arrays have the same CachedField for a field that is read and copied.
		LinkedHashMap<CachedField, Integer> positions = new LinkedHashMap<>();
		for (CachedField[] array : new CachedField[][] {fields, copyFields}) {
			for (CachedField cachedField : array) {
				Field field = cachedField.field;
				if (!Modifier.isFinal(field.getModifiers()) || positions.containsKey(cachedField)) continue;
				Class declaringClass = field.getDeclaringClass();
				if (!classesByType.containsKey(declaringClass)) {
					ClassFields classFields = null;
					try {
						classFields = new ClassFields(declaringClass);
					} catch (Throwable ex) {
						if (DEBUG) debug("kryo", "Final fields are set with reflection: " + className(declaringClass), ex);
					}
					classesByType.put(declaringClass, classFields);
				}
				ClassFields classFields = classesByType.get(declaringClass);
				int position = classFields == null ? -1 : classFields.indexOf(field.getName());
				if (position != -1)
					positions.put(cachedField, position);
				else if (DEBUG && classFields != null)
					debug("kryo", "Final field is set with reflection, it is not serializable: " + field);
			}
		}
		if (positions.isEmpty()) return null; // Eg the final fields were removed.
		try {
			ClassFields[] classes = new ClassFields[positions.size()];
			int[] positionsArray = new int[classes.length];
			int index = 0;
			for (CachedField cachedField : positions.keySet()) {
				classes[index] = classesByType.get(cachedField.field.getDeclaringClass());
				positionsArray[index] = positions.get(cachedField);
				cachedField.index = index++;
			}
			return new FinalFieldSetter(classes, positionsArray);
		} catch (IOException ex) {
			throw new KryoException(ex);
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

	/** Sets a final field.
	 * @param index The {@link CachedField#index} of the field. */
	void set (Object object, int index, Object value) {
		ClassFields classFields = classes[index];
		FieldsInput input = this.input;
		input.classFields = classFields;
		input.object = object;
		input.position = positions[index];
		input.value = value;
		input.next = 0;
		try {
			classFields.handle.invokeExact(object, (ObjectInputStream)input);
		} catch (Throwable ex) {
			throw new KryoException("Error setting final field: " + classFields.fields[positions[index]], ex);
		} finally {
			input.object = null;
			input.value = null;
		}
	}

	/** The serializable fields of a class, which the method handle sets. */
	static final class ClassFields {
		final MethodHandle handle;
		final ObjectStreamClass streamClass;
		final String[] names;
		final Field[] fields;

		ClassFields (Class type) throws Exception {
			if (!Serializable.class.isAssignableFrom(type)) throw new KryoException("Class is not serializable.");
			MethodHandle handle = (MethodHandle)defaultReadObject.invoke(reflectionFactory, type);
			if (handle == null) throw new KryoException("No method handle, eg because of serialPersistentFields.");
			this.handle = handle.asType(MethodType.methodType(void.class, Object.class, ObjectInputStream.class));
			streamClass = ObjectStreamClass.lookup(type);
			ObjectStreamField[] streamFields = streamClass.getFields();
			names = new String[streamFields.length];
			fields = new Field[streamFields.length];
			for (int i = 0; i < streamFields.length; i++) {
				names[i] = streamFields[i].getName();
				fields[i] = type.getDeclaredField(names[i]);
				fields[i].setAccessible(true);
			}
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
		Object object, value;
		int position;
		int next; // The handle gets the fields in the order of the ObjectStreamClass.
		private final GetField getField = new GetField() {
			public ObjectStreamClass getObjectStreamClass () {
				return classFields.streamClass;
			}

			public boolean defaulted (String name) {
				return false;
			}

			/** Returns the new value for the field to set, else the current value of the field. Never null for a primitive field. */
			private Object value (String name) {
				ClassFields classFields = FieldsInput.this.classFields;
				int i = next;
				if (i < classFields.names.length && classFields.names[i].equals(name))
					next++;
				else {
					i = classFields.indexOf(name);
					if (i == -1) throw new IllegalArgumentException(name);
				}
				if (i == position) return value;
				try {
					return classFields.fields[i].get(object);
				} catch (IllegalAccessException ex) {
					throw new KryoException(ex);
				}
			}

			public boolean get (String name, boolean val) {
				return (Boolean)value(name);
			}

			public byte get (String name, byte val) {
				return (Byte)value(name);
			}

			public char get (String name, char val) {
				return (Character)value(name);
			}

			public short get (String name, short val) {
				return (Short)value(name);
			}

			public int get (String name, int val) {
				return (Integer)value(name);
			}

			public long get (String name, long val) {
				return (Long)value(name);
			}

			public float get (String name, float val) {
				return (Float)value(name);
			}

			public double get (String name, double val) {
				return (Double)value(name);
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
