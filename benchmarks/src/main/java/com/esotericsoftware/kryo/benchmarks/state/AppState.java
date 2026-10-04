package com.esotericsoftware.kryo.benchmarks.state;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import java.util.*;

public class AppState {
	@Tag(0) private String name;
	@Tag(1) private long version;
	@Tag(3) private List<Entity70> list0;
	@Tag(4) private List<Entity71> list1;
	@Tag(5) private List<Entity72> list2;
	@Tag(6) private List<Entity73> list3;
	@Tag(7) private List<Entity74> list4;
	@Tag(8) private List<Entity75> list5;
	@Tag(9) private List<Entity76> list6;
	@Tag(10) private List<Entity77> list7;
	@Tag(2) private Map<String, Entity70> index;

	public AppState () {
	}

	public static AppState create (long seed, int scale) {
		Gen g = new Gen(seed);
		AppState s = new AppState();
		s.name = "state";
		s.version = 7;
		s.list0 = new ArrayList<>();
		for (int i = 0; i < scale; i++) s.list0.add(Entity70.create(g));
		s.list1 = new ArrayList<>();
		for (int i = 0; i < scale; i++) s.list1.add(Entity71.create(g));
		s.list2 = new ArrayList<>();
		for (int i = 0; i < scale; i++) s.list2.add(Entity72.create(g));
		s.list3 = new ArrayList<>();
		for (int i = 0; i < scale; i++) s.list3.add(Entity73.create(g));
		s.list4 = new ArrayList<>();
		for (int i = 0; i < scale; i++) s.list4.add(Entity74.create(g));
		s.list5 = new ArrayList<>();
		for (int i = 0; i < scale; i++) s.list5.add(Entity75.create(g));
		s.list6 = new ArrayList<>();
		for (int i = 0; i < scale; i++) s.list6.add(Entity76.create(g));
		s.list7 = new ArrayList<>();
		for (int i = 0; i < scale; i++) s.list7.add(Entity77.create(g));
		s.index = new HashMap<>();
		for (Entity70 e : s.list0) s.index.put(g.key(), e);
		return s;
	}

	public static Class[] classes () {
		return new Class[] {Kind0.class, Kind1.class, Kind2.class, Kind3.class, Kind4.class, Kind5.class, Base0.class, Entity1.class, Entity2.class, Base3.class, Entity4.class, Entity5.class, Entity6.class, Base7.class, Entity8.class, Entity9.class, Entity10.class, Entity11.class, Entity12.class, Entity13.class, Entity14.class, Entity15.class, Entity16.class, Entity17.class, Entity18.class, Entity19.class, Entity20.class, Entity21.class, Entity22.class, Entity23.class, Entity24.class, Entity25.class, Entity26.class, Entity27.class, Entity28.class, Entity29.class, Entity30.class, Entity31.class, Entity32.class, Entity33.class, Entity34.class, Entity35.class, Entity36.class, Entity37.class, Entity38.class, Entity39.class, Entity40.class, Entity41.class, Entity42.class, Entity43.class, Entity44.class, Entity45.class, Entity46.class, Entity47.class, Entity48.class, Entity49.class, Entity50.class, Entity51.class, Entity52.class, Entity53.class, Entity54.class, Entity55.class, Entity56.class, Entity57.class, Entity58.class, Entity59.class, Entity60.class, Entity61.class, Entity62.class, Entity63.class, Entity64.class, Entity65.class, Entity66.class, Entity67.class, Entity68.class, Entity69.class, Entity70.class, Entity71.class, Entity72.class, Entity73.class, Entity74.class, Entity75.class, Entity76.class, Entity77.class, AppState.class};
	}
}
