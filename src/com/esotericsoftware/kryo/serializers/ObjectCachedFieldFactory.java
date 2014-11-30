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
