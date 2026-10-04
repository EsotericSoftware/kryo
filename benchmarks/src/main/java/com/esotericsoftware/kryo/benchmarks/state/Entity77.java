package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity77 {
	@Tag(0) private String f0_entity77;
	@Tag(1) private int f1_entity77;
	@Tag(2) private Base0 f2_entity77;
	@Tag(3) private Base0 f3_entity77;
	@Tag(4) private List<Entity12> f4_entity77;
	@Tag(5) private double f5_entity77;
	@Tag(6) private int f6_entity77;
	@Tag(7) private int f7_entity77;
	@Tag(8) private String f8_entity77;
	@Tag(9) private String f9_entity77;
	@Tag(10) private boolean f10_entity77;
	@Tag(11) private long f11_entity77;
	@Tag(12) private Entity53 f12_entity77;
	@Tag(13) private int[] f13_entity77;
	@Tag(14) private Map<String, Entity48> f14_entity77;
	@Tag(15) private Entity39 f15_entity77;
	@Tag(16) private Kind5 f16_entity77;

	public Entity77 () {
	}

	void init (Gen g) {
		f0_entity77 = g.string();
		f1_entity77 = g.r.nextInt(1000) - 100;
		f2_entity77 = g.r.nextInt(7) == 0 ? null : Base0.createAny(g);
		f3_entity77 = g.r.nextInt(7) == 0 ? null : Base0.createAny(g);
		f4_entity77 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f4_entity77 != null) for (int i = 0, n = g.size(); i < n; i++) f4_entity77.add(g.obj(Entity12.class, () -> Entity12.create(g)));
		f5_entity77 = g.r.nextDouble() * 1000;
		f6_entity77 = g.r.nextInt(1000) - 100;
		f7_entity77 = g.r.nextInt(1000) - 100;
		f8_entity77 = g.string();
		f9_entity77 = g.string();
		f10_entity77 = g.r.nextBoolean();
		f11_entity77 = g.r.nextLong() >>> g.r.nextInt(64);
		f12_entity77 = g.r.nextInt(7) == 0 ? null : g.obj(Entity53.class, () -> Entity53.create(g));
		f13_entity77 = g.ints();
		f14_entity77 = g.r.nextInt(10) == 0 ? null : new HashMap<>(); if (f14_entity77 != null) for (int i = 0, n = g.size(); i < n; i++) f14_entity77.put(g.key(), g.obj(Entity48.class, () -> Entity48.create(g)));
		f15_entity77 = g.r.nextInt(7) == 0 ? null : g.obj(Entity39.class, () -> Entity39.create(g));
		f16_entity77 = g.r.nextInt(8) == 0 ? null : g.pick(Kind5.values());
	}

	static Entity77 create (Gen g) {
		Entity77 o = new Entity77();
		o.init(g);
		return o;
	}
}
