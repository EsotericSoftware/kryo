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

package com.esotericsoftware.kryo.util;

import com.esotericsoftware.kryo.KryoException;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/** @author Nathan Sweet */
public class GenericsUtil {
	/** Returns the class for the specified type after replacing any type variables using the class hierarchy between the specified
	 * classes. A type variable that can't be resolved is returned, or a GenericArrayType for an array of a type variable passed to
	 * a super class, eg {@code T[]} for {@code Super<T[]>}.
	 * @param toClass Must be a sub class of fromClass. */
	public static Type resolveType (Class fromClass, Class toClass, Type type) {
		// Explicit type, eg String.
		if (type instanceof Class) return type;

		// Type variable, eg T.
		if (type instanceof TypeVariable) return resolveTypeVariable(fromClass, toClass, type, true);

		// Type which has a type parameter, eg ArrayList<T> or ArrayList<ArrayList<T>>.
		if (type instanceof ParameterizedType) return ((ParameterizedType)type).getRawType();

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
	 *         resolved, or a GenericArrayType for an array of a type variable passed to a super class. */
	private static Type resolveTypeVariable (Class fromClass, Class current, Type type, boolean first) {
		Type genericSuper = current.getGenericSuperclass();
		if (!(genericSuper instanceof ParameterizedType)) return type; // No type arguments passed to super class.

		// Search fromClass to current inclusive, using the call stack to traverse the class hierarchy in super class first order.
		Class superClass = current.getSuperclass();
		if (superClass != fromClass) {
			Type resolved = resolveTypeVariable(fromClass, superClass, type, false);
			if (resolved instanceof Class) return resolved; // Resolved in a super class.
			type = resolved;
		}

		// An array of a type variable that the super class passed to its super class, eg T[] for Super<T[]>.
		int dimensions = 0;
		Type variable = type;
		while (variable instanceof GenericArrayType) {
			variable = ((GenericArrayType)variable).getGenericComponentType();
			dimensions++;
		}

		// Match the type variable name to the super class parameter.
		String name = variable.toString(); // Java 8: getTypeName
		TypeVariable[] params = superClass.getTypeParameters();
		for (int i = 0, n = params.length; i < n; i++) {
			TypeVariable param = params[i];
			if (param.getName().equals(name)) {
				// Use the super class' type variable index to find the actual class in the sub class declaration.
				Type arg = ((ParameterizedType)genericSuper).getActualTypeArguments()[i];
				for (int d = 0; d < dimensions; d++)
					arg = new ArrayOfType(arg);

				// Success, the type variable was explicitly declared.
				if (arg instanceof Class) return arg;
				if (arg instanceof ParameterizedType) return resolveType(fromClass, current, arg);
				if (arg instanceof GenericArrayType) {
					// An array of a type variable of the sub class, eg T[] for Super<T[]>, is resolved by the next sub class or when
					// serializing. Resolving the type variable here would drop the dimensions, and recurse endlessly if it has
					// the name of the super class parameter.
					Type component = arg;
					while (component instanceof GenericArrayType)
						component = ((GenericArrayType)component).getGenericComponentType();
					if (component instanceof TypeVariable) return arg;
					return resolveType(fromClass, current, arg);
				}

				if (arg instanceof TypeVariable) {
					if (first) return type; // Failure, no more sub classes.
					return arg; // Look for the new type variable in the next sub class.
				}
			}
		}

		// We have exhausted looking through superclasses for a concrete generic type
		// definition, so we return the type variable instead.
		return type;
	}

	/** An array of a type argument, which keeps the dimensions of a type variable passed to a super class as an array, eg T[] for
	 * {@code Super<T[]>}, while the type variable is replaced by the type argument of a sub class. */
	static private final class ArrayOfType implements GenericArrayType {
		private final Type componentType;

		ArrayOfType (Type componentType) {
			this.componentType = componentType;
		}

		public Type getGenericComponentType () {
			return componentType;
		}

		public String toString () {
			return componentType + "[]";
		}
	}

	/** Resolves type variables for the type parameters of the specified type by using the class hierarchy between the specified
	 * classes.
	 * @param toClass Must be a sub class of fromClass.
	 * @return Null if the type has no type parameters, else contains Class entries for type parameters that were resolved and
	 *         TypeVariable entries for type parameters that couldn't be resolved. */
	public static Type[] resolveTypeParameters (Class fromClass, Class toClass, Type type) {
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

}
