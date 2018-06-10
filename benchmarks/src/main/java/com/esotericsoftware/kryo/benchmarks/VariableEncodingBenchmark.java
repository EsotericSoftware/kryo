/* Copyright (c) 2008-2018, Nathan Sweet
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

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@BenchmarkMode(Mode.SingleShotTime)
@Measurement(batchSize = 150000000)
public class VariableEncodingBenchmark {
	@State(Scope.Thread)
	static public class BenchmarkState {
		final Output output;
		final Input input;

		public BenchmarkState () {
			output = new Output(1024 * 512);
			input = new Input(output.getBuffer());
		}

		public void reset () {
			input.setPosition(0);
			output.setPosition(0);
		}
	}

	@State(Scope.Thread)
	static public class ReadInt extends BenchmarkState {
		public ReadInt () {
			new VariableEncodingBenchmark().writeInt(this);
		}
	}

	@State(Scope.Thread)
	static public class ReadVarInt extends BenchmarkState {
		public ReadVarInt () {
			new VariableEncodingBenchmark().readVarInt(this);
		}
	}

	@State(Scope.Thread)
	static public class ReadLong extends BenchmarkState {
		public ReadLong () {
			new VariableEncodingBenchmark().writeLong(this);
		}
	}

	@State(Scope.Thread)
	static public class ReadVarLong extends BenchmarkState {
		public ReadVarLong () {
			new VariableEncodingBenchmark().writeVarLong(this);
		}
	}

	@Benchmark
	public void writeInt (BenchmarkState state) {
		state.reset();
		state.output.writeInt(1234);
	}

	@Benchmark
	public void readInt (ReadInt state) {
		state.reset();
		state.input.readInt();
	}

	@Benchmark
	public void writeVarInt (BenchmarkState state) {
		state.reset();
		state.output.writeVarInt(1234, true);
	}

	@Benchmark
	public int readVarInt (ReadVarInt state) {
		state.reset();
		return state.input.readVarInt(true);
	}

	@Benchmark
	public void writeLong (BenchmarkState state) {
		state.reset();
		state.output.writeLong(12341234);
	}

	@Benchmark
	public long readLong (ReadLong state) {
		state.reset();
		return state.input.readLong();
	}

	@Benchmark
	public void writeVarLong (BenchmarkState state) {
		state.reset();
		state.output.writeVarLong(12341234, true);
	}

	@Benchmark
	public long readVarLong (ReadLong state) {
		state.reset();
		return state.input.readVarLong(true);
	}
}
