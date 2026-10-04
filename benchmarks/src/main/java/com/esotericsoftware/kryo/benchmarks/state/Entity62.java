package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity62 {
	@Tag(0) private Map<String, Entity4> f0_entity62;
	@Tag(1) private Integer f1_entity62;
	@Tag(2) private double f2_entity62;

	public Entity62 () {
	}

	void init (Gen g) {
		f0_entity62 = g.r.nextInt(10) == 0 ? null : new HashMap<>(); if (f0_entity62 != null) for (int i = 0, n = g.size(); i < n; i++) f0_entity62.put(g.key(), g.obj(Entity4.class, () -> Entity4.create(g)));
		f1_entity62 = g.r.nextInt(10) == 0 ? null : g.r.nextInt(100000);
		f2_entity62 = g.r.nextDouble() * 1000;
	}

	static Entity62 create (Gen g) {
		Entity62 o = new Entity62();
		o.init(g);
		return o;
	}
}
