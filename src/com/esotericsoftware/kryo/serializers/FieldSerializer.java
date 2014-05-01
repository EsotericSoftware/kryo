
package com.esotericsoftware.kryo.serializers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.esotericsoftware.kryo.Generics;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.factories.ReflectionSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.IntArray;
import com.esotericsoftware.kryo.util.ObjectMap;
import com.esotericsoftware.kryo.util.Util;
import com.esotericsoftware.reflectasm.FieldAccess;

import static com.esotericsoftware.minlog.Log.*;

// BOZO - Make primitive serialization with ReflectASM configurable?

/** Serializes objects using direct field assignment. No header or schema data is stored, only the data for each field. This
 * reduces output size but means if any field is added or removed, previously serialized bytes are invalidated. If fields are
 * public, bytecode generation will be used instead of reflection.
 * @see Serializer
 * @see Kryo#register(Class, Serializer)
 * @author Nathan Sweet <misc@n4te.com>
 * @author Roman Levenstein <romixlev@gmail.com> */
public class FieldSerializer<T> extends Serializer<T> implements Comparator<FieldSerializer.CachedField> {
	final Kryo kryo;
	final Class type;
	/** type variables declared for this type */
	final private TypeVariable[] typeParameters;
	private CachedField[] fields = new CachedField[0];
	private CachedField[] transientFields = new CachedField[0];
	protected HashSet<CachedField> removedFields = new HashSet();
	Object access;
	private boolean fieldsCanBeNull = true, setFieldsAsAccessible = true;
	private boolean ignoreSyntheticFields = true;
	private boolean fixedFieldTypes;
	/** If set, ASM-backend is used. Otherwise Unsafe-based backend or reflection is used */
	private boolean useAsmEnabled;
	private FieldSerializerUnsafeUtil unsafeUtil;

	private FieldSerializerGenericsUtil genericsUtil;
	
	private FieldSerializerAnnotationsUtil annotationsUtil;

	/** Concrete classes passed as values for type variables */
	private Class[] generics;

	private Generics genericsScope;

	/** If set, this serializer tries to use a variable length encoding for int and long fields */
	private boolean varIntsEnabled;

	/** If set, adjacent primitive fields are written in bulk This flag may only work with Oracle JVMs, because they layout
	 * primitive fields in memory in such a way that primitive fields are grouped together. This option has effect only when used
	 * with Unsafe-based FieldSerializer.
	 * <p>
	 * FIXME: Not all versions of Sun/Oracle JDK properly work with this option. Disable it for now. Later add dynamic checks to
	 * see if this feature is supported by a current JDK version.
	 * </p> */
	private boolean useMemRegions = false;

	/** If set, transient fields will be copied */
	private boolean copyTransient = true;

	/** If set, transient fields will be serialized */
	private final boolean serializeTransient = false;

	private boolean hasObjectFields = false;

	static CachedFieldFactory asmFieldFactory;
	static CachedFieldFactory objectFieldFactory;
	static CachedFieldFactory unsafeFieldFactory;

	static boolean unsafeAvailable;
	static Class<?> unsafeUtilClass;
	static Method sortFieldsByOffsetMethod;

	static {
		try {
			unsafeUtilClass = FieldSerializer.class.getClassLoader().loadClass("com.esotericsoftware.kryo.util.UnsafeUtil");
			Method unsafeMethod = unsafeUtilClass.getMethod("unsafe");
			sortFieldsByOffsetMethod = unsafeUtilClass.getMethod("sortFieldsByOffset", List.class);
			Object unsafe = unsafeMethod.invoke(null);
			if (unsafe != null) unsafeAvailable = true;
		} catch (Throwable e) {
			if (TRACE) trace("kryo", "sun.misc.Unsafe is unavailable.");
		}
	}

	{
		useAsmEnabled = !unsafeAvailable;
		varIntsEnabled = true;
		if (TRACE) trace("kryo", "Optimize ints: " + varIntsEnabled);
	}

	public FieldSerializer (Kryo kryo, Class type) {
		this.kryo = kryo;
		this.type = type;
		this.typeParameters = type.getTypeParameters();
		this.useAsmEnabled = kryo.getAsmEnabled();
		if (!this.useAsmEnabled && !unsafeAvailable) {
			this.useAsmEnabled = true;
			if (TRACE) trace("kryo", "sun.misc.Unsafe is unavailable, using ASM.");
		}
		this.genericsUtil = new FieldSerializerGenericsUtil(this);
		this.unsafeUtil = FieldSerializerUnsafeUtil.Factory.getInstance(this);
		this.annotationsUtil = new FieldSerializerAnnotationsUtil(this);
		rebuildCachedFields();
	}

