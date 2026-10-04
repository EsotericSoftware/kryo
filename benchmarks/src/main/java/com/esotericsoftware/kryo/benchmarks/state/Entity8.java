package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity8 extends Base7 {
	@Tag(4) private int f0_entity8;
	@Tag(5) private boolean f1_entity8;
	@Tag(6) private boolean f2_entity8;
	@Tag(7) private long f3_entity8;
	@Tag(8) private Integer f4_entity8;

	public Entity8 () {
	}

	void init (Gen g) {
		super.init(g);
		f0_entity8 = g.r.nextInt(1000) - 100;
		f1_entity8 = g.r.nextBoolean();
		f2_entity8 = g.r.nextBoolean();
		f3_entity8 = g.r.nextLong() >>> g.r.nextInt(64);
		f4_entity8 = g.r.nextInt(10) == 0 ? null : g.r.nextInt(100000);
	}

	static Entity8 create (Gen g) {
		Entity8 o = new Entity8();
		o.init(g);
		return o;
	}
}
