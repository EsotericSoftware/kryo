package com.esotericsoftware.kryo.pool;

import java.lang.ref.SoftReference;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.esotericsoftware.kryo.Kryo;

/**
 * A simple, queue based pool for {@link Kryo} instances. Kryo instances
 * are cached using {@link SoftReference}s and therefore should not cause
 * OOMEs.
 * 
 * Usage:
 * <pre>
 * import com.esotericsoftware.kryo.Kryo;
 * import com.esotericsoftware.kryo.pool.KryoCallback;
 * import com.esotericsoftware.kryo.pool.KryoFactory;
 * import com.esotericsoftware.kryo.pool.KryoPool;
 * 
 * KryoFactory factory = new KryoFactory() {
 *   public Kryo create () {
 *     Kryo kryo = new Kryo();
 *     // configure kryo instance, customize settings
 *     return kryo;
 *   }
 * };
 * KryoPool pool = new KryoPool(factory);
 * Kryo kryo = pool.borrow();
 * // do s.th. with kryo here, and afterwards release it
 * pool.release(kryo);
 * 
 * // or use a callback to work with kryo (pool.run borrows+releases for you)
 * String value = pool.run(new KryoCallback<String>() {
 *   public String execute(Kryo kryo) {
 *     return kryo.readObject(input, String.class);
 *   }
 * });
 *
 * </pre>
 * 
 * @author Martin Grotzke
 */
public class KryoPool {
	
	private final Queue<SoftReference<Kryo>> queue;
	private final KryoFactory factory;

	public KryoPool(KryoFactory factory) {
		this(factory, new ConcurrentLinkedQueue<SoftReference<Kryo>>());
	}

	public KryoPool(KryoFactory factory, Queue<SoftReference<Kryo>> queue) {
		this.factory = factory;
		this.queue = queue;
	}

	public int size () {
		return queue.size();
	}

	public Kryo borrow () {
		Kryo res;
		SoftReference<Kryo> ref;
		while((ref = queue.poll()) != null) {
			if((res = ref.get()) != null) {
				return res;
			}
		}
		return factory.create();
	}

	public void release (Kryo kryo) {
		queue.offer(new SoftReference(kryo));
	}

	public <T> T run(KryoCallback<T> callback) {
		Kryo kryo = borrow();
		try {
			return callback.execute(kryo);
		} finally {
			release(kryo);
		}
	}

}
