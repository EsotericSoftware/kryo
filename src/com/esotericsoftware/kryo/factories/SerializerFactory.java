package com.esotericsoftware.kryo.factories;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

/**
 * A serializer factory that allows the creation of serializers. This factory will be called when a {@link Kryo}
 * serializer discovers a new type for which no serializer is yet known. For example, when a factory is registered
 * via {@link Kryo#setDefaultSerializer(SerializerFactory)} a different serializer can be created dependent on the
 * type of a class.
 *
 * @author Rafael Winterhalter <rafael.wth@web.de>
 */
public interface SerializerFactory {

    /**
     * Creates a new serializer
     * @param kryo The serializer instance requesting the new serializer.
     * @param type The type of the object that is to be serialized.
     * @return An implementation of a serializer that is able to serialize an object of type {@code type}.
     */
	Serializer makeSerializer (Kryo kryo, Class<?> type);
}
