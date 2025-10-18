/* Copyright (c) 2008-2025, Nathan Sweet
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

package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.unsafe.UnsafeByteBufferInput;
import com.esotericsoftware.kryo.unsafe.UnsafeByteBufferOutput;
import com.esotericsoftware.kryo.unsafe.UnsafeInput;
import com.esotericsoftware.kryo.unsafe.UnsafeOutput;
import com.esotericsoftware.minlog.Log;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** Timed Kryo serialization with various buffers and settings.
 * @author Roman Levenstein <romixlev@gmail.com>
 * @author Nathan Sweet */
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class SerializationBenchmarkTest extends KryoTestCase {
	private static final int WARMUP_ITERATIONS = 1000;

	/** Number of runs. */
	private static final int RUN_CNT = 1;

	/** Number of iterations. Set it to something rather big for obtaining meaningful results */
	private static final int ITER_CNT = 100;
	// static private final int ITER_CNT = 20000;

	private static final int SLEEP_BETWEEN_RUNS = 100;

	private static final int OUTPUT_BUFFER_SIZE = 4096 * 10 * 4;

	final SampleObject object = createObject();

	private static SampleObject createObject () {
		long[] longArray = new long[3000];
		for (int i = 0; i < longArray.length; i++)
			longArray[i] = i;

		double[] doubleArray = new double[3000];
		for (int i = 0; i < doubleArray.length; i++)
			doubleArray[i] = 0.1 * i;

		return new SampleObject(123, 123.456f, (short)321, longArray, doubleArray);
	}

	@Test
	void testOutput () throws Exception {
		Output output = new Output(OUTPUT_BUFFER_SIZE);
		Input input = new Input(output.getBuffer());
		run("Output", 1, WARMUP_ITERATIONS, output, input, false);
		run("Output", RUN_CNT, ITER_CNT, output, input, true);
	}

	@Test
	void testOutputFixed () throws Exception {
		Output output = new Output(OUTPUT_BUFFER_SIZE);
		Input input = new Input(output.getBuffer());
		input.setVariableLengthEncoding(false);
		output.setVariableLengthEncoding(false);
		run("OutputFixed", 1, WARMUP_ITERATIONS, output, input, false);
		run("OutputFixed", RUN_CNT, ITER_CNT, output, input, true);
	}

	@Test
	void testByteBufferOutput () throws Exception {
		ByteBufferOutput output = new ByteBufferOutput(OUTPUT_BUFFER_SIZE);
		ByteBufferInput input = new ByteBufferInput(output.getByteBuffer());
		run("ByteBufferOutput", 1, WARMUP_ITERATIONS, output, input, false);
		run("ByteBufferOutput", RUN_CNT, ITER_CNT, output, input, true);
	}

	@Test
	void testByteBufferOutputFixed () throws Exception {
		ByteBufferOutput output = new ByteBufferOutput(OUTPUT_BUFFER_SIZE);
		ByteBufferInput input = new ByteBufferInput(output.getByteBuffer());
		input.setVariableLengthEncoding(false);
		output.setVariableLengthEncoding(false);
		run("ByteBufferOutputFixed", 1, WARMUP_ITERATIONS, output, input, false);
		run("ByteBufferOutputFixed", RUN_CNT, ITER_CNT, output, input, true);
	}

	@Test
	@Unsafe
	void testUnsafeOutput () throws Exception {
		UnsafeOutput output = new UnsafeOutput(OUTPUT_BUFFER_SIZE);
		UnsafeInput input = new UnsafeInput(output.getBuffer());
		run("UnsafeOutput", 1, WARMUP_ITERATIONS, output, input, false);
		run("UnsafeOutput", RUN_CNT, ITER_CNT, output, input, true);
	}

	@Test
	@Unsafe
	void testUnsafeOutputFixed () throws Exception {
		UnsafeOutput output = new UnsafeOutput(OUTPUT_BUFFER_SIZE);
		UnsafeInput input = new UnsafeInput(output.getBuffer());
		input.setVariableLengthEncoding(false);
		output.setVariableLengthEncoding(false);
		run("UnsafeOutputFixed", 1, WARMUP_ITERATIONS, output, input, false);
		run("UnsafeOutputFixed", RUN_CNT, ITER_CNT, output, input, true);
	}

	@Test
	@Unsafe
	void testUnsafeByteBufferOutput () throws Exception {
		UnsafeByteBufferOutput output = new UnsafeByteBufferOutput(OUTPUT_BUFFER_SIZE);
		UnsafeByteBufferInput input = new UnsafeByteBufferInput(output.getByteBuffer());
		run("UnsafeByteBufferOutput", 1, WARMUP_ITERATIONS, output, input, false);
		run("UnsafeByteBufferOutput", RUN_CNT, ITER_CNT, output, input, true);
	}

	@Test
	@Unsafe
	void testUnsafeByteBufferOutputFixed () throws Exception {
		UnsafeByteBufferOutput output = new UnsafeByteBufferOutput(OUTPUT_BUFFER_SIZE);
		UnsafeByteBufferInput input = new UnsafeByteBufferInput(output.getByteBuffer());
		input.setVariableLengthEncoding(false);
		output.setVariableLengthEncoding(false);
		run("UnsafeByteBufferOutputFixed", 1, WARMUP_ITERATIONS, output, input, false);
		run("UnsafeByteBufferOutputFixed", RUN_CNT, ITER_CNT, output, input, true);
	}

	private void cleanUpAfterRun () throws InterruptedException {
		System.gc();
		Thread.sleep(SLEEP_BETWEEN_RUNS);
		System.gc();
	}

	private void run (String name, int runCount, int iterations, Output output, Input input, boolean print) throws Exception {
		Kryo kryo = new Kryo();
		kryo.register(long[].class);
		kryo.register(double[].class);
		kryo.register(SampleObject.class);
		kryo.setReferences(true);

		long average = 0;
		long best = Long.MAX_VALUE;
		for (int i = 0; i < runCount; i++) {
			SampleObject object2 = null;

			long start = System.nanoTime();

			for (int j = 0; j < iterations; j++) {
				output.setPosition(0);
				kryo.writeObject(output, object);

				input.setPosition(0);
				input.setLimit(output.position());
				object2 = kryo.readObject(input, SampleObject.class);
			}

			long duration = System.nanoTime() - start;
			duration = TimeUnit.NANOSECONDS.toMillis(duration);

			// Check that unmarshalled object is equal to original one (should never fail).
			if (!object.equals(object2)) throw new RuntimeException("Unmarshalled object is not equal to original object.");

			if (print) System.out.format("%s (run %d): %,d ms\n", name, i + 1, duration);
			average += duration;
			best = Math.min(best, duration);
			cleanUpAfterRun();
		}

		average /= runCount;

		if (print) {
			System.out.format("%s (average): %,d ms\n", name, average);
			System.out.format("%s (best time): %,d ms\n\n", name, best);
		}
	}

	public void setUp () throws Exception {
		super.setUp();
		Log.WARN();
	}

	public static class SampleObject implements Externalizable, KryoSerializable {
		private int intValue;
		public float floatValue;
		protected Short shortValue;
		long[] longArray;
		private double[] doubleArray;
		private SampleObject selfReference;

		public SampleObject () {
		}

		SampleObject (int intVal, float floatValue, Short shortValue, long[] longArray, double[] doubleArray) {
			this.intValue = intVal;
			this.floatValue = floatValue;
			this.shortValue = shortValue;
			this.longArray = longArray;
			this.doubleArray = doubleArray;
			selfReference = this;
		}

		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SampleObject other = (SampleObject)o;
			assert this == selfReference;
			assert other == other.selfReference;
			return intValue == other.intValue && floatValue == other.floatValue && shortValue.equals(other.shortValue)
				&& Arrays.equals(doubleArray, other.doubleArray) && Arrays.equals(longArray, other.longArray);
		}

		// Required by Kryo serialization.
		public void read (Kryo kryo, Input in) {
			intValue = kryo.readObject(in, Integer.class);
			floatValue = kryo.readObject(in, Float.class);
			shortValue = kryo.readObject(in, Short.class);
			longArray = kryo.readObject(in, long[].class);
			doubleArray = kryo.readObject(in, double[].class);
			selfReference = kryo.readObject(in, SampleObject.class);
		}

		// Required by Java Externalizable.
		public void readExternal (ObjectInput in) throws IOException, ClassNotFoundException {
			intValue = in.readInt();
			floatValue = in.readFloat();
			shortValue = in.readShort();
			longArray = (long[])in.readObject();
			doubleArray = (double[])in.readObject();
			selfReference = (SampleObject)in.readObject();
		}

		// Required by Kryo serialization.
		public void write (Kryo kryo, Output out) {
			kryo.writeObject(out, intValue);
			kryo.writeObject(out, floatValue);
			kryo.writeObject(out, shortValue);
			kryo.writeObject(out, longArray);
			kryo.writeObject(out, doubleArray);
			kryo.writeObject(out, selfReference);
		}

		// Required by Java Externalizable.
		public void writeExternal (ObjectOutput out) throws IOException {
			out.writeInt(intValue);
			out.writeFloat(floatValue);
			out.writeShort(shortValue);
			out.writeObject(longArray);
			out.writeObject(doubleArray);
			out.writeObject(selfReference);
		}
	}
}
