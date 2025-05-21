/* Copyright (c) 2008-2022, Nathan Sweet
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

/** Base class for implementations of {@link Generics} that stores generic type information.
 * @author Nathan Sweet */
abstract class BaseGenerics implements Generics {
	final Kryo kryo;

	private int genericTypesSize;
	private GenericType[] genericTypes = new GenericType[16];
	private int[] depths = new int[16];

	protected BaseGenerics (Kryo kryo) {
		this.kryo = kryo;
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
	public int getGenericTypesSize () {
		return genericTypesSize;
	}

}
