package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity12 {
	@Tag(0) private String f0_entity12;
	@Tag(1) private double[] f1_entity12;
	@Tag(2) private int[] f2_entity12;
	@Tag(3) private Kind1 f3_entity12;
	@Tag(4) private boolean f4_entity12;
	@Tag(5) private Kind1 f5_entity12;
	@Tag(6) private int[] f6_entity12;
	@Tag(7) private String f7_entity12;
	@Tag(8) private double f8_entity12;
	@Tag(9) private double[] f9_entity12;
	@Tag(10) private String f10_entity12;
	@Tag(11) private long f11_entity12;
	@Tag(12) private String f12_entity12;
	@Tag(13) private String f13_entity12;
	@Tag(14) private String f14_entity12;
	@Tag(15) private Kind1 f15_entity12;
	@Tag(16) private boolean f16_entity12;

	public Entity12 () {
	}

	void init (Gen g) {
		f0_entity12 = g.string();
		f1_entity12 = g.doubles();
		f2_entity12 = g.ints();
		f3_entity12 = g.r.nextInt(8) == 0 ? null : g.pick(Kind1.values());
		f4_entity12 = g.r.nextBoolean();
		f5_entity12 = g.r.nextInt(8) == 0 ? null : g.pick(Kind1.values());
		f6_entity12 = g.ints();
		f7_entity12 = g.string();
		f8_entity12 = g.r.nextDouble() * 1000;
		f9_entity12 = g.doubles();
		f10_entity12 = g.string();
		f11_entity12 = g.r.nextLong() >>> g.r.nextInt(64);
		f12_entity12 = g.string();
		f13_entity12 = g.string();
		f14_entity12 = g.string();
		f15_entity12 = g.r.nextInt(8) == 0 ? null : g.pick(Kind1.values());
		f16_entity12 = g.r.nextBoolean();
	}

	static Entity12 create (Gen g) {
		Entity12 o = new Entity12();
		o.init(g);
		return o;
	}
}
