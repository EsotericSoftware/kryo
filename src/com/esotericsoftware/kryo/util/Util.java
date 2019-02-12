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

package com.esotericsoftware.kryo.util;

import static com.esotericsoftware.minlog.Log.*;

import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.util.Generics.GenericType;

import java.lang.reflect.Type;

/** A few utility methods, mostly for private use.
 * @author Nathan Sweet */
public class Util {
	static public final boolean isAndroid = "Dalvik".equals(System.getProperty("java.vm.name"));

	/** True if Unsafe is available. Unsafe can be disabled by setting the system property "kryo.unsafe" to "false". */
	static public final boolean unsafe;
	static {
		boolean found = false;
		if ("false".equals(System.getProperty("kryo.unsafe"))) {
			if (TRACE) trace("kryo", "Unsafe is disabled.");
		} else {
			try {
				found = Class.forName("com.esotericsoftware.kryo.unsafe.UnsafeUtil", true, FieldSerializer.class.getClassLoader())
					.getField("unsafe").get(null) != null;
			} catch (Throwable ex) {
				if (TRACE) trace("kryo", "Unsafe is unavailable.", ex);
			}
		}
		unsafe = found;
	}

	// Maximum reasonable array length. See: https://stackoverflow.com/questions/3038392/do-java-arrays-have-a-maximum-size
	static public final int maxArraySize = Integer.MAX_VALUE - 8;

	static public boolean isClassAvailable (String className) {
		try {
			Class.forName(className);
			return true;
		} catch (Exception ex) {
			debug("kryo", "Class not available: " + className);
			return false;
		}
	}

	/** Returns the primitive wrapper class for a primitive class.
	 * @param type Must be a primitive class. */
	static public Class getWrapperClass (Class type) {
		if (type == int.class) return Integer.class;
		if (type == float.class) return Float.class;
		if (type == boolean.class) return Boolean.class;
		if (type == byte.class) return Byte.class;
		if (type == long.class) return Long.class;
		if (type == char.class) return Character.class;
		if (type == double.class) return Double.class;
		if (type == short.class) return Short.class;
		return Void.class;
	}

	/** Returns the primitive class for a primitive wrapper class. Otherwise returns the type parameter.
	 * @param type Must be a wrapper class. */
	static public Class getPrimitiveClass (Class type) {
		if (type == Integer.class) return int.class;
		if (type == Float.class) return float.class;
		if (type == Boolean.class) return boolean.class;
		if (type == Byte.class) return byte.class;
		if (type == Long.class) return long.class;
		if (type == Character.class) return char.class;
		if (type == Double.class) return double.class;
		if (type == Short.class) return short.class;
		if (type == Void.class) return void.class;
		return type;
	}

	static public boolean isWrapperClass (Class type) {
		return type == Integer.class || type == Float.class || type == Boolean.class || type == Byte.class || type == Long.class
			|| type == Character.class || type == Double.class || type == Short.class;
	}

	static public boolean isEnum (Class type) {
		// Use this rather than type.isEnum() to return true for an enum value that is an inner class, eg: enum A {b{}}
		return Enum.class.isAssignableFrom(type) && type != Enum.class;
	}

	/** Logs a message about an object. The log level and the string format of the object depend on the object type. */
	static public void log (String message, Object object, int position) {
		if (object == null) {
			if (TRACE) trace("kryo", message + ": null" + pos(position));
			return;
		}
		Class type = object.getClass();
		if (type.isPrimitive() || isWrapperClass(type) || type == String.class) {
			if (TRACE) trace("kryo", message + ": " + string(object) + pos(position));
		} else
			debug("kryo", message + ": " + string(object) + pos(position));
	}

	static public String pos (int position) {
		return position == -1 ? "" : " [" + position + "]";
	}

