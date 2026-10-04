package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity59 {
	@Tag(0) private int f0_entity59;
	@Tag(1) private Long f1_entity59;
	@Tag(2) private Integer f2_entity59;
	@Tag(3) private Entity45 f3_entity59;
	@Tag(4) private double f4_entity59;
	@Tag(5) private int f5_entity59;
	@Tag(6) private Map<String, Entity52> f6_entity59;
	@Tag(7) private int f7_entity59;
	@Tag(8) private Entity10 f8_entity59;
	@Tag(9) private String f9_entity59;

	public Entity59 () {
	}

	void init (Gen g) {
		f0_entity59 = g.r.nextInt(1000) - 100;
		f1_entity59 = g.r.nextInt(10) == 0 ? null : g.r.nextLong();
		f2_entity59 = g.r.nextInt(10) == 0 ? null : g.r.nextInt(100000);
		f3_entity59 = g.r.nextInt(7) == 0 ? null : g.obj(Entity45.class, () -> Entity45.create(g));
		f4_entity59 = g.r.nextDouble() * 1000;
		f5_entity59 = g.r.nextInt(1000) - 100;
		f6_entity59 = g.r.nextInt(10) == 0 ? null : new HashMap<>(); if (f6_entity59 != null) for (int i = 0, n = g.size(); i < n; i++) f6_entity59.put(g.key(), g.obj(Entity52.class, () -> Entity52.create(g)));
		f7_entity59 = g.r.nextInt(1000) - 100;
		f8_entity59 = g.r.nextInt(7) == 0 ? null : g.obj(Entity10.class, () -> Entity10.create(g));
		f9_entity59 = g.string();
	}

	static Entity59 create (Gen g) {
		Entity59 o = new Entity59();
		o.init(g);
		return o;
	}
}
