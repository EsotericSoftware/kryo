package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity30 {
	@Tag(0) private Kind3 f0_entity30;
	@Tag(1) private int f1_entity30;
	@Tag(2) private boolean f2_entity30;
	@Tag(3) private int[] f3_entity30;
	@Tag(4) private Kind0 f4_entity30;
	@Tag(5) private double f5_entity30;
	@Tag(6) private String f6_entity30;
	@Tag(7) private int f7_entity30;
	@Tag(8) private long f8_entity30;
	@Tag(9) private int f9_entity30;

	public Entity30 () {
	}

	void init (Gen g) {
		f0_entity30 = g.r.nextInt(8) == 0 ? null : g.pick(Kind3.values());
		f1_entity30 = g.r.nextInt(1000) - 100;
		f2_entity30 = g.r.nextBoolean();
		f3_entity30 = g.ints();
		f4_entity30 = g.r.nextInt(8) == 0 ? null : g.pick(Kind0.values());
		f5_entity30 = g.r.nextDouble() * 1000;
		f6_entity30 = g.string();
		f7_entity30 = g.r.nextInt(1000) - 100;
		f8_entity30 = g.r.nextLong() >>> g.r.nextInt(64);
		f9_entity30 = g.r.nextInt(1000) - 100;
	}

	static Entity30 create (Gen g) {
		Entity30 o = new Entity30();
		o.init(g);
		return o;
	}
}
