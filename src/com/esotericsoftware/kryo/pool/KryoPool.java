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

import java.lang.ref.SoftReference;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.esotericsoftware.kryo.Kryo;

/**
 * A simple pool interface for {@link Kryo} instances. Use the {@link KryoPool.Builder} to
 * construct a pool instance.
 * 
 * Usage:
 * <pre>
 * import com.esotericsoftware.kryo.Kryo;
 * import com.esotericsoftware.kryo.pool.*;
 * 
 * KryoFactory factory = new KryoFactory() {
 *   public Kryo create () {
 *     Kryo kryo = new Kryo();
 *     // configure kryo instance, customize settings
 *     return kryo;
 *   }
 * };
 * // Simple pool, you might also activate SoftReferences to fight OOMEs.
 * KryoPool pool = new KryoPool.Builder(factory).build();
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
public interface KryoPool {

	/**
	 * Takes a {@link Kryo} instance from the pool or creates a new one
	 * (using the factory) if the pool is empty.
	 */
	Kryo borrow ();

	/**
	 * Returns the given {@link Kryo} instance to the pool.
	 */
	void release (Kryo kryo);

	/**
	 * Runs the provided {@link KryoCallback} with a {@link Kryo} instance
	 * from the pool (borrow/release around {@link KryoCallback#execute(Kryo)}).
	 */
	<T> T run(KryoCallback<T> callback);

	/**
	 * Builder for a {@link KryoPool} instance, constructs a {@link KryoPoolQueueImpl} instance.
	 */
	public static class Builder {

		private final KryoFactory factory;
		private Queue<Kryo> queue = new ConcurrentLinkedQueue<Kryo>();
		private boolean softReferences;

		public Builder(KryoFactory factory) {
			if(factory == null) {
				throw new IllegalArgumentException("factory must not be null");
			}
			this.factory = factory;
		}

		/**
		 * Use the given queue for pooling kryo instances (by default a {@link ConcurrentLinkedQueue}
		 * is used).
		 */
		public Builder queue(Queue<Kryo> queue) {
			if(queue == null) {
				throw new IllegalArgumentException("queue must not be null");
			}
			this.queue = queue;
			return this;
		}

		/**
		 * Use {@link SoftReference}s for pooled {@link Kryo} instances, so that
		 * instances may be garbage collected when there's memory demand (by default
		 * disabled).
		 */
		public Builder softReferences() {
			softReferences = true;
			return this;
		}

		/**
		 * Build the pool.
		 */
		public KryoPool build() {
			Queue<Kryo> q = softReferences ? new SoftReferenceQueue(queue) : queue;
			return new KryoPoolQueueImpl(factory, q);
		}

		@Override
		public String toString () {
			return getClass().getName() + "[queue.class=" + queue.getClass() + ", softReferences=" + softReferences + "]";
		}
	}

}
