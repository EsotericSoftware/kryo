package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity19 {
	@Tag(0) private double f0_entity19;
	@Tag(1) private long f1_entity19;
	@Tag(2) private boolean f2_entity19;
	@Tag(3) private String f3_entity19;
	@Tag(4) private String f4_entity19;
	@Tag(5) private Integer f5_entity19;
	@Tag(6) private String f6_entity19;
	@Tag(7) private String f7_entity19;
	@Tag(8) private int f8_entity19;
	@Tag(9) private long f9_entity19;
	@Tag(10) private Kind5 f10_entity19;
	@Tag(11) private boolean f11_entity19;
	@Tag(12) private long f12_entity19;
	@Tag(13) private String f13_entity19;
	@Tag(14) private String f14_entity19;

	public Entity19 () {
	}

	void init (Gen g) {
		f0_entity19 = g.r.nextDouble() * 1000;
		f1_entity19 = g.r.nextLong() >>> g.r.nextInt(64);
		f2_entity19 = g.r.nextBoolean();
		f3_entity19 = g.string();
		f4_entity19 = g.string();
		f5_entity19 = g.r.nextInt(10) == 0 ? null : g.r.nextInt(100000);
		f6_entity19 = g.string();
		f7_entity19 = g.string();
		f8_entity19 = g.r.nextInt(1000) - 100;
		f9_entity19 = g.r.nextLong() >>> g.r.nextInt(64);
		f10_entity19 = g.r.nextInt(8) == 0 ? null : g.pick(Kind5.values());
		f11_entity19 = g.r.nextBoolean();
		f12_entity19 = g.r.nextLong() >>> g.r.nextInt(64);
		f13_entity19 = g.string();
		f14_entity19 = g.string();
	}

	static Entity19 create (Gen g) {
		Entity19 o = new Entity19();
		o.init(g);
		return o;
	}
}
