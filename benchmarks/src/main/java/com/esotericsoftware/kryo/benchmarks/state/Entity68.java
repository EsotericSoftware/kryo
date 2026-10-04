package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity68 {
	@Tag(0) private long f0_entity68;
	@Tag(1) private String f1_entity68;
	@Tag(2) private List<Entity4> f2_entity68;
	@Tag(3) private int f3_entity68;
	@Tag(4) private long f4_entity68;
	@Tag(5) private long f5_entity68;
	@Tag(6) private Long f6_entity68;

	public Entity68 () {
	}

	void init (Gen g) {
		f0_entity68 = g.r.nextLong() >>> g.r.nextInt(64);
		f1_entity68 = g.string();
		f2_entity68 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f2_entity68 != null) for (int i = 0, n = g.size(); i < n; i++) f2_entity68.add(g.obj(Entity4.class, () -> Entity4.create(g)));
		f3_entity68 = g.r.nextInt(1000) - 100;
		f4_entity68 = g.r.nextLong() >>> g.r.nextInt(64);
		f5_entity68 = g.r.nextLong() >>> g.r.nextInt(64);
		f6_entity68 = g.r.nextInt(10) == 0 ? null : g.r.nextLong();
	}

	static Entity68 create (Gen g) {
		Entity68 o = new Entity68();
		o.init(g);
		return o;
	}
}
