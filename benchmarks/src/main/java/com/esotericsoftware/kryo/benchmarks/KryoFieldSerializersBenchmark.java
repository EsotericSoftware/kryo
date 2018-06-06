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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

public class KryoFieldSerializersBenchmark {
	static class BenchmarkState {
		Kryo kryo;
		byte[] buffer;
		Output output;
		SampleObject1 object;

		void setup (Class<? extends Serializer> defaultSerializer) {
			kryo = new Kryo();
			kryo.setDefaultSerializer(defaultSerializer);
			kryo.setRegistrationRequired(true);
			kryo.register(double[].class);
			kryo.register(long[].class);
			kryo.register(SampleObject1.class);

			buffer = new byte[1024];
			output = new Output(buffer);

			object = SampleObject1.createSample();
		}
	}

	@State(Scope.Thread)
	public static class FieldSerializerState extends BenchmarkState {
		@Setup(Level.Invocation)
		public void setup () {
			super.setup(FieldSerializer.class);
		}
	}

	@State(Scope.Thread)
	public static class CompatibleFieldSerializerState extends BenchmarkState {
		@Setup(Level.Invocation)
		public void setup () {
			super.setup(CompatibleFieldSerializer.class);
		}
	}

	@State(Scope.Thread)
	public static class TaggedFieldSerializerState extends BenchmarkState {
		@Setup(Level.Invocation)
		public void setup () {
			super.setup(TaggedFieldSerializer.class);
		}
	}

	@State(Scope.Thread)
	public static class VersionFieldSerializerState extends BenchmarkState {
		@Setup(Level.Invocation)
		public void setup () {
			super.setup(VersionFieldSerializer.class);
		}
	}

	@State(Scope.Thread)
	public static class CustomSerializerState extends BenchmarkState {
		@Setup(Level.Invocation)
		public void setup () {
			super.setup(FieldSerializer.class);
			kryo.register(SampleObject1.class, new Serializer<SampleObject1>() {
				public void write (Kryo kryo, Output output, SampleObject1 object) {
					output.writeInt(object.intValue);
					output.writeFloat(object.floatValue);
					output.writeShort(object.shortValue.intValue());
					kryo.writeObject(output, object.longArray); // could be referenced
					kryo.writeObject(output, object.doubleArray); // could be referenced
					output.writeString(object.stringValue);
				}

				public SampleObject1 read (Kryo kryo, Input input, Class<? extends SampleObject1> type) {
					return new SampleObject1( //
						input.readInt(), //
						input.readFloat(), //
						input.readShort(), //
						kryo.readObject(input, long[].class), //
						kryo.readObject(input, double[].class), //
						input.readString() //
					);
				}
			});
		}
	}

	@Benchmark
	public byte[] fieldSerializerSer (FieldSerializerState state, Blackhole blackhole) {
		state.kryo.writeObject(state.output, state.object);
		state.output.close();
		return state.buffer;
	}

	@Benchmark
	public byte[] compatibleFieldSerializerSer (CompatibleFieldSerializerState state) {
		state.kryo.writeObject(state.output, state.object);
		state.output.close();
		return state.buffer;
	}

	@Benchmark
	public byte[] taggedFieldSerializerSer (TaggedFieldSerializerState state) {
		state.kryo.writeObject(state.output, state.object);
		state.output.close();
		return state.buffer;
	}

	@Benchmark
	public byte[] versionFieldSerializerSer (VersionFieldSerializerState state) {
		state.kryo.writeObject(state.output, state.object);
		state.output.close();
		return state.buffer;
	}

	@Benchmark
	public byte[] customSerializerSer (CustomSerializerState state) {
		state.kryo.writeObject(state.output, state.object);
		state.output.close();
		return state.buffer;
	}
}
