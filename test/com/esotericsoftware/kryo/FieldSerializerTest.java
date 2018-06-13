/* Copyright (c) 2008, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryo;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.CollectionSerializer.BindCollection;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.IntArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.LongArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer.Bind;
import com.esotericsoftware.kryo.serializers.FieldSerializer.Optional;
import com.esotericsoftware.kryo.serializers.MapSerializer.BindMap;

import static com.esotericsoftware.kryo.KryoTestUtil.*;
import static org.junit.Assert.*;

/** @author Nathan Sweet <misc@n4te.com> */
public class FieldSerializerTest  {
	private Kryo kryo = new TestKryoFactory().create();
	private final boolean supportsCopy = true;
	private KryoTestSupport support = new KryoTestSupport(kryo, supportsCopy);

	@Test
	public void testDefaultTypes () {
		kryo.register(DefaultTypes.class);
		kryo.register(byte[].class);
		DefaultTypes test = new DefaultTypes();
		test.booleanField = true;
		test.byteField = 123;
		test.charField = 'Z';
		test.shortField = 12345;
		test.intField = 123456;
		test.longField = 123456789;
		test.floatField = 123.456f;
		test.doubleField = 1.23456d;
		test.BooleanField = true;
		test.ByteField = -12;
		test.CharacterField = 'X';
		test.ShortField = -12345;
		test.IntegerField = -123456;
		test.LongField = -123456789l;
		test.FloatField = -123.3f;
		test.DoubleField = -0.121231d;
		test.StringField = "stringvalue";
		test.byteArrayField = new byte[] {2, 1, 0, -1, -2};
		support.roundTrip(78, 88, test);

		kryo.register(HasStringField.class);
		test.hasStringField = new HasStringField();
		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(DefaultTypes.class);
		serializer.getField("hasStringField").setCanBeNull(false);
		support.roundTrip(79, 89, test);
		serializer.setFixedFieldTypes(true);
		serializer.getField("hasStringField").setCanBeNull(false);
		support.roundTrip(78, 88, test);
	}

	@Test
	public void testFieldRemoval () {
		kryo.register(DefaultTypes.class);
		kryo.register(byte[].class);
		kryo.register(HasStringField.class);

		HasStringField hasStringField = new HasStringField();
		hasStringField.text = "moo";
		support.roundTrip(4, 4, hasStringField);

		DefaultTypes test = new DefaultTypes();
		test.intField = 12;
		test.StringField = "value";
		test.CharacterField = 'X';
		test.child = new DefaultTypes();
		support.roundTrip(71, 91, test);

		support = new KryoTestSupport(kryo, !supportsCopy);
		test.StringField = null;
		support.roundTrip(67, 87, test);

		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(DefaultTypes.class);
		serializer.removeField("LongField");
		serializer.removeField("floatField");
		serializer.removeField("FloatField");

		support = new KryoTestSupport(kryo, supportsCopy);
		support.roundTrip(55, 75, test);
	}

	@Test
	public void testFieldRemovalOnGenerics () {
		kryo.register(IsGeneric.class);
		kryo.register(DefaultTypes.class);
		kryo.register(byte[].class);

		FieldSerializer serializer = new FieldSerializer<IsGeneric>(kryo, IsGeneric.class);
		serializer.removeField("y");
		kryo.register(IsGeneric.class, serializer);

		IsGeneric<IsGeneric<DefaultTypes>> test = new IsGeneric<IsGeneric<DefaultTypes>>();
		test.item = new IsGeneric<DefaultTypes>();

		try {
			support.roundTrip(5, 11, test);
		} catch (KryoException e) {
			e.printStackTrace();
			fail("Couldn't serialize generic with a removed field.");
		}
	}

	@Test
	public void testOptionalRegistration () {
		kryo.setRegistrationRequired(false);
		DefaultTypes test = new DefaultTypes();
		test.intField = 12;
		test.StringField = "value";
		test.CharacterField = 'X';
		test.hasStringField = new HasStringField();
		test.child = new DefaultTypes();
		test.child.hasStringField = new HasStringField();
		support.roundTrip(195, 215, test);
		test.hasStringField = null;
		support.roundTrip(193, 213, test);

		test = new DefaultTypes();
		test.booleanField = true;
		test.byteField = 123;
		test.charField = 1234;
		test.shortField = 12345;
		test.intField = 123456;
		test.longField = 123456789;
		test.floatField = 123.456f;
		test.doubleField = 1.23456d;
		test.BooleanField = true;
		test.ByteField = -12;
		test.CharacterField = 123;
		test.ShortField = -12345;
		test.IntegerField = -123456;
		test.LongField = -123456789l;
		test.FloatField = -123.3f;
		test.DoubleField = -0.121231d;
		test.StringField = "stringvalue";
		test.byteArrayField = new byte[] {2, 1, 0, -1, -2};

		kryo = new Kryo();
		support = new KryoTestSupport(kryo, supportsCopy);
		support.roundTrip(140, 150, test);

		C c = new C();
		c.a = new A();
		c.a.value = 123;
		c.a.b = new B();
		c.a.b.value = 456;
		c.d = new D();
		c.d.e = new E();
		c.d.e.f = new F();
		support.roundTrip(63, 73, c);
	}

