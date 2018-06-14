package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;

class RoundTripAssertionOutput<T> {
    private final Input input;
    private final T deserializeObject;

    RoundTripAssertionOutput(Input input, T object) {
        this.input = input;
        this.deserializeObject = object;
    }

    Input getKryoInput() {
        return input;
    }

    T getDeserializeObject() {
        return deserializeObject;
    }
}
