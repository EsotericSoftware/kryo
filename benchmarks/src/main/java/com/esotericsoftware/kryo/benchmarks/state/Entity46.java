package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity46 {
	@Tag(0) private int f0_entity46;
	@Tag(1) private Entity25 f1_entity46;
	@Tag(2) private String f2_entity46;
	@Tag(3) private double[] f3_entity46;
	@Tag(4) private boolean f4_entity46;

	public Entity46 () {
	}

	void init (Gen g) {
		f0_entity46 = g.r.nextInt(1000) - 100;
		f1_entity46 = g.r.nextInt(7) == 0 ? null : g.obj(Entity25.class, () -> Entity25.create(g));
		f2_entity46 = g.string();
		f3_entity46 = g.doubles();
		f4_entity46 = g.r.nextBoolean();
	}

	static Entity46 create (Gen g) {
		Entity46 o = new Entity46();
		o.init(g);
		return o;
	}
}
