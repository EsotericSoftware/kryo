package com.esotericsoftware.kryo.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;

public class KryoPoolTest {
	
	private KryoFactory factory;
	private KryoPool pool;
	
	@Before
	public void beforeMethod() {
		factory = new KryoFactory() {
			@Override
			public Kryo create () {
				Kryo kryo = new Kryo();
				// configure kryo
				return kryo;
			}
		};
		pool = new KryoPool(factory);
	}

	@Test
	public void getShouldReturnAvailableInstance() {
		Kryo kryo = pool.borrow();
		pool.release(kryo);
		assertTrue(kryo == pool.borrow());
	}

	@Test
	public void releaseShouldAddKryoToPool() {
		assertEquals(0, pool.size());
		Kryo kryo = pool.borrow();
		pool.release(kryo);
		assertEquals(1, pool.size());
	}

	@Test
	public void testSize() {
		assertEquals(0, pool.size());
		Kryo kryo1 = pool.borrow();
		assertEquals(0, pool.size());
		Kryo kryo2 = pool.borrow();
		assertFalse(kryo1 == kryo2);
		pool.release(kryo1);
		assertEquals(1, pool.size());
		pool.release(kryo2);
		assertEquals(2, pool.size());
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
		assertEquals(0, pool.size());
		pool.run(new KryoCallback<String>() {
			@Override
			public String execute(Kryo kryo) {
				return null;
			}
		});
		assertEquals(1, pool.size());
	}

	@Test
	public void runWithKryoShouldAddKryoToPoolOnException() {
		assertEquals(0, pool.size());
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
		assertEquals(1, pool.size());
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
