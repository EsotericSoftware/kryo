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
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/** @author Johno Crawford (johno.crawford@gmail.com) */
@Fork(5)
@State(Scope.Thread)
public class KryoIOPoolTest {

    @Param({ "1024", "4096" })
    public int bufferSize;

    private KryoPool kryoPool;
    private KryoOutputPool outputPool;
    private KryoInputPool inputPool;
    private Message message;
    private byte[] messageBytes;

    @Setup
    public void setup() {
        kryoPool = new KryoPool.Builder(new KryoFactory() {
            public Kryo create () {
                return new Kryo();
            }
        }).softReferences().build();
        outputPool = new KryoOutputPool.Builder().softReferences().maxPooledBufferSize(512 * 1024).maxBufferSize(768 * 1024).build();
        inputPool = new KryoInputPool.Builder().softReferences().maxPooledBufferSize(512 * 1024).build();
        message = new Message();
        message.payload = new byte[4 * 1024];
        messageBytes = outputPool.run(new KryoIOCallback<Output, byte[]>() {
            public byte[] apply(final Output output) {
                return kryoPool.run(new KryoCallback<byte[]>() {
                    public byte[] execute(Kryo kryo) {
                        kryo.writeClassAndObject(output, message);
                        return output.toBytes();
                    }
                });
            }
        }, bufferSize);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public byte[] testPooledOutput() throws Exception {
        return outputPool.run(new KryoIOCallback<Output, byte[]>() {
            public byte[] apply(final Output output) {
                return kryoPool.run(new KryoCallback<byte[]>() {
                    public byte[] execute(Kryo kryo) {
                        kryo.writeClassAndObject(output, "huuhaa");
                        return output.toBytes();
                    }
                });
            }
        }, bufferSize);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public byte[] testOutput() throws Exception {
        return kryoPool.run(new KryoCallback<byte[]>() {
            public byte[] execute(Kryo kryo) {
                Output output = new Output(bufferSize);
                kryo.writeClassAndObject(output, "huuhaa");
                return output.toBytes();
            }
        });
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public Message testPooledInput() throws Exception {
        return inputPool.run(new KryoIOCallback<Input, Message>() {
            public Message apply(final Input input) {
                input.setBuffer(messageBytes);
                return kryoPool.run(new KryoCallback<Message>() {
                    public Message execute(Kryo kryo) {
                        @SuppressWarnings("unchecked")
                        Message obj = (Message) kryo.readClassAndObject(input);
                        return obj;
                    }
                });
            }
        }, bufferSize);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public Message testInput() throws Exception {
        final Input input = new Input(bufferSize);
        input.setBuffer(messageBytes);
        return kryoPool.run(new KryoCallback<Message>() {
            public Message execute(Kryo kryo) {
                @SuppressWarnings("unchecked")
                Message obj = (Message) kryo.readClassAndObject(input);
                return obj;
            }
        });
    }

    public static final class Message {
        public byte[] payload;
    }
}