/* Copyright (c) 2016, Martin Grotzke
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

package com.esotericsoftware.kryo.serializers.java8;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.Optional;

import static com.esotericsoftware.minlog.Log.DEBUG;
import static com.esotericsoftware.minlog.Log.debug;

/**
 * Serializer for {@link Optional}.
 */
public class OptionalSerializer extends Serializer<Optional> {

    private static final boolean OPTIONAL_SUPPORTED;

    static {
        boolean supported = false;
        try {
            Class.forName("java.util.Optional");
            supported = true;
        } catch (Exception e) {
            if (DEBUG) {
                debug("Class 'java.util.Optional' not found, 'OptionalSerializer' won't be registered as default serializer.");
            }
        }
        OPTIONAL_SUPPORTED = supported;
    }

    {
        setAcceptsNull(false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(Kryo kryo, Output output, Optional object) {
        Object nullable = object.isPresent() ? object.get() : null;
        kryo.writeClassAndObject(output, nullable);
    }

    @Override
    public Optional read(Kryo kryo, Input input, Class<Optional> type) {
        return Optional.ofNullable(kryo.readClassAndObject(input));
    }

    @Override
    public Optional copy(Kryo kryo, Optional original) {
        if(original.isPresent()) {
            return Optional.of(kryo.copy(original.get()));
        }
        return original;
    }

    public static void addDefaultSerializer(Kryo kryo) {
        if(OPTIONAL_SUPPORTED)
            kryo.addDefaultSerializer(Optional.class, OptionalSerializer.class);
    }
}
