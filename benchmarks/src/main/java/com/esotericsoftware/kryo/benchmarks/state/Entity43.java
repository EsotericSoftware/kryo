package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity43 {
	@Tag(0) private long f0_entity43;
	@Tag(1) private String f1_entity43;
	@Tag(2) private boolean f2_entity43;
	@Tag(3) private String f3_entity43;
	@Tag(4) private Entity2 f4_entity43;
	@Tag(5) private long f5_entity43;
	@Tag(6) private int f6_entity43;
	@Tag(7) private int f7_entity43;

	public Entity43 () {
	}

	void init (Gen g) {
		f0_entity43 = g.r.nextLong() >>> g.r.nextInt(64);
		f1_entity43 = g.string();
		f2_entity43 = g.r.nextBoolean();
		f3_entity43 = g.string();
		f4_entity43 = g.r.nextInt(7) == 0 ? null : g.obj(Entity2.class, () -> Entity2.create(g));
		f5_entity43 = g.r.nextLong() >>> g.r.nextInt(64);
		f6_entity43 = g.r.nextInt(1000) - 100;
		f7_entity43 = g.r.nextInt(1000) - 100;
	}

	static Entity43 create (Gen g) {
		Entity43 o = new Entity43();
		o.init(g);
		return o;
	}
}
