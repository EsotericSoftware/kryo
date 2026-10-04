package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity13 {
	@Tag(0) private String f0_entity13;
	@Tag(1) private int f1_entity13;

	public Entity13 () {
	}

	void init (Gen g) {
		f0_entity13 = g.string();
		f1_entity13 = g.r.nextInt(1000) - 100;
	}

	static Entity13 create (Gen g) {
		Entity13 o = new Entity13();
		o.init(g);
		return o;
	}
}
