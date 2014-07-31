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
