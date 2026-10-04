package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity6 extends Base3 {
	@Tag(3) private int f0_entity6;
	@Tag(4) private List<Entity27> f1_entity6;
	@Tag(5) private int f2_entity6;
	@Tag(6) private double[] f3_entity6;
	@Tag(7) private int[] f4_entity6;
	@Tag(8) private int f5_entity6;
	@Tag(9) private String f6_entity6;
	@Tag(10) private long f7_entity6;

	public Entity6 () {
	}

	void init (Gen g) {
		super.init(g);
		f0_entity6 = g.r.nextInt(1000) - 100;
		f1_entity6 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f1_entity6 != null) for (int i = 0, n = g.size(); i < n; i++) f1_entity6.add(g.obj(Entity27.class, () -> Entity27.create(g)));
		f2_entity6 = g.r.nextInt(1000) - 100;
		f3_entity6 = g.doubles();
		f4_entity6 = g.ints();
		f5_entity6 = g.r.nextInt(1000) - 100;
		f6_entity6 = g.string();
		f7_entity6 = g.r.nextLong() >>> g.r.nextInt(64);
	}

	static Entity6 create (Gen g) {
		Entity6 o = new Entity6();
		o.init(g);
		return o;
	}
}
