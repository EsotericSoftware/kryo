package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity40 {
	@Tag(0) private String f0_entity40;
	@Tag(1) private Entity32 f1_entity40;
	@Tag(2) private long f2_entity40;
	@Tag(3) private String f3_entity40;
	@Tag(4) private int f4_entity40;
	@Tag(5) private boolean f5_entity40;
	@Tag(6) private long f6_entity40;

	public Entity40 () {
	}

	void init (Gen g) {
		f0_entity40 = g.string();
		f1_entity40 = g.r.nextInt(7) == 0 ? null : g.obj(Entity32.class, () -> Entity32.create(g));
		f2_entity40 = g.r.nextLong() >>> g.r.nextInt(64);
		f3_entity40 = g.string();
		f4_entity40 = g.r.nextInt(1000) - 100;
		f5_entity40 = g.r.nextBoolean();
		f6_entity40 = g.r.nextLong() >>> g.r.nextInt(64);
	}

	static Entity40 create (Gen g) {
		Entity40 o = new Entity40();
		o.init(g);
		return o;
	}
}
