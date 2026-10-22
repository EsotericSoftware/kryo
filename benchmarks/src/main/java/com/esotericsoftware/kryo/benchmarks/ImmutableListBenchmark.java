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
import com.esotericsoftware.kryo.serializers.ImmutableCollectionsSerializers;

import java.util.List;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/** Measures reading and copying immutable lists created by {@code List.of}, or by {@code Stream.toList} with a null as the last
 * element. */
public class ImmutableListBenchmark {
	@Benchmark
	public Object read (BenchmarkState state) {
		state.input.setPosition(0);
		return state.kryo.readClassAndObject(state.input);
	}

	@Benchmark
	public Object copy (BenchmarkState state) {
		return state.kryo.copy(state.list);
	}

	@State(Scope.Thread)
	public static class BenchmarkState {
		@Param({"0", "1", "2", "3", "10", "100", "1000"}) public int size;
		@Param({"false", "true"}) public boolean nulls;

		final Kryo kryo = new Kryo();
		final Output output = new Output(1024 * 64);
		Input input;
		List<Object> list;

		@Setup(Level.Trial)
		public void setup () {
			ImmutableCollectionsSerializers.registerSerializers(kryo);
			if (nulls)
				list = IntStream.range(0, size).mapToObj(i -> i == size - 1 ? null : (Object)i).toList();
			else
				list = List.of(IntStream.range(0, size).boxed().toArray());
			kryo.writeClassAndObject(output, list);
			input = new Input(output.toBytes());
		}
	}
}
