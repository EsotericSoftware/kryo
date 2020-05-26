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

package com.esotericsoftware.kryo;

import static com.esotericsoftware.kryo.util.Util.getDimensionCount;
import static com.esotericsoftware.kryo.util.Util.getElementClass;

import com.esotericsoftware.kryo.util.GenericsStrategy;
import com.esotericsoftware.kryo.util.GenericsUtil;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/** Stores a type and its type parameters, recursively. */
public class GenericType {
	Type type; // Either a Class or TypeVariable.
	GenericType[] arguments;

	public GenericType (Class fromClass, Class toClass, Type context) {
		initialize(fromClass, toClass, context);
	}

	private void initialize (Class fromClass, Class toClass, Type context) {
		if (context instanceof ParameterizedType) {
			// Type with a type parameter, eg ArrayList<T>.
			ParameterizedType paramType = (ParameterizedType)context;
			Class rawType = (Class)paramType.getRawType();
			type = rawType;
			Type[] actualArgs = paramType.getActualTypeArguments();
			int n = actualArgs.length;
			arguments = new GenericType[n];
			for (int i = 0; i < n; i++)
				arguments[i] = new GenericType(fromClass, toClass, actualArgs[i]);

		} else if (context instanceof GenericArrayType) {
			// Array with a type parameter, eg "ArrayList<T>[]". Discard array types, resolve type parameter of component type.
			int dimensions = 1;
			while (true) {
				context = ((GenericArrayType)context).getGenericComponentType();
				if (!(context instanceof GenericArrayType)) break;
				dimensions++;
			}
			initialize(fromClass, toClass, context);
			Type componentType = GenericsUtil.resolveType(fromClass, toClass, context);
			if (componentType instanceof Class) {
				if (dimensions == 1)
					type = Array.newInstance((Class)componentType, 0).getClass();
				else
					type = Array.newInstance((Class)componentType, new int[dimensions]).getClass();
			}

		} else {
			// No type parameters (is a class or type variable).
			type = GenericsUtil.resolveType(fromClass, toClass, context);
		}
	}

	/** If this type is a type variable, resolve it to a class.
	 * @return May be null. */
	public Class resolve (GenericsStrategy generics) {
		if (type instanceof Class) return (Class)type;
		return generics.resolveTypeVariable((TypeVariable)type);
	}

	public Type getType () {
		return type;
	}

	/** @return May be null. */
	public GenericType[] getTypeParameters () {
		return arguments;
	}

	@Override
	public String toString () {
		StringBuilder buffer = new StringBuilder(32);
		boolean array = false;
		if (type instanceof Class) {
			Class c = (Class)type;
			array = c.isArray();
			buffer.append((array ? getElementClass(c) : c).getSimpleName());
			if (arguments != null) {
				buffer.append('<');
				for (int i = 0, n = arguments.length; i < n; i++) {
					if (i > 0) buffer.append(", ");
					buffer.append(arguments[i].toString());
				}
				buffer.append('>');
			}
		} else
			buffer.append(type.toString()); // Java 8: getTypeName
		if (array) {
			for (int i = 0, n = getDimensionCount((Class)type); i < n; i++)
				buffer.append("[]");
		}
		return buffer.toString();
	}
}