	/** Returns the object formatted as a string. The format depends on the object's type and whether {@link Object#toString()} has
	 * been overridden. */
	static public String string (Object object) {
		if (object == null) return "null";
		Class type = object.getClass();
		if (type.isArray()) return className(type);
		String className = TRACE ? className(type) : type.getSimpleName();
		try {
			if (type.getMethod("toString", new Class[0]).getDeclaringClass() == Object.class) return className;
		} catch (Exception ignored) {
		}
		try {
			String value = String.valueOf(object) + " (" + className + ")";
			return value.length() > 97 ? value.substring(0, 97) + "..." : value;
		} catch (Throwable ex) {
			return className + " (toString exception: " + ex + ")";
		}
	}

	/** Returns the class formatted as a string. The format varies depending on the type. */
	static public String className (Class type) {
		if (type == null) return "null";
		if (type.isArray()) {
			Class elementClass = getElementClass(type);
			StringBuilder buffer = new StringBuilder(16);
			for (int i = 0, n = getDimensionCount(type); i < n; i++)
				buffer.append("[]");
			return className(elementClass) + buffer;
		}
		if (type.isPrimitive() || type == Object.class || type == Boolean.class || type == Byte.class || type == Character.class
			|| type == Short.class || type == Integer.class || type == Long.class || type == Float.class || type == Double.class
			|| type == String.class) {
			return type.getSimpleName();
		}
		return type.getName();
	}

	/** Returns the classes formatted as a string. The format varies depending on the type. */
	static public String classNames (Class[] types) {
		StringBuilder buffer = new StringBuilder(32);
		for (int i = 0, n = types.length; i < n; i++) {
			if (i > 0) buffer.append(", ");
			buffer.append(className(types[i]));
		}
		return buffer.toString();
	}

	static public String simpleName (Type type) {
		if (type instanceof Class) return ((Class)type).getSimpleName();
		return type.toString(); // Java 8: getTypeName
	}

	static public String simpleName (Class type, GenericType genericType) {
		StringBuilder buffer = new StringBuilder(32);
		buffer.append((type.isArray() ? getElementClass(type) : type).getSimpleName());
		if (genericType.arguments != null) {
			buffer.append('<');
			for (int i = 0, n = genericType.arguments.length; i < n; i++) {
				if (i > 0) buffer.append(", ");
				buffer.append(genericType.arguments[i].toString());
			}
			buffer.append('>');
		}
		if (type.isArray()) {
			for (int i = 0, n = getDimensionCount(type); i < n; i++)
				buffer.append("[]");
		}
		return buffer.toString();
	}

	/** Returns the number of dimensions of an array. */
	static public int getDimensionCount (Class arrayClass) {
		int depth = 0;
		Class nextClass = arrayClass.getComponentType();
		while (nextClass != null) {
			depth++;
			nextClass = nextClass.getComponentType();
		}
		return depth;
	}

	/** Returns the base element type of an n-dimensional array class. */
	static public Class getElementClass (Class arrayClass) {
		Class elementClass = arrayClass;
		while (elementClass.getComponentType() != null)
			elementClass = elementClass.getComponentType();
		return elementClass;
	}

	static public boolean isAscii (String value) {
		for (int i = 0, n = value.length(); i < n; i++)
			if (value.charAt(i) > 127) return false;
		return true;
	}

	/** @param factoryClass Must have a constructor that takes a serializer class, or a zero argument constructor.
	 * @param serializerClass May be null if the factory already knows the serializer class to create. */
	static public <T extends SerializerFactory> T newFactory (Class<T> factoryClass, Class<? extends Serializer> serializerClass) {
		try {
			if (serializerClass != null) {
				try {
					return factoryClass.getConstructor(Class.class).newInstance(serializerClass);
				} catch (NoSuchMethodException ex) {
				}
			}
			return factoryClass.newInstance();
		} catch (Exception ex) {
			if (serializerClass == null)
				throw new IllegalArgumentException("Unable to create serializer factory: " + factoryClass.getName(), ex);
			else {
				throw new IllegalArgumentException("Unable to create serializer factory \"" + factoryClass.getName()
					+ "\" for serializer class: " + className(serializerClass), ex);
			}
		}
	}
}
