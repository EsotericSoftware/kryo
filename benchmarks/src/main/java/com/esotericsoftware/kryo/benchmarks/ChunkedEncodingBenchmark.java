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
import com.esotericsoftware.kryo.SerializerFactory.CompatibleFieldSerializerFactory;
import com.esotericsoftware.kryo.benchmarks.data.Image;
import com.esotericsoftware.kryo.benchmarks.data.Image.Size;
import com.esotericsoftware.kryo.benchmarks.data.Media;
import com.esotericsoftware.kryo.benchmarks.data.Media.Player;
import com.esotericsoftware.kryo.benchmarks.data.MediaContent;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.ArrayList;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/** Reads data written by CompatibleFieldSerializer with chunked encoding, with and without skipping a field whose class is
 * unknown when reading. */
public class ChunkedEncodingBenchmark {
	@Benchmark
	public Object read (BenchmarkState state) {
		return state.read(state.kryo);
	}

	/** The class of the field with the media content is unknown, so the field is skipped. */
	@Benchmark
	public Object skip (BenchmarkState state) {
		return state.read(state.skipKryo);
	}

	@State(Scope.Thread)
	static public class BenchmarkState {
		@Param({"false", "true"}) public boolean legacyChunks;
		@Param({"false", "true"}) public boolean references;

		Kryo kryo, skipKryo;
		final Output output = new Output(1024 * 512);
		final Input input = new Input();

		@Setup(Level.Trial)
		public void setup () {
			kryo = kryo(true);
			skipKryo = kryo(false);
			Envelope envelope = new Envelope();
			envelope.id = 1;
			envelope.content = new MediaContent().populate(references);
			envelope.name = "envelope";
			kryo.writeObject(output, envelope);
			input.setBuffer(output.getBuffer(), 0, output.position());
		}

		private Kryo kryo (boolean registerContent) {
			Kryo kryo = new Kryo();
			CompatibleFieldSerializerFactory factory = new CompatibleFieldSerializerFactory();
			factory.getConfig().setChunkedEncoding(true);
			factory.getConfig().setLegacyChunks(legacyChunks);
			kryo.setDefaultSerializer(factory);
			kryo.setReferences(references);
			kryo.register(Envelope.class, 20);
			if (registerContent) {
				kryo.register(MediaContent.class, 21);
				kryo.register(Media.class, 22);
				kryo.register(Image.class, 23);
				kryo.register(Size.class, 24);
				kryo.register(Player.class, 25);
				kryo.register(ArrayList.class, 26);
			}
			return kryo;
		}

		Object read (Kryo kryo) {
			input.setPosition(0);
			return kryo.readObject(input, Envelope.class);
		}
	}

	static public class Envelope {
		public int id;
		public MediaContent content;
		public String name;
	}
}
