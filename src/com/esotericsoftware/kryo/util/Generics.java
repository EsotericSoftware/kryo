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

import static com.esotericsoftware.kryo.util.Util.*;

import com.esotericsoftware.kryo.Kryo;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Handles storage of generic type information */
public interface Generics {

	/** Builds a {@link GenericsHierarchy} for the specified type. */
	GenericsHierarchy buildHierarchy (Class type);

	/** Sets the type that is currently being serialized. Must be balanced by {@link #popGenericType()}. Between those calls, the
	 * {@link GenericType#getTypeParameters() type parameters} are returned by {@link #nextGenericTypes()} and
	 * {@link #nextGenericClass()}. */
	void pushGenericType (GenericType fieldType);

	/** Removes the generic types being tracked since the corresponding {@link #pushGenericType(GenericType)}. This is safe to call
	 * even if {@link #pushGenericType(GenericType)} was not called. */
	void popGenericType ();

	/** Returns the current generic type, if it has type parameters, and {@link #pushGenericType(GenericType) pushes} the next
	 * level of type parameters for subsequent calls. Must be balanced by {@link #popGenericType()} (optional if null is returned).
	 * If the type has multiple type parameters, the last is used to advance to the next level of type parameters.
	 * @return May be null. */
	GenericType nextGenericType ();

	/** Returns the type parameters of {@link #nextGenericType()}. Must be balanced by {@link #popGenericType()} (optional if null
	 * is returned).
	 * <p>
	 * {@link #nextGenericClass()} is easier to use when a class has a single type parameter. When a class has multiple type
	 * parameters, {@link #pushGenericType(GenericType)} must be used for all except the last parameter.
	 * @return May be null. */
	GenericType[] nextGenericTypes ();

	/** Returns the type parameters of {@link #nextGenericType()} for the specified super class or interface of the declared type,
	 * eg the key and value types of {@link java.util.Map} if the declared type is {@code class IntMap<V> extends HashMap<Integer,
	 * V>}. A type parameter that is not known is Object. Otherwise like {@link #nextGenericTypes()}, but the last of the returned
	 * type parameters is used to advance to the next level. The default implementation returns {@link #nextGenericTypes()}.
	 * @return May be null. */
	default GenericType[] nextGenericTypes (Class superType) {
		return nextGenericTypes();
	}

	/** Resolves the first type parameter and returns the class, or null if it could not be resolved or there are no type
	 * parameters. Uses {@link #nextGenericTypes()}, so must be balanced by {@link #popGenericType()} (optional if null is
	 * returned).
	 * <p>
	 * This method is intended for ease of use when a class has a single type parameter.
	 * @return May be null. */
	Class nextGenericClass ();

	/** Stores the types of the type parameters for the specified class hierarchy, resolved from the type arguments of the declared
	 * type, eg the type of the field that holds the object. The declared type can be a super class or interface of the class of
	 * the hierarchy. Must be balanced by {@link #popTypeVariables(int)} if {@code > 0} is returned.
	 * @param type The declared type, as returned by {@link #nextGenericType()}.
	 * @return The number of entries that were pushed. */
	int pushTypeVariables (GenericsHierarchy hierarchy, GenericType type);

	/** Removes the number of entries that were pushed by {@link #pushTypeVariables(GenericsHierarchy, GenericType)}.
	 * @param count Must be even. */
	void popTypeVariables (int count);

	/** Returns the class for the specified type variable, or null if it is not known.
	 * @return May be null. */
	Class resolveTypeVariable (TypeVariable typeVariable);

	/** Returns the number of generic types currently tracked */
	int getGenericTypesSize ();

	/** Discards all tracked generic type information, returning to an empty state. This is called by {@link Kryo#reset()} so a
	 * reused Kryo instance is not left with stale generics if a serializer throws an exception before a balancing
	 * {@link #popGenericType()} or {@link #popTypeVariables(int)}. Implementations should be cheap when there is nothing to
	 * discard, since this is called after every serialization and deserialization (when auto-reset is enabled). The default
	 * implementation does nothing. */
	default void reset () {
	}

	/** Stores the type parameters for a class and, for parameters passed to super classes, the corresponding super class type
	 * parameters. */
	class GenericsHierarchy {
		static final GenericsHierarchy EMPTY = new GenericsHierarchy(0, 0, new int[0], new TypeVariable[0]);

		/* The class of the hierarchy, or null for EMPTY. */
		final Class type;
		/*
		 * The argument indices for the class itself, the most recently used other declared type and all other declared types, see
		 * argumentIndices.
		 */
		private final int[] identityIndices;
		private Class lastDeclared;
		private int[] lastIndices;
		private IdentityMap<Class, int[]> superTypeIndices;

		/* Total number of type parameters in the hierarchy. */
		final int total;
		/* Total number of type parameters at the root of the hierarchy. */
		final int rootTotal;
		final int[] counts;
		final TypeVariable[] parameters;

