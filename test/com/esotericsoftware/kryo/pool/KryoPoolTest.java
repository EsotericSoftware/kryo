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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.esotericsoftware.kryo.Kryo;

@RunWith(Parameterized.class)
public class KryoPoolTest {

	private static KryoFactory factory = new KryoFactory() {
		@Override
		public Kryo create () {
			Kryo kryo = new Kryo();
			// configure kryo
			return kryo;
		}
	};

	@Parameters
	public static Collection<Object[]> data () {
		return Arrays.asList(new Object[][] {
			{new KryoPool.Builder(factory)},
			{new KryoPool.Builder(factory).softReferences()}
		});
	}

	private KryoPool pool;

	public KryoPoolTest(KryoPool.Builder builder) {
		pool = builder.build();
	}

	@Before
	public void beforeMethod() {
		// clear the pool's queue
		((KryoPoolQueueImpl)pool).clear();
	}

	private int size() {
		return ((KryoPoolQueueImpl)pool).size();
	}

	@Test
	public void getShouldReturnAvailableInstance() {
		Kryo kryo = pool.borrow();
		pool.release(kryo);
		assertTrue(kryo == pool.borrow());
	}

	@Test
	public void releaseShouldAddKryoToPool() {
		assertEquals(0, size());
		Kryo kryo = pool.borrow();
		pool.release(kryo);
		assertEquals(1, size());
	}

	@Test
	public void testSize() {
		assertEquals(0, size());
		Kryo kryo1 = pool.borrow();
		assertEquals(0, size());
		Kryo kryo2 = pool.borrow();
		assertFalse(kryo1 == kryo2);
		pool.release(kryo1);
		assertEquals(1, size());
		pool.release(kryo2);
		assertEquals(2, size());
	}

	@Test
	public void runWithKryoShouldReturnResult() {
		String value = pool.run(new KryoCallback<String>() {
			@Override
			public String execute(Kryo kryo) {
				return "foo";
			}
		});
		assertEquals("foo", value);
	}

	@Test
	public void runWithKryoShouldAddKryoToPool() {
		assertEquals(0, size());
		pool.run(new KryoCallback<String>() {
			@Override
			public String execute(Kryo kryo) {
				return null;
			}
		});
		assertEquals(1, size());
	}

	@Test
	public void runWithKryoShouldAddKryoToPoolOnException() {
		assertEquals(0, size());
		try {
			pool.run(new KryoCallback<String>() {
				@Override
				public String execute(Kryo kryo) {
					throw new RuntimeException();
				}
			});
			fail("Exception should be rethrown.");
		} catch(RuntimeException e) {
			// expected
		}
		assertEquals(1, size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void runWithKryoShouldRethrowException() {
		String value = pool.run(new KryoCallback<String>() {
			@Override
			public String execute(Kryo kryo) {
				throw new IllegalArgumentException();
			}
		});
	}
	
}
