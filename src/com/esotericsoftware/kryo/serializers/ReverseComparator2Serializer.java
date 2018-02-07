package com.esotericsoftware.kryo.serializers;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Serializes the {@link Comparator} wrapper called {@code java.util.Collections$ReverseComparator2}, which is
 * returned by the {@link Collections#reverseOrder(Comparator)} method.
 *
 * @author Mitchell Skaggs
 */

public class ReverseComparator2Serializer extends Serializer<Comparator<?>> {
	private static Class<? extends Comparator<?>> reverseComparator2Class;
	private static Field internalComparatorField;

	static {
		try {
			//noinspection unchecked
			reverseComparator2Class = (Class<? extends Comparator<?>>) Class
					.forName("java.util.Collections$ReverseComparator2");
			internalComparatorField = reverseComparator2Class.getDeclaredField("cmp");

			internalComparatorField.setAccessible(true);

		} catch (ClassNotFoundException | NoSuchFieldException e) {
			throw new RuntimeException("Could not obtain Collections$ReverseComparator2 or its methods via reflection",
					e);
		}
	}

	public ReverseComparator2Serializer() {
		super(false, true);
	}

	public static void addDefaultSerializers(Kryo kryo) {
		kryo.addDefaultSerializer(reverseComparator2Class, new ReverseComparator2Serializer());
	}

	@Override
	public void write(Kryo kryo, Output output, Comparator<?> object) {
		try {
			Comparator<?> internalComparator = (Comparator<?>) internalComparatorField.get(object);
			kryo.writeClassAndObject(output, internalComparator);

		} catch (IllegalAccessException e) {
			throw new RuntimeException("Could not write comparator!", e);
		}
	}

	@Override
	public Comparator<?> read(Kryo kryo, Input input, Class<Comparator<?>> type) {
		return Collections.reverseOrder((Comparator<?>) kryo.readClassAndObject(input));
	}
}
