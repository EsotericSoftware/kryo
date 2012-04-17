
package com.esotericsoftware.kryo.serializers;

import java.util.Arrays;

import com.esotericsoftware.kryo.Kryo;

/** Serializer for lists created via {@link Arrays#asList(Object...)}. It can be used in this way:
 * <p>
 * <code>
 * kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
 * kryo.addDefaultSerializer("java.util.Arrays$ArrayList", ArraysAsListSerializer.class);
 * </code>
 * <p>
 * Note this will only work with {@link Kryo#setInstantiatorStrategy(org.objenesis.strategy.InstantiatorStrategy)
 * StdInstantiatorStrategy} and Arrays$ArrayList is a private class, so a different JRE implementation may not have this class at
 * all. */
public class ArraysAsListSerializer extends FieldSerializer {
	public ArraysAsListSerializer (Kryo kryo, Class type) {
		super(kryo, type);
		getField("a").setSerializer(kryo.getArraySerializer());
	}
}
