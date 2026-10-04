package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity52 {
	@Tag(0) private boolean f0_entity52;
	@Tag(1) private Base0 f1_entity52;
	@Tag(2) private int f2_entity52;
	@Tag(3) private String f3_entity52;
	@Tag(4) private long f4_entity52;

	public Entity52 () {
	}

	void init (Gen g) {
		f0_entity52 = g.r.nextBoolean();
		f1_entity52 = g.r.nextInt(7) == 0 ? null : Base0.createAny(g);
		f2_entity52 = g.r.nextInt(1000) - 100;
		f3_entity52 = g.string();
		f4_entity52 = g.r.nextLong() >>> g.r.nextInt(64);
	}

	static Entity52 create (Gen g) {
		Entity52 o = new Entity52();
		o.init(g);
		return o;
	}
}
