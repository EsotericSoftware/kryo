package com.esotericsoftware.kryo.pool;

import java.util.Queue;

import com.esotericsoftware.kryo.Kryo;

/**
 * A simple {@link Queue} based {@link KryoPool} implementation, should be built
 * using the KryoPool.Builder.
 *
 * @author Martin Grotzke
 */
class KryoPoolQueueImpl implements KryoPool {

	private final Queue<Kryo> queue;
	private final KryoFactory factory;

	KryoPoolQueueImpl(KryoFactory factory, Queue<Kryo> queue) {
		this.factory = factory;
		this.queue = queue;
	}

	public int size () {
		return queue.size();
	}

	public Kryo borrow () {
		Kryo res;
		if((res = queue.poll()) != null) {
			return res;
		}
		return factory.create();
	}

	public void release (Kryo kryo) {
		queue.offer(kryo);
	}

	public <T> T run(KryoCallback<T> callback) {
		Kryo kryo = borrow();
		try {
			return callback.execute(kryo);
		} finally {
			release(kryo);
		}
	}

	public void clear() {
		queue.clear();
	}

}
