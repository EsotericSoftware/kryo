/* Copyright (c) 2008-2025, Nathan Sweet
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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer.FieldSerializerConfig;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;

class FieldSerializerGenericsTest extends KryoTestCase {
	@Test
	void testNoStackOverflowForSimpleGenericsCase () {
		FooRef fooRef = new FooRef();
		GenericFoo<FooRef> genFoo1 = new GenericFoo(fooRef);
		GenericFoo<FooRef> genFoo2 = new GenericFoo(fooRef);
		List<GenericFoo<?>> foos = new ArrayList();
		foos.add(genFoo2);
		foos.add(genFoo1);
		new FooContainer(foos);
		Kryo kryo = new Kryo();
		kryo.setReferences(true);
		kryo.setRegistrationRequired(false);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		kryo.writeObject(new Output(outputStream), genFoo1);
	}

	@Test
	void testNoStackOverflowForComplexGenericsCase () {
		BarRef barRef = new BarRef();
		GenericBar<BarRef> genBar1 = new GenericBar(barRef);
		GenericBar<BarRef> genBar2 = new GenericBar(barRef);
		List<GenericBar<?>> bars = new ArrayList();
		bars.add(genBar2);
		bars.add(genBar1);
		new GenericBarContainer<GenericBar>(new BarContainer(bars));
		Kryo kryo = new Kryo();
		kryo.setReferences(true);
		kryo.setRegistrationRequired(false);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		kryo.writeObject(new Output(outputStream), genBar1);
	}

	@Test
	void testMultipleValuesWithDifferentParameters () {
		// MultipleValues has fields with different parameterized types for Value.
		MultipleValues values = new MultipleValues();
		values.integer = new Value(123);
		values.string = new Value("abc");

		CollectionSerializer collectionSerializer = new CollectionSerializer();
		collectionSerializer.setElementsCanBeNull(false); // Increase generics savings so difference is more easily seen.

		kryo.register(MultipleValues.class);
		kryo.register(Value.class);
		kryo.register(ArrayList.class, collectionSerializer);

		roundTrip(28, values);
	}

	@Test
	void testParameterPassedToSuper () {
		SuperTest superTest = new SuperTest();
		superTest.integer = new PassArgToSupers();
		superTest.integer.list = new ArrayList();
		superTest.integer.list.add(1);
		superTest.integer.list.add(2);
		superTest.integer.list.add(3);
		superTest.integer.value = 512;
		superTest.string = new PassArgToSupers();
		superTest.string.list = new ArrayList();
		superTest.string.list.add("list1");
		superTest.string.list.add("list2");
		superTest.string.list.add("list3");
		superTest.string.value = "value";

		CollectionSerializer collectionSerializer = new CollectionSerializer();
		collectionSerializer.setElementsCanBeNull(false); // Increase generics savings so difference is more easily seen.

		kryo.register(SuperTest.class);
		kryo.register(PassArgToSupers.class);
		kryo.register(ArrayList.class, collectionSerializer);

		roundTrip(33, superTest);
	}

	@Test
	void testNestedLists () {
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

		// Increase generics savings so difference is more easily seen.
		FieldSerializerConfig config = new FieldSerializerConfig();
		FieldSerializer nestedListValueSerializer = new FieldSerializer(kryo, NestedListValue.class, config);
		nestedListValueSerializer.getField("value").setCanBeNull(false);

		CollectionSerializer collectionSerializer = new CollectionSerializer();
		collectionSerializer.setElementsCanBeNull(false);

		kryo.register(ArrayList.class, collectionSerializer);
		kryo.register(NestedLists.class);
		kryo.register(NestedListValue.class, nestedListValueSerializer);

		NestedLists nestedLists = new NestedLists();
		nestedLists.lists = new ArrayList(Arrays.asList(new NestedListValue(123), new NestedListValue(456)));
		roundTrip(7, nestedLists);
	}

	// ---

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

	// ---

	public static final class Value<T> {
		public T value;
		public ArrayList<T> list;

		public Value () {
		}

		public Value (T value) {
			this.value = value;
			list = new ArrayList();
			list.add(value);
			list.add(value);
			list.add(value);
		}

		public boolean equals (Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
	}

	public static final class MultipleValues {
		public Value<String> string;
		public Value<Integer> integer;

		public boolean equals (Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
	}

	// ---

	public static class NestedLists {
		public ArrayList<NestedListValue<Integer>> lists;

		public boolean equals (Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
	}

	public static final class NestedListValue<T> {
		public T value;

		public NestedListValue (T value) {
			this.value = value;
		}

		public boolean equals (Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
	}

	// ---

	public static class Super<X> {
		public ArrayList<X> list;
		public X value;

		public boolean equals (Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
	}

	public static class PassArgToSuper<T> extends Super<T> {
	}

	public static final class PassArgToSupers<T> extends PassArgToSuper<T> {
	}

	public static final class SuperTest {
		public PassArgToSupers<Integer> integer;
		public PassArgToSupers<String> string;

		public boolean equals (Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
	}
}
