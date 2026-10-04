package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public abstract class Base7 {
	@Tag(0) protected double f0_base7;
	@Tag(1) protected Long f1_base7;
	@Tag(2) protected long f2_base7;
	@Tag(3) protected int f3_base7;

	public Base7 () {
	}

	void init (Gen g) {
		f0_base7 = g.r.nextDouble() * 1000;
		f1_base7 = g.r.nextInt(10) == 0 ? null : g.r.nextLong();
		f2_base7 = g.r.nextLong() >>> g.r.nextInt(64);
		f3_base7 = g.r.nextInt(1000) - 100;
	}

	static Base7 createAny (Gen g) {
		switch (g.r.nextInt(4)) {
		case 0: return g.obj(Entity8.class, () -> Entity8.create(g));
		case 1: return g.obj(Entity9.class, () -> Entity9.create(g));
		case 2: return g.obj(Entity10.class, () -> Entity10.create(g));
		case 3: return g.obj(Entity11.class, () -> Entity11.create(g));
		default: throw new IllegalStateException();
		}
	}
}
