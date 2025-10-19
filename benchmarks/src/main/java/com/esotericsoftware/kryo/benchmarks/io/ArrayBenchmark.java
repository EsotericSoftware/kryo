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

package com.esotericsoftware.kryo.benchmarks.io;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@BenchmarkMode(Mode.SingleShotTime)
@Measurement(batchSize = 12000000)
public class ArrayBenchmark {
	@Benchmark
	public void writeInts (WriteIntsState state) {
		state.reset();
		state.output.writeInts(state.ints, 0, state.ints.length);
	}

	@Benchmark
	public void readInts (ReadIntsState state) {
		state.reset();
		state.input.readInts(state.ints.length);
	}

	@Benchmark
	public void writeVarInts (WriteIntsState state) {
		state.reset();
		state.output.writeInts(state.ints, 0, state.ints.length, true);
	}

	@Benchmark
	public void readVarInts (ReadIntsState state) {
		state.reset();
		state.input.readInts(state.ints.length, true);
	}

	@Benchmark
	public void writeLongs (WriteLongsState state) {
		state.reset();
		state.output.writeLongs(state.longs, 0, state.longs.length);
	}

	@Benchmark
	public void readLongs (ReadLongsState state) {
		state.reset();
		state.input.readLongs(state.longs.length);
	}

	@Benchmark
	public void writeVarLongs (WriteLongsState state) {
		state.reset();
		state.output.writeLongs(state.longs, 0, state.longs.length, true);
	}

	@Benchmark
	public void readVarLongs (ReadLongsState state) {
		state.reset();
		state.input.readLongs(state.longs.length, true);
	}

	//

	@State(Scope.Thread)
	static public class WriteIntsState extends InputOutputState {
		public int[] ints = {0, 1, 2, 3, 4, 5, 63, 64, 65, 127, 128, 129, 4000, 5000, 6000, 16000, 32000, 256000, 1024000, -1, -2,
			-3, -4, Integer.MIN_VALUE, Integer.MAX_VALUE};
	}

	@State(Scope.Thread)
	static public class ReadIntsState extends WriteIntsState {
		public void setup () {
			super.setup();
			new ArrayBenchmark().writeInts(this);
		}
	}

	@State(Scope.Thread)
	static public class WriteLongsState extends InputOutputState {
		public long[] longs = {0, 1, 2, 3, 4, 5, 63, 64, 65, 127, 128, 129, 4000, 5000, 6000, 16000, 32000, 256000, 1024000, -1, -2,
			-3, -4, Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, 9999999999l};

		public void setup () {
			super.setup();
			new ArrayBenchmark().writeLongs(this);
		}
	}

	@State(Scope.Thread)
	static public class ReadLongsState extends WriteLongsState {
		public void setup () {
			super.setup();
			new ArrayBenchmark().writeLongs(this);
		}
	}
}
