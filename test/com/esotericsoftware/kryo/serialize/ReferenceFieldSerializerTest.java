
package com.esotericsoftware.kryo.serialize;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serialize.FieldSerializerTest.HasStringField;
import com.esotericsoftware.kryo.serialize.FieldSerializerTest.TestClass;

public class ReferenceFieldSerializerTest extends KryoTestCase {
	public void testReferenceFieldSerializer () {
		Kryo kryo = new Kryo();
		kryo.register(TestClass.class, new ReferenceFieldSerializer(kryo, TestClass.class));
		kryo.register(HasStringField.class, new ReferenceFieldSerializer(kryo, HasStringField.class));

		HasStringField hasStringField = new HasStringField();
		hasStringField.text = "moo";
		roundTrip(kryo, 7, hasStringField);
		roundTrip(new ReferenceFieldSerializer(kryo, HasStringField.class), 7, hasStringField);

		TestClass test = new TestClass();
		test.optional = 12;
		test.nullField = "value";
		test.text = "123";
		test.child = new TestClass();
		roundTrip(kryo, 41, test);
		test.nullField = null;
		roundTrip(kryo, 35, test);

		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(TestClass.class);
		serializer.removeField("optional");
		roundTrip(kryo, 33, test);
	}

	public void testReference () {
		Kryo kryo = new Kryo();
		kryo.register(TestClass.class, new ReferenceFieldSerializer(kryo, TestClass.class));

		TestClass test = new TestClass();
		test.optional = 12;
		test.nullField = "value";
		test.text = "123";
		test.child = new TestClass();
		test.child2 = test.child;
		TestClass object2 = roundTrip(kryo, 42, test);
		assertTrue(object2.child == object2.child2);
	}

	public void testCyclicReference () {
		Kryo kryo = new Kryo();
		kryo.register(TestClass.class, new ReferenceFieldSerializer(kryo, TestClass.class));

		TestClass test = new TestClass();
		test.optional = 12;
		test.nullField = "value";
		test.text = "123";
		test.child = test;
		TestClass object2 = roundTrip(kryo, 22, test);
		assertTrue(object2.child == object2);
	}
}
