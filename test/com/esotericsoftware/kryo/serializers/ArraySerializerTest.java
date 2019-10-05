/* Copyright (c) 2008-2018, Nathan Sweet
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

package com.esotericsoftware.kryo.serializers;

import static org.junit.Assert.*;

import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.ObjectArraySerializer;

import java.util.BitSet;

import org.junit.Test;

/** @author Nathan Sweet */
public class ArraySerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@Test
	public void testArrays () {
		kryo.register(int[].class);
		kryo.register(int[][].class);
		kryo.register(int[][][].class);
		kryo.register(String[].class);
		kryo.register(Object[].class);
		roundTrip(4, new Object[] {null, null});
		roundTrip(6, new Object[] {null, "2"});
		roundTrip(6, new int[] {1, 2, 3, 4});
		roundTrip(7, new int[] {1, 2, -100, 4});
		roundTrip(9, new int[] {1, 2, -100, 40000});
		roundTrip(9, new int[][] {{1, 2}, {100, 4}});
		roundTrip(11, new int[][] {{1}, {2}, {100}, {4}});
		roundTrip(13, new int[][][] {{{1}, {2}}, {{100}, {4}}});
		roundTrip(12, new String[] {"11", "2222", "3", "4"});
		roundTrip(11, new String[] {"11", "2222", null, "4"});
		roundTrip(28,
			new Object[] {new String[] {"11", "2222", null, "4"}, new int[] {1, 2, 3, 4}, new int[][] {{1, 2}, {100, 4}}});

		ObjectArraySerializer serializer = new ObjectArraySerializer(kryo, String[].class);
		kryo.register(String[].class, serializer);
		serializer.setElementsAreSameType(true);
		roundTrip(11, new String[] {"11", "2222", null, "4"});
		serializer.setElementsAreSameType(false);
		roundTrip(11, new String[] {"11", "2222", null, "4"});
		roundTrip(5, new String[] {null, null, null});
		roundTrip(2, new String[] {});
		serializer.setElementsAreSameType(true);
		roundTrip(12, new String[] {"11", "2222", "3", "4"});
		serializer.setElementsCanBeNull(false);
		roundTrip(12, new String[] {"11", "2222", "3", "4"});

		serializer = new ObjectArraySerializer(kryo, Float[].class);
		kryo.register(Float[][].class, serializer);
		kryo.register(Float[].class, serializer);
		Float[][] array = new Float[4][];
		array[0] = new Float[] {0.0f, 1.0f};
		array[1] = null;
		array[2] = new Float[] {2.0f, 3.0f};
		array[3] = new Float[] {3.0f};
		roundTrip(31, array);
	}

	@Test
	public void testRecursiveArray () {
		Object[] array = new Object[1];
		array[0] = array;
		kryo.register(Object[].class);
		Object[] copy = kryo.copy(array);
		assertTrue(copy == copy[0]);
	}

	@Test
	public void testStringArray () {
		String moo = "moooooooooooooooooo";
		String[] array = {moo, "dog", moo};
		kryo.register(String[].class);
		roundTrip(43, array);

		kryo.setReferences(true);
		roundTrip(28, array);
	}
}
