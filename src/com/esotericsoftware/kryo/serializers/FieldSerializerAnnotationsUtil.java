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
