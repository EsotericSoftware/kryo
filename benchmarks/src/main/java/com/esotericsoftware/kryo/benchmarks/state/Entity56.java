package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity56 {
	@Tag(0) private String f0_entity56;
	@Tag(1) private String f1_entity56;
	@Tag(2) private Entity54 f2_entity56;

	public Entity56 () {
	}

	void init (Gen g) {
		f0_entity56 = g.string();
		f1_entity56 = g.string();
		f2_entity56 = g.r.nextInt(7) == 0 ? null : g.obj(Entity54.class, () -> Entity54.create(g));
	}

	static Entity56 create (Gen g) {
		Entity56 o = new Entity56();
		o.init(g);
		return o;
	}
}
