package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity17 {
	@Tag(0) private int f0_entity17;
	@Tag(1) private int f1_entity17;
	@Tag(2) private String f2_entity17;
	@Tag(3) private String f3_entity17;
	@Tag(4) private int f4_entity17;
	@Tag(5) private String f5_entity17;

	public Entity17 () {
	}

	void init (Gen g) {
		f0_entity17 = g.r.nextInt(1000) - 100;
		f1_entity17 = g.r.nextInt(1000) - 100;
		f2_entity17 = g.string();
		f3_entity17 = g.string();
		f4_entity17 = g.r.nextInt(1000) - 100;
		f5_entity17 = g.string();
	}

	static Entity17 create (Gen g) {
		Entity17 o = new Entity17();
		o.init(g);
		return o;
	}
}
