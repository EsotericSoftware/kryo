/* Copyright (c) 2008-2026, Nathan Sweet
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
import static com.esotericsoftware.kryo.util.Log.*;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.ReflectionSerializerFactory;
import com.esotericsoftware.kryo.serializers.FieldSerializer.Bind;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.kryo.serializers.FieldSerializer.FieldAccessType;
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
import com.esotericsoftware.kryo.serializers.ReflectField.StringReflectField;
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** @author Nathan Sweet */
class CachedFields implements Comparator<CachedField> {
	static final CachedField[] emptyCachedFields = new CachedField[0];

	/** Caches shared by all serializers and Kryo instances. Not used on Android, which has ClassValue only since API level 34. */
	static private final class SharedCaches {
		/** The declared fields of a class. Class#getDeclaredFields() returns new Field objects for each call, which take as much
		 * memory as the cached fields. */
		static final ClassValue<Field[]> declaredFields = new ClassValue<>() {
			protected Field[] computeValue (Class type) {
				return type.getDeclaredFields();
			}
		};

		/** The generic types of the fields of a serialized class, including the fields of its super classes. */
		static final ClassValue<ConcurrentHashMap<Field, GenericType>> genericTypes = new ClassValue<>() {
			protected ConcurrentHashMap<Field, GenericType> computeValue (Class type) {
				return new ConcurrentHashMap<>();
			}
		};
	}

	private final FieldSerializer serializer;
	/** Hidden classes can't be defined on Android or in a native image, and can be disabled by setting the system property
	 * "kryo.hiddenFields" to "false". Checked before {@link HiddenFields} is used, which can't be loaded on Android. Can be set by
	 * tests. */
	static boolean hiddenFields = !isAndroid && !isNativeImage && !"false".equals(System.getProperty("kryo.hiddenFields"));

	/** True if {@link CodeGeneration} can be used: Java 24+, not on Android or in a native image. Checked before the class is
	 * used, which can't be loaded on older Java versions. */
	static final boolean codeGeneration = !isAndroid && !isNativeImage && Runtime.version().feature() >= 24;

	/** Returns why {@link #codeGeneration} is false, for logging. */
	static String codeGenerationUnavailable () {
		if (isAndroid) return "Code generation is not available on Android.";
		if (isNativeImage) return "Code generation is not available in a native image.";
		return "Code generation needs Java 24 or later, this is Java " + Runtime.version().feature() + ".";
	}

	/** The simple name of the field implementation, without the suffix of a hidden class, for logging. */
	static String implementationName (CachedField field) {
		String name = field.getClass().getSimpleName();
		int slash = name.indexOf('/');
		return slash == -1 ? name : name.substring(0, slash);
	}

	CachedField[] fields = new CachedField[0];
	CachedField[] copyFields = new CachedField[0];
	private final ArrayList<Field> removedFields = new ArrayList();
	/** True while {@link FieldSerializer#initializeCachedFields()} is called. */
	private boolean initializing;

	public CachedFields (FieldSerializer serializer) {
		this.serializer = serializer;
	}

	private FieldAccessType fieldAccess () {
		return fieldAccess(serializer.config.fieldAccess, unsafe, isAndroid);
	}

	/** Returns the configured field access, or if Unsafe is configured but not available, VarHandles, or reflection on Android,
	 * which has VarHandles only since API level 33. */
	static FieldAccessType fieldAccess (FieldAccessType configured, boolean unsafe, boolean android) {
		if (configured != FieldAccessType.UNSAFE || unsafe) return configured;
		return android ? FieldAccessType.REFLECTION : FieldAccessType.VARHANDLE;
	}

	public void rebuild () {
		if (serializer.type.isInterface()) { // No fields to serialize.
			fields = emptyCachedFields;
			copyFields = emptyCachedFields;
			initialize();
			return;
		}

		ArrayList<CachedField> newFields = new ArrayList(), newCopyFields = new ArrayList();
		RecordComponent[] recordComponents = isRecord(serializer.type) ? serializer.type.getRecordComponents() : null;
		Class nextClass = serializer.type;
		while (nextClass != Object.class) {
			for (Field field : isAndroid ? nextClass.getDeclaredFields() : SharedCaches.declaredFields.get(nextClass))
				addField(field, recordComponents, newFields, newCopyFields);
			nextClass = nextClass.getSuperclass();
		}

		if (fields.length != newFields.size()) fields = new CachedField[newFields.size()];
		newFields.toArray(fields);
		Arrays.sort(fields, this);

		if (copyFields.length != newCopyFields.size()) copyFields = new CachedField[newCopyFields.size()];
		newCopyFields.toArray(copyFields);
		Arrays.sort(copyFields, this);

		initialize();
	}

	private void initialize () {
		initializing = true;
		try {
			serializer.initializeCachedFields();
		} finally {
			initializing = false;
		}
		serializer.fieldsChanged();
	}

	/** Called after a field was removed. A field removed by {@link FieldSerializer#initializeCachedFields()} is not remembered,
	 * because it is removed again when the fields are rebuilt. */
	private void removed (CachedField cachedField) {
		if (!initializing) removedFields.add(cachedField.field);
	}

