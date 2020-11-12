/* Copyright (c) 2008-2020, Mr14huashao
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

import com.esotericsoftware.kryo.KryoTestCase;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DataSetTest extends KryoTestCase {

    @Test
    public void testCuckooObjectMap() {
        final CuckooObjectMap cuckooObjectMap = new CuckooObjectMap(40);
        cuckooObjectMap.put("check", "value");
        cuckooObjectMap.put("other", "great");
        final CuckooObjectMap cuckooObjectMap1 = new CuckooObjectMap(cuckooObjectMap);
        final Object value = cuckooObjectMap.get("check");
        final Object value1 = cuckooObjectMap1.get("check");
        cuckooObjectMap1.putAll(cuckooObjectMap);
        cuckooObjectMap1.shrink(30);
        final Boolean checkKey = cuckooObjectMap1.containsKey("check");
        final Boolean checkValue = cuckooObjectMap1.containsValue("great", true);
        String findKey = cuckooObjectMap1.findKey("great", true).toString();
        assertEquals("value", value1.toString());
        assertEquals("value", value.toString());
        assertEquals("other", findKey);
        assertEquals(true, checkKey);
        assertEquals(true, checkValue);
        cuckooObjectMap1.remove("other");
        cuckooObjectMap1.shrink(39);
        cuckooObjectMap1.clear();
    }

    @Test
    public void testIdentityMap() {
        final IdentityMap identityMap = new IdentityMap(40);
        final IdentityMap identityMap1 = new IdentityMap(40, 0.75F);
        identityMap.put("check", "value");
        identityMap.put("other", "great");
        final IdentityMap identityMap2 = new IdentityMap(identityMap);
        final Boolean checkKey = identityMap.containsKey("check");
        final Boolean checkValue = identityMap2.containsValue("great", true);
        String findKey = identityMap2.findKey("great", true).toString();
        assertEquals("other", findKey);
        assertEquals(true, checkKey);
        assertEquals(true, checkValue);
        identityMap2.remove("check");
        identityMap2.shrink(50);
        identityMap2.clear();
    }

    @Test
    public void testIdentityObjectIntMap() {
        final IdentityObjectIntMap identityObjectIntMap = new IdentityObjectIntMap(40);
        final IdentityObjectIntMap identityObjectIntMap1 = new IdentityObjectIntMap(40, 0.75F);
        identityObjectIntMap.put("fire", 119);
        identityObjectIntMap.put("doctor", 120);
        final IdentityObjectIntMap identityObjectIntMap2 = new IdentityObjectIntMap(identityObjectIntMap);
        final Boolean checkKey = identityObjectIntMap.containsKey("fire");
        final Boolean checkValue = identityObjectIntMap.containsValue(119);
        String findKey = identityObjectIntMap2.findKey(120).toString();
        assertEquals("doctor", findKey);
        assertEquals(true, checkKey);
        assertEquals(true, checkValue);
        identityObjectIntMap2.remove("doctor", 166);
        identityObjectIntMap2.shrink(20);
        identityObjectIntMap2.clear();
    }

    @Test
    public void testIntArray() {
        final int[] tempArray = {4, 6, 5, 7, 9};
        final IntArray intArray = new IntArray(false, 50);
        intArray.add(1);
        intArray.add(2);
        intArray.add(3);
        final IntArray intArray1 = new IntArray(intArray);
        final IntArray intArray2 = new IntArray(30);
        intArray1.addAll(tempArray);
        intArray2.addAll(intArray);
        intArray.insert(0, 19);
        intArray.swap(2, 3);
        final int[] ensureCapacityArray = intArray1.ensureCapacity(100);
        final int valueIndex = intArray.indexOf(3);
        final int lastIndexOf = intArray.lastIndexOf(2);
        final Boolean isRemove = intArray.removeValue(1);
        intArray.addAll(tempArray);
        intArray.removeRange(0, 2);
        intArray.sort();
        final int[] setSize = intArray.setSize(25);
        intArray.truncate(30);
        intArray.reverse();
        intArray.shrink();
        Boolean isRemoveAll = intArray1.removeAll(intArray2);
        assertEquals(true, isRemoveAll);
        intArray.clear();
    }

    @Test
    public void testIntMap() {
        final IntMap equalsMap = new IntMap(40);
        final IntMap tempIntMap = new IntMap(50, 0.8F);
        tempIntMap.put(1, "first");
        tempIntMap.put(2, "second");
        tempIntMap.put(3, "thirst");
        tempIntMap.put(4, "fourth");
        equalsMap.put(1, "first");
        equalsMap.put(2, "second");
        equalsMap.put(3, "thirst");
        equalsMap.put(4, "fourth");
        final IntMap intMap = new IntMap();
        final IntMap intMap1 = new IntMap(tempIntMap);
        intMap.putAll(intMap1);
        equalsMap.remove(4);
        intMap.remove(4);
        assertEquals(true, intMap.notEmpty());
        assertEquals(true, intMap.containsValue("first", true));
        assertEquals(true, intMap.containsKey(1));
        final int keyIndex = intMap.findKey("thirst", true, 5);
        assertEquals(3, keyIndex);
        assertEquals(true, intMap.equalsIdentity(equalsMap));
        assertEquals(false, intMap.isEmpty());
        assertEquals("second", intMap.get(2));
        intMap.shrink(10);
        intMap.clear();
    }

    @Test
    public void testObjectIntMap() {
        final ObjectIntMap tempObjectIntMap = new ObjectIntMap(40);
        final ObjectIntMap objectIntMap = new ObjectIntMap(40, 0.75F);
        tempObjectIntMap.put("first", 1);
        tempObjectIntMap.put("second", 2);
        tempObjectIntMap.put("thirst", 3);
        final ObjectIntMap objectIntMap2 = new ObjectIntMap(tempObjectIntMap);
        objectIntMap.putAll(tempObjectIntMap);
        assertEquals(1, objectIntMap.get("first", 3));
        final int increment = objectIntMap.getAndIncrement(4, 5, 5);
        final String checkKey = objectIntMap.findKey(3).toString();
        assertEquals("thirst", checkKey);
        assertEquals(true, objectIntMap.containsValue(2));
        assertEquals(true, objectIntMap.notEmpty());
        assertEquals(false, objectIntMap.isEmpty());
        objectIntMap.remove("first", 6);
        objectIntMap.shrink(20);
        objectIntMap.clear();
    }

    @Test
    public void testObjectMap() {
        final ObjectMap tempObjectMap = new ObjectMap(40);
        final ObjectMap equalsObject = new ObjectMap(34);
        final ObjectMap objectMap = new ObjectMap(40, 0.75F);
        tempObjectMap.put("first", 1);
        tempObjectMap.put("second", 2);
        tempObjectMap.put("thirst", 3);
        equalsObject.put("first", 1);
        equalsObject.put("second", 2);
        equalsObject.put("thirst", 3);
        final ObjectMap objectMap2 = new ObjectMap(tempObjectMap);
        objectMap.putAll(tempObjectMap);
        assertEquals(1, objectMap.get("first", 3));
        final String checkKey = objectMap.findKey(3, true).toString();
        assertEquals("thirst", checkKey);
        assertEquals(true, objectMap.containsValue(2, true));
        assertEquals(true, objectMap.notEmpty());
        assertEquals(false, objectMap.isEmpty());
        assertEquals(true, objectMap.equalsIdentity(equalsObject));
        objectMap.remove("first");
        objectMap.shrink(20);
        objectMap.clear();
    }
}
