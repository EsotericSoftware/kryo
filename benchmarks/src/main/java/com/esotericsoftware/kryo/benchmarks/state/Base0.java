package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public abstract class Base0 {
	@Tag(0) protected int f0_base0;
	@Tag(1) protected boolean f1_base0;
	@Tag(2) protected int f2_base0;

	public Base0 () {
	}

	void init (Gen g) {
		f0_base0 = g.r.nextInt(1000) - 100;
		f1_base0 = g.r.nextBoolean();
		f2_base0 = g.r.nextInt(1000) - 100;
	}

	static Base0 createAny (Gen g) {
		switch (g.r.nextInt(2)) {
		case 0: return g.obj(Entity1.class, () -> Entity1.create(g));
		case 1: return g.obj(Entity2.class, () -> Entity2.create(g));
		default: throw new IllegalStateException();
		}
	}
}
