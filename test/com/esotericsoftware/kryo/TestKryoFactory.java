package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.pool.KryoFactory;

/**
 * <pre>
 *     FactoryMethod for Kryo Unit Testing.
 * </pre>
 */
public class TestKryoFactory implements KryoFactory {
    @Override
    public Kryo create() {
        Kryo kryo = new Kryo();
        kryo.setReferences(false);
        kryo.setRegistrationRequired(true);
        return kryo;
    }
}
