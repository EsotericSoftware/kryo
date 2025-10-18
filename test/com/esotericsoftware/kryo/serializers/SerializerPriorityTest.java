/* Copyright (c) 2008-2025, Nathan Sweet
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

package com.esotericsoftware.kryo.serializers;

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.KryoSerializableSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.TreeMapSerializer;

import java.util.TreeMap;

import org.junit.jupiter.api.Test;

/**
 * @author Niels Basjes
 */
@SuppressWarnings("synthetic-access")
class SerializerPriorityTest {

    // Dummy serializer
    public static class MyAnnotationSerializer extends Serializer<Object> {
        @Override
        public void write(Kryo kryo, Output output, Object object) {
        }

        @Override
        public Object read(Kryo kryo, Input input, Class type) {
            return null;
        }
    }

    // Dummy KryoSerializable implementation
    interface BaseKryoSerializable extends KryoSerializable {
        @Override
        default void write(Kryo kryo, Output output) {
        }

        @Override
        default void read(Kryo kryo, Input input) {
        }
    }

    static class MyTreeMap extends TreeMap<String, String> {   }
    static class MyKryoSerializable implements BaseKryoSerializable {    }
    static class MyTreeMapKryoSerializable extends TreeMap<String, String> implements BaseKryoSerializable {    }

    @DefaultSerializer(MyAnnotationSerializer.class)
    static class MyAnnotated {    }

    @DefaultSerializer(MyAnnotationSerializer.class)
    static class MyAnnotatedTreeMap extends TreeMap<String, String> {    }

    @DefaultSerializer(MyAnnotationSerializer.class)
    static class MyAnnotatedKryoSerializable implements BaseKryoSerializable {    }

    @DefaultSerializer(MyAnnotationSerializer.class)
    static class MyAnnotatedTreeMapKryoSerializable extends TreeMap<String, String> implements BaseKryoSerializable {    }

    @Test
    void testAnnotatedSerializer() {
        assertSerializer(MyAnnotated.class, MyAnnotationSerializer.class);
    }

    @Test
    void testTreeMapSerializer() {
        assertSerializer(MyTreeMap.class, TreeMapSerializer.class);
    }

    @Test
    void testKryoSerializableSerializer() {
        assertSerializer(MyKryoSerializable.class, KryoSerializableSerializer.class);
    }

    @Test
    void testKryoSerializableSerializerHasHigherPriorityThanDefaultSerializer() {
        assertSerializer(MyTreeMapKryoSerializable.class, KryoSerializableSerializer.class);
    }

    @Test
    void testAnnotatedSerializerHasHigherPriorityThanDefaultSerializer() {
        assertSerializer(MyAnnotatedTreeMap.class, MyAnnotationSerializer.class);
    }

    @Test
    void testAnnotatedSerializerHasHigherPriorityThanKryoSerializable() {
        assertSerializer(MyAnnotatedKryoSerializable.class, MyAnnotationSerializer.class);
    }

    @Test
    void testAnnotatedSerializerHasHigherPriorityThanKryoSerializableAndDefaultSerializer() {
        assertSerializer(MyAnnotatedTreeMapKryoSerializable.class, MyAnnotationSerializer.class);
    }

    private void assertSerializer(Class<?> myClass, Class<? extends Serializer<?>> expectedSerializer) {
        Kryo kryo = new Kryo();
        kryo.register(myClass);
        assertEquals(expectedSerializer, kryo.getSerializer(myClass).getClass());
    }

}
