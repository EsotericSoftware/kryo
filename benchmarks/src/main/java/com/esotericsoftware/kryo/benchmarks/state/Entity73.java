package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity73 {
	@Tag(0) private boolean f0_entity73;
	@Tag(1) private double f1_entity73;
	@Tag(2) private int f2_entity73;
	@Tag(3) private int f3_entity73;

	public Entity73 () {
	}

	void init (Gen g) {
		f0_entity73 = g.r.nextBoolean();
		f1_entity73 = g.r.nextDouble() * 1000;
		f2_entity73 = g.r.nextInt(1000) - 100;
		f3_entity73 = g.r.nextInt(1000) - 100;
	}

	static Entity73 create (Gen g) {
		Entity73 o = new Entity73();
		o.init(g);
		return o;
	}
}
