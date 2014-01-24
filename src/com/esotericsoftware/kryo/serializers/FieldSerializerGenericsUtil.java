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
import java.util.HashMap;
import java.util.Map;

import com.esotericsoftware.kryo.Generics;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;

/**
 * A few utility methods for using generic type parameters, mostly by FieldSerializer
 * 
 * @author Roman Levenstein <romixlev@gmail.com>
 */
final class FieldSerializerGenericsUtil {
	private Kryo kryo;
	private FieldSerializer serializer;

	public FieldSerializerGenericsUtil (FieldSerializer serializer) {
		this.serializer = serializer;
		this.kryo = serializer.getKryo();
	}

	/*** Create a mapping from type variable names (which are declared as type parameters of a generic class) to the concrete classes
	 * used for type instantiation.
	 * 
	 * @param clazz class with generic type arguments
	 * @param generics concrete types used to instantiate the class
	 * @return new scope for type parameters */
	Generics buildGenericsScope (Class clazz, Class[] generics) {
		Class typ = clazz;
		TypeVariable[] typeParams = null;

		while (typ != null) {
			typeParams = typ.getTypeParameters();
			if (typeParams == null || typeParams.length == 0) {
				typ = typ.getComponentType();
			} else
				break;
		}

		if (typeParams != null && typeParams.length > 0) {
			Generics genScope;
			trace("kryo", "Class " + clazz.getName() + " has generic type parameters");
			int typeVarNum = 0;
			Map<String, Class> typeVar2concreteClass;
			typeVar2concreteClass = new HashMap<String, Class>();
			for (TypeVariable typeVar : typeParams) {
				String typeVarName = typeVar.getName();
				if (TRACE) {
					trace("kryo", "Type parameter variable: name=" + typeVarName + " type bounds=" + Arrays.toString(typeVar.getBounds()));
				}

				final Class<?> concreteClass = getTypeVarConcreteClass(generics, typeVarNum, typeVarName);
				if (concreteClass != null) {
					typeVar2concreteClass.put(typeVarName, concreteClass);
					if (TRACE) trace("kryo", "Concrete type used for " + typeVarName + " is: " + concreteClass.getName());
				}

				typeVarNum++;
			}
			genScope = new Generics(typeVar2concreteClass);
			return genScope;
		} else
			return null;
	}

	private Class<?> getTypeVarConcreteClass (Class[] generics, int typeVarNum, String typeVarName) {
		if (generics != null && generics.length > typeVarNum) {
			// If passed concrete classes are known explicitly, use this information
			return generics[typeVarNum];
		} else {
			// Otherwise try to derive the information from the current GenericScope
			if (TRACE) trace("kryo", "Trying to use kryo.getGenericScope");
			Generics scope = kryo.getGenericsScope();
			if (scope != null) {
				return scope.getConcreteClass(typeVarName);
			}
		}
		return null;
	}

	Class[] computeFieldGenerics (Type fieldGenericType, Field field, Class[] fieldClass) {
		Class[] fieldGenerics = null;
		if (fieldGenericType != null) {
			if (fieldGenericType instanceof TypeVariable && serializer.getGenericsScope() != null) {
				TypeVariable typeVar = (TypeVariable)fieldGenericType;
				// Obtain information about a concrete type of a given variable from the environment
				Class concreteClass = serializer.getGenericsScope().getConcreteClass(typeVar.getName());
				if (concreteClass != null) {
					fieldClass[0] = concreteClass;
					fieldGenerics = new Class[] {fieldClass[0]};
					if (TRACE)
						trace("kryo", "Determined concrete class of '" + field.getName() + "' to be " + fieldClass[0].getName());
				}
			} else if (fieldGenericType instanceof ParameterizedType) {
				ParameterizedType parameterizedType = (ParameterizedType)fieldGenericType;
				// Get actual type arguments of the current field's type
				Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
				// if(actualTypeArguments != null && generics != null) {
				if (actualTypeArguments != null) {
					fieldGenerics = new Class[actualTypeArguments.length];
					for (int i = 0; i < actualTypeArguments.length; ++i) {
						Type t = actualTypeArguments[i];
						if (t instanceof Class)
							fieldGenerics[i] = (Class)t;
						else if (t instanceof ParameterizedType)
							fieldGenerics[i] = (Class)((ParameterizedType)t).getRawType();
						else if (t instanceof TypeVariable && serializer.getGenericsScope() != null)
							fieldGenerics[i] = serializer.getGenericsScope().getConcreteClass(((TypeVariable)t).getName());
						else if (t instanceof WildcardType)
							fieldGenerics[i] = Object.class;
						else if (t instanceof GenericArrayType) {
							Type componentType = ((GenericArrayType)t).getGenericComponentType();
							if (componentType instanceof Class)
								fieldGenerics[i] = Array.newInstance((Class)componentType, 0).getClass();
							else if (componentType instanceof TypeVariable) {
								Generics scope = serializer.getGenericsScope();
								if (scope != null) {
									Class clazz = scope.getConcreteClass(((TypeVariable)componentType).getName());
									if (clazz != null) {
										fieldGenerics[i] = Array.newInstance(clazz, 0).getClass();
									}
								}
							}
						} else
							fieldGenerics[i] = null;
					}
					if (TRACE && fieldGenerics != null) {
						trace("kryo", "Determined concrete class of parametrized '" + field.getName() + "' to be " + fieldGenericType + " where type parameters are " + Arrays.toString(fieldGenerics));
					}
				}
			} else if (fieldGenericType instanceof GenericArrayType) {
				// TODO: store generics for arrays as well?
				GenericArrayType arrayType = (GenericArrayType)fieldGenericType;
				Type genericComponentType = arrayType.getGenericComponentType();
				Class[] tmpFieldClass = new Class[] {fieldClass[0]};
				fieldGenerics = computeFieldGenerics(genericComponentType, field, tmpFieldClass);
				// Kryo.getGenerics(fieldGenericType);
				if (TRACE && fieldGenerics != null) {
					trace("kryo", "Determined concrete class of a generic array '" + field.getName() + "' to be " + fieldGenericType + " where type parameters are " + Arrays.toString(fieldGenerics));
				} else if (TRACE) trace("kryo", "Determined concrete class of '" + field.getName() + "' to be " + fieldGenericType);
			}
		}

		return fieldGenerics;
	}
	
