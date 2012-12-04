
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.MapSerializer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

	public void testReadingNestedObjectsFirst () {
		ArrayList list = new ArrayList();
		list.add("1");
		list.add("1");
		list.add("2");
		list.add("1");
		list.add("1");
		List subList = list.subList(0, 5);

		kryo.setRegistrationRequired(false);
		kryo.register(ArrayList.class);
		kryo.register(subList.getClass(), new SubListSerializer());
		roundTrip(26, subList);
	}

	static public class SubListSerializer extends Serializer<List> {
		private Field listField, offsetField, sizeField;

		public SubListSerializer () {
			try {
				Class sublistClass = Class.forName("java.util.SubList");
				listField = sublistClass.getDeclaredField("l");
				offsetField = sublistClass.getDeclaredField("offset");
				sizeField = sublistClass.getDeclaredField("size");
				listField.setAccessible(true);
				offsetField.setAccessible(true);
				sizeField.setAccessible(true);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		public void write (Kryo kryo, Output output, List list) {
			try {
				kryo.writeClassAndObject(output, listField.get(list));
				int fromIndex = offsetField.getInt(list);
				int count = sizeField.getInt(list);
				output.writeInt(fromIndex);
				output.writeInt(count);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		public List read (Kryo kryo, Input input, Class<List> type) {
			List list = (List)kryo.readClassAndObject(input);
			int fromIndex = input.readInt();
			int count = input.readInt();
			return list.subList(fromIndex, fromIndex + count);
		}
	}
}
