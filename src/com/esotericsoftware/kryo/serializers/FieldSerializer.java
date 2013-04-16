
package com.esotericsoftware.kryo.serializers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.esotericsoftware.kryo.Generics;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.IntArray;
import com.esotericsoftware.kryo.util.ObjectMap;
import static com.esotericsoftware.kryo.util.UnsafeUtil.unsafe;
import com.esotericsoftware.kryo.util.Util;
import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.kryo.serializers.AsmCacheFields.*;
import com.esotericsoftware.kryo.serializers.UnsafeCacheFields.*;
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
	Object access;
	private boolean fieldsCanBeNull = true, setFieldsAsAccessible = true;
	private boolean ignoreSyntheticFields = true;
	private boolean fixedFieldTypes;
	/** If set, ASM-backend is used. Otherwise Unsafe-based backend or reflection is used */
	private boolean useAsmBackend;
	
	/** Concrete classes passed as values for type variables */
	private Class[] generics;
	
	
	private Generics genericsScope;
	
	/** If set, this serializer tries to use a variable length encoding
	* for int and long fields
	*/ 
	private boolean optimizeInts;
	
	/** If set, adjacent primitive fields are written in bulk
	* This flag may only work with Oracle JVMs, because they 
	* layout primitive fields in memory in such a way that 
	* primitive fields are grouped together.
	* This option has effect only when used with Unsafe-based FieldSerializer.
	* <p>
	* FIXME: Not all versions of Sun/Oracle JDK properly work with this option. Disable it for now. Later add dynamic checks
	* to see if this feature is supported by a current JDK version.
	* </p>
	*/
	private boolean useMemRegions = false;
	
	/** If set, transient fields will be copied */
	private final boolean copyTransient = true;
	
	/** If set, transient fields will be serialized */
	private final boolean serializeTransient = false;
	
	private boolean hasObjectFields = false;
	
	{
		useAsmBackend = unsafe() != null;
		optimizeInts = true;
		if(TRACE) trace("kryo", "optimize ints is " + optimizeInts);
	}

	// BOZO - Get rid of kryo here?
	public FieldSerializer (Kryo kryo, Class type) {
		this.kryo = kryo;
		this.type = type;
		this.typeParameters = type.getTypeParameters();
		this.useAsmBackend = kryo.getAsmBackend();
		if(TRACE) trace("kryo", "FieldSerializer(Kryo, Class)");
		rebuildCachedFields();
	}

	public FieldSerializer (Kryo kryo, Class type, Class[] generics) {
		this.kryo = kryo;
		this.type = type;
		this.generics = generics;
		this.typeParameters = type.getTypeParameters();
		this.useAsmBackend = kryo.getAsmBackend();
		if(TRACE) trace("kryo", "FieldSerializer(Kryo, Class, Generics)");
		rebuildCachedFields();
	}

	/***
	 * Create a mapping from type variable names (which are declared as type parameters of a generic class) to the 
	 * concrete classes used for type instantiation. 
	 *  
	 * @param clazz class with generic type arguments 
	 * @param generics concrete types used to instantiate the class
	 * @return new scope for type parameters
	 */
	private Generics buildGenericsScope(Class clazz, Class[] generics) {
		Class typ = clazz;
		TypeVariable[] typeParams = null;

		while (typ != null) {
			typeParams = typ.getTypeParameters();
			if (typeParams == null || typeParams.length == 0) {
				typ = typ.getComponentType();
			} else
				break;
		}		
		
		if(typeParams != null && typeParams.length > 0) {
				Generics genScope;
				trace("kryo", "Class " + clazz.getName() + " has generic type parameters");
				int typeVarNum = 0;
				Map<String, Class> typeVar2concreteClass;
				typeVar2concreteClass = new HashMap<String, Class>();
				for(TypeVariable typeVar: typeParams) {
					String typeVarName = typeVar.getName();
					if(TRACE) { 
						trace("kryo", "Type parameter variable: name=" + typeVarName + 
							" type bounds=" + Arrays.toString(typeVar.getBounds()));
					}
					if(generics != null && generics.length>typeVarNum) {
						// If passed concrete classes are known explicitly, use this information
						typeVar2concreteClass.put(typeVarName, generics[typeVarNum]);
						if(TRACE) trace("kryo", "Concrete type used for " +typeVarName + " is: " + generics[typeVarNum].getName());
					} else {
						// Otherwise try to derive the information from the current GenericScope
						if(TRACE) trace("kryo", "Trying to use kryo.getGenericScope");						
						Generics scope = kryo.getGenericsScope();
						if(scope != null) {
							Class concreteClass = scope.getConcreteClass(typeVarName);
							if(concreteClass != null) {
								typeVar2concreteClass.put(typeVarName, concreteClass);
								if(TRACE) {
									trace("kryo", "Concrete type used for " +typeVarName + " is: " + 
										concreteClass.getName());							
								}
							}
						}
					}

					typeVarNum++;
			}
			genScope = new Generics(typeVar2concreteClass);
			return genScope;
		} else
			return null;
	}
	
	/** Called when the list of cached fields must be rebuilt. This is done any time settings are changed that affect which fields
	 * will be used. It is called from the constructor for FieldSerializer, but not for subclasses. Subclasses must call this from
	 * their constructor. */
	private void rebuildCachedFields () {
		if(TRACE) {
			// new RuntimeException("Call stack for rebuildCachedFields").printStackTrace(System.out);
			trace("kryo", "rebuilding cache fields for " + type.getName());
		}
		if(TRACE && generics != null) trace("kryo", "generic type parameters are" + Arrays.toString(generics));
		if (type.isInterface()) {
			fields = new CachedField[0]; // No fields to serialize.
			return;
		}
		
		hasObjectFields = false;

		// For generic classes, generate a mapping from type variable names to the concrete types
		// This mapping is the same for the whole class.
		Generics genScope = buildGenericsScope(type, generics);
		genericsScope = genScope;
		
		// Push proper scopes at serializer construction time
		if(genericsScope!=null)
			kryo.pushGenericsScope(type, genericsScope);

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

		IntArray useAsm = new IntArray();

		// Sort fields by their offsets
		if (useMemRegions && !useAsmBackend && unsafe() != null) {
			Field[] allFieldsArray = allFields.toArray(new Field[] {});

			Comparator<Field> fieldOffsetComparator = new Comparator<Field>() {
				@Override
				public int compare(Field f1, Field f2) {
					long offset1 = unsafe().objectFieldOffset(f1);
					long offset2 = unsafe().objectFieldOffset(f2);
					if (offset1 < offset2)
						return -1;
					if (offset1 == offset2)
						return 0;
					return 1;
				}
			};

			Arrays.sort(allFieldsArray, fieldOffsetComparator);
			allFields = Arrays.asList(allFieldsArray);
			for(Field f: allFields) {
				if(TRACE) trace("kryo", "Field " + f.getName()+ " at offset " + unsafe().objectFieldOffset(f));
			}
		}			
		
		ArrayList<Field> validFields = new ArrayList(allFields.size());
		
		
		for (int i = 0, n = allFields.size(); i < n; i++) {
			Field field = allFields.get(i);

			int modifiers = field.getModifiers();
			if (Modifier.isTransient(modifiers)) continue;
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

			validFields.add(field);

			// BOZO - Must be public?
			useAsm.add(!Modifier.isFinal(modifiers) && Modifier.isPublic(modifiers)
				&& Modifier.isPublic(field.getType().getModifiers()) ? 1 : 0);
		}

		ArrayList<Field> validTransientFields = new ArrayList(allFields.size());
		
		
		// Process transient fields
		for (int i = 0, n = allFields.size(); i < n; i++) {
			Field field = allFields.get(i);

			int modifiers = field.getModifiers();
			if (!Modifier.isTransient(modifiers)) continue;
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

			validTransientFields.add(field);

			// BOZO - Must be public?
			useAsm.add(!Modifier.isFinal(modifiers) && Modifier.isPublic(modifiers)
				&& Modifier.isPublic(field.getType().getModifiers()) ? 1 : 0);
		}
		
		// Use ReflectASM for any public fields.
		if (useAsmBackend && !Util.isAndroid && Modifier.isPublic(type.getModifiers()) && useAsm.indexOf(1) != -1) {
			try {
				access = FieldAccess.get(type);
			} catch (RuntimeException ignored) {
			}
		}

		ArrayList<CachedField> cachedFields = new ArrayList(validFields.size());
		ArrayList<CachedField> cachedTransientFields = new ArrayList(validTransientFields.size());
		
		createCachedFields(useAsm, validFields, cachedFields, 0);
		createCachedFields(useAsm, validTransientFields, cachedTransientFields, validFields.size());

		Collections.sort(cachedFields, this);
		fields = cachedFields.toArray(new CachedField[cachedFields.size()]);

		Collections.sort(cachedTransientFields, this);
		transientFields = cachedTransientFields.toArray(new CachedField[cachedTransientFields.size()]);
		
		initializeCachedFields();

		if(genericsScope!=null)
			kryo.popGenericsScope();
	}

	private void createCachedFields(IntArray useAsm,
			ArrayList<Field> validFields, ArrayList<CachedField> cachedFields, int baseIndex) {
		// Find adjacent fields of primitive types
		long startPrimitives = 0;
		long endPrimitives = 0;
		boolean lastWasPrimitive = false;
		int primitiveLength = 0;
		int lastAccessIndex = -1;
		Field lastField = null;
		long fieldOffset = -1;
		long fieldEndOffset = -1;
		long lastFieldEndOffset = -1;
		
		for (int i = 0, n = validFields.size(); i < n; i++) {
			Field field = validFields.get(i);

			int accessIndex = -1;
			if (access != null && useAsm.get(baseIndex + i) == 1) 
				accessIndex = ((FieldAccess)access).getIndex(field.getName());

			if(useAsmBackend || !useMemRegions)
				cachedFields.add(newCachedField(field, cachedFields.size(), accessIndex));
			else {

				fieldOffset = unsafe().objectFieldOffset(field);
				fieldEndOffset = fieldOffset + fieldSizeOf(field.getType());

				if (!field.getType().isPrimitive() && lastWasPrimitive) {
					// This is not a primitive field. Therefore, it marks
					// the end of a region of primitive fields
					endPrimitives = lastFieldEndOffset;
					lastWasPrimitive = false;
					if (primitiveLength > 1) {
						if(TRACE)
								trace("kryo", "Class "
										+ type.getName()
										+ ". Found a set of consecutive primitive fields. Number of fields = "
										+ primitiveLength
										+ ". Byte length = "
										+ (endPrimitives - startPrimitives)
										+ " Start offset = "
										+ startPrimitives + " endOffset="
										+ endPrimitives);
						// TODO: register a region instead of a field
						CachedField cf = new UnsafeRegionField(startPrimitives, (endPrimitives - startPrimitives));
						cf.field = lastField;
						cachedFields.add(cf);						
					} else {
						if(lastField != null)
							cachedFields.add(newCachedField(lastField, cachedFields.size(), lastAccessIndex));
					}						
					cachedFields.add(newCachedField(field, cachedFields.size(), accessIndex));						
				} else if(!field.getType().isPrimitive() ) {
					cachedFields.add(newCachedField(field, cachedFields.size(), accessIndex));											
				} else if (!lastWasPrimitive) {
					// If previous field was non primitive, it marks a start
					// of a region of primitive fields
					startPrimitives = fieldOffset;
					lastWasPrimitive = true;
					primitiveLength = 1;
				} else {
					primitiveLength++;
				}
			}
			
			lastAccessIndex = accessIndex;
			lastField = field;
			lastFieldEndOffset = fieldEndOffset;
		}
		
		if(!useAsmBackend && useMemRegions && lastWasPrimitive) {
			endPrimitives = lastFieldEndOffset; 
			if (primitiveLength > 1) {
				if(TRACE) {
						trace("kryo", "Class "
								+ type.getName()
								+ ". Found a set of consecutive primitive fields. Number of fields = "
								+ primitiveLength
								+ ". Byte length = "
								+ (endPrimitives - startPrimitives)
								+ " Start offset = "
								+ startPrimitives + " endOffset="
								+ endPrimitives);
				}
				// register a region instead of a field
				CachedField cf = new UnsafeRegionField(startPrimitives, (endPrimitives - startPrimitives));
				cf.field = lastField;
				cachedFields.add(cf);						
			} else {
				if(lastField != null)
					cachedFields.add(newCachedField(lastField, cachedFields.size(), lastAccessIndex));
			}
		}
	}
	
	private int fieldSizeOf(Class<?> clazz) {
		if(clazz == int.class || clazz == float.class)
			return 4;

		if(clazz == long.class || clazz == double.class)
			return 8;

		if(clazz == byte.class || clazz == boolean.class)
			return 1;
		
		if(clazz == short.class || clazz == char.class)
			return 2;
		
		// Everything else is a reference to an object, i.e. an address
		return unsafe().addressSize();
	}

	public void setGenerics (Kryo kryo, Class[] generics) {
		this.generics = generics;
		if(TRACE) trace("kryo", "setGenerics");
		if(typeParameters != null && typeParameters.length > 0)
			rebuildCachedFields();
	}
	
	protected void initializeCachedFields () {
	}
	
	
	private Class[] computeFieldGenerics(Type fieldGenericType, Field field) {
		Class[] fieldGenerics = null;
		if(fieldGenericType != null) {
			if (fieldGenericType instanceof TypeVariable) {
				TypeVariable typeVar = (TypeVariable) fieldGenericType;
				// Obtain information about a concrete type of a given variable from the environment  
				Class concreteClass = genericsScope.getConcreteClass(typeVar.getName());
				if(concreteClass != null) {
					Class fieldClass = concreteClass;
					fieldGenerics = new Class[] {fieldClass};
					if(TRACE) trace("kryo", "Determined concrete class of  " + field.getName() + " to be " + fieldClass.getName());
				}
			} else if(fieldGenericType instanceof ParameterizedType) {
				ParameterizedType parameterizedType = (ParameterizedType)fieldGenericType;
				// Get actual type arguments of the current field's type
				Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
//				if(actualTypeArguments != null && generics != null) {
				if(actualTypeArguments != null) {
					fieldGenerics = new Class[actualTypeArguments.length];
					for(int i=0; i < actualTypeArguments.length; ++i) {
						Type t = actualTypeArguments[i];
						if (t instanceof Class)
							fieldGenerics[i] = (Class)t;
						else if (t instanceof ParameterizedType)
							fieldGenerics[i] = (Class)((ParameterizedType)t).getRawType();
						else if (t instanceof TypeVariable)
							fieldGenerics[i] = genericsScope.getConcreteClass(((TypeVariable) t).getName());
						else if (t instanceof WildcardType)
							fieldGenerics[i] = Object.class;
						else
							fieldGenerics[i] = null;
					}
					if(TRACE && fieldGenerics != null) {
						trace("kryo", "Determined concrete class of  " + field.getName() + " to be " + 
							fieldGenericType + " where type parameters are " + Arrays.toString(fieldGenerics)); 
					}
				}
			} else if(fieldGenericType instanceof GenericArrayType) {
				// TODO: store generics for arrays as well?
				GenericArrayType arrayType = (GenericArrayType)fieldGenericType;
				Type genericComponentType = arrayType.getGenericComponentType();
				fieldGenerics = computeFieldGenerics(genericComponentType, field);
//				Kryo.getGenerics(fieldGenericType);
				if(TRACE && fieldGenerics != null) {
					trace("kryo", "Determined concrete class of  " + field.getName() + " to be " + fieldGenericType + 
						" where type parameters are " + Arrays.toString(fieldGenerics));				
				}
				if(TRACE) 
					trace("kryo", "Determined concrete class of  " + field.getName() + " to be " + fieldGenericType);		
			}
		}
		
		return fieldGenerics;
	}

	private CachedField newCachedField (Field field, int fieldIndex, int accessIndex) {
		Class fieldClass = field.getType();
		Type fieldGenericType = field.getGenericType();
		Class[] fieldGenerics = null;
		
		if(TRACE) {
			if(fieldGenericType != fieldClass)
				trace("kryo", "Field " + field.getName() + " of type " + fieldClass + " of generic type " + fieldGenericType);
			else
				trace("kryo", "Field " + field.getName() + " of type " + fieldClass);
		}
		
		if(TRACE && fieldGenericType != null) 
			trace("kryo", "Field generic type is of class " + fieldGenericType.getClass().getName());
		
		if(fieldClass != fieldGenericType) {
			// Get set of provided type parameters
			
			// Get list of field specific concrete classes passed as generic parameters 
			Class[] cachedFieldGenerics = kryo.getGenerics(fieldGenericType);
			
			// Build a generics scope for this field
			Generics scope = buildGenericsScope(fieldClass, cachedFieldGenerics);
			
			// Is it a field of a generic parameter type, i.e. "T field"? 
			if (fieldClass == Object.class && fieldGenericType instanceof TypeVariable && genericsScope != null) {
				TypeVariable typeVar = (TypeVariable) fieldGenericType;
				// Obtain information about a concrete type of a given variable from the environment  
				Class concreteClass = genericsScope.getConcreteClass(typeVar.getName());
				if (concreteClass != null) {
					scope = new Generics();
					scope.add(typeVar.getName(), concreteClass);
				}
			}
			
			if(TRACE) {
				trace("kryo", "Generics scope of field " + field.getName() + " of class " + 
					fieldGenericType + " is " + scope);			
			}
		}
		
		if(fieldGenericType != null) {
			if (fieldGenericType instanceof TypeVariable && genericsScope != null) {
				TypeVariable typeVar = (TypeVariable) fieldGenericType;
				// Obtain information about a concrete type of a given variable from the environment  
				Class concreteClass = genericsScope.getConcreteClass(typeVar.getName());
				if(concreteClass != null) {
					fieldClass = concreteClass;
					fieldGenerics = new Class[] {fieldClass};
					if(TRACE) trace("kryo", "Determined concrete class of  " + field.getName() + " to be " + fieldClass.getName());
				}
			} else if(fieldGenericType instanceof ParameterizedType) {
				ParameterizedType parameterizedType = (ParameterizedType)fieldGenericType;
				// Get actual type arguments of the current field's type
				Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
//				if(actualTypeArguments != null && generics != null) {
				if(actualTypeArguments != null) {
					fieldGenerics = new Class[actualTypeArguments.length];
					for(int i=0; i < actualTypeArguments.length; ++i) {
						Type t = actualTypeArguments[i];
						if (t instanceof Class)
							fieldGenerics[i] = (Class)t;
						else if (t instanceof ParameterizedType)
							fieldGenerics[i] = (Class)((ParameterizedType)t).getRawType();
						else if (t instanceof TypeVariable && genericsScope != null)
							fieldGenerics[i] = genericsScope.getConcreteClass(((TypeVariable) t).getName());
						else if (t instanceof WildcardType)
							fieldGenerics[i] = Object.class;
						else
							fieldGenerics[i] = null;
					}
					if(TRACE && fieldGenerics != null) {
						trace("kryo", "Determined concrete class of  " + field.getName() + " to be " + fieldGenericType + 
							" where type parameters are " + Arrays.toString(fieldGenerics));
					}
				}
			} else if(fieldGenericType instanceof GenericArrayType) {
				// TODO: store generics for arrays as well?
				GenericArrayType arrayType = (GenericArrayType)fieldGenericType;
				Type genericComponentType = arrayType.getGenericComponentType();
				fieldGenerics = computeFieldGenerics(genericComponentType, field);
//				Type componentType = arrayType.getGenericComponentType();
//				Kryo.getGenerics(fieldGenericType);
				if(TRACE && fieldGenerics != null) {
					trace("kryo", "Determined concrete class of  " + field.getName() + " to be " + fieldGenericType + 
						" where type parameters are " + Arrays.toString(fieldGenerics));				
				}
				if(TRACE) trace("kryo", "Determined concrete class of  " + field.getName() + " to be " + fieldGenericType);		
			}
		}

		CachedField cachedField;
		if (accessIndex != -1) {
			if (fieldClass.isPrimitive()) {
				if (fieldClass == boolean.class)
					cachedField = new BooleanField();
				else if (fieldClass == byte.class)
					cachedField = new ByteField();
				else if (fieldClass == char.class)
					cachedField = new CharField();
				else if (fieldClass == short.class)
					cachedField = new ShortField();
				else if (fieldClass == int.class)
					cachedField = new IntField();
				else if (fieldClass == long.class)
					cachedField = new LongField();
				else if (fieldClass == float.class)
					cachedField = new FloatField();
				else if (fieldClass == double.class)
					cachedField = new DoubleField();
				else {
					if (TRACE)
						trace("kryo", "ObjectField1");
					cachedField = new AsmObjectField(this);
				}
			} else if (fieldClass == String.class
				&& (!kryo.getReferences() || !kryo.getReferenceResolver().useReferences(String.class))) {
				cachedField = new StringField();
			} else {
				if(TRACE) trace("kryo", "ObjectField2");
				cachedField = new AsmObjectField(this);
			}
		} else if(!useAsmBackend) {
			if (fieldClass.isPrimitive()) {
				if (fieldClass == boolean.class)
					cachedField = new UnsafeBooleanField(field);
				else if (fieldClass == byte.class)
					cachedField = new UnsafeByteField(field);
				else if (fieldClass == char.class)
					cachedField = new UnsafeCharField(field);
				else if (fieldClass == short.class)
					cachedField = new UnsafeShortField(field);
				else if (fieldClass == int.class)
					cachedField = new UnsafeIntField(field);
				else if (fieldClass == long.class)
					cachedField = new UnsafeLongField(field);
				else if (fieldClass == float.class)
					cachedField = new UnsafeFloatField(field);
				else if (fieldClass == double.class)
					cachedField = new UnsafeDoubleField(field);
				else {
					if (TRACE)
						trace("kryo", "ObjectField1");
					cachedField = new UnsafeObjectField(this);
				}
			} else if (fieldClass == String.class
				&& (!kryo.getReferences() || !kryo.getReferenceResolver().useReferences(String.class))) {
				cachedField = new UnsafeStringField(field);
			} else {
				if(TRACE) trace("kryo", "ObjectField2");
				cachedField = new UnsafeObjectField(this);
			}			
		} else {
			if(TRACE) trace("kryo", "ObjectField3");
			cachedField = new ObjectField(this);
			if(fieldGenerics != null)
				((ObjectField)cachedField).generics = fieldGenerics;
			else {
				Class[] cachedFieldGenerics = kryo.getGenerics(fieldGenericType);
				((ObjectField) cachedField).generics = cachedFieldGenerics;
				if(TRACE) trace("kryo", "Field generics: " + Arrays.toString(cachedFieldGenerics));
			}
		}

		if(fieldGenerics != null && cachedField instanceof FieldSerializer.ObjectField) {
			if (fieldGenerics[0] != null) {
				// If any information about concrete types for generic arguments of current field's type
				// was deriver, remember it.
				((ObjectField) cachedField).generics = fieldGenerics;
				if (TRACE)
					trace("kryo",
							"Field generics: " + Arrays.toString(fieldGenerics));
			}
		}
		if(cachedField instanceof FieldSerializer.ObjectField) {
			hasObjectFields = true;
		}
		
		cachedField.field = field;
		cachedField.optimizeInts = optimizeInts;
		
		if(!useAsmBackend) {
			cachedField.offset = unsafe().objectFieldOffset(field);
		}
		
		cachedField.access = (FieldAccess) access;
		cachedField.accessIndex = accessIndex;
		cachedField.canBeNull = fieldsCanBeNull && !fieldClass.isPrimitive() && !field.isAnnotationPresent(NotNull.class);

		// Always use the same serializer for this field if the field's class is final.
		if (kryo.isFinal(fieldClass) || fixedFieldTypes) cachedField.valueClass = fieldClass;

		return cachedField;
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
		if(TRACE) trace("kryo", "setFieldsCanBeNull");
		rebuildCachedFields();
	}

	/** Controls which fields are serialized. Calling this method resets the {@link #getFields() cached fields}.
	 * @param setFieldsAsAccessible If true, all non-transient fields (inlcuding private fields) will be serialized and
	 *           {@link Field#setAccessible(boolean) set as accessible} if necessary (default). If false, only fields in the public
	 *           API will be serialized. */
	public void setFieldsAsAccessible (boolean setFieldsAsAccessible) {
		this.setFieldsAsAccessible = setFieldsAsAccessible;
		if(TRACE) trace("kryo", "setFieldsAsAccessible: " + setFieldsAsAccessible);
		rebuildCachedFields();
	}

	/** Controls if synthetic fields are serialized. Default is true. Calling this method resets the {@link #getFields() cached
	 * fields}.
	 * @param ignoreSyntheticFields If true, only non-synthetic fields will be serialized. */
	public void setIgnoreSyntheticFields (boolean ignoreSyntheticFields) {
		this.ignoreSyntheticFields = ignoreSyntheticFields;
		if(TRACE) trace("kryo", "setIgnoreSyntheticFields:" + ignoreSyntheticFields);
		rebuildCachedFields();
	}

	/** Sets the default value for {@link CachedField#setClass(Class)} to the field's declared type. This allows FieldSerializer to
	 * be more efficient, since it knows field values will not be a subclass of their declared type. Default is false. Calling this
	 * method resets the {@link #getFields() cached fields}. */
	public void setFixedFieldTypes (boolean fixedFieldTypes) {
		this.fixedFieldTypes = fixedFieldTypes;
		if(TRACE) trace("kryo", "setFixedFieldTypes: " + fixedFieldTypes);
		rebuildCachedFields();
	}

	/** Controls whether ASM should be used. Calling this method resets the {@link #getFields() cached fields}.
	 * @param setUseAsm If true, ASM will be used for fast serialization. If false, Unsafe will be used (default)
	 */
	public void setUseAsm (boolean setUseAsm) {
		useAsmBackend = setUseAsm;
//		optimizeInts = useAsmBackend;
		if(TRACE) trace("kryo", "setUseAsm: " + setUseAsm);
		rebuildCachedFields();
	}

