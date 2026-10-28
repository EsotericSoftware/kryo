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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Kryo.Format;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/** Stores the generic type arguments and actual classes for type variables in the current location in the object graph.
 * @author Nathan Sweet */
public final class DefaultGenerics implements Generics {
	private final Kryo kryo;
	private Format format;

	private int genericTypesSize;
	private GenericType[] genericTypes = new GenericType[16];
	private int[] depths = new int[16];

	private int argumentsSize;
	private Type[] arguments = new Type[16];

	public DefaultGenerics (Kryo kryo) {
		this.kryo = kryo;
	}

	@Override
	public GenericsHierarchy buildHierarchy (Class type) {
		// Type variables are only stored for the type parameters of the class itself.
		if (type.getTypeParameters().length == 0 && !kryo5()) return GenericsHierarchy.EMPTY;
		return new GenericsHierarchy(type);
	}

	@Override
	public void pushGenericType (GenericType fieldType) {
		// Ensure genericTypes and depths capacity.
		int size = genericTypesSize;
		if (size + 1 == genericTypes.length) {
			GenericType[] genericTypesNew = new GenericType[genericTypes.length << 1];
			System.arraycopy(genericTypes, 0, genericTypesNew, 0, size);
			genericTypes = genericTypesNew;
			int[] depthsNew = new int[depths.length << 1];
			System.arraycopy(depths, 0, depthsNew, 0, size);
			depths = depthsNew;
		}

		genericTypesSize = size + 1;
		genericTypes[size] = fieldType;
		depths[size] = kryo.getDepth();
	}

	@Override
	public void popGenericType () {
		int size = genericTypesSize;
		if (size == 0) return;
		size--;
		if (depths[size] < kryo.getDepth()) return;
		genericTypes[size] = null;
		genericTypesSize = size;
	}

	@Override
	public GenericType nextGenericType () {
		int index = genericTypesSize;
		if (index > 0) {
			index--;
			GenericType genericType = genericTypes[index];
			if (genericType.arguments == null) return null;
			// The depth must match to prevent the types being wrong if a serializer doesn't call nextGenericTypes.
			if (depths[index] == kryo.getDepth() - 1) {
				pushGenericType(genericType.arguments[genericType.arguments.length - 1]);
				return genericType;
			}
		}
		return null;
	}

	@Override
	public GenericType[] nextGenericTypes () {
		GenericType genericType = nextGenericType();
		return genericType == null ? null : genericType.arguments;
	}

	@Override
	public GenericType[] nextGenericTypes (Class superType) {
		if (kryo5()) return nextGenericTypes(); // Kryo 5 used the type arguments by position.
		int index = genericTypesSize;
		if (index > 0) {
			index--;
			GenericType genericType = genericTypes[index];
			if (genericType.arguments == null) return null;
			// The depth must match to prevent the types being wrong if a serializer doesn't call nextGenericTypes.
			if (depths[index] == kryo.getDepth() - 1) {
				GenericType[] arguments = genericType.superTypeArguments(superType);
				pushGenericType(arguments[arguments.length - 1]);
				return arguments;
			}
		}
		return null;
	}

	@Override
	public Class nextGenericClass () {
		GenericType[] arguments = nextGenericTypes();
		if (arguments == null) return null;
		return arguments[0].resolve(this);
	}

	@Override
	public int pushTypeVariables (GenericsHierarchy hierarchy, GenericType type) {
		if (kryo5()) return pushTypeVariablesByPosition(hierarchy, type);
		int startSize = argumentsSize;
		if (type == null) {
			// The type arguments are unknown. The type variables hide those of an enclosing object of the same class, if any.
			if (startSize == 0) return 0;
			TypeVariable[] params = hierarchy.parameters;
			ensureCapacity(params.length);
			for (TypeVariable param : params) {
				arguments[argumentsSize++] = param;
				arguments[argumentsSize++] = null;
			}
			return argumentsSize - startSize;
		}
		Class declared = (Class)type.type; // Always a class, because the type has type arguments.
		GenericType[] args = type.arguments;

		// The type parameters of the class, which come first in the hierarchy. The arguments are for the declared type, which can
		// be a super type of the class with different type parameters. An unknown argument is stored as null, so the type variable
		// doesn't resolve to that of an enclosing object of the same class.
		boolean resolvedAny = false;
		if (hierarchy.rootTotal > 0) {
			int[] indices = hierarchy.argumentIndices(declared);
			ensureCapacity(hierarchy.total);
			int[] counts = hierarchy.counts;
			TypeVariable[] params = hierarchy.parameters;
			for (int i = 0, p = 0, n = indices.length; i < n; i++) {
				int index = indices[i];
				Class resolved = index == -1 ? null : resolve(args[index], startSize);
				if (resolved != null) resolvedAny = true;
				for (int nn = p + counts[i]; p < nn; p++) {
					arguments[argumentsSize++] = params[p];
					arguments[argumentsSize++] = resolved;
				}
			}
		}

		// If no type parameters of the class are passed to the declared super class, eg because the class extends it as a raw type,
		// use the arguments for the type parameters of the declared class.
		if (!resolvedAny && declared != hierarchy.type && !declared.isInterface()) {
			TypeVariable[] params = type.typeVariables();
			ensureCapacity(params.length);
			for (int i = 0; i < params.length; i++) {
				Class resolved = resolve(args[i], startSize);
				if (resolved == null) continue;
				arguments[argumentsSize++] = params[i];
				arguments[argumentsSize++] = resolved;
			}
		}

		return argumentsSize - startSize;
	}

