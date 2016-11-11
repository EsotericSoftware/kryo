
package com.esotericsoftware.kryo;

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

import com.esotericsoftware.kryo.io.FastOutput;

@RunWith(Parameterized.class)
public class FieldSerializerGenericsTest {

	@Parameters(name = "optimizedGenerics_{0}")
	public static Iterable<?> optimizedGenerics () {
		return Arrays.asList(true, false);
	}

	private boolean optimizedGenerics;

	public FieldSerializerGenericsTest (boolean optimizedGenerics) {
		this.optimizedGenerics = optimizedGenerics;
	}

	@Test
	public void testNoStackOverflowForSimpleGenericsCase () {
		FooRef fooRef = new FooRef();
		GenericFoo<FooRef> genFoo1 = new GenericFoo<FooRef>(fooRef);
		GenericFoo<FooRef> genFoo2 = new GenericFoo<FooRef>(fooRef);
		List<GenericFoo<?>> foos = new ArrayList<GenericFoo<?>>();
		foos.add(genFoo2);
		foos.add(genFoo1);
		new FooContainer(foos);
		Kryo kryo = new Kryo();
		kryo.getFieldSerializerConfig().setOptimizedGenerics(optimizedGenerics);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		kryo.writeObject(new FastOutput(outputStream), genFoo1);
	}

	@Test
	public void testNoStackOverflowForComplexGenericsCase () {
		BarRef barRef = new BarRef();
		GenericBar<BarRef> genBar1 = new GenericBar<BarRef>(barRef);
		GenericBar<BarRef> genBar2 = new GenericBar<BarRef>(barRef);
		List<GenericBar<?>> bars = new ArrayList<GenericBar<?>>();
		bars.add(genBar2);
		bars.add(genBar1);
		new GenericBarContainer<GenericBar>(new BarContainer(bars));
		Kryo kryo = new Kryo();
		kryo.getFieldSerializerConfig().setOptimizedGenerics(optimizedGenerics);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		kryo.writeObject(new FastOutput(outputStream), genBar1);
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
		List<GenericFoo<?>> foos = new ArrayList<GenericFoo<?>>();

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
