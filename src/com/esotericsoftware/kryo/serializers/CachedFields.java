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

package com.esotericsoftware.kryo.serializers;

import static com.esotericsoftware.kryo.util.Util.*;
import static com.esotericsoftware.minlog.Log.*;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.ReflectionSerializerFactory;
import com.esotericsoftware.kryo.serializers.AsmField.BooleanAsmField;
import com.esotericsoftware.kryo.serializers.AsmField.ByteAsmField;
import com.esotericsoftware.kryo.serializers.AsmField.CharAsmField;
import com.esotericsoftware.kryo.serializers.AsmField.DoubleAsmField;
import com.esotericsoftware.kryo.serializers.AsmField.FloatAsmField;
import com.esotericsoftware.kryo.serializers.AsmField.IntAsmField;
import com.esotericsoftware.kryo.serializers.AsmField.LongAsmField;
import com.esotericsoftware.kryo.serializers.AsmField.ShortAsmField;
import com.esotericsoftware.kryo.serializers.AsmField.StringAsmField;
import com.esotericsoftware.kryo.serializers.FieldSerializer.Bind;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.kryo.serializers.FieldSerializer.FieldSerializerConfig;
import com.esotericsoftware.kryo.serializers.FieldSerializer.NotNull;
import com.esotericsoftware.kryo.serializers.FieldSerializer.Optional;
import com.esotericsoftware.kryo.serializers.ReflectField.BooleanReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.ByteReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.CharReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.DoubleReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.FloatReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.IntReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.LongReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.ShortReflectField;
import com.esotericsoftware.kryo.serializers.UnsafeField.BooleanUnsafeField;
import com.esotericsoftware.kryo.serializers.UnsafeField.ByteUnsafeField;
import com.esotericsoftware.kryo.serializers.UnsafeField.CharUnsafeField;
import com.esotericsoftware.kryo.serializers.UnsafeField.DoubleUnsafeField;
import com.esotericsoftware.kryo.serializers.UnsafeField.FloatUnsafeField;
import com.esotericsoftware.kryo.serializers.UnsafeField.IntUnsafeField;
import com.esotericsoftware.kryo.serializers.UnsafeField.LongUnsafeField;
import com.esotericsoftware.kryo.serializers.UnsafeField.ShortUnsafeField;
import com.esotericsoftware.kryo.serializers.UnsafeField.StringUnsafeField;
import com.esotericsoftware.kryo.util.Generics.GenericType;
import com.esotericsoftware.reflectasm.FieldAccess;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

/** @author Nathan Sweet */
class CachedFields implements Comparator<CachedField> {
	static final CachedField[] emptyCachedFields = new CachedField[0];

	private final FieldSerializer serializer;
	CachedField[] fields = new CachedField[0];
	CachedField[] copyFields = new CachedField[0];
	private final ArrayList<Field> removedFields = new ArrayList();
	private Object access;

	public CachedFields (FieldSerializer serializer) {
		this.serializer = serializer;
	}

	public void rebuild () {
		if (serializer.type.isInterface()) { // No fields to serialize.
			fields = emptyCachedFields;
			copyFields = emptyCachedFields;
			serializer.initializeCachedFields();
			return;
		}

		ArrayList<CachedField> newFields = new ArrayList(), newCopyFields = new ArrayList();
		boolean asm = !unsafe && !isAndroid && Modifier.isPublic(serializer.type.getModifiers());
		Class nextClass = serializer.type;
		while (nextClass != Object.class) {
			for (Field field : nextClass.getDeclaredFields())
				addField(field, asm, newFields, newCopyFields);
			nextClass = nextClass.getSuperclass();
		}

		if (fields.length != newFields.size()) fields = new CachedField[newFields.size()];
		newFields.toArray(fields);
		Arrays.sort(fields, this);

		if (copyFields.length != newCopyFields.size()) copyFields = new CachedField[newCopyFields.size()];
		newCopyFields.toArray(copyFields);
		Arrays.sort(copyFields, this);

		serializer.initializeCachedFields();
	}

