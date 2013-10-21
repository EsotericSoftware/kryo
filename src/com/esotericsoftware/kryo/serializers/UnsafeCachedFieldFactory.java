package com.esotericsoftware.kryo.serializers;

import java.lang.reflect.Field;

import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedFieldFactory;
import com.esotericsoftware.kryo.serializers.UnsafeCacheFields.*;

class UnsafeCachedFieldFactory implements CachedFieldFactory {
	public CachedField createCachedField (Class fieldClass, Field field, FieldSerializer ser) {
		CachedField cachedField;
		// Use Unsafe-based serializers
		if (fieldClass.isPrimitive()) {
			if (fieldClass == boolean.class)
				cachedField = new UnsafeBooleanField(field);
			else if (fieldClass == byte.class)
				cachedField = new UnsafeByteField(field);
			else if (fieldClass == char.class)
				cachedField = new UnsafeCharField(field);
			else if (fieldClass == short.class)
				cachedField = new UnsafeShortField(field);
			else if (fieldClass == int.class)
				cachedField = new UnsafeIntField(field);
			else if (fieldClass == long.class)
				cachedField = new UnsafeLongField(field);
			else if (fieldClass == float.class)
				cachedField = new UnsafeFloatField(field);
			else if (fieldClass == double.class)
				cachedField = new UnsafeDoubleField(field);
			else {
				cachedField = new UnsafeObjectField(ser);
			}
		} else if (fieldClass == String.class
			&& (!ser.kryo.getReferences() || !ser.kryo.getReferenceResolver().useReferences(String.class))) {
			cachedField = new UnsafeStringField(field);
		} else {
			cachedField = new UnsafeObjectField(ser);
		}
		return cachedField;
	}
}
