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

import static com.esotericsoftware.kryo.util.Log.*;
import static com.esotericsoftware.kryo.util.Util.*;

import com.esotericsoftware.kryo.KryoException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Creates objects without calling their constructors, for {@link StdInstantiatorStrategy} and
 * {@link SerializingInstantiatorStrategy}: with the serialization constructors of the JDK's
 * {@code sun.reflect.ReflectionFactory}, like Java serialization and Objenesis on HotSpot, else with Unsafe, else with
 * Objenesis. */
final class Instantiators {
	/** {@code sun.reflect.ReflectionFactory}, accessed with reflection, so there is no compile time dependency and no class
	 * loading where it doesn't exist, eg on Android. */
	static private final Object reflectionFactory;
	static private final Method newConstructorForSerialization;
	static {
		Object factory = null;
		Method method = null;
		try {
			Class factoryClass = Class.forName("sun.reflect.ReflectionFactory");
			factory = factoryClass.getMethod("getReflectionFactory").invoke(null);
			method = factoryClass.getMethod("newConstructorForSerialization", Class.class, Constructor.class);
		} catch (Throwable ex) {
			if (DEBUG) debug("kryo", "ReflectionFactory is not available, objects are created without a constructor with Unsafe or "
				+ "Objenesis.", ex);
		}
		reflectionFactory = factory;
		newConstructorForSerialization = method;
	}

	static boolean reflectionFactory () {
		return newConstructorForSerialization != null;
	}

	/** Returns an instantiator that calls a serialization constructor: the type's fields have their default values and the
	 * specified constructor of a super class is called. Returns null if the constructor can't be created, eg in a native image
	 * without serialization metadata for the type. */
	static <T> ObjectInstantiator<T> serializationConstructor (Class<T> type, Constructor superConstructor) {
		Constructor<T> constructor;
		try {
			constructor = (Constructor<T>)newConstructorForSerialization.invoke(reflectionFactory, type, superConstructor);
			constructor.setAccessible(true);
		} catch (Throwable ex) {
			if (DEBUG) debug("kryo", "Unable to create the serialization constructor: " + className(type), ex);
			return null;
		}
		// Only Constructor#newInstance calls the serialization constructor. A method handle for it would resolve the constructor
		// of the type with the same signature, which runs the type's constructor, if it exists.
		return () -> {
			try {
				return constructor.newInstance();
			} catch (InvocationTargetException ex) {
				if (ex.getCause() instanceof Error) throw (Error)ex.getCause();
				throw error(type, ex.getCause());
			} catch (Exception ex) {
				throw error(type, ex);
			}
		};
	}

	/** Returns an instantiator that allocates the instance with Unsafe, without calling any constructor. */
	static <T> ObjectInstantiator<T> unsafeAllocation (Class<T> type) {
		return () -> {
			try {
				return (T)UnsafeAllocation.allocate(type);
			} catch (InstantiationException ex) {
				throw error(type, ex);
			}
		};
	}

	/** Loaded only if Unsafe is available. */
	static private final class UnsafeAllocation {
		static Object allocate (Class type) throws InstantiationException {
			return com.esotericsoftware.kryo.unsafe.UnsafeUtil.unsafe.allocateInstance(type);
		}
	}

	/** Returns the Objenesis strategy, which is an optional dependency of Kryo.
	 * @throws KryoException if Objenesis is not on the classpath. */
	static InstantiatorStrategy objenesis (String strategy, boolean serializing) {
		try {
			return serializing ? ObjenesisStrategy.serializing() : ObjenesisStrategy.std();
		} catch (NoClassDefFoundError ex) {
			throw new KryoException(strategy + " needs Objenesis on this platform, which is an optional dependency of Kryo: add "
				+ "org.objenesis:objenesis to the classpath.", ex);
		}
	}

	static KryoException error (Class type, Throwable cause) {
		return new KryoException("Error constructing instance of class: " + className(type), cause);
	}
}
