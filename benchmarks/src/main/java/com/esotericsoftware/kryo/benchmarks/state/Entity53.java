package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity53 {
	@Tag(0) private Entity21 f0_entity53;
	@Tag(1) private int f1_entity53;
	@Tag(2) private String f2_entity53;
	@Tag(3) private Kind2 f3_entity53;
	@Tag(4) private Entity33 f4_entity53;
	@Tag(5) private double f5_entity53;
	@Tag(6) private int f6_entity53;
	@Tag(7) private double f7_entity53;
	@Tag(8) private String f8_entity53;
	@Tag(9) private double f9_entity53;
	@Tag(10) private int f10_entity53;
	@Tag(11) private boolean f11_entity53;

	public Entity53 () {
	}

	void init (Gen g) {
		f0_entity53 = g.r.nextInt(7) == 0 ? null : g.obj(Entity21.class, () -> Entity21.create(g));
		f1_entity53 = g.r.nextInt(1000) - 100;
		f2_entity53 = g.string();
		f3_entity53 = g.r.nextInt(8) == 0 ? null : g.pick(Kind2.values());
		f4_entity53 = g.r.nextInt(7) == 0 ? null : g.obj(Entity33.class, () -> Entity33.create(g));
		f5_entity53 = g.r.nextDouble() * 1000;
		f6_entity53 = g.r.nextInt(1000) - 100;
		f7_entity53 = g.r.nextDouble() * 1000;
		f8_entity53 = g.string();
		f9_entity53 = g.r.nextDouble() * 1000;
		f10_entity53 = g.r.nextInt(1000) - 100;
		f11_entity53 = g.r.nextBoolean();
	}

	static Entity53 create (Gen g) {
		Entity53 o = new Entity53();
		o.init(g);
		return o;
	}
}
