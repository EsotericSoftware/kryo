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

package com.esotericsoftware.kryo.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;

import com.esotericsoftware.kryo.KryoException;

/** @author Nathan Sweet */
public class GenericsUtil {
	/** Returns the class for the specified type after replacing any type variables using the class hierarchy between the specified
	 * classes.
	 * @param toClass Must be a sub class of fromClass. */
	static public Type resolveType (Class fromClass, Class toClass, Type type) {
		// Explicit type, eg String.
		if (type instanceof Class) return (Class)type;

		// Type variable, eg T.
		if (type instanceof TypeVariable) return resolveTypeVariable(fromClass, toClass, type, true);

		// Type which has a type parameter, eg ArrayList<T> or ArrayList<ArrayList<T>>.
		if (type instanceof ParameterizedType) return (Class)((ParameterizedType)type).getRawType();

		// Array which has a type variable, eg T[] or T[][], and also arrays with a type parameter, eg ArrayList<T>[].
		if (type instanceof GenericArrayType) {
			int dimensions = 1;
			while (true) {
				type = ((GenericArrayType)type).getGenericComponentType();
				if (!(type instanceof GenericArrayType)) break;
				dimensions++;
			}
			Type componentType = resolveType(fromClass, toClass, type);
			if (!(componentType instanceof Class)) return type;
			if (dimensions == 1) return Array.newInstance((Class)componentType, 0).getClass();
			return Array.newInstance((Class)componentType, new int[dimensions]).getClass();
		}

		// Type which has a wildcard, eg ArrayList<?>, ArrayList<? extends Number>, or ArrayList<? super Integer>.
		if (type instanceof WildcardType) {
			// Check upper bound first, it is more likely. There is always an upper bound.
			Type upperBound = ((WildcardType)type).getUpperBounds()[0];
			if (upperBound != Object.class) return resolveType(fromClass, toClass, upperBound);
			Type[] lowerBounds = ((WildcardType)type).getLowerBounds();
			if (lowerBounds.length != 0) return resolveType(fromClass, toClass, lowerBounds[0]);
			return Object.class;
		}

		// If this happens, there is a case we need to handle.
		throw new KryoException("Unable to resolve type: " + type);
	}

	/** Returns the class for the specified type variable by finding the first class in the hierarchy between the specified classes
	 * which explicitly specifies the type variable's class.
	 * @param current Must be a sub class of fromClass.
	 * @return A Class if the type variable was resolved, else a TypeVariable to continue searching or if the type could not be
	 *         resolved. */
	static private Type resolveTypeVariable (Class fromClass, Class current, Type type, boolean first) {
		// Search fromClass to current inclusive, using the call stack to traverse the class hierarchy in super class first order.
		Class superClass = current.getSuperclass();
		TypeVariable[] params = superClass.getTypeParameters();
		if (params.length == 0) return type; // The super class has no type parameters.
		if (superClass != fromClass) {
			if (superClass == null) return type;
			Type resolved = resolveTypeVariable(fromClass, superClass, type, false);
			if (resolved instanceof Class) return (Class)resolved; // Resolved in a super class.
			type = resolved; // resolveTypeVariable never returns null when reentrant.
		}

		String name = type.getTypeName();
		for (int i = 0, n = params.length; i < n; i++) {
			TypeVariable param = params[i];
			if (param.getName().equals(name)) {
				// Use the super class type variable index to find the actual class in the sub class declaration.
				Type genericSuper = current.getGenericSuperclass();
				if (genericSuper instanceof ParameterizedType) {
					Type arg = ((ParameterizedType)genericSuper).getActualTypeArguments()[i];

					// Success, the type variable was explicitly declared.
					if (arg instanceof Class) return (Class)arg;
					if (arg instanceof ParameterizedType) return resolveType(fromClass, current, arg);

					if (arg instanceof TypeVariable) {
						if (first) return type; // Failure, no more sub classes.
						return arg; // Look for the new type variable in the next sub class.
					}
					break;
				}
			}
		}

		// If this happens, there is a case we need to handle.
		throw new KryoException("Unable to resolve type variable: " + type);
	}

