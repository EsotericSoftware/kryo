
package com.esotericsoftware.kryo.serializers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.kryo.util.IntArray;

/* Helper interface for using Unsafe-based operations inside FieldSerializer. 
 * @author Roman Levenstein <romixlev@gmail.com> */
interface FieldSerializerUnsafeUtil {

	/** Use Unsafe-based information about fields layout in memory to build a list of cached fields and memory regions representing
	 * consecutive fields in memory */
	public abstract void createUnsafeCacheFieldsAndRegions (List<Field> validFields, List<CachedField> cachedFields,
		int baseIndex, IntArray useAsm);

	public abstract long getObjectFieldOffset (Field field);

	static class Factory {
		static Constructor<FieldSerializerUnsafeUtil> fieldSerializerUnsafeUtilConstructor;

		static {
			try {
				fieldSerializerUnsafeUtilConstructor = (Constructor<FieldSerializerUnsafeUtil>)FieldSerializer.class.getClassLoader()
					.loadClass("com.esotericsoftware.kryo.serializers.FieldSerializerUnsafeUtilImpl")
					.getConstructor(FieldSerializer.class);
			} catch (Throwable e) {

			}
		}

		static FieldSerializerUnsafeUtil getInstance (FieldSerializer serializer) {
			if (fieldSerializerUnsafeUtilConstructor != null) {
				try {
					return fieldSerializerUnsafeUtilConstructor.newInstance(serializer);
				} catch (Exception e) {
				}
			}
			return null;
		}
	}
}
