package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity47 {
	@Tag(0) private Kind5 f0_entity47;
	@Tag(1) private String f1_entity47;
	@Tag(2) private double f2_entity47;
	@Tag(3) private double f3_entity47;
	@Tag(4) private double f4_entity47;
	@Tag(5) private long f5_entity47;
	@Tag(6) private int f6_entity47;
	@Tag(7) private String f7_entity47;
	@Tag(8) private int f8_entity47;

	public Entity47 () {
	}

	void init (Gen g) {
		f0_entity47 = g.r.nextInt(8) == 0 ? null : g.pick(Kind5.values());
		f1_entity47 = g.string();
		f2_entity47 = g.r.nextDouble() * 1000;
		f3_entity47 = g.r.nextDouble() * 1000;
		f4_entity47 = g.r.nextDouble() * 1000;
		f5_entity47 = g.r.nextLong() >>> g.r.nextInt(64);
		f6_entity47 = g.r.nextInt(1000) - 100;
		f7_entity47 = g.string();
		f8_entity47 = g.r.nextInt(1000) - 100;
	}

	static Entity47 create (Gen g) {
		Entity47 o = new Entity47();
		o.init(g);
		return o;
	}
}