	public FieldSerializer (Kryo kryo, Class type, Class[] generics) {
		this.kryo = kryo;
		this.type = type;
		this.generics = generics;
		this.typeParameters = type.getTypeParameters();
		this.useAsmEnabled = kryo.getAsmEnabled();
		if (!this.useAsmEnabled && !unsafeAvailable) {
			this.useAsmEnabled = true;
			if (TRACE) trace("kryo", "sun.misc.Unsafe is unavailable, using ASM.");
		}
		this.genericsUtil = new FieldSerializerGenericsUtil(this);
		this.unsafeUtil = FieldSerializerUnsafeUtil.Factory.getInstance(this);
		this.annotationsUtil = new FieldSerializerAnnotationsUtil(this);
		rebuildCachedFields();
	}

	/** Called when the list of cached fields must be rebuilt. This is done any time settings are changed that affect which fields
	 * will be used. It is called from the constructor for FieldSerializer, but not for subclasses. Subclasses must call this from
	 * their constructor. */
	protected void rebuildCachedFields () {
		rebuildCachedFields(false);
	}

	/**
	 * Rebuilds the list of cached fields.
	 * @param minorRebuild if set, processing due to changes in generic type parameters will be optimized
	 */
	protected void rebuildCachedFields (boolean minorRebuild) {
		/**
		 * TODO: Optimize rebuildCachedFields invocations performed due to changes in generic type parameters
		 */
		
		if (TRACE && generics != null) trace("kryo", "Generic type parameters: " + Arrays.toString(generics));
		if (type.isInterface()) {
			fields = new CachedField[0]; // No fields to serialize.
			return;
		}

		hasObjectFields = false;

		// For generic classes, generate a mapping from type variable names to the concrete types
		// This mapping is the same for the whole class.
		Generics genScope = genericsUtil.buildGenericsScope(type, generics);
		genericsScope = genScope;

		// Push proper scopes at serializer construction time
		if (genericsScope != null) kryo.pushGenericsScope(type, genericsScope);

		List<Field> validFields;
		List<Field> validTransientFields;
		IntArray useAsm = new IntArray();
		
		if (!minorRebuild) {
			// Collect all fields.
			List<Field> allFields = new ArrayList();
			Class nextClass = type;
			while (nextClass != Object.class) {
				Field[] declaredFields = nextClass.getDeclaredFields();
				if (declaredFields != null) {
					for (Field f : declaredFields) {
						if (Modifier.isStatic(f.getModifiers())) continue;
						allFields.add(f);
					}
				}
				nextClass = nextClass.getSuperclass();
			}

			ObjectMap context = kryo.getContext();

			// Sort fields by their offsets
			if (useMemRegions && !useAsmEnabled && unsafeAvailable) {
				try {
					Field[] allFieldsArray = (Field[])sortFieldsByOffsetMethod.invoke(null, allFields);
					allFields = Arrays.asList(allFieldsArray);
				} catch (Exception e) {
					throw new RuntimeException("Cannot invoke UnsafeUtil.sortFieldsByOffset()", e);
				}
			}

			// TODO: useAsm is modified as a side effect, this should be pulled out of buildValidFields
			// Build a list of valid non-transient fields
			validFields = buildValidFields(false, allFields, context, useAsm);
			// Build a list of valid transient fields
			validTransientFields = buildValidFields(true, allFields, context, useAsm);

			// Use ReflectASM for any public fields.
			if (useAsmEnabled && !Util.isAndroid && Modifier.isPublic(type.getModifiers()) && useAsm.indexOf(1) != -1) {
				try {
					access = FieldAccess.get(type);
				} catch (RuntimeException ignored) {
				}
			}
		} else {
			// It is a minor rebuild
			validFields = buildValidFieldsFromCachedFields(fields, useAsm);
			// Build a list of valid transient fields
			validTransientFields = buildValidFieldsFromCachedFields(transientFields, useAsm);
		}

		List<CachedField> cachedFields = new ArrayList(validFields.size());
		List<CachedField> cachedTransientFields = new ArrayList(validTransientFields.size());

		// Process non-transient fields
		createCachedFields(useAsm, validFields, cachedFields, 0);
		// Process transient fields
		createCachedFields(useAsm, validTransientFields, cachedTransientFields, validFields.size());

		Collections.sort(cachedFields, this);
		fields = cachedFields.toArray(new CachedField[cachedFields.size()]);

		Collections.sort(cachedTransientFields, this);
		transientFields = cachedTransientFields.toArray(new CachedField[cachedTransientFields.size()]);

		initializeCachedFields();

		if (genericsScope != null) kryo.popGenericsScope();

		for (CachedField field : removedFields)
			removeField(field);
		
		annotationsUtil.processAnnotatedFields(this);
	}

