package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity74 {
	@Tag(0) private Entity29 f0_entity74;
	@Tag(1) private int f1_entity74;
	@Tag(2) private Kind0 f2_entity74;
	@Tag(3) private long f3_entity74;
	@Tag(4) private List<Entity4> f4_entity74;
	@Tag(5) private Integer f5_entity74;
	@Tag(6) private Kind2 f6_entity74;
	@Tag(7) private List<Entity38> f7_entity74;

	public Entity74 () {
	}

	void init (Gen g) {
		f0_entity74 = g.r.nextInt(7) == 0 ? null : g.obj(Entity29.class, () -> Entity29.create(g));
		f1_entity74 = g.r.nextInt(1000) - 100;
		f2_entity74 = g.r.nextInt(8) == 0 ? null : g.pick(Kind0.values());
		f3_entity74 = g.r.nextLong() >>> g.r.nextInt(64);
		f4_entity74 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f4_entity74 != null) for (int i = 0, n = g.size(); i < n; i++) f4_entity74.add(g.obj(Entity4.class, () -> Entity4.create(g)));
		f5_entity74 = g.r.nextInt(10) == 0 ? null : g.r.nextInt(100000);
		f6_entity74 = g.r.nextInt(8) == 0 ? null : g.pick(Kind2.values());
		f7_entity74 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f7_entity74 != null) for (int i = 0, n = g.size(); i < n; i++) f7_entity74.add(g.obj(Entity38.class, () -> Entity38.create(g)));
	}

	static Entity74 create (Gen g) {
		Entity74 o = new Entity74();
		o.init(g);
		return o;
	}
}
