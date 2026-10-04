package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity31 {
	@Tag(0) private int f0_entity31;
	@Tag(1) private boolean f1_entity31;
	@Tag(2) private Kind3 f2_entity31;
	@Tag(3) private String f3_entity31;
	@Tag(4) private String f4_entity31;
	@Tag(5) private int f5_entity31;
	@Tag(6) private String f6_entity31;
	@Tag(7) private int f7_entity31;
	@Tag(8) private int f8_entity31;
	@Tag(9) private String f9_entity31;
	@Tag(10) private int f10_entity31;
	@Tag(11) private String f11_entity31;
	@Tag(12) private String f12_entity31;
	@Tag(13) private String f13_entity31;
	@Tag(14) private String f14_entity31;
	@Tag(15) private double f15_entity31;

	public Entity31 () {
	}

	void init (Gen g) {
		f0_entity31 = g.r.nextInt(1000) - 100;
		f1_entity31 = g.r.nextBoolean();
		f2_entity31 = g.r.nextInt(8) == 0 ? null : g.pick(Kind3.values());
		f3_entity31 = g.string();
		f4_entity31 = g.string();
		f5_entity31 = g.r.nextInt(1000) - 100;
		f6_entity31 = g.string();
		f7_entity31 = g.r.nextInt(1000) - 100;
		f8_entity31 = g.r.nextInt(1000) - 100;
		f9_entity31 = g.string();
		f10_entity31 = g.r.nextInt(1000) - 100;
		f11_entity31 = g.string();
		f12_entity31 = g.string();
		f13_entity31 = g.string();
		f14_entity31 = g.string();
		f15_entity31 = g.r.nextDouble() * 1000;
	}

	static Entity31 create (Gen g) {
		Entity31 o = new Entity31();
		o.init(g);
		return o;
	}
}
