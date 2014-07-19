package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.ObjectArraySerializer;

/** @author Devin Chollak <legendary_hunterx@hotmail.com> */
public class ArraySerializerMethodTest extends KryoTestCase {
	{
		supportsCopy = true;
	}
	
	public void testArrays () {
		kryo.registerArrays(String.class, 3);
		kryo.registerArrays(Object.class, 2);
		kryo.registerArrays(int.class, 2);
		roundTrip(4, 4, new Object[] {null, null});
		roundTrip(6, 6, new Object[] {null, "2"});
		roundTrip(6, 18, new int[] {1, 2, 3, 4});
		roundTrip(7, 18, new int[] {1, 2, -100, 4});
		roundTrip(9, 18, new int[] {1, 2, -100, 40000});
		roundTrip(16, 16, new Object[][] { {"A1", "A2"}, {"B1", "B2"}});
		roundTrip(15, 15, new String[][] { {"1"}, {"2"}, {"100"}, {"4"}});
		roundTrip(12, 12, new String[] {"11", "2222", "3", "4"});
		roundTrip(11, 11, new String[] {"11", "2222", null, "4"});
		roundTrip(28, 51, new Object[] {new String[] {"11", "2222", null, "4"}, new int[] {1, 2, 3, 4}, new int[][] { {1, 2}, {100, 4}}});
		roundTrip(24, 24, new String[][][] { {{"1"}}, {{"2"}}, {{"1"}, {"0"}, {"0"}}, {{"4"}}});
		

		ObjectArraySerializer serializer = new ObjectArraySerializer(kryo, String[].class);
		kryo.register(String[].class, serializer);
		serializer.setElementsAreSameType(true);
		roundTrip(11, 11, new String[] {"11", "2222", null, "4"});
		serializer.setElementsAreSameType(false);
		roundTrip(11, 11, new String[] {"11", "2222", null, "4"});
		roundTrip(5, 5, new String[] {null, null, null});
		roundTrip(2, 2, new String[] {});
		serializer.setElementsAreSameType(true);
		roundTrip(12, 12, new String[] {"11", "2222", "3", "4"});
		serializer.setElementsCanBeNull(false);
		roundTrip(12, 12, new String[] {"11", "2222", "3", "4"});

		serializer = new ObjectArraySerializer(kryo, Float[].class);
		kryo.register(Float[][].class, serializer);
		kryo.register(Float[].class, serializer);
		Float[][] array = new Float[4][];
		array[0] = new Float[] {0.0f, 1.0f};
		array[1] = null;
		array[2] = new Float[] {2.0f, 3.0f};
		array[3] = new Float[] {3.0f};
		roundTrip(31, 31, array);
	}
}