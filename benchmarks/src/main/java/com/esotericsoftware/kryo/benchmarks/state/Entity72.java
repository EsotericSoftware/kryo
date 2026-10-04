package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity72 {
	@Tag(0) private int f0_entity72;
	@Tag(1) private String f1_entity72;
	@Tag(2) private List<Entity49> f2_entity72;
	@Tag(3) private int f3_entity72;
	@Tag(4) private List<Entity23> f4_entity72;
	@Tag(5) private Map<String, Entity66> f5_entity72;
	@Tag(6) private Entity50 f6_entity72;
	@Tag(7) private int[] f7_entity72;
	@Tag(8) private Entity17 f8_entity72;

	public Entity72 () {
	}

	void init (Gen g) {
		f0_entity72 = g.r.nextInt(1000) - 100;
		f1_entity72 = g.string();
		f2_entity72 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f2_entity72 != null) for (int i = 0, n = g.size(); i < n; i++) f2_entity72.add(g.obj(Entity49.class, () -> Entity49.create(g)));
		f3_entity72 = g.r.nextInt(1000) - 100;
		f4_entity72 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f4_entity72 != null) for (int i = 0, n = g.size(); i < n; i++) f4_entity72.add(g.obj(Entity23.class, () -> Entity23.create(g)));
		f5_entity72 = g.r.nextInt(10) == 0 ? null : new HashMap<>(); if (f5_entity72 != null) for (int i = 0, n = g.size(); i < n; i++) f5_entity72.put(g.key(), g.obj(Entity66.class, () -> Entity66.create(g)));
		f6_entity72 = g.r.nextInt(7) == 0 ? null : g.obj(Entity50.class, () -> Entity50.create(g));
		f7_entity72 = g.ints();
		f8_entity72 = g.r.nextInt(7) == 0 ? null : g.obj(Entity17.class, () -> Entity17.create(g));
	}

	static Entity72 create (Gen g) {
		Entity72 o = new Entity72();
		o.init(g);
		return o;
	}
}
