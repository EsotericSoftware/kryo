package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity51 {
	@Tag(0) private int f0_entity51;
	@Tag(1) private int f1_entity51;
	@Tag(2) private long f2_entity51;
	@Tag(3) private String f3_entity51;
	@Tag(4) private String f4_entity51;
	@Tag(5) private Entity2 f5_entity51;
	@Tag(6) private List<Entity30> f6_entity51;
	@Tag(7) private String f7_entity51;

	public Entity51 () {
	}

	void init (Gen g) {
		f0_entity51 = g.r.nextInt(1000) - 100;
		f1_entity51 = g.r.nextInt(1000) - 100;
		f2_entity51 = g.r.nextLong() >>> g.r.nextInt(64);
		f3_entity51 = g.string();
		f4_entity51 = g.string();
		f5_entity51 = g.r.nextInt(7) == 0 ? null : g.obj(Entity2.class, () -> Entity2.create(g));
		f6_entity51 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f6_entity51 != null) for (int i = 0, n = g.size(); i < n; i++) f6_entity51.add(g.obj(Entity30.class, () -> Entity30.create(g)));
		f7_entity51 = g.string();
	}

	static Entity51 create (Gen g) {
		Entity51 o = new Entity51();
		o.init(g);
		return o;
	}
}
