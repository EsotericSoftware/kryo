package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity66 {
	@Tag(0) private long f0_entity66;
	@Tag(1) private Entity21 f1_entity66;
	@Tag(2) private String f2_entity66;
	@Tag(3) private String f3_entity66;
	@Tag(4) private int f4_entity66;
	@Tag(5) private Kind1 f5_entity66;
	@Tag(6) private String f6_entity66;

	public Entity66 () {
	}

	void init (Gen g) {
		f0_entity66 = g.r.nextLong() >>> g.r.nextInt(64);
		f1_entity66 = g.r.nextInt(7) == 0 ? null : g.obj(Entity21.class, () -> Entity21.create(g));
		f2_entity66 = g.string();
		f3_entity66 = g.string();
		f4_entity66 = g.r.nextInt(1000) - 100;
		f5_entity66 = g.r.nextInt(8) == 0 ? null : g.pick(Kind1.values());
		f6_entity66 = g.string();
	}

	static Entity66 create (Gen g) {
		Entity66 o = new Entity66();
		o.init(g);
		return o;
	}
}