	@Test
	public void testReferences () {
		C c = new C();
		c.a = new A();
		c.a.value = 123;
		c.a.b = new B();
		c.a.b.value = 456;
		c.d = new D();
		c.d.e = new E();
		c.d.e.f = new F();
		c.d.e.f.a = c.a;

		kryo = new Kryo();
		support = new KryoTestSupport(kryo, supportsCopy);

		C c2 = support.roundTrip(63, 73, c).getDeserializeObject();
		assertTrue(c2.a == c2.d.e.f.a);

		// Test reset clears unregistered class names.
		c2 = support.roundTrip(63, 73, c).getDeserializeObject();
		assertTrue(c2.a == c2.d.e.f.a);

		kryo = new Kryo();
		kryo.setRegistrationRequired(true);
		support = new KryoTestSupport(kryo, supportsCopy);
		kryo.register(A.class);
		kryo.register(B.class);
		kryo.register(C.class);
		kryo.register(D.class);
		kryo.register(E.class);
		kryo.register(F.class);
		c2 = support.roundTrip(15, 25, c).getDeserializeObject();
		assertTrue(c2.a == c2.d.e.f.a);
	}

	@Test
	public void testRegistrationOrder () {
		A a = new A();
		a.value = 100;
		a.b = new B();
		a.b.value = 200;
		a.b.a = new A();
		a.b.a.value = 300;

		kryo.register(A.class);
		kryo.register(B.class);
		support.roundTrip(10, 16, a);

		kryo = new Kryo();
		kryo.setReferences(false);
		kryo.register(B.class);
		kryo.register(A.class);
		support.roundTrip(10, 16, a);
	}

	@Test
	public void testExceptionTrace () {
		C c = new C();
		c.a = new A();
		c.a.value = 123;
		c.a.b = new B();
		c.a.b.value = 456;
		c.d = new D();
		c.d.e = new E();
		c.d.e.f = new F();

		Kryo kryoWithoutF = new Kryo();
		kryoWithoutF.setReferences(false);
		kryoWithoutF.setRegistrationRequired(true);
		kryoWithoutF.register(A.class);
		kryoWithoutF.register(B.class);
		kryoWithoutF.register(C.class);
		kryoWithoutF.register(D.class);
		kryoWithoutF.register(E.class);

		Output output = new Output(512);
		try {
			kryoWithoutF.writeClassAndObject(output, c);
			fail("Should have failed because F is not registered.");
		} catch (KryoException ignored) {
		}

		kryo.register(A.class);
		kryo.register(B.class);
		kryo.register(C.class);
		kryo.register(D.class);
		kryo.register(E.class);
		kryo.register(F.class);
		kryo.setRegistrationRequired(true);

		output.clear();
		kryo.writeClassAndObject(output, c);
		output.flush();
		assertEquals(14, output.total());

		Input input = new Input(output.getBuffer());
		kryo.readClassAndObject(input);

		try {
			input.setPosition(0);
			kryoWithoutF.readClassAndObject(input);
			fail("Should have failed because F is not registered.");
		} catch (KryoException ignored) {
		}
	}

