package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity60 {
	@Tag(0) private boolean f0_entity60;
	@Tag(1) private Kind4 f1_entity60;
	@Tag(2) private Entity30 f2_entity60;
	@Tag(3) private List<Entity17> f3_entity60;
	@Tag(4) private String f4_entity60;
	@Tag(5) private int f5_entity60;
	@Tag(6) private double f6_entity60;
	@Tag(7) private int f7_entity60;
	@Tag(8) private long f8_entity60;

	public Entity60 () {
	}

	void init (Gen g) {
		f0_entity60 = g.r.nextBoolean();
		f1_entity60 = g.r.nextInt(8) == 0 ? null : g.pick(Kind4.values());
		f2_entity60 = g.r.nextInt(7) == 0 ? null : g.obj(Entity30.class, () -> Entity30.create(g));
		f3_entity60 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f3_entity60 != null) for (int i = 0, n = g.size(); i < n; i++) f3_entity60.add(g.obj(Entity17.class, () -> Entity17.create(g)));
		f4_entity60 = g.string();
		f5_entity60 = g.r.nextInt(1000) - 100;
		f6_entity60 = g.r.nextDouble() * 1000;
		f7_entity60 = g.r.nextInt(1000) - 100;
		f8_entity60 = g.r.nextLong() >>> g.r.nextInt(64);
	}

	static Entity60 create (Gen g) {
		Entity60 o = new Entity60();
		o.init(g);
		return o;
	}
}
