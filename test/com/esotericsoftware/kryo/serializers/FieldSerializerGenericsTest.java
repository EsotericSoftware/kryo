/* Copyright (c) 2008-2017, Nathan Sweet
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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.FieldSerializerFactory;
import com.esotericsoftware.kryo.io.Output;

@RunWith(Parameterized.class)
public class FieldSerializerGenericsTest {

	@Parameters(name = "optimizedGenerics_{0}")
	static public Iterable optimizedGenerics () {
		return Arrays.asList(true, false);
	}

	private boolean optimizedGenerics;

	public FieldSerializerGenericsTest (boolean optimizedGenerics) {
		this.optimizedGenerics = optimizedGenerics;
	}

	@Test
	public void testNoStackOverflowForSimpleGenericsCase () {
		FooRef fooRef = new FooRef();
		GenericFoo<FooRef> genFoo1 = new GenericFoo(fooRef);
		GenericFoo<FooRef> genFoo2 = new GenericFoo(fooRef);
		List<GenericFoo<?>> foos = new ArrayList();
		foos.add(genFoo2);
		foos.add(genFoo1);
		new FooContainer(foos);
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		FieldSerializerFactory factory = new FieldSerializerFactory();
		factory.getConfig().setOptimizedGenerics(optimizedGenerics);
		kryo.setDefaultSerializer(factory);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		kryo.writeObject(new Output(outputStream), genFoo1);
	}

	@Test
	public void testNoStackOverflowForComplexGenericsCase () {
		BarRef barRef = new BarRef();
		GenericBar<BarRef> genBar1 = new GenericBar(barRef);
		GenericBar<BarRef> genBar2 = new GenericBar(barRef);
		List<GenericBar<?>> bars = new ArrayList();
		bars.add(genBar2);
		bars.add(genBar1);
		new GenericBarContainer<GenericBar>(new BarContainer(bars));
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		FieldSerializerFactory factory = new FieldSerializerFactory();
		factory.getConfig().setOptimizedGenerics(optimizedGenerics);
		kryo.setDefaultSerializer(factory);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		kryo.writeObject(new Output(outputStream), genBar1);
	}

	static class GenericBarContainer<T extends Bar> {
		BarContainer barContainer;

		public GenericBarContainer (BarContainer barContainer) {
			this.barContainer = barContainer;
			for (GenericBar<?> foo : barContainer.foos) {
				foo.container = this;
			}
		}
	}

	static class BarContainer {
		List<GenericBar<?>> foos;

		public BarContainer (List<GenericBar<?>> foos) {
			this.foos = foos;
		}
	}

	interface Bar {
	}

	static class BarRef implements Bar {
	}

	static class GenericBar<B extends Bar> implements Bar {
		private Map<String, Object> map = Collections.singletonMap("myself", (Object)this);
		B foo;
		GenericBarContainer<?> container;

		public GenericBar (B foo) {
			this.foo = foo;
		}
	}

	interface Foo {
	}

	static class FooRef implements Foo {
	}

	static class FooContainer {
		List<GenericFoo<?>> foos = new ArrayList();

		public FooContainer (List<GenericFoo<?>> foos) {
			this.foos = foos;
			for (GenericFoo<?> foo : foos) {
				foo.container = this;
			}
		}
	}

	static class GenericFoo<B extends Foo> implements Foo {
		private Map<String, Object> map = Collections.singletonMap("myself", (Object)this);
		B foo;
		FooContainer container;

		public GenericFoo (B foo) {
			this.foo = foo;
		}
	}
}
