
package com.esotericsoftware.kryo;

import static com.esotericsoftware.minlog.Log.*;

/** A few utility methods, mostly private.
 * @author Nathan Sweet <misc@n4te.com> */
public class Util {
	static public boolean isAndroid;
	static {
		try {
			Class.forName("android.os.Process");
			isAndroid = true;
		} catch (Exception ignored) {
		}
	}

	static Class getWrapperClass (Class type) {
		if (type == boolean.class)
			return Boolean.class;
		else if (type == byte.class)
			return Byte.class;
		else if (type == char.class)
			return Character.class;
		else if (type == short.class)
			return Short.class;
		else if (type == int.class)
			return Integer.class;
		else if (type == long.class)
			return Long.class;
		else if (type == float.class) //
			return Float.class;
		return Double.class;
	}

	static void log (String message, Object object) {
		if (object == null) {
			if (TRACE) trace("kryo", message + ": null");
			return;
		}
		Class type = object.getClass();
		if (type.isPrimitive() || type == Boolean.class || type == Byte.class || type == Character.class || type == Short.class
			|| type == Integer.class || type == Long.class || type == Float.class || type == Double.class || type == String.class) {
			if (TRACE) trace("kryo", message + ": " + string(object));
		} else {
			debug("kryo", message + ": " + string(object));
		}
	}

	static String string (Object object) {
		if (object == null) return "null";
		Class type = object.getClass();
		if (type.isArray()) return className(type);
		try {
			if (type.getMethod("toString", new Class[0]).getDeclaringClass() == Object.class)
				return TRACE ? className(type) : type.getSimpleName();
		} catch (Exception ignored) {
		}
		return String.valueOf(object);
	}

	static String className (Class type) {
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

	static public int getDimensionCount (Class arrayClass) {
		int depth = 0;
		Class nextClass = arrayClass.getComponentType();
		while (nextClass != null) {
			depth++;
			nextClass = nextClass.getComponentType();
		}
		return depth;
	}

	static public Class getElementClass (Class arrayClass) {
		Class elementClass = arrayClass;
		while (elementClass.getComponentType() != null)
			elementClass = elementClass.getComponentType();
		return elementClass;
	}
}
