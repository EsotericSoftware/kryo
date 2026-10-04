package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity57 {
	@Tag(0) private boolean f0_entity57;
	@Tag(1) private boolean f1_entity57;
	@Tag(2) private Kind5 f2_entity57;
	@Tag(3) private Entity44 f3_entity57;
	@Tag(4) private List<Entity21> f4_entity57;
	@Tag(5) private double[] f5_entity57;
	@Tag(6) private String f6_entity57;
	@Tag(7) private Integer f7_entity57;
	@Tag(8) private String f8_entity57;

	public Entity57 () {
	}

	void init (Gen g) {
		f0_entity57 = g.r.nextBoolean();
		f1_entity57 = g.r.nextBoolean();
		f2_entity57 = g.r.nextInt(8) == 0 ? null : g.pick(Kind5.values());
		f3_entity57 = g.r.nextInt(7) == 0 ? null : g.obj(Entity44.class, () -> Entity44.create(g));
		f4_entity57 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f4_entity57 != null) for (int i = 0, n = g.size(); i < n; i++) f4_entity57.add(g.obj(Entity21.class, () -> Entity21.create(g)));
		f5_entity57 = g.doubles();
		f6_entity57 = g.string();
		f7_entity57 = g.r.nextInt(10) == 0 ? null : g.r.nextInt(100000);
		f8_entity57 = g.string();
	}

	static Entity57 create (Gen g) {
		Entity57 o = new Entity57();
		o.init(g);
		return o;
	}
}
