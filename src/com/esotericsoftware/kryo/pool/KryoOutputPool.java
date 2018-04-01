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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentLinkedQueue;

/** A simple pool interface for {@link Output} instances. Use the {@link KryoOutputPool.Builder} to construct a pool instance.
 *
 * @author Johno Crawford (johno.crawford@gmail.com) */
public interface KryoOutputPool {

    int DEFAULT_MAX_POOLED_BUFFER_SIZE = 512 * 1024;
    int DEFAULT_MAX_BUFFER_SIZE = 768 * 1024;

    <R> R run(final KryoIOCallback<Output, R> callback, final int bufferSize);

    class Builder {

        private int maxPooledBufferSize = DEFAULT_MAX_POOLED_BUFFER_SIZE;
        private int maxBufferSize = DEFAULT_MAX_BUFFER_SIZE;
        private boolean softReferences;

        /** Use {@link SoftReference}s for pooled {@link Kryo} instances, so that instances may be garbage collected when there's
         * memory demand (by default disabled). */
        public KryoOutputPool.Builder softReferences() {
            softReferences = true;
            return this;
        }

        /** Objects larger than <code>maxPooledBufferSize</code> will not be recycled. */
        public KryoOutputPool.Builder maxPooledBufferSize(int maxPooledBufferSize) {
            this.maxPooledBufferSize = maxPooledBufferSize;
            return this;
        }

        /** The maximum buffer size for the underlying {@link Output}. */
        public KryoOutputPool.Builder maxBufferSize(int maxBufferSize) {
            this.maxBufferSize = maxBufferSize;
            return this;
        }

        /** Build the pool. */
        public KryoOutputPool build () {
            return new KryoOutputPoolImpl(softReferences ?
                    new SoftReferenceQueue<Output>(new ConcurrentLinkedQueue<Output>()) :
                    new ConcurrentLinkedQueue<Output>(), maxPooledBufferSize, maxBufferSize);
        }
    }
}