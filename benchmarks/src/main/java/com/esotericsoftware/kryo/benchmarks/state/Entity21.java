package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity21 {
	@Tag(0) private String f0_entity21;
	@Tag(1) private int f1_entity21;
	@Tag(2) private String f2_entity21;
	@Tag(3) private Kind1 f3_entity21;
	@Tag(4) private int f4_entity21;

	public Entity21 () {
	}

	void init (Gen g) {
		f0_entity21 = g.string();
		f1_entity21 = g.r.nextInt(1000) - 100;
		f2_entity21 = g.string();
		f3_entity21 = g.r.nextInt(8) == 0 ? null : g.pick(Kind1.values());
		f4_entity21 = g.r.nextInt(1000) - 100;
	}

	static Entity21 create (Gen g) {
		Entity21 o = new Entity21();
		o.init(g);
		return o;
	}
}
