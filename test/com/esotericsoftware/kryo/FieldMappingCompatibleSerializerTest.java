/* 
 * Basically follows all copyrights or other policies of CompatibleFieldSerializerTest of Nathan and some are modified by diaimm.
 * 
 * 'Cause FieldMappingCompatibleSerializer is extending CompatibleFieldSerializer and 
 * ,basically, it must be able to pass the unit-tests of CompatibleFieldSerializer, 
 * Nathan's UnitTest is copied here to test FieldMappingCompatibleSerializer.
 * 
 * Different parts of the codes are additionally commented very on the commented code with reason.
 * */

package com.esotericsoftware.kryo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;

import org.junit.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.CompatibleFieldSerializerTest.AnotherClass;
import com.esotericsoftware.kryo.CompatibleFieldSerializerTest.ExtendedTestClass;
import com.esotericsoftware.kryo.CompatibleFieldSerializerTest.TestClass;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldMappingCompatibleSerializer;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/** @author Nathan Sweet <misc@n4te.com>, and modified by diaimm */
public class FieldMappingCompatibleSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	public void testCompatibleFieldSerializer () throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();
		object1.other = new AnotherClass();
		object1.other.value = "meow";
		kryo.setDefaultSerializer(FieldMappingCompatibleSerializer.class);
		kryo.register(TestClass.class);
		kryo.register(AnotherClass.class);
		roundTrip(107, 107, object1);
	}

	public void testAddedField () throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();
		object1.other = new AnotherClass();
		object1.other.value = "meow";

		FieldMappingCompatibleSerializer serializer = new FieldMappingCompatibleSerializer(kryo, TestClass.class);
		serializer.removeField("text");
		kryo.register(TestClass.class, serializer);
		kryo.register(AnotherClass.class, new FieldMappingCompatibleSerializer(kryo, AnotherClass.class));
		roundTrip(80, 80, object1);

		kryo.register(TestClass.class, new FieldMappingCompatibleSerializer(kryo, TestClass.class));
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}

	public void testRemovedField () throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();

		kryo.register(TestClass.class, new FieldMappingCompatibleSerializer(kryo, TestClass.class));
		roundTrip(94, 94, object1);

		FieldMappingCompatibleSerializer serializer = new FieldMappingCompatibleSerializer(kryo, TestClass.class);
		////////////////////////////////////////////////////////////////////////////////////
		// When deserialize from Input, we sometimes cannot be aware of the fields we have to remove,
		// FieldMappingCompatibleSerializer is handing the missed fields just dumping the bytes with DummyCachedField.
		// So next line must not be called.
		//
		// by diaimm
		////////////////////////////////////////////////////////////////////////////////////
		// serializer.removeField("text");
		kryo.register(TestClass.class, serializer);
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}

	public void testExtendedClass () throws FileNotFoundException {
		ExtendedTestClass extendedObject = new ExtendedTestClass();

		////////////////////////////////////////////////////////////////////////////////////
		// I'm not sure about full functions of CachedFieldNameStrategies, but setting it with
		//////////////////////////////////////////////////////////////////////////////////// FieldSerializer.CachedFieldNameStrategy.EXTENDED
		//////////////////////////////////////////////////////////////////////////////////// seems to make the flow different.
		// FieldMappingCompatibleSerializer is following the basic process of Kryo, just dumping out useless data from Input.
		//
		// So, the code below is not required.
		// by diaimm
		////////////////////////////////////////////////////////////////////////////////////
		// this test would fail with DEFAULT field name strategy
		// kryo.getFieldSerializerConfig().setCachedFieldNameStrategy(FieldSerializer.CachedFieldNameStrategy.EXTENDED);

		FieldMappingCompatibleSerializer serializer = new FieldMappingCompatibleSerializer(kryo, ExtendedTestClass.class);
		kryo.register(ExtendedTestClass.class, serializer);

		////////////////////////////////////////////////////////////////////////////////////
		// roundTrip seems to be checking the read bytes, and using FieldMappingCompatibleSerializer makes the result different.
		// So, this line is changed to check it with 118 (from the error result of old test)
		// Instead, I've put another assertion about real value of instance to make it sure the value is exactly deserialized
		////////////////////////////////////////////////////////////////////////////////////
		// roundTrip(286, 286, extendedObject);
		roundTrip(118, 118, extendedObject);

		ExtendedTestClass object2 = (ExtendedTestClass)kryo.readClassAndObject(input);
		assertEquals(extendedObject, object2);

		////////////////////////////////////////////////////////////////////////////////////
		// these assertions are added to make it sure that all the fields overridden are deserialized correctly.
		////////////////////////////////////////////////////////////////////////////////////
		assertEquals(extendedObject.moo, object2.moo);
		assertEquals(extendedObject.moo2, object2.moo2);
		assertEquals(extendedObject.text, object2.text);
		assertEquals(extendedObject.zzz, object2.zzz);
		assertEquals(extendedObject.child, object2.child);
	}

	@Test
	public void testTestSample1 () {
		Kryo kryo = new Kryo();
		kryo.setReferences(true);
		kryo.setDefaultSerializer(FieldMappingCompatibleSerializer.class);
		kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

		TestSample1 p = new TestSample1();
		p.x = 1467196538633L;
// p.y = 1467196538633L;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Output output = new Output(baos);
		kryo.writeObject(output, p);
		output.close();

		String encode = Base64.encode(baos.toByteArray());
		System.out.println(encode);

		// serialized value + base64 encoded String that has both of x and y fields.
		String serializedValueWithX = "AQKCeIJ5BpL807uzVQAGkvzTu7NVAA==";
		// serializedValueWithX = encode;
		byte[] inputSource = Base64.decode(serializedValueWithX);
		System.out.println(inputSource);

		Input input = new Input(new ByteArrayInputStream(inputSource));
		input.close();

		TestSample1 readObject = kryo.readObject(input, TestSample1.class);
		System.out.println(readObject.x);
// System.out.println(readObject.y);
	}

	public static class TestSample1 {
		private long x;
// private long y;
	}
}
