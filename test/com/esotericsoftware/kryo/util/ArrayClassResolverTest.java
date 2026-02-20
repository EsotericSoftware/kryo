/* Copyright (c) 2008-2022, Nathan Sweet
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

package com.esotericsoftware.kryo.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * You can test {@link ArrayClassResolver} also by below method:
 * temporarily modify {@link KryoTestCase#setUp()}, then run all test cases.
 *
 * <pre>
 * @BeforeEach
 * public void setUp () throws Exception {
 *	 if (debug && WARN) warn("*** DEBUG TEST ***");
 *
 * 	 kryo = new Kryo(new ArrayClassResolver(), null);
 * }
 * </pre>
 *
 * @author lifeinwild1@gmail.com
 */
public class ArrayClassResolverTest extends KryoTestCase {
    @BeforeEach
    public void setUp () throws Exception {
        super.setUp();

        ArrayClassResolver resolver = new ArrayClassResolver();
        kryo = new Kryo(resolver, null);
        kryo.register(ArrayList.class);
    }

    @Test
    void testHugeID () {
        ArrayClassResolver resolver = new ArrayClassResolver();

        int id0 = 0;
        Registration input0 = new Registration(TestModel0.class, new TestSerializer0(), id0);
        resolver.register(input0);

        int id1 = 1;
        Registration input1 = new Registration(TestModel1.class, new TestSerializer1(), id1);
        resolver.register(input1);

        int id1000000 = 1000000;
        Registration input1000000 = new Registration(TestModel1000000.class, new TestSerializer1000000(), id1000000);
        resolver.register(input1000000);

        Registration r0 = resolver.getRegistration(id0);
        assertEquals(input0, r0);

        Registration r1 = resolver.getRegistration(id1);
        assertEquals(input1, r1);

        Registration r1000000 = resolver.getRegistration(id1000000);
        assertEquals(input1000000, r1000000);
    }

    private static class TestModel1{

    }
    private static class TestSerializer1 extends Serializer<TestModel1> {

        @Override
        public void write(Kryo kryo, Output output, TestModel1 object) {

        }

        @Override
        public TestModel1 read(Kryo kryo, Input input, Class<? extends TestModel1> type) {
            return null;
        }
    }

    private static class TestModel0{

    }
    private static class TestSerializer0 extends Serializer<TestModel0> {

        @Override
        public void write(Kryo kryo, Output output, TestModel0 object) {

        }

        @Override
        public TestModel0 read(Kryo kryo, Input input, Class<? extends TestModel0> type) {
            return null;
        }
    }
    private static class TestModel1000000{

    }
    private static class TestSerializer1000000 extends Serializer<TestModel1000000> {

        @Override
        public void write(Kryo kryo, Output output, TestModel1000000 object) {

        }

        @Override
        public TestModel1000000 read(Kryo kryo, Input input, Class<? extends TestModel1000000> type) {
            return null;
        }
    }
    @Test
    void testArrayList () {
        ArrayList test = new ArrayList();
        test.add("one");
        test.add("two");
        test.add("three");

        ArrayList copy = kryo.copy(test);
        assertNotSame(test, copy);
        assertEquals(test, copy);
    }


}
