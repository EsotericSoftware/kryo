package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity42 {
	@Tag(0) private Entity23 f0_entity42;
	@Tag(1) private Entity17 f1_entity42;
	@Tag(2) private String f2_entity42;
	@Tag(3) private Kind2 f3_entity42;
	@Tag(4) private Entity13 f4_entity42;

	public Entity42 () {
	}

	void init (Gen g) {
		f0_entity42 = g.r.nextInt(7) == 0 ? null : g.obj(Entity23.class, () -> Entity23.create(g));
		f1_entity42 = g.r.nextInt(7) == 0 ? null : g.obj(Entity17.class, () -> Entity17.create(g));
		f2_entity42 = g.string();
		f3_entity42 = g.r.nextInt(8) == 0 ? null : g.pick(Kind2.values());
		f4_entity42 = g.r.nextInt(7) == 0 ? null : g.obj(Entity13.class, () -> Entity13.create(g));
	}

	static Entity42 create (Gen g) {
		Entity42 o = new Entity42();
		o.init(g);
		return o;
	}
}
