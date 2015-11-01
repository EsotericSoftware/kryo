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