	@Test
	public void testNoDefaultConstructor () {
		kryo.register(SimpleNoDefaultConstructor.class, new Serializer<SimpleNoDefaultConstructor>() {
			public SimpleNoDefaultConstructor read (Kryo kryo, Input input, Class<SimpleNoDefaultConstructor> type) {
				return new SimpleNoDefaultConstructor(input.readInt(true));
			}

			public void write (Kryo kryo, Output output, SimpleNoDefaultConstructor object) {
				output.writeInt(object.constructorValue, true);
			}

			public SimpleNoDefaultConstructor copy (Kryo kryo, SimpleNoDefaultConstructor original) {
				return new SimpleNoDefaultConstructor(original.constructorValue);
			}
		});
		SimpleNoDefaultConstructor object1 = new SimpleNoDefaultConstructor(2);
		support.roundTrip(2, 5, object1);

		kryo.register(ComplexNoDefaultConstructor.class,
			new FieldSerializer<ComplexNoDefaultConstructor>(kryo, ComplexNoDefaultConstructor.class) {
				public void write (Kryo kryo, Output output, ComplexNoDefaultConstructor object) {
					output.writeString(object.name);
					super.write(kryo, output, object);
				}

				protected ComplexNoDefaultConstructor create (Kryo kryo, Input input, Class type) {
					String name = input.readString();
					return new ComplexNoDefaultConstructor(name);
				}

				protected ComplexNoDefaultConstructor createCopy (Kryo kryo, ComplexNoDefaultConstructor original) {
					return new ComplexNoDefaultConstructor(original.name);
				}
			});
		ComplexNoDefaultConstructor object2 = new ComplexNoDefaultConstructor("has no zero arg constructor!");
		object2.anotherField1 = 1234;
		object2.anotherField2 = "abcd";
		support.roundTrip(35, 37, object2);
	}

	@Test
	public void testNonNull () {
		kryo.register(HasNonNull.class);
		HasNonNull nonNullValue = new HasNonNull();
		nonNullValue.nonNullText = "moo";
		support.roundTrip(4, 4, nonNullValue);
	}

	@Test
	public void testDefaultSerializerAnnotation () {
		kryo = new Kryo();
		support = new KryoTestSupport(kryo, supportsCopy);
		support.roundTrip(82, 89, new HasDefaultSerializerAnnotation(123));
	}

	@Test
	public void testOptionalAnnotation () {
		kryo = new Kryo();
		support = new KryoTestSupport(kryo, supportsCopy);
		support.roundTrip(72, 72, new HasOptionalAnnotation());

		kryo = new Kryo();
		support = new KryoTestSupport(kryo, supportsCopy);
		kryo.getContext().put("smurf", null);
		support.roundTrip(73, 76, new HasOptionalAnnotation());
	}

	@Test
	public void testCyclicGrgaph () throws Exception {
		kryo = new Kryo();
		kryo.setRegistrationRequired(true);
		kryo.register(DefaultTypes.class);
		kryo.register(byte[].class);
		support = new KryoTestSupport(kryo, supportsCopy);
		DefaultTypes test = new DefaultTypes();
		test.child = test;
		support.roundTrip(35, 45, test);
	}

	@SuppressWarnings("synthetic-access")
	@Test
	public void testInstantiatorStrategy () {
		kryo.register(HasArgumentConstructor.class);
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		HasArgumentConstructor test = new HasArgumentConstructor("cow");
		support.roundTrip(4, 4, test);

		kryo.register(HasPrivateConstructor.class);
		test = new HasPrivateConstructor();
		support.roundTrip(4, 4, test);
	}

	/** This test uses StdInstantiatorStrategy and therefore requires a no-arg constructor. **/
	@SuppressWarnings("synthetic-access")
	@Test
	public void testDefaultInstantiatorStrategy () {
		kryo.register(HasArgumentConstructor.class);
		HasArgumentConstructor test = new HasPrivateConstructor();
		HasPrivateConstructor.invocations = 0;

		kryo.register(HasPrivateConstructor.class);
		support.roundTrip(4, 4, test);
		assertEquals("Default constructor should not be invoked with StdInstantiatorStrategy strategy", 25,
			HasPrivateConstructor.invocations);
	}

	/** This test uses StdInstantiatorStrategy and should bypass invocation of no-arg constructor, even if it is provided. **/
	@SuppressWarnings("synthetic-access")
	@Test
	public void testStdInstantiationStrategy () {
		kryo.register(HasArgumentConstructor.class);
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		HasArgumentConstructor test = new HasPrivateConstructor();
		HasPrivateConstructor.invocations = 0;

		kryo.register(HasPrivateConstructor.class);
		support.roundTrip(4, 4, test);
		assertEquals("Default constructor should not be invoked with StdInstantiatorStrategy strategy", 0,
			HasPrivateConstructor.invocations);
	}

	@Test
	public void testGenericTypesOptimized () {
		doTestGenerics(true);
	}

	@Test
	public void testGenericTypesNonOptimized () {
		doTestGenerics(false);
	}

	/** Check that it is OK to change the optimizedGenerics setting on the same Kryo instance multiple times. */
	@Test
	public void testGenericTypesOptimizedAndNonOptimized () {
		Kryo kryoInstance = kryo;
		doTestGenerics(true);
		assertEquals("The same instance of Kryo should be used", kryoInstance, kryo);
		doTestGenerics(false);
		assertEquals("The same instance of Kryo should be used", kryoInstance, kryo);
		doTestGenerics(true);
		assertEquals("The same instance of Kryo should be used", kryoInstance, kryo);
		doTestGenerics(false);
		assertEquals("The same instance of Kryo should be used", kryoInstance, kryo);
	}

