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

package com.esotericsoftware.kryo;

import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.trace;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

/*** Helper class to map type name variables to concrete classes that are used during instantiation
 * 
 * @author Roman Levenstein <romixlev@gmail.com> */
public class Generics {
	private Map<String, Class> typeVar2class;

	private Generics parentScope;

	public Generics () {
		typeVar2class = new HashMap<String, Class>();
		parentScope = null;
	}

	public Generics (Map<String, Class> mappings) {
		typeVar2class = new HashMap<String, Class>(mappings);
		parentScope = null;
	}

	public Generics (Generics parentScope) {
		typeVar2class = new HashMap<String, Class>();
		this.parentScope = parentScope;
	}

	public void add (String typeVar, Class clazz) {
		typeVar2class.put(typeVar, clazz);
	}

	public Class getConcreteClass (String typeVar) {
		Class clazz = typeVar2class.get(typeVar);
		if (clazz == null && parentScope != null) return parentScope.getConcreteClass(typeVar);
		return clazz;
	}

	public void setParentScope (Generics scope) {
		if (parentScope != null) throw new IllegalStateException("Parent scope can be set just once");
		parentScope = scope;
	}

	public Generics getParentScope () {
		return parentScope;
	}
	
	public Map<String, Class> getMappings() {
		return typeVar2class;
	}

	public String toString () {
		return typeVar2class.toString();
	}

	public void resetParentScope () {
		parentScope = null;
	}
}
