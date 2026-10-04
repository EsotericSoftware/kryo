package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity24 {
	@Tag(0) private Long f0_entity24;
	@Tag(1) private double f1_entity24;
	@Tag(2) private Kind4 f2_entity24;
	@Tag(3) private boolean f3_entity24;
	@Tag(4) private int f4_entity24;
	@Tag(5) private double f5_entity24;
	@Tag(6) private long f6_entity24;
	@Tag(7) private double f7_entity24;
	@Tag(8) private boolean f8_entity24;
	@Tag(9) private String f9_entity24;
	@Tag(10) private double f10_entity24;
	@Tag(11) private int f11_entity24;
	@Tag(12) private int f12_entity24;
	@Tag(13) private int f13_entity24;
	@Tag(14) private int f14_entity24;
	@Tag(15) private String f15_entity24;
	@Tag(16) private String f16_entity24;
	@Tag(17) private int f17_entity24;
	@Tag(18) private int f18_entity24;

	public Entity24 () {
	}

	void init (Gen g) {
		f0_entity24 = g.r.nextInt(10) == 0 ? null : g.r.nextLong();
		f1_entity24 = g.r.nextDouble() * 1000;
		f2_entity24 = g.r.nextInt(8) == 0 ? null : g.pick(Kind4.values());
		f3_entity24 = g.r.nextBoolean();
		f4_entity24 = g.r.nextInt(1000) - 100;
		f5_entity24 = g.r.nextDouble() * 1000;
		f6_entity24 = g.r.nextLong() >>> g.r.nextInt(64);
		f7_entity24 = g.r.nextDouble() * 1000;
		f8_entity24 = g.r.nextBoolean();
		f9_entity24 = g.string();
		f10_entity24 = g.r.nextDouble() * 1000;
		f11_entity24 = g.r.nextInt(1000) - 100;
		f12_entity24 = g.r.nextInt(1000) - 100;
		f13_entity24 = g.r.nextInt(1000) - 100;
		f14_entity24 = g.r.nextInt(1000) - 100;
		f15_entity24 = g.string();
		f16_entity24 = g.string();
		f17_entity24 = g.r.nextInt(1000) - 100;
		f18_entity24 = g.r.nextInt(1000) - 100;
	}

	static Entity24 create (Gen g) {
		Entity24 o = new Entity24();
		o.init(g);
		return o;
	}
}