	private List<Field> buildValidFieldsFromCachedFields (CachedField[] cachedFields, IntArray useAsm) {
		ArrayList<Field> fields = new ArrayList<Field>(cachedFields.length);
		for(CachedField f: cachedFields) {
			fields.add(f.field);
			useAsm.add((f.accessIndex > -1)?1:0);
		}
		return fields;
	}

	private List<Field> buildValidFields (boolean transientFields, List<Field> allFields, ObjectMap context, IntArray useAsm) {
		List<Field> result = new ArrayList(allFields.size());

		for (int i = 0, n = allFields.size(); i < n; i++) {
			Field field = allFields.get(i);

			int modifiers = field.getModifiers();
			if (Modifier.isTransient(modifiers) != transientFields) continue;
			if (Modifier.isStatic(modifiers)) continue;
			if (field.isSynthetic() && ignoreSyntheticFields) continue;

			if (!field.isAccessible()) {
				if (!setFieldsAsAccessible) continue;
				try {
					field.setAccessible(true);
				} catch (AccessControlException ex) {
					continue;
				}
			}

			Optional optional = field.getAnnotation(Optional.class);
			if (optional != null && !context.containsKey(optional.value())) continue;

			result.add(field);

			// BOZO - Must be public?
			useAsm.add(!Modifier.isFinal(modifiers) && Modifier.isPublic(modifiers)
				&& Modifier.isPublic(field.getType().getModifiers()) ? 1 : 0);
		}
		return result;
	}

	private void createCachedFields (IntArray useAsm, List<Field> validFields, List<CachedField> cachedFields, int baseIndex) {

		if (useAsmEnabled || !useMemRegions) {
			for (int i = 0, n = validFields.size(); i < n; i++) {
				Field field = validFields.get(i);
				int accessIndex = -1;
				if (access != null && useAsm.get(baseIndex + i) == 1) accessIndex = ((FieldAccess)access).getIndex(field.getName());
				cachedFields.add(newCachedField(field, cachedFields.size(), accessIndex));
			}
		} else {
			unsafeUtil.createUnsafeCacheFieldsAndRegions(validFields, cachedFields, baseIndex, useAsm);
		}
	}

	public void setGenerics (Kryo kryo, Class[] generics) {
		this.generics = generics;
		if (typeParameters != null && typeParameters.length > 0) {
			// There is no need to rebuild all cached fields from scratch.
			// Generic parameter types do not affect the set of fields, offsets of fields,
			// transient and non-transient properties. They only affect the type of
			// fields and serializers selected for each field.
			rebuildCachedFields(true);
		}
	}
	
	/** Get generic type parameters of the class controlled by this serializer.
	 * @return generic type parameters or null, if there are none.
	 */
	public Class[] getGenerics() {
		return generics;
	}

	protected void initializeCachedFields () {
	}

	CachedField newCachedField (Field field, int fieldIndex, int accessIndex) {
		Class[] fieldClass = new Class[] {field.getType()};
		Type fieldGenericType = field.getGenericType();
		CachedField cachedField;

		if (fieldGenericType == fieldClass[0]) {
			// This is a field without generic type parameters
			if (TRACE) trace("kryo", "Field " + field.getName() + ": " + fieldClass[0]);
			cachedField = newMatchingCachedField(field, accessIndex, fieldClass[0], fieldGenericType, null);
		} else {
			cachedField = genericsUtil.newCachedFieldOfGenericType(field, accessIndex, fieldClass, fieldGenericType);
		}

		if (cachedField instanceof ObjectField) {
			hasObjectFields = true;
		}

		cachedField.field = field;
		cachedField.varIntsEnabled = varIntsEnabled;

		if (!useAsmEnabled) {
			cachedField.offset = unsafeUtil.getObjectFieldOffset(field);
		}

		cachedField.access = (FieldAccess)access;
		cachedField.accessIndex = accessIndex;
		cachedField.canBeNull = fieldsCanBeNull && !fieldClass[0].isPrimitive() && !field.isAnnotationPresent(NotNull.class);

		// Always use the same serializer for this field if the field's class is final.
		if (kryo.isFinal(fieldClass[0]) || fixedFieldTypes) cachedField.valueClass = fieldClass[0];

		return cachedField;
	}

