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

import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.trace;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.esotericsoftware.kryo.Generics;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.factories.ReflectionSerializerFactory;
import com.esotericsoftware.kryo.serializers.FieldSerializer.Bind;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;

/**
 * A few utility methods for processing field annotations.
 * 
 * @author Roman Levenstein <romixlev@gmail.com>
 */
final class FieldSerializerAnnotationsUtil {
	public FieldSerializerAnnotationsUtil (FieldSerializer serializer) {
	}

	/**
	 * Process annotated fields and set serializers according to the 
	 * provided annotation.
	 * 
	 * @see FieldSerializer.Bind
	 * @see CollectionSerializer.BindCollection
	 * @see MapSerializer.BindMap
	 */
	public void processAnnotatedFields (FieldSerializer fieldSerializer) {
		CachedField[] fields = fieldSerializer.getFields();
		for (int i = 0, n = fields.length; i < n; i++) {
			Field field = fields[i].getField();

			// Set a specific serializer for a particular field
			if (field.isAnnotationPresent(FieldSerializer.Bind.class)) {
				Class<? extends Serializer> serializerClass = field.getAnnotation(FieldSerializer.Bind.class).value();
				Serializer s = ReflectionSerializerFactory.makeSerializer(fieldSerializer.getKryo(), serializerClass, field.getClass());
				fields[i].setSerializer(s);
			}

			if (field.isAnnotationPresent(CollectionSerializer.BindCollection.class)
				&& field.isAnnotationPresent(MapSerializer.BindMap.class)) {

			}
			// Set a specific collection serializer for a particular field
			if (field.isAnnotationPresent(CollectionSerializer.BindCollection.class)) {
				if (fields[i].serializer != null)
					throw new RuntimeException("CollectionSerialier.Bind cannot be used with field "
						+ fields[i].getField().getDeclaringClass().getName() + "." + fields[i].getField().getName()
						+ ", because it has a serializer already.");
				CollectionSerializer.BindCollection annotation = field
					.getAnnotation(CollectionSerializer.BindCollection.class);
				if (Collection.class.isAssignableFrom(fields[i].field.getType())) {
					Class<? extends Serializer> elementSerializerClass = annotation.elementSerializer();
					if (elementSerializerClass == Serializer.class) elementSerializerClass = null;
					Serializer elementSerializer = (elementSerializerClass == null) ? null : ReflectionSerializerFactory
						.makeSerializer(fieldSerializer.getKryo(), elementSerializerClass, field.getClass());
					boolean elementsCanBeNull = annotation.elementsCanBeNull();
					Class<?> elementClass = annotation.elementClass();
					if (elementClass == Object.class) elementClass = null;
					CollectionSerializer serializer = new CollectionSerializer();
					serializer.setElementsCanBeNull(elementsCanBeNull);
					serializer.setElementClass(elementClass, elementSerializer);
					fields[i].setSerializer(serializer);
				} else {
					throw new RuntimeException(
						"CollectionSerialier.Bind should be used only with fields implementing java.util.Collection, but field "
							+ fields[i].getField().getDeclaringClass().getName() + "." + fields[i].getField().getName()
							+ " does not implement it.");
				}
			}

			// Set a specific map serializer for a particular field
			if (field.isAnnotationPresent(MapSerializer.BindMap.class)) {
				if (fields[i].serializer != null)
					throw new RuntimeException("MapSerialier.Bind cannot be used with field "
						+ fields[i].getField().getDeclaringClass().getName() + "." + fields[i].getField().getName()
						+ ", because it has a serializer already.");
				MapSerializer.BindMap annotation = field.getAnnotation(MapSerializer.BindMap.class);
				if (Map.class.isAssignableFrom(fields[i].field.getType())) {
					Class<? extends Serializer> valueSerializerClass = annotation.valueSerializer();
					Class<? extends Serializer> keySerializerClass = annotation.keySerializer();

					if (valueSerializerClass == Serializer.class) valueSerializerClass = null;
					if (keySerializerClass == Serializer.class) keySerializerClass = null;

					Serializer valueSerializer = (valueSerializerClass == null) ? null : ReflectionSerializerFactory.makeSerializer(
						fieldSerializer.getKryo(), valueSerializerClass, field.getClass());
					Serializer keySerializer = (keySerializerClass == null) ? null : ReflectionSerializerFactory.makeSerializer(
						fieldSerializer.getKryo(), keySerializerClass, field.getClass());
					boolean valuesCanBeNull = annotation.valuesCanBeNull();
					boolean keysCanBeNull = annotation.keysCanBeNull();
					Class<?> keyClass = annotation.keyClass();
					Class<?> valueClass = annotation.valueClass();

					if (keyClass == Object.class) keyClass = null;
					if (valueClass == Object.class) valueClass = null;

					MapSerializer serializer = new MapSerializer();
					serializer.setKeysCanBeNull(keysCanBeNull);
					serializer.setValuesCanBeNull(valuesCanBeNull);
					serializer.setKeyClass(keyClass, keySerializer);
					serializer.setValueClass(valueClass, valueSerializer);
					fields[i].setSerializer(serializer);
				} else {
					throw new RuntimeException(
						"MapSerialier.Bind should be used only with fields implementing java.util.Map, but field "
							+ fields[i].getField().getDeclaringClass().getName() + "." + fields[i].getField().getName()
							+ " does not implement it.");
				}
			}

		}
	}

}
