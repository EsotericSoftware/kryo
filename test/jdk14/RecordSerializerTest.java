/* Copyright (c) 2008-2020, Nathan Sweet
 * Copyright (C) 2020, Oracle and/or its affiliates.
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

package jdk14;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.stream.IntStream;

import static java.lang.System.out;
import static org.junit.Assert.assertEquals;

public class RecordSerializerTest extends KryoTestCase {
    {
        supportsCopy = false;
    }

    /** Test where the single object is a record. */
    public record RecordRectangle (String height, int width, long x, double y) { }

    @Test
    public void testBasicRecord() {
        out.println("testBasicRecord\n");
        kryo.register(RecordRectangle.class);

        final var r1 = new RecordRectangle("one", 2, 3L, 4.0);
        final var output = new Output(32);
        kryo.writeObject(output, r1);
        out.println("Serialized record: \n" + Arrays.toString(output.getBuffer()));

        final var input = new Input(output.getBuffer(), 0, output.position());
        final var r2 = kryo.readObject(input, RecordRectangle.class);
        out.println("Deserialized record: \n" + r2);

        doAssertEquals(r1, r2);
        roundTrip(14, r1);
        out.println("------\n");
    }

    /** Test where the single object is an empty record. */
    public record EmptyRecord () { }

    @Test
    public void testEmptyRecord() {
        out.println("testEmptyRecord\n");
        kryo.register(EmptyRecord.class);

        final var r1 = new EmptyRecord();
        final var output = new Output(32);
        kryo.writeObject(output, r1);
        out.println("Serialized record: \n" + Arrays.toString(output.getBuffer()));

        final var input = new Input(output.getBuffer(), 0, output.position());
        final var r2 = kryo.readObject(input, EmptyRecord.class);
        out.println("Deserialized record: \n" + r2);

        doAssertEquals(r1, r2);
        roundTrip(1, r1);
        out.println("------\n");
    }

    /** Test deserialisation of an empty record where the input provides values.
     *  In this case no values are read from the input.
     */
    @Test
    public void testDeserializeEmptyRecordWithValues() {
        out.println("testDeserializeEmptyRecordWithValues\n");
        kryo.register(EmptyRecord.class);

        final var input = new Input(new byte[]{1, 2, -128, 0});  // create bad input
        out.println("Serialized record: \n" + Arrays.toString(input.getBuffer()));
        final var r = kryo.readObject(input, EmptyRecord.class);
        out.println("Deserialized record: \n" + r);
        out.println("------\n");
    }

    /** Test deserialisation of a record where the number of input values exceeds
     *  the number of record components. In this case values are read from the
     *  input sequentially until the number of record components is met,
     *  any additional input values are ignored.
     */
    public record RecordPoint (int x, int y) { }

    @Test
    public void testDeserializeWrongNumberOfValues() {
        out.println("testDeserializeWrongNumberOfValues\n");
        kryo.register(RecordPoint.class);

        final var r1 = new RecordPoint(1,1);
        final var input = new Input(new byte[]{2, 2, 2});  // create bad input
        out.println("Serialized record: \n" + Arrays.toString(input.getBuffer()));
        final var r2 = kryo.readObject(input, RecordPoint.class);
        out.println("Deserialized record: \n" + r2);
        doAssertEquals(r1, r2);
        out.println("------\n");
    }

    /** Test where the record has an explicit constructor.*/
    public record RecordWithConstructor (String height, int width, long x, double y) {
        public RecordWithConstructor(String height) {
            this(height, 20, 30L, 40.0);
        }
    }

    @Test
    public void testRecordWithConstructor() {
        out.println("testRecordWithConstructor\n");
        kryo.register(RecordWithConstructor.class);

        final var r1 = new RecordWithConstructor("ten");
        final var output = new Output(32);
        kryo.writeObject(output, r1);
        out.println("Serialized record: \n" + Arrays.toString(output.getBuffer()));

        final var input = new Input(output.getBuffer(), 0, output.position());
        final var r2 = kryo.readObject(input, RecordWithConstructor.class);
        out.println("Deserialized record: \n" + r2);

        doAssertEquals(r1, r2);
        roundTrip(14, r1);
        out.println("------\n");
    }

    /** Test where the record component object is a record. */
    public record RecordOfRecord (RecordRectangle r) { }

    @Test
    public void testRecordOfRecord() {
        out.println("testRecordOfRecord\n");
        kryo.register(RecordOfRecord.class);
        kryo.register(RecordRectangle.class);

        final var r1 = new RecordOfRecord(new RecordRectangle("one", 2, 3L, 4.0));
        final var output = new Output(32);
        kryo.writeObject(output, r1);
        out.println("Serialized record: \n" + Arrays.toString(output.getBuffer()));

        final var input = new Input(output.getBuffer(), 0, output.position());
        final var r2 = kryo.readObject(input, RecordOfRecord.class);
        out.println("Deserialized record: \n" + r2);

        doAssertEquals(r1, r2);
        roundTrip(15, r1);
        out.println("------\n");
    }

    /** Test where the single object is an array of records. */
    @Test
    public void testArrayOfRecords() {
        out.println("testArrayOfRecords\n");
        kryo.register(RecordPoint.class);
        kryo.register(RecordPoint[].class);

        final var arr = new RecordPoint[100];
        IntStream.range(0, 100).forEach(i -> arr[i] = new RecordPoint(i, i+1));

        roundTrip(375, arr);
    }

    /** Test where the record component object is an array of records. */
    public record RecordWithArray (RecordRectangle[] recordArray) { }

    @Test
    public void testRecordWithArray() {
        out.println("testRecordWithArray\n");
        kryo.register(RecordWithArray.class);
        kryo.register(RecordRectangle[].class);
        kryo.register(RecordRectangle.class);

        final var r1 = new RecordWithArray(new RecordRectangle[] {new RecordRectangle("one", 2, 3L, 4.0)});
        final var output = new Output(32);
        kryo.writeObject(output, r1);
        out.println("Serialized record: \n" + Arrays.toString(output.getBuffer()));

        final var input = new Input(output.getBuffer(), 0, output.position());
        final var r2 = kryo.readObject(input, RecordWithArray.class);
        out.println("Deserialized record: \n" + r2);

        assertWithArrayEquals(r1, r2);
        out.println("------\n");
    }

    private void assertWithArrayEquals(final RecordWithArray expected,
                                         final RecordWithArray actual) {
        assertEquals(expected.getClass(), actual.getClass());
        final RecordRectangle[] expectedArray = expected.recordArray();
        final RecordRectangle[] actualArray = actual.recordArray();
        assertEquals(Array.getLength(expectedArray), Array.getLength(actualArray));
        for (int i = 0; i < Array.getLength(expectedArray); i++) {
            assertEquals(Array.get(expectedArray, i), Array.get(actualArray, i));
        }
    }

    /** Test where record components are non-primitives with their default
     *  value (null).
     */
    public record RecordWithNull (Object o, Number n, String s) { }

    @Test
    public void testRecordWithNull() {
        out.println("testRecordWithNull\n");
        kryo.register(RecordWithNull.class);
        kryo.register(Object.class);
        kryo.register(Number.class);
        kryo.register(String.class);

        final var r1 = new RecordWithNull(null, null, null);
        final var output = new Output(32);
        kryo.writeObject(output, r1);
        out.println("Serialized record: \n" + Arrays.toString(output.getBuffer()));

        final var input = new Input(output.getBuffer(), 0, output.position());
        final var r2 = kryo.readObject(input, RecordWithNull.class);
        out.println("Deserialized record: \n" + r2);

        doAssertEquals(r1, r2);
        roundTrip(4, r1);
        out.println("------\n");
    }

    /** Test where record components are primitives with their default values. */
    public record RecordWithDefaultValues(byte b, short s, int i, long l, float f, double d, char c, boolean bool) { }

    @Test
    public void testRecordWithPrimitiveDefaultValues() {
        out.println("testRecordWithPrimitiveDefaultValues\n");
        kryo.register(RecordWithDefaultValues.class);

        final var r1 = new RecordWithDefaultValues(
                (byte)0, (short)0, 0, 0l, 0.0f, 0.0d, '\u0000', false);
        final var output = new Output(32);
        kryo.writeObject(output, r1);
        out.println("Serialized record: \n" + Arrays.toString(output.getBuffer()));

        final var input = new Input(output.getBuffer(), 0, output.position());
        final var r2 = kryo.readObject(input, RecordWithDefaultValues.class);
        out.println("Deserialized record: \n" + r2);

        doAssertEquals(r1, r2);
        roundTrip(21, r1);
        out.println("------\n");
    }

    /** Test where the an exception is thrown in the record constructor. */
    public record PositivePoint (int x, int y) {
        public PositivePoint { // compact syntax
            if (x < 0)
                throw new IllegalArgumentException("negative x:" + x);
            if (y < 0)
                throw new IllegalArgumentException("negative y:" + y);
        }
    }

    @Test
    public void testDeserializeRecordWithIllegalValue1() {
        out.println("testDeserializeRecordWithIllegalValue1\n");
        kryo.register(PositivePoint.class);

        final var input = new Input(new byte[]{1, 2});  // create bad input of -1, 1
        out.println("Serialized record: \n" + Arrays.toString(input.getBuffer()));
        var e = expectThrows(IllegalArgumentException.class,
                () -> kryo.readObject(input, PositivePoint.class));
        assertEquals("negative x:-1", e.getMessage());
        out.println("------\n");
    }

    @Test
    public void testDeserializeRecordWithIllegalValue2() {
        out.println("testDeserializeRecordWithIllegalValue2\n");
        kryo.register(PositivePoint.class);

        final var input = new Input(new byte[]{2, 1});  // create bad input of 1, -1
        out.println("Serialized record: \n" + Arrays.toString(input.getBuffer()));
        var e = expectThrows(IllegalArgumentException.class,
                () -> kryo.readObject(input, PositivePoint.class));
        assertEquals("negative y:-1", e.getMessage());
        out.println("------\n");
    }

    static <T extends Throwable> T expectThrows(Class<T> throwableClass, Runnable task) {
        try {
            task.run();
            throw new AssertionError("Exception not thrown");
        } catch (KryoException ce) {
            Throwable cause = ce.getCause();
            if (!throwableClass.isInstance(cause)) {
                throw new RuntimeException("expected: " + throwableClass + ", actual: " + cause);
            }
            return throwableClass.cast(cause);
        }
    }

    /** Test where the record parameters are the same but in different order.
     *  This is supported as record components are sorted by name during
     *  de/serialization.
     */
    public record R (long l, int i, String s) { }
    public record R1 (int i, long l, String s) { }
    public record R2 (String s, int i, long l) { }

    @Test
    public void testRecordWithParametersReordered1() {
        out.println("testRecordWithParametersReordered1\n");
        kryo.register(R.class);
        kryo.register(R1.class);

        final var r = new R(1L, 1, "foo");
        final var output = new Output(32);
        kryo.writeObject(output, r);
        out.println("Serialized record: \n" + Arrays.toString(output.getBuffer()));

        final var input = new Input(output.getBuffer(), 0, output.position());
        final var r1 = kryo.readObject(input, R1.class);
        out.println("Deserialized record: \n" + r1);

        roundTrip(6, r1);
        out.println("------\n");
    }

    @Test
    public void testRecordWithParametersReordered2() {
        out.println("testRecordWithParametersReordered2\n");
        kryo.register(R.class);
        kryo.register(R2.class);

        final var r = new R(1L, 1, "foo");
        final var output = new Output(32);
        kryo.writeObject(output, r);
        out.println("Serialized record: \n" + Arrays.toString(output.getBuffer()));

        final var input = new Input(output.getBuffer(), 0, output.position());
        final var r2 = kryo.readObject(input, R2.class);
        out.println("Deserialized record: \n" + r2);

        roundTrip(6, r2);
        out.println("------\n");
    }
}
