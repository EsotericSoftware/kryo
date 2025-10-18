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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PoolTest extends KryoTestCase {

	private static final int MAXIMUM_CAPACITY = 4;
	
	public static Collection<Object[]> data () {
		return Arrays.asList(new Object[][] {
			{new TestPool(false, false, MAXIMUM_CAPACITY)},
			{new TestPool(true, false, MAXIMUM_CAPACITY)},
			{new TestPool(false, true, MAXIMUM_CAPACITY)},
			{new TestPool(true, true, MAXIMUM_CAPACITY)}});
	}

	@ParameterizedTest
	@MethodSource("data")
	void obtainShouldReturnAvailableInstance (Pool<Kryo> pool) {
		Kryo kryo = pool.obtain();
		pool.free(kryo);
		assertSame(kryo, pool.obtain());
	}

	@ParameterizedTest
	@MethodSource("data")
	void freeShouldAddKryoToPool (Pool<Kryo> pool) {
		assertEquals(0, pool.getFree());
		Kryo kryo = pool.obtain();
		pool.free(kryo);
		assertEquals(1, pool.getFree());
	}

	@ParameterizedTest
	@MethodSource("data")
	void freeShouldNotAddMoreThanMaximumCapacityToKryoPool (Pool<Kryo> pool) {
		final List<Kryo> kryos = IntStream.rangeClosed(0, MAXIMUM_CAPACITY + 1)
			.mapToObj(i -> pool.obtain())
			.collect(Collectors.toList());
		for (Kryo kryo : kryos) {
			pool.free(kryo);
		}
		assertEquals(MAXIMUM_CAPACITY, pool.getFree());
	}

	@ParameterizedTest
	@MethodSource("data")
	void testSize (Pool<Kryo> pool) {
		assertEquals(0, pool.getFree());
		Kryo kryo1 = pool.obtain();
		assertEquals(0, pool.getFree());
		Kryo kryo2 = pool.obtain();
		assertNotSame(kryo1, kryo2);
		pool.free(kryo1);
		assertEquals(1, pool.getFree());
		pool.free(kryo2);
		assertEquals(2, pool.getFree());
	}

	private static class TestPool extends Pool<Kryo> {

		private final boolean threadSafe;
		private final boolean softReferences;
		private final int maximumCapacity;

		public TestPool (boolean threadSafe, boolean softReferences, int maximumCapacity) {
			super(threadSafe, softReferences, maximumCapacity);
			this.threadSafe = threadSafe;
			this.softReferences = softReferences;
			this.maximumCapacity = maximumCapacity;
		}

		@Override
		protected Kryo create () {
			return new Kryo();
		}

		public String toString() {
			return "TestPool{" +
					"threadSafe=" + threadSafe +
					", softReferences=" + softReferences +
					", maximumCapacity=" + maximumCapacity +
					'}';
		}
	}
}
