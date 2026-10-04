package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity20 {
	@Tag(0) private String f0_entity20;
	@Tag(1) private Kind2 f1_entity20;
	@Tag(2) private double f2_entity20;
	@Tag(3) private String f3_entity20;
	@Tag(4) private boolean f4_entity20;

	public Entity20 () {
	}

	void init (Gen g) {
		f0_entity20 = g.string();
		f1_entity20 = g.r.nextInt(8) == 0 ? null : g.pick(Kind2.values());
		f2_entity20 = g.r.nextDouble() * 1000;
		f3_entity20 = g.string();
		f4_entity20 = g.r.nextBoolean();
	}

	static Entity20 create (Gen g) {
		Entity20 o = new Entity20();
		o.init(g);
		return o;
	}
}
