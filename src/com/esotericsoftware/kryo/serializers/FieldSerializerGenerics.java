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

import static com.esotericsoftware.minlog.Log.*;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.serializers.GenericsResolver.GenericsScope;

/** Utility methods used by FieldSerializer for generic type parameters.
 * @author Roman Levenstein <romixlev@gmail.com>
 * @author Nathan Sweet */
final class FieldSerializerGenerics {
	private FieldSerializer serializer;

	public FieldSerializerGenerics (FieldSerializer serializer) {
		this.serializer = serializer;
	}

	/** Returns the parameterized types for a field that are known at compile time, or null if the field is not parameterized. */
	Class[] getGenericsWithoutScope (Field field) {
		Type genericType = field.getGenericType();
		if (!(genericType instanceof ParameterizedType)) return null;
		Type[] actualTypes = ((ParameterizedType)genericType).getActualTypeArguments();
		int n = actualTypes.length;
		Class[] genericTypes = new Class[n];
		for (int i = 0; i < n; i++) {
			Type actualType = actualTypes[i];
			if (actualType instanceof Class)
				genericTypes[i] = (Class)actualType;
			else if (actualType instanceof ParameterizedType)
				genericTypes[i] = (Class)((ParameterizedType)actualType).getRawType();
			else if (actualType instanceof GenericArrayType) {
				Type componentType = ((GenericArrayType)actualType).getGenericComponentType();
				if (componentType instanceof Class) genericTypes[i] = Array.newInstance((Class)componentType, 0).getClass();
			} else
				genericTypes[i] = Object.class;
		}
		return genericTypes;
	}

	/*** Create a mapping from type variable names (which are declared as type parameters of a generic class) to the concrete
	 * classes used for type instantiation.
	 * @param clazz class with generic type arguments
	 * @param genericTypes concrete types used to instantiate the class
	 * @return new scope for type parameters */
	GenericsScope newGenericsScope (Class clazz, Class[] genericTypes) {
		TypeVariable[] typeParams = null;
		Class type = clazz;
		outer:
		while (type != null) {
			typeParams = type == serializer.type ? serializer.typeParameters : type.getTypeParameters();
			if (typeParams.length > 0) break;
			if (type != serializer.type)
				type = type.getComponentType();
			else {
				type = serializer.componentType;
				if (type != null) continue;
				// This is not a generic type. Check if a super class is generic.
				type = serializer.type;
				Type superClass = null;
				do {
					superClass = type.getGenericSuperclass();
					type = type.getSuperclass();
					if (superClass == null) break outer;
				} while (!(superClass instanceof ParameterizedType));
				typeParams = type.getTypeParameters();
				Type[] typeArgs = ((ParameterizedType)superClass).getActualTypeArguments();
				genericTypes = new Class[typeArgs.length];
				for (int i = 0; i < typeArgs.length; i++)
					genericTypes[i] = typeArgs[i] instanceof Class ? (Class)typeArgs[i] : Object.class;
				break;
			}
		}
		if (typeParams.length == 0) return null;

		if (TRACE) trace("kryo", "Class " + clazz.getName() + " has generic type parameters");
		int typeVarNum = 0;
		GenericsScope scope = new GenericsScope();
		for (TypeVariable typeVar : typeParams) {
			String typeVarName = typeVar.getName();
			if (TRACE) trace("kryo",
				"Type parameter variable: name=" + typeVarName + " type bounds=" + Arrays.toString(typeVar.getBounds()));

			Class concreteClass = getTypeVarConcreteClass(genericTypes, typeVarNum, typeVarName);
			if (concreteClass != null) {
				scope.add(typeVarName, concreteClass);
				if (TRACE) trace("kryo", "Concrete type used for " + typeVarName + " is: " + concreteClass.getName());
			}

			typeVarNum++;
		}
		return scope;
	}

	private Class getTypeVarConcreteClass (Class[] genericTypes, int typeVarNum, String typeVarName) {
		if (genericTypes != null && genericTypes.length > typeVarNum) {
			// If passed concrete classes are known explicitly, use this information
			return genericTypes[typeVarNum];
		}
		// Otherwise try to derive the information from the current GenericScope
		if (TRACE) trace("kryo", "Trying to use kryo.genericsResolver");
		return serializer.getKryo().getGenericsResolver().getConcreteClass(typeVarName);
	}

	Class[] computeFieldGenerics (Type fieldGenericType, Field field, Class[] fieldClass) {
		Class[] fieldGenerics = null;
		if (fieldGenericType != null) {
			if (fieldGenericType instanceof TypeVariable && serializer.cachedFields.genericsScope != null) {
				TypeVariable typeVar = (TypeVariable)fieldGenericType;
				// Obtain information about a concrete type of a given variable from the environment
				Class concreteClass = serializer.cachedFields.genericsScope.getConcreteClass(typeVar.getName());
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
						else if (t instanceof TypeVariable && serializer.cachedFields.genericsScope != null) {
							fieldGenerics[i] = serializer.cachedFields.genericsScope.getConcreteClass(((TypeVariable)t).getName());
							if (fieldGenerics[i] == null) fieldGenerics[i] = Object.class;
						} else if (t instanceof WildcardType)
							fieldGenerics[i] = Object.class;
						else if (t instanceof GenericArrayType) {
							Type componentType = ((GenericArrayType)t).getGenericComponentType();
							if (componentType instanceof Class)
								fieldGenerics[i] = Array.newInstance((Class)componentType, 0).getClass();
							else if (componentType instanceof TypeVariable) {
								GenericsScope scope = serializer.cachedFields.genericsScope;
								if (scope != null) {
									Class clazz = scope.getConcreteClass(((TypeVariable)componentType).getName());
									if (clazz != null) fieldGenerics[i] = Array.newInstance(clazz, 0).getClass();
								}
							}
						} else
							fieldGenerics[i] = null;
					}
					if (TRACE && fieldGenerics != null) {
						trace("kryo", "Determined concrete class of parametrized '" + field.getName() + "' to be " + fieldGenericType
							+ " where type parameters are " + Arrays.toString(fieldGenerics));
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
					trace("kryo", "Determined concrete class of a generic array '" + field.getName() + "' to be " + fieldGenericType
						+ " where type parameters are " + Arrays.toString(fieldGenerics));
				} else if (TRACE) trace("kryo", "Determined concrete class of '" + field.getName() + "' to be " + fieldGenericType);
			}
		}