	private void addField (Field field, boolean asm, ArrayList<CachedField> fields, ArrayList<CachedField> copyFields) {
		int modifiers = field.getModifiers();
		if (Modifier.isStatic(modifiers)) return;
		FieldSerializerConfig config = serializer.config;
		if (field.isSynthetic() && config.ignoreSyntheticFields) return;

		if (!field.isAccessible()) {
			if (!config.setFieldsAsAccessible) return;
			try {
				field.setAccessible(true);
			} catch (AccessControlException ex) {
				if (DEBUG) debug("kryo", "Unable to set field as accessible: " + field);
				return;
			}
		}

		Optional optional = field.getAnnotation(Optional.class);
		if (optional != null && !serializer.kryo.getContext().containsKey(optional.value())) return;

		if (removedFields.contains(field)) return;

		boolean isTransient = Modifier.isTransient(modifiers);
		if (isTransient && !config.serializeTransient && !config.copyTransient) return;

		Class declaringClass = field.getDeclaringClass();
		GenericType genericType = new GenericType(declaringClass, serializer.type, field.getGenericType());
		Class fieldClass = genericType.getType() instanceof Class ? (Class)genericType.getType() : field.getType();
		int accessIndex = -1;
		if (asm //
			&& !Modifier.isFinal(modifiers) //
			&& Modifier.isPublic(modifiers) //
			&& Modifier.isPublic(fieldClass.getModifiers())) {
			try {
				if (access == null) access = FieldAccess.get(serializer.type);
				accessIndex = ((FieldAccess)access).getIndex(field);
			} catch (RuntimeException ex) {
				if (DEBUG) debug("kryo", "Unable to use ReflectASM.", ex);
			}
		}

		CachedField cachedField;
		if (unsafe)
			cachedField = newUnsafeField(field, fieldClass, genericType);
		else if (accessIndex != -1) {
			cachedField = newAsmField(field, fieldClass, genericType);
			cachedField.access = (FieldAccess)access;
			cachedField.accessIndex = accessIndex;
		} else
			cachedField = newReflectField(field, fieldClass, genericType);

		cachedField.varEncoding = config.varEncoding;
		if (config.extendedFieldNames)
			cachedField.name = declaringClass.getSimpleName() + "." + field.getName();
		else
			cachedField.name = field.getName();

		if (cachedField instanceof ReflectField) { // Object field.
			cachedField.canBeNull = config.fieldsCanBeNull && !field.isAnnotationPresent(NotNull.class);
			if (serializer.kryo.isFinal(fieldClass) || config.fixedFieldTypes) cachedField.valueClass = fieldClass;

			if (TRACE) {
				trace("kryo",
					"Cached " + fieldClass.getSimpleName() + " field: " + field.getName() + " (" + className(declaringClass) + ")");
			}
		} else { // Must be a primitive or String.
			cachedField.canBeNull = fieldClass == String.class && config.fieldsCanBeNull;
			cachedField.valueClass = fieldClass;

			if (TRACE) trace("kryo",
				"Cached " + fieldClass.getSimpleName() + " field: " + field.getName() + " (" + className(declaringClass) + ")");
		}

		applyAnnotations(cachedField);

		if (isTransient) {
			if (config.serializeTransient) fields.add(cachedField);
			if (config.copyTransient) copyFields.add(cachedField);
		} else {
			fields.add(cachedField);
			copyFields.add(cachedField);
		}
	}

	private CachedField newUnsafeField (Field field, Class fieldClass, GenericType genericType) {
		if (fieldClass.isPrimitive()) {
			if (fieldClass == int.class) return new IntUnsafeField(field);
			if (fieldClass == float.class) return new FloatUnsafeField(field);
			if (fieldClass == boolean.class) return new BooleanUnsafeField(field);
			if (fieldClass == long.class) return new LongUnsafeField(field);
			if (fieldClass == double.class) return new DoubleUnsafeField(field);
			if (fieldClass == short.class) return new ShortUnsafeField(field);
			if (fieldClass == char.class) return new CharUnsafeField(field);
			if (fieldClass == byte.class) return new ByteUnsafeField(field);
		}
		if (fieldClass == String.class
			&& (!serializer.kryo.getReferences() || !serializer.kryo.getReferenceResolver().useReferences(String.class)))
			return new StringUnsafeField(field);
		return new UnsafeField(field, serializer, genericType);
	}

