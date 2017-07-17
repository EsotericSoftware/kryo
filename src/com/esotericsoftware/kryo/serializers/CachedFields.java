
package com.esotericsoftware.kryo.serializers;

import static com.esotericsoftware.kryo.util.Util.*;
import static com.esotericsoftware.minlog.Log.*;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
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
import com.esotericsoftware.kryo.serializers.FieldSerializerGenericsUtil.Generics;
import com.esotericsoftware.kryo.serializers.ReflectField.BooleanReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.ByteReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.CharReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.DoubleReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.FloatReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.IntReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.LongReflectField;
import com.esotericsoftware.kryo.serializers.ReflectField.ShortReflectField;
import com.esotericsoftware.reflectasm.FieldAccess;

class CachedFields implements Comparator<FieldSerializer.CachedField> {
	static final CachedField[] emptyCachedFields = new CachedField[0];

	private final FieldSerializer serializer;
	private final Kryo kryo;
	private final Class type;
	private final FieldSerializerConfig config;

	CachedField[] fields = new CachedField[0];
	CachedField[] transientFields = new CachedField[0];
	protected final ArrayList<Field> removedFields = new ArrayList();
	Object access;

	private final FieldSerializerGenericsUtil genericsUtil;
	Class[] generics;
	Generics genericsScope;

	public CachedFields (FieldSerializer serializer, Class[] generics) {
		this.serializer = serializer;
		kryo = serializer.kryo;
		type = serializer.type;
		config = serializer.config;
		this.generics = generics;

		genericsUtil = new FieldSerializerGenericsUtil(serializer);
	}

	public void updateGenerics () {
		if (TRACE && generics != null) trace("kryo", "Generic type parameters: " + Arrays.toString(generics));

		if (config.optimizedGenerics) {
			// For generic classes, generate a mapping from type variable names to the concrete types.
			genericsScope = genericsUtil.buildGenericsScope(type, generics);
			if (genericsScope != null) kryo.getGenericsResolver().pushScope(type, genericsScope);
		}

		for (CachedField cachedField : fields)
			if (cachedField instanceof ReflectField) genericsUtil.updateGenericCachedField((ReflectField)cachedField);
		for (CachedField cachedField : transientFields)
			if (cachedField instanceof ReflectField) genericsUtil.updateGenericCachedField((ReflectField)cachedField);

		if (genericsScope != null) kryo.getGenericsResolver().popScope();
	}

	/** Called when the list of cached fields must be rebuilt. This is done any time settings are changed that affect which fields
	 * will be used. It is called from the constructor for FieldSerializer. Subclasses may need to call this from their
	 * constructor. */
	public void rebuild () {
		if (type.isInterface()) {
			fields = emptyCachedFields; // No fields to serialize.
			return;
		}

		ArrayList<CachedField> newFields = new ArrayList(), newTransientFields = new ArrayList();
		boolean asm = !isAndroid && Modifier.isPublic(type.getModifiers());
		Class nextClass = type;
		while (nextClass != Object.class) {
			for (Field field : nextClass.getDeclaredFields())
				addField(field, asm, newFields, newTransientFields);
			nextClass = nextClass.getSuperclass();
		}

		if (fields.length != newFields.size()) fields = new CachedField[newFields.size()];
		newFields.toArray(fields);
		Arrays.sort(this.fields, this);

		if (transientFields.length != newTransientFields.size()) transientFields = new CachedField[newTransientFields.size()];
		newTransientFields.toArray(transientFields);
		Arrays.sort(transientFields, this);

		serializer.initializeCachedFields();
	}

