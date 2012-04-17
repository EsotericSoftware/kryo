
package com.esotericsoftware.kryo;

import java.util.Arrays;
import java.util.List;

import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.serializers.ArraysAsListSerializer;

public class ArraysAsListSerializerTest extends KryoTestCase {
	public void testArraysAsList () {
		kryo.setRegistrationRequired(false);
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		kryo.addDefaultSerializer("java.util.Arrays$ArrayList", ArraysAsListSerializer.class);
		List<Integer> test = Arrays.asList(1, 2, 3);
		roundTrip(60, test);
	}
}
