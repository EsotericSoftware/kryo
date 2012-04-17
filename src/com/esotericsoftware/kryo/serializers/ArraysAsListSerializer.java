
package com.esotericsoftware.kryo.serializers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

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
public class ArraysAsListSerializer extends CollectionSerializer {
	public ArraysAsListSerializer (Kryo kryo) {
		super(kryo);
	}

	public Collection create (Kryo kryo, Input input, Class type) {
		return new ArrayList();
	}
}
