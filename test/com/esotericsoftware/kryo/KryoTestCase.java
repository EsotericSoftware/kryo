/* Copyright (c) 2008, Nathan Sweet
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;

import org.junit.Assert;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.FastInput;
import com.esotericsoftware.kryo.io.FastOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.esotericsoftware.kryo.io.UnsafeMemoryInput;
import com.esotericsoftware.kryo.io.UnsafeMemoryOutput;
import com.esotericsoftware.kryo.io.UnsafeOutput;

import junit.framework.TestCase;

/** Convenience methods for round tripping objects.
 * 
 * @author Nathan Sweet <misc@n4te.com> */
abstract public class KryoTestCase extends TestCase {
	protected Kryo kryo;
	private Output output;
	private Input input;
	private Object object1, object2;
	protected boolean supportsCopy;

	static interface StreamFactory {
		public Output createOutput (OutputStream os);

		public Output createOutput (OutputStream os, int size);

		public Output createOutput (int size, int limit);

		public Input createInput (InputStream os, int size);

		public Input createInput (byte[] buffer);
	}

	protected void setUp () throws Exception {
		// Log.TRACE();

		kryo = new Kryo();
		kryo.setReferences(false);
		kryo.setRegistrationRequired(true);
		// kryo.useAsmBackend(false);
	}

	static class RoundTripOutput<T> {
		private final Input input;
		private final T deserializeObject;

		private RoundTripOutput (Input input, T object) {
			this.input = input;
			this.deserializeObject = object;
		}

		Input getKryoInput () {
			return input;
		}

		T getDeserializeObject () {
			return deserializeObject;
		}
	}

	public <T> RoundTripOutput<T> roundTrip (int length, int unsafeLength, T object1) {

		roundTripWithStreamFactory(unsafeLength, object1, new StreamFactory() {
			public Output createOutput (OutputStream os) {
				return new UnsafeMemoryOutput(os);
			}

			public Output createOutput (OutputStream os, int size) {
				return new UnsafeMemoryOutput(os, size);
			}

			public Output createOutput (int size, int limit) {
				return new UnsafeMemoryOutput(size, limit);
			}

			public Input createInput (InputStream os, int size) {
				return new UnsafeMemoryInput(os, size);
			}

			public Input createInput (byte[] buffer) {
				return new UnsafeMemoryInput(buffer);
			}
		});

		roundTripWithStreamFactory(unsafeLength, object1, new StreamFactory() {
			public Output createOutput (OutputStream os) {
				return new UnsafeOutput(os);
			}

			public Output createOutput (OutputStream os, int size) {
				return new UnsafeOutput(os, size);
			}

			public Output createOutput (int size, int limit) {
				return new UnsafeOutput(size, limit);
			}

			public Input createInput (InputStream os, int size) {
				return new UnsafeInput(os, size);
			}

			public Input createInput (byte[] buffer) {
				return new UnsafeInput(buffer);
			}
		});

		roundTripWithStreamFactory(length, object1, new StreamFactory() {
			public Output createOutput (OutputStream os) {
				return new ByteBufferOutput(os);
			}

			public Output createOutput (OutputStream os, int size) {
				return new ByteBufferOutput(os, size);
			}

			public Output createOutput (int size, int limit) {
				return new ByteBufferOutput(size, limit);
			}

			public Input createInput (InputStream os, int size) {
				return new ByteBufferInput(os, size);
			}

			public Input createInput (byte[] buffer) {
				return new ByteBufferInput(buffer);
			}
		});

		roundTripWithStreamFactory(unsafeLength, object1, new StreamFactory() {
			public Output createOutput (OutputStream os) {
				return new FastOutput(os);
			}

			public Output createOutput (OutputStream os, int size) {
				return new FastOutput(os, size);
			}

			public Output createOutput (int size, int limit) {
				return new FastOutput(size, limit);
			}

			public Input createInput (InputStream os, int size) {
				return new FastInput(os, size);
			}

			public Input createInput (byte[] buffer) {
				return new FastInput(buffer);
			}
		});

		return roundTripWithStreamFactory(length, object1, new StreamFactory() {
			public Output createOutput (OutputStream os) {
				return new Output(os);
			}

			public Output createOutput (OutputStream os, int size) {
				return new Output(os, size);
			}

			public Output createOutput (int size, int limit) {
				return new Output(size, limit);
			}

			public Input createInput (InputStream os, int size) {
				return new Input(os, size);
			}

			public Input createInput (byte[] buffer) {
				return new Input(buffer);
			}
		});
	}