	private CachedField newAsmField (Field field, Class fieldClass, GenericType genericType) {
		if (fieldClass.isPrimitive()) {
			if (fieldClass == int.class) return new IntAsmField(field);
			if (fieldClass == float.class) return new FloatAsmField(field);
			if (fieldClass == boolean.class) return new BooleanAsmField(field);
			if (fieldClass == long.class) return new LongAsmField(field);
			if (fieldClass == double.class) return new DoubleAsmField(field);
			if (fieldClass == short.class) return new ShortAsmField(field);
			if (fieldClass == char.class) return new CharAsmField(field);
			if (fieldClass == byte.class) return new ByteAsmField(field);
		}
		if (fieldClass == String.class
			&& (!serializer.kryo.getReferences() || !serializer.kryo.getReferenceResolver().useReferences(String.class)))
			return new StringAsmField(field);
		return new AsmField(field, serializer, genericType);
	}

	private CachedField newReflectField (Field field, Class fieldClass, GenericType genericType) {
		if (fieldClass.isPrimitive()) {
			if (fieldClass == int.class) return new IntReflectField(field);
			if (fieldClass == float.class) return new FloatReflectField(field);
			if (fieldClass == boolean.class) return new BooleanReflectField(field);
			if (fieldClass == long.class) return new LongReflectField(field);
			if (fieldClass == double.class) return new DoubleReflectField(field);
			if (fieldClass == short.class) return new ShortReflectField(field);
			if (fieldClass == char.class) return new CharReflectField(field);
			if (fieldClass == byte.class) return new ByteReflectField(field);
		}
		return new ReflectField(field, serializer, genericType);
	}

	public int compare (CachedField o1, CachedField o2) {
		// Fields are sorted by name so the order of the data is known.
		return o1.name.compareTo(o2.name);
	}

	/** Removes a field so that it won't be serialized. */
	public void removeField (String fieldName) {
		boolean found = false;
		for (int i = 0; i < fields.length; i++) {
			CachedField cachedField = fields[i];
			if (cachedField.name.equals(fieldName)) {
				CachedField[] newFields = new CachedField[fields.length - 1];
				System.arraycopy(fields, 0, newFields, 0, i);
				System.arraycopy(fields, i + 1, newFields, i, newFields.length - i);
				fields = newFields;
				removedFields.add(cachedField.field);
				found = true;
				break;
			}
		}
		for (int i = 0; i < copyFields.length; i++) {
			CachedField cachedField = copyFields[i];
			if (cachedField.name.equals(fieldName)) {
				CachedField[] newFields = new CachedField[copyFields.length - 1];
				System.arraycopy(copyFields, 0, newFields, 0, i);
				System.arraycopy(copyFields, i + 1, newFields, i, newFields.length - i);
				copyFields = newFields;
				removedFields.add(cachedField.field);
				found = true;
				break;
			}
		}
		if (!found)
			throw new IllegalArgumentException("Field \"" + fieldName + "\" not found on class: " + serializer.type.getName());
	}

	/** Removes a field so that it won't be serialized. */
	public void removeField (CachedField removeField) {
		boolean found = false;
		for (int i = 0; i < fields.length; i++) {
			CachedField cachedField = fields[i];
			if (cachedField == removeField) {
				CachedField[] newFields = new CachedField[fields.length - 1];
				System.arraycopy(fields, 0, newFields, 0, i);
				System.arraycopy(fields, i + 1, newFields, i, newFields.length - i);
				fields = newFields;
				removedFields.add(cachedField.field);
				found = true;
				break;
			}
		}
		for (int i = 0; i < copyFields.length; i++) {
			CachedField cachedField = copyFields[i];
			if (cachedField == removeField) {
				CachedField[] newFields = new CachedField[copyFields.length - 1];
				System.arraycopy(copyFields, 0, newFields, 0, i);
				System.arraycopy(copyFields, i + 1, newFields, i, newFields.length - i);
				copyFields = newFields;
				removedFields.add(cachedField.field);
				found = true;
				break;
			}
		}
		if (!found)
			throw new IllegalArgumentException("Field \"" + removeField + "\" not found on class: " + serializer.type.getName());
	}

