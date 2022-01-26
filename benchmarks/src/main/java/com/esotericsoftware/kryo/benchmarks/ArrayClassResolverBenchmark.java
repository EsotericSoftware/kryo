/* Copyright (c) 2008-2020, Nathan Sweet
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
import com.esotericsoftware.kryo.util.ArrayClassResolver;
import org.openjdk.jmh.annotations.*;

import java.util.*;

import static com.esotericsoftware.kryo.benchmarks.FieldSerializerBenchmark.*;

/**
 * {@link ArrayClassResolver} is fast especially in {@link #deserializeCollection(DeserializeCollectionStateArray)}.
 *
 * <pre>
 * new module/old module=4125/3511=17% faster in a environment.
 * Benchmark                                           Mode  Cnt     Score   Error  Units
 * ArrayClassResolverBenchmark.deserializeCollection  thrpt       4125.996          ops/s
 * FieldSerializerBenchmark.deserializeCollection     thrpt       3511.014          ops/s
 * </pre>
 *
 * @author lifeinwild1@gmail.com
 */
public class ArrayClassResolverBenchmark {
	@Benchmark
	public void field (FieldSerializerStateArray state) {
		state.roundTrip();
	}

	@Benchmark
	public void compatible (CompatibleStateArray state) {
		state.roundTrip();
	}

	@Benchmark
	public void tagged (TaggedStateArray state) {
		state.roundTrip();
	}

	@Benchmark
	public void version (VersionStateArray state) {
		state.roundTrip();
	}

	@Benchmark
	public void custom (CustomStateArray state) {
		state.roundTrip();
	}

	@Benchmark
	public void deserializeCollection(DeserializeCollectionStateArray state) { state.roundTrip(); }

	//

	public static Kryo createKryoArray(){
		return new Kryo(new ArrayClassResolver(), null);
	}

	/**
	 * ad-hoc code for profiling
	 */
	public static void main(String args[]){
		//ArrayClassResolver is efficient for collection.
		Map<Long, String> m = new HashMap<>();

		//The performance difference is proportional to the size of collection.
		int size = 2000;
		for(long i=0;i<size;i++)
			m.put(i, "abcdefhijklmnopqrstuvwxyz");

		//comment out either.
//		Kryo k = new Kryo();
		Kryo k = new Kryo(new ArrayClassResolver(), null);

		k.register(HashMap.class);

		System.out.println("serialize");

		//no difference in terms of performance in serializing.
		int loop = 1;
		int buf = 1000 * 100;
		Output out = new Output(buf);
		for(int i=0;i<loop;i++) {
			out.setPosition(0);
			k.writeClassAndObject(out, m);
		}

		//set a breakpoint here for configuring profiler
		System.out.println("deserialize");

		//measure this loop by profiler
		int loop2 = 1000 * 1000;
		for(int i=0;i<loop2;i++) {
			Input in = new Input(out.getBuffer());
			HashMap<Long, String> deserialized = (HashMap<Long, String>)k.readClassAndObject(in);
			if(!m.equals(deserialized)){
				System.out.println("error");
			}
		}

		/*
		Result:
											Total Time	Invocations
		DefaultClassResolver.readClass()	19,872 ms 	10,006,631
		ArrayClassResolver.readClass()		12,371 ms 	10,029,051		60% faster
	    */
	}

	static public class FieldSerializerStateArray extends FieldSerializerState {
		@Override
		public Kryo createKryo() {
			return createKryoArray();
		}
	}

	static public class CompatibleStateArray extends CompatibleState {
		@Override
		public Kryo createKryo() {
			return createKryoArray();
		}
	}

	static public class TaggedStateArray extends TaggedState {
		@Override
		public Kryo createKryo() {
			return createKryoArray();
		}
	}

	static public class VersionStateArray extends VersionState {
		@Override
		public Kryo createKryo() {
			return createKryoArray();
		}
	}

	static public class CustomStateArray extends CustomState {
		@Override
		public Kryo createKryo() {
			return createKryoArray();
		}
	}

	static public class DeserializeCollectionStateArray extends DeserializeCollectionState {
		@Override
		protected Kryo createKryo() {
			return createKryoArray();
		}
	}

	@State(Scope.Thread)
	static public abstract class DeserializeCollectionState{
		final Kryo kryo = createKryo();
		final Output output = new Output(1024 * 1024);
		final Input input = new Input(output.getBuffer());
		Object object;

		abstract protected Kryo createKryo();

		@Setup(Level.Trial)
		public void setup () {
			HashMap<Long, String> m = new HashMap<>();
			for(long i=0;i<2000;i++)
				m.put(i, "val");

			object = m;

			kryo.register(HashMap.class);

			output.setPosition(0);
			kryo.writeObject(output, object);
		}

		public void roundTrip() {
			input.setPosition(0);
			input.setLimit(output.position());
			kryo.readObject(input, object.getClass());
		}
	}
}
