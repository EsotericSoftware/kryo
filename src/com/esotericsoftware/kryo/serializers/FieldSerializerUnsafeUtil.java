/* Copyright (c) 2008, Nathan Sweet
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
