package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity45 {
	@Tag(0) private double f0_entity45;
	@Tag(1) private List<Entity9> f1_entity45;
	@Tag(2) private Kind0 f2_entity45;
	@Tag(3) private String f3_entity45;
	@Tag(4) private int f4_entity45;
	@Tag(5) private int f5_entity45;
	@Tag(6) private Entity24 f6_entity45;
	@Tag(7) private List<Entity32> f7_entity45;
	@Tag(8) private int f8_entity45;
	@Tag(9) private Map<String, Entity33> f9_entity45;
	@Tag(10) private Map<String, Entity30> f10_entity45;
	@Tag(11) private List<Entity25> f11_entity45;
	@Tag(12) private Entity33 f12_entity45;
	@Tag(13) private int[] f13_entity45;
	@Tag(14) private String f14_entity45;
	@Tag(15) private Entity33 f15_entity45;
	@Tag(16) private Entity1 f16_entity45;
	@Tag(17) private String f17_entity45;
	@Tag(18) private int f18_entity45;
	@Tag(19) private Base0 f19_entity45;
	@Tag(20) private boolean f20_entity45;
	@Tag(21) private boolean f21_entity45;
	@Tag(22) private boolean f22_entity45;
	@Tag(23) private Entity22 f23_entity45;
	@Tag(24) private Map<String, Entity25> f24_entity45;
	@Tag(25) private Entity31 f25_entity45;
	@Tag(26) private String f26_entity45;
	@Tag(27) private String f27_entity45;
	@Tag(28) private Entity21 f28_entity45;
	@Tag(29) private double f29_entity45;

	public Entity45 () {
	}

	void init (Gen g) {
		f0_entity45 = g.r.nextDouble() * 1000;
		f1_entity45 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f1_entity45 != null) for (int i = 0, n = g.size(); i < n; i++) f1_entity45.add(g.obj(Entity9.class, () -> Entity9.create(g)));
		f2_entity45 = g.r.nextInt(8) == 0 ? null : g.pick(Kind0.values());
		f3_entity45 = g.string();
		f4_entity45 = g.r.nextInt(1000) - 100;
		f5_entity45 = g.r.nextInt(1000) - 100;
		f6_entity45 = g.r.nextInt(7) == 0 ? null : g.obj(Entity24.class, () -> Entity24.create(g));
		f7_entity45 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f7_entity45 != null) for (int i = 0, n = g.size(); i < n; i++) f7_entity45.add(g.obj(Entity32.class, () -> Entity32.create(g)));
		f8_entity45 = g.r.nextInt(1000) - 100;
		f9_entity45 = g.r.nextInt(10) == 0 ? null : new HashMap<>(); if (f9_entity45 != null) for (int i = 0, n = g.size(); i < n; i++) f9_entity45.put(g.key(), g.obj(Entity33.class, () -> Entity33.create(g)));
		f10_entity45 = g.r.nextInt(10) == 0 ? null : new HashMap<>(); if (f10_entity45 != null) for (int i = 0, n = g.size(); i < n; i++) f10_entity45.put(g.key(), g.obj(Entity30.class, () -> Entity30.create(g)));
		f11_entity45 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f11_entity45 != null) for (int i = 0, n = g.size(); i < n; i++) f11_entity45.add(g.obj(Entity25.class, () -> Entity25.create(g)));
		f12_entity45 = g.r.nextInt(7) == 0 ? null : g.obj(Entity33.class, () -> Entity33.create(g));
		f13_entity45 = g.ints();
		f14_entity45 = g.string();
		f15_entity45 = g.r.nextInt(7) == 0 ? null : g.obj(Entity33.class, () -> Entity33.create(g));
		f16_entity45 = g.r.nextInt(7) == 0 ? null : g.obj(Entity1.class, () -> Entity1.create(g));
		f17_entity45 = g.string();
		f18_entity45 = g.r.nextInt(1000) - 100;
		f19_entity45 = g.r.nextInt(7) == 0 ? null : Base0.createAny(g);
		f20_entity45 = g.r.nextBoolean();
		f21_entity45 = g.r.nextBoolean();
		f22_entity45 = g.r.nextBoolean();
		f23_entity45 = g.r.nextInt(7) == 0 ? null : g.obj(Entity22.class, () -> Entity22.create(g));
		f24_entity45 = g.r.nextInt(10) == 0 ? null : new HashMap<>(); if (f24_entity45 != null) for (int i = 0, n = g.size(); i < n; i++) f24_entity45.put(g.key(), g.obj(Entity25.class, () -> Entity25.create(g)));
		f25_entity45 = g.r.nextInt(7) == 0 ? null : g.obj(Entity31.class, () -> Entity31.create(g));
		f26_entity45 = g.string();
		f27_entity45 = g.string();
		f28_entity45 = g.r.nextInt(7) == 0 ? null : g.obj(Entity21.class, () -> Entity21.create(g));
		f29_entity45 = g.r.nextDouble() * 1000;
	}

	static Entity45 create (Gen g) {
		Entity45 o = new Entity45();
		o.init(g);
		return o;
	}
}
