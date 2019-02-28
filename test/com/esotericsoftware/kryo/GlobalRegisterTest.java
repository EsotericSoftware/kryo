package com.esotericsoftware.kryo;

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
}
