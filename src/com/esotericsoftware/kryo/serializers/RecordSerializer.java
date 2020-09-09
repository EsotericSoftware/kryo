/* Copyright (c) 2008-2020, Nathan Sweet
 * Copyright (C) 2020, Oracle and/or its affiliates.
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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;

import static com.esotericsoftware.minlog.Log.TRACE;
import static com.esotericsoftware.minlog.Log.trace;
import static java.lang.invoke.MethodType.methodType;

public class RecordSerializer<T> extends ImmutableSerializer<T> {
    private static final MethodHandle MH_IS_RECORD;
    private static final MethodHandle MH_GET_RECORD_COMPONENTS;
    private static final MethodHandle MH_GET_NAME;
    private static final MethodHandle MH_GET_TYPE;
    private static final MethodHandles.Lookup LOOKUP;

    static {
        MethodHandle MH_isRecord;
        MethodHandle MH_getRecordComponents;
        MethodHandle MH_getName;
        MethodHandle MH_getType;
        LOOKUP = MethodHandles.lookup();

        try {
            // reflective machinery required to access the record components
            // without a static dependency on Java SE 14 APIs
            Class<?> c = Class.forName("java.lang.reflect.RecordComponent");
            MH_isRecord = LOOKUP.findVirtual(Class.class, "isRecord", methodType(boolean.class));
            MH_getRecordComponents = LOOKUP.findVirtual(Class.class, "getRecordComponents",
                    methodType(Array.newInstance(c, 0).getClass()))
                    .asType(methodType(Object[].class, Class.class));
            MH_getName = LOOKUP.findVirtual(c, "getName", methodType(String.class))
                    .asType(methodType(String.class, Object.class));
            MH_getType = LOOKUP.findVirtual(c, "getType", methodType(Class.class))
                    .asType(methodType(Class.class, Object.class));
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // pre-Java-14
            MH_isRecord = null;
            MH_getRecordComponents = null;
            MH_getName = null;
            MH_getType = null;
        } catch (IllegalAccessException unexpected) {
            throw new AssertionError(unexpected);
        }

        MH_IS_RECORD = MH_isRecord;
        MH_GET_RECORD_COMPONENTS = MH_getRecordComponents;
        MH_GET_NAME = MH_getName;
        MH_GET_TYPE = MH_getType;
    }

    public RecordSerializer() {
    }

    @Override
    public void write(Kryo kryo, Output output, T object) {
        final Class<?> cls = object.getClass();
        if (!isRecord(cls)) {
            throw new KryoException(object + " is not a record");
        }
        for (RecordComponent rc : recordComponents(cls, Comparator.comparing(RecordComponent::name))) {
            final Class<?> type = rc.type();
            final String name = rc.name();
            try {
                if (TRACE) trace("kryo", "Write property: " + name + " (" + type.getName() + ")");
                if (rc.type().isPrimitive()) {
                    kryo.writeObject(output, componentValue(object, rc));
                } else {
                    kryo.writeObjectOrNull(output, componentValue(object, rc), type);
                }
            } catch (KryoException ex) {
                ex.addTrace(name + " (" + type.getName() + ")");
                throw ex;
            } catch (Throwable t) {
                KryoException ex = new KryoException(t);
                ex.addTrace(name + " (" + type.getName() + ")");
                throw ex;
            }
        }
    }

    @Override
    public T read(Kryo kryo, Input input, Class<? extends T> type) {
        if (!isRecord(type)) {
            throw new KryoException("Not a record (" + type + ")");
        }
        final RecordComponent[] recordComponents =
                recordComponents(type, Comparator.comparing(RecordComponent::name));
        final Object[] values = new Object[recordComponents.length];
        for (int i = 0; i < recordComponents.length; i++) {
            final RecordComponent rc = recordComponents[i];
            final String name = rc.name();
            try {
                if (TRACE) trace("kryo", "Read property: " + name + " (" + type.getName() + ")");
                // Populate values in the order required by the canonical constructor
                values[rc.index()] = rc.type().isPrimitive() ? kryo.readObject(input, rc.type())
                        : kryo.readObjectOrNull(input, rc.type());
            } catch (KryoException ex) {
                ex.addTrace(name + " (" + type.getName() + ")");
                throw ex;
            } catch (Throwable t) {
                KryoException ex = new KryoException(t);
                ex.addTrace(name + " (" + type.getName() + ")");
                throw ex;
            }
        }
        Arrays.sort(recordComponents, Comparator.comparing(RecordComponent::index));
        return invokeCanonicalConstructor(type, recordComponents, values);
    }

    /** Returns true if, and only if, the given class is a record class. */
    private boolean isRecord(Class<?> type) {
        try {
            return (boolean) MH_IS_RECORD.invokeExact(type);
        } catch (Throwable t) {
            throw new KryoException("Could not determine type (" + type + ")");
        }
    }

    /** A record component, which has a name, a type and an index.
     *  The latter is the index of the record component in the class file's
     *  record attribute, required to invoke the record's canonical constructor .*/
    final static class RecordComponent {
        private final String name;
        private final Class<?> type;
        private final int index;

        RecordComponent(String name, Class<?> type, int index) {
            this.name = name;
            this.type = type;
            this.index = index;
        }

        String name() { return name; }
        Class<?> type() { return type; }
        int index() { return index; }
    }

    /**
     * Returns an ordered array of the record components for the given record
     * class. The order is imposed by the given comparator. If the given
     * comparator is null, the order is that of the record components in the
     * record attribute of the class file.
     */
    private static <T> RecordComponent[] recordComponents(Class<T> type,
                                                          Comparator<RecordComponent> comparator) {
        try {
            Object[] rawComponents = (Object[]) MH_GET_RECORD_COMPONENTS.invokeExact(type);
            RecordComponent[] recordComponents = new RecordComponent[rawComponents.length];
            for (int i = 0; i < rawComponents.length; i++) {
                final Object comp = rawComponents[i];
                recordComponents[i] = new RecordComponent(
                        (String) MH_GET_NAME.invokeExact(comp),
                        (Class<?>) MH_GET_TYPE.invokeExact(comp), i);
            }
            if (comparator != null) Arrays.sort(recordComponents, comparator);
            return recordComponents;
        } catch (Throwable t) {
            KryoException ex = new KryoException(t);
            ex.addTrace("Could not retrieve record components (" + type.getName() + ")");
            throw ex;
        }
    }

    /** Retrieves the value of the record component for the given record object. */
    private static Object componentValue(Object recordObject,
                                         RecordComponent recordComponent) {
        try {
            MethodHandle MH_get = LOOKUP.findVirtual(recordObject.getClass(),
                    recordComponent.name(),
                    methodType(recordComponent.type()));
            return (Object) MH_get.invoke(recordObject);
        } catch (Throwable t) {
            KryoException ex = new KryoException(t);
            ex.addTrace("Could not retrieve record components ("
                    + recordObject.getClass().getName() + ")");
            throw ex;
        }
    }

    /**
     * Invokes the canonical constructor of a record class with the
     * given argument values.
     */
    private static <T> T invokeCanonicalConstructor(Class<T> recordType,
                                                RecordComponent[] recordComponents,
                                                Object[] args) {
        try {
            Class<?>[] paramTypes = Arrays.stream(recordComponents)
                    .map(RecordComponent::type)
                    .toArray(Class<?>[]::new);
            MethodHandle MH_canonicalConstructor =
                    LOOKUP.findConstructor(recordType, methodType(void.class, paramTypes))
                            .asType(methodType(Object.class, paramTypes));
            return (T)MH_canonicalConstructor.invokeWithArguments(args);
        } catch (Throwable t) {
            KryoException ex = new KryoException(t);
            ex.addTrace("Could not construct type (" + recordType.getName() + ")");
            throw ex;
        }
    }
}
