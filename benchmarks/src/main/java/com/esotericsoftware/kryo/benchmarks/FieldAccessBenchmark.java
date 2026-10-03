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
import com.esotericsoftware.kryo.SerializerFactory.FieldSerializerFactory;
import com.esotericsoftware.kryo.benchmarks.data.Image;
import com.esotericsoftware.kryo.benchmarks.data.Image.Size;
import com.esotericsoftware.kryo.benchmarks.data.Media;
import com.esotericsoftware.kryo.benchmarks.data.Media.Player;
import com.esotericsoftware.kryo.benchmarks.data.MediaContent;
import com.esotericsoftware.kryo.benchmarks.data.Sample;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer.FieldAccessType;
import com.esotericsoftware.kryo.serializers.FieldSerializer.FieldSerializerConfig;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/** Compares the FieldSerializer field access implementations, selected with {@link FieldSerializerConfig#setFieldAccess(FieldAccessType)}. Each
 * parameter combination runs in its own fork, so the property is set before FieldSerializer reads it. */
public class FieldAccessBenchmark {
	@Benchmark
	public Object write (BenchmarkState state) {
		state.output.setPosition(0);
		state.kryo.writeObject(state.output, state.object);
		return state.output;
	}

	@Benchmark
	public Object read (BenchmarkState state) {
		state.input.setPosition(0);
		return state.kryo.readObject(state.input, state.object.getClass());
	}

	@Benchmark
	public Object copy (BenchmarkState state) {
		return state.kryo.copy(state.object);
	}

	@State(Scope.Thread)
	public static class BenchmarkState {
		@Param() public FieldAccessType fieldAccess;
		@Param() public ObjectType objectType;
		/** If true, other classes are serialized and copied before the benchmark, so that the JIT sees megamorphic call sites
		 * like in an application with many serialized classes. */
		@Param({"false", "true"}) public boolean polluted;

		Kryo kryo;
		final Output output = new Output(1024 * 512);
		Input input;
		Object object;

		@Setup(Level.Trial)
		public void setup () {
			kryo = new Kryo();
			FieldSerializerConfig config = new FieldSerializerConfig();
			config.setFieldAccess(fieldAccess);
			kryo.setDefaultSerializer(new FieldSerializerFactory(config));
			switch (objectType) {
			case sample:
				object = new Sample().populate(false);
				kryo.register(int[].class);
				kryo.register(long[].class);
				kryo.register(float[].class);
				kryo.register(double[].class);
				kryo.register(short[].class);
				kryo.register(char[].class);
				kryo.register(boolean[].class);
				kryo.register(Sample.class);
				break;
			case media:
				object = new MediaContent().populate(false);
				kryo.register(Image.class);
				kryo.register(Size.class);
				kryo.register(Media.class);
				kryo.register(Player.class);
				kryo.register(ArrayList.class);
				kryo.register(MediaContent.class);
				break;
			case privateFields:
				object = new PrivateFields().populate();
				kryo.register(PrivateFields.class);
				kryo.register(ArrayList.class);
				break;
			}

			if (polluted) pollute();

			output.setPosition(0);
			kryo.writeObject(output, object);
			input = new Input(output.toBytes());
		}

		private void pollute () {
			for (Class type : POLLUTION) {
				kryo.register(type);
				Object pollution = kryo.newInstance(type);
				for (int i = 0; i < 20_000; i++) {
					output.setPosition(0);
					kryo.writeObject(output, pollution);
					kryo.readObject(new Input(output.getBuffer(), 0, output.position()), type);
					kryo.copy(pollution);
				}
			}
		}

		public enum ObjectType {
			sample, media, privateFields
		}
	}

	static final Class[] POLLUTION = {Pollution1.class, Pollution2.class, Pollution3.class, Pollution4.class, Pollution5.class, Pollution6.class, Pollution7.class, Pollution8.class, Pollution9.class, Pollution10.class, Pollution11.class, Pollution12.class};

	public static class Pollution1 {
		public int i1 = 1;
		public long l1 = 1;
		public double d1 = 1;
		public boolean b1 = true;
		public String s1 = "s1";
		public String t1 = "t1";
		public Object o1 = "o1";
	}

	public static class Pollution2 {
		public int i2 = 2;
		public long l2 = 2;
		public double d2 = 2;
		public boolean b2 = true;
		public String s2 = "s2";
		public String t2 = "t2";
		public Object o2 = "o2";
	}

	public static class Pollution3 {
		public int i3 = 3;
		public long l3 = 3;
		public double d3 = 3;
		public boolean b3 = true;
		public String s3 = "s3";
		public String t3 = "t3";
		public Object o3 = "o3";
	}

	public static class Pollution4 {
		public int i4 = 4;
		public long l4 = 4;
		public double d4 = 4;
		public boolean b4 = true;
		public String s4 = "s4";
		public String t4 = "t4";
		public Object o4 = "o4";
	}

	public static class Pollution5 {
		public int i5 = 5;
		public long l5 = 5;
		public double d5 = 5;
		public boolean b5 = true;
		public String s5 = "s5";
		public String t5 = "t5";
		public Object o5 = "o5";
	}

	public static class Pollution6 {
		public int i6 = 6;
		public long l6 = 6;
		public double d6 = 6;
		public boolean b6 = true;
		public String s6 = "s6";
		public String t6 = "t6";
		public Object o6 = "o6";
	}

	public static class Pollution7 {
		public int i7 = 7;
		public long l7 = 7;
		public double d7 = 7;
		public boolean b7 = true;
		public String s7 = "s7";
		public String t7 = "t7";
		public Object o7 = "o7";
	}

	public static class Pollution8 {
		public int i8 = 8;
		public long l8 = 8;
		public double d8 = 8;
		public boolean b8 = true;
		public String s8 = "s8";
		public String t8 = "t8";
		public Object o8 = "o8";
	}

	public static class Pollution9 {
		public int i9 = 9;
		public long l9 = 9;
		public double d9 = 9;
		public boolean b9 = true;
		public String s9 = "s9";
		public String t9 = "t9";
		public Object o9 = "o9";
	}

	public static class Pollution10 {
		public int i10 = 10;
		public long l10 = 10;
		public double d10 = 10;
		public boolean b10 = true;
		public String s10 = "s10";
		public String t10 = "t10";
		public Object o10 = "o10";
	}

	public static class Pollution11 {
		public int i11 = 11;
		public long l11 = 11;
		public double d11 = 11;
		public boolean b11 = true;
		public String s11 = "s11";
		public String t11 = "t11";
		public Object o11 = "o11";
	}

	public static class Pollution12 {
		public int i12 = 12;
		public long l12 = 12;
		public double d12 = 12;
		public boolean b12 = true;
		public String s12 = "s12";
		public String t12 = "t12";
		public Object o12 = "o12";
	}

	/** A typical class with private fields, which can't use ASM. */
	static class PrivateFields {
		private int id;
		private long timestamp;
		private double score;
		private boolean active;
		private short count;
		private String name;
		private String description;
		private Integer boxed;
		private List<String> tags;
		private PrivateFields child;

		PrivateFields populate () {
			id = 123;
			timestamp = 1234567890123L;
			score = 1.5;
			active = true;
			count = 7;
			name = "name";
			description = "a longer description";
			boxed = 42;
			tags = new ArrayList<>(List.of("a", "b", "c"));
			child = new PrivateFields();
			child.id = 1;
			child.name = "child";
			return this;
		}
	}
}
