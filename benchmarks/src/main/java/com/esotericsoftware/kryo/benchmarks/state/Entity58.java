package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity58 {
	@Tag(0) private long f0_entity58;
	@Tag(1) private String f1_entity58;
	@Tag(2) private int f2_entity58;
	@Tag(3) private Long f3_entity58;
	@Tag(4) private int f4_entity58;
	@Tag(5) private Entity45 f5_entity58;
	@Tag(6) private List<Entity28> f6_entity58;
	@Tag(7) private String f7_entity58;
	@Tag(8) private int f8_entity58;
	@Tag(9) private int f9_entity58;

	public Entity58 () {
	}

	void init (Gen g) {
		f0_entity58 = g.r.nextLong() >>> g.r.nextInt(64);
		f1_entity58 = g.string();
		f2_entity58 = g.r.nextInt(1000) - 100;
		f3_entity58 = g.r.nextInt(10) == 0 ? null : g.r.nextLong();
		f4_entity58 = g.r.nextInt(1000) - 100;
		f5_entity58 = g.r.nextInt(7) == 0 ? null : g.obj(Entity45.class, () -> Entity45.create(g));
		f6_entity58 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f6_entity58 != null) for (int i = 0, n = g.size(); i < n; i++) f6_entity58.add(g.obj(Entity28.class, () -> Entity28.create(g)));
		f7_entity58 = g.string();
		f8_entity58 = g.r.nextInt(1000) - 100;
		f9_entity58 = g.r.nextInt(1000) - 100;
	}

	static Entity58 create (Gen g) {
		Entity58 o = new Entity58();
		o.init(g);
		return o;
	}
}
