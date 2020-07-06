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

package com.esotericsoftware.kryo.util;

import static org.junit.Assert.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PoolTest extends KryoTestCase {
	@Parameters
	public static Collection<Object[]> data () {
		return Arrays.asList(new Object[][] {{new Pool<Kryo>(true, false, 16) {
			protected Kryo create () {
				return new Kryo();
			}
		}}, {new Pool<Kryo>(true, true, 16) {
			protected Kryo create () {
				return new Kryo();
			}
		}}});
	}

	private final Pool<Kryo> pool;

	public PoolTest (Pool<Kryo> pool) {
		this.pool = pool;
	}

	@Before
	public void beforeMethod () {
		// clear the pool's queue
		pool.clear();
	}

	@Test
	public void getShouldReturnAvailableInstance () {
		Kryo kryo = pool.obtain();
		pool.free(kryo);
		assertSame(kryo, pool.obtain());
	}

	@Test
	public void releaseShouldAddKryoToPool () {
		assertEquals(0, pool.getFree());
		Kryo kryo = pool.obtain();
		pool.free(kryo);
		assertEquals(1, pool.getFree());
	}

	@Test
	public void testSize () {
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
}
