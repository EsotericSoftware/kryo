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

package com.esotericsoftware.kryo.serializers;

import static com.esotericsoftware.kryo.util.Util.*;

import com.esotericsoftware.kryo.util.IgnoreAndroid;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** A value per class that is computed on first use and shared by all serializers and Kryo instances, like {@link ClassValue}, but
 * without keeping Kryo's class loader alive.
 * <p>
 * A ClassValue keeps a value as long as its class. A value that references Kryo's classes would keep Kryo's class loader alive as
 * long as a class of a parent loader, eg a library of a container when Kryo is in a web application. So the values for classes of
 * Kryo's class loader and the loaders below it are kept with a ClassValue, and are dropped with their class. The values for all
 * other classes are kept in a map, which is dropped with Kryo's class loader. On Android, which has ClassValue only since API
 * level 34 and doesn't unload classes, all values are kept in the map. */
class ClassCache<V> {
	static private final ClassLoader kryoLoader = ClassCache.class.getClassLoader();

	final Function<Class, V> compute;
	private final ConcurrentHashMap<Class, V> map = new ConcurrentHashMap<>();

	private ClassCache (Function<Class, V> compute) {
		this.compute = compute;
	}

	static <V> ClassCache<V> create (Function<Class, V> compute) {
		return isAndroid ? new ClassCache<>(compute) : new WithClassValue<>(compute);
	}

	V get (Class type) {
		V value = map.get(type);
		if (value == null) {
			value = compute.apply(type); // Not in computeIfAbsent, which fails if computing uses the map.
			V existing = map.putIfAbsent(type, value);
			if (existing != null) value = existing;
		}
		return value;
	}

	/** Returns true if the class can't outlive Kryo's class loader: it is from that loader or a loader below it. */
	static boolean belowKryo (Class type) {
		if (kryoLoader == null) return true; // Kryo is never unloaded.
		for (ClassLoader loader = type.getClassLoader(); loader != null; loader = loader.getParent())
			if (loader == kryoLoader) return true;
		return false;
	}

	@IgnoreAndroid
	static private final class WithClassValue<V> extends ClassCache<V> {
		/** The value of a class whose value is in the map. A plain object, which doesn't reference Kryo's classes. */
		static private final Object inMap = new Object();

		private final ClassValue<Object> values = new ClassValue<>() {
			protected Object computeValue (Class type) {
				return belowKryo(type) ? compute.apply(type) : inMap;
			}
		};

		WithClassValue (Function<Class, V> compute) {
			super(compute);
		}

		V get (Class type) {
			Object value = values.get(type);
			return value != inMap ? (V)value : super.get(type);
		}
	}
}
