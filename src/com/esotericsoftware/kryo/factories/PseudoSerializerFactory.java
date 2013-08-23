package com.esotericsoftware.kryo.factories;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

public class PseudoSerializerFactory implements SerializerFactory {

	private final Serializer<?> serializer;

	public PseudoSerializerFactory (Serializer<?> serializer) {
		this.serializer = serializer;
	}

	@Override
	public Serializer makeSerializer (Kryo kryo, Class<?> type) {
		return serializer;
	}
}