		public GenericsHierarchy (Class type) {
			IntArray counts = new IntArray();
			ArrayList<TypeVariable> parameters = new ArrayList();

			int total = 0;
			Class current = type;
			do {
				TypeVariable[] params = current.getTypeParameters();
				for (int i = 0, n = params.length; i < n; i++) {
					TypeVariable param = params[i];
					parameters.add(param);
					counts.add(1);

					// If the parameter is passed to a super class, also store the super class type variable, recursively.
					Class currentSuper = current;
					while (true) {
						Type genericSuper = currentSuper.getGenericSuperclass();
						currentSuper = currentSuper.getSuperclass();
						if (!(genericSuper instanceof ParameterizedType)) break;
						TypeVariable[] superParams = currentSuper.getTypeParameters();
						Type[] superArgs = ((ParameterizedType)genericSuper).getActualTypeArguments();
						for (int ii = 0, nn = superArgs.length; ii < nn; ii++) {
							Type superArg = superArgs[ii];
							if (superArg == param) {
								// We could skip if the super class doesn't use the type in a field.
								param = superParams[ii];
								parameters.add(param);
								counts.incr(counts.size - 1, 1);
							}
						}
					}

					total += counts.peek();
				}
				current = current.getSuperclass();
			} while (current != null);

			this.type = type;
			this.total = total;
			this.rootTotal = type.getTypeParameters().length;
			identityIndices = computeArgumentIndices(type);
			this.counts = counts.toArray();
			this.parameters = parameters.toArray(new TypeVariable[parameters.size()]);
		}

		GenericsHierarchy (int total, int rootTotal, int[] counts, TypeVariable[] parameters) {
			type = null;
			identityIndices = null;
			this.total = total;
			this.rootTotal = rootTotal;
			this.counts = counts;
			this.parameters = parameters;
		}

		/** Returns, for each type parameter of the class, the index of the type argument of the declared class it is passed to, or
		 * -1 if it is not passed to the declared class. For example, for {@code class Sub<A, B> extends Base<B, A>} and the
		 * declared class {@code Base}, this returns {@code [1, 0]}. */
		int[] argumentIndices (Class declared) {
			if (declared == type) return identityIndices; // Fast path.
			if (declared == lastDeclared) return lastIndices;
			int[] indices = superTypeIndices == null ? null : superTypeIndices.get(declared);
			if (indices == null) {
				indices = computeArgumentIndices(declared);
				// Usually there is only one other declared type, so the map is only needed for the second one.
				if (lastDeclared != null) {
					if (superTypeIndices == null) {
						superTypeIndices = new IdentityMap(4);
						superTypeIndices.put(lastDeclared, lastIndices);
					}
					superTypeIndices.put(declared, indices);
				}
			}
			lastDeclared = declared;
			lastIndices = indices;
			return indices;
		}

		private int[] computeArgumentIndices (Class declared) {
			List declaredArguments = Arrays.asList(superTypeArguments(type, declared));
			TypeVariable[] parameters = type.getTypeParameters();
			int[] indices = new int[parameters.length];
			for (int i = 0; i < parameters.length; i++)
				indices[i] = declaredArguments.indexOf(parameters[i]);
			return indices;
		}

		/** Returns the type arguments of the declared class in terms of the type parameters of the type. An argument is null if it
		 * is not known, eg for a raw super type. Returns an empty array if the declared class is not the type or a super type. */
		static private Type[] superTypeArguments (Class type, Class declared) {
			if (type == declared) {
				// A copy as Type[], since the caller replaces type variables with other types.
				TypeVariable[] parameters = declared.getTypeParameters();
				return Arrays.copyOf(parameters, parameters.length, Type[].class);
			}
			Type[] interfaces = type.getGenericInterfaces();
			for (int i = -1; i < interfaces.length; i++) {
				Type superType = i == -1 ? type.getGenericSuperclass() : interfaces[i];
				Type[] actual = null;
				if (superType instanceof ParameterizedType) {
					actual = ((ParameterizedType)superType).getActualTypeArguments();
					superType = ((ParameterizedType)superType).getRawType();
				}
				if (!(superType instanceof Class) || !declared.isAssignableFrom((Class)superType)) continue;
				// Replace the type parameters of the super type with the arguments the type passes to it.
				List superParameters = Arrays.asList(((Class)superType).getTypeParameters());
				Type[] arguments = superTypeArguments((Class)superType, declared);
				for (int ii = 0; ii < arguments.length; ii++) {
					if (!(arguments[ii] instanceof TypeVariable)) continue;
					// The type variable can belong to an enclosing class instead of the super type, then it is not known.
					int index = superParameters.indexOf(arguments[ii]);
					arguments[ii] = actual == null || index == -1 ? null : actual[index];
				}
				return arguments;
			}
			return new Type[0];
		}

		public String toString () {
			StringBuilder buffer = new StringBuilder();
			buffer.append("[");
			int[] counts = this.counts;
			TypeVariable[] parameters = this.parameters;
			for (int i = 0, p = 0, n = counts.length; i < n; i++) {
				int count = counts[i];
				for (int nn = p + count; p < nn; p++) {
					if (buffer.length() > 1) buffer.append(", ");
					GenericDeclaration declaration = parameters[p].getGenericDeclaration();
					if (declaration instanceof Class)
						buffer.append(((Class)declaration).getSimpleName());
					else
						buffer.append(declaration);
					buffer.append('<');
					buffer.append(parameters[p].getName());
					buffer.append('>');
				}
			}
			buffer.append("]");
			return buffer.toString();
		}
	}

