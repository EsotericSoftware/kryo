
package com.esotericsoftware.kryo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import com.esotericsoftware.kryo.MapSerializerTest.KeyComparator;
import com.esotericsoftware.kryo.MapSerializerTest.KeyThatIsntComparable;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;

/** @author Nathan Sweet <misc@n4te.com> */
public class CollectionSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	public void testCollections () {
		kryo.register(ArrayList.class);
		kryo.register(LinkedList.class);
		kryo.register(CopyOnWriteArrayList.class);
		roundTrip(11, 11, list("1", "2", "3"));
		roundTrip(13, 19, list("1", "2", null, 1, 2));
		roundTrip(15, 24, list("1", "2", null, 1, 2, 5));
		roundTrip(11, 11, list("1", "2", "3"));
		roundTrip(11, 11, list("1", "2", "3"));
		roundTrip(13, 13, list("1", "2", list("3")));
		roundTrip(13, 13, new LinkedList(list("1", "2", list("3"))));
		roundTrip(13, 13, new CopyOnWriteArrayList(list("1", "2", list("3"))));

		CollectionSerializer serializer = new CollectionSerializer();
		kryo.register(ArrayList.class, serializer);
		kryo.register(LinkedList.class, serializer);
		kryo.register(CopyOnWriteArrayList.class, serializer);
		serializer.setElementClass(String.class, kryo.getSerializer(String.class));
		roundTrip(8, 8, list("1", "2", "3"));
		serializer.setElementClass(String.class, new StringSerializer());
		roundTrip(8, 8, list("1", "2", "3"));
		serializer.setElementsCanBeNull(false);
		roundTrip(8, 8, list("1", "2", "3"));

		kryo.register(TreeSet.class);
		TreeSet set = new TreeSet();
		set.add("1");
		set.add("2");
		roundTrip(9, 9, set);

		kryo.register(KeyThatIsntComparable.class);
		kryo.register(KeyComparator.class);
		set = new TreeSet(new KeyComparator());
		set.add(new KeyThatIsntComparable("1"));
		set.add(new KeyThatIsntComparable("2"));
		roundTrip(9, 9, set);
	}

}
