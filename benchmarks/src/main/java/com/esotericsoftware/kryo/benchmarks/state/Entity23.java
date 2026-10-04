package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity23 {
	@Tag(0) private double f0_entity23;
	@Tag(1) private Long f1_entity23;
	@Tag(2) private int f2_entity23;
	@Tag(3) private Kind3 f3_entity23;
	@Tag(4) private boolean f4_entity23;
	@Tag(5) private String f5_entity23;
	@Tag(6) private String f6_entity23;
	@Tag(7) private Kind4 f7_entity23;
	@Tag(8) private Kind3 f8_entity23;
	@Tag(9) private String f9_entity23;
	@Tag(10) private long f10_entity23;
	@Tag(11) private String f11_entity23;
	@Tag(12) private boolean f12_entity23;
	@Tag(13) private long f13_entity23;
	@Tag(14) private long f14_entity23;
	@Tag(15) private long f15_entity23;
	@Tag(16) private int f16_entity23;
	@Tag(17) private String f17_entity23;
	@Tag(18) private int f18_entity23;
	@Tag(19) private boolean f19_entity23;
	@Tag(20) private int f20_entity23;
	@Tag(21) private Kind4 f21_entity23;
	@Tag(22) private int f22_entity23;

	public Entity23 () {
	}

	void init (Gen g) {
		f0_entity23 = g.r.nextDouble() * 1000;
		f1_entity23 = g.r.nextInt(10) == 0 ? null : g.r.nextLong();
		f2_entity23 = g.r.nextInt(1000) - 100;
		f3_entity23 = g.r.nextInt(8) == 0 ? null : g.pick(Kind3.values());
		f4_entity23 = g.r.nextBoolean();
		f5_entity23 = g.string();
		f6_entity23 = g.string();
		f7_entity23 = g.r.nextInt(8) == 0 ? null : g.pick(Kind4.values());
		f8_entity23 = g.r.nextInt(8) == 0 ? null : g.pick(Kind3.values());
		f9_entity23 = g.string();
		f10_entity23 = g.r.nextLong() >>> g.r.nextInt(64);
		f11_entity23 = g.string();
		f12_entity23 = g.r.nextBoolean();
		f13_entity23 = g.r.nextLong() >>> g.r.nextInt(64);
		f14_entity23 = g.r.nextLong() >>> g.r.nextInt(64);
		f15_entity23 = g.r.nextLong() >>> g.r.nextInt(64);
		f16_entity23 = g.r.nextInt(1000) - 100;
		f17_entity23 = g.string();
		f18_entity23 = g.r.nextInt(1000) - 100;
		f19_entity23 = g.r.nextBoolean();
		f20_entity23 = g.r.nextInt(1000) - 100;
		f21_entity23 = g.r.nextInt(8) == 0 ? null : g.pick(Kind4.values());
		f22_entity23 = g.r.nextInt(1000) - 100;
	}

	static Entity23 create (Gen g) {
		Entity23 o = new Entity23();
		o.init(g);
		return o;
	}
}
