package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity70 {
	@Tag(0) private Entity6 f0_entity70;
	@Tag(1) private double f1_entity70;
	@Tag(2) private String f2_entity70;
	@Tag(3) private String f3_entity70;
	@Tag(4) private double f4_entity70;
	@Tag(5) private String f5_entity70;
	@Tag(6) private double f6_entity70;
	@Tag(7) private int f7_entity70;
	@Tag(8) private Entity29 f8_entity70;
	@Tag(9) private double f9_entity70;
	@Tag(10) private long f10_entity70;

	public Entity70 () {
	}

	void init (Gen g) {
		f0_entity70 = g.r.nextInt(7) == 0 ? null : g.obj(Entity6.class, () -> Entity6.create(g));
		f1_entity70 = g.r.nextDouble() * 1000;
		f2_entity70 = g.string();
		f3_entity70 = g.string();
		f4_entity70 = g.r.nextDouble() * 1000;
		f5_entity70 = g.string();
		f6_entity70 = g.r.nextDouble() * 1000;
		f7_entity70 = g.r.nextInt(1000) - 100;
		f8_entity70 = g.r.nextInt(7) == 0 ? null : g.obj(Entity29.class, () -> Entity29.create(g));
		f9_entity70 = g.r.nextDouble() * 1000;
		f10_entity70 = g.r.nextLong() >>> g.r.nextInt(64);
	}

	static Entity70 create (Gen g) {
		Entity70 o = new Entity70();
		o.init(g);
		return o;
	}
}
