
package com.esotericsoftware.kryo;

import java.io.FileNotFoundException;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;

public class TaggedFieldSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	public void testTaggedFieldSerializer () throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.moo = 2;
		object1.child = new TestClass();
		object1.child.moo = 5;
		object1.other = new AnotherClass();
		object1.other.value = "meow";
		object1.ignored = 32;
		kryo.setDefaultSerializer(TaggedFieldSerializer.class);
		kryo.register(TestClass.class);
		kryo.register(AnotherClass.class);
		TestClass object2 = roundTrip(57, object1);
		assertTrue(object2.ignored == 0);
	}

	public void testAddedField () throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();
		object1.other = new AnotherClass();
		object1.other.value = "meow";

		TaggedFieldSerializer serializer = new TaggedFieldSerializer(kryo, TestClass.class);
		serializer.removeField("text");
		kryo.register(TestClass.class, serializer);
		kryo.register(AnotherClass.class, new TaggedFieldSerializer(kryo, AnotherClass.class));
		roundTrip(39, object1);

		kryo.register(TestClass.class, new TaggedFieldSerializer(kryo, TestClass.class));
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}

	static public class TestClass {
		@Tag(0) public String text = "something";
		@Tag(1) public int moo = 120;
		@Tag(2) public long moo2 = 1234120;
		@Tag(3) public TestClass child;
		@Tag(4) public int zzz = 123;
		@Tag(5) public AnotherClass other;
		@Tag(6) @Deprecated public int ignored;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			TestClass other = (TestClass)obj;
			if (child == null) {
				if (other.child != null) return false;
			} else if (!child.equals(other.child)) return false;
			if (moo != other.moo) return false;
			if (moo2 != other.moo2) return false;
			if (text == null) {
				if (other.text != null) return false;
			} else if (!text.equals(other.text)) return false;
			if (zzz != other.zzz) return false;
			return true;
		}
	}

	static public class AnotherClass {
		@Tag(1) String value;
	}
}
