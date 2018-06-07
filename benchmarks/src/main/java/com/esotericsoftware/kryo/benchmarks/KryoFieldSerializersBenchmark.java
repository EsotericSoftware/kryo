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
import com.esotericsoftware.kryo.benchmarks.data.Sample;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

public class KryoFieldSerializersBenchmark {
	@State(Scope.Thread)
	static abstract class BenchmarkState {
		final Kryo kryo;
		final Output output;
		final Input input;
		final Sample object;

		public BenchmarkState () {
			kryo = new Kryo();
			kryo.register(double[].class);
			kryo.register(int[].class);
			kryo.register(long[].class);
			kryo.register(float[].class);
			kryo.register(double[].class);
			kryo.register(short[].class);
			kryo.register(char[].class);
			kryo.register(boolean[].class);
			kryo.register(Sample.class);

			output = new Output(1024);
			input = new Input(output.getBuffer());

			object = new Sample();
			object.defaultValues();
		}

		@Setup(Level.Trial)
		abstract public void setup ();

		@TearDown(Level.Invocation)
		public void reset () {
			input.setPosition(0);
			input.setLimit(output.position());
			Sample object2 = kryo.readObject(input, Sample.class);
			if (!object.equals(object2)) throw new RuntimeException();

			output.setPosition(0);
		}
	}

	static public class FieldSerializerState extends BenchmarkState {
		public void setup () {
			kryo.setDefaultSerializer(FieldSerializer.class);
		}
	}

	static public class CompatibleFieldSerializerState extends BenchmarkState {
		public void setup () {
			kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
		}
	}

	static public class TaggedFieldSerializerState extends BenchmarkState {
		public void setup () {
			kryo.setDefaultSerializer(TaggedFieldSerializer.class);
		}
	}

	static public class VersionFieldSerializerState extends BenchmarkState {
		public void setup () {
			kryo.setDefaultSerializer(VersionFieldSerializer.class);
		}
	}

	static public class CustomSerializerState extends BenchmarkState {
		public void setup () {
			kryo.setDefaultSerializer(FieldSerializer.class);
			kryo.register(Sample.class, new Serializer<Sample>() {
				public void write (Kryo kryo, Output output, Sample object) {
					output.writeInt(object.intValue);
					output.writeLong(object.longValue);
					output.writeFloat(object.floatValue);
					output.writeDouble(object.doubleValue);
					output.writeShort(object.shortValue);
					output.writeChar(object.charValue);
					output.writeBoolean(object.booleanValue);
					kryo.writeObject(output, object.IntValue);
					kryo.writeObject(output, object.LongValue);
					kryo.writeObject(output, object.FloatValue);
					kryo.writeObject(output, object.DoubleValue);
					kryo.writeObject(output, object.ShortValue);
					kryo.writeObject(output, object.CharValue);
					kryo.writeObject(output, object.BooleanValue);

					kryo.writeObject(output, object.intArray);
					kryo.writeObject(output, object.longArray);
					kryo.writeObject(output, object.floatArray);
					kryo.writeObject(output, object.doubleArray);
					kryo.writeObject(output, object.shortArray);
					kryo.writeObject(output, object.charArray);
					kryo.writeObject(output, object.booleanArray);

					kryo.writeObject(output, object.string);
					kryo.writeObject(output, object.sample);
				}

				public Sample read (Kryo kryo, Input input, Class<? extends Sample> type) {
					Sample object = new Sample();
					object.intValue = input.readInt();
					object.longValue = input.readLong();
					object.floatValue = input.readFloat();
					object.doubleValue = input.readDouble();
					object.shortValue = input.readShort();
					object.charValue = input.readChar();
					object.booleanValue = input.readBoolean();
					object.IntValue = kryo.readObject(input, Integer.class);
					object.LongValue = kryo.readObject(input, Long.class);
					object.FloatValue = kryo.readObject(input, Float.class);
					object.DoubleValue = kryo.readObject(input, Double.class);
					object.ShortValue = kryo.readObject(input, Short.class);
					object.CharValue = kryo.readObject(input, Character.class);
					object.BooleanValue = kryo.readObject(input, Boolean.class);

					object.intArray = kryo.readObject(input, int[].class);
					object.longArray = kryo.readObject(input, long[].class);
					object.floatArray = kryo.readObject(input, float[].class);
					object.doubleArray = kryo.readObject(input, double[].class);
					object.shortArray = kryo.readObject(input, short[].class);
					object.charArray = kryo.readObject(input, char[].class);
					object.booleanArray = kryo.readObject(input, boolean[].class);

					object.string = kryo.readObject(input, String.class);
					object.sample = kryo.readObject(input, Sample.class);

					return object;
				}
			});
		}
	}

	@Benchmark
	public void fieldSerializerSer (FieldSerializerState state) {
		state.kryo.writeObject(state.output, state.object);
	}

	@Benchmark
	public void compatibleFieldSerializerSer (CompatibleFieldSerializerState state) {
		state.kryo.writeObject(state.output, state.object);
	}

	@Benchmark
	public void taggedFieldSerializerSer (TaggedFieldSerializerState state) {
		state.kryo.writeObject(state.output, state.object);
	}

	@Benchmark
	public void versionFieldSerializerSer (VersionFieldSerializerState state) {
		state.kryo.writeObject(state.output, state.object);
	}

	@Benchmark
	public void customSerializerSer (CustomSerializerState state) {
		state.kryo.writeObject(state.output, state.object);
	}
}
