package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity49 {
	@Tag(0) private Kind5 f0_entity49;
	@Tag(1) private Entity15 f1_entity49;
	@Tag(2) private long f2_entity49;
	@Tag(3) private int f3_entity49;
	@Tag(4) private int f4_entity49;
	@Tag(5) private Entity33 f5_entity49;

	public Entity49 () {
	}

	void init (Gen g) {
		f0_entity49 = g.r.nextInt(8) == 0 ? null : g.pick(Kind5.values());
		f1_entity49 = g.r.nextInt(7) == 0 ? null : g.obj(Entity15.class, () -> Entity15.create(g));
		f2_entity49 = g.r.nextLong() >>> g.r.nextInt(64);
		f3_entity49 = g.r.nextInt(1000) - 100;
		f4_entity49 = g.r.nextInt(1000) - 100;
		f5_entity49 = g.r.nextInt(7) == 0 ? null : g.obj(Entity33.class, () -> Entity33.create(g));
	}

	static Entity49 create (Gen g) {
		Entity49 o = new Entity49();
		o.init(g);
		return o;
	}
}