	private void doTestGenerics(boolean optimizedGenerics) {
		kryo.getFieldSerializerConfig().setOptimizedGenerics(optimizedGenerics);
		kryo.setReferences(true);
		kryo.setRegistrationRequired(true);
		kryo.register(HasGenerics.class);
		kryo.register(ArrayList.class);
		kryo.register(ArrayList[].class);
		kryo.register(HashMap.class);

		// It may happen that classes were registered already befor this function
		// was called. In this case, invoke the setters on the FieldSerializer
		// objects directly.
		FieldSerializer<?> fieldSerializer;
		fieldSerializer = (FieldSerializer<?>)kryo.getSerializer(HasGenerics.class);
		fieldSerializer.setOptimizedGenerics(optimizedGenerics);

		HasGenerics test = new HasGenerics();
		test.list1 = new ArrayList();
		test.list1.add(1);
		test.list1.add(2);
		test.list1.add(3);
		test.list1.add(4);
		test.list1.add(5);
		test.list1.add(6);
		test.list1.add(7);
		test.list1.add(8);
		test.list2 = new ArrayList();
		test.list2.add(test.list1);
		test.map1 = new HashMap();
		test.map1.put("a", test.list1);
		test.list3 = new ArrayList();
		test.list3.add(null);
		test.list4 = new ArrayList();
		test.list4.add(null);
		test.list5 = new ArrayList();
		test.list5.add("one");
		test.list5.add("two");
		support.roundTrip(optimizedGenerics ? 53 : 56, optimizedGenerics ? 80 : 83, test);
		ArrayList[] al = new ArrayList[1];
		al[0] = new ArrayList(Arrays.asList(new String[] {"A", "B", "S"}));
		support.roundTrip(18, 18, al);
	}

	@Test
	public void testRegistration () {
		int id = kryo.getNextRegistrationId();
		kryo.register(DefaultTypes.class, id);
		kryo.register(DefaultTypes.class, id);
		kryo.register(new Registration(byte[].class, kryo.getDefaultSerializer(byte[].class), id + 1));
		kryo.register(byte[].class, kryo.getDefaultSerializer(byte[].class), id + 1);
		kryo.register(HasStringField.class, kryo.getDefaultSerializer(HasStringField.class));

		DefaultTypes test = new DefaultTypes();
		test.intField = 12;
		test.StringField = "meow";
		test.CharacterField = 'z';
		test.byteArrayField = new byte[] {0, 1, 2, 3, 4};
		test.child = new DefaultTypes();
		support.roundTrip(75, 95, test);
	}

	@Test
	public void testTransients () {
		kryo.register(HasTransients.class);
		HasTransients objectWithTransients1 = new HasTransients();
		objectWithTransients1.transientField1 = "Test";
		objectWithTransients1.anotherField2 = 5;
		objectWithTransients1.anotherField3 = "Field2";

		FieldSerializer<HasTransients> ser = (FieldSerializer<HasTransients>)kryo.getSerializer(HasTransients.class);
		ser.setCopyTransient(false);

		HasTransients objectWithTransients3 = kryo.copy(objectWithTransients1);
		assertTrue("Objects should be different if copy does not include transient fields",
			!objectWithTransients3.equals(objectWithTransients1));
		assertEquals("transient fields should be null", objectWithTransients3.transientField1, null);

		ser.setCopyTransient(true);
		HasTransients objectWithTransients2 = kryo.copy(objectWithTransients1);
		assertEquals("Objects should be equal if copy includes transient fields", objectWithTransients2, objectWithTransients1);
	}

	@Test
	public void testTransientsUsingGlobalConfig () {
		kryo.getFieldSerializerConfig().setCopyTransient(false);
		kryo.register(HasTransients.class);
		HasTransients objectWithTransients1 = new HasTransients();
		objectWithTransients1.transientField1 = "Test";
		objectWithTransients1.anotherField2 = 5;
		objectWithTransients1.anotherField3 = "Field2";

		FieldSerializer<HasTransients> ser = (FieldSerializer<HasTransients>)kryo.getSerializer(HasTransients.class);
		HasTransients objectWithTransients3 = kryo.copy(objectWithTransients1);
		assertTrue("Objects should be different if copy does not include transient fields",
			!objectWithTransients3.equals(objectWithTransients1));
		assertEquals("transient fields should be null", objectWithTransients3.transientField1, null);

		ser.setCopyTransient(true);
		HasTransients objectWithTransients2 = kryo.copy(objectWithTransients1);
		assertEquals("Objects should be equal if copy includes transient fields", objectWithTransients2, objectWithTransients1);
	}

