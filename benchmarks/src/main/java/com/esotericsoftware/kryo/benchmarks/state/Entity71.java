package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity71 {
	@Tag(0) private long f0_entity71;
	@Tag(1) private boolean f1_entity71;
	@Tag(2) private Entity15 f2_entity71;
	@Tag(3) private Kind3 f3_entity71;
	@Tag(4) private Kind2 f4_entity71;
	@Tag(5) private String f5_entity71;

	public Entity71 () {
	}

	void init (Gen g) {
		f0_entity71 = g.r.nextLong() >>> g.r.nextInt(64);
		f1_entity71 = g.r.nextBoolean();
		f2_entity71 = g.r.nextInt(7) == 0 ? null : g.obj(Entity15.class, () -> Entity15.create(g));
		f3_entity71 = g.r.nextInt(8) == 0 ? null : g.pick(Kind3.values());
		f4_entity71 = g.r.nextInt(8) == 0 ? null : g.pick(Kind2.values());
		f5_entity71 = g.string();
	}

	static Entity71 create (Gen g) {
		Entity71 o = new Entity71();
		o.init(g);
		return o;
	}
}
