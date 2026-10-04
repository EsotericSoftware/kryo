package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity28 {
	@Tag(0) private int f0_entity28;
	@Tag(1) private long f1_entity28;
	@Tag(2) private String f2_entity28;
	@Tag(3) private String f3_entity28;
	@Tag(4) private boolean f4_entity28;
	@Tag(5) private boolean f5_entity28;
	@Tag(6) private long f6_entity28;
	@Tag(7) private String f7_entity28;
	@Tag(8) private String f8_entity28;
	@Tag(9) private int f9_entity28;
	@Tag(10) private double f10_entity28;
	@Tag(11) private int f11_entity28;
	@Tag(12) private String f12_entity28;
	@Tag(13) private int f13_entity28;
	@Tag(14) private double f14_entity28;
	@Tag(15) private String f15_entity28;
	@Tag(16) private String f16_entity28;

	public Entity28 () {
	}

	void init (Gen g) {
		f0_entity28 = g.r.nextInt(1000) - 100;
		f1_entity28 = g.r.nextLong() >>> g.r.nextInt(64);
		f2_entity28 = g.string();
		f3_entity28 = g.string();
		f4_entity28 = g.r.nextBoolean();
		f5_entity28 = g.r.nextBoolean();
		f6_entity28 = g.r.nextLong() >>> g.r.nextInt(64);
		f7_entity28 = g.string();
		f8_entity28 = g.string();
		f9_entity28 = g.r.nextInt(1000) - 100;
		f10_entity28 = g.r.nextDouble() * 1000;
		f11_entity28 = g.r.nextInt(1000) - 100;
		f12_entity28 = g.string();
		f13_entity28 = g.r.nextInt(1000) - 100;
		f14_entity28 = g.r.nextDouble() * 1000;
		f15_entity28 = g.string();
		f16_entity28 = g.string();
	}

	static Entity28 create (Gen g) {
		Entity28 o = new Entity28();
		o.init(g);
		return o;
	}
}
