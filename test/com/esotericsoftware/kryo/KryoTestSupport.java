package com.esotericsoftware.kryo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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

public class KryoTestSupport {

    private final Kryo kryo;
    private final boolean supportsCopy;

	public KryoTestSupport (Kryo kryo, boolean supportsCopy) {
        this.kryo = kryo;
        this.supportsCopy = supportsCopy;
    }

	public KryoTestSupport (Kryo kryo) {
        this.kryo = kryo;
        // Java primitive boolean default
        this.supportsCopy = false;
    }

    interface StreamFactory {
        Output createOutput(OutputStream os, int size);

        Output createOutput(int size, int limit);

        Input createInput(InputStream os, int size);

        Input createInput(byte[] buffer);
    }

    public <T> RoundTripAssertionOutput<T> roundTrip(int length, int unsafeLength, T object1) {

        roundTripWithStreamFactory(unsafeLength, object1, new StreamFactory() {
            public Output createOutput(OutputStream os) {
                return new UnsafeMemoryOutput(os);
            }

            public Output createOutput(OutputStream os, int size) {
                return new UnsafeMemoryOutput(os, size);
            }

            public Output createOutput(int size, int limit) {
                return new UnsafeMemoryOutput(size, limit);
            }

            public Input createInput(InputStream os, int size) {
                return new UnsafeMemoryInput(os, size);
            }

            public Input createInput(byte[] buffer) {
                return new UnsafeMemoryInput(buffer);
            }
        });

        roundTripWithStreamFactory(unsafeLength, object1, new StreamFactory() {
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

        roundTripWithStreamFactory(length, object1, new StreamFactory() {
            public Output createOutput(OutputStream os) {
                return new ByteBufferOutput(os);
            }

            public Output createOutput(OutputStream os, int size) {
                return new ByteBufferOutput(os, size);
            }

            public Output createOutput(int size, int limit) {
                return new ByteBufferOutput(size, limit);
            }

            public Input createInput(InputStream os, int size) {
                return new ByteBufferInput(os, size);
            }

            public Input createInput(byte[] buffer) {
                return new ByteBufferInput(buffer);
            }
        });

        roundTripWithStreamFactory(unsafeLength, object1, new StreamFactory() {
            public Output createOutput(OutputStream os) {
                return new FastOutput(os);
            }

            public Output createOutput(OutputStream os, int size) {
                return new FastOutput(os, size);
            }

            public Output createOutput(int size, int limit) {
                return new FastOutput(size, limit);
            }

            public Input createInput(InputStream os, int size) {
                return new FastInput(os, size);
            }

            public Input createInput(byte[] buffer) {
                return new FastInput(buffer);
            }
        });

        return roundTripWithStreamFactory(length, object1, new StreamFactory() {
            public Output createOutput(OutputStream os) {
                return new Output(os);
            }

            public Output createOutput(OutputStream os, int size) {
                return new Output(os, size);
            }

            public Output createOutput(int size, int limit) {
                return new Output(size, limit);
            }

            public Input createInput(InputStream os, int size) {
                return new Input(os, size);
            }

            public Input createInput(byte[] buffer) {
                return new Input(buffer);
            }
        });
    }

    public <T> RoundTripAssertionOutput<T> roundTripWithStreamFactory(int length, T object1, StreamFactory sf) {

        // Test output to stream, large buffer.
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        Output output = sf.createOutput(outStream, 4096);
        kryo.writeClassAndObject(output, object1);
        output.flush();

        // Test input from stream, large buffer.
        Input input = sf.createInput(new ByteArrayInputStream(outStream.toByteArray()), 4096);
        Object object2 = kryo.readClassAndObject(input);
        Assert.assertEquals("Incorrect number of bytes read.", length, input.total());
        Assert.assertEquals("Incorrect number of bytes written.", length, output.total());
        doAssertEquals(object1, object2);

        // Test output to stream, small buffer.
        outStream = new ByteArrayOutputStream();
        output = sf.createOutput(outStream, 10);
        kryo.writeClassAndObject(output, object1);
        output.flush();

        // Test input from stream, small buffer.
        input = sf.createInput(new ByteArrayInputStream(outStream.toByteArray()), 10);
        object2 = kryo.readClassAndObject(input);
        Assert.assertEquals("Incorrect number of bytes read.", length, input.total());
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
        Assert.assertEquals("Incorrect length.", length, output.total());
        Assert.assertEquals("Incorrect number of bytes read.", length, input.total());
        input.rewind();

        if (supportsCopy) {
            // Test copy.
            T copy = kryo.copy(object1);
            doAssertEquals(object1, copy);
            copy = kryo.copyShallow(object1);
            doAssertEquals(object1, copy);
        }

        return new RoundTripAssertionOutput<T>(input, (T) object2);
    }

    protected void doAssertEquals(Object object1, Object object2) {
		KryoTestUtil.assertEquals(arrayToList(object1), arrayToList(object2));
    }

    public void assertEquals(Object object1, Object object2) {
		KryoTestUtil.assertEquals(arrayToList(object1), arrayToList(object2));
    }

	private Object arrayToList (Object array) {
		return KryoTestUtil.arrayToList(array);
    }

    static public ArrayList list(Object... items) {
		return KryoTestUtil.list(items);
    }
}