	@Test
	public void testSerializeTransients () {
		kryo.register(HasTransients.class);
		HasTransients objectWithTransients1 = new HasTransients();
		objectWithTransients1.transientField1 = "Test";
		objectWithTransients1.anotherField2 = 5;
		objectWithTransients1.anotherField3 = "Field2";

		ByteArrayOutputStream outputStream;
		Output output;
		Input input;
		byte[] outBytes;

		FieldSerializer<HasTransients> ser = (FieldSerializer<HasTransients>)kryo.getSerializer(HasTransients.class);
		ser.setSerializeTransient(false);

		outputStream = new ByteArrayOutputStream();
		output = new Output(outputStream);
		ser.write(kryo, output, objectWithTransients1);
		output.flush();

		outBytes = outputStream.toByteArray();
		input = new Input(outBytes);
		HasTransients objectWithTransients3 = ser.read(kryo, input, HasTransients.class);
		assertTrue("Objects should be different if write does not include transient fields",
			!objectWithTransients3.equals(objectWithTransients1));
		assertEquals("transient fields should be null", objectWithTransients3.transientField1, null);

		ser.setSerializeTransient(true);

		outputStream = new ByteArrayOutputStream();
		output = new Output(outputStream);
		ser.write(kryo, output, objectWithTransients1);
		output.flush();

		outBytes = outputStream.toByteArray();
		input = new Input(outBytes);
		HasTransients objectWithTransients2 = ser.read(kryo, input, HasTransients.class);
		assertTrue("Objects should be equal if write includes transient fields",
			objectWithTransients2.equals(objectWithTransients1));
	}

	@Test
	public void testSerializeTransientsUsingGlobalConfig () {
		kryo.getFieldSerializerConfig().setSerializeTransient(false);
		kryo.register(HasTransients.class);
		HasTransients objectWithTransients1 = new HasTransients();
		objectWithTransients1.transientField1 = "Test";
		objectWithTransients1.anotherField2 = 5;
		objectWithTransients1.anotherField3 = "Field2";

		ByteArrayOutputStream outputStream;
		Output output;
		Input input;
		byte[] outBytes;

		FieldSerializer<HasTransients> ser = (FieldSerializer<HasTransients>)kryo.getSerializer(HasTransients.class);
		outputStream = new ByteArrayOutputStream();
		output = new Output(outputStream);
		ser.write(kryo, output, objectWithTransients1);
		output.flush();

		outBytes = outputStream.toByteArray();
		input = new Input(outBytes);
		HasTransients objectWithTransients3 = ser.read(kryo, input, HasTransients.class);
		assertTrue("Objects should be different if write does not include transient fields",
			!objectWithTransients3.equals(objectWithTransients1));
		assertEquals("transient fields should be null", objectWithTransients3.transientField1, null);

		ser.setSerializeTransient(true);

		outputStream = new ByteArrayOutputStream();
		output = new Output(outputStream);
		ser.write(kryo, output, objectWithTransients1);
		output.flush();

		outBytes = outputStream.toByteArray();
		input = new Input(outBytes);
		HasTransients objectWithTransients2 = ser.read(kryo, input, HasTransients.class);
		assertTrue("Objects should be equal if write includes transient fields",
			objectWithTransients2.equals(objectWithTransients1));
	}

	@Test
	public void testCorrectlyAnnotatedFields () {
		kryo.register(int[].class);
		kryo.register(long[].class);
		kryo.register(HashMap.class);
		kryo.register(ArrayList.class);
		kryo.register(AnnotatedFields.class);
		AnnotatedFields obj1 = new AnnotatedFields();
		obj1.map = new HashMap<String, int[]>();
		obj1.map.put("key1", new int[] {1, 2, 3});
		obj1.map.put("key2", new int[] {3, 4, 5});
		obj1.map.put("key3", null);

		obj1.collection = new ArrayList<long[]>();
		obj1.collection.add(new long[] {1, 2, 3});

		support.roundTrip(31, 73, obj1);
	}

	@Test
	public void testWronglyAnnotatedCollectionFields () {
		try {
			kryo.register(WronglyAnnotatedCollectionFields.class);
			WronglyAnnotatedCollectionFields obj1 = new WronglyAnnotatedCollectionFields();
			support.roundTrip(31, 73, obj1);
		} catch (RuntimeException e) {
			Throwable cause = e.getCause().getCause();
			assertTrue("Exception should complain about a field not implementing java.util.Collection",
				cause.getMessage().contains("be used only with fields implementing java.util.Collection"));
			return;
		}

		assertFalse("Exception was expected", true);
	}

