
package com.esotericsoftware.kryo.serialize;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Kryo;

/**
 * Serializes class objects.
 */
public class ClassSerializer extends SimpleSerializer<Class> {
	private final Kryo kryo;

	public ClassSerializer (Kryo kryo) {
		this.kryo = kryo;
	}

	public void write (ByteBuffer buffer, Class clazz) {
		kryo.writeClass(buffer, clazz);
	}

	public Class read (ByteBuffer buffer) {
		return kryo.readClass(buffer).getType();
	}
}
