package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity50 {
	@Tag(0) private Base0 f0_entity50;
	@Tag(1) private Kind4 f1_entity50;
	@Tag(2) private String f2_entity50;
	@Tag(3) private Entity27 f3_entity50;
	@Tag(4) private long f4_entity50;
	@Tag(5) private List<Entity28> f5_entity50;
	@Tag(6) private Integer f6_entity50;
	@Tag(7) private int f7_entity50;
	@Tag(8) private List<Entity27> f8_entity50;

	public Entity50 () {
	}

	void init (Gen g) {
		f0_entity50 = g.r.nextInt(7) == 0 ? null : Base0.createAny(g);
		f1_entity50 = g.r.nextInt(8) == 0 ? null : g.pick(Kind4.values());
		f2_entity50 = g.string();
		f3_entity50 = g.r.nextInt(7) == 0 ? null : g.obj(Entity27.class, () -> Entity27.create(g));
		f4_entity50 = g.r.nextLong() >>> g.r.nextInt(64);
		f5_entity50 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f5_entity50 != null) for (int i = 0, n = g.size(); i < n; i++) f5_entity50.add(g.obj(Entity28.class, () -> Entity28.create(g)));
		f6_entity50 = g.r.nextInt(10) == 0 ? null : g.r.nextInt(100000);
		f7_entity50 = g.r.nextInt(1000) - 100;
		f8_entity50 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f8_entity50 != null) for (int i = 0, n = g.size(); i < n; i++) f8_entity50.add(g.obj(Entity27.class, () -> Entity27.create(g)));
	}

	static Entity50 create (Gen g) {
		Entity50 o = new Entity50();
		o.init(g);
		return o;
	}
}
