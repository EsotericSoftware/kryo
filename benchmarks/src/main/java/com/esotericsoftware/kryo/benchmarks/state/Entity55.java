package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity55 {
	@Tag(0) private String f0_entity55;
	@Tag(1) private Kind5 f1_entity55;
	@Tag(2) private String f2_entity55;
	@Tag(3) private String f3_entity55;
	@Tag(4) private List<Entity10> f4_entity55;
	@Tag(5) private String f5_entity55;
	@Tag(6) private String f6_entity55;
	@Tag(7) private int f7_entity55;
	@Tag(8) private int f8_entity55;
	@Tag(9) private long f9_entity55;
	@Tag(10) private int[] f10_entity55;

	public Entity55 () {
	}

	void init (Gen g) {
		f0_entity55 = g.string();
		f1_entity55 = g.r.nextInt(8) == 0 ? null : g.pick(Kind5.values());
		f2_entity55 = g.string();
		f3_entity55 = g.string();
		f4_entity55 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f4_entity55 != null) for (int i = 0, n = g.size(); i < n; i++) f4_entity55.add(g.obj(Entity10.class, () -> Entity10.create(g)));
		f5_entity55 = g.string();
		f6_entity55 = g.string();
		f7_entity55 = g.r.nextInt(1000) - 100;
		f8_entity55 = g.r.nextInt(1000) - 100;
		f9_entity55 = g.r.nextLong() >>> g.r.nextInt(64);
		f10_entity55 = g.ints();
	}

	static Entity55 create (Gen g) {
		Entity55 o = new Entity55();
		o.init(g);
		return o;
	}
}
