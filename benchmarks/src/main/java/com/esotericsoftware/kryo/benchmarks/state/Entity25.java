package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity25 {
	@Tag(0) private String f0_entity25;
	@Tag(1) private Long f1_entity25;
	@Tag(2) private int f2_entity25;
	@Tag(3) private int f3_entity25;
	@Tag(4) private String f4_entity25;
	@Tag(5) private String f5_entity25;

	public Entity25 () {
	}

	void init (Gen g) {
		f0_entity25 = g.string();
		f1_entity25 = g.r.nextInt(10) == 0 ? null : g.r.nextLong();
		f2_entity25 = g.r.nextInt(1000) - 100;
		f3_entity25 = g.r.nextInt(1000) - 100;
		f4_entity25 = g.string();
		f5_entity25 = g.string();
	}

	static Entity25 create (Gen g) {
		Entity25 o = new Entity25();
		o.init(g);
		return o;
	}
}
