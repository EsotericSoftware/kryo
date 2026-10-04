package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity18 {
	@Tag(0) private int f0_entity18;
	@Tag(1) private long f1_entity18;
	@Tag(2) private Kind1 f2_entity18;
	@Tag(3) private int f3_entity18;
	@Tag(4) private Kind2 f4_entity18;
	@Tag(5) private boolean f5_entity18;
	@Tag(6) private Kind2 f6_entity18;
	@Tag(7) private int f7_entity18;

	public Entity18 () {
	}

	void init (Gen g) {
		f0_entity18 = g.r.nextInt(1000) - 100;
		f1_entity18 = g.r.nextLong() >>> g.r.nextInt(64);
		f2_entity18 = g.r.nextInt(8) == 0 ? null : g.pick(Kind1.values());
		f3_entity18 = g.r.nextInt(1000) - 100;
		f4_entity18 = g.r.nextInt(8) == 0 ? null : g.pick(Kind2.values());
		f5_entity18 = g.r.nextBoolean();
		f6_entity18 = g.r.nextInt(8) == 0 ? null : g.pick(Kind2.values());
		f7_entity18 = g.r.nextInt(1000) - 100;
	}

	static Entity18 create (Gen g) {
		Entity18 o = new Entity18();
		o.init(g);
		return o;
	}
}
