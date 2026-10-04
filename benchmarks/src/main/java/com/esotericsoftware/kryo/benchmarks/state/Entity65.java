package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity65 {
	@Tag(0) private int f0_entity65;
	@Tag(1) private List<Entity33> f1_entity65;
	@Tag(2) private Entity28 f2_entity65;
	@Tag(3) private Base3 f3_entity65;
	@Tag(4) private List<Entity45> f4_entity65;
	@Tag(5) private Entity29 f5_entity65;
	@Tag(6) private Entity36 f6_entity65;
	@Tag(7) private String f7_entity65;
	@Tag(8) private int f8_entity65;

	public Entity65 () {
	}

	void init (Gen g) {
		f0_entity65 = g.r.nextInt(1000) - 100;
		f1_entity65 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f1_entity65 != null) for (int i = 0, n = g.size(); i < n; i++) f1_entity65.add(g.obj(Entity33.class, () -> Entity33.create(g)));
		f2_entity65 = g.r.nextInt(7) == 0 ? null : g.obj(Entity28.class, () -> Entity28.create(g));
		f3_entity65 = g.r.nextInt(7) == 0 ? null : Base3.createAny(g);
		f4_entity65 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f4_entity65 != null) for (int i = 0, n = g.size(); i < n; i++) f4_entity65.add(g.obj(Entity45.class, () -> Entity45.create(g)));
		f5_entity65 = g.r.nextInt(7) == 0 ? null : g.obj(Entity29.class, () -> Entity29.create(g));
		f6_entity65 = g.r.nextInt(7) == 0 ? null : g.obj(Entity36.class, () -> Entity36.create(g));
		f7_entity65 = g.string();
		f8_entity65 = g.r.nextInt(1000) - 100;
	}

	static Entity65 create (Gen g) {
		Entity65 o = new Entity65();
		o.init(g);
		return o;
	}
}
