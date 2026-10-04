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

package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializerFactory.CompatibleFieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.TaggedFieldSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.Throughput)
@Fork(1)
public class StateBenchmark {
	@Benchmark
	public Object field (FieldState state) {
		return state.roundTrip();
	}

	@Benchmark
	public Object compatible (CompatibleState state) {
		return state.roundTrip();
	}

	@Benchmark
	public Object tagged (TaggedState state) {
		return state.roundTrip();
	}

	@Benchmark
	public Object version (VersionState state) {
		return state.roundTrip();
	}

	@State(Scope.Thread)
	static public abstract class BenchmarkState {
		@Param({"true"}) public boolean references;
		@Param({"4"}) public int scale;

		final Kryo kryo = new Kryo();
		final Output output = new Output(1024 * 1024, -1);
		Input input;
		Object object;

		@Setup(Level.Trial)
		public void setup () {
			kryo.register(ArrayList.class);
			kryo.register(HashMap.class);
			kryo.register(int[].class);
			kryo.register(double[].class);
			for (Class type : AppState.classes())
				kryo.register(type);
			kryo.setReferences(references);
			object = AppState.create(1, scale);

			// Sanity check: a round trip must serialize to the same bytes.
			output.reset();
			kryo.writeObject(output, object);
			byte[] first = output.toBytes();
			Object copy = kryo.readObject(new Input(first), AppState.class);
			output.reset();
			kryo.writeObject(output, copy);
			// HashMap iteration order may differ after a round trip, which with references also changes the reference IDs, so
			// compare the graphs instead of the bytes.
			if (!deepEquals(object, copy, new java.util.IdentityHashMap<>()))
				throw new IllegalStateException("Round trip mismatch.");
			System.out.println("\nPayload: " + first.length + " bytes, objects: " + countObjects(object));
			input = new Input(output.getBuffer());
		}

		/** Counts distinct objects reachable from the root (not counting enums and boxed primitives). */
		static int countObjects (Object root) {
			java.util.Set<Object> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
			java.util.ArrayDeque<Object> stack = new java.util.ArrayDeque<>();
			push(stack, root);
			while (!stack.isEmpty()) {
				Object o = stack.pop();
				if (o instanceof Enum || o instanceof Number || o instanceof Boolean || !seen.add(o)) continue;
				if (o instanceof java.util.Collection) for (Object e : (java.util.Collection)o) push(stack, e);
				else if (o instanceof java.util.Map) for (Object e : ((java.util.Map)o).entrySet()) {
					push(stack, ((java.util.Map.Entry)e).getKey());
					push(stack, ((java.util.Map.Entry)e).getValue());
				}
				else if (o.getClass().getPackageName().equals(AppState.class.getPackageName())) {
					for (Class c = o.getClass(); c != Object.class; c = c.getSuperclass())
						for (java.lang.reflect.Field f : c.getDeclaredFields()) {
							if (f.getType().isPrimitive() || java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
							f.setAccessible(true);
							try {
								push(stack, f.get(o));
							} catch (IllegalAccessException ex) {
								throw new RuntimeException(ex);
							}
						}
				}
			}
			return seen.size();
		}

		static void push (java.util.ArrayDeque<Object> stack, Object o) {
			if (o != null) stack.push(o);
		}

		/** Compares two object graphs field by field, maps by key. */
		static boolean deepEquals (Object a, Object b, java.util.IdentityHashMap<Object, Object> seen) {
			if (a == b) return true;
			if (a == null || b == null || a.getClass() != b.getClass()) return false;
			if (seen.get(a) == b) return true;
			seen.put(a, b);
			if (a instanceof int[]) return Arrays.equals((int[])a, (int[])b);
			if (a instanceof double[]) return Arrays.equals((double[])a, (double[])b);
			if (a instanceof java.util.List) {
				java.util.List la = (java.util.List)a, lb = (java.util.List)b;
				if (la.size() != lb.size()) return false;
				for (int i = 0; i < la.size(); i++)
					if (!deepEquals(la.get(i), lb.get(i), seen)) return false;
				return true;
			}
			if (a instanceof java.util.Map) {
				java.util.Map ma = (java.util.Map)a, mb = (java.util.Map)b;
				if (ma.size() != mb.size()) return false;
				for (Object e : ma.entrySet()) {
					Object key = ((java.util.Map.Entry)e).getKey();
					if (!mb.containsKey(key) || !deepEquals(((java.util.Map.Entry)e).getValue(), mb.get(key), seen)) return false;
				}
				return true;
			}
			if (!a.getClass().getPackageName().equals(AppState.class.getPackageName()) || a instanceof Enum) return a.equals(b);
			for (Class c = a.getClass(); c != Object.class; c = c.getSuperclass())
				for (java.lang.reflect.Field f : c.getDeclaredFields()) {
					if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
					f.setAccessible(true);
					try {
						if (!deepEquals(f.get(a), f.get(b), seen)) return false;
					} catch (IllegalAccessException ex) {
						throw new RuntimeException(ex);
					}
				}
			return true;
		}

		public Object roundTrip () {
			output.reset();
			kryo.writeObject(output, object);
			input.setPosition(0);
			input.setLimit(output.position());
			return kryo.readObject(input, AppState.class);
		}
	}

	static public class FieldState extends BenchmarkState {
		public void setup () {
			kryo.setDefaultSerializer(FieldSerializer.class);
			super.setup();
		}
	}

	static public class CompatibleState extends BenchmarkState {
		@Param({"false"}) public boolean chunked;

		public void setup () {
			CompatibleFieldSerializerFactory factory = new CompatibleFieldSerializerFactory();
			factory.getConfig().setChunkedEncoding(chunked);
			factory.getConfig().setReadUnknownFieldData(true);
			kryo.setDefaultSerializer(factory);
			super.setup();
		}
	}

	static public class TaggedState extends BenchmarkState {
		@Param({"false"}) public boolean chunked;

		public void setup () {
			TaggedFieldSerializerFactory factory = new TaggedFieldSerializerFactory();
			factory.getConfig().setChunkedEncoding(chunked);
			if (chunked) factory.getConfig().setReadUnknownTagData(true);
			kryo.setDefaultSerializer(factory);
			super.setup();
		}
	}

	static public class VersionState extends BenchmarkState {
		public void setup () {
			kryo.setDefaultSerializer(VersionFieldSerializer.class);
			super.setup();
		}
	}
}
