package com.esotericsoftware.kryo.serializers;

import java.lang.reflect.Field;

import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedFieldFactory;
import com.esotericsoftware.kryo.serializers.ObjectField.ObjectBooleanField;
import com.esotericsoftware.kryo.serializers.ObjectField.ObjectByteField;
import com.esotericsoftware.kryo.serializers.ObjectField.ObjectCharField;
import com.esotericsoftware.kryo.serializers.ObjectField.ObjectDoubleField;
import com.esotericsoftware.kryo.serializers.ObjectField.ObjectFloatField;
import com.esotericsoftware.kryo.serializers.ObjectField.ObjectIntField;
import com.esotericsoftware.kryo.serializers.ObjectField.ObjectLongField;
import com.esotericsoftware.kryo.serializers.ObjectField.ObjectShortField;

class ObjectCachedFieldFactory implements CachedFieldFactory {
	public CachedField createCachedField (Class fieldClass, Field field, FieldSerializer ser) {
		CachedField cachedField;
		if (fieldClass.isPrimitive()) {
			if (fieldClass == boolean.class)
				cachedField = new ObjectBooleanField(ser);
			else if (fieldClass == byte.class)
				cachedField = new ObjectByteField(ser);
			else if (fieldClass == char.class)
				cachedField = new ObjectCharField(ser);
			else if (fieldClass == short.class)
				cachedField = new ObjectShortField(ser);
			else if (fieldClass == int.class)
				cachedField = new ObjectIntField(ser);
			else if (fieldClass == long.class)
				cachedField = new ObjectLongField(ser);
			else if (fieldClass == float.class)
				cachedField = new ObjectFloatField(ser);
			else if (fieldClass == double.class)
				cachedField = new ObjectDoubleField(ser);
			else {
				cachedField = new ObjectField(ser);
			}
		}	else		
			cachedField = new ObjectField(ser);
		return cachedField;
	}
}
