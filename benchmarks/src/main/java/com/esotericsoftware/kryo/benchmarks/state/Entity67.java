package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity67 {
	@Tag(0) private List<Entity14> f0_entity67;
	@Tag(1) private Entity52 f1_entity67;
	@Tag(2) private int f2_entity67;
	@Tag(3) private List<Entity53> f3_entity67;
	@Tag(4) private List<Entity8> f4_entity67;
	@Tag(5) private long f5_entity67;
	@Tag(6) private Kind3 f6_entity67;
	@Tag(7) private Base7 f7_entity67;
	@Tag(8) private List<Entity28> f8_entity67;
	@Tag(9) private int f9_entity67;
	@Tag(10) private long f10_entity67;
	@Tag(11) private double f11_entity67;
	@Tag(12) private double f12_entity67;
	@Tag(13) private long f13_entity67;
	@Tag(14) private List<Entity47> f14_entity67;
	@Tag(15) private Integer f15_entity67;
	@Tag(16) private String f16_entity67;
	@Tag(17) private int f17_entity67;
	@Tag(18) private Kind4 f18_entity67;

	public Entity67 () {
	}

	void init (Gen g) {
		f0_entity67 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f0_entity67 != null) for (int i = 0, n = g.size(); i < n; i++) f0_entity67.add(g.obj(Entity14.class, () -> Entity14.create(g)));
		f1_entity67 = g.r.nextInt(7) == 0 ? null : g.obj(Entity52.class, () -> Entity52.create(g));
		f2_entity67 = g.r.nextInt(1000) - 100;
		f3_entity67 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f3_entity67 != null) for (int i = 0, n = g.size(); i < n; i++) f3_entity67.add(g.obj(Entity53.class, () -> Entity53.create(g)));
		f4_entity67 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f4_entity67 != null) for (int i = 0, n = g.size(); i < n; i++) f4_entity67.add(g.obj(Entity8.class, () -> Entity8.create(g)));
		f5_entity67 = g.r.nextLong() >>> g.r.nextInt(64);
		f6_entity67 = g.r.nextInt(8) == 0 ? null : g.pick(Kind3.values());
		f7_entity67 = g.r.nextInt(7) == 0 ? null : Base7.createAny(g);
		f8_entity67 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f8_entity67 != null) for (int i = 0, n = g.size(); i < n; i++) f8_entity67.add(g.obj(Entity28.class, () -> Entity28.create(g)));
		f9_entity67 = g.r.nextInt(1000) - 100;
		f10_entity67 = g.r.nextLong() >>> g.r.nextInt(64);
		f11_entity67 = g.r.nextDouble() * 1000;
		f12_entity67 = g.r.nextDouble() * 1000;
		f13_entity67 = g.r.nextLong() >>> g.r.nextInt(64);
		f14_entity67 = g.r.nextInt(10) == 0 ? null : new ArrayList<>(); if (f14_entity67 != null) for (int i = 0, n = g.size(); i < n; i++) f14_entity67.add(g.obj(Entity47.class, () -> Entity47.create(g)));
		f15_entity67 = g.r.nextInt(10) == 0 ? null : g.r.nextInt(100000);
		f16_entity67 = g.string();
		f17_entity67 = g.r.nextInt(1000) - 100;
		f18_entity67 = g.r.nextInt(8) == 0 ? null : g.pick(Kind4.values());
	}

	static Entity67 create (Gen g) {
		Entity67 o = new Entity67();
		o.init(g);
		return o;
	}
}
