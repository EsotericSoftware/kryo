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

package com.esotericsoftware.kryo.pool;

import com.esotericsoftware.kryo.io.Input;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** @author Johno Crawford (johno.crawford@gmail.com) */
@RunWith(Parameterized.class)
public class KryoInputPoolTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {{new KryoInputPool.Builder()}, {new KryoInputPool.Builder().softReferences()}});
    }

    private KryoInputPool pool;

    public KryoInputPoolTest(KryoInputPool.Builder builder) {
        pool = builder.build();
    }

    @Test
    public void getShouldReturnAvailableInstance() {
        final Input[] result = new Input[2];
        pool.run(new KryoIOCallback<Input, Object>() {
            public Object apply(Input input) {
                assertEquals(0, input.position());
                byte[] payload = new byte[] {1,2,3,4};
                input.setBuffer(payload);
                assertArrayEquals(payload, input.readBytes(4));
                result[0] = input;
                return null;
            }
        }, 0);
        assertEquals(0, result[0].position());
        pool.run(new KryoIOCallback<Input, Object>() {
            public Object apply(Input input) {
                result[1] = input;
                return null;
            }
        }, 0);
        assertTrue(result[0] == result[1]);
    }

    @Test
    public void largeObjectNotRecycled() {
        final Input[] result = new Input[2];
        pool.run(new KryoIOCallback<Input, Object>() {
            public Object apply(Input input) {
                result[0] = input;
                return null;
            }
        }, KryoInputPool.DEFAULT_MAX_POOLED_BUFFER_SIZE + 1);
        pool.run(new KryoIOCallback<Input, Object>() {
            public Object apply(Input input) {
                result[1] = input;
                return null;
            }
        }, 0);
        assertTrue(result[0] != result[1]);
    }
}
