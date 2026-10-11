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
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializer;

import java.util.ArrayList;
import java.util.HashMap;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/** Measures {@link CollectionSerializer} and {@link MapSerializer} writing and reading an ArrayList of strings and a HashMap with
 * string keys and integer values. The elements parameter selects how the classes of the elements, keys and values are written:
 * known to the serializers like from a field with type arguments ({@code typed}), once for all elements because they have the same
 * class ({@code same}, {@code nulls} with a null last element or value), or for each element because they differ ({@code mixed},
 * alternating strings and integers). */
public class CollectionSerializerBenchmark {
	@Benchmark
	public void writeList (BenchmarkState state) {
		state.listOutput.setPosition(0);
		state.kryo.writeClassAndObject(state.listOutput, state.list);
	}

	@Benchmark
	public Object readList (BenchmarkState state) {
		state.listInput.setPosition(0);
		return state.kryo.readClassAndObject(state.listInput);
	}

	@Benchmark
	public void writeMap (BenchmarkState state) {
		state.mapOutput.setPosition(0);
		state.kryo.writeClassAndObject(state.mapOutput, state.map);
	}

	@Benchmark
	public Object readMap (BenchmarkState state) {
		state.mapInput.setPosition(0);
		return state.kryo.readClassAndObject(state.mapInput);
	}

	@State(Scope.Thread)
	public static class BenchmarkState {
		@Param({"1", "10", "100", "1000"}) public int size;
		@Param({"typed", "same", "nulls", "mixed"}) public Elements elements;
		@Param({"false", "true"}) public boolean references;

		final Kryo kryo = new Kryo();
		final Output listOutput = new Output(1024 * 64), mapOutput = new Output(1024 * 64);
		Input listInput, mapInput;
		ArrayList list;
		HashMap map;

		@Setup(Level.Trial)
		public void setup () {
			kryo.setReferences(references);
			CollectionSerializer listSerializer = new CollectionSerializer();
			MapSerializer mapSerializer = new MapSerializer();
			if (elements == Elements.typed) {
				listSerializer.setElementClass(String.class, kryo.getSerializer(String.class));
				mapSerializer.setKeyClass(String.class, kryo.getSerializer(String.class));
				mapSerializer.setValueClass(Integer.class, kryo.getSerializer(Integer.class));
			}
			kryo.register(ArrayList.class, listSerializer);
			kryo.register(HashMap.class, mapSerializer);

			list = new ArrayList(size);
			map = new HashMap(size);
			for (int i = 0; i < size; i++) {
				Object string = Integer.toString(i), integer = i;
				if (elements == Elements.mixed && i % 2 == 1) {
					string = i;
					integer = Integer.toString(i);
				}
				if (elements == Elements.nulls && i == size - 1) {
					string = null;
					integer = null;
				}
				list.add(string);
				map.put(string, integer);
			}

			kryo.writeClassAndObject(listOutput, list);
			listInput = new Input(listOutput.toBytes());
			kryo.writeClassAndObject(mapOutput, map);
			mapInput = new Input(mapOutput.toBytes());
		}
	}

	public enum Elements {
		typed, same, nulls, mixed
	}
}
