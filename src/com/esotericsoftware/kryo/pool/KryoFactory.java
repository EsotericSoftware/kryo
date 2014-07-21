package com.esotericsoftware.kryo.pool;

import com.esotericsoftware.kryo.Kryo;

/**
 * Factory to create new configured instances of {@link Kryo}.
 * 
 * @author Martin Grotzke
 */
public interface KryoFactory {
	Kryo create();
}