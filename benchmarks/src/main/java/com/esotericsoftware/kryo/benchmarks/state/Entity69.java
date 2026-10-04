package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity69 {
	@Tag(0) private String f0_entity69;
	@Tag(1) private Long f1_entity69;
	@Tag(2) private List<Entity41> f2_entity69;

	public Entity69 () {
	}

	void init (Gen g) {
		f0_entity69 = g.string();
		f1_entity69 = g.r.nextInt(10) == 0 ? null : g.r.nextLong();
		f2_entity69 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f2_entity69 != null) for (int i = 0, n = g.size(); i < n; i++) f2_entity69.add(g.obj(Entity41.class, () -> Entity41.create(g)));
	}

	static Entity69 create (Gen g) {
		Entity69 o = new Entity69();
		o.init(g);
		return o;
	}
}
