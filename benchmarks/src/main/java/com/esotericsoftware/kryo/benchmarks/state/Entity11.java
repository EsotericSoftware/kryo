package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity11 extends Base7 {
	@Tag(4) private int f0_entity11;
	@Tag(5) private double f1_entity11;
	@Tag(6) private int f2_entity11;

	public Entity11 () {
	}

	void init (Gen g) {
		super.init(g);
		f0_entity11 = g.r.nextInt(1000) - 100;
		f1_entity11 = g.r.nextDouble() * 1000;
		f2_entity11 = g.r.nextInt(1000) - 100;
	}

	static Entity11 create (Gen g) {
		Entity11 o = new Entity11();
		o.init(g);
		return o;
	}
}
