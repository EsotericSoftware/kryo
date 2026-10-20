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

package com.esotericsoftware.kryo;

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.unsafe.UnsafeByteBufferInput;
import com.esotericsoftware.kryo.unsafe.UnsafeByteBufferOutput;
import com.esotericsoftware.kryo.unsafe.UnsafeInput;
import com.esotericsoftware.kryo.unsafe.UnsafeOutput;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Round trips a KryoSerializable object with large primitive arrays and a reference to itself through each buffer type, with
 * variable and fixed length encoding, reusing the buffers. Formerly a timed benchmark, see the benchmarks module for those.
 * @author Roman Levenstein <romixlev@gmail.com>
 * @author Nathan Sweet */
class SerializationBenchmarkTest extends KryoTestCase {
	private static final int ITERATIONS = 3;
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

	@BeforeEach
	public void setUp () throws Exception {
		super.setUp();
		kryo.register(long[].class);
		kryo.register(double[].class);
		kryo.register(SampleObject.class);
		kryo.setReferences(true);
	}

	@Test
	void testOutput () {
		Output output = new Output(OUTPUT_BUFFER_SIZE);
		run(output, new Input(output.getBuffer()));
	}

	@Test
	void testOutputFixed () {
		Output output = new Output(OUTPUT_BUFFER_SIZE);
		Input input = new Input(output.getBuffer());
		input.setVariableLengthEncoding(false);
		output.setVariableLengthEncoding(false);
		run(output, input);
	}

	@Test
	void testByteBufferOutput () {
		ByteBufferOutput output = new ByteBufferOutput(OUTPUT_BUFFER_SIZE);
		run(output, new ByteBufferInput(output.getByteBuffer()));
	}

	@Test
	void testByteBufferOutputFixed () {
		ByteBufferOutput output = new ByteBufferOutput(OUTPUT_BUFFER_SIZE);
		ByteBufferInput input = new ByteBufferInput(output.getByteBuffer());
		input.setVariableLengthEncoding(false);
		output.setVariableLengthEncoding(false);
		run(output, input);
	}

	@Test
	@Unsafe
	void testUnsafeOutput () {
		UnsafeOutput output = new UnsafeOutput(OUTPUT_BUFFER_SIZE);
		run(output, new UnsafeInput(output.getBuffer()));
	}

	@Test
	@Unsafe
	void testUnsafeOutputFixed () {
		UnsafeOutput output = new UnsafeOutput(OUTPUT_BUFFER_SIZE);
		UnsafeInput input = new UnsafeInput(output.getBuffer());
		input.setVariableLengthEncoding(false);
		output.setVariableLengthEncoding(false);
		run(output, input);
	}

	@Test
	@Unsafe
	void testUnsafeByteBufferOutput () {
		UnsafeByteBufferOutput output = new UnsafeByteBufferOutput(OUTPUT_BUFFER_SIZE);
		run(output, new UnsafeByteBufferInput(output.getByteBuffer()));
	}

	@Test
	@Unsafe
	void testUnsafeByteBufferOutputFixed () {
		UnsafeByteBufferOutput output = new UnsafeByteBufferOutput(OUTPUT_BUFFER_SIZE);
		UnsafeByteBufferInput input = new UnsafeByteBufferInput(output.getByteBuffer());
		input.setVariableLengthEncoding(false);
		output.setVariableLengthEncoding(false);
		run(output, input);
	}

	/** Writes and reads the object a few times with the same buffers, which share their memory. */
	private void run (Output output, Input input) {
		for (int i = 0; i < ITERATIONS; i++) {
			output.setPosition(0);
			kryo.writeObject(output, object);

			input.setPosition(0);
			input.setLimit(output.position());
			SampleObject read = kryo.readObject(input, SampleObject.class);
			assertEquals(output.position(), input.position());
			assertEquals(object, read);
			assertSame(read, read.selfReference);
		}
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
