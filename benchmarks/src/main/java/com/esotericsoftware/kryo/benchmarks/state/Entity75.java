package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity75 {
	@Tag(0) private boolean f0_entity75;
	@Tag(1) private List<Entity25> f1_entity75;
	@Tag(2) private List<Entity46> f2_entity75;
	@Tag(3) private String f3_entity75;
	@Tag(4) private int f4_entity75;

	public Entity75 () {
	}

	void init (Gen g) {
		f0_entity75 = g.r.nextBoolean();
		f1_entity75 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f1_entity75 != null) for (int i = 0, n = g.size(); i < n; i++) f1_entity75.add(g.obj(Entity25.class, () -> Entity25.create(g)));
		f2_entity75 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f2_entity75 != null) for (int i = 0, n = g.size(); i < n; i++) f2_entity75.add(g.obj(Entity46.class, () -> Entity46.create(g)));
		f3_entity75 = g.string();
		f4_entity75 = g.r.nextInt(1000) - 100;
	}

	static Entity75 create (Gen g) {
		Entity75 o = new Entity75();
		o.init(g);
		return o;
	}
}
