package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity4 extends Base3 {
	@Tag(3) private String f0_entity4;
	@Tag(4) private int f1_entity4;
	@Tag(5) private boolean f2_entity4;
	@Tag(6) private Base7 f3_entity4;
	@Tag(7) private double[] f4_entity4;

	public Entity4 () {
	}

	void init (Gen g) {
		super.init(g);
		f0_entity4 = g.string();
		f1_entity4 = g.r.nextInt(1000) - 100;
		f2_entity4 = g.r.nextBoolean();
		f3_entity4 = g.r.nextInt(7) == 0 ? null : Base7.createAny(g);
		f4_entity4 = g.doubles();
	}

	static Entity4 create (Gen g) {
		Entity4 o = new Entity4();
		o.init(g);
		return o;
	}
}
