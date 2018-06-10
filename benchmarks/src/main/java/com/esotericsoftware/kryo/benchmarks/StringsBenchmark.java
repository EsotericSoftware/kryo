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

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@BenchmarkMode(Mode.SingleShotTime)
@Measurement(batchSize = 40000)
public class StringsBenchmark {
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
	static public class ReadString extends BenchmarkState {
		public ReadString () {
			new StringsBenchmark().writeString(this);
		}
	}

	@State(Scope.Thread)
	static public class ReadStringLong extends BenchmarkState {
		public ReadStringLong () {
			new StringsBenchmark().writeStringLong(this);
		}
	}

	@State(Scope.Thread)
	static public class ReadAsciiLong extends BenchmarkState {
		public ReadAsciiLong () {
			new StringsBenchmark().writeAsciiLong(this);
		}
	}

	@Benchmark
	public void writeString (BenchmarkState state) {
		state.reset();
		for (int i = 0; i < 1000; i++)
			state.output.writeString("abc0123456789");
	}

	@Benchmark
	public void readString (ReadString state) {
		state.reset();
		for (int i = 0; i < 1000; i++)
			state.input.readString();
	}

	@Benchmark
	public void writeStringLong (BenchmarkState state) {
		state.reset();
		for (int i = 0; i < 1000; i++)
			state.output.writeString("abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789");
	}

	@Benchmark
	public void readStringLong (ReadStringLong state) {
		state.reset();
		for (int i = 0; i < 1000; i++)
			state.input.readString();
	}

	@Benchmark
	public void writeAsciiLong (BenchmarkState state) {
		state.reset();
		for (int i = 0; i < 1000; i++)
			state.output.writeAscii("abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789");
	}

	@Benchmark
	public void readAsciiLong (ReadAsciiLong state) {
		state.reset();
		for (int i = 0; i < 1000; i++)
			state.input.readString();
	}
}
