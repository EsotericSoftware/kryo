package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity54 {
	@Tag(0) private int f0_entity54;
	@Tag(1) private double f1_entity54;
	@Tag(2) private Entity33 f2_entity54;
	@Tag(3) private long f3_entity54;
	@Tag(4) private int f4_entity54;
	@Tag(5) private int f5_entity54;
	@Tag(6) private Kind2 f6_entity54;
	@Tag(7) private Long f7_entity54;
	@Tag(8) private int f8_entity54;
	@Tag(9) private Entity14 f9_entity54;
	@Tag(10) private List<Entity25> f10_entity54;

	public Entity54 () {
	}

	void init (Gen g) {
		f0_entity54 = g.r.nextInt(1000) - 100;
		f1_entity54 = g.r.nextDouble() * 1000;
		f2_entity54 = g.r.nextInt(7) == 0 ? null : g.obj(Entity33.class, () -> Entity33.create(g));
		f3_entity54 = g.r.nextLong() >>> g.r.nextInt(64);
		f4_entity54 = g.r.nextInt(1000) - 100;
		f5_entity54 = g.r.nextInt(1000) - 100;
		f6_entity54 = g.r.nextInt(8) == 0 ? null : g.pick(Kind2.values());
		f7_entity54 = g.r.nextInt(10) == 0 ? null : g.r.nextLong();
		f8_entity54 = g.r.nextInt(1000) - 100;
		f9_entity54 = g.r.nextInt(7) == 0 ? null : g.obj(Entity14.class, () -> Entity14.create(g));
		f10_entity54 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f10_entity54 != null) for (int i = 0, n = g.size(); i < n; i++) f10_entity54.add(g.obj(Entity25.class, () -> Entity25.create(g)));
	}

	static Entity54 create (Gen g) {
		Entity54 o = new Entity54();
		o.init(g);
		return o;
	}
}
