package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity9 extends Base7 {
	@Tag(4) private Long f0_entity9;
	@Tag(5) private boolean f1_entity9;
	@Tag(6) private String f2_entity9;
	@Tag(7) private int f3_entity9;
	@Tag(8) private String f4_entity9;
	@Tag(9) private Kind3 f5_entity9;
	@Tag(10) private Integer f6_entity9;

	public Entity9 () {
	}

	void init (Gen g) {
		super.init(g);
		f0_entity9 = g.r.nextInt(10) == 0 ? null : g.r.nextLong();
		f1_entity9 = g.r.nextBoolean();
		f2_entity9 = g.string();
		f3_entity9 = g.r.nextInt(1000) - 100;
		f4_entity9 = g.string();
		f5_entity9 = g.r.nextInt(8) == 0 ? null : g.pick(Kind3.values());
		f6_entity9 = g.r.nextInt(10) == 0 ? null : g.r.nextInt(100000);
	}

	static Entity9 create (Gen g) {
		Entity9 o = new Entity9();
		o.init(g);
		return o;
	}
}
