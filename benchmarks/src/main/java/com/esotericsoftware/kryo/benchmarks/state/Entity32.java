package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity32 {
	@Tag(0) private Kind0 f0_entity32;
	@Tag(1) private String f1_entity32;
	@Tag(2) private Kind2 f2_entity32;
	@Tag(3) private boolean f3_entity32;
	@Tag(4) private double f4_entity32;
	@Tag(5) private boolean f5_entity32;
	@Tag(6) private String f6_entity32;
	@Tag(7) private String f7_entity32;
	@Tag(8) private int f8_entity32;
	@Tag(9) private String f9_entity32;

	public Entity32 () {
	}

	void init (Gen g) {
		f0_entity32 = g.r.nextInt(8) == 0 ? null : g.pick(Kind0.values());
		f1_entity32 = g.string();
		f2_entity32 = g.r.nextInt(8) == 0 ? null : g.pick(Kind2.values());
		f3_entity32 = g.r.nextBoolean();
		f4_entity32 = g.r.nextDouble() * 1000;
		f5_entity32 = g.r.nextBoolean();
		f6_entity32 = g.string();
		f7_entity32 = g.string();
		f8_entity32 = g.r.nextInt(1000) - 100;
		f9_entity32 = g.string();
	}

	static Entity32 create (Gen g) {
		Entity32 o = new Entity32();
		o.init(g);
		return o;
	}
}
