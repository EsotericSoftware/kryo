package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity15 {
	@Tag(0) private Integer f0_entity15;
	@Tag(1) private Kind2 f1_entity15;
	@Tag(2) private int f2_entity15;
	@Tag(3) private long f3_entity15;
	@Tag(4) private String f4_entity15;
	@Tag(5) private String f5_entity15;
	@Tag(6) private boolean f6_entity15;
	@Tag(7) private Kind0 f7_entity15;
	@Tag(8) private Kind0 f8_entity15;

	public Entity15 () {
	}

	void init (Gen g) {
		f0_entity15 = g.r.nextInt(10) == 0 ? null : g.r.nextInt(100000);
		f1_entity15 = g.r.nextInt(8) == 0 ? null : g.pick(Kind2.values());
		f2_entity15 = g.r.nextInt(1000) - 100;
		f3_entity15 = g.r.nextLong() >>> g.r.nextInt(64);
		f4_entity15 = g.string();
		f5_entity15 = g.string();
		f6_entity15 = g.r.nextBoolean();
		f7_entity15 = g.r.nextInt(8) == 0 ? null : g.pick(Kind0.values());
		f8_entity15 = g.r.nextInt(8) == 0 ? null : g.pick(Kind0.values());
	}

	static Entity15 create (Gen g) {
		Entity15 o = new Entity15();
		o.init(g);
		return o;
	}
}