	@Test
	public void testWronglyAnnotatedMapFields () {
		try {
			kryo.register(WronglyAnnotatedMapFields.class);
			WronglyAnnotatedMapFields obj1 = new WronglyAnnotatedMapFields();
			support.roundTrip(31, 73, obj1);
		} catch (RuntimeException e) {
			Throwable cause = e.getCause().getCause();
			assertTrue("Exception should complain about a field not implementing java.util.Map ",
				cause.getMessage().contains("be used only with fields implementing java.util.Map"));
			return;
		}

		assertFalse("Exception was expected", true);
	}

	@Test
	public void testMultipleTimesAnnotatedMapFields () {
		try {
			kryo.register(MultipleTimesAnnotatedCollectionFields.class);
			MultipleTimesAnnotatedCollectionFields obj1 = new MultipleTimesAnnotatedCollectionFields();
			support.roundTrip(31, 73, obj1);
		} catch (RuntimeException e) {
			Throwable cause = e.getCause().getCause();
			assertTrue("Exception should complain about a field that has a serializer already",
				cause.getMessage().contains("already"));
			return;
		}

		assertFalse("Exception was expected", true);
	}

	static public class DefaultTypes {
		// Primitives.
		public boolean booleanField;
		public byte byteField;
		public char charField;
		public short shortField;
		public int intField;
		public long longField;
		public float floatField;
		public double doubleField;
		// Primitive wrappers.
		public Boolean BooleanField;
		public Byte ByteField;
		public Character CharacterField;
		public Short ShortField;
		public Integer IntegerField;
		public Long LongField;
		public Float FloatField;
		public Double DoubleField;
		// Other.
		public String StringField;
		public byte[] byteArrayField;

		DefaultTypes child;
		HasStringField hasStringField;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			DefaultTypes other = (DefaultTypes)obj;
			if (BooleanField == null) {
				if (other.BooleanField != null) return false;
			} else if (!BooleanField.equals(other.BooleanField)) return false;
			if (ByteField == null) {
				if (other.ByteField != null) return false;
			} else if (!ByteField.equals(other.ByteField)) return false;
			if (CharacterField == null) {
				if (other.CharacterField != null) return false;
			} else if (!CharacterField.equals(other.CharacterField)) return false;
			if (DoubleField == null) {
				if (other.DoubleField != null) return false;
			} else if (!DoubleField.equals(other.DoubleField)) return false;
			if (FloatField == null) {
				if (other.FloatField != null) return false;
			} else if (!FloatField.equals(other.FloatField)) return false;
			if (IntegerField == null) {
				if (other.IntegerField != null) return false;
			} else if (!IntegerField.equals(other.IntegerField)) return false;
			if (LongField == null) {
				if (other.LongField != null) return false;
			} else if (!LongField.equals(other.LongField)) return false;
			if (ShortField == null) {
				if (other.ShortField != null) return false;
			} else if (!ShortField.equals(other.ShortField)) return false;
			if (StringField == null) {
				if (other.StringField != null) return false;
			} else if (!StringField.equals(other.StringField)) return false;
			if (booleanField != other.booleanField) return false;

			Object list1 = arrayToList(byteArrayField);
			Object list2 = arrayToList(other.byteArrayField);
			if (list1 != list2) {
				if (list1 == null || list2 == null) return false;
				if (!list1.equals(list2)) return false;
			}

			if (child != other.child) {
				if (child == null || other.child == null) return false;
				if (child != this && !child.equals(other.child)) return false;
			}

