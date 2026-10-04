package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity29 {
	@Tag(0) private long f0_entity29;
	@Tag(1) private String f1_entity29;
	@Tag(2) private String f2_entity29;
	@Tag(3) private String f3_entity29;
	@Tag(4) private String f4_entity29;
	@Tag(5) private String f5_entity29;
	@Tag(6) private String f6_entity29;
	@Tag(7) private Kind2 f7_entity29;
	@Tag(8) private int f8_entity29;

	public Entity29 () {
	}

	void init (Gen g) {
		f0_entity29 = g.r.nextLong() >>> g.r.nextInt(64);
		f1_entity29 = g.string();
		f2_entity29 = g.string();
		f3_entity29 = g.string();
		f4_entity29 = g.string();
		f5_entity29 = g.string();
		f6_entity29 = g.string();
		f7_entity29 = g.r.nextInt(8) == 0 ? null : g.pick(Kind2.values());
		f8_entity29 = g.r.nextInt(1000) - 100;
	}

	static Entity29 create (Gen g) {
		Entity29 o = new Entity29();
		o.init(g);
		return o;
	}
}