	CachedField newMatchingCachedField (Field field, int accessIndex, Class fieldClass, Type fieldGenericType,
		Class[] fieldGenerics) {
		CachedField cachedField;
		if (accessIndex != -1) {
			cachedField = getAsmFieldFactory().createCachedField(fieldClass, field, this);
		} else if (!useAsmEnabled) {
			cachedField = getUnsafeFieldFactory().createCachedField(fieldClass, field, this);
		} else {
			cachedField = getObjectFieldFactory().createCachedField(fieldClass, field, this);
			if (fieldGenerics != null)
				((ObjectField)cachedField).generics = fieldGenerics;
			else {
				Class[] cachedFieldGenerics = FieldSerializerGenericsUtil.getGenerics(fieldGenericType, kryo);
				((ObjectField)cachedField).generics = cachedFieldGenerics;
				if (TRACE) trace("kryo", "Field generics: " + Arrays.toString(cachedFieldGenerics));
			}
		}
		return cachedField;
	}

	private CachedFieldFactory getAsmFieldFactory () {
		if (asmFieldFactory == null) asmFieldFactory = new AsmCachedFieldFactory();
		return asmFieldFactory;
	}

	private CachedFieldFactory getObjectFieldFactory () {
		if (objectFieldFactory == null) objectFieldFactory = new ObjectCachedFieldFactory();
		return objectFieldFactory;
	}

	private CachedFieldFactory getUnsafeFieldFactory () {
		// Use reflection to load UnsafeFieldFactory, so that there is no explicit dependency
		// on anything using Unsafe. This is required to make FieldSerializer work on those
		// platforms that do not support sun.misc.Unsafe properly.
		if (unsafeFieldFactory == null) {
			try {
				unsafeFieldFactory = (CachedFieldFactory)this.getClass().getClassLoader()
					.loadClass("com.esotericsoftware.kryo.serializers.UnsafeCachedFieldFactory").newInstance();
			} catch (Exception e) {
				throw new RuntimeException("Cannot create UnsafeFieldFactory", e);
			}
		}
		return unsafeFieldFactory;
	}

	public int compare (CachedField o1, CachedField o2) {
		// Fields are sorted by alpha so the order of the data is known.
		return o1.field.getName().compareTo(o2.field.getName());
	}

	/** Sets the default value for {@link CachedField#setCanBeNull(boolean)}. Calling this method resets the {@link #getFields()
	 * cached fields}.
	 * @param fieldsCanBeNull False if none of the fields are null. Saves 0-1 byte per field. True if it is not known (default). */
	public void setFieldsCanBeNull (boolean fieldsCanBeNull) {
		this.fieldsCanBeNull = fieldsCanBeNull;
		if (TRACE) trace("kryo", "setFieldsCanBeNull: " + fieldsCanBeNull);
		rebuildCachedFields();
	}

	/** Controls which fields are serialized. Calling this method resets the {@link #getFields() cached fields}.
	 * @param setFieldsAsAccessible If true, all non-transient fields (inlcuding private fields) will be serialized and
	 *           {@link Field#setAccessible(boolean) set as accessible} if necessary (default). If false, only fields in the public
	 *           API will be serialized. */
	public void setFieldsAsAccessible (boolean setFieldsAsAccessible) {
		this.setFieldsAsAccessible = setFieldsAsAccessible;
		if (TRACE) trace("kryo", "setFieldsAsAccessible: " + setFieldsAsAccessible);
		rebuildCachedFields();
	}

	/** Controls if synthetic fields are serialized. Default is true. Calling this method resets the {@link #getFields() cached
	 * fields}.
	 * @param ignoreSyntheticFields If true, only non-synthetic fields will be serialized. */
	public void setIgnoreSyntheticFields (boolean ignoreSyntheticFields) {
		this.ignoreSyntheticFields = ignoreSyntheticFields;
		if (TRACE) trace("kryo", "setIgnoreSyntheticFields: " + ignoreSyntheticFields);
		rebuildCachedFields();
	}

