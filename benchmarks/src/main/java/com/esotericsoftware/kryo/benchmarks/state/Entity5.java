package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity5 extends Base3 {
	@Tag(3) private Kind0 f0_entity5;
	@Tag(4) private Map<String, Entity2> f1_entity5;
	@Tag(5) private double f2_entity5;
	@Tag(6) private Entity24 f3_entity5;
	@Tag(7) private long f4_entity5;
	@Tag(8) private double f5_entity5;
	@Tag(9) private boolean f6_entity5;
	@Tag(10) private Base0 f7_entity5;
	@Tag(11) private boolean f8_entity5;
	@Tag(12) private double f9_entity5;
	@Tag(13) private double f10_entity5;

	public Entity5 () {
	}

	void init (Gen g) {
		super.init(g);
		f0_entity5 = g.r.nextInt(8) == 0 ? null : g.pick(Kind0.values());
		f1_entity5 = g.r.nextInt(10) == 0 ? null : new HashMap<>(); if (f1_entity5 != null) for (int i = 0, n = g.size(); i < n; i++) f1_entity5.put(g.key(), g.obj(Entity2.class, () -> Entity2.create(g)));
		f2_entity5 = g.r.nextDouble() * 1000;
		f3_entity5 = g.r.nextInt(7) == 0 ? null : g.obj(Entity24.class, () -> Entity24.create(g));
		f4_entity5 = g.r.nextLong() >>> g.r.nextInt(64);
		f5_entity5 = g.r.nextDouble() * 1000;
		f6_entity5 = g.r.nextBoolean();
		f7_entity5 = g.r.nextInt(7) == 0 ? null : Base0.createAny(g);
		f8_entity5 = g.r.nextBoolean();
		f9_entity5 = g.r.nextDouble() * 1000;
		f10_entity5 = g.r.nextDouble() * 1000;
	}

	static Entity5 create (Gen g) {
		Entity5 o = new Entity5();
		o.init(g);
		return o;
	}
}
