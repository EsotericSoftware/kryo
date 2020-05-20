package com.esotericsoftware.kryo.util;

import com.esotericsoftware.kryo.util.DefaultGenericsStrategy.GenericType;
import com.esotericsoftware.kryo.util.DefaultGenericsStrategy.GenericsHierarchy;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

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
/** Provides an interface on how generics are handled. */
public interface GenericsStrategy {
	/** Sets the type that is currently being serialized. Must be balanced by {@link #popGenericType()}. Between those calls, the
	 * {@link GenericType#getTypeParameters() type parameters} are returned by {@link #nextGenericTypes()} and	
	 * {@link #nextGenericClass()}. */
	public void pushGenericType (GenericType fieldType);
	
	/** Removes the generic types being tracked since the corresponding {@link #pushGenericType(GenericType)}. This is safe to call
	 * even if {@link #pushGenericType(GenericType)} was not called. */
	public void popGenericType ();
	
	/** Returns the current type parameters and {@link #pushGenericType(GenericType) pushes} the next level of type parameters for
	 * subsquent calls. Must be balanced by {@link #popGenericType()} (optional if null is returned). If multiple type parameters
	 * are returned, the last is used to advance to the next level of type parameters.
	 * <p>
	 * {@link #nextGenericClass()} is easier to use when a class has a single type parameter. When a class has multiple type
	 * parameters, {@link #pushGenericType(GenericType)} must be used for all except the last parameter.
	 * @return May be null. */
	public GenericType[] nextGenericTypes ();
	
	/** Resolves the first type parameter and returns the class, or null if it could not be resolved or there are no type
	 * parameters. Uses {@link #nextGenericTypes()}, so must be balanced by {@link #popGenericType()} (optional if null is
	 * returned).
	 * <p>
	 * This method is intended for ease of use when a class has a single type parameter.
	 * @return May be null. */
	public Class nextGenericClass ();
	
	/** Stores the types of the type parameters for the specified class hierarchy. Must be balanced by
	 * {@link #popTypeVariables(int)} if >0 is returned.
	 * @param args May contain null for type arguments that aren't known.
	 * @return The number of entries that were pushed. */
	public int pushTypeVariables (GenericsHierarchy hierarchy, GenericType[] args);
	

	/** Removes the number of entries that were pushed by {@link #pushTypeVariables(GenericsHierarchy, GenericType[])}.
	 * @param count Must be even. */
	public void popTypeVariables (int count);
	
	/** Returns the class for the specified type variable, or null if it is not known.
	 * @return May be null. */
	public Class resolveTypeVariable (TypeVariable typeVariable);
}