			if (byteField != other.byteField) return false;
			if (charField != other.charField) return false;
			if (Double.doubleToLongBits(doubleField) != Double.doubleToLongBits(other.doubleField)) return false;
			if (Float.floatToIntBits(floatField) != Float.floatToIntBits(other.floatField)) return false;
			if (intField != other.intField) return false;
			if (longField != other.longField) return false;
			if (shortField != other.shortField) return false;
			return true;
		}
	}

	static public final class A {
		public int value;
		public B b;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			A other = (A)obj;
			if (b == null) {
				if (other.b != null) return false;
			} else if (!b.equals(other.b)) return false;
			if (value != other.value) return false;
			return true;
		}
	}

	static public final class B {
		public int value;
		public A a;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			B other = (B)obj;
			if (a == null) {
				if (other.a != null) return false;
			} else if (!a.equals(other.a)) return false;
			if (value != other.value) return false;
			return true;
		}
	}

	static public final class C {
		public A a;
		public D d;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			C other = (C)obj;
			if (a == null) {
				if (other.a != null) return false;
			} else if (!a.equals(other.a)) return false;
			if (d == null) {
				if (other.d != null) return false;
			} else if (!d.equals(other.d)) return false;
			return true;
		}
	}

	static public final class D {
		public E e;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			D other = (D)obj;
			if (e == null) {
				if (other.e != null) return false;
			} else if (!e.equals(other.e)) return false;
			return true;
		}
	}

	static public final class E {
		public F f;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			E other = (E)obj;
			if (f == null) {
				if (other.f != null) return false;
			} else if (!f.equals(other.f)) return false;
			return true;
		}
	}

	static public final class F {
		public int value;
		public final int finalValue = 12;
		public A a;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			F other = (F)obj;
			if (finalValue != other.finalValue) return false;
			if (value != other.value) return false;
			return true;
		}
	}

	static public class SimpleNoDefaultConstructor {
		int constructorValue;

		public SimpleNoDefaultConstructor (int constructorValue) {
			this.constructorValue = constructorValue;
		}

		public int getConstructorValue () {
			return constructorValue;
		}

		public int hashCode () {
			final int prime = 31;
			int result = 1;
			result = prime * result + constructorValue;
			return result;
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			SimpleNoDefaultConstructor other = (SimpleNoDefaultConstructor)obj;
			if (constructorValue != other.constructorValue) return false;
			return true;
		}
	}

	static public class HasTransients {
		public transient String transientField1;
		public int anotherField2;
		public String anotherField3;

		public HasTransients () {
		}

		public int hashCode () {
			final int prime = 31;
			int result = 1;
			result = prime * result + anotherField2;
			result = prime * result + ((anotherField3 == null) ? 0 : anotherField3.hashCode());
			result = prime * result + ((transientField1 == null) ? 0 : transientField1.hashCode());
			return result;
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			HasTransients other = (HasTransients)obj;
			if (anotherField2 != other.anotherField2) return false;
			if (anotherField3 == null) {
				if (other.anotherField3 != null) return false;
			} else if (!anotherField3.equals(other.anotherField3)) return false;
			if (transientField1 == null) {
				if (other.transientField1 != null) return false;
			} else if (!transientField1.equals(other.transientField1)) return false;
			return true;
		}
	}

	static public class ComplexNoDefaultConstructor {
		public transient String name;
		public int anotherField1;
		public String anotherField2;

		public ComplexNoDefaultConstructor (String name) {
			this.name = name;
		}

		public int hashCode () {
			final int prime = 31;
			int result = 1;
			result = prime * result + anotherField1;
			result = prime * result + ((anotherField2 == null) ? 0 : anotherField2.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ComplexNoDefaultConstructor other = (ComplexNoDefaultConstructor)obj;
			if (anotherField1 != other.anotherField1) return false;
			if (anotherField2 == null) {
				if (other.anotherField2 != null) return false;
			} else if (!anotherField2.equals(other.anotherField2)) return false;
			if (name == null) {
				if (other.name != null) return false;
			} else if (!name.equals(other.name)) return false;
			return true;
		}
	}

	static public class HasNonNull {
		@NotNull public String nonNullText;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			HasNonNull other = (HasNonNull)obj;
			if (nonNullText == null) {
				if (other.nonNullText != null) return false;
			} else if (!nonNullText.equals(other.nonNullText)) return false;
			return true;
		}
	}

	static public class HasStringField {
		public String text;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			HasStringField other = (HasStringField)obj;
			if (text == null) {
				if (other.text != null) return false;
			} else if (!text.equals(other.text)) return false;
			return true;
		}
	}

	static public class HasOptionalAnnotation {
		@Optional("smurf") int moo;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			HasOptionalAnnotation other = (HasOptionalAnnotation)obj;
			if (moo != other.moo) return false;
			return true;
		}
	}

	@DefaultSerializer(HasDefaultSerializerAnnotationSerializer.class)
	static public class HasDefaultSerializerAnnotation {
		long time;

		public HasDefaultSerializerAnnotation (long time) {
			this.time = time;
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			HasDefaultSerializerAnnotation other = (HasDefaultSerializerAnnotation)obj;
			if (time != other.time) return false;
			return true;
		}
	}

	static public class HasDefaultSerializerAnnotationSerializer extends Serializer<HasDefaultSerializerAnnotation> {
		public void write (Kryo kryo, Output output, HasDefaultSerializerAnnotation object) {
			output.writeLong(object.time, true);
		}

		public HasDefaultSerializerAnnotation read (Kryo kryo, Input input, Class type) {
			return new HasDefaultSerializerAnnotation(input.readLong(true));
		}

		public HasDefaultSerializerAnnotation copy (Kryo kryo, HasDefaultSerializerAnnotation original) {
			return new HasDefaultSerializerAnnotation(original.time);
		}
	}

	static public class HasArgumentConstructor {
		public String moo;

		public HasArgumentConstructor (String moo) {
			this.moo = moo;
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			HasArgumentConstructor other = (HasArgumentConstructor)obj;
			if (moo == null) {
				if (other.moo != null) return false;
			} else if (!moo.equals(other.moo)) return false;
			return true;
		}
	}

	static public class HasPrivateConstructor extends HasArgumentConstructor {
		static int invocations;

		private HasPrivateConstructor () {
			super("cow");
			HasPrivateConstructor.invocations++;
		}
	}

	static public class HasGenerics {
		ArrayList<Integer> list1;
		List<List<?>> list2 = new ArrayList<List<?>>();
		List<?> list3 = new ArrayList();
		ArrayList<?> list4 = new ArrayList();
		ArrayList<String> list5;
		HashMap<String, ArrayList<Integer>> map1;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			HasGenerics other = (HasGenerics)obj;
			if (list1 == null) {
				if (other.list1 != null) return false;
			} else if (!list1.equals(other.list1)) return false;
			if (list2 == null) {
				if (other.list2 != null) return false;
			} else if (!list2.equals(other.list2)) return false;
			if (list3 == null) {
				if (other.list3 != null) return false;
			} else if (!list3.equals(other.list3)) return false;
			if (list4 == null) {
				if (other.list4 != null) return false;
			} else if (!list4.equals(other.list4)) return false;
			if (list5 == null) {
				if (other.list5 != null) return false;
			} else if (!list5.equals(other.list5)) return false;
			if (map1 == null) {
				if (other.map1 != null) return false;
			} else if (!map1.equals(other.map1)) return false;
			return true;
		}
	}

	static public class MultipleTimesAnnotatedCollectionFields {
		// This annotation should result in an exception, because
		// it is applied to a non-collection field
		@BindCollection(elementSerializer = LongArraySerializer.class, elementClass = long[].class, elementsCanBeNull = false) @Bind(CollectionSerializer.class) Collection collection;
	}

	static public class WronglyAnnotatedCollectionFields {
		// This annotation should result in an exception, because
		// it is applied to a non-collection field
		@BindCollection(elementSerializer = LongArraySerializer.class, elementClass = long[].class, elementsCanBeNull = false) int collection;
	}

	static public class WronglyAnnotatedMapFields {
		// This annotation should result in an exception, because
		// it is applied to a non-map field
		@BindMap(valueSerializer = IntArraySerializer.class, keySerializer = StringSerializer.class, valueClass = int[].class, keyClass = String.class, keysCanBeNull = false) Object map;
	}

	static public class AnnotatedFields {
		@Bind(StringSerializer.class) Object stringField;

		@BindMap(valueSerializer = IntArraySerializer.class, keySerializer = StringSerializer.class, valueClass = int[].class, keyClass = String.class, keysCanBeNull = false) Map map;

		@BindCollection(elementSerializer = LongArraySerializer.class, elementClass = long[].class, elementsCanBeNull = false) Collection collection;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			AnnotatedFields other = (AnnotatedFields)obj;
			if (map == null) {
				if (other.map != null) return false;
			} else {
				if (other.map == null) return false;
				if (map.size() != other.map.size()) return false;
				for (Object e : map.entrySet()) {
					Map.Entry entry = (Map.Entry)e;
					if (!other.map.containsKey(entry.getKey())) return false;
					Object otherValue = other.map.get(entry.getKey());
					if (entry.getValue() == null && otherValue != null) return false;
					if (!Arrays.equals((int[])entry.getValue(), (int[])otherValue)) return false;
				}
			}
			if (collection == null) {
				if (other.collection != null) return false;
			} else {
				if (other.collection == null) return false;
				if (collection.size() != other.collection.size()) return false;
				Iterator it1 = collection.iterator();
				Iterator it2 = other.collection.iterator();
				while (it1.hasNext()) {
					Object e1 = it1.next();
					Object e2 = it2.next();
					if (!Arrays.equals((long[])e1, (long[])e2)) return false;
				}
			}
			return true;
		}
	}

	static public class IsGeneric<T> {
		T item;
		private int y;
		private int z;

		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (!(o instanceof IsGeneric)) return false;

			IsGeneric isGeneric = (IsGeneric)o;

			if (z != isGeneric.z) return false;
			if (item != null ? !item.equals(isGeneric.item) : isGeneric.item != null) return false;

			return true;
		}
	}
}
