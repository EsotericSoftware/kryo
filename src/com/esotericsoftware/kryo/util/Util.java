
package com.esotericsoftware.kryo.util;

import static com.esotericsoftware.minlog.Log.*;

/** A few utility methods, mostly for private use.
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

	/** Returns the primitive wrapper class for a primitive class.
	 * @param type Must be a primitive class. */
	static public Class getWrapperClass (Class type) {
		if (type == int.class)
			return Integer.class;
		else if (type == float.class)
			return Float.class;
		else if (type == boolean.class)
			return Boolean.class;
		else if (type == long.class)
			return Long.class;
		else if (type == byte.class)
			return Byte.class;
		else if (type == char.class)
			return Character.class;
		else if (type == short.class) //
			return Short.class;
		else if (type == double.class)
			return Double.class;
		return Void.class;
	}

	/** Returns the primitive class for a primitive wrapper class. Otherwise returns the type parameter.
	 * @param type Must be a wrapper class. */
	static public Class getPrimitiveClass (Class type) {
		if (type == Integer.class)
			return int.class;
		else if (type == Float.class)
			return float.class;
		else if (type == Boolean.class)
			return boolean.class;
		else if (type == Long.class)
			return long.class;
		else if (type == Byte.class)
			return byte.class;
		else if (type == Character.class)
			return char.class;
		else if (type == Short.class) //
			return short.class;
		else if (type == Double.class) //
			return double.class;
		else if (type == Void.class)
			return void.class;
		return type;
	}
	
	static public boolean isWrapperClass (Class type) {
		return type == Integer.class || type == Float.class || type == Boolean.class || type == Long.class || type == Byte.class
			|| type == Character.class || type == Short.class || type == Double.class;
	}

	/** Logs a message about an object. The log level and the string format of the object depend on the object type. */
	static public void log (String message, Object object) {
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

	/** Returns the object formatted as a string. The format depends on the object's type and whether {@link Object#toString()} has
	 * been overridden. */
	static public String string (Object object) {
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

	/** Returns the class formatted as a string. The format varies depending on the type. */
	static public String className (Class type) {
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
	
	/** Converts an "int" value between endian systems. */
	static public int swapInt(int i) {
		return   ((i & 0xFF) << 24) | 
			    ((i & 0xFF00) << 8) | 
			   ((i & 0xFF0000) >> 8)| 
			   ((i >> 24) & 0xFF);
	}

	/** Converts a "long" value between endian systems. */
	static public long swapLong(long value) {
        return
            ( ( ( value >> 0 ) & 0xff ) << 56 )|
            ( ( ( value >> 8 ) & 0xff ) << 48 )|
            ( ( ( value >> 16 ) & 0xff ) << 40 )|
            ( ( ( value >> 24 ) & 0xff ) << 32 )|
            ( ( ( value >> 32 ) & 0xff ) << 24 )|
            ( ( ( value >> 40 ) & 0xff ) << 16 )|
            ( ( ( value >> 48 ) & 0xff ) << 8 )|
            ( ( ( value >> 56 ) & 0xff ) << 0 );
    }
}
