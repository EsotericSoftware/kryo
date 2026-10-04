package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity33 {
	@Tag(0) private String f0_entity33;
	@Tag(1) private String f1_entity33;
	@Tag(2) private int f2_entity33;
	@Tag(3) private boolean f3_entity33;
	@Tag(4) private double[] f4_entity33;
	@Tag(5) private Kind0 f5_entity33;
	@Tag(6) private double f6_entity33;
	@Tag(7) private int f7_entity33;
	@Tag(8) private double f8_entity33;
	@Tag(9) private String f9_entity33;
	@Tag(10) private String f10_entity33;
	@Tag(11) private int f11_entity33;
	@Tag(12) private int f12_entity33;
	@Tag(13) private Kind0 f13_entity33;
	@Tag(14) private boolean f14_entity33;
	@Tag(15) private int f15_entity33;
	@Tag(16) private String f16_entity33;
	@Tag(17) private String f17_entity33;
	@Tag(18) private String f18_entity33;

	public Entity33 () {
	}

	void init (Gen g) {
		f0_entity33 = g.string();
		f1_entity33 = g.string();
		f2_entity33 = g.r.nextInt(1000) - 100;
		f3_entity33 = g.r.nextBoolean();
		f4_entity33 = g.doubles();
		f5_entity33 = g.r.nextInt(8) == 0 ? null : g.pick(Kind0.values());
		f6_entity33 = g.r.nextDouble() * 1000;
		f7_entity33 = g.r.nextInt(1000) - 100;
		f8_entity33 = g.r.nextDouble() * 1000;
		f9_entity33 = g.string();
		f10_entity33 = g.string();
		f11_entity33 = g.r.nextInt(1000) - 100;
		f12_entity33 = g.r.nextInt(1000) - 100;
		f13_entity33 = g.r.nextInt(8) == 0 ? null : g.pick(Kind0.values());
		f14_entity33 = g.r.nextBoolean();
		f15_entity33 = g.r.nextInt(1000) - 100;
		f16_entity33 = g.string();
		f17_entity33 = g.string();
		f18_entity33 = g.string();
	}

	static Entity33 create (Gen g) {
		Entity33 o = new Entity33();
		o.init(g);
		return o;
	}
}
