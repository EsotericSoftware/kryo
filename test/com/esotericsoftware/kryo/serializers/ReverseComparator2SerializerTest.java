package com.esotericsoftware.kryo.serializers;

import java.util.Collections;
import java.util.Comparator;

import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * @author Mitchell Skaggs
 */
public class ReverseComparator2SerializerTest extends KryoTestCase {

	public void testSimpleComparatorSerializer() {
		kryo.register(TestComparator.class);

		final Comparator<String> initialComparator = new TestComparator<>();
		// test directly
		Output output = new Output(1024);
		kryo.writeClassAndObject(output, initialComparator);
		byte[] bytes = output.toBytes();

		//noinspection unchecked
		final Comparator<String> serializedComparator = (Comparator<String>) kryo.readClassAndObject(new Input(bytes));

		assertEquals(initialComparator, serializedComparator);
	}

	public void testReverseComparatorSerializer() {
		kryo.register(TestComparator.class);
		kryo.register(Collections.reverseOrder(new TestComparator<String>()).getClass());

		final Comparator<String> initialComparator = Collections.reverseOrder(new TestComparator<String>());
		// test directly
		Output output = new Output(1024);
		kryo.writeClassAndObject(output, initialComparator);
		byte[] bytes = output.toBytes();

		//noinspection unchecked
		final Comparator<String> serializedComparator = (Comparator<String>) kryo.readClassAndObject(new Input(bytes));

		assertEquals(initialComparator, serializedComparator);
	}

	public static class TestComparator<T extends Comparable<T>> implements Comparator<T> {
		@Override
		public int compare(T o1, T o2) {
			return o1.compareTo(o2);
		}

		@Override
		public boolean equals(Object obj) {
			return obj != null && this.getClass().equals(obj.getClass());
		}

		@Override
		public int hashCode() {
			return 0;
		}
	}
}
