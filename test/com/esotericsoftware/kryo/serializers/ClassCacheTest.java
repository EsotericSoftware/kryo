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

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.Kryo;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ClassCacheTest {
	@Test
	void testSameValue () {
		ClassCache<Object> cache = ClassCache.create(type -> new Object());
		// A JDK class, whose value is in the map, and a class of Kryo's class loader, whose value is in the ClassValue.
		assertFalse(ClassCache.belowKryo(String.class));
		assertTrue(ClassCache.belowKryo(ClassCacheTest.class));
		assertSame(cache.get(String.class), cache.get(String.class));
		assertSame(cache.get(ClassCacheTest.class), cache.get(ClassCacheTest.class));
		assertNotSame(cache.get(String.class), cache.get(ClassCacheTest.class));
	}

	/** Kryo in a child class loader, eg of a web application, copies an object of a class in the parent loader, eg a library of
	 * the container, with FieldSerializer, which caches the fields and their generic types of the class. The class must not keep
	 * Kryo's class loader alive. */
	@Test
	void testKryoLoaderNotKeptAlive () throws Exception {
		URL testClasses = ClassCacheTest.class.getProtectionDomain().getCodeSource().getLocation();
		try (URLClassLoader parent = new URLClassLoader(new URL[] {testClasses}, ClassLoader.getPlatformClassLoader())) {
			WeakReference<ClassLoader> loader = useKryoInChildLoader(parent);
			for (int i = 0; i < 250 && loader.get() != null; i++) { // Up to 5 seconds.
				System.gc();
				Thread.sleep(20);
			}
			assertNull(loader.get(), "Kryo's class loader was not collected.");
		}
	}

	static private WeakReference<ClassLoader> useKryoInChildLoader (ClassLoader parent) throws Exception {
		URL kryoClasses = Kryo.class.getProtectionDomain().getCodeSource().getLocation();
		URLClassLoader loader = new URLClassLoader(new URL[] {kryoClasses}, parent);
		Class kryoClass = loader.loadClass(Kryo.class.getName());
		assertNotSame(Kryo.class, kryoClass);
		Object kryo = kryoClass.getConstructor().newInstance();
		kryoClass.getMethod("setRegistrationRequired", boolean.class).invoke(kryo, false);
		Object model = parent.loadClass(Model.class.getName()).getConstructor().newInstance();
		Object copy = kryoClass.getMethod("copy", Object.class).invoke(kryo, model);
		assertNotSame(model, copy);
		loader.close();
		return new WeakReference<>(loader);
	}

	public static class Model {
		public List<String> names = new ArrayList<>(List.of("a"));
		public Object other = "x";
	}
}
