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

import java.lang.reflect.Constructor;

/** Creates objects without calling a constructor, so the fields have their default values. On the JDK, the serialization
 * constructor of {@code sun.reflect.ReflectionFactory} is used, which calls only {@link Object}'s constructor, like Java
 * serialization does for the non-serializable part of an object and like Objenesis does on HotSpot. Where it is not available,
 * the instance is allocated with Unsafe. Otherwise, eg on Android, <a href="http://objenesis.org/">Objenesis</a> is used, which
 * is an optional dependency of Kryo that the versioned jar includes: add {@code org.objenesis:objenesis} with the default jar.
 * <p>
 * Most classes expect their constructors to be called, so creating objects this way may leave them in an invalid state. Usually
 * it is used as the fallback of a {@link DefaultInstantiatorStrategy}, for classes without a no-arg constructor. */
public class StdInstantiatorStrategy implements InstantiatorStrategy {
	static private final Constructor objectConstructor;
	static {
		try {
			objectConstructor = Object.class.getDeclaredConstructor();
		} catch (NoSuchMethodException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}

	private InstantiatorStrategy objenesis;

	public <T> ObjectInstantiator<T> newInstantiatorOf (Class<T> type) {
		if (Instantiators.reflectionFactory()) {
			ObjectInstantiator<T> instantiator = Instantiators.serializationConstructor(type, objectConstructor);
			if (instantiator != null) return instantiator;
		}
		if (unsafe) return Instantiators.unsafeAllocation(type);
		if (objenesis == null) objenesis = Instantiators.objenesis("StdInstantiatorStrategy", false);
		return objenesis.newInstantiatorOf(type);
	}
}
