package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity44 {
	@Tag(0) private Long f0_entity44;
	@Tag(1) private String f1_entity44;
	@Tag(2) private List<Entity19> f2_entity44;
	@Tag(3) private Kind4 f3_entity44;
	@Tag(4) private Long f4_entity44;
	@Tag(5) private Entity17 f5_entity44;
	@Tag(6) private Map<String, Entity20> f6_entity44;

	public Entity44 () {
	}

	void init (Gen g) {
		f0_entity44 = g.r.nextInt(10) == 0 ? null : g.r.nextLong();
		f1_entity44 = g.string();
		f2_entity44 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f2_entity44 != null) for (int i = 0, n = g.size(); i < n; i++) f2_entity44.add(g.obj(Entity19.class, () -> Entity19.create(g)));
		f3_entity44 = g.r.nextInt(8) == 0 ? null : g.pick(Kind4.values());
		f4_entity44 = g.r.nextInt(10) == 0 ? null : g.r.nextLong();
		f5_entity44 = g.r.nextInt(7) == 0 ? null : g.obj(Entity17.class, () -> Entity17.create(g));
		f6_entity44 = g.r.nextInt(10) == 0 ? null : new HashMap<>(); if (f6_entity44 != null) for (int i = 0, n = g.size(); i < n; i++) f6_entity44.put(g.key(), g.obj(Entity20.class, () -> Entity20.create(g)));
	}

	static Entity44 create (Gen g) {
		Entity44 o = new Entity44();
		o.init(g);
		return o;
	}
}
