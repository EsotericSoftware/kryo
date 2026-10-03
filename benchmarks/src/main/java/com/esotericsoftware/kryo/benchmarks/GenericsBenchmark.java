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

package com.esotericsoftware.kryo.benchmarks;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/** Measures FieldSerializer with type variables that are resolved from the declared type of a field, for different relations
 * between the declared type and the class of the value. */
public class GenericsBenchmark {
	@Benchmark
	public Object write (BenchmarkState state) {
		state.output.setPosition(0);
		state.kryo.writeObject(state.output, state.object);
		return state.output;
	}

	@Benchmark
	public Object read (BenchmarkState state) {
		state.input.setPosition(0);
		return state.kryo.readObject(state.input, state.object.getClass());
	}

	@State(Scope.Thread)
	public static class BenchmarkState {
		@Param() public ObjectType objectType;

		final Kryo kryo = new Kryo();
		final Output output = new Output(1024 * 512);
		Input input;
		Object object;

		@Setup(Level.Trial)
		public void setup () {
			kryo.register(ArrayList.class);
			kryo.register(PlainRoot.class);
			kryo.register(Plain.class);
			kryo.register(HolderRoot.class);
			kryo.register(Holder.class);
			kryo.register(SubRoot.class);
			kryo.register(Sub.class);
			kryo.register(ImplRoot.class);
			kryo.register(Impl.class);

			switch (objectType) {
			case plain:
				PlainRoot plain = new PlainRoot();
				for (int i = 0; i < SIZE; i++)
					plain.values.add(new Plain().populate());
				object = plain;
				break;
			case sameClass:
				HolderRoot holder = new HolderRoot();
				for (int i = 0; i < SIZE; i++)
					holder.values.add(new Holder<String>().populate("value" + i));
				object = holder;
				break;
			case subclass:
				SubRoot sub = new SubRoot();
				for (int i = 0; i < SIZE; i++)
					sub.values.add(new Sub<String>().populate("value" + i));
				object = sub;
				break;
			case interfaceImpl:
				ImplRoot impl = new ImplRoot();
				for (int i = 0; i < SIZE; i++)
					impl.values.add(new Impl<String>().populate("value" + i));
				object = impl;
				break;
			}

			kryo.writeObject(output, object);
			input = new Input(output.toBytes());
		}

		public enum ObjectType {
			/** No type variables, the declared type has no type arguments. */
			plain,
			/** The class of the value is the declared class: {@code Holder<String>}. */
			sameClass,
			/** The class of the value is a subclass of the declared class: {@code Base<String>} with a {@code Sub<T>}. */
			subclass,
			/** The class of the value implements the declared interface: {@code Container<String>} with an {@code Impl<T>}. */
			interfaceImpl
		}
	}

	static final int SIZE = 100;

	public static class Plain {
		public String value;
		public List<String> list = new ArrayList<>();

		Plain populate () {
			value = "value";
			list.add("a");
			list.add("b");
			return this;
		}
	}

	public static class PlainRoot {
		public List<Plain> values = new ArrayList<>();
	}

	public static class Holder<T> {
		public T value;
		public List<T> list = new ArrayList<>();

		Holder<T> populate (T value) {
			this.value = value;
			list.add(value);
			list.add(value);
			return this;
		}
	}

	public static class HolderRoot {
		public List<Holder<String>> values = new ArrayList<>();
	}

	public static class Base<T> {
		public T value;
	}

	public static class Sub<T> extends Base<T> {
		public List<T> list = new ArrayList<>();

		Sub<T> populate (T value) {
			this.value = value;
			list.add(value);
			list.add(value);
			return this;
		}
	}

	public static class SubRoot {
		public List<Base<String>> values = new ArrayList<>();
	}

	public interface Container<T> {
	}

	public static class Impl<T> implements Container<T> {
		public T value;
		public List<T> list = new ArrayList<>();

		Impl<T> populate (T value) {
			this.value = value;
			list.add(value);
			list.add(value);
			return this;
		}
	}

	public static class ImplRoot {
		public List<Container<String>> values = new ArrayList<>();
	}
}