	/** Assigns the type arguments of the declared type by position to the type parameters of the class, like Kryo 5, and doesn't
	 * hide the type variables of an enclosing object of the same class if the declared type is not known. */
	private int pushTypeVariablesByPosition (GenericsHierarchy hierarchy, GenericType type) {
		if (type == null) return 0;
		GenericType[] args = type.arguments;
		if (hierarchy.total == 0 || hierarchy.rootTotal > args.length || args.length > hierarchy.counts.length) return 0;
		int startSize = argumentsSize;
		ensureCapacity(hierarchy.total);
		int[] counts = hierarchy.counts;
		TypeVariable[] params = hierarchy.parameters;
		for (int i = 0, p = 0, n = args.length; i < n; i++) {
			Class resolved = args[i].resolve(this);
			if (resolved == null) continue; // Like Kryo 5, the next argument is assigned to these type parameters.
			for (int nn = p + counts[i]; p < nn; p++) {
				arguments[argumentsSize++] = params[p];
				arguments[argumentsSize++] = resolved;
			}
		}
		return argumentsSize - startSize;
	}

	/** Returns true if type variables are resolved like Kryo 5. The format can't change once it was read. */
	private boolean kryo5 () {
		Format format = this.format;
		if (format == null) this.format = format = kryo.getFormat();
		return format == Format.KRYO5;
	}

	/** Ensures there is room for the specified number of type variables and their classes. */
	private void ensureCapacity (int count) {
		int sizeNeeded = argumentsSize + count * 2;
		if (sizeNeeded > arguments.length) {
			Type[] newArray = new Type[Math.max(sizeNeeded, arguments.length << 1)];
			System.arraycopy(arguments, 0, newArray, 0, argumentsSize);
			arguments = newArray;
		}
	}

	@Override
	public void popTypeVariables (int count) {
		int n = argumentsSize, i = n - count;
		argumentsSize = i;
		while (i < n)
			arguments[i++] = null;
	}

	/** Resolves a type argument of the declared type against the entries below the ones being pushed: the declared type can use
	 * the type variables of the class itself, eg a field {@code Pair<Y, X>} in {@code Pair<X, Y>}, which must resolve to the
	 * entries of the enclosing object. */
	private Class resolve (GenericType argument, int size) {
		if (argument.type instanceof Class) return (Class)argument.type;
		Class resolved = resolveTypeVariable((TypeVariable)argument.type, size);
		for (int i = 0; i < argument.dimensions && resolved != null; i++)
			resolved = Util.getArrayType(resolved);
		return resolved;
	}

	@Override
	public Class resolveTypeVariable (TypeVariable typeVariable) {
		return resolveTypeVariable(typeVariable, argumentsSize);
	}

	private Class resolveTypeVariable (TypeVariable typeVariable, int size) {
		for (int i = size - 2; i >= 0; i -= 2) {
			final Type arg = arguments[i];
			if (arg == typeVariable || arg.equals(typeVariable)) return (Class)arguments[i + 1];
		}
		return null;
	}

	@Override
	public int getGenericTypesSize () {
		return genericTypesSize;
	}

	@Override
	public void reset () {
		// Fast path: after a balanced (successful) serialization both stacks are already empty.
		if (genericTypesSize == 0 && argumentsSize == 0) return;
		// Slow path: a serializer threw before popping. Discard the leaked entries so the next serialization starts clean.
		for (int i = 0; i < genericTypesSize; i++)
			genericTypes[i] = null;
		genericTypesSize = 0;
		for (int i = 0; i < argumentsSize; i++)
			arguments[i] = null;
		argumentsSize = 0;
	}

	public String toString () {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < argumentsSize; i += 2) {
			if (i != 0) buffer.append(", ");
			buffer.append(((TypeVariable)arguments[i]).getName());
			buffer.append("=");
			buffer.append(arguments[i + 1] == null ? "?" : ((Class)arguments[i + 1]).getSimpleName());
		}
		return buffer.toString();
	}

}