		return fieldGenerics;
	}

	/** Special processing for fields of generic types. */
	void updateGenericCachedField (ReflectField reflectField) {
		// This is a field with generic type parameters.
		Field field = reflectField.field;
		Type fieldGenericType = field.getGenericType();
		Class fieldClass = field.getType();
		if (TRACE)
			trace("kryo", "Field " + field.getName() + ": " + fieldClass + " <" + fieldGenericType.getClass().getName() + ">");

		// Get list of field specific concrete classes passed as generic parameters
		Class[] cachedFieldGenerics = getGenerics(fieldGenericType, serializer.getKryo());

		GenericsScope scope = newGenericsScope(fieldClass, cachedFieldGenerics);

		// Is it a field of a generic parameter type, i.e. "T field"?
		if (fieldClass == Object.class && fieldGenericType instanceof TypeVariable
			&& serializer.cachedFields.genericsScope != null) {
			TypeVariable typeVar = (TypeVariable)fieldGenericType;
			// Obtain information about a concrete type of a given variable from the environment
			Class concreteClass = serializer.cachedFields.genericsScope.getConcreteClass(typeVar.getName());
			if (concreteClass != null) {
				scope = new GenericsScope();
				scope.add(typeVar.getName(), concreteClass);
			}
		}

		if (TRACE) trace("kryo", "Generics scope of field '" + reflectField + "' of class " + fieldGenericType + " is " + scope);

		Class[] c = {fieldClass}; // BOZO - Using an array is nasty!
		Class[] fieldGenerics = computeFieldGenerics(fieldGenericType, field, c);
		if (fieldClass != c[0]) fieldClass = c[0];

		if (fieldGenerics != null)
			reflectField.genericTypes = fieldGenerics;
		else if (fieldGenericType != null) {
			reflectField.genericTypes = cachedFieldGenerics;
			if (TRACE) trace("kryo", "Field generics: " + Arrays.toString(cachedFieldGenerics));
		}

		if (fieldGenerics != null && fieldGenerics.length > 0 && fieldGenerics[0] != null) {
			// If any information about concrete types for generic arguments of current field's type was derived, remember it.
			reflectField.genericTypes = fieldGenerics;
			if (TRACE) trace("kryo", "Field generics: " + Arrays.toString(fieldGenerics));
		} else
			reflectField.genericTypes = null;

		reflectField.canBeNull = serializer.config.fieldsCanBeNull && !fieldClass.isPrimitive()
			&& !field.isAnnotationPresent(NotNull.class);

		// Always use the same serializer for this field if the field's class is final.
		reflectField.valueClass = serializer.getKryo().isFinal(fieldClass) || serializer.config.fixedFieldTypes ? fieldClass : null;
	}

	/** Returns the first level of classes or interfaces for a generic type.
	 * @return null if the specified type is not generic or its generic types are not classes. */
	static public Class[] getGenerics (Type genericType, Kryo kryo) {
		if (genericType instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType)genericType).getGenericComponentType();
			if (componentType instanceof Class) return new Class[] {(Class)componentType};
			return getGenerics(componentType, kryo);
		}
		if (!(genericType instanceof ParameterizedType)) return null;
		Type[] actualTypes = ((ParameterizedType)genericType).getActualTypeArguments();
		Class[] genericTypes = new Class[actualTypes.length];
		int count = 0;
		for (int i = 0, n = actualTypes.length; i < n; i++) {
			Type actualType = actualTypes[i];

			genericTypes[i] = Object.class;
			if (actualType instanceof Class)
				genericTypes[i] = (Class)actualType;

			else if (actualType instanceof ParameterizedType)
				genericTypes[i] = (Class)((ParameterizedType)actualType).getRawType();

			else if (actualType instanceof TypeVariable) {
				GenericsResolver resolver = kryo.getGenericsResolver();
				Class clazz = resolver.getConcreteClass(((TypeVariable)actualType).getName());
				if (clazz == null) continue;
				genericTypes[i] = clazz;

			} else if (actualType instanceof GenericArrayType) {
				Type componentType = ((GenericArrayType)actualType).getGenericComponentType();
				if (componentType instanceof Class)
					genericTypes[i] = Array.newInstance((Class)componentType, 0).getClass();
				else if (componentType instanceof TypeVariable) {
					GenericsResolver resolver = kryo.getGenericsResolver();
					Class clazz = resolver.getConcreteClass(((TypeVariable)componentType).getName());
					if (clazz != null) genericTypes[i] = Array.newInstance(clazz, 0).getClass();
				} else {
					Class[] componentGenerics = getGenerics(componentType, kryo);
					if (componentGenerics != null) genericTypes[i] = componentGenerics[0];
				}

			} else
				continue;
			count++;
		}
		if (count == 0) return null;
		return genericTypes;
	}
}
