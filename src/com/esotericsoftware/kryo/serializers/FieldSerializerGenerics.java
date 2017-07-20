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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

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
	 * @param generics May be null.
	 * @return May be null. */
	public GenericsScope newGenericsScope (Class type, Class[] generics) {
		TypeVariable[] typeParameters = null;
		outer:
		while (type != null) {
			typeParameters = type == serializer.type ? this.typeParameters : type.getTypeParameters();
			if (typeParameters.length > 0) break;
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
				typeParameters = type.getTypeParameters();
				if (typeParameters.length == 0) return null;
				Type[] actualTypes = ((ParameterizedType)superClass).getActualTypeArguments();
				int n = actualTypes.length;
				generics = new Class[n];
				for (int i = 0; i < n; i++)
					generics[i] = actualTypes[i] instanceof Class ? (Class)actualTypes[i] : Object.class;
				break;
			}
		}

		GenericsScope scope = new GenericsScope();
		for (int i = 0, n = typeParameters.length; i < n; i++) {
			Class concreteClass = null;
			if (generics != null && generics.length > i)
				concreteClass = generics[i];
			else
				concreteClass = serializer.kryo.getGenericsResolver().getConcreteClass(typeParameters[i].getName());
			if (concreteClass != null) scope.add(typeParameters[i].getName(), concreteClass);
		}
		return scope;
	}

	/** Special processing for fields of generic types. */
	public void updateGenericCachedField (ReflectField reflectField, GenericsScope scope) {
		Field field = reflectField.field;

		// Try to determine the field class from the generics scope.
		Class fieldClass = field.getType();
		Type fieldGenericType = field.getGenericType();
		if (fieldGenericType instanceof TypeVariable) {
			Class c = scope.getConcreteClass(((TypeVariable)fieldGenericType).getName());
			if (c != null) fieldClass = c;
		}

		// Reset settings which are based on the field class to their default.
		reflectField.canBeNull = serializer.config.fieldsCanBeNull && !fieldClass.isPrimitive()
			&& !field.isAnnotationPresent(NotNull.class);
		reflectField.valueClass = serializer.kryo.isFinal(fieldClass) || serializer.config.fixedFieldTypes ? fieldClass : null;

		reflectField.generics = getGenericsWithGlobalScope(fieldGenericType);
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
				Class c = serializer.kryo.getGenericsResolver().getConcreteClass(((TypeVariable)type).getName());
				if (c != null) genericClass = c;

			} else if (type instanceof GenericArrayType) {
				Type componentType = ((GenericArrayType)type).getGenericComponentType();
				if (componentType instanceof Class)
					genericClass = Array.newInstance((Class)componentType, 0).getClass();
				else if (componentType instanceof TypeVariable) {
					Class c = serializer.kryo.getGenericsResolver().getConcreteClass(((TypeVariable)componentType).getName());
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
