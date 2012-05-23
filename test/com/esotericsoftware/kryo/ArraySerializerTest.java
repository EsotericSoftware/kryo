
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.ObjectArraySerializer;

/** @author Nathan Sweet <misc@n4te.com> */
public class ArraySerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	public void testArrays () {
		kryo.register(int[].class);
		kryo.register(int[][].class);
		kryo.register(int[][][].class);
		kryo.register(String[].class);
		kryo.register(Object[].class);
		roundTrip(6, new int[] {1, 2, 3, 4});
		roundTrip(7, new int[] {1, 2, -100, 4});
		roundTrip(9, new int[] {1, 2, -100, 40000});
		roundTrip(9, new int[][] { {1, 2}, {100, 4}});
		roundTrip(11, new int[][] { {1}, {2}, {100}, {4}});
		roundTrip(13, new int[][][] { { {1}, {2}}, { {100}, {4}}});
		roundTrip(12, new String[] {"11", "2222", "3", "4"});
		roundTrip(11, new String[] {"11", "2222", null, "4"});
		roundTrip(28,
			new Object[] {new String[] {"11", "2222", null, "4"}, new int[] {1, 2, 3, 4}, new int[][] { {1, 2}, {100, 4}}});

		ObjectArraySerializer serializer = new ObjectArraySerializer();
		kryo.register(String[].class, serializer);
		serializer.setElementsAreSameType(true);
		roundTrip(11, new String[] {"11", "2222", null, "4"});
		serializer.setElementsAreSameType(false);
		roundTrip(11, new String[] {"11", "2222", null, "4"});
		roundTrip(5, new String[] {null, null, null});
		roundTrip(2, new String[] {});
		serializer.setElementsAreSameType(true);
		roundTrip(12, new String[] {"11", "2222", "3", "4"});
		serializer.setElementsCanBeNull(false);
		roundTrip(12, new String[] {"11", "2222", "3", "4"});

		serializer = new ObjectArraySerializer();
		kryo.register(Float[][].class, serializer);
		kryo.register(Float[].class, serializer);
		Float[][] array = new Float[4][];
		array[0] = new Float[] {0.0f, 1.0f};
		array[1] = null;
		array[2] = new Float[] {2.0f, 3.0f};
		array[3] = new Float[] {3.0f};
		roundTrip(31, array);
	}
}
