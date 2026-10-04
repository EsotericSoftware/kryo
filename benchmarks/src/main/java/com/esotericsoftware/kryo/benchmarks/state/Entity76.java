package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity76 {
	@Tag(0) private double f0_entity76;
	@Tag(1) private boolean f1_entity76;
	@Tag(2) private long f2_entity76;
	@Tag(3) private boolean f3_entity76;
	@Tag(4) private String f4_entity76;
	@Tag(5) private int f5_entity76;
	@Tag(6) private Entity59 f6_entity76;
	@Tag(7) private boolean f7_entity76;
	@Tag(8) private int f8_entity76;
	@Tag(9) private long f9_entity76;

	public Entity76 () {
	}

	void init (Gen g) {
		f0_entity76 = g.r.nextDouble() * 1000;
		f1_entity76 = g.r.nextBoolean();
		f2_entity76 = g.r.nextLong() >>> g.r.nextInt(64);
		f3_entity76 = g.r.nextBoolean();
		f4_entity76 = g.string();
		f5_entity76 = g.r.nextInt(1000) - 100;
		f6_entity76 = g.r.nextInt(7) == 0 ? null : g.obj(Entity59.class, () -> Entity59.create(g));
		f7_entity76 = g.r.nextBoolean();
		f8_entity76 = g.r.nextInt(1000) - 100;
		f9_entity76 = g.r.nextLong() >>> g.r.nextInt(64);
	}

	static Entity76 create (Gen g) {
		Entity76 o = new Entity76();
		o.init(g);
		return o;
	}
}
