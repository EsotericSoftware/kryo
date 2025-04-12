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

import static com.esotericsoftware.minlog.Log.*;
import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.unsafe.UnsafeByteBufferInput;
import com.esotericsoftware.kryo.unsafe.UnsafeByteBufferOutput;
import com.esotericsoftware.kryo.unsafe.UnsafeInput;
import com.esotericsoftware.kryo.unsafe.UnsafeOutput;
import com.esotericsoftware.kryo.util.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;

/** Convenience methods for round tripping objects.
 * @author Nathan Sweet */
public abstract class KryoTestCase {
	// When true, roundTrip will only do a single write/read to make debugging easier (breaks some tests).
	private static final boolean debug = false;

	protected Kryo kryo;
	protected Output output;
	protected Input input;
	protected Object object1, object2;
	protected boolean supportsCopy;

	static interface BufferFactory {
		public Output createOutput (OutputStream os);

		public Output createOutput (OutputStream os, int size);

		public Output createOutput (int size, int limit);

		public Input createInput (InputStream os, int size);

		public Input createInput (byte[] buffer);
	}

	@BeforeEach
	public void setUp () throws Exception {
		if (debug && WARN) warn("*** DEBUG TEST ***");

		kryo = new Kryo();
	}

	/** @param lengthGenerics Pass Integer.MIN_VALUE to disable checking the length for the generic serialization.
	 * @param lengthNoGenerics Pass Integer.MIN_VALUE to disable checking the length for the non-generic serialization. */
	public <T> void roundTrip (int lengthGenerics, int lengthNoGenerics, T object1) {
		roundTrip(lengthGenerics, object1, true);
		roundTrip(lengthNoGenerics, object1, false);
	}

	/** @param length Pass Integer.MIN_VALUE to disable checking the length. */
	private <T> void roundTrip (int length, T object1, boolean optimizedGenerics) {
		try {
			kryo.setOptimizedGenerics(optimizedGenerics);
			roundTrip(length, object1);
		} finally {
			kryo.setOptimizedGenerics(true);
		}
	}

	/** @param length Pass Integer.MIN_VALUE to disable checking the length. */
	public <T> T roundTrip (int length, T object1) {
		T object2 = roundTripWithBufferFactory(length, object1, new BufferFactory() {
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

		if (debug) return object2;

		roundTripWithBufferFactory(length, object1, new BufferFactory() {
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
				ByteBuffer byteBuffer = allocateByteBuffer(buffer);
				return new ByteBufferInput(byteBuffer.asReadOnlyBuffer());
			}
		});

		if (Util.isUnsafeAvailable()) {
			roundTripWithBufferFactory(length, object1, new BufferFactory() {
				public Output createOutput(OutputStream os) {
					return new UnsafeOutput(os);
				}

				public Output createOutput(OutputStream os, int size) {
					return new UnsafeOutput(os, size);
				}

				public Output createOutput(int size, int limit) {
					return new UnsafeOutput(size, limit);
				}

				public Input createInput(InputStream os, int size) {
					return new UnsafeInput(os, size);
				}

				public Input createInput(byte[] buffer) {
					return new UnsafeInput(buffer);
				}
			});
		}

		if (Util.isUnsafeAvailable()) {
			roundTripWithBufferFactory(length, object1, new BufferFactory() {
				public Output createOutput(OutputStream os) {
					return new UnsafeByteBufferOutput(os);
				}

				public Output createOutput(OutputStream os, int size) {
					return new UnsafeByteBufferOutput(os, size);
				}

				public Output createOutput(int size, int limit) {
					return new UnsafeByteBufferOutput(size, limit);
				}

				public Input createInput(InputStream os, int size) {
					return new UnsafeByteBufferInput(os, size);
				}

				public Input createInput(byte[] buffer) {
					ByteBuffer byteBuffer = allocateByteBuffer(buffer);
					return new UnsafeByteBufferInput(byteBuffer.asReadOnlyBuffer());
				}
			});
		}

		return object2;
	}

	private ByteBuffer allocateByteBuffer(byte[] buffer) {
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(buffer.length);
		byteBuffer.put(buffer);
		((Buffer) byteBuffer).flip();
		return byteBuffer;
	}

	/** @param length Pass Integer.MIN_VALUE to disable checking the length. */
	public <T> T roundTripWithBufferFactory (int length, T object1, BufferFactory sf) {
		boolean checkLength = length != Integer.MIN_VALUE;

		this.object1 = object1;

		// Test output to stream, large buffer.
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		output = sf.createOutput(outStream, 4096);
		kryo.writeClassAndObject(output, object1);
		output.flush();

		if (debug) System.out.println();

		// Test input from stream, large buffer.
		input = sf.createInput(new ByteArrayInputStream(outStream.toByteArray()), 4096);
		object2 = kryo.readClassAndObject(input);
		doAssertEquals(object1, object2);
		if (checkLength) {
			assertEquals(length, input.total(), "Incorrect number of bytes read.");
			assertEquals(length, output.total(), "Incorrect number of bytes written.");
		}

		if (debug) return (T)object2;

		// Test output to stream, small buffer.
		outStream = new ByteArrayOutputStream();
		output = sf.createOutput(outStream, 10);
		kryo.writeClassAndObject(output, object1);
		output.flush();

		// Test input from stream, small buffer.
		input = sf.createInput(new ByteArrayInputStream(outStream.toByteArray()), 10);
		object2 = kryo.readClassAndObject(input);
		doAssertEquals(object1, object2);
		if (checkLength) assertEquals(length, input.total(), "Incorrect number of bytes read.");

		if (object1 != null) {
			// Test null with serializer.
			Serializer serializer = kryo.getRegistration(object1.getClass()).getSerializer();
			output.reset();
			outStream.reset();
			kryo.writeObjectOrNull(output, null, serializer);
			output.flush();

			// Test null from byte array with and without serializer.
			input = sf.createInput(new ByteArrayInputStream(outStream.toByteArray()), 10);
			assertNull(kryo.readObjectOrNull(input, object1.getClass(), serializer));

			input = sf.createInput(new ByteArrayInputStream(outStream.toByteArray()), 10);
			assertNull(kryo.readObjectOrNull(input, object1.getClass()));
		}

		// Test output to byte array.
		output = sf.createOutput(length * 2, -1);
		kryo.writeClassAndObject(output, object1);
		output.flush();

		// Test input from byte array.
		input = sf.createInput(output.toBytes());
		object2 = kryo.readClassAndObject(input);
		doAssertEquals(object1, object2);
		if (checkLength) {
			assertEquals( length, output.total(), "Incorrect length.");
			assertEquals( length, input.total(), "Incorrect number of bytes read.");
		}
		input.reset();

		if (supportsCopy) {
			// Test copy.
			T copy = kryo.copy(object1);
			doAssertEquals(object1, copy);
			copy = kryo.copyShallow(object1);
			doAssertEquals(object1, copy);
		}

		// Ensure generic types are balanced after each round of serialization
		assertEquals(0, kryo.getGenerics().getGenericTypesSize());

		return (T)object2;
	}

	protected void doAssertEquals (Object object1, Object object2) {
		assertEquals(arrayToList(object1), arrayToList(object2));
	}

	public static Object arrayToList (Object array) {
		if (array == null || !array.getClass().isArray()) return array;
		ArrayList list = new ArrayList(Array.getLength(array));
		for (int i = 0, n = Array.getLength(array); i < n; i++)
			list.add(arrayToList(Array.get(array, i)));
		return list;
	}

	public static ArrayList list (Object... items) {
		ArrayList list = new ArrayList();
		for (Object item : items)
			list.add(item);
		return list;
	}
}
