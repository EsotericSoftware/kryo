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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

/** Stores type variables and their actual types for the current location in the object graph.
 * @author Nathan Sweet */
public class GenericsScope {
	private int size;
	private Type[] arguments = new Type[16];

	/** Stores the actual classes for the specified type parameters.
	 * @param args May contain null for type arguments that aren't known.
	 * @return The number of entries that were pushed. */
	public int push (GenericsHierarchy hierarchy, Class[] args) {
		int startSize = this.size;

		ensureCapacity(hierarchy.total);
		int[] counts = hierarchy.counts;
		TypeVariable[] params = hierarchy.params;
		for (int i = 0, p = 0, n = args.length; i < n; i++) {
			Class arg = args[i];
			int count = counts[i];
			if (arg == null)
				p += count;
			else {
				for (int nn = p + count; p < nn; p++) {
					arguments[size] = params[p];
					arguments[size + 1] = arg;
					size += 2;
				}
			}
		}

		return size - startSize;
	}

	private void ensureCapacity (int additionalCapacity) {
		int sizeNeeded = size + additionalCapacity;
		if (sizeNeeded <= arguments.length) return;
		Type[] newArray = new Type[Math.max(sizeNeeded, arguments.length << 1)];
		System.arraycopy(arguments, 0, newArray, 0, size);
		arguments = newArray;
	}

	/** Removes the number of entries that were pushed.
	 * @param count Must be even. */
	public void pop (int count) {
		int n = size, i = n - count;
		size = i;
		while (i < n)
			arguments[i++] = null;
	}

	/** Returns the actual class for the specified type variable, or null if it is not known.
	 * @return May be null. */
	public Class resolveTypeVariable (TypeVariable typeVariable) {
		for (int i = size - 2; i >= 0; i -= 2) {
			if (arguments[i] == typeVariable) return (Class)arguments[i + 1];
		}
		return null;
	}

	public boolean isEmpty () {
		return size == 0;
	}

	public String toString () {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < size; i += 2) {
			if (i != 0) buffer.append(", ");
			buffer.append(((TypeVariable)arguments[i]).getName());
			buffer.append("=");
			buffer.append(((Class)arguments[i + 1]).getSimpleName());
		}
		return buffer.toString();
	}

	/** Stores the type parameters for a class and, for parameters passed to super classes, the corresponding super class type
	 * parameters. */
	static public class GenericsHierarchy {
		final int total;
		final int[] counts;
		final TypeVariable[] params;

		public GenericsHierarchy (Class type) {
			ArrayList<TypeVariable> temp = new ArrayList();

			TypeVariable[] params = type.getTypeParameters();
			counts = new int[params.length];
			int total = 0;
			for (int i = 0, n = params.length; i < n; i++) {
				TypeVariable param = params[i];
				temp.add(param);
				counts[i] = 1;

				// If the parameter is passed to a super class, also store the super class type variable, recursively.
				Class current = type;
				while (true) {
					Type genericSuper = current.getGenericSuperclass();
					current = current.getSuperclass();
					if (!(genericSuper instanceof ParameterizedType)) break;
					TypeVariable[] superParams = current.getTypeParameters();
					Type[] superArgs = ((ParameterizedType)genericSuper).getActualTypeArguments();
					for (int ii = 0, nn = superArgs.length; ii < nn; ii++) {
						Type superArg = superArgs[ii];
						if (superArg == param) {
							param = superParams[ii];
							temp.add(param);
							counts[i]++;
						}
					}
				}

				total += counts[i];
			}

			this.total = total;
			this.params = temp.toArray(new TypeVariable[temp.size()]);
		}
	}

	/** Stores the partially resolved generic types for a field. */
	static public class Generics {
		final Type[] types; // Entries are either Class or TypeVariable.
		final Class[] resolved;

		private Generics (Type[] types) {
			this.types = types;
			resolved = new Class[types.length];
		}

		/** Use the scope to resolve type variables.
		 * @return May be null or contain null. */
		public Class[] resolve (GenericsScope scope) {
			if (scope.isEmpty()) {
				for (int i = 0, n = types.length; i < n; i++) {
					Type type = types[i];
					resolved[i] = type instanceof Class ? (Class)type : null;
				}
			} else {
				for (int i = 0, n = types.length; i < n; i++) {
					Type type = types[i];
					resolved[i] = type instanceof Class ? (Class)type : scope.resolveTypeVariable((TypeVariable)type);
				}
			}
			for (int i = 0, n = resolved.length; i < n; i++)
				if (resolved[i] != null) return resolved;
			return null;
		}

		public void setGenerics (Kryo kryo, Serializer serializer) {
			Class[] resolved = resolve(kryo.getGenericsScope());
			if (resolved != null) serializer.setGenerics(kryo, resolved);
		}

		public Type[] getTypes () {
			return types;
		}

		public String toString () {
			StringBuilder buffer = new StringBuilder();
			for (int i = 0, n = types.length; i < n; i++) {
				if (i != 0) buffer.append(", ");
				Type type = types[i];
				if (type instanceof Class)
					buffer.append(((Class)type).getSimpleName());
				else
					buffer.append(types[i].getTypeName());
			}
			return buffer.toString();
		}

		/** @return May be null if the type has no type parameters. */
		static public Generics create (Class fromClass, Class toClass, Type type) {
			Type[] types = GenericsUtil.resolveTypeParameters(fromClass, toClass, type);
			if (types == null) return null;
			for (int i = 0, n = types.length; i < n; i++)
				if (types[i] != Object.class) return new Generics(types);
			return null;
		}
	}
}
