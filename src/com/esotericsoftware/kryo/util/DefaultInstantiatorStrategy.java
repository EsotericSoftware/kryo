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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/** Creates objects with their no-arg constructor, using a method handle, or reflection on Android and in a native image. If a
 * class has no no-arg constructor or it can't be accessed, the fallback strategy is used, if any, eg a
 * {@link StdInstantiatorStrategy}. */
public class DefaultInstantiatorStrategy implements InstantiatorStrategy {
	private InstantiatorStrategy fallbackStrategy;

	public DefaultInstantiatorStrategy () {
	}

	public DefaultInstantiatorStrategy (InstantiatorStrategy fallbackStrategy) {
		this.fallbackStrategy = fallbackStrategy;
	}

	public void setFallbackInstantiatorStrategy (final InstantiatorStrategy fallbackStrategy) {
		this.fallbackStrategy = fallbackStrategy;
	}

	public InstantiatorStrategy getFallbackInstantiatorStrategy () {
		return fallbackStrategy;
	}

	public ObjectInstantiator newInstantiatorOf (final Class type) {
		Constructor ctor = null;
		try {
			ctor = type.getDeclaredConstructor((Class[])null);
			// Also for public constructors, so that they can be called if the class is not public.
			ctor.setAccessible(true);
		} catch (Exception ex) {
			// Without setAccessible, the constructor can only be called if it and the class are public.
			if (ctor != null && (!Modifier.isPublic(ctor.getModifiers()) || !Modifier.isPublic(type.getModifiers()))) ctor = null;
		}

		if (ctor != null) {
			// Method handle, except on Android and in native images, where reflection is used.
			if (!Util.isAndroid && !Util.isNativeImage) {
				try {
					final MethodHandle handle = MethodHandles.lookup().unreflectConstructor(ctor)
						.asType(MethodType.methodType(Object.class));
					return new ObjectInstantiator() {
						public Object newInstance () {
							try {
								return handle.invokeExact();
							} catch (Error ex) {
								throw ex;
							} catch (Throwable ex) {
								throw createInstantiationError(type, ex);
							}
						}
					};
				} catch (Exception ignored) {
				}
			}

			// Reflection.
			final Constructor constructor = ctor;
			return new ObjectInstantiator() {
				public Object newInstance () {
					try {
						return constructor.newInstance();
					} catch (InvocationTargetException ex) {
						if (ex.getCause() instanceof Error) throw (Error)ex.getCause();
						throw createInstantiationError(type, ex.getCause());
					} catch (Exception ex) {
						throw createInstantiationError(type, ex);
					}
				}
			};
		}

		if (fallbackStrategy == null) {
			if (type.isMemberClass() && !Modifier.isStatic(type.getModifiers())) {
				throw new KryoException("Class cannot be created (non-static member class): " + className(type)
					+ "\nNote: An inner class is serialized with its outer instance, but it has no no-arg constructor, so it needs an "
					+ "instantiator strategy that can create it, eg new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()). "
					+ "Making the class static is safer.");
			} else {
				StringBuilder message = new StringBuilder("Class cannot be created (missing no-arg constructor): " + className(type));
				if (type.getSimpleName().equals("")) {
					message
						.append("\nNote: An anonymous class is serialized with its outer instance and captured variables, but it has "
							+ "no no-arg constructor, so it needs an instantiator strategy that can create it, eg new "
							+ "DefaultInstantiatorStrategy(new StdInstantiatorStrategy()). Anonymous classes have no predictable names, so "
							+ "a named class is safer, eg instead of double brace initialization.");
				}

				if (type.isInterface()) {
					message.append(
						"\nNote: The type you are trying to serialize into is abstract (interface). Kryo will not be able to create an instance of it. Possible solutions:\n")
						.append(
							"You can either use a class that implements the interface or use a custom ObjectInstantiator to create an instance.");
				}

				throw new KryoException(message.toString());
			}
		}
		// InstantiatorStrategy.
		return fallbackStrategy.newInstantiatorOf(type);
	}

	KryoException createInstantiationError (Class type, Throwable throwable) {
		StringBuilder message = new StringBuilder("Error constructing instance of class: " + className(type));
		// Note: For Array and Primitive types the abstract bit is always set.
		if (!type.isArray() && !type.isPrimitive() && Modifier.isAbstract(type.getModifiers())) {
			message.append(
				"\nNote: The type you are trying to serialize into is abstract. Kryo will not be able to create an instance of it. Possible solutions:\n")
				.append("You can either use a concrete subclass or use a custom ObjectInstantiator to create an instance.");
		}
		return new KryoException(message.toString(), throwable);
	}
}
