package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity39 {
	@Tag(0) private Entity31 f0_entity39;
	@Tag(1) private long f1_entity39;
	@Tag(2) private int f2_entity39;
	@Tag(3) private int[] f3_entity39;
	@Tag(4) private Long f4_entity39;
	@Tag(5) private boolean f5_entity39;

	public Entity39 () {
	}

	void init (Gen g) {
		f0_entity39 = g.r.nextInt(7) == 0 ? null : g.obj(Entity31.class, () -> Entity31.create(g));
		f1_entity39 = g.r.nextLong() >>> g.r.nextInt(64);
		f2_entity39 = g.r.nextInt(1000) - 100;
		f3_entity39 = g.ints();
		f4_entity39 = g.r.nextInt(10) == 0 ? null : g.r.nextLong();
		f5_entity39 = g.r.nextBoolean();
	}

	static Entity39 create (Gen g) {
		Entity39 o = new Entity39();
		o.init(g);
		return o;
	}
}
