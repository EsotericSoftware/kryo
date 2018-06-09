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

import com.esotericsoftware.kryo.io.Output;

import java.util.Queue;

/** A simple {@link Queue} based {@link KryoOutputPool} implementation, should be built using the KryoOutputPool.Builder.
 *
 * @author Johno Crawford (johno.crawford@gmail.com) */
class KryoOutputPoolImpl extends KryoIOPoolQueueImpl<Output> implements KryoOutputPool {

    private final int maxPooledBufferSize;
    private final int maxBufferSize;

    KryoOutputPoolImpl(Queue<Output> queue, int maxPooledBufferSize, int maxBufferSize) {
        super(queue);
        this.maxPooledBufferSize = maxPooledBufferSize;
        this.maxBufferSize = maxBufferSize;
    }

    @Override
    protected Output create(final int bufferSize) {
        return new Output(bufferSize, maxBufferSize);
    }

    @Override
    protected void release(Output output) {
        if (output.getBuffer().length < maxPooledBufferSize) {
            output.clear();
            super.release(output);
        }
    }
}
