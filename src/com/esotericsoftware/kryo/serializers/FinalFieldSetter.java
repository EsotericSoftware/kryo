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

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Sets a final field of a serializable class if setting final fields with reflection is denied (JEP 500, eg with
 * {@code --illegal-final-field-mutation=deny}). Otherwise final fields are set with reflection like other fields, and this class
 * is not used. With Unsafe field access, final fields are set with Unsafe, which is not affected, so this class is not used
 * either.
 * <p>
 * Java serialization is allowed to set final fields. Since Java 24, {@code ReflectionFactory.defaultReadObjectForSerialization}
 * provides a method handle that does what {@link ObjectInputStream#defaultReadObject()} does for one class: it sets all
 * serializable fields declared by that class, including final fields, to the values it gets from
 * {@link ObjectInputStream#readFields()}. This class is the {@link ObjectInputStream} for that method handle. It doesn't read
 * Java serialization data, but returns the new value for the field to set, and the current value of the object for all other
 * serializable fields of the class, so those keep their values.
 * <p>
 * FieldSerializer and its subclasses call {@link #set(Object, Object)} of {@link CachedField#finalSetter} right after reading or
 * copying the value of a final field. So the fields are set in the same order as with reflection, which matters eg if the object
 * is added to a HashSet while its other fields are read.
 * <p>
 * {@link #create(Field)} is called when the field is first set, see {@link CachedField#finalUnresolved}, because it obtains a
 * method handle for setting the field to find out whether that is denied, which Java 26+ warns about like setting the field with
 * reflection. So the warning is only shown when Kryo sets a final field, not when a serializer is created, eg to write objects.
 * <p>
 * Only final fields of serializable classes that are not transient can be set this way. Other final fields, eg in a superclass
 * that isn't serializable or a transient final field that is copied, are set with reflection, which fails as before.
 * <p>
 * Not used on Android. */
final class FinalFieldSetter extends ObjectInputStream {
	static private final Object reflectionFactory;
	static private final Method defaultReadObject;
	/** Tests set this to use this class also if final fields can be set with reflection. */
	static boolean force;

	static {
		Object factory = null;
		Method method = null;
		try { // Java 24+.
			Class factoryClass = Class.forName("sun.reflect.ReflectionFactory");
			factory = factoryClass.getMethod("getReflectionFactory").invoke(null);
			method = factoryClass.getMethod("defaultReadObjectForSerialization", Class.class);
		} catch (Throwable ex) {
			if (TRACE) trace("kryo", "Final fields are set with reflection.", ex);
		}
		reflectionFactory = factory;
		defaultReadObject = method;
	}

	/** The serializable fields of a class do not change, so they are shared by all serializers. */
	static private final ClassValue<ClassFields> classFieldsCache = new ClassValue<>() {
		protected ClassFields computeValue (Class type) {
			return new ClassFields(type);
		}
	};

	private final ClassFields classFields;
	/** The position of the field to set in the serializable fields of its class. */
	private final int position;
	private Object object, value;
	private int next; // The method handle gets the fields in the order of the ObjectStreamClass.

	private FinalFieldSetter (ClassFields classFields, int position) throws IOException {
		this.classFields = classFields;
		this.position = position;
	}

	/** Returns null if the final field is set with reflection: that is allowed, the method handles are not available (before Java
	 * 24), or the field is not a serializable field of a serializable class, eg it is transient. Setting it with reflection fails
	 * in the last case. Called when the field is first set, see {@link CachedField#finalUnresolved}. */
	static FinalFieldSetter create (Field field) {
		if (defaultReadObject == null) return null;
		if (!force) {
			try {
				// Java has no API to ask whether final fields can be set with reflection. Getting a method handle for setting the
				// field fails if it is denied, without setting anything. If it is allowed with a warning, the warning is shown here.
				MethodHandles.lookup().unreflectSetter(field);
				return null;
			} catch (IllegalAccessException denied) {
			}
		}
		ClassFields classFields = classFieldsCache.get(field.getDeclaringClass());
		int position = classFields.indexOf(field.getName());
		if (position == -1) {
			if (DEBUG) debug("kryo", "Final field is set with reflection, it is not a serializable field: " + field);
			return null;
		}
		try {
			return new FinalFieldSetter(classFields, position);
		} catch (IOException ex) {
			throw new KryoException(ex);
		}
	}

	/** Sets the final field. */
	void set (Object object, Object value) {
		this.object = object;
		this.value = value;
		next = 0;
		try {
			classFields.handle.invokeExact(object, (ObjectInputStream)this);
		} catch (Throwable ex) {
			throw new KryoException("Error setting final field: " + classFields.fields[position], ex);
		} finally {
			this.object = null;
			this.value = null;
		}
	}

	public GetField readFields () {
		return getField;
	}

	/** Provides the field values to the method handle, instead of reading them with Java serialization. */
	private final GetField getField = new GetField() {
		public ObjectStreamClass getObjectStreamClass () {
			return classFields.streamClass;
		}

		public boolean defaulted (String name) {
			return false;
		}

		/** Returns the new value for the field to set, else the current value of the field. Never null for a primitive field. */
		private Object value (String name) {
			ClassFields classFields = FinalFieldSetter.this.classFields;
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

	/** The serializable fields of a class, which the method handle sets. There are none if the method handle is not available, eg
	 * because the class is not serializable. */
	static private final class ClassFields {
		MethodHandle handle;
		ObjectStreamClass streamClass;
		String[] names = {};
		Field[] fields = {};

		ClassFields (Class type) {
			if (!Serializable.class.isAssignableFrom(type)) return;
			try {
				// Null eg if the class has serialPersistentFields.
				MethodHandle handle = (MethodHandle)defaultReadObject.invoke(reflectionFactory, type);
				if (handle == null) return;
				streamClass = ObjectStreamClass.lookup(type);
				ObjectStreamField[] streamFields = streamClass.getFields();
				String[] names = new String[streamFields.length];
				Field[] fields = new Field[streamFields.length];
				for (int i = 0; i < streamFields.length; i++) {
					names[i] = streamFields[i].getName();
					fields[i] = type.getDeclaredField(names[i]);
					fields[i].setAccessible(true);
				}
				this.handle = handle.asType(MethodType.methodType(void.class, Object.class, ObjectInputStream.class));
				this.names = names;
				this.fields = fields;
			} catch (Throwable ex) {
				if (DEBUG) debug("kryo", "Final fields are set with reflection: " + className(type), ex);
			}
		}

		int indexOf (String name) {
			for (int i = 0; i < names.length; i++)
				if (names[i].equals(name)) return i;
			return -1;
		}
	}
}
