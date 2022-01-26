package com.esotericsoftware.kryo.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class has only few test, but I tested {@link ArrayClassResolver} fully by below method:
 * I temporarily modified the kryo instance of {@link KryoTestCase#setUp()}, then all test cases passed.
 *
 * <pre>
 * @BeforeEach
 * public void setUp () throws Exception {
 *	 if (debug && WARN) warn("*** DEBUG TEST ***");
 *
 * 	 kryo = new Kryo(new ArrayClassResolver(), null);
 * }
 *
 * Tests passed: 267 of 267 tests
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
    void testBasic () {
        ArrayList test = new ArrayList();
        test.add("one");
        test.add("two");
        test.add("three");

        ArrayList copy = kryo.copy(test);
        assertNotSame(test, copy);
        assertEquals(test, copy);
    }


}