	/** Sets the default value for {@link CachedField#setClass(Class)} to the field's declared type. This allows FieldSerializer to
	 * be more efficient, since it knows field values will not be a subclass of their declared type. Default is false. Calling this
	 * method resets the {@link #getFields() cached fields}. */
	public void setFixedFieldTypes (boolean fixedFieldTypes) {
		this.fixedFieldTypes = fixedFieldTypes;
		if (TRACE) trace("kryo", "setFixedFieldTypes: " + fixedFieldTypes);
		rebuildCachedFields();
	}

	/** Controls whether ASM should be used. Calling this method resets the {@link #getFields() cached fields}.
	 * @param setUseAsm If true, ASM will be used for fast serialization. If false, Unsafe will be used (default) */
	public void setUseAsm (boolean setUseAsm) {
		useAsmEnabled = setUseAsm;
		if (!useAsmEnabled && !unsafeAvailable) {
			useAsmEnabled = true;
			if (TRACE) trace("kryo", "sun.misc.Unsafe is unavailable, using ASM.");
		}
		// optimizeInts = useAsmBackend;
		if (TRACE) trace("kryo", "setUseAsm: " + setUseAsm);
		rebuildCachedFields();
	}

	// Enable/disable copying of transient fields
	public void setCopyTransient (boolean setCopyTransient) {
		copyTransient = setCopyTransient;
	}

	/** This method can be called for different fields having the same type. Even though the raw type is the same, if the type is
	 * generic, it could happen that different concrete classes are used to instantiate it. Therefore, in case of different
	 * instantiation parameters, the fields analysis should be repeated.
	 * 
	 * TODO: Cache serializer instances generated for a given set of generic parameters. Reuse it later instead of recomputing
	 * every time. */
	public void write (Kryo kryo, Output output, T object) {
		if (TRACE) trace("kryo", "FieldSerializer.write fields of class: " + object.getClass().getName());

		if (typeParameters != null && generics != null) {
			// Rebuild fields info. It may result in rebuilding the genericScope
			rebuildCachedFields();
		}

		if (genericsScope != null) {
			// Push proper scopes at serializer usage time
			kryo.pushGenericsScope(type, genericsScope);
		}

		CachedField[] fields = this.fields;
		for (int i = 0, n = fields.length; i < n; i++)
			fields[i].write(output, object);

		// Serialize transient fields
		if (serializeTransient) {
			for (int i = 0, n = transientFields.length; i < n; i++)
				transientFields[i].write(output, object);
		}

		if (genericsScope != null) {
			// Pop the scope for generics
			kryo.popGenericsScope();
		}
	}

	public T read (Kryo kryo, Input input, Class<T> type) {
		try {

			if (typeParameters != null && generics != null) {
				// Rebuild fields info. It may result in rebuilding the
				// genericScope
				rebuildCachedFields();
			}

			if (genericsScope != null) {
				// Push a new scope for generics
				kryo.pushGenericsScope(type, genericsScope);
			}

			T object = create(kryo, input, type);
			kryo.reference(object);

			CachedField[] fields = this.fields;
			for (int i = 0, n = fields.length; i < n; i++)
				fields[i].read(input, object);

			// De-serialize transient fields
			if (serializeTransient) {
				for (int i = 0, n = transientFields.length; i < n; i++)
					transientFields[i].read(input, object);
			}
			return object;
		} finally {
			if (genericsScope != null && kryo.getGenericsScope() != null) {
				// Pop the scope for generics
				kryo.popGenericsScope();
			}
		}
	}

	/** Used by {@link #read(Kryo, Input, Class)} to create the new object. This can be overridden to customize object creation, eg
	 * to call a constructor with arguments. The default implementation uses {@link Kryo#newInstance(Class)}. */
	protected T create (Kryo kryo, Input input, Class<T> type) {
		return kryo.newInstance(type);
	}

	/** Allows specific fields to be optimized. */
	public CachedField getField (String fieldName) {
		for (CachedField cachedField : fields)
			if (cachedField.field.getName().equals(fieldName)) return cachedField;
		throw new IllegalArgumentException("Field \"" + fieldName + "\" not found on class: " + type.getName());
	}

