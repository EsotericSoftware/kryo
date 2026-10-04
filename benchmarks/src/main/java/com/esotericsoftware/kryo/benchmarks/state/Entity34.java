package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity34 {
	@Tag(0) private int f0_entity34;
	@Tag(1) private String f1_entity34;
	@Tag(2) private Map<String, Entity11> f2_entity34;
	@Tag(3) private int f3_entity34;
	@Tag(4) private int f4_entity34;
	@Tag(5) private int f5_entity34;
	@Tag(6) private String f6_entity34;
	@Tag(7) private List<Entity28> f7_entity34;
	@Tag(8) private String f8_entity34;
	@Tag(9) private boolean f9_entity34;
	@Tag(10) private boolean f10_entity34;
	@Tag(11) private List<Entity12> f11_entity34;
	@Tag(12) private boolean f12_entity34;
	@Tag(13) private Kind2 f13_entity34;
	@Tag(14) private Kind2 f14_entity34;
	@Tag(15) private boolean f15_entity34;
	@Tag(16) private long f16_entity34;

	public Entity34 () {
	}

	void init (Gen g) {
		f0_entity34 = g.r.nextInt(1000) - 100;
		f1_entity34 = g.string();
		f2_entity34 = g.r.nextInt(10) == 0 ? null : new HashMap<>(); if (f2_entity34 != null) for (int i = 0, n = g.size(); i < n; i++) f2_entity34.put(g.key(), g.obj(Entity11.class, () -> Entity11.create(g)));
		f3_entity34 = g.r.nextInt(1000) - 100;
		f4_entity34 = g.r.nextInt(1000) - 100;
		f5_entity34 = g.r.nextInt(1000) - 100;
		f6_entity34 = g.string();
		f7_entity34 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f7_entity34 != null) for (int i = 0, n = g.size(); i < n; i++) f7_entity34.add(g.obj(Entity28.class, () -> Entity28.create(g)));
		f8_entity34 = g.string();
		f9_entity34 = g.r.nextBoolean();
		f10_entity34 = g.r.nextBoolean();
		f11_entity34 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f11_entity34 != null) for (int i = 0, n = g.size(); i < n; i++) f11_entity34.add(g.obj(Entity12.class, () -> Entity12.create(g)));
		f12_entity34 = g.r.nextBoolean();
		f13_entity34 = g.r.nextInt(8) == 0 ? null : g.pick(Kind2.values());
		f14_entity34 = g.r.nextInt(8) == 0 ? null : g.pick(Kind2.values());
		f15_entity34 = g.r.nextBoolean();
		f16_entity34 = g.r.nextLong() >>> g.r.nextInt(64);
	}

	static Entity34 create (Gen g) {
		Entity34 o = new Entity34();
		o.init(g);
		return o;
	}
}
