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

package com.esotericsoftware.kryo.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

class UtilTest {

    @Test
    void testIsAssignableTo() {
        assertTrue(Util.isAssignableTo(Long.class, long.class));
        assertTrue(Util.isAssignableTo(long.class, Long.class));
        assertTrue(Util.isAssignableTo(Long.class, Long.class));
        assertTrue(Util.isAssignableTo(long.class, long.class));
        assertTrue(Util.isAssignableTo(Long.class, Object.class));
        assertTrue(Util.isAssignableTo(long.class, Object.class));
        assertTrue(Util.isAssignableTo(Integer.class, Comparable.class));
        assertTrue(Util.isAssignableTo(Integer.class, Serializable.class));
        assertTrue(Util.isAssignableTo(int.class, Comparable.class));
        assertTrue(Util.isAssignableTo(int.class, Serializable.class));

        assertFalse(Util.isAssignableTo(String.class, Long.class));
        assertFalse(Util.isAssignableTo(String.class, long.class));
    }

    @Test
    void testGetArrayType() {
        assertEquals(int[].class, Util.getArrayType(int.class));
        assertEquals(Integer[].class, Util.getArrayType(Integer.class));
        assertEquals(String[].class, Util.getArrayType(String.class));
        assertEquals(String[][].class, Util.getArrayType(String[].class));
        assertEquals(Object[].class, Util.getArrayType(Object.class));
    }
}
