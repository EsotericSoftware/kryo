package com.esotericsoftware.kryo.pool;

import com.esotericsoftware.kryo.Kryo;

/**
 * Callback to run with a provided kryo instance.
 *
 * @author Martin Grotzke
 *
 * @param <T> The type of the result of the interaction with kryo.
 */
public interface KryoCallback<T> {
	T execute(Kryo kryo);
}