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

import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.serializers.GenericsResolver.GenericsScope;

/** Utility methods used by FieldSerializer for generic type parameters.
 * @author Roman Levenstein <romixlev@gmail.com>
 * @author Nathan Sweet */
final class FieldSerializerGenerics {
	private FieldSerializer serializer;
	final TypeVariable[] typeParameters;
	final private Class componentType;

	public FieldSerializerGenerics (FieldSerializer serializer) {
		this.serializer = serializer;
		typeParameters = serializer.type.getTypeParameters();
		componentType = typeParameters.length == 0 ? serializer.type.getComponentType() : null;
	}

	/*** Create a mapping from type variable names (which are declared as type parameters of a generic class) to the concrete
	 * classes used for type instantiation.
	 * @param type class with generic type arguments
	 * @param generics concrete types used to instantiate the class
	 * @return May be null. */
	public GenericsScope newGenericsScope (Class type, Class[] generics) {
		TypeVariable[] typeParams = null;
		outer:
		while (type != null) {
			typeParams = type == serializer.type ? typeParameters : type.getTypeParameters();
			if (typeParams.length > 0) break;
			if (type != serializer.type)
				type = type.getComponentType();
			else {
				type = componentType;
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
				Type[] actualTypes = ((ParameterizedType)superClass).getActualTypeArguments();
				int n = actualTypes.length;
				generics = new Class[n];
				for (int i = 0; i < n; i++)
					generics[i] = actualTypes[i] instanceof Class ? (Class)actualTypes[i] : Object.class;
				break;
			}
		}
		if (typeParams.length == 0) return null;

		GenericsScope scope = new GenericsScope();
		for (int i = 0, n = typeParams.length; i < n; i++) {
			TypeVariable typeVar = typeParams[i];
			String typeVarName = typeVar.getName();
			if (TRACE) trace("kryo",
				"Type parameter variable: name=" + typeVarName + " type bounds=" + Arrays.toString(typeVar.getBounds()));

			Class concreteClass = null;
			if (generics != null && generics.length > i)
				concreteClass = generics[i]; // Concrete classes are known.
			else
				concreteClass = serializer.kryo.getGenericsResolver().getConcreteClass(typeVarName); // Look in scope.
			if (concreteClass != null) {
				scope.add(typeVarName, concreteClass);
				if (TRACE) trace("kryo", "Concrete type used for " + typeVarName + " is: " + concreteClass.getName());
			}
		}
		return scope;
	}

	/** Special processing for fields of generic types.
	 * @param scope */
	public void updateGenericCachedField (ReflectField reflectField, GenericsScope scope) {
		// This is a field with generic type parameters.
		Field field = reflectField.field;
		Type fieldGenericType = field.getGenericType();
		Class fieldClass = field.getType();
		if (TRACE)
			trace("kryo", "Field " + field.getName() + ": " + fieldClass + " <" + fieldGenericType.getClass().getName() + ">");

		Class[] c = {fieldClass}; // BOZO - Using an array is nasty!
		Class[] fieldGenerics = getGenericsWithScope(field, c, fieldGenericType, scope);
		if (fieldClass != c[0]) fieldClass = c[0];

		if (fieldGenerics != null)
			reflectField.generics = fieldGenerics;
		else if (fieldGenericType != null) {
			reflectField.generics = getGenericsWithGlobalScope(fieldGenericType);
			if (TRACE && reflectField.generics != null) trace("kryo", "Field generics: " + Arrays.toString(reflectField.generics));
		}

		if (fieldGenerics != null && fieldGenerics.length > 0 && fieldGenerics[0] != null) {
			// If any information about concrete types for generic arguments of current field's type was derived, remember it.
			reflectField.generics = fieldGenerics;
			if (TRACE) trace("kryo", "Field generics: " + Arrays.toString(fieldGenerics));
		} else
			reflectField.generics = null;

		reflectField.canBeNull = serializer.config.fieldsCanBeNull && !fieldClass.isPrimitive()
			&& !field.isAnnotationPresent(NotNull.class);

		// Always use the same serializer for this field if the field's class is final.
		reflectField.valueClass = serializer.kryo.isFinal(fieldClass) || serializer.config.fixedFieldTypes ? fieldClass : null;
	}

	/** Returns the parameterized types for a field that are known after compilation.
	 * @return May be null. */
	public Class[] getGenerics (Field field) {
		Type genericType = field.getGenericType();
		if (!(genericType instanceof ParameterizedType)) return null;
		Type[] actualTypes = ((ParameterizedType)genericType).getActualTypeArguments();
		int n = actualTypes.length, unknown = 0;
		Class[] generics = new Class[n];
		for (int i = 0; i < n; i++) {
			Type actualType = actualTypes[i];
			if (actualType instanceof Class)
				generics[i] = (Class)actualType;
			else if (actualType instanceof ParameterizedType)
				generics[i] = (Class)((ParameterizedType)actualType).getRawType();
			else if (actualType instanceof GenericArrayType) {
				Type componentType = ((GenericArrayType)actualType).getGenericComponentType();
				if (componentType instanceof Class) generics[i] = Array.newInstance((Class)componentType, 0).getClass();
			} else {
				generics[i] = Object.class;
				unknown++;
			}
		}
		return unknown == n ? null : generics;
	}

