package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity16 {
	@Tag(0) private String f0_entity16;
	@Tag(1) private int f1_entity16;
	@Tag(2) private Kind1 f2_entity16;
	@Tag(3) private Kind4 f3_entity16;
	@Tag(4) private double f4_entity16;
	@Tag(5) private double f5_entity16;
	@Tag(6) private int[] f6_entity16;
	@Tag(7) private long f7_entity16;
	@Tag(8) private String f8_entity16;
	@Tag(9) private String f9_entity16;

	public Entity16 () {
	}

	void init (Gen g) {
		f0_entity16 = g.string();
		f1_entity16 = g.r.nextInt(1000) - 100;
		f2_entity16 = g.r.nextInt(8) == 0 ? null : g.pick(Kind1.values());
		f3_entity16 = g.r.nextInt(8) == 0 ? null : g.pick(Kind4.values());
		f4_entity16 = g.r.nextDouble() * 1000;
		f5_entity16 = g.r.nextDouble() * 1000;
		f6_entity16 = g.ints();
		f7_entity16 = g.r.nextLong() >>> g.r.nextInt(64);
		f8_entity16 = g.string();
		f9_entity16 = g.string();
	}

	static Entity16 create (Gen g) {
		Entity16 o = new Entity16();
		o.init(g);
		return o;
	}
}
