package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity10 extends Base7 {
	@Tag(4) private double f0_entity10;
	@Tag(5) private double f1_entity10;
	@Tag(6) private boolean f2_entity10;
	@Tag(7) private int f3_entity10;
	@Tag(8) private String f4_entity10;
	@Tag(9) private int f5_entity10;
	@Tag(10) private int f6_entity10;
	@Tag(11) private int f7_entity10;
	@Tag(12) private boolean f8_entity10;
	@Tag(13) private boolean f9_entity10;
	@Tag(14) private String f10_entity10;
	@Tag(15) private String f11_entity10;
	@Tag(16) private boolean f12_entity10;
	@Tag(17) private int f13_entity10;
	@Tag(18) private long f14_entity10;
	@Tag(19) private Kind0 f15_entity10;
	@Tag(20) private int[] f16_entity10;
	@Tag(21) private double f17_entity10;

	public Entity10 () {
	}

	void init (Gen g) {
		super.init(g);
		f0_entity10 = g.r.nextDouble() * 1000;
		f1_entity10 = g.r.nextDouble() * 1000;
		f2_entity10 = g.r.nextBoolean();
		f3_entity10 = g.r.nextInt(1000) - 100;
		f4_entity10 = g.string();
		f5_entity10 = g.r.nextInt(1000) - 100;
		f6_entity10 = g.r.nextInt(1000) - 100;
		f7_entity10 = g.r.nextInt(1000) - 100;
		f8_entity10 = g.r.nextBoolean();
		f9_entity10 = g.r.nextBoolean();
		f10_entity10 = g.string();
		f11_entity10 = g.string();
		f12_entity10 = g.r.nextBoolean();
		f13_entity10 = g.r.nextInt(1000) - 100;
		f14_entity10 = g.r.nextLong() >>> g.r.nextInt(64);
		f15_entity10 = g.r.nextInt(8) == 0 ? null : g.pick(Kind0.values());
		f16_entity10 = g.ints();
		f17_entity10 = g.r.nextDouble() * 1000;
	}

	static Entity10 create (Gen g) {
		Entity10 o = new Entity10();
		o.init(g);
		return o;
	}
}
