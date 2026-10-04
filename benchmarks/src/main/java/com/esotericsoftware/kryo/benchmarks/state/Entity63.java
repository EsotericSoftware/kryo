package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity63 {
	@Tag(0) private Entity20 f0_entity63;
	@Tag(1) private Entity12 f1_entity63;
	@Tag(2) private String f2_entity63;
	@Tag(3) private int f3_entity63;
	@Tag(4) private Kind5 f4_entity63;
	@Tag(5) private String f5_entity63;
	@Tag(6) private String f6_entity63;

	public Entity63 () {
	}

	void init (Gen g) {
		f0_entity63 = g.r.nextInt(7) == 0 ? null : g.obj(Entity20.class, () -> Entity20.create(g));
		f1_entity63 = g.r.nextInt(7) == 0 ? null : g.obj(Entity12.class, () -> Entity12.create(g));
		f2_entity63 = g.string();
		f3_entity63 = g.r.nextInt(1000) - 100;
		f4_entity63 = g.r.nextInt(8) == 0 ? null : g.pick(Kind5.values());
		f5_entity63 = g.string();
		f6_entity63 = g.string();
	}

	static Entity63 create (Gen g) {
		Entity63 o = new Entity63();
		o.init(g);
		return o;
	}
}
