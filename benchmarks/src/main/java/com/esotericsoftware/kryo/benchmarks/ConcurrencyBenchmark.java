
package com.esotericsoftware.kryo.benchmarks;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@State(Scope.Benchmark)
public class ConcurrencyBenchmark {

	@Benchmark
	public void single (SingleThreadState state, Blackhole blackhole) {
		final String data = UUID.randomUUID().toString();
		final Object result = state.roundTrip(data);
		blackhole.consume(result);
	}

	@Benchmark
	public void threadLocal (ThreadLocalState state, Blackhole blackhole) {
		final String data = UUID.randomUUID().toString();
		final Object result = state.roundTrip(data);
		blackhole.consume(result);
	}

	@Benchmark
	public void pool (PoolState state, Blackhole blackhole) {
		final String data = UUID.randomUUID().toString();
		final Object result = state.roundTrip(data);
		blackhole.consume(result);
	}

	@State(Scope.Benchmark)
	public static class SingleThreadState extends AbstractConcurrencyState {

		final Kryo kryo = createKryo();
		final Output output = createOutput();
		final Input input = createInput();

		public Object roundTrip (Object data) {
			synchronized (kryo) {
				final byte[] result = serialize(kryo, output, data);
				return deserialize(kryo, input, result);
			}
		}
	}

	@State(Scope.Benchmark)
	public static class ThreadLocalState extends AbstractConcurrencyState {

		static final ThreadLocal<Kryo> kryo = ThreadLocal.withInitial(ThreadLocalState::createKryo);
		static final ThreadLocal<Output> output = ThreadLocal.withInitial(ThreadLocalState::createOutput);
		static final ThreadLocal<Input> input = ThreadLocal.withInitial(ThreadLocalState::createInput);

		public Object roundTrip (Object data) {
			final Kryo k = kryo.get();
			final Output out = output.get();
			final Input in = input.get();
			final byte[] result = serialize(k, out, data);
			return deserialize(k, in, result);
		}
	}

	@State(Scope.Benchmark)
	public static class PoolState extends AbstractConcurrencyState {

		static final int CAPACITY = 8;

		static final Pool<Kryo> kryoPool = new Pool<Kryo>(true, false, CAPACITY) {
			protected Kryo create () {
				return createKryo();
			}
		};
		static final Pool<Output> outputPool = new Pool<Output>(true, false, CAPACITY) {
			protected Output create () {
				return createOutput();
			}
		};
		static final Pool<Input> inputPool = new Pool<Input>(true, false, CAPACITY) {
			protected Input create () {
				return createInput();
			}
		};

		public Object roundTrip (Object data) {
			final Kryo k = kryoPool.obtain();
			final Output out = outputPool.obtain();
			final Input in = inputPool.obtain();
			try {
				final byte[] result = serialize(k, out, data);
				outputPool.free(out);
				return deserialize(k, in, result);
			} finally {
				kryoPool.free(k);
				inputPool.free(in);
			}
		}
	}

	public abstract static class AbstractConcurrencyState {

		public abstract Object roundTrip (Object data);

		byte[] serialize (Kryo kryo, Output output, Object data) {
			try (final ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
				output.setOutputStream(stream);
				kryo.writeClassAndObject(output, data);
				output.flush();
				return stream.toByteArray();
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				output.reset();
			}
		}

		Object deserialize (Kryo kryo, Input input, byte[] data) {
			try {
				input.setBuffer(data);
				return kryo.readClassAndObject(input);
			} finally {
				input.reset();
			}
		}

		static Kryo createKryo () {
			final Kryo kryo = new Kryo();
			kryo.register(ArrayList.class);
			return kryo;
		}

		static Output createOutput () {
			return new Output(4096, -1);
		}

		static Input createInput () {
			return new Input(4096);
		}
	}

	public static void main (String[] args) throws RunnerException {
		final Options opt = new OptionsBuilder()
			.include(".*" + ConcurrencyBenchmark.class.getSimpleName() + ".*")
			.warmupIterations(3)
			.warmupTime(TimeValue.seconds(3))
			.measurementIterations(3)
			.measurementTime(TimeValue.seconds(3))
			.threads(2)
			.forks(1)
			.build();
		new Runner(opt).run();

	}
}
