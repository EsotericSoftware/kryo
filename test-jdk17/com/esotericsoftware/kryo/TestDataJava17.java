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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/** Test data for {@link com.esotericsoftware.kryo.serializers.RecordSerializerTest}.
 * @author Julia Boes <julia.boes@oracle.com>
 * @author Chris Hegarty <chris.hegarty@oracle.com>
 */
public class TestDataJava17 extends SerializationCompatTestData.TestData {
    public record Rec (byte b, short s, int i, long l, float f, double d, boolean bool, char c, String str, Integer[] n) {
        // Overriden because of https://stackoverflow.com/questions/61261226/java-14-records-and-arrays
        public boolean equals(Object o) {
            return EqualsBuilder.reflectionEquals(this, o);
        }

        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    };

    private Rec rec;

    public TestDataJava17() {
        rec = new Rec("b".getBytes()[0], (short)1, 2, 3L, 4.0f, 5.0d, true, 'c', "foo", new Integer[]{1,2,3});
    }
}
