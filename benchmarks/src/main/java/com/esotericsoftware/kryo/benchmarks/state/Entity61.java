package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity61 {
	@Tag(0) private long f0_entity61;
	@Tag(1) private boolean f1_entity61;
	@Tag(2) private double f2_entity61;
	@Tag(3) private int[] f3_entity61;
	@Tag(4) private long f4_entity61;
	@Tag(5) private boolean f5_entity61;
	@Tag(6) private Kind4 f6_entity61;
	@Tag(7) private Entity22 f7_entity61;
	@Tag(8) private Entity6 f8_entity61;
	@Tag(9) private String f9_entity61;
	@Tag(10) private Entity36 f10_entity61;
	@Tag(11) private long f11_entity61;
	@Tag(12) private long f12_entity61;

	public Entity61 () {
	}

	void init (Gen g) {
		f0_entity61 = g.r.nextLong() >>> g.r.nextInt(64);
		f1_entity61 = g.r.nextBoolean();
		f2_entity61 = g.r.nextDouble() * 1000;
		f3_entity61 = g.ints();
		f4_entity61 = g.r.nextLong() >>> g.r.nextInt(64);
		f5_entity61 = g.r.nextBoolean();
		f6_entity61 = g.r.nextInt(8) == 0 ? null : g.pick(Kind4.values());
		f7_entity61 = g.r.nextInt(7) == 0 ? null : g.obj(Entity22.class, () -> Entity22.create(g));
		f8_entity61 = g.r.nextInt(7) == 0 ? null : g.obj(Entity6.class, () -> Entity6.create(g));
		f9_entity61 = g.string();
		f10_entity61 = g.r.nextInt(7) == 0 ? null : g.obj(Entity36.class, () -> Entity36.create(g));
		f11_entity61 = g.r.nextLong() >>> g.r.nextInt(64);
		f12_entity61 = g.r.nextLong() >>> g.r.nextInt(64);
	}

	static Entity61 create (Gen g) {
		Entity61 o = new Entity61();
		o.init(g);
		return o;
	}
}
