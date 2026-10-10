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
import com.esotericsoftware.kryo.SerializerFactory.FieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.TaggedFieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.VersionFieldSerializerFactory;
import com.esotericsoftware.kryo.benchmarks.data.graph.ObjectGraph;
import com.esotericsoftware.kryo.bytecode.Bytecode;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;

import java.util.ArrayList;
import java.util.HashMap;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/** Round trip of an object graph of many classes, which resembles the state of an application. The classes are generated, see
 * the README. */
public class ObjectGraphBenchmark {
	@Benchmark
	public Object field (FieldSerializerState state) {
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

	@Benchmark
	public Object copy (FieldSerializerState state) {
		return state.kryo.copy(state.object);
	}

	//

	@State(Scope.Thread)
	static public abstract class BenchmarkState {
		@Param({"true"}) public boolean references;
		/** The size of the object graph: 4 is about 1,400 objects, 16 about 5,200. */
		@Param({"4"}) public int scale;

		final Kryo kryo = new Kryo();
		final Output output = new Output(1024 * 1024, -1);
		final Input input = new Input();
		Object object;

		/** If true, the field serializers use generated code, which needs Java 24+ or ASM on the classpath. */
		@Param({"false"}) public boolean codeGeneration;

		@Setup(Level.Trial)
		public void setup () {
			if (codeGeneration && !Bytecode.classFileApi && !Bytecode.asm)
				throw new IllegalStateException("Code generation needs Java 24+ or ASM on the classpath.");
			// Before registering, because FieldSerializer decides when it is created whether String fields use references.
			kryo.setReferences(references);
			kryo.register(ArrayList.class);
			kryo.register(HashMap.class);
			kryo.register(int[].class);
			kryo.register(double[].class);
			for (Class type : ObjectGraph.classes())
				kryo.register(type);
			object = ObjectGraph.create(1, scale);
		}

		public Object roundTrip () {
			output.reset();
			kryo.writeObject(output, object);
			input.setBuffer(output.getBuffer(), 0, output.position());
			return kryo.readObject(input, ObjectGraph.class);
		}
	}

	static public class FieldSerializerState extends BenchmarkState {
		public void setup () {
			FieldSerializerFactory factory = new FieldSerializerFactory();
			factory.getConfig().setCodeGeneration(codeGeneration);
			kryo.setDefaultSerializer(factory);
			super.setup();
		}
	}

	static public class CompatibleState extends BenchmarkState {
		@Param({"false"}) public boolean chunked;

		public void setup () {
			CompatibleFieldSerializerFactory factory = new CompatibleFieldSerializerFactory();
			factory.getConfig().setCodeGeneration(codeGeneration);
			factory.getConfig().setChunkedEncoding(chunked);
			kryo.setDefaultSerializer(factory);
			super.setup();
		}
	}

	static public class TaggedState extends BenchmarkState {
		@Param({"false"}) public boolean chunked;

		public void setup () {
			TaggedFieldSerializerFactory factory = new TaggedFieldSerializerFactory();
			factory.getConfig().setCodeGeneration(codeGeneration);
			factory.getConfig().setChunkedEncoding(chunked);
			if (chunked) factory.getConfig().setReadUnknownTagData(true);
			kryo.setDefaultSerializer(factory);
			super.setup();
		}
	}

	static public class VersionState extends BenchmarkState {
		public void setup () {
			VersionFieldSerializerFactory factory = new VersionFieldSerializerFactory();
			factory.getConfig().setCodeGeneration(codeGeneration);
			kryo.setDefaultSerializer(factory);
			super.setup();
		}
	}
}
