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
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test for java 8 closures. */
class ClosureSerializerTest extends KryoTestCase {
	@BeforeEach
	public void setUp () throws Exception {
		super.setUp();
		// kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		kryo.register(Object[].class);
		kryo.register(Class.class);
		kryo.register(getClass()); // The closure's capturing class must be registered.
		kryo.register(ClosureSerializer.Closure.class, new ClosureSerializer());
	}

	@Test
	void testClosureNotRegistered () {
		// The class of a closure can't be found by its name, so it is not registered implicitly (#1137).
		for (boolean registrationRequired : new boolean[] {true, false}) {
			Kryo kryo = new Kryo();
			kryo.setRegistrationRequired(registrationRequired);
			Callable<Integer> closure = (Callable<Integer> & java.io.Serializable)( () -> 72363);
			IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> kryo.writeClassAndObject(new Output(1024), closure));
			assertTrue(ex.getMessage().contains("new ClosureSerializer()"), ex.getMessage());
			assertThrows(IllegalArgumentException.class, () -> kryo.copy(closure));
		}
	}

	@Test
	void testSerializableClosure () {
		Callable<Integer> closure1 = (Callable<Integer> & java.io.Serializable)( () -> 72363);

		// The length cannot be checked reliable, as it can vary based on the JVM.
		roundTrip(Integer.MIN_VALUE, closure1);

		Output output = new Output(1024, -1);
		kryo.writeObject(output, closure1);

		Input input = new Input(output.getBuffer(), 0, output.position());
		Callable<Integer> closure2 = (Callable<Integer>)kryo.readObject(input, ClosureSerializer.Closure.class);

		doAssertEquals(closure1, closure2);
	}

	@Test
	void testCapturingClosure () {
		final int number = 72363;
		Supplier<Integer> closure1 = (Supplier<Integer> & java.io.Serializable) () -> number;

		// The length cannot be checked reliable, as it can vary based on the JVM.
		roundTrip(Integer.MIN_VALUE, closure1);

		Output output = new Output(1024, -1);
		kryo.writeObject(output, closure1);

		Input input = new Input(output.getBuffer(), 0, output.position());
		Supplier<Integer> closure2 = (Supplier<Integer>)kryo.readObject(input, ClosureSerializer.Closure.class);

		doAssertEquals(closure1, closure2);
	}

	@Test
	void testMethodReference () {
		Supplier<Integer> closure1 = (Supplier<Integer> & java.io.Serializable)NumberFactory::getNumber;

		// The length cannot be checked reliable, as it can vary based on the JVM.
		roundTrip(Integer.MIN_VALUE, closure1);

		Output output = new Output(1024, -1);
		kryo.writeObject(output, closure1);

		Input input = new Input(output.getBuffer(), 0, output.position());
		Supplier<Integer> closure2 = (Supplier<Integer>)kryo.readObject(input, ClosureSerializer.Closure.class);

		doAssertEquals(closure1, closure2);
	}

	@Test
	void testCopyClosure () {
		Callable<Integer> closure1 = (Callable<Integer> & java.io.Serializable)( () -> 72363);

		final Callable<Integer> closure2 = kryo.copy(closure1);

		doAssertEquals(closure1, closure2);
	}

	@Test
	void testNestedClassClosure () {
		// The capturing class is the nested class, not the nest host.
		kryo.register(Nested.class);
		Supplier<Integer> closure1 = Nested.closure(72363);
		Supplier<Integer> closure2 = roundTrip(Integer.MIN_VALUE, closure1);
		doAssertEquals(closure1, closure2);
		doAssertEquals(closure1, kryo.copy(closure1));

		// The capturing class of a closure created by another closure is the enclosing class.
		Supplier<Supplier<Integer>> outer = () -> (Supplier<Integer> & java.io.Serializable) () -> 72363;
		closure1 = outer.get();
		closure2 = roundTrip(Integer.MIN_VALUE, closure1);
		doAssertEquals(closure1, closure2);
	}

	@Test
	void testClosureFromOtherClassLoader () throws Exception {
		// The capturing class is resolved with the closure's class loader, which can differ from Kryo's.
		String name = OtherLoaderClosure.class.getName();
		ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
			protected Class<?> loadClass (String className, boolean resolve) throws ClassNotFoundException {
				if (!className.equals(name)) return super.loadClass(className, resolve);
				synchronized (getClassLoadingLock(className)) {
					Class<?> type = findLoadedClass(className);
					if (type == null) {
						try (InputStream input = ClosureSerializerTest.class.getResourceAsStream("ClosureSerializerTest$OtherLoaderClosure.class")) {
							byte[] bytes = input.readAllBytes();
							type = defineClass(className, bytes, 0, bytes.length);
						} catch (IOException ex) {
							throw new ClassNotFoundException(className, ex);
						}
					}
					return type;
				}
			}
		};
		Class<?> type = loader.loadClass(name);
		assertNotSame(OtherLoaderClosure.class, type);
		kryo.register(type);
		Supplier<Integer> closure1 = (Supplier<Integer>)type.getMethod("closure").invoke(null);
		assertSame(loader, closure1.getClass().getClassLoader());

		Supplier<Integer> closure2 = roundTrip(Integer.MIN_VALUE, closure1);
		assertSame(loader, closure2.getClass().getClassLoader());
		doAssertEquals(closure1, closure2);

		closure2 = kryo.copy(closure1);
		assertSame(loader, closure2.getClass().getClassLoader());
		doAssertEquals(closure1, closure2);
	}

	protected void doAssertEquals (Object object1, Object object2) {
		try {
			if (object1 instanceof Callable) {
				assertEquals(((Callable)object1).call(), ((Callable)object2).call());
			}
			if (object1 instanceof Supplier) {
				assertEquals(((Supplier)object1).get(), ((Supplier)object2).get());
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex.getMessage());
		}
	}

	static class NumberFactory {
		static int getNumber () {
			return 72363;
		}
	}

	static class Nested {
		static Supplier<Integer> closure (int number) {
			return (Supplier<Integer> & java.io.Serializable) () -> number;
		}
	}

	/** Loaded a second time by another class loader in {@link #testClosureFromOtherClassLoader()}, so it must not reference the
	 * test class. */
	public static class OtherLoaderClosure {
		public static Supplier<Integer> closure () {
			return (Supplier<Integer> & java.io.Serializable) () -> 72363;
		}
	}
}
