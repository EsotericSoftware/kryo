package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity27 {
	@Tag(0) private long f0_entity27;
	@Tag(1) private Kind0 f1_entity27;
	@Tag(2) private double f2_entity27;
	@Tag(3) private boolean f3_entity27;
	@Tag(4) private int f4_entity27;
	@Tag(5) private String f5_entity27;
	@Tag(6) private Integer f6_entity27;
	@Tag(7) private boolean f7_entity27;

	public Entity27 () {
	}

	void init (Gen g) {
		f0_entity27 = g.r.nextLong() >>> g.r.nextInt(64);
		f1_entity27 = g.r.nextInt(8) == 0 ? null : g.pick(Kind0.values());
		f2_entity27 = g.r.nextDouble() * 1000;
		f3_entity27 = g.r.nextBoolean();
		f4_entity27 = g.r.nextInt(1000) - 100;
		f5_entity27 = g.string();
		f6_entity27 = g.r.nextInt(10) == 0 ? null : g.r.nextInt(100000);
		f7_entity27 = g.r.nextBoolean();
	}

	static Entity27 create (Gen g) {
		Entity27 o = new Entity27();
		o.init(g);
		return o;
	}
}
