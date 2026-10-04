package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity1 extends Base0 {
	@Tag(3) private String f0_entity1;
	@Tag(4) private String f1_entity1;
	@Tag(5) private boolean f2_entity1;
	@Tag(6) private Kind1 f3_entity1;
	@Tag(7) private long f4_entity1;

	public Entity1 () {
	}

	void init (Gen g) {
		super.init(g);
		f0_entity1 = g.string();
		f1_entity1 = g.string();
		f2_entity1 = g.r.nextBoolean();
		f3_entity1 = g.r.nextInt(8) == 0 ? null : g.pick(Kind1.values());
		f4_entity1 = g.r.nextLong() >>> g.r.nextInt(64);
	}

	static Entity1 create (Gen g) {
		Entity1 o = new Entity1();
		o.init(g);
		return o;
	}
}
