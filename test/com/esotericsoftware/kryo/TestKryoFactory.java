package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.pool.KryoFactory;

public class TestKryoFactory implements KryoFactory {
    @Override
    public Kryo create() {
        Kryo kryo = new Kryo();
        kryo.setReferences(false);
        kryo.setRegistrationRequired(true);
        return kryo;
    }
}