	private void addField (Field field, boolean asm, ArrayList<CachedField> fields, ArrayList<CachedField> transientFields) {
		int modifiers = field.getModifiers();
		if (Modifier.isStatic(modifiers)) return;
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
		if (optional != null && !kryo.getContext().containsKey(optional.value())) return;

		if (removedFields.contains(field)) return;

		Class fieldClass = field.getType();
		int accessIndex = -1;
		if (asm //
			&& !Modifier.isFinal(modifiers) //
			&& Modifier.isPublic(modifiers) //
			&& Modifier.isPublic(fieldClass.getModifiers())) {
			try {
				if (access == null) access = FieldAccess.get(type);
				accessIndex = ((FieldAccess)access).getIndex(field);
			} catch (RuntimeException ex) {
				if (DEBUG) debug("kryo", "Unable to use ReflectASM.", ex);
				asm = false;
			}
		}

		CachedField cachedField = createCachedField(fieldClass, accessIndex != -1);
		cachedField.field = field;
		cachedField.varInt = config.varInts;
		cachedField.access = (FieldAccess)access;
		cachedField.accessIndex = accessIndex;
		if (config.extendedFieldNames)
			cachedField.name = cachedField.field.getDeclaringClass().getSimpleName() + "." + field.getName();
		else
			cachedField.name = field.getName();

		if (cachedField instanceof ReflectField) {
			cachedField.canBeNull = config.fieldsCanBeNull && !field.isAnnotationPresent(NotNull.class);
			if (kryo.isFinal(fieldClass) || config.fixedFieldTypes) cachedField.valueClass = fieldClass;

			Class[] generics = genericsUtil.getGenericsWithoutScope(field);
			((ReflectField)cachedField).generics = generics;

			if (TRACE) {
				if (generics != null) {
					trace("kryo", "Cached " + fieldClass.getSimpleName() + "<" + simpleNames(generics) + "> field: " + field.getName()
						+ " (" + className(field.getDeclaringClass()) + ")");
				} else {
					trace("kryo", "Cached " + fieldClass.getSimpleName() + " field: " + field.getName() + " ("
						+ className(field.getDeclaringClass()) + ")");
				}
			}
		} else { // Must be a primitive or String.
			cachedField.canBeNull = fieldClass == String.class && config.fieldsCanBeNull;
			cachedField.valueClass = fieldClass;

			if (TRACE) trace("kryo", "Cached " + fieldClass.getSimpleName() + " field: " + field.getName() + " ("
				+ className(field.getDeclaringClass()) + ")");
		}

		applyAnnotations(cachedField);

		if (Modifier.isTransient(modifiers))
			transientFields.add(cachedField);
		else
			fields.add(cachedField);
	}

	private CachedField createCachedField (Class fieldClass, boolean asm) {
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
			if (fieldClass == String.class && (!kryo.getReferences() || !kryo.getReferenceResolver().useReferences(String.class)))
				return new StringAsmField();
			return new AsmField(kryo, type);
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
		return new ReflectField(kryo, type);
	}

	public int compare (CachedField o1, CachedField o2) {
		// Fields are sorted by name so the order of the data is known.
		return o1.name.compareTo(o2.name);
	}

	/** Removes a field so that it won't be serialized. */
	public void removeField (String fieldName) {
		for (int i = 0; i < fields.length; i++) {
			CachedField cachedField = fields[i];
			if (cachedField.name.equals(fieldName)) {
				CachedField[] newFields = new CachedField[fields.length - 1];
				System.arraycopy(fields, 0, newFields, 0, i);
				System.arraycopy(fields, i + 1, newFields, i, newFields.length - i);
				fields = newFields;
				removedFields.add(cachedField.field);
				return;
			}
		}

		for (int i = 0; i < transientFields.length; i++) {
			CachedField cachedField = transientFields[i];
			if (cachedField.name.equals(fieldName)) {
				CachedField[] newFields = new CachedField[transientFields.length - 1];
				System.arraycopy(transientFields, 0, newFields, 0, i);
				System.arraycopy(transientFields, i + 1, newFields, i, newFields.length - i);
				transientFields = newFields;
				removedFields.add(cachedField.field);
				return;
			}
		}
		throw new IllegalArgumentException("Field \"" + fieldName + "\" not found on class: " + type.getName());
	}

	/** Removes a field so that it won't be serialized. */
	public void removeField (CachedField removeField) {
		for (int i = 0; i < fields.length; i++) {
			CachedField cachedField = fields[i];
			if (cachedField == removeField) {
				CachedField[] newFields = new CachedField[fields.length - 1];
				System.arraycopy(fields, 0, newFields, 0, i);
				System.arraycopy(fields, i + 1, newFields, i, newFields.length - i);
				fields = newFields;
				removedFields.add(cachedField.field);
				return;
			}
		}

		for (int i = 0; i < transientFields.length; i++) {
			CachedField cachedField = transientFields[i];
			if (cachedField == removeField) {
				CachedField[] newFields = new CachedField[transientFields.length - 1];
				System.arraycopy(transientFields, 0, newFields, 0, i);
				System.arraycopy(transientFields, i + 1, newFields, i, newFields.length - i);
				transientFields = newFields;
				removedFields.add(cachedField.field);
				return;
			}
		}
		throw new IllegalArgumentException("Field \"" + removeField + "\" not found on class: " + type.getName());
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
			cachedField.setSerializer(ReflectionSerializerFactory.newSerializer(kryo, serializerClass, field.getClass()));
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
		return ReflectionSerializerFactory.newSerializer(kryo, serializerClass, field.getClass());
	}
}
