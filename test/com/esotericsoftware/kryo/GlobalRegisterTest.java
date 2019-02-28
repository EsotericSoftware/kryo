package com.esotericsoftware.kryo;

import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class GlobalRegisterTest {

    static class SomeClass {
    }

    @Test
    public void testGlobalRegister() {
        Kryo.registerGlobal(SomeClass.class);

        Kryo kryo = new Kryo();

        assertNotNull(kryo.getRegistration(SomeClass.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGlovalUnregister() {
        Kryo.unregisterGlobal(SomeClass.class);

        Kryo kryo = new Kryo();

        kryo.getRegistration(SomeClass.class);
    }

    @Test
    public void testUnregisterForMissing() {
        Kryo.unregisterGlobal(SomeClass.class);
    }
}
