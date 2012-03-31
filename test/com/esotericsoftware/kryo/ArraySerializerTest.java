
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.serializers.ArraySerializer;

public class ArraySerializerTest extends KryoTestCase {
	public void testArrays () {
		kryo.register(int[].class);
		kryo.register(int[][].class);
		kryo.register(int[][][].class);
		kryo.register(String[].class);
		kryo.register(Object[].class);
		roundTrip(7, new int[] {1, 2, 3, 4});
		roundTrip(8, new int[] {1, 2, -100, 4});
		roundTrip(10, new int[] {1, 2, -100, 40000});
		roundTrip(11, new int[][] { {1, 2}, {100, 4}});
		roundTrip(13, new int[][] { {1}, {2}, {100}, {4}});
		roundTrip(16, new int[][][] { { {1}, {2}}, { {100}, {4}}});
		roundTrip(19, new String[] {"11", "2222", "3", "4"});
		roundTrip(17, new String[] {"11", "2222", null, "4"});
		roundTrip(38,
			new Object[] {new String[] {"11", "2222", null, "4"}, new int[] {1, 2, 3, 4}, new int[][] { {1, 2}, {100, 4}}});

		ArraySerializer serializer = new ArraySerializer();
		kryo.register(int[].class, serializer);
		kryo.register(int[][].class, serializer);
		kryo.register(int[][][].class, serializer);
		kryo.register(String[].class, serializer);
		kryo.register(Object[].class, serializer);
		serializer.setDimensionCount(1);
		serializer.setElementsAreSameType(true);
		roundTrip(16, new String[] {"11", "2222", null, "4"});
		serializer.setElementsAreSameType(false);
		roundTrip(16, new String[] {"11", "2222", null, "4"});
		roundTrip(5, new String[] {null, null, null});
		roundTrip(2, new String[] {});
		serializer.setElementsAreSameType(true);
		roundTrip(18, new String[] {"11", "2222", "3", "4"});
		serializer.setElementsCanBeNull(false);
		roundTrip(14, new String[] {"11", "2222", "3", "4"});
		serializer.setLength(4);
		roundTrip(13, new String[] {"11", "2222", "3", "4"});

		serializer = new ArraySerializer();
		kryo.register(float[][].class, serializer);
		float[][] array = new float[4][];
		array[0] = new float[] {0.0f, 1.0f};
		array[1] = null;
		array[2] = new float[] {2.0f, 3.0f};
		array[3] = new float[] {3.0f};
		roundTrip(28, array);
	}
}