	/** @param recordComponents May be null if the type is not a record. */
	private void addField (Field field, RecordComponent[] recordComponents, ArrayList<CachedField> fields,
		ArrayList<CachedField> copyFields) {
		int modifiers = field.getModifiers();
		if (Modifier.isStatic(modifiers)) return;
		FieldSerializerConfig config = serializer.config;
		if (field.isSynthetic() && config.ignoreSyntheticFields) return;

		if (!config.setFieldsAsAccessible) {
			if (!isPublicApi(field)) return;
		} else {
			try {
				field.setAccessible(true);
			} catch (SecurityException ex) {
				if (DEBUG) debug("kryo", "Unable to set field as accessible: " + field);
				return;
			}
		}

		Optional[] optionals = field.getAnnotationsByType(Optional.class);
		if (optionals.length > 0 && Arrays.stream(optionals).noneMatch(
			optional -> serializer.kryo.getContext().containsKey(optional.value()))) {
			return;
		}

		if (removedFields.contains(field)) return;

		boolean isTransient = Modifier.isTransient(modifiers);
		if (isTransient && !config.serializeTransient && !config.copyTransient) return;

		Class type = serializer.type;
		Class declaringClass = field.getDeclaringClass();
		GenericType genericType = genericType(declaringClass, type, field);
		Class fieldClass = genericType.getType() instanceof Class ? (Class)genericType.getType() : field.getType();
		CachedField cachedField;
		FieldAccessType fieldAccess = fieldAccess();
		if (fieldAccess == FieldAccessType.UNSAFE && !isRecord(type))
			cachedField = newUnsafeField(field, fieldClass, genericType);
		else if (fieldAccess != FieldAccessType.REFLECTION && !Modifier.isFinal(modifiers)
		// Android has VarHandles only since API level 33, so they are only used there if configured explicitly.
			&& (!isAndroid || fieldAccess == FieldAccessType.VARHANDLE))
			cachedField = newVarHandleField(field, fieldClass, genericType);
		else {
			cachedField = newReflectField(field, fieldClass, genericType);
			// A final field is set with reflection, which may be denied. Records set them with their constructor.
			// FinalFieldSetter is not loaded on Android, which has ClassValue only since API level 34.
			if (Modifier.isFinal(modifiers) && recordComponents == null && !isAndroid)
				cachedField.finalSetter = FinalFieldSetter.create(field);
		}

		cachedField.varEncoding = config.varEncoding;
		if (config.extendedFieldNames)
			cachedField.name = declaringClass.getSimpleName() + "." + field.getName();
		else
			cachedField.name = field.getName();

		if (cachedField instanceof ReflectField) { // Object field.
			cachedField.canBeNull = config.fieldsCanBeNull && !field.isAnnotationPresent(NotNull.class);
			if (serializer.kryo.isFinal(fieldClass) || config.fixedFieldTypes) cachedField.valueClass = fieldClass;

		} else { // Must be a primitive or String.
			cachedField.canBeNull = fieldClass == String.class && config.fieldsCanBeNull;
			cachedField.valueClass = fieldClass;
		}
		if (TRACE) {
			trace("kryo", "Cached " + fieldClass.getSimpleName() + " field: " + field.getName() + " (" + className(declaringClass)
				+ ") with " + implementationName(cachedField));
		}

		if (recordComponents != null) {
			for (int i = 0; i < recordComponents.length; i++) {
				if (recordComponents[i].getName().equals(field.getName())) {
					cachedField.index = i;
					break;
				}
			}
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

	/** Returns the generic type of a field, which all serializers and Kryo instances share, like the Field objects. The generic
	 * type of a primitive field is only needed while the field is added. */
	static private GenericType genericType (Class declaringClass, Class type, Field field) {
		if (isAndroid || field.getType().isPrimitive()) return new GenericType(declaringClass, type, field.getGenericType());
		return SharedCaches.genericTypes.get(type).computeIfAbsent(field,
			key -> new GenericType(declaringClass, type, key.getGenericType()));
	}

	/** Returns true if the field can be read and written without {@link Field#setAccessible(boolean)}: a public, non-final field
	 * of a public class. */
	static private boolean isPublicApi (Field field) {
		int modifiers = field.getModifiers();
		if (!Modifier.isPublic(modifiers) || Modifier.isFinal(modifiers)) return false;
		for (Class type = field.getDeclaringClass(); type != null; type = type.getEnclosingClass())
			if (!Modifier.isPublic(type.getModifiers())) return false;
		return true;
	}

	/** Returns true if a String field is written directly as a string, which all field access types decide the same way, so they
	 * write the same data, eg for a final field that VarHandles can't set. Not with references for strings, and not with
	 * {@link Bind} or {@link NotNull}, which only the fields for objects apply. Kryo remembers the decision for strings, so it
	 * can't change afterward. */
	private boolean isStringField (Field field, Class fieldClass) {
		return fieldClass == String.class && !field.isAnnotationPresent(Bind.class) && !field.isAnnotationPresent(NotNull.class)
			&& !serializer.kryo.usesStringReferences();
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
		if (isStringField(field, fieldClass)) return new StringUnsafeField(field);
		return new UnsafeField(field, serializer, genericType);
	}

	private CachedField newVarHandleField (Field field, Class fieldClass, GenericType genericType) {
		boolean string = isStringField(field, fieldClass);
		// Generated code doesn't call the fields to write and read, so they don't need a hidden class each.
		if (hiddenFields && !serializer.codeGenerated()) {
			try {
				return HiddenFields.create(field, fieldClass, string, serializer, genericType);
			} catch (KryoException ex) {
				if (DEBUG) debug("kryo", "Unable to access field with a hidden class, using a VarHandle: " + field, ex);
			}
		}
		try {
			if (fieldClass.isPrimitive()) {
				if (fieldClass == int.class) return new VarHandleField.IntVarHandleField(field);
				if (fieldClass == float.class) return new VarHandleField.FloatVarHandleField(field);
				if (fieldClass == boolean.class) return new VarHandleField.BooleanVarHandleField(field);
				if (fieldClass == long.class) return new VarHandleField.LongVarHandleField(field);
				if (fieldClass == double.class) return new VarHandleField.DoubleVarHandleField(field);
				if (fieldClass == short.class) return new VarHandleField.ShortVarHandleField(field);
				if (fieldClass == char.class) return new VarHandleField.CharVarHandleField(field);
				if (fieldClass == byte.class) return new VarHandleField.ByteVarHandleField(field);
			}
			if (string) return new VarHandleField.StringVarHandleField(field);
			return new VarHandleField(field, serializer, genericType);
		} catch (KryoException ex) {
			// Eg a public field in a package that is exported but not open to Kryo, which can be accessed with reflection.
			if (DEBUG) debug("kryo", "Unable to access field with a VarHandle, using reflection: " + field, ex);
			return newReflectField(field, fieldClass, genericType);
		}
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
		if (isStringField(field, fieldClass)) return new StringReflectField(field);
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
				removed(cachedField);
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
				removed(cachedField);
				found = true;
				break;
			}
		}
		if (!found)
			throw new IllegalArgumentException("Field \"" + fieldName + "\" not found on class: " + serializer.type.getName());
		if (!initializing) serializer.fieldsChanged();
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
				removed(cachedField);
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
				removed(cachedField);
				found = true;
				break;
			}
		}
		if (!found)
			throw new IllegalArgumentException("Field \"" + removeField + "\" not found on class: " + serializer.type.getName());
		if (!initializing) serializer.fieldsChanged();
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

			Serializer serializer = newSerializer(field, valueClass, annotation.serializer(), annotation.serializerFactory(), false,
				"@Bind serializer and serializerFactory require valueClass");
			if (serializer != null) cachedField.setSerializer(serializer);

			cachedField.setCanBeNull(annotation.canBeNull() && !field.isAnnotationPresent(NotNull.class));
			cachedField.setVariableLengthEncoding(annotation.variableLengthEncoding());
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
			Serializer elementSerializer = newSerializer(field, elementClass, annotation.elementSerializer(),
				annotation.elementSerializerFactory(), true,
				"@BindCollection elementSerializer and elementSerializerFactory require elementClass to deserialize elements");

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
			Serializer valueSerializer = newSerializer(field, valueClass, annotation.valueSerializer(),
				annotation.valueSerializerFactory(), true,
				"@BindMap valueSerializer and valueSerializerFactory require valueClass to deserialize values");

			Class keyClass = annotation.keyClass();
			if (keyClass == Object.class) keyClass = null;
			Serializer keySerializer = newSerializer(field, keyClass, annotation.keySerializer(), annotation.keySerializerFactory(),
				true, "@BindMap keySerializer and keySerializerFactory require keyClass to deserialize keys");

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

	/** @param warnIfClassMissing If true, a warning is logged when a serializer or factory is set without the value class.
	 *           Otherwise the value class is only reported as missing if creating the serializer fails.
	 * @param missingClassMessage The message used when the value class is missing. */
	private Serializer newSerializer (Field field, Class valueClass, Class serializerClass, Class factoryClass,
		boolean warnIfClassMissing, String missingClassMessage) {
		if (serializerClass == Serializer.class) serializerClass = null;
		if (factoryClass == SerializerFactory.class) factoryClass = null;
		if (factoryClass == null && serializerClass == null) return null;
		String fieldName = field.getDeclaringClass().getName() + "." + field.getName();
		if (warnIfClassMissing && valueClass == null && WARN) warn("kryo", missingClassMessage + ": " + fieldName);
		if (factoryClass == null) factoryClass = ReflectionSerializerFactory.class;
		SerializerFactory factory = newFactory(factoryClass, serializerClass);
		try {
			return factory.newSerializer(serializer.kryo, valueClass);
		} catch (RuntimeException ex) {
			// Most serializers and factories need the class, give a hint if it was not set.
			if (valueClass != null) throw ex;
			throw new KryoException(missingClassMessage + ": " + fieldName, ex);
		}
	}
}