	/** @return May be null. */
	private Class[] getGenericsWithScope (Field field, Class[] fieldClass, Type fieldGenericType, GenericsScope scope) {
		Class[] generics = null;
		if (fieldGenericType instanceof TypeVariable && scope != null) {
			TypeVariable typeVar = (TypeVariable)fieldGenericType;
			Class concreteClass = scope.getConcreteClass(typeVar.getName());
			if (concreteClass != null) {
				fieldClass[0] = concreteClass;
				generics = new Class[] {fieldClass[0]};
				if (TRACE) trace("kryo", "Determined concrete class of '" + field.getName() + "' to be " + fieldClass[0].getName());
			}
		} else if (fieldGenericType instanceof ParameterizedType) {
			Type[] actualTypes = ((ParameterizedType)fieldGenericType).getActualTypeArguments();
			int n = actualTypes.length;
			generics = new Class[n];
			for (int i = 0; i < n; ++i) {
				Type type = actualTypes[i];
				if (type instanceof Class)
					generics[i] = (Class)type;
				else if (type instanceof ParameterizedType)
					generics[i] = (Class)((ParameterizedType)type).getRawType();
				else if (type instanceof TypeVariable && scope != null) {
					generics[i] = scope.getConcreteClass(((TypeVariable)type).getName());
					if (generics[i] == null) generics[i] = Object.class;
				} else if (type instanceof WildcardType)
					generics[i] = Object.class;
				else if (type instanceof GenericArrayType) {
					Type componentType = ((GenericArrayType)type).getGenericComponentType();
					if (componentType instanceof Class)
						generics[i] = Array.newInstance((Class)componentType, 0).getClass();
					else if (componentType instanceof TypeVariable && scope != null) {
						Class c = scope.getConcreteClass(((TypeVariable)componentType).getName());
						if (c != null) generics[i] = Array.newInstance(c, 0).getClass();
					}
				} else
					generics[i] = null;
			}
			if (TRACE && generics != null) {
				trace("kryo", "Determined concrete class of parametrized '" + field.getName() + "' to be " + fieldGenericType
					+ " where type parameters are " + Arrays.toString(generics));
			}
		} else if (fieldGenericType instanceof GenericArrayType) {
			GenericArrayType arrayType = (GenericArrayType)fieldGenericType;
			Type genericComponentType = arrayType.getGenericComponentType();
			Class[] tmpFieldClass = new Class[] {fieldClass[0]};
			generics = getGenericsWithScope(field, tmpFieldClass, genericComponentType, scope);
			// Kryo.getGenerics(fieldGenericType);
			if (TRACE && generics != null) {
				trace("kryo", "Determined concrete class of a generic array '" + field.getName() + "' to be " + fieldGenericType
					+ " where type parameters are " + Arrays.toString(generics));
			} else if (TRACE) trace("kryo", "Determined concrete class of '" + field.getName() + "' to be " + fieldGenericType);
		}
		return generics;
	}

	/** @return May be null. */
	private Class[] getGenericsWithGlobalScope (Type genericType) {
		if (genericType instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType)genericType).getGenericComponentType();
			if (componentType instanceof Class) return new Class[] {(Class)componentType};
			return getGenericsWithGlobalScope(componentType);
		}
		if (!(genericType instanceof ParameterizedType)) return null;
		Type[] actualTypes = ((ParameterizedType)genericType).getActualTypeArguments();
		int n = actualTypes.length, unknown = 0;
		Class[] generics = new Class[n];
		for (int i = 0; i < n; i++) {
			Type type = actualTypes[i];
			Class genericClass = null;
			if (type instanceof Class)
				genericClass = (Class)type;

			else if (type instanceof ParameterizedType)
				genericClass = (Class)((ParameterizedType)type).getRawType();

			else if (type instanceof TypeVariable) {
				GenericsResolver resolver = serializer.kryo.getGenericsResolver();
				Class c = resolver.getConcreteClass(((TypeVariable)type).getName());
				if (c != null) genericClass = c;

			} else if (type instanceof GenericArrayType) {
				Type componentType = ((GenericArrayType)type).getGenericComponentType();
				if (componentType instanceof Class)
					genericClass = Array.newInstance((Class)componentType, 0).getClass();
				else if (componentType instanceof TypeVariable) {
					GenericsResolver resolver = serializer.kryo.getGenericsResolver();
					Class c = resolver.getConcreteClass(((TypeVariable)componentType).getName());
					if (c != null) genericClass = Array.newInstance(c, 0).getClass();
				} else {
					Class[] componentGenerics = getGenericsWithGlobalScope(componentType);
					if (componentGenerics != null) genericClass = componentGenerics[0];
				}
			}
			if (genericClass == null) {
				genericClass = Object.class;
				unknown++;
			}
			generics[i] = genericClass;
		}
		return unknown == n ? null : generics;
	}
}
