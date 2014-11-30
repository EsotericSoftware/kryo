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
