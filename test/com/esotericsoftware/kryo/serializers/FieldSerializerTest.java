/* Copyright (c) 2008-2025, Nathan Sweet
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

package com.esotericsoftware.kryo.serializers;

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.SerializerFactory.FieldSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer.BindCollection;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.IntArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.LongArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer.Bind;
import com.esotericsoftware.kryo.serializers.FieldSerializer.NotNull;
import com.esotericsoftware.kryo.serializers.FieldSerializer.Optional;
import com.esotericsoftware.kryo.serializers.MapSerializer.BindMap;
import com.esotericsoftware.kryo.util.Util;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;

/** @author Nathan Sweet */
@SuppressWarnings("synthetic-access")
class FieldSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@Test
	void testDefaultTypes () {
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
		roundTrip(78, test);

		kryo.register(HasStringField.class);
		test.hasStringField = new HasStringField();
		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(DefaultTypes.class);
		serializer.getField("hasStringField").setCanBeNull(false);
		roundTrip(79, test);
		serializer.getFieldSerializerConfig().setFixedFieldTypes(true);
		serializer.updateFields();
		serializer.getField("hasStringField").setCanBeNull(false);
		roundTrip(78, test);
	}

	@Test
	void testFieldRemoval () {
		kryo.register(DefaultTypes.class);
		kryo.register(byte[].class);
		kryo.register(HasStringField.class);

		HasStringField hasStringField = new HasStringField();
		hasStringField.text = "moo";
		roundTrip(4, hasStringField);

		DefaultTypes test = new DefaultTypes();
		test.intField = 12;
		test.StringField = "value";
		test.CharacterField = 'X';
		test.child = new DefaultTypes();
		roundTrip(71, test);

		supportsCopy = false;

		test.StringField = null;
		roundTrip(67, test);

		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(DefaultTypes.class);
		serializer.removeField("LongField");
		serializer.removeField("floatField");
		serializer.removeField("FloatField");
		roundTrip(55, test);

		supportsCopy = true;
	}

	@Test
	void testFieldRemovalOnGenerics () {
		kryo.register(IsGeneric.class);
		kryo.register(DefaultTypes.class);
		kryo.register(byte[].class);

		FieldSerializer serializer = new FieldSerializer(kryo, IsGeneric.class);
		serializer.removeField("y");
		kryo.register(IsGeneric.class, serializer);

		IsGeneric<IsGeneric<DefaultTypes>> test = new IsGeneric();
		test.item = new IsGeneric();

		try {
			roundTrip(5, test);
		} catch (KryoException ex) {
			ex.printStackTrace();
			fail("Couldn't serialize generic with a removed field.");
		}
	}

	@Test
	void testOptionalRegistration () {
		kryo.setRegistrationRequired(false);
		DefaultTypes test = new DefaultTypes();
		test.intField = 12;
		test.StringField = "value";
		test.CharacterField = 'X';
		test.hasStringField = new HasStringField();
		test.child = new DefaultTypes();
		test.child.hasStringField = new HasStringField();
		roundTrip(219, test);
		test.hasStringField = null;
		roundTrip(217, test);

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
		kryo.setRegistrationRequired(false);
		kryo.setReferences(true);
		roundTrip(152, test);

		C c = new C();
		c.a = new A();
		c.a.value = 123;
		c.a.b = new B();
		c.a.b.value = 456;
		c.d = new D();
		c.d.e = new E();
		c.d.e.f = new F();
		roundTrip(75, c);
	}

	@Test
	void testReferences () {
		C c = new C();
		c.a = new A();
		c.a.value = 123;
		c.a.b = new B();
		c.a.b.value = 456;
		c.d = new D();
		c.d.e = new E();
		c.d.e.f = new F();
		c.d.e.f.d = c.d; // Circular.

		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setReferences(true);
		roundTrip(75, c);
		C c2 = (C)object2;
		assertSame(c2.d, c2.d.e.f.d);

		// Test reset clears unregistered class names.
		roundTrip(75, c);
		c2 = (C)object2;
		assertSame(c2.d, c2.d.e.f.d);

		kryo = new Kryo();
		kryo.register(A.class);
		kryo.register(B.class);
		kryo.register(C.class);
		kryo.register(D.class);
		kryo.register(E.class);
		kryo.register(F.class);
		kryo.setReferences(true);
		roundTrip(15, c);
		c2 = (C)object2;
		assertSame(c2.d, c2.d.e.f.d);
	}

	@Test
	void testRegistrationOrder () {
		A a = new A();
		a.value = 100;
		a.b = new B();
		a.b.value = 200;
		a.b.a = new A();
		a.b.a.value = 300;

		kryo.register(A.class);
		kryo.register(B.class);
		roundTrip(10, a);

		kryo = new Kryo();
		kryo.register(B.class);
		kryo.register(A.class);
		roundTrip(10, a);
	}

	@Test
	void testExceptionTrace () {
		C c = new C();
		c.a = new A();
		c.a.value = 123;
		c.a.b = new B();
		c.a.b.value = 456;
		c.d = new D();
		c.d.e = new E();
		c.d.e.f = new F();

		Kryo kryoWithoutF = new Kryo();
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

		output.reset();
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
	void testNoDefaultConstructor () {
		kryo.register(SimpleNoDefaultConstructor.class, new Serializer<SimpleNoDefaultConstructor>() {
			public SimpleNoDefaultConstructor read (Kryo kryo, Input input, Class<? extends SimpleNoDefaultConstructor> type) {
				return new SimpleNoDefaultConstructor(input.readVarInt(true));
			}

			public void write (Kryo kryo, Output output, SimpleNoDefaultConstructor object) {
				output.writeVarInt(object.constructorValue, true);
			}

			public SimpleNoDefaultConstructor copy (Kryo kryo, SimpleNoDefaultConstructor original) {
				return new SimpleNoDefaultConstructor(original.constructorValue);
			}
		});
		SimpleNoDefaultConstructor object1 = new SimpleNoDefaultConstructor(2);
		roundTrip(2, object1);

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
		roundTrip(35, object2);
	}

	@Test
	void testNonNull () {
		kryo.register(HasNonNull.class);
		HasNonNull nonNullValue = new HasNonNull();
		nonNullValue.nonNullText = "moo";
		roundTrip(4, nonNullValue);
	}

	@Test
	void testDefaultSerializerAnnotation () {
		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setReferences(true);
		roundTrip(92, new HasDefaultSerializerAnnotation(123));
	}

	@Test
	void testOptionalAnnotation () {
		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setReferences(true);
		roundTrip(82, new HasOptionalAnnotation());
		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setReferences(true);
		kryo.getContext().put("smurf", null);
		roundTrip(83, new HasOptionalAnnotation());
	}

	@Test
	void testCyclicGrgaph () {
		kryo = new Kryo();
		kryo.register(DefaultTypes.class);
		kryo.register(byte[].class);
		kryo.setReferences(true);
		DefaultTypes test = new DefaultTypes();
		test.child = test;
		roundTrip(35, test);
	}

	@Test
	void testInstantiatorStrategy () {
		kryo.register(HasArgumentConstructor.class);
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		HasArgumentConstructor test = new HasArgumentConstructor("cow");
		roundTrip(4, test);

		kryo.register(HasPrivateConstructor.class);
		test = new HasPrivateConstructor();
		roundTrip(4, test);
	}

	/** This test uses StdInstantiatorStrategy and therefore requires a no-arg constructor. **/
	@Test
	void testDefaultInstantiatorStrategy () {
		kryo.register(HasArgumentConstructor.class);
		HasArgumentConstructor test = new HasPrivateConstructor();
		HasPrivateConstructor.invocations = 0;

		kryo.register(HasPrivateConstructor.class);
		roundTrip(4, test);
		assertEquals(Util.isUnsafeAvailable() ? 20 : 10, HasPrivateConstructor.invocations, "Wrong number of constructor invocations");
	}

	/** This test uses StdInstantiatorStrategy and should bypass invocation of no-arg constructor, even if it is provided. **/
	@Test
	void testStdInstantiatorStrategy () {
		kryo.register(HasArgumentConstructor.class);
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		HasArgumentConstructor test = new HasPrivateConstructor();
		HasPrivateConstructor.invocations = 0;

		kryo.register(HasPrivateConstructor.class);
		roundTrip(4, test);
		assertEquals(0, HasPrivateConstructor.invocations,
			"Default constructor should not be invoked with StdInstantiatorStrategy strategy");
	}

	@Test
	void testGenericTypes () {
		kryo.setReferences(true);
		kryo.register(HasGenerics.class);
		kryo.register(ListContainer.class);
		kryo.register(ArrayList.class);
		kryo.register(ArrayList[].class);
		kryo.register(HashMap.class);

		HasGenerics<Integer> test = new HasGenerics();
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
		test.container = new ListContainer();
		test.container.list = new ArrayList();
		test.container.list.add("one");
		test.container.list.add("two");
		test.container.list.add("three");
		test.container.list.add("four");
		test.container.list.add("five");
		roundTrip(66, test);

		ArrayList[] al = new ArrayList[1];
		al[0] = new ArrayList(Arrays.asList(new String[] {"A", "B", "S"}));
		roundTrip(17, al);
	}

	@Test
	void testRegistration () {
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
		roundTrip(75, test);
	}

	@Test
	void testTransients () {
		kryo.register(HasTransients.class);
		HasTransients objectWithTransients1 = new HasTransients();
		objectWithTransients1.transientField1 = "Test";
		objectWithTransients1.anotherField2 = 5;
		objectWithTransients1.anotherField3 = "Field2";

		FieldSerializer ser = (FieldSerializer)kryo.getSerializer(HasTransients.class);
		ser.getFieldSerializerConfig().setCopyTransient(false);
		ser.updateFields();

		HasTransients objectWithTransients3 = kryo.copy(objectWithTransients1);
		assertTrue(!objectWithTransients3.equals(objectWithTransients1),
			"Objects should be different if copy does not include transient fields");
		assertNull(objectWithTransients3.transientField1, "transient fields should be null");

		ser.getFieldSerializerConfig().setCopyTransient(true);
		ser.updateFields();
		HasTransients objectWithTransients2 = kryo.copy(objectWithTransients1);
		assertEquals(objectWithTransients2, objectWithTransients1, "Objects should be equal if copy includes transient fields");
	}

	@Test
	void testTransientsUsingGlobalConfig () {
		FieldSerializerFactory factory = new FieldSerializerFactory();
		factory.getConfig().setCopyTransient(false);
		kryo.setDefaultSerializer(factory);
		kryo.register(HasTransients.class);
		HasTransients objectWithTransients1 = new HasTransients();
		objectWithTransients1.transientField1 = "Test";
		objectWithTransients1.anotherField2 = 5;
		objectWithTransients1.anotherField3 = "Field2";

		FieldSerializer ser = (FieldSerializer)kryo.getSerializer(HasTransients.class);
		HasTransients objectWithTransients3 = kryo.copy(objectWithTransients1);
		assertTrue(!objectWithTransients3.equals(objectWithTransients1),
			"Objects should be different if copy does not include transient fields");
		assertNull(objectWithTransients3.transientField1, "transient fields should be null");

		ser.getFieldSerializerConfig().setCopyTransient(true);
		ser.updateFields();
		HasTransients objectWithTransients2 = kryo.copy(objectWithTransients1);
		assertEquals(objectWithTransients2, objectWithTransients1, "Objects should be equal if copy includes transient fields");
	}

	@Test
	void testSerializeTransients () {
		kryo.register(HasTransients.class);
		HasTransients objectWithTransients1 = new HasTransients();
		objectWithTransients1.transientField1 = "Test";
		objectWithTransients1.anotherField2 = 5;
		objectWithTransients1.anotherField3 = "Field2";

		ByteArrayOutputStream outputStream;
		Output output;
		Input input;
		byte[] outBytes;

		FieldSerializer<HasTransients> ser = (FieldSerializer)kryo.getSerializer(HasTransients.class);
		ser.getFieldSerializerConfig().setSerializeTransient(false);
		ser.updateFields();

		outputStream = new ByteArrayOutputStream();
		output = new Output(outputStream);
		ser.write(kryo, output, objectWithTransients1);
		output.flush();

		outBytes = outputStream.toByteArray();
		input = new Input(outBytes);
		HasTransients objectWithTransients3 = ser.read(kryo, input, HasTransients.class);
		assertTrue(!objectWithTransients3.equals(objectWithTransients1),
				"Objects should be different if write does not include transient fields");
		assertNull(objectWithTransients3.transientField1, "transient fields should be null");

		ser.getFieldSerializerConfig().setSerializeTransient(true);
		ser.updateFields();

		outputStream = new ByteArrayOutputStream();
		output = new Output(outputStream);
		ser.write(kryo, output, objectWithTransients1);
		output.flush();

		outBytes = outputStream.toByteArray();
		input = new Input(outBytes);
		HasTransients objectWithTransients2 = ser.read(kryo, input, HasTransients.class);
		assertEquals(objectWithTransients2, objectWithTransients1, "Objects should be equal if write includes transient fields");
	}

	@Test
	void testSerializeTransientsUsingGlobalConfig () {
		FieldSerializerFactory factory = new FieldSerializerFactory();
		factory.getConfig().setSerializeTransient(false);
		kryo.setDefaultSerializer(factory);
		kryo.register(HasTransients.class);
		HasTransients objectWithTransients1 = new HasTransients();
		objectWithTransients1.transientField1 = "Test";
		objectWithTransients1.anotherField2 = 5;
		objectWithTransients1.anotherField3 = "Field2";

		ByteArrayOutputStream outputStream;
		Output output;
		Input input;
		byte[] outBytes;

		FieldSerializer<HasTransients> ser = (FieldSerializer)kryo.getSerializer(HasTransients.class);
		outputStream = new ByteArrayOutputStream();
		output = new Output(outputStream);
		ser.write(kryo, output, objectWithTransients1);
		output.flush();

		outBytes = outputStream.toByteArray();
		input = new Input(outBytes);
		HasTransients objectWithTransients3 = ser.read(kryo, input, HasTransients.class);
		assertTrue(!objectWithTransients3.equals(objectWithTransients1),
				"Objects should be different if write does not include transient fields");
		assertNull(objectWithTransients3.transientField1, "transient fields should be null");

		ser.getFieldSerializerConfig().setSerializeTransient(true);
		ser.updateFields();

		outputStream = new ByteArrayOutputStream();
		output = new Output(outputStream);
		ser.write(kryo, output, objectWithTransients1);
		output.flush();

		outBytes = outputStream.toByteArray();
		input = new Input(outBytes);
		HasTransients objectWithTransients2 = ser.read(kryo, input, HasTransients.class);
		assertEquals(objectWithTransients2, objectWithTransients1, "Objects should be equal if write includes transient fields");
	}

	@Test
	void testCorrectlyAnnotatedFields () {
		kryo.register(int[].class);
		kryo.register(long[].class);
		kryo.register(HashMap.class);
		kryo.register(ArrayList.class);
		kryo.register(AnnotatedFields.class);
		kryo.register(byte[].class);
		kryo.register(AnnotatedFields.HasFields.class);
		AnnotatedFields obj1 = new AnnotatedFields();
		obj1.stringField = "meow";

		obj1.map = new HashMap();
		obj1.map.put("key1", new int[] {1, 2, 3});
		obj1.map.put("key2", new int[] {3, 4, 5});
		obj1.map.put("key3", null);

		obj1.collection = new ArrayList();
		obj1.collection.add(new long[] {1, 2, 3});

		roundTrip(36, obj1);

		obj1.listOfHasFields = new ArrayList();
		AnnotatedFields.HasFields hasFields = new AnnotatedFields.HasFields();
		hasFields.number = 42;
		hasFields.text = "foo";
		obj1.listOfHasFields.add(hasFields);

		roundTrip(41, obj1);
	}

	@Test
	void testWronglyAnnotatedCollectionFields () {
		try {
			kryo.register(WronglyAnnotatedCollectionFields.class);
			WronglyAnnotatedCollectionFields obj1 = new WronglyAnnotatedCollectionFields();
			roundTrip(31, obj1);
		} catch (RuntimeException ex) {
			assertTrue(ex.getMessage().contains("only be used with a field implementing Collection"),
					"Exception should complain about a field not implementing java.util.Collection");
			return;
		}

		fail("Exception was expected");
	}

	@Test
	void testWronglyAnnotatedMapFields () {
		try {
			kryo.register(WronglyAnnotatedMapFields.class);
			WronglyAnnotatedMapFields obj1 = new WronglyAnnotatedMapFields();
			roundTrip(31, obj1);
		} catch (RuntimeException ex) {
			assertTrue(ex.getMessage().contains("can only be used with a field implementing Map"),
					"Exception should complain about a field not implementing java.util.Map ");
			return;
		}

		fail("Exception was expected");
	}

	@Test
	void testMultipleTimesAnnotatedMapFields () {
		try {
			kryo.register(MultipleTimesAnnotatedCollectionFields.class);
			MultipleTimesAnnotatedCollectionFields obj1 = new MultipleTimesAnnotatedCollectionFields();
			roundTrip(31, obj1);
		} catch (RuntimeException ex) {
			assertTrue(ex.getMessage().contains("already has a serializer"),
					"Exception should complain about a field that has a serializer already");
			return;
		}

		fail("Exception was expected");
	}

	@Test
	void testDeep () {
		kryo.register(Deep.class);
		Deep root = new Deep();
		Deep current = root;
		for (int i = 1; i <= 500; i++) {
			current.i = i;
			current.deep = new Deep();
			current = current.deep;
		}
		roundTrip(1440, root);
	}

	@Test
	void testCircularReference () {
		kryo.register(CircularReference.class);
		kryo.register(CircularReference.Inner.class);

		CircularReference instance = new CircularReference();
		try {
			roundTrip(1, instance);
		} catch (KryoException ex) {
			assertTrue(ex.getMessage().contains("A StackOverflow occurred."));
			return;
		}

		fail("Exception was expected");
	}

	public static class DefaultTypes {
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

	public static final class A {
		public int value;
		@Bind(valueClass = B.class, serializerFactory = FieldSerializerFactory.class) public B b;

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

	public static final class B {
		public int value;
		@Bind(valueClass = A.class) public A a;

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

	public static final class C {
		public A a;
		@Bind(serializer = FieldSerializer.class, valueClass = D.class) public D d;

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

	public static final class D {
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

	public static final class E {
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

	public static final class F {
		public int value;
		public final int finalValue = 12;
		public D d;

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

	public static class SimpleNoDefaultConstructor {
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

	public static class HasTransients {
		public transient String transientField1;
		public int anotherField2;
		public String anotherField3;

		public HasTransients () {
		}

		public int hashCode () {
			final int prime = 31;
			int result = 1;
			result = prime * result + anotherField2;
			result = prime * result + (anotherField3 == null ? 0 : anotherField3.hashCode());
			result = prime * result + (transientField1 == null ? 0 : transientField1.hashCode());
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

	public static class ComplexNoDefaultConstructor {
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
			result = prime * result + (anotherField2 == null ? 0 : anotherField2.hashCode());
			result = prime * result + (name == null ? 0 : name.hashCode());
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

	@DefaultSerializer(FieldSerializer.class)
	public static class HasNonNull {
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

	@DefaultSerializer(serializerFactory = FieldSerializerFactory.class)
	public static class HasStringField {
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

	public static class HasOptionalAnnotation {
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
	public static class HasDefaultSerializerAnnotation {
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

	public static class HasDefaultSerializerAnnotationSerializer extends Serializer<HasDefaultSerializerAnnotation> {
		public void write (Kryo kryo, Output output, HasDefaultSerializerAnnotation object) {
			output.writeVarLong(object.time, true);
		}

		public HasDefaultSerializerAnnotation read (Kryo kryo, Input input, Class<? extends HasDefaultSerializerAnnotation> type) {
			return new HasDefaultSerializerAnnotation(input.readVarLong(true));
		}

		public HasDefaultSerializerAnnotation copy (Kryo kryo, HasDefaultSerializerAnnotation original) {
			return new HasDefaultSerializerAnnotation(original.time);
		}
	}

	public static class HasArgumentConstructor {
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

	public static class HasPrivateConstructor extends HasArgumentConstructor {
		static int invocations;

		private HasPrivateConstructor () {
			super("cow");
			HasPrivateConstructor.invocations++;
		}
	}

	public static class HasGenerics<T> {
		public ArrayList<T> list1;
		private List<List> list2 = new ArrayList();
		public List list3 = new ArrayList();
		ArrayList list4 = new ArrayList();
		public ListContainer<String> container;
		protected HashMap<String, ArrayList<Integer>> map1;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			HasGenerics other = (HasGenerics)obj;
			if (container == null) {
				if (other.container != null) return false;
			} else if (!container.equals(other.container)) return false;
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
			if (map1 == null) {
				if (other.map1 != null) return false;
			} else if (!map1.equals(other.map1)) return false;
			return true;
		}
	}

	public static class ListContainer<T> {
		public ArrayList<T> list;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ListContainer other = (ListContainer)obj;
			if (list == null) {
				if (other.list != null) return false;
			} else if (!list.equals(other.list)) return false;
			return true;
		}
	}

	public static class MultipleTimesAnnotatedCollectionFields {
		// This annotation should result in an exception, because
		// it is applied to a non-collection field
		@BindCollection(elementSerializer = LongArraySerializer.class, //
			elementClass = long[].class, //
			elementsCanBeNull = false) //
		@Bind(serializer = CollectionSerializer.class) //
		Collection collection;
	}

	public static class WronglyAnnotatedCollectionFields {
		// This annotation should result in an exception, because
		// it is applied to a non-collection field
		@BindCollection(elementSerializer = LongArraySerializer.class, //
			elementClass = long[].class, //
			elementsCanBeNull = false) //
		int collection;
	}

	public static class WronglyAnnotatedMapFields {
		// This annotation should result in an exception, because
		// it is applied to a non-map field
		@BindMap(valueSerializer = IntArraySerializer.class, //
			keySerializer = StringSerializer.class, //
			valueClass = int[].class, //
			keyClass = String.class, //
			keysCanBeNull = false) //
		Object map;
	}

	public static class AnnotatedFields {
		public static class HasFields {
			public int number;
			public String text;

			public boolean equals (Object obj) {
				if (this == obj) return true;
				if (obj == null) return false;
				if (getClass() != obj.getClass()) return false;
				HasFields other = (HasFields)obj;
				if (number != other.number) return false;
				if (!Objects.equals(text, other.text)) return false;
				return true;
			}
		}

		@Bind(serializer = StringSerializer.class) Object stringField;

		@BindMap(valueSerializer = IntArraySerializer.class, //
			keySerializer = StringSerializer.class, //
			valueClass = int[].class, //
			keyClass = String.class, //
			keysCanBeNull = false) //
		Map map;

		@BindCollection(elementSerializer = LongArraySerializer.class, //
			elementClass = long[].class, //
			elementsCanBeNull = false) //
		Collection collection;

		@BindCollection(elementSerializer = FieldSerializer.class, //
			elementClass = HasFields.class, //
			elementsCanBeNull = false) //
		List listOfHasFields;

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
			if (!Objects.equals(listOfHasFields, other.listOfHasFields)) return false;
			return true;
		}
	}

	public static class IsGeneric<T> {
		T item;
		private int y;
		private int z;

		public boolean equals (Object o) {
			if (this == o) return true;
			if (!(o instanceof IsGeneric)) return false;

			IsGeneric isGeneric = (IsGeneric)o;

			if (z != isGeneric.z) return false;
			if (item != null ? !item.equals(isGeneric.item) : isGeneric.item != null) return false;

			return true;
		}
	}

	public static class Deep {
		public int i;
		public Deep deep;

		public int hashCode () {
			final int prime = 31;
			int result = 1;
			result = prime * result + i;
			return result;
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Deep other = (Deep)obj;
			if (i != other.i) return false;
			return true;
		}
	}

	static class CircularReference {
		Inner b = new Inner(this);

		static class Inner {
			CircularReference a;
	
			public Inner(CircularReference a) {
				this.a = a;
			}
		}
	}

}
