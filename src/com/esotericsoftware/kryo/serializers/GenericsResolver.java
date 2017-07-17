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

package com.esotericsoftware.kryo.serializers;

import static com.esotericsoftware.kryo.util.Util.*;
import static com.esotericsoftware.minlog.Log.*;

import java.util.ArrayList;

/** Resolves a type name variable to a concrete class using the current class serialization stack
 * @author Jeroen van Erp <jeroen@hierynomus.com> */
public final class GenericsResolver {
	private final ArrayList<GenericsScope> stack = new ArrayList();

	Class getConcreteClass (String typeVar) {
		for (int i = 0, n = stack.size(); i < n; i++) {
			Class concreteClass = stack.get(i).getConcreteClass(typeVar);
			if (concreteClass != null) return concreteClass;
		}
		return null;
	}

	void pushScope (Class type, GenericsScope scope) {
		if (TRACE) trace("kryo", "New generics scope for class " + type.getName() + ": " + scope);
		stack.add(scope);
	}

	void popScope () {
		stack.remove(stack.size() - 1);
	}

	/** Maps type name variables to concrete classes that are used during instantiation.
	 * @author Roman Levenstein <romixlev@gmail.com> */
	static final class GenericsScope {
		private final ArrayList entries = new ArrayList(4);

		public void add (String typeVar, Class concreteClass) {
			entries.add(typeVar);
			entries.add(concreteClass);
		}

		/** @return May be null. */
		public Class getConcreteClass (String typeVar) {
			for (int i = 0, n = entries.size(); i < n; i += 2)
				if (typeVar.equals(entries.get(i))) return (Class)entries.get(i + 1);
			return null;
		}

		public String toString () {
			StringBuilder buffer = new StringBuilder();
			for (int i = 0, n = entries.size(); i < n; i += 2) {
				if (i > 0) buffer.append(", ");
				buffer.append(entries.get(i));
				buffer.append("=");
				buffer.append(className((Class)entries.get(i + 1)));
			}
			return buffer.toString();
		}
	}
}