	/** Removes a field so that it won't be serialized. */
	public void removeField (String fieldName) {
		for (int i = 0; i < fields.length; i++) {
			CachedField cachedField = fields[i];
			if (cachedField.field.getName().equals(fieldName)) {
				CachedField[] newFields = new CachedField[fields.length - 1];
				System.arraycopy(fields, 0, newFields, 0, i);
				System.arraycopy(fields, i + 1, newFields, i, newFields.length - i);
				fields = newFields;
				removedFields.add(cachedField);
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
				removedFields.add(cachedField);
				return;
			}
		}
		throw new IllegalArgumentException("Field \"" + removeField + "\" not found on class: " + type.getName());
	}

	/**
	 * Get all fields controlled by this FieldSerializer 
	 * @return all fields controlled by this FieldSerializer
	 */
	public CachedField[] getFields () {
		return fields;
	}
	
	public Class getType () {
		return type;
	}

	public Kryo getKryo () {
		return kryo;
	}

	public boolean getUseAsmEnabled () {
		return useAsmEnabled;
	}

	public boolean getUseMemRegions () {
		return useMemRegions;
	}

	public boolean getCopyTransient () {
		return copyTransient;
	}

	/** Used by {@link #copy(Kryo, Object)} to create the new object. This can be overridden to customize object creation, eg to
	 * call a constructor with arguments. The default implementation uses {@link Kryo#newInstance(Class)}. */
	protected T createCopy (Kryo kryo, T original) {
		return (T)kryo.newInstance(original.getClass());
	}

	public T copy (Kryo kryo, T original) {
		T copy = createCopy(kryo, original);
		kryo.reference(copy);

		// Copy transient fields
		if (copyTransient) {
			for (int i = 0, n = transientFields.length; i < n; i++)
				transientFields[i].copy(original, copy);
		}

		for (int i = 0, n = fields.length; i < n; i++)
			fields[i].copy(original, copy);

		return copy;
	}

	public final Generics getGenericsScope () {
		return genericsScope;
	}

	/** Controls how a field will be serialized. */
	public static abstract class CachedField<X> {
		Field field;
		FieldAccess access;
		Class valueClass;
		Serializer serializer;
		boolean canBeNull;
		int accessIndex = -1;
		long offset = -1;
		boolean varIntsEnabled = true;

		/** @param valueClass The concrete class of the values for this field. This saves 1-2 bytes. The serializer registered for the
		 *           specified class will be used. Only set to a non-null value if the field type in the class definition is final
		 *           or the values for this field will not vary. */
		public void setClass (Class valueClass) {
			this.valueClass = valueClass;
			this.serializer = null;
		}

		/** @param valueClass The concrete class of the values for this field. This saves 1-2 bytes. Only set to a non-null value if
		 *           the field type in the class definition is final or the values for this field will not vary. */
		public void setClass (Class valueClass, Serializer serializer) {
			this.valueClass = valueClass;
			this.serializer = serializer;
		}

		public void setSerializer (Serializer serializer) {
			this.serializer = serializer;
		}
		
		public Serializer getSerializer() {
			return this.serializer;
		}

		public void setCanBeNull (boolean canBeNull) {
			this.canBeNull = canBeNull;
		}

		public Field getField () {
			return field;
		}

		public String toString () {
			return field.getName();
		}

		abstract public void write (Output output, Object object);

		abstract public void read (Input input, Object object);

		abstract public void copy (Object original, Object copy);
	}

	public static interface CachedFieldFactory {
		public CachedField createCachedField (Class fieldClass, Field field, FieldSerializer ser);
	}

	/** Indicates a field should be ignored when its declaring class is registered unless the {@link Kryo#getContext() context} has
	 * a value set for the specified key. This can be useful when a field must be serialized for one purpose, but not for another.
	 * Eg, a class for a networked application could have a field that should not be serialized and sent to clients, but should be
	 * serialized when stored on the server.
	 * @author Nathan Sweet <misc@n4te.com> */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	static public @interface Optional {
		public String value();
	}

	/**
	 * Used to annotate fields with a specific Kryo serializer.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Bind {

	    /**
	     * Value.
	     * 
	     * @return the class<? extends serializer> used for this field
	     */
	    @SuppressWarnings("rawtypes")
	    Class<? extends Serializer> value();

	}
}
