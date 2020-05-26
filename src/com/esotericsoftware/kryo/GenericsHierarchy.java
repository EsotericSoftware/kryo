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

import com.esotericsoftware.kryo.util.IntArray;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;

/** Stores the type parameters for a class and, for parameters passed to super classes, the corresponding super class type
 * parameters. */
public class GenericsHierarchy {
	final int total;
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

		this.total = total;
		this.counts = counts.toArray();
		this.parameters = parameters.toArray(new TypeVariable[parameters.size()]);
	}

	@Override
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

	public boolean isEmpty () {
		return total == 0;
	}
}
