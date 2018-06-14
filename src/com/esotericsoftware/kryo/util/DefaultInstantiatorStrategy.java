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

package com.esotericsoftware.kryo.util;

import static com.esotericsoftware.kryo.util.Util.*;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.reflectasm.ConstructorAccess;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.InstantiatorStrategy;

public class DefaultInstantiatorStrategy implements org.objenesis.strategy.InstantiatorStrategy {
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
		if (!Util.isAndroid) {
			// Use ReflectASM if the class is not a non-static member class.
			Class enclosingType = type.getEnclosingClass();
			boolean isNonStaticMemberClass = enclosingType != null && type.isMemberClass()
				&& !Modifier.isStatic(type.getModifiers());
			if (!isNonStaticMemberClass) {
				try {
					final ConstructorAccess access = ConstructorAccess.get(type);
					return new ObjectInstantiator() {
						public Object newInstance () {
							try {
								return access.newInstance();
							} catch (Exception ex) {
								throw new KryoException("Error constructing instance of class: " + className(type), ex);
							}
						}
					};
				} catch (Exception ignored) {
				}
			}
		}

		// Reflection.
		try {
			Constructor ctor;
			try {
				ctor = type.getConstructor((Class[])null);
			} catch (Exception ex) {
				ctor = type.getDeclaredConstructor((Class[])null);
				ctor.setAccessible(true);
			}
			final Constructor constructor = ctor;
			return new ObjectInstantiator() {
				public Object newInstance () {
					try {
						return constructor.newInstance();
					} catch (Exception ex) {
						throw new KryoException("Error constructing instance of class: " + className(type), ex);
					}
				}
			};
		} catch (Exception ignored) {
		}

		if (fallbackStrategy == null) {
			if (type.isMemberClass() && !Modifier.isStatic(type.getModifiers()))
				throw new KryoException("Class cannot be created (non-static member class): " + className(type));
			else {
				StringBuilder message = new StringBuilder("Class cannot be created (missing no-arg constructor): " + className(type));
				if (type.getSimpleName().equals("")) {
					message
						.append(
							"\nNote: This is an anonymous class, which is not serializable by default in Kryo. Possible solutions:\n")
						.append("1. Remove uses of anonymous classes, including double brace initialization, from the containing\n")
						.append(
							"class. This is the safest solution, as anonymous classes don't have predictable names for serialization.\n")
						.append("2. Register a FieldSerializer for the containing class and call FieldSerializer\n")
						.append("setIgnoreSyntheticFields(false) on it. This is not safe but may be sufficient temporarily.");
				}
				throw new KryoException(message.toString());
			}
		}
		// InstantiatorStrategy.
		return fallbackStrategy.newInstantiatorOf(type);
	}
}
