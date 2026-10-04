package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity36 {
	@Tag(0) private double f0_entity36;
	@Tag(1) private boolean f1_entity36;
	@Tag(2) private double f2_entity36;
	@Tag(3) private String f3_entity36;
	@Tag(4) private int f4_entity36;
	@Tag(5) private int f5_entity36;

	public Entity36 () {
	}

	void init (Gen g) {
		f0_entity36 = g.r.nextDouble() * 1000;
		f1_entity36 = g.r.nextBoolean();
		f2_entity36 = g.r.nextDouble() * 1000;
		f3_entity36 = g.string();
		f4_entity36 = g.r.nextInt(1000) - 100;
		f5_entity36 = g.r.nextInt(1000) - 100;
	}

	static Entity36 create (Gen g) {
		Entity36 o = new Entity36();
		o.init(g);
		return o;
	}
}
