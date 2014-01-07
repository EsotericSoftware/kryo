package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.factories.SerializerFactory;

import java.util.ArrayList;

/**
 * Copyright (c) 2012,2013
 */
public class DefaultSerializerRegistry {

    private final ArrayList<Entry> defaultSerializers = new ArrayList<Entry>( 32 );


    static final class Entry {
        final Class type;
        final SerializerFactory serializerFactory;

        Entry(Class type, SerializerFactory serializerFactory) {
            this.type = type;
            this.serializerFactory = serializerFactory;
        }
    }
}