	/** Stores a type and its type parameters, recursively. */
	class GenericType {
		/** A type parameter that is not known. */
		static final GenericType unknown = new GenericType(Object.class, Object.class, Object.class);

		Type type; // Either a Class or TypeVariable.
		GenericType[] arguments;
		private TypeVariable[] typeVariables;
		private SuperTypeArguments lastSuperType;

		/** A GenericType can be used by multiple Kryo instances and threads after it was created. */
		public GenericType (Class fromClass, Class toClass, Type context) {
			initialize(fromClass, toClass, context);
			cacheTypeVariables();
		}

		/** Caches the type parameters of the class, because {@link Class#getTypeParameters()} returns a copy. They are only needed
		 * for a class with type arguments. */
		private void cacheTypeVariables () {
			if (arguments != null && type instanceof Class) typeVariables = ((Class)type).getTypeParameters();
		}

		private void initialize (Class fromClass, Class toClass, Type context) {
			if (context instanceof ParameterizedType) {
				// Type with a type parameter, eg ArrayList<T>.
				ParameterizedType paramType = (ParameterizedType)context;
				Class rawType = (Class)paramType.getRawType();
				type = rawType;
				Type[] actualArgs = paramType.getActualTypeArguments();
				int n = actualArgs.length;
				// A non-generic inner class of a generic class, eg Outer<String>.Inner, has no type arguments of its own.
				if (n == 0) return;
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
		public Class resolve (Generics generics) {
			if (type instanceof Class) return (Class)type;
			return generics.resolveTypeVariable((TypeVariable)type);
		}

		public Type getType () {
			return type;
		}

		/** Returns the type arguments for the super class or interface, cached for the last super type. If this type is not a
		 * subtype, its own type arguments are returned.
		 * @return May be null. */
		GenericType[] superTypeArguments (Class superType) {
			// One immutable object for both values, so this is correct if multiple threads use this type.
			SuperTypeArguments last = lastSuperType;
			if (last == null || last.superType != superType)
				lastSuperType = last = new SuperTypeArguments(superType, computeSuperTypeArguments(superType));
			return last.arguments;
		}

		private GenericType[] computeSuperTypeArguments (Class superType) {
			if (arguments == null || !(type instanceof Class) || !superType.isAssignableFrom((Class)type)) return arguments;
			Type[] superArguments = GenericsHierarchy.superTypeArguments((Class)type, superType);
			TypeVariable[] parameters = typeVariables();
			// Usually the type parameters are passed to the super type unchanged, eg for HashMap<K, V> and Map<K, V>.
			if (Arrays.equals(superArguments, parameters) || superArguments.length == 0) return arguments;
			List parameterList = Arrays.asList(parameters);
			GenericType[] result = new GenericType[superArguments.length];
			for (int i = 0; i < superArguments.length; i++)
				result[i] = substitute(superArguments[i], parameterList);
			return result;
		}

		/** Returns the generic type for a type argument of the super type, with the type parameters of this type replaced by its
		 * type arguments, eg {@code List<Long>} for {@code List<V>} if this type is {@code ListMap<Long>}. */
		private GenericType substitute (Type argument, List parameterList) {
			int index = parameterList.indexOf(argument);
			if (index != -1) return index < arguments.length ? arguments[index] : unknown;
			if (argument instanceof Class) return new GenericType((Class)type, (Class)type, argument);
			if (argument instanceof GenericArrayType) { // Eg V[] or List<V>[].
				GenericType component = substitute(((GenericArrayType)argument).getGenericComponentType(), parameterList);
				if (!(component.type instanceof Class)) return unknown;
				return new GenericType((Class)type, (Class)type, Array.newInstance((Class)component.type, 0).getClass());
			}
			if (!(argument instanceof ParameterizedType)) return unknown; // Not known, eg a raw super type or a wildcard.
			ParameterizedType parameterized = (ParameterizedType)argument;
			GenericType result = new GenericType((Class)type, (Class)type, parameterized.getRawType());
			Type[] actual = parameterized.getActualTypeArguments();
			result.arguments = new GenericType[actual.length];
			for (int i = 0; i < actual.length; i++)
				result.arguments[i] = substitute(actual[i], parameterList);
			result.cacheTypeVariables();
			return result;
		}

		/** Returns the type parameters of the class, for a class with type arguments. */
		TypeVariable[] typeVariables () {
			return typeVariables;
		}

		/** @return May be null. */
		public GenericType[] getTypeParameters () {
			return arguments;
		}

		/** The type arguments for a super type. */
		static private final class SuperTypeArguments {
			final Class superType;
			final GenericType[] arguments;

			SuperTypeArguments (Class superType, GenericType[] arguments) {
				this.superType = superType;
				this.arguments = arguments;
			}
		}

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
}
