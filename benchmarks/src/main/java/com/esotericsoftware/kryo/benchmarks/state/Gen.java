package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

import java.util.function.Supplier;

/** Deterministic data generator. Every fifth requested object reuses an existing instance of its class (shared references). */
final class Gen {
	static final String[] WORDS = "alpha beta gamma delta order customer invoice account session token product price amount status pending active closed region europe north south config value limit timeout retry user admin group role permission cart item shipping address street city zip country phone email note comment description title label".split(" ");
	final Random r;
	final Map<Class, List<Object>> pool = new HashMap<>();

	Gen (long seed) {
		r = new Random(seed);
	}

	<T> T obj (Class<T> type, Supplier<T> create) {
		List<Object> existing = pool.computeIfAbsent(type, k -> new ArrayList<>());
		if (!existing.isEmpty() && r.nextInt(5) == 0) return (T)existing.get(r.nextInt(existing.size()));
		T o = create.get();
		existing.add(o);
		return o;
	}

	int size () {
		int n = r.nextInt(10);
		return n < 3 ? n : n < 8 ? n - 2 : 4 + r.nextInt(8);
	}

	String string () {
		int n = r.nextInt(10);
		if (n == 0) return null;
		if (n == 1) return "";
		StringBuilder b = new StringBuilder(WORDS[r.nextInt(WORDS.length)]);
		for (int i = 0, c = r.nextInt(4); i < c; i++)
			b.append(' ').append(WORDS[r.nextInt(WORDS.length)]);
		if (r.nextInt(20) == 0) b.append(" \u00fcber\u00e9");
		return b.toString();
	}

	String key () {
		return WORDS[r.nextInt(WORDS.length)] + "-" + r.nextInt(100000);
	}

	int[] ints () {
		if (r.nextInt(5) == 0) return null;
		int[] a = new int[r.nextInt(16)];
		for (int i = 0; i < a.length; i++)
			a[i] = r.nextInt(5000);
		return a;
	}

	double[] doubles () {
		if (r.nextInt(5) == 0) return null;
		double[] a = new double[r.nextInt(8)];
		for (int i = 0; i < a.length; i++)
			a[i] = r.nextDouble();
		return a;
	}

	<T> T pick (T[] values) {
		return values[r.nextInt(values.length)];
	}
}
