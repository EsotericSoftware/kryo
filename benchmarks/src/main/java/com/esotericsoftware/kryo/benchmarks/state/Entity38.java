package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity38 {
	@Tag(0) private List<Entity15> f0_entity38;
	@Tag(1) private int f1_entity38;
	@Tag(2) private long f2_entity38;
	@Tag(3) private int f3_entity38;
	@Tag(4) private String f4_entity38;
	@Tag(5) private int f5_entity38;
	@Tag(6) private Entity33 f6_entity38;
	@Tag(7) private String f7_entity38;
	@Tag(8) private String f8_entity38;
	@Tag(9) private Map<String, Entity26> f9_entity38;

	public Entity38 () {
	}

	void init (Gen g) {
		f0_entity38 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f0_entity38 != null) for (int i = 0, n = g.size(); i < n; i++) f0_entity38.add(g.obj(Entity15.class, () -> Entity15.create(g)));
		f1_entity38 = g.r.nextInt(1000) - 100;
		f2_entity38 = g.r.nextLong() >>> g.r.nextInt(64);
		f3_entity38 = g.r.nextInt(1000) - 100;
		f4_entity38 = g.string();
		f5_entity38 = g.r.nextInt(1000) - 100;
		f6_entity38 = g.r.nextInt(7) == 0 ? null : g.obj(Entity33.class, () -> Entity33.create(g));
		f7_entity38 = g.string();
		f8_entity38 = g.string();
		f9_entity38 = g.r.nextInt(10) == 0 ? null : new HashMap<>(); if (f9_entity38 != null) for (int i = 0, n = g.size(); i < n; i++) f9_entity38.put(g.key(), g.obj(Entity26.class, () -> Entity26.create(g)));
	}

	static Entity38 create (Gen g) {
		Entity38 o = new Entity38();
		o.init(g);
		return o;
	}
}
