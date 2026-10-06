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

import com.esotericsoftware.kryo.KryoException;

import java.io.Serializable;
import java.lang.reflect.Constructor;

/** Creates objects like Java serialization does: the class must implement {@link Serializable} and the no-arg constructor of its
 * first non-serializable super class is called, the other constructors are not. On the JDK, the serialization constructor of
 * {@code sun.reflect.ReflectionFactory} is used. Otherwise, eg on Android, <a href="http://objenesis.org/">Objenesis</a> is used,
 * which is an optional dependency of Kryo that the versioned jar includes: add {@code org.objenesis:objenesis} with the default
 * jar. */
public class SerializingInstantiatorStrategy implements InstantiatorStrategy {
	private InstantiatorStrategy objenesis;

	public <T> ObjectInstantiator<T> newInstantiatorOf (Class<T> type) {
		if (!Serializable.class.isAssignableFrom(type))
			throw new KryoException(
				"Class is not Serializable, SerializingInstantiatorStrategy can't create it: " + className(type));
		if (Instantiators.reflectionFactory()) {
			Class superclass = type;
			while (Serializable.class.isAssignableFrom(superclass))
				superclass = superclass.getSuperclass();
			Constructor superConstructor;
			try {
				superConstructor = superclass.getDeclaredConstructor();
			} catch (NoSuchMethodException ex) {
				throw new KryoException("The first non-serializable super class has no no-arg constructor: " + className(superclass)
					+ " (" + className(type) + ")", ex);
			}
			ObjectInstantiator<T> instantiator = Instantiators.serializationConstructor(type, superConstructor);
			if (instantiator != null) return instantiator;
		}
		if (objenesis == null) objenesis = Instantiators.objenesis("SerializingInstantiatorStrategy", true);
		return objenesis.newInstantiatorOf(type);
	}
}