//  Uncomment this method, if we want to allow explicit control over copying of transient fields
//	public void setCopyTransient (boolean setCopyTransient) {
//		copyTransient = setCopyTransient;
//		if(TRACE) trace("kryo", "setCopyTransient");
//	}

	/**
	 * This method can be called for different fields having the same type.
	 * Even though the raw type is the same, if the type is generic, it could
	 * happen that different concrete classes are used to instantiate it.
	 * Therefore, in case of different instantiation parameters, the 
	 * fields analysis should be repeated.
	 * 
	 * TODO: Cache serializer instances generated for a given set of generic parameters.
	 * Reuse it later instead of recomputing every time. 
	 *   
	 */
	public void write (Kryo kryo, Output output, T object) {
		if(TRACE) trace("kryo", "FieldSerializer.write fields of class " + object.getClass().getName());
		

		if(typeParameters != null && generics != null) {
			// Rebuild fields info. It may result in rebuilding the genericScope
			rebuildCachedFields();
		}
		
		if(genericsScope != null) {
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

		if(genericsScope != null) {
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
				return;
			}
		}
		throw new IllegalArgumentException("Field \"" + fieldName + "\" not found on class: " + type.getName());
	}

	public CachedField[] getFields () {
		return fields;
	}

	public Class getType () {
		return type;
	}

	/** Used by {@link #copy(Kryo, Object)} to create the new object. This can be overridden to customize object creation, eg to
	 * call a constructor with arguments. The default implementation uses {@link Kryo#newInstance(Class)}. */
	protected T createCopy (Kryo kryo, T original) {
		return (T)kryo.newInstance(original.getClass());
	}

	public T copy(Kryo kryo, T original) {
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

	public final Generics getGenericsScope() {
		return  genericsScope;
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
		boolean optimizeInts = true;

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
	
	/***
	 * Defer generation of serializers until it is really required at run-time.
	 * By default, use reflection-based approach.
	 *
	 */
	public static class ObjectField extends CachedField {
		public Class[] generics;
		final FieldSerializer fieldSerializer;
		final Class type;
		final Kryo kryo;
		
		ObjectField(FieldSerializer fieldSerializer){
			this.fieldSerializer = fieldSerializer;
			this.kryo = fieldSerializer.kryo;
			this.type = fieldSerializer.type;
		}
		
		public Object getField(Object object) throws IllegalArgumentException, IllegalAccessException {
			return field.get(object);
		}

		public void setField(Object object, Object value) throws IllegalArgumentException, IllegalAccessException {
			field.set(object, value);
		}
		
		final public void write (Output output, Object object) {
			try {
//				if(typeVar2concreteClass != null) {
//					// Push a new scope for generics
//					kryo.pushGenericsScope(type, new Generics(typeVar2concreteClass));
//				}
				
				if (TRACE) trace("kryo", "Write field: " + this + " (" + object.getClass().getName() + ")"  + " pos=" + output.position());

				Object value = getField(object);

				Serializer serializer = this.serializer;
				if (valueClass == null) {
					// The concrete type of the field is unknown, write the class first.
					if (value == null) {
						kryo.writeClass(output, null);
						return;
					}
					Registration registration = kryo.writeClass(output, value.getClass());
					if (serializer == null) serializer = registration.getSerializer();
//					if (generics != null) 
						serializer.setGenerics(kryo, generics);
					kryo.writeObject(output, value, serializer);
				} else {
					// The concrete type of the field is known, always use the same serializer.
					if (serializer == null) this.serializer = serializer = kryo.getSerializer(valueClass);
//					if (generics != null) 
						serializer.setGenerics(kryo, generics);
					if (canBeNull) {
						kryo.writeObjectOrNull(output, value, serializer);
					} else {
						if (value == null) {
							throw new KryoException("Field value is null but canBeNull is false: " + this + " ("
								+ object.getClass().getName() + ")");
						}
						kryo.writeObject(output, value, serializer);
					}
				}
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field: " + this + " (" + object.getClass().getName() + ")", ex);
			} catch (KryoException ex) {
				ex.addTrace(this + " (" + object.getClass().getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(this + " (" + object.getClass().getName() + ")");
				throw ex;
			} finally {
//				if(typeVar2concreteClass != null)
//					kryo.popGenericsScope();				
			}
		}

		final public void read (Input input, Object object) {
			try {
				if (TRACE) trace("kryo", "Read field: " + this + " (" + type.getName() + ")" + " pos=" + input.position());
				Object value;

				Class concreteType = valueClass;
				Serializer serializer = this.serializer;
				if (concreteType == null) {
					Registration registration = kryo.readClass(input);
					if (registration == null)
						value = null;
					else {
						if (serializer == null) serializer = registration.getSerializer();
//						if (generics != null) 
							serializer.setGenerics(kryo, generics);
						value = kryo.readObject(input, registration.getType(), serializer);
					}
				} else {
					if (serializer == null) this.serializer = serializer = kryo.getSerializer(valueClass);
//					if (generics != null) 
						serializer.setGenerics(kryo, generics);
					if (canBeNull)
						value = kryo.readObjectOrNull(input, concreteType, serializer);
					else
						value = kryo.readObject(input, concreteType, serializer);
				}

				setField(object, value);
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field: " + this + " (" + type.getName() + ")", ex);
			} catch (KryoException ex) {
				ex.addTrace(this + " (" + type.getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(this + " (" + type.getName() + ")");
				throw ex;
			} finally {
//				if(typeVar2concreteClass != null)
//					kryo.popGenericsScope();								
			}
		}

		public void copy (Object original, Object copy) {
			try {
				if (accessIndex != -1) {
					FieldAccess access = (FieldAccess)fieldSerializer.access;
					access.set(copy, accessIndex, kryo.copy(access.get(original, accessIndex)));
				} else
					field.set(copy, kryo.copy(field.get(original)));
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field: " + this + " (" + type.getName() + ")", ex);
			} catch (KryoException ex) {
				ex.addTrace(this + " (" + type.getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(this + " (" + type.getName() + ")");
				throw ex;
			}
		}
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
}
