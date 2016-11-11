/* Copyright (c) 2008, Nathan Sweet
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

package com.esotericsoftware.kryo.serializers;

import static com.esotericsoftware.minlog.Log.*;

import java.util.LinkedList;

/** INTERNAL API
 *
 * Helper class that resolves a type name variable to a concrete class using the current class serialization stack
 *
 * @author Jeroen van Erp <jeroen@hierynomus.com> */
public final class GenericsResolver {
	private LinkedList<Generics> stack = new LinkedList<Generics>();

	public GenericsResolver () {
	}

	Class getConcreteClass (String typeVar) {
		for (Generics generics : stack) {
			Class clazz = generics.getConcreteClass(typeVar);
			if (clazz != null) return clazz;
		}
		return null;
	}

	boolean isSet () {
		return !stack.isEmpty();
	}

	void pushScope (Class type, Generics scope) {
		if (TRACE) trace("generics", "Settting a new generics scope for class " + type.getName() + ": " + scope);
		stack.addFirst(scope);
	}

	void popScope () {
		stack.removeFirst();
	}
}
