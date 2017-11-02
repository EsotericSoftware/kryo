/* Copyright (c) 2008-2017, Nathan Sweet
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.Serializer;
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
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.kryo.serializers.FieldSerializer.Optional;
import com.esotericsoftware.kryo.serializers.ReflectField.BooleanReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.ByteReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.CharReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.DoubleReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.FloatReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.IntReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.LongReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.ShortReflectField;
import com.esotericsoftware.kryo.util.Generics.GenericType;
import com.esotericsoftware.reflectasm.FieldAccess;

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
		boolean asm = !isAndroid && Modifier.isPublic(serializer.type.getModifiers());
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

		CachedField cachedField = newCachedField(fieldClass, accessIndex != -1, genericType);
		cachedField.field = field;
		cachedField.varInt = config.varInts;
		cachedField.access = (FieldAccess)access;
		cachedField.accessIndex = accessIndex;
		if (config.extendedFieldNames)
			cachedField.name = declaringClass.getSimpleName() + "." + field.getName();
		else
			cachedField.name = field.getName();

		if (cachedField instanceof ReflectField) {
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

	private CachedField newCachedField (Class fieldClass, boolean asm, GenericType genericType) {
		if (asm) {
			if (fieldClass.isPrimitive()) {
				if (fieldClass == boolean.class) return new BooleanAsmField();
				if (fieldClass == byte.class) return new ByteAsmField();
				if (fieldClass == char.class) return new CharAsmField();
				if (fieldClass == short.class) return new ShortAsmField();
				if (fieldClass == int.class) return new IntAsmField();
				if (fieldClass == long.class) return new LongAsmField();
				if (fieldClass == float.class) return new FloatAsmField();
				if (fieldClass == double.class) return new DoubleAsmField();
			}
			if (fieldClass == String.class
				&& (!serializer.kryo.getReferences() || !serializer.kryo.getReferenceResolver().useReferences(String.class)))
				return new StringAsmField();
			return new AsmField(serializer, genericType);
		}
		if (fieldClass.isPrimitive()) {
			if (fieldClass == boolean.class) return new BooleanReflectField();
			if (fieldClass == byte.class) return new ByteReflectField();
			if (fieldClass == char.class) return new CharReflectField();
			if (fieldClass == short.class) return new ShortReflectField();
			if (fieldClass == int.class) return new IntReflectField();
			if (fieldClass == long.class) return new LongReflectField();
			if (fieldClass == float.class) return new FloatReflectField();
			if (fieldClass == double.class) return new DoubleReflectField();
		}
		return new ReflectField(serializer, genericType);
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

		// Set a specific serializer for a particular field.
		if (field.isAnnotationPresent(FieldSerializer.Bind.class)) {
			Class serializerClass = field.getAnnotation(FieldSerializer.Bind.class).value();
			cachedField.setSerializer(ReflectionSerializerFactory.newSerializer(serializer.kryo, serializerClass, field.getClass()));
		}

		// Set a specific collection serializer for a particular field
		if (field.isAnnotationPresent(CollectionSerializer.BindCollection.class)) {
			if (cachedField.serializer != null) {
				throw new RuntimeException("CollectionSerialier.Bind cannot be used with field " + field.getDeclaringClass().getName()
					+ "." + field.getName() + ", because it has a serializer already.");
			}
			if (Collection.class.isAssignableFrom(field.getType())) {
				CollectionSerializer.BindCollection annotation = field.getAnnotation(CollectionSerializer.BindCollection.class);
				Serializer elementSerializer = newSerializer(annotation.elementSerializer(), field);
				Class elementClass = annotation.elementClass();

				CollectionSerializer serializer = new CollectionSerializer();
				serializer.setElementsCanBeNull(annotation.elementsCanBeNull());
				serializer.setElementClass(elementClass == Object.class ? null : elementClass, elementSerializer);
				cachedField.setSerializer(serializer);
			} else {
				throw new RuntimeException(
					"CollectionSerialier.Bind should be used only with fields implementing java.util.Collection, but field "
						+ field.getDeclaringClass().getName() + "." + field.getName() + " does not implement it.");
			}
		}

		// Set a specific map serializer for a particular field
		if (field.isAnnotationPresent(MapSerializer.BindMap.class)) {
			if (cachedField.serializer != null) {
				throw new RuntimeException("MapSerialier.Bind cannot be used with field " + field.getDeclaringClass().getName() + "."
					+ field.getName() + ", it has a serializer already.");
			}
			if (Map.class.isAssignableFrom(field.getType())) {
				MapSerializer.BindMap annotation = field.getAnnotation(MapSerializer.BindMap.class);
				Serializer valueSerializer = newSerializer(annotation.valueSerializer(), field);
				Serializer keySerializer = newSerializer(annotation.keySerializer(), field);

				Class keyClass = annotation.keyClass();
				Class valueClass = annotation.valueClass();

				MapSerializer serializer = new MapSerializer();
				serializer.setKeysCanBeNull(annotation.keysCanBeNull());
				serializer.setValuesCanBeNull(annotation.valuesCanBeNull());
				serializer.setKeyClass(keyClass == Object.class ? null : keyClass, keySerializer);
				serializer.setValueClass(valueClass == Object.class ? null : valueClass, valueSerializer);
				cachedField.setSerializer(serializer);
			} else {
				throw new RuntimeException("MapSerialier.Bind should be used only with fields implementing java.util.Map, but field "
					+ field.getDeclaringClass().getName() + "." + field.getName() + " does not implement it.");
			}
		}
	}

	private Serializer newSerializer (Class serializerClass, Field field) {
		if (serializerClass == Serializer.class) return null;
		return ReflectionSerializerFactory.newSerializer(serializer.kryo, serializerClass, field.getClass());
	}
}
