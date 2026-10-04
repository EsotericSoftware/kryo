package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class Entity22 {
	@Tag(0) private int f0_entity22;
	@Tag(1) private String f1_entity22;
	@Tag(2) private String f2_entity22;
	@Tag(3) private int f3_entity22;
	@Tag(4) private int f4_entity22;

	public Entity22 () {
	}

	void init (Gen g) {
		f0_entity22 = g.r.nextInt(1000) - 100;
		f1_entity22 = g.string();
		f2_entity22 = g.string();
		f3_entity22 = g.r.nextInt(1000) - 100;
		f4_entity22 = g.r.nextInt(1000) - 100;
	}

	static Entity22 create (Gen g) {
		Entity22 o = new Entity22();
		o.init(g);
		return o;
	}
}
