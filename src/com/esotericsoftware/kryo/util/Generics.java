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
		/* The argument indices if the declared type is the class of the hierarchy, see argumentIndices. */
		private final int[] identityIndices;
		/* The most recently used declared type that is a super type of the class, and its argument indices. */
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
			identityIndices = new int[rootTotal];
			for (int i = 0; i < rootTotal; i++)
				identityIndices[i] = i;
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
		 * -1 if it is not passed to the declared class. Returns null if the declared class is not the class or one of its super
		 * types. For example, for {@code class Sub<A, B> extends Base<B, A>} and the declared class {@code Base}, this returns
		 * {@code [1, 0]}.
		 * @param declaredType The declared class, other types return null. */
		int[] argumentIndices (Type declaredType) {
			if (declaredType == type) return identityIndices; // Fast path.
			if (declaredType == lastDeclared) return lastIndices;
			if (type == null || !(declaredType instanceof Class)) return null;
			Class declared = (Class)declaredType;
			if (superTypeIndices == null) superTypeIndices = new IdentityMap();
			int[] indices = superTypeIndices.get(declared);
			if (indices == null && !superTypeIndices.containsKey(declared)) {
				indices = computeArgumentIndices(declared);
				superTypeIndices.put(declared, indices);
			}
			lastDeclared = declared;
			lastIndices = indices;
			return indices;
		}

		private int[] computeArgumentIndices (Class declared) {
			Type[] declaredArguments = superTypeArguments(type, declared);
			if (declaredArguments == null) return null;
			TypeVariable[] typeParameters = type.getTypeParameters();
			int[] indices = new int[typeParameters.length];
			outer:
			for (int i = 0; i < typeParameters.length; i++) {
				for (int ii = 0; ii < declaredArguments.length; ii++) {
					if (declaredArguments[ii] == typeParameters[i]) {
						indices[i] = ii;
						continue outer;
					}
				}
				indices[i] = -1;
			}
			return indices;
		}

		/** Returns the type arguments of the declared class in terms of the type parameters of the type, or null if the declared
		 * class is not the type or one of its super types. An argument is null if it is not known, eg for a raw super type. */
		static private Type[] superTypeArguments (Class type, Class declared) {
			if (type == declared) {
				// A copy as Type[], since the caller replaces type variables with other types.
				TypeVariable[] parameters = declared.getTypeParameters();
				return Arrays.copyOf(parameters, parameters.length, Type[].class);
			}
			Type[] interfaces = type.getGenericInterfaces();
			for (int i = -1; i < interfaces.length; i++) {
				Type superType = i == -1 ? type.getGenericSuperclass() : interfaces[i];
				Class superClass;
				if (superType instanceof ParameterizedType)
					superClass = (Class)((ParameterizedType)superType).getRawType();
				else if (superType instanceof Class)
					superClass = (Class)superType;
				else
					continue;
				if (!declared.isAssignableFrom(superClass)) continue;
				Type[] arguments = superTypeArguments(superClass, declared);
				if (arguments == null) continue;
				// Replace the type parameters of the super type with the arguments the type passes to it.
				TypeVariable[] superParameters = superClass.getTypeParameters();
				Type[] actual = superType instanceof ParameterizedType ? ((ParameterizedType)superType).getActualTypeArguments()
					: null;
				for (int ii = 0; ii < arguments.length; ii++) {
					if (!(arguments[ii] instanceof TypeVariable)) continue;
					Type replacement = null;
					if (actual != null) {
						for (int iii = 0; iii < superParameters.length; iii++) {
							if (superParameters[iii] == arguments[ii]) {
								replacement = actual[iii];
								break;
							}
						}
					}
					arguments[ii] = replacement;
				}
				return arguments;
			}
			return null;
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

		/** @return May be null. */
		public GenericType[] getTypeParameters () {
			return arguments;
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