	public <T> RoundTripOutput<T> roundTripWithStreamFactory (int length, T object1, StreamFactory sf) {
		this.object1 = object1;

		// Test output to stream, large buffer.
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		output = sf.createOutput(outStream, 4096);
		kryo.writeClassAndObject(output, object1);
		output.flush();

		// Test input from stream, large buffer.
		byte[] out = outStream.toByteArray();
		input = sf.createInput(new ByteArrayInputStream(outStream.toByteArray()), 4096);
		object2 = kryo.readClassAndObject(input);
		assertEquals("Incorrect number of bytes read.", length, input.total());
		assertEquals("Incorrect number of bytes written.", length, output.total());
		doAssertEquals(object1, object2);

		// Test output to stream, small buffer.
		outStream = new ByteArrayOutputStream();
		output = sf.createOutput(outStream, 10);
		kryo.writeClassAndObject(output, object1);
		output.flush();

		// Test input from stream, small buffer.
		input = sf.createInput(new ByteArrayInputStream(outStream.toByteArray()), 10);
		object2 = kryo.readClassAndObject(input);
		assertEquals("Incorrect number of bytes read.", length, input.total());
		doAssertEquals(object1, object2);

		if (object1 != null) {
			// Test null with serializer.
			Serializer serializer = kryo.getRegistration(object1.getClass()).getSerializer();
			output.clear();
			outStream.reset();
			kryo.writeObjectOrNull(output, null, serializer);
			output.flush();

			// Test null from byte array with and without serializer.
			input = sf.createInput(new ByteArrayInputStream(outStream.toByteArray()), 10);
			assertEquals(null, kryo.readObjectOrNull(input, object1.getClass(), serializer));

			input = sf.createInput(new ByteArrayInputStream(outStream.toByteArray()), 10);
			assertEquals(null, kryo.readObjectOrNull(input, object1.getClass()));
		}

		// Test output to byte array.
		output = sf.createOutput(length * 2, -1);
		kryo.writeClassAndObject(output, object1);
		output.flush();

		// Test input from byte array.
		input = sf.createInput(output.toBytes());
		object2 = kryo.readClassAndObject(input);
		doAssertEquals(object1, object2);
		assertEquals("Incorrect length.", length, output.total());
		assertEquals("Incorrect number of bytes read.", length, input.total());
		input.rewind();

		if (supportsCopy) {
			// Test copy.
			T copy = kryo.copy(object1);
			doAssertEquals(object1, copy);
			copy = kryo.copyShallow(object1);
			doAssertEquals(object1, copy);
		}

		return new RoundTripOutput<>(input, (T)object2);
	}

	protected void doAssertEquals (Object object1, Object object2) {
		Assert.assertEquals(arrayToList(object1), arrayToList(object2));
	}

	static public void assertEquals (Object object1, Object object2) {
		Assert.assertEquals(arrayToList(object1), arrayToList(object2));
	}

	static public Object arrayToList (Object array) {
		if (array == null || !array.getClass().isArray()) return array;
		ArrayList list = new ArrayList(Array.getLength(array));
		for (int i = 0, n = Array.getLength(array); i < n; i++)
			list.add(arrayToList(Array.get(array, i)));
		return list;
	}

	static public ArrayList list (Object... items) {
		ArrayList list = new ArrayList();
		for (Object item : items)
			list.add(item);
		return list;
	}
}