	/** Special processing for fiels of generic types */
	CachedField newCachedFieldOfGenericType (Field field, int accessIndex, Class[] fieldClass, Type fieldGenericType) {
		Class[] fieldGenerics;
		CachedField cachedField;
		// This is a field with generic type parameters
		if (TRACE) {
			trace("kryo", "Field '" + field.getName() + "' of type " + fieldClass[0] + " of generic type " + fieldGenericType);
		}

		if (TRACE && fieldGenericType != null)
			trace("kryo", "Field generic type is of class " + fieldGenericType.getClass().getName());

		// Get set of provided type parameters

		// Get list of field specific concrete classes passed as generic parameters
		Class[] cachedFieldGenerics = FieldSerializerGenericsUtil.getGenerics(fieldGenericType, kryo);

		// Build a generics scope for this field
		Generics scope = buildGenericsScope(fieldClass[0], cachedFieldGenerics);

		// Is it a field of a generic parameter type, i.e. "T field"?
		if (fieldClass[0] == Object.class && fieldGenericType instanceof TypeVariable && serializer.getGenericsScope() != null) {
			TypeVariable typeVar = (TypeVariable)fieldGenericType;
			// Obtain information about a concrete type of a given variable from the environment
			Class concreteClass = serializer.getGenericsScope().getConcreteClass(typeVar.getName());
			if (concreteClass != null) {
				scope = new Generics();
				scope.add(typeVar.getName(), concreteClass);
			}
		}

		if (TRACE) {
			trace("kryo", "Generics scope of field '" + field.getName() + "' of class " + fieldGenericType + " is " + scope);
		}

		fieldGenerics = computeFieldGenerics(fieldGenericType, field, fieldClass);
		cachedField = serializer.newMatchingCachedField(field, accessIndex, fieldClass[0], fieldGenericType, fieldGenerics);

		if (fieldGenerics != null && cachedField instanceof ObjectField) {
			if (fieldGenerics.length > 0 && fieldGenerics[0] != null) {
				// If any information about concrete types for generic arguments of current field's type
				// was deriver, remember it.
				((ObjectField)cachedField).generics = fieldGenerics;
				if (TRACE) trace("kryo", "Field generics: " + Arrays.toString(fieldGenerics));
			}
		}
		return cachedField;
	}
	
	/** Returns the first level of classes or interfaces for a generic type.
	 * @return null if the specified type is not generic or its generic types are not classes. */
	public static Class[] getGenerics (Type genericType, Kryo kryo) {
		if (genericType instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType)genericType).getGenericComponentType();
			if (componentType instanceof Class)
				return new Class[] {(Class)componentType};
			else
				return getGenerics(componentType, kryo);
		}
		if (!(genericType instanceof ParameterizedType)) return null;
		if (TRACE) trace("kryo", "Processing generic type " + genericType);
		Type[] actualTypes = ((ParameterizedType)genericType).getActualTypeArguments();
		Class[] generics = new Class[actualTypes.length];
		int count = 0;
		for (int i = 0, n = actualTypes.length; i < n; i++) {
			Type actualType = actualTypes[i];
			if (TRACE) trace("kryo", "Processing actual type " + actualType + " (" + actualType.getClass().getName() + ")");
			generics[i] = Object.class;
			if (actualType instanceof Class)
				generics[i] = (Class)actualType;
			else if (actualType instanceof ParameterizedType)
				generics[i] = (Class)((ParameterizedType)actualType).getRawType();
			else if (actualType instanceof TypeVariable) {
				Generics scope = kryo.getGenericsScope();
				if (scope != null) {
					Class clazz = scope.getConcreteClass(((TypeVariable)actualType).getName());
					if (clazz != null) {
						generics[i] = clazz;
					} else
						continue;
				} else
					continue;
			} else if (actualType instanceof GenericArrayType) {
				Type componentType = ((GenericArrayType)actualType).getGenericComponentType();
				if (componentType instanceof Class)
					generics[i] = Array.newInstance((Class)componentType, 0).getClass();
				else if (componentType instanceof TypeVariable) {
					Generics scope = kryo.getGenericsScope();
					if (scope != null) {
						Class clazz = scope.getConcreteClass(((TypeVariable)componentType).getName());
						if (clazz != null) {
							generics[i] = Array.newInstance(clazz, 0).getClass();
						}
					}
				} else {
					Class[] componentGenerics = getGenerics(componentType, kryo);
					if (componentGenerics != null) generics[i] = componentGenerics[0];
				}
			} else
				continue;
			count++;
		}
		if (count == 0) return null;
		return generics;
	}
}
