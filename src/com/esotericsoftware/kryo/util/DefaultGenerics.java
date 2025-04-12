/* Copyright (c) 2008-2025, Nathan Sweet
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

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/** Stores the generic type arguments and actual classes for type variables in the current location in the object graph.
 * @author Nathan Sweet */
public final class DefaultGenerics implements Generics {
	private final Kryo kryo;

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
	public GenericType[] nextGenericTypes () {
		int index = genericTypesSize;
		if (index > 0) {
			index--;
			GenericType genericType = genericTypes[index];
			if (genericType.arguments == null) return null;
			// The depth must match to prevent the types being wrong if a serializer doesn't call nextGenericTypes.
			if (depths[index] == kryo.getDepth() - 1) {
				pushGenericType(genericType.arguments[genericType.arguments.length - 1]);
				return genericType.arguments;
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
	public int pushTypeVariables (GenericsHierarchy hierarchy, GenericType[] args) {
		// Do not store type variables if hierarchy is empty, or we do not have arguments for all root parameters, or we have more
		// arguments than the hierarchy has parameters.
		if (hierarchy.total == 0 || hierarchy.rootTotal > args.length || args.length > hierarchy.counts.length) return 0;

		int startSize = this.argumentsSize;

		// Ensure arguments capacity.
		int sizeNeeded = startSize + hierarchy.total;
		if (sizeNeeded > arguments.length) {
			Type[] newArray = new Type[Math.max(sizeNeeded, arguments.length << 1)];
			System.arraycopy(arguments, 0, newArray, 0, startSize);
			arguments = newArray;
		}

		// Resolve and store the type arguments.
		int[] counts = hierarchy.counts;
		TypeVariable[] params = hierarchy.parameters;
		for (int i = 0, p = 0, n = args.length; i < n; i++) {
			GenericType arg = args[i];
			Class resolved = arg.resolve(this);
			if (resolved == null) continue;
			int count = counts[i];
			if (arg == null)
				p += count;
			else {
				for (int nn = p + count; p < nn; p++) {
					arguments[argumentsSize] = params[p];
					arguments[argumentsSize + 1] = resolved;
					argumentsSize += 2;
				}
			}
		}

		return argumentsSize - startSize;
	}

	@Override
	public void popTypeVariables (int count) {
		int n = argumentsSize, i = n - count;
		argumentsSize = i;
		while (i < n)
			arguments[i++] = null;
	}

	@Override
	public Class resolveTypeVariable (TypeVariable typeVariable) {
		for (int i = argumentsSize - 2; i >= 0; i -= 2) {
			final Type arg = arguments[i];
			if (arg == typeVariable || arg.equals(typeVariable)) return (Class)arguments[i + 1];
		}
		return null;
	}

	@Override
	public int getGenericTypesSize () {
		return genericTypesSize;
	}

	public String toString () {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < argumentsSize; i += 2) {
			if (i != 0) buffer.append(", ");
			buffer.append(((TypeVariable)arguments[i]).getName());
			buffer.append("=");
			buffer.append(((Class)arguments[i + 1]).getSimpleName());
		}
		return buffer.toString();
	}

}
