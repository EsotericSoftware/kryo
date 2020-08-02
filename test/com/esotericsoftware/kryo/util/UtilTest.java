package com.esotericsoftware.kryo.util;

import junit.framework.TestCase;

public class UtilTest extends TestCase {

    public void testIsAssignableTo() {
        assertTrue(Util.isAssignableTo(Long.class, long.class));
        assertTrue(Util.isAssignableTo(long.class, Long.class));
        assertTrue(Util.isAssignableTo(Long.class, Long.class));
        assertTrue(Util.isAssignableTo(long.class, long.class));

        assertFalse(Util.isAssignableTo(String.class, Long.class));
        assertFalse(Util.isAssignableTo(String.class, long.class));
    }

}
