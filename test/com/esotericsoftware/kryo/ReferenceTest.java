
package com.esotericsoftware.kryo;

import java.util.Map;
import java.util.TreeMap;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.MapSerializer;

public class ReferenceTest extends KryoTestCase {
	static public class Ordering {
		public String order;
	}

	static public class Stuff extends TreeMap {
		public Ordering ordering;

		public Stuff (Ordering ordering) {
			this.ordering = ordering;
		}
	}

	public void testChildObjectBeforeReference () {
		Ordering ordering = new Ordering();
		ordering.order = "assbackwards";
		Stuff stuff = new Stuff(ordering);
		stuff.put("key", "value");
		stuff.put("something", 456);
		stuff.put("self", stuff);

		Kryo kryo = new Kryo();
		kryo.addDefaultSerializer(Stuff.class, new MapSerializer() {
			public void write (Kryo kryo, Output output, Map object) {
				kryo.writeObjectOrNull(output, ((Stuff)object).ordering, Ordering.class);
				super.write(kryo, output, object);
			}

			protected Map create (Kryo kryo, Input input, Class<Map> type) {
				Ordering ordering = kryo.readObjectOrNull(input, Ordering.class);
				return new Stuff(ordering);
			}
		});

		Output output = new Output(512, -1);
		kryo.writeObject(output, stuff);

		Input input = new Input(output.getBuffer(), 0, output.position());
		Stuff stuff2 = kryo.readObject(input, Stuff.class);

		assertEquals(stuff.ordering.order, stuff2.ordering.order);
		assertEquals(stuff.get("key"), stuff2.get("key"));
		assertEquals(stuff.get("something"), stuff2.get("something"));
		assertTrue(stuff.get("self") == stuff);
		assertTrue(stuff2.get("self") == stuff2);
	}
}