	/** Resolves type variables for the type parameters of the specified type by using the class hierarchy between the specified
	 * classes.
	 * @param toClass Must be a sub class of fromClass.
	 * @return Null if the type has no type parameters, else contains Class entries for type parameters that were resolved and
	 *         TypeVariable entries for type parameters that couldn't be resolved. */
	static public Type[] resolveTypeParameters (Class fromClass, Class toClass, Type type) {
		// Type which has a type parameter, eg ArrayList<T>.
		if (type instanceof ParameterizedType) {
			Type[] actualArgs = ((ParameterizedType)type).getActualTypeArguments();
			int n = actualArgs.length;
			Type[] generics = new Type[n];
			for (int i = 0; i < n; i++)
				generics[i] = resolveType(fromClass, toClass, actualArgs[i]);
			return generics;
		}

		// Array which has a type parameter, eg "ArrayList<T>[]". Discard array types, resolve type parameter of the component type.
		if (type instanceof GenericArrayType) {
			while (true) {
				type = ((GenericArrayType)type).getGenericComponentType();
				if (!(type instanceof GenericArrayType)) break;
			}
			return resolveTypeParameters(fromClass, toClass, type);
		}

		return null; // No type parameters (is a class or type variable).
	}

	static public class GenericType {
		Type type; // Class or TypeVariable.
		GenericType[] children;

		public GenericType (Class fromClass, Class toClass, Type context) {
			initialize(fromClass, toClass, context);
		}

		private void initialize (Class fromClass, Class toClass, Type context) {
			if (context instanceof ParameterizedType) {
				// Type with a type parameter, eg ArrayList<T>.
				ParameterizedType paramType = (ParameterizedType)context;
				Class rawType = (Class)paramType.getRawType();
				type = rawType;
				TypeVariable[] params = rawType.getTypeParameters();
				Type[] args = paramType.getActualTypeArguments();
				int n = args.length;
				children = new GenericType[n];
				for (int i = 0; i < n; i++)
					children[i] = new GenericType(fromClass, toClass, args[i]);

			} else if (context instanceof GenericArrayType) {
				// Array with a type parameter, eg "ArrayList<T>[]". Discard array types, resolve type parameter of component type.
				int dimensions = 1;
				while (true) {
					context = ((GenericArrayType)context).getGenericComponentType();
					if (!(context instanceof GenericArrayType)) break;
					dimensions++;
				}
				initialize(fromClass, toClass, context);
				Type componentType = resolveType(fromClass, toClass, context);
				if (componentType instanceof Class) {
					if (dimensions == 1)
						type = Array.newInstance((Class)componentType, 0).getClass();
					else
						type = Array.newInstance((Class)componentType, new int[dimensions]).getClass();
				}

			} else {
				// No type parameters (is a class or type variable).
				type = resolveType(fromClass, toClass, context);
			}
		}

		/** Use the scope to resolve this type variable.
		 * @return May be null. */
		public Class resolve (GenericsScope scope) {
			if (type instanceof Class) return (Class)type;
			return scope.resolveTypeVariable((TypeVariable)type);
		}

		public Type getType () {
			return type;
		}

		/** @return May be null. */
		public GenericType[] getChildren () {
			return children;
		}

		public String toString () {
			StringBuilder buffer = new StringBuilder();
			toString(buffer, 1);
			buffer.setLength(buffer.length() - 1);
			return buffer.toString();
		}

		private void toString (StringBuilder buffer, int indent) {
			buffer.append(type.getTypeName());
			buffer.append('\n');

			if (children == null) return;
			for (int i = 0, n = children.length; i < n; i++) {
				for (int ii = 0; ii < indent; ii++)
					buffer.append("|  ");
				children[i].toString(buffer, indent + 1);
			}
		}
	}

	static public class SuperTest<T> {
		public HashMap<String, ArrayList<String>> map;
		public ArrayList<ArrayList<ArrayList<String>>> list;
		public ArrayList<ArrayList<ArrayList<T>>> listWithParam;
		public ArrayList<T[]> listWithArrayParam;
		public ArrayList<T[][]> listWithArrayArrayParam;
		public String string;
		public String[] stringArray;
		public T t;
		public T[] tArray;
		public T[][] tArrayArray;
	}

	static public class Test extends SuperTest<Integer> {
	}

	static public void main (String[] args) throws Exception {
// ResolvedGenerics r = new ResolvedGenerics();
// r.add(Test.class, Test.class, Test.class.getField("list").getGenericType());
// System.out.println(r.scope);

		GenericType g = new GenericType(SuperTest.class, Test.class,
			SuperTest.class.getField("listWithArrayArrayParam").getGenericType());
		System.out.println(g);
	}
}
