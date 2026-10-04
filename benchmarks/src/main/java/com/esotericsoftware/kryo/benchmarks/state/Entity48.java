package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity48 {
	@Tag(0) private double f0_entity48;
	@Tag(1) private Entity2 f1_entity48;
	@Tag(2) private String f2_entity48;
	@Tag(3) private int f3_entity48;

	public Entity48 () {
	}

	void init (Gen g) {
		f0_entity48 = g.r.nextDouble() * 1000;
		f1_entity48 = g.r.nextInt(7) == 0 ? null : g.obj(Entity2.class, () -> Entity2.create(g));
		f2_entity48 = g.string();
		f3_entity48 = g.r.nextInt(1000) - 100;
	}

	static Entity48 create (Gen g) {
		Entity48 o = new Entity48();
		o.init(g);
		return o;
	}
}
