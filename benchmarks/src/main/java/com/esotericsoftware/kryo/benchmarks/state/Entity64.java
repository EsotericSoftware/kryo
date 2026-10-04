package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity64 {
	@Tag(0) private String f0_entity64;
	@Tag(1) private String f1_entity64;
	@Tag(2) private Map<String, Entity13> f2_entity64;
	@Tag(3) private Entity32 f3_entity64;
	@Tag(4) private String f4_entity64;
	@Tag(5) private Entity34 f5_entity64;

	public Entity64 () {
	}

	void init (Gen g) {
		f0_entity64 = g.string();
		f1_entity64 = g.string();
		f2_entity64 = g.r.nextInt(10) == 0 ? null : new HashMap<>(); if (f2_entity64 != null) for (int i = 0, n = g.size(); i < n; i++) f2_entity64.put(g.key(), g.obj(Entity13.class, () -> Entity13.create(g)));
		f3_entity64 = g.r.nextInt(7) == 0 ? null : g.obj(Entity32.class, () -> Entity32.create(g));
		f4_entity64 = g.string();
		f5_entity64 = g.r.nextInt(7) == 0 ? null : g.obj(Entity34.class, () -> Entity34.create(g));
	}

	static Entity64 create (Gen g) {
		Entity64 o = new Entity64();
		o.init(g);
		return o;
	}
}
