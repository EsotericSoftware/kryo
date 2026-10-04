package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity35 {
	@Tag(0) private boolean f0_entity35;
	@Tag(1) private List<Entity23> f1_entity35;
	@Tag(2) private long f2_entity35;
	@Tag(3) private double f3_entity35;
	@Tag(4) private Kind5 f4_entity35;

	public Entity35 () {
	}

	void init (Gen g) {
		f0_entity35 = g.r.nextBoolean();
		f1_entity35 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f1_entity35 != null) for (int i = 0, n = g.size(); i < n; i++) f1_entity35.add(g.obj(Entity23.class, () -> Entity23.create(g)));
		f2_entity35 = g.r.nextLong() >>> g.r.nextInt(64);
		f3_entity35 = g.r.nextDouble() * 1000;
		f4_entity35 = g.r.nextInt(8) == 0 ? null : g.pick(Kind5.values());
	}

	static Entity35 create (Gen g) {
		Entity35 o = new Entity35();
		o.init(g);
		return o;
	}
}