	/** Sets serializers using annotations.
	 * @see FieldSerializer.Bind
	 * @see CollectionSerializer.BindCollection
	 * @see MapSerializer.BindMap */
	private void applyAnnotations (CachedField cachedField) {
		Field field = cachedField.field;

		// Set the CachedField settings for any field.
		if (field.isAnnotationPresent(FieldSerializer.Bind.class)) {
			if (cachedField.serializer != null) {
				throw new KryoException("@Bind applied to a field that already has a serializer: "
					+ cachedField.field.getDeclaringClass().getName() + "." + cachedField.field.getName());
			}
			Bind annotation = field.getAnnotation(FieldSerializer.Bind.class);

			Class valueClass = annotation.valueClass();
			if (valueClass == Object.class) valueClass = null;
			if (valueClass != null) cachedField.setValueClass(valueClass);

			Serializer serializer = newSerializer(valueClass, annotation.serializer(), annotation.serializerFactory());
			if (serializer != null) cachedField.setSerializer(serializer);

			cachedField.setCanBeNull(annotation.canBeNull());
			cachedField.setVariableLengthEncoding(annotation.variableLengthEncoding());
			cachedField.setOptimizePositive(annotation.optimizePositive());
		}

		// Set CollectionSerializer settings for a collection field.
		if (field.isAnnotationPresent(CollectionSerializer.BindCollection.class)) {
			if (cachedField.serializer != null) {
				throw new KryoException("@BindCollection applied to a field that already has a serializer: "
					+ cachedField.field.getDeclaringClass().getName() + "." + cachedField.field.getName());
			}
			if (!Collection.class.isAssignableFrom(field.getType())) throw new KryoException(
				"@BindCollection can only be used with a field implementing Collection: " + className(field.getType()));
			CollectionSerializer.BindCollection annotation = field.getAnnotation(CollectionSerializer.BindCollection.class);

			Class elementClass = annotation.elementClass();
			if (elementClass == Object.class) elementClass = null;
			Serializer elementSerializer = newSerializer(elementClass, annotation.elementSerializer(),
				annotation.elementSerializerFactory());

			CollectionSerializer serializer = new CollectionSerializer();
			serializer.setElementsCanBeNull(annotation.elementsCanBeNull());
			if (elementClass != null) serializer.setElementClass(elementClass);
			if (elementSerializer != null) serializer.setElementSerializer(elementSerializer);
			cachedField.setSerializer(serializer);
		}

		// Set MapSerializer settings for a map field.
		if (field.isAnnotationPresent(MapSerializer.BindMap.class)) {
			if (cachedField.serializer != null) {
				throw new KryoException("@BindMap applied to a field that already has a serializer: "
					+ cachedField.field.getDeclaringClass().getName() + "." + cachedField.field.getName());
			}
			if (!Map.class.isAssignableFrom(field.getType()))
				throw new KryoException("@BindMap can only be used with a field implementing Map: " + className(field.getType()));
			MapSerializer.BindMap annotation = field.getAnnotation(MapSerializer.BindMap.class);

			Class valueClass = annotation.valueClass();
			if (valueClass == Object.class) valueClass = null;
			Serializer valueSerializer = newSerializer(valueClass, annotation.valueSerializer(),
				annotation.valueSerializerFactory());

			Class keyClass = annotation.keyClass();
			if (keyClass == Object.class) keyClass = null;
			Serializer keySerializer = newSerializer(keyClass, annotation.keySerializer(), annotation.keySerializerFactory());

			MapSerializer serializer = new MapSerializer();
			serializer.setKeysCanBeNull(annotation.keysCanBeNull());
			serializer.setValuesCanBeNull(annotation.valuesCanBeNull());
			if (keyClass != null) serializer.setKeyClass(keyClass);
			if (keySerializer != null) serializer.setKeySerializer(keySerializer);
			if (valueClass != null) serializer.setValueClass(valueClass);
			if (valueSerializer != null) serializer.setValueSerializer(valueSerializer);
			cachedField.setSerializer(serializer);
		}
	}

	private Serializer newSerializer (Class valueClass, Class serializerClass, Class factoryClass) {
		if (serializerClass == Serializer.class) serializerClass = null;
		if (factoryClass == SerializerFactory.class) factoryClass = null;
		if (factoryClass == null && serializerClass != null) factoryClass = ReflectionSerializerFactory.class;
		if (factoryClass == null) return null;
		return newFactory(factoryClass, serializerClass).newSerializer(serializer.kryo, valueClass);
	}
}
