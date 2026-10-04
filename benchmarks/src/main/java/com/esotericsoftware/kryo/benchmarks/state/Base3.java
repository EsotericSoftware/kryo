package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public abstract class Base3 {
	@Tag(0) protected int[] f0_base3;
	@Tag(1) protected Kind3 f1_base3;
	@Tag(2) protected Base7 f2_base3;

	public Base3 () {
	}

	void init (Gen g) {
		f0_base3 = g.ints();
		f1_base3 = g.r.nextInt(8) == 0 ? null : g.pick(Kind3.values());
		f2_base3 = g.r.nextInt(7) == 0 ? null : Base7.createAny(g);
	}

	static Base3 createAny (Gen g) {
		switch (g.r.nextInt(3)) {
		case 0: return g.obj(Entity4.class, () -> Entity4.create(g));
		case 1: return g.obj(Entity5.class, () -> Entity5.create(g));
		case 2: return g.obj(Entity6.class, () -> Entity6.create(g));
		default: throw new IllegalStateException();
		}
	}
}
