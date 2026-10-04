package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity26 {
	@Tag(0) private String f0_entity26;
	@Tag(1) private Kind2 f1_entity26;
	@Tag(2) private int f2_entity26;
	@Tag(3) private String f3_entity26;
	@Tag(4) private double f4_entity26;
	@Tag(5) private int f5_entity26;
	@Tag(6) private String f6_entity26;
	@Tag(7) private String f7_entity26;
	@Tag(8) private String f8_entity26;
	@Tag(9) private int[] f9_entity26;
	@Tag(10) private long f10_entity26;

	public Entity26 () {
	}

	void init (Gen g) {
		f0_entity26 = g.string();
		f1_entity26 = g.r.nextInt(8) == 0 ? null : g.pick(Kind2.values());
		f2_entity26 = g.r.nextInt(1000) - 100;
		f3_entity26 = g.string();
		f4_entity26 = g.r.nextDouble() * 1000;
		f5_entity26 = g.r.nextInt(1000) - 100;
		f6_entity26 = g.string();
		f7_entity26 = g.string();
		f8_entity26 = g.string();
		f9_entity26 = g.ints();
		f10_entity26 = g.r.nextLong() >>> g.r.nextInt(64);
	}

	static Entity26 create (Gen g) {
		Entity26 o = new Entity26();
		o.init(g);
		return o;
	}
}
