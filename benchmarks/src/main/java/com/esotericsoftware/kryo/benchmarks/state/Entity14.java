package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity14 {
	@Tag(0) private long f0_entity14;
	@Tag(1) private long f1_entity14;
	@Tag(2) private int f2_entity14;
	@Tag(3) private boolean f3_entity14;
	@Tag(4) private Integer f4_entity14;
	@Tag(5) private String f5_entity14;
	@Tag(6) private int f6_entity14;
	@Tag(7) private boolean f7_entity14;
	@Tag(8) private long f8_entity14;
	@Tag(9) private int f9_entity14;
	@Tag(10) private double f10_entity14;
	@Tag(11) private long f11_entity14;

	public Entity14 () {
	}

	void init (Gen g) {
		f0_entity14 = g.r.nextLong() >>> g.r.nextInt(64);
		f1_entity14 = g.r.nextLong() >>> g.r.nextInt(64);
		f2_entity14 = g.r.nextInt(1000) - 100;
		f3_entity14 = g.r.nextBoolean();
		f4_entity14 = g.r.nextInt(10) == 0 ? null : g.r.nextInt(100000);
		f5_entity14 = g.string();
		f6_entity14 = g.r.nextInt(1000) - 100;
		f7_entity14 = g.r.nextBoolean();
		f8_entity14 = g.r.nextLong() >>> g.r.nextInt(64);
		f9_entity14 = g.r.nextInt(1000) - 100;
		f10_entity14 = g.r.nextDouble() * 1000;
		f11_entity14 = g.r.nextLong() >>> g.r.nextInt(64);
	}

	static Entity14 create (Gen g) {
		Entity14 o = new Entity14();
		o.init(g);
		return o;
	}
}
