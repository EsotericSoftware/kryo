package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity41 {
	@Tag(0) private String f0_entity41;
	@Tag(1) private String f1_entity41;
	@Tag(2) private String f2_entity41;
	@Tag(3) private String f3_entity41;
	@Tag(4) private String f4_entity41;
	@Tag(5) private double f5_entity41;
	@Tag(6) private String f6_entity41;
	@Tag(7) private Entity15 f7_entity41;
	@Tag(8) private double f8_entity41;
	@Tag(9) private List<Entity31> f9_entity41;
	@Tag(10) private int[] f10_entity41;
	@Tag(11) private Integer f11_entity41;
	@Tag(12) private long f12_entity41;
	@Tag(13) private boolean f13_entity41;
	@Tag(14) private String f14_entity41;
	@Tag(15) private int f15_entity41;
	@Tag(16) private int f16_entity41;

	public Entity41 () {
	}

	void init (Gen g) {
		f0_entity41 = g.string();
		f1_entity41 = g.string();
		f2_entity41 = g.string();
		f3_entity41 = g.string();
		f4_entity41 = g.string();
		f5_entity41 = g.r.nextDouble() * 1000;
		f6_entity41 = g.string();
		f7_entity41 = g.r.nextInt(7) == 0 ? null : g.obj(Entity15.class, () -> Entity15.create(g));
		f8_entity41 = g.r.nextDouble() * 1000;
		f9_entity41 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f9_entity41 != null) for (int i = 0, n = g.size(); i < n; i++) f9_entity41.add(g.obj(Entity31.class, () -> Entity31.create(g)));
		f10_entity41 = g.ints();
		f11_entity41 = g.r.nextInt(10) == 0 ? null : g.r.nextInt(100000);
		f12_entity41 = g.r.nextLong() >>> g.r.nextInt(64);
		f13_entity41 = g.r.nextBoolean();
		f14_entity41 = g.string();
		f15_entity41 = g.r.nextInt(1000) - 100;
		f16_entity41 = g.r.nextInt(1000) - 100;
	}

	static Entity41 create (Gen g) {
		Entity41 o = new Entity41();
		o.init(g);
		return o;
	}
}
