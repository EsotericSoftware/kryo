package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity2 extends Base0 {
	@Tag(3) private Kind4 f0_entity2;
	@Tag(4) private int f1_entity2;
	@Tag(5) private int f2_entity2;
	@Tag(6) private int f3_entity2;
	@Tag(7) private String f4_entity2;
	@Tag(8) private double f5_entity2;
	@Tag(9) private int f6_entity2;

	public Entity2 () {
	}

	void init (Gen g) {
		super.init(g);
		f0_entity2 = g.r.nextInt(8) == 0 ? null : g.pick(Kind4.values());
		f1_entity2 = g.r.nextInt(1000) - 100;
		f2_entity2 = g.r.nextInt(1000) - 100;
		f3_entity2 = g.r.nextInt(1000) - 100;
		f4_entity2 = g.string();
		f5_entity2 = g.r.nextDouble() * 1000;
		f6_entity2 = g.r.nextInt(1000) - 100;
	}

	static Entity2 create (Gen g) {
		Entity2 o = new Entity2();
		o.init(g);
		return o;
	}
}
