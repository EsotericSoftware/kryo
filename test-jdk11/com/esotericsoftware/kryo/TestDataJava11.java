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

import com.esotericsoftware.kryo.SerializationCompatTestData;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestDataJava11 extends SerializationCompatTestData.TestData {
    private List<Integer> emptyImmutableList;
    private Map<Integer, Integer> emptyImmutableMap;
    private Set<Integer> emptyImmutableSet;
    private List<Integer> singleImmutableList;
    private Map<Integer, Integer> singleImmutableMap;
    private Set<Integer> singleImmutableSet;
    private List<Integer> immutableList;
    private Map<Integer, Integer> immutableMap;
    private Set<Integer> immutableSet;

    public TestDataJava11 () {
        emptyImmutableList = List.of();
        emptyImmutableMap = Map.of();
        emptyImmutableSet = Set.of();
        singleImmutableList = List.of(42);
        singleImmutableMap = Map.of(42, 42);
        singleImmutableSet = Set.of(42);
        immutableList = List.of(1, 2, 3);
        immutableMap = Map.of(1, 2, 3, 4);
        immutableSet = Set.of(1, 2, 3);
    }
}
