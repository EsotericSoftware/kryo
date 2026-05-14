/* Copyright (c) 2008-2022, Nathan Sweet
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
import com.esotericsoftware.kryo.SerializerFactory.CompatibleFieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.TaggedFieldSerializerFactory;
import com.esotericsoftware.kryo.benchmarks.data.Image;
import com.esotericsoftware.kryo.benchmarks.data.Image.Size;
import com.esotericsoftware.kryo.benchmarks.data.Media;
import com.esotericsoftware.kryo.benchmarks.data.Media.Player;
import com.esotericsoftware.kryo.benchmarks.data.MediaContent;
import com.esotericsoftware.kryo.benchmarks.data.Sample;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.RecordSerializer;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;

import java.util.ArrayList;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class RecordSerializerBenchmark {
	@Benchmark
	public void field (FieldSerializerState state) {
		state.roundTrip();
	}

	@Benchmark
	public void record (RecordSerializerState state) {
		state.roundTrip();
	}

	@State(Scope.Thread)
	static public abstract class BenchmarkState {
		@Param({"true", "false"}) public boolean references;

		final Kryo kryo = new Kryo();
		final Output output = new Output(1024 * 512);
		final Input input = new Input(output.getBuffer());
		Object object;

		@Setup(Level.Trial)
		public void setup () {
			object = new RecordRectangle("2134324", 10, 10L, 20D);
			kryo.setReferences(references);
		}

		public void roundTrip () {
			output.setPosition(0);
			kryo.writeObject(output, object);
			input.setPosition(0);
			input.setLimit(output.position());
			kryo.readObject(input, object.getClass());
		}
	}

	public record RecordRectangle (String height, int width, long x, double y) { }

	static public class FieldSerializerState extends BenchmarkState {
		public void setup () {
			kryo.setDefaultSerializer(FieldSerializer.class);
			kryo.register(RecordRectangle.class);
			super.setup();
		}
	}

	static public class RecordSerializerState extends BenchmarkState {
		public void setup () {
			kryo.register(RecordRectangle.class, new RecordSerializer<>());
			super.setup();
		}
	}
}
