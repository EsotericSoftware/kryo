
package com.esotericsoftware.kryo.serialize;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import junit.framework.TestCase;

import org.junit.Assert;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.Serializer;

// TODO - Write tests for all serializers.
// TODO - Break this monolithic test into smaller tests.

public class SerializerTest extends TestCase {
	private ByteBuffer buffer = ByteBuffer.allocateDirect(500);

	private int intValues[] = {
	//
		0, 1, 2, 3, //
		-1, -2, -3, //
		32, -32, //
		127, 128, 129, //
		-125, -126, -127, -128, //
		252, 253, 254, 255, 256, //
		-252, -253, -254, -255, -256, //
		32767, 32768, 32769, //
		-32767, -32768, -32769, //
		65535, 65536, 65537, //
		Integer.MAX_VALUE, Integer.MIN_VALUE, //
	};

	public void testStrings () {
		roundTrip(new StringSerializer(), 21, "abcdef\u00E1\u00E9\u00ED\u00F3\u00FA\u1234");
	}

	public void testCollection () {
		Kryo kryo = new Kryo();
		CollectionSerializer serializer = new CollectionSerializer(kryo);
		roundTrip(serializer, 11, new ArrayList(Arrays.asList("1", "2", "3")));
		roundTrip(serializer, 13, new ArrayList(Arrays.asList("1", "2", null, 1, 2)));
		roundTrip(serializer, 15, new ArrayList(Arrays.asList("1", "2", null, 1, 2, 5)));
		roundTrip(serializer, 11, new ArrayList(Arrays.asList("1", "2", "3")));
		roundTrip(serializer, 11, new ArrayList(Arrays.asList("1", "2", "3")));
		serializer.setElementClass(String.class);
		roundTrip(serializer, 11, new ArrayList(Arrays.asList("1", "2", "3")));
		serializer.setElementsCanBeNull(false);
		roundTrip(serializer, 8, new ArrayList(Arrays.asList("1", "2", "3")));
	}

	public void testArray () {
		Kryo kryo = new Kryo();
		ArraySerializer serializer = new ArraySerializer(kryo);
		roundTrip(serializer, 7, new int[] {1, 2, 3, 4});
		roundTrip(serializer, 11, new int[] {1, 2, -100, 4});
		roundTrip(serializer, 13, new int[] {1, 2, -100, 40000});
		roundTrip(serializer, 10, new int[][] { {1, 2}, {100, 4}});
		roundTrip(serializer, 12, new int[][] { {1}, {2}, {100}, {4}});
		roundTrip(serializer, 15, new int[][][] { { {1}, {2}}, { {100}, {4}}});
		roundTrip(serializer, 19, new String[] {"11", "2222", "3", "4"});
		roundTrip(serializer, 17, new String[] {"11", "2222", null, "4"});
		serializer.setDimensionCount(1);
		serializer.setElementsAreSameType(true);
		roundTrip(serializer, 16, new String[] {"11", "2222", null, "4"});
		serializer.setElementsAreSameType(false);
		roundTrip(serializer, 16, new String[] {"11", "2222", null, "4"});
		roundTrip(new ArraySerializer(kryo), 6, new String[] {null, null, null});
		roundTrip(new ArraySerializer(kryo), 3, new String[] {});
		serializer.setElementsCanBeNull(true);
		serializer.setElementsAreSameType(true);
		roundTrip(serializer, 18, new String[] {"11", "2222", "3", "4"});
		serializer.setElementsCanBeNull(false);
		roundTrip(serializer, 14, new String[] {"11", "2222", "3", "4"});
		serializer.setLength(4);
		roundTrip(serializer, 13, new String[] {"11", "2222", "3", "4"});
	}

	public void testMap () {
		Kryo kryo = new Kryo();
		HashMap map = new HashMap();
		map.put("123", "456");
		map.put("789", "abc");
		MapSerializer serializer = new MapSerializer(kryo);
		roundTrip(serializer, 22, map);
		serializer.setKeyClass(String.class);
		serializer.setKeysCanBeNull(false);
		serializer.setValueClass(String.class);
		roundTrip(serializer, 20, map);
		serializer.setValuesCanBeNull(false);
		roundTrip(serializer, 18, map);
	}

	public void testShort () {
		roundTrip(new ShortSerializer(true), 2, (short)123);
		roundTrip(new ShortSerializer(false), 2, (short)123);

		buffer.clear();
		ShortSerializer.put(buffer, (short)250, false);
		buffer.flip();
		assertEquals(3, buffer.limit());
		assertEquals((short)250, ShortSerializer.get(buffer, false));

		buffer.clear();
		ShortSerializer.put(buffer, (short)250, true);
		buffer.flip();
		assertEquals(1, buffer.limit());
		assertEquals((short)250, ShortSerializer.get(buffer, true));

		buffer.clear();
		ShortSerializer.put(buffer, (short)123, true);
		buffer.flip();
		assertEquals(1, buffer.limit());
		assertEquals((short)123, ShortSerializer.get(buffer, true));
	}

	public void testNumbers () {
		for (int value : intValues) {
			roundTrip(value, false);
			roundTrip(value, true);
		}
		// now go through powers of 2
		for (long value = 65536; value > 0; value /= 2) {
			System.out.println(value);
			roundTrip(value, false);
			roundTrip(value, true);

			roundTrip(value + 1, false);
			roundTrip(value + 1, true);

			roundTrip(value - 1, false);
			roundTrip(value - 1, true);

			roundTrip(-value, false);
			roundTrip(-value, true);

			roundTrip(-value + 1, false);
			roundTrip(-value + 1, true);

			roundTrip(-value - 1, false);
			roundTrip(-value - 1, true);
		}
	}

	private void roundTrip (long value, boolean optimizePositive) {
		buffer.clear();
		LongSerializer.put(buffer, value, optimizePositive);
		buffer.flip();
		long result = LongSerializer.get(buffer, optimizePositive);
		System.out.println(value + " long bytes, " + optimizePositive + ": " + buffer.limit());
		assertEquals(result, value);

		int intValue = (int)value;
		buffer.clear();
		IntSerializer.put(buffer, intValue, optimizePositive);
		buffer.flip();
		result = IntSerializer.get(buffer, optimizePositive);
		System.out.println(intValue + " int bytes, " + optimizePositive + ": " + buffer.limit());
		assertEquals(result, intValue);

		short shortValue = (short)value;
		buffer.clear();
		ShortSerializer.put(buffer, shortValue, optimizePositive);
		buffer.flip();
		short shortResult = ShortSerializer.get(buffer, optimizePositive);
		System.out.println(shortValue + " short bytes, " + optimizePositive + ": " + buffer.limit());
		assertEquals(shortResult, shortValue);
	}

	private <T> T roundTrip (Serializer serializer, int length, T object1) {
		buffer.clear();
		serializer.writeObject(buffer, object1);
		buffer.flip();
		System.out.println(object1 + " bytes: " + buffer.remaining());
		assertEquals("Incorrect length.", length, buffer.remaining());

		Object object2 = serializer.readObject(buffer, object1.getClass());
		if (object1.getClass().isArray()) {
			if (object1 instanceof int[])
				Assert.assertArrayEquals((int[])object1, (int[])object2);
			else if (object1 instanceof int[][])
				Assert.assertArrayEquals((int[][])object1, (int[][])object2);
			else if (object1 instanceof int[][][])
				Assert.assertArrayEquals((int[][][])object1, (int[][][])object2);
			else if (object1 instanceof String[])
				Assert.assertArrayEquals((String[])object1, (String[])object2);
			else
				fail();
		} else
			assertEquals(object1, object2);
		return (T)object2;
	}

	private <T> T roundTrip (Kryo kryo, int length, T object1) {
		buffer.clear();
		kryo.writeClassAndObject(buffer, object1);
		buffer.flip();
		System.out.println(object1 + " bytes: " + buffer.remaining());
		assertEquals("Incorrect length.", length, buffer.remaining());

		Object object2 = kryo.readClassAndObject(buffer);
		assertEquals(object1, object2);
		return (T)object2;
	}

	public void testNonNull () {
		Kryo kryo = new Kryo();

		StringTestClass value = new StringTestClass();
		value.text = "moo";

		buffer.clear();
		FieldSerializer fieldSerializer = new FieldSerializer(kryo, StringTestClass.class);
		fieldSerializer.writeObjectData(buffer, value);
		buffer.flip();
		assertEquals("Incorrect length.", 5, buffer.remaining());

		NonNullTestClass nonNullValue = new NonNullTestClass();
		nonNullValue.nonNullText = "moo";

		buffer.clear();
		fieldSerializer = new FieldSerializer(kryo, NonNullTestClass.class);
		fieldSerializer.writeObjectData(buffer, nonNullValue);
		buffer.flip();
		assertEquals("Incorrect length.", 4, buffer.remaining());
	}

	public void testDateSerializer () {
		Kryo kryo = new Kryo();
		kryo.register(Date.class, new DateSerializer());
		buffer.clear();
		Date date = new Date(0);
		kryo.writeClassAndObject(buffer, date);
		buffer.flip();
		assertEquals(date, kryo.readClassAndObject(buffer));
		buffer.clear();
		date = new Date(1234567);
		kryo.writeClassAndObject(buffer, date);
		buffer.flip();
		assertEquals(date, kryo.readClassAndObject(buffer));
	}

	public void testFieldSerializer () {
		TestClass value = new TestClass();
		value.child = new TestClass();

		Kryo kryo = new Kryo();

		FieldSerializer serializer = new FieldSerializer(kryo, TestClass.class);
		serializer.removeField("optional");
		value.optional = 123;
		kryo.register(TestClass.class, serializer);

		TestClass value2 = roundTrip(serializer, 35, value);
		assertEquals(0, value2.optional);

		serializer = new FieldSerializer(kryo, TestClass.class);
		value.optional = 123;
		value2 = roundTrip(serializer, 36, value);
		assertEquals(123, value2.optional);
	}

	public void testFieldSerializerRegistrationOrder () {
		A a = new A();
		a.value = 100;
		a.b = new B();
		a.b.value = 200;
		a.b.a = new A();
		a.b.a.value = 300;

		Kryo kryo = new Kryo();
		kryo.register(A.class);
		kryo.register(B.class);
		roundTrip(kryo, 9, a);

		kryo = new Kryo();
		kryo.register(B.class);
		kryo.register(A.class);
		roundTrip(kryo, 9, a);
	}

	public void testNoDefaultConstructor () {
		SimpleNoDefaultConstructor object1 = new SimpleNoDefaultConstructor(2);
		roundTrip(new SimpleSerializer<SimpleNoDefaultConstructor>() {
			public SimpleNoDefaultConstructor read (ByteBuffer buffer) {
				return new SimpleNoDefaultConstructor(IntSerializer.get(buffer, true));
			}

			public void write (ByteBuffer buffer, SimpleNoDefaultConstructor object) {
				IntSerializer.put(buffer, object.constructorValue, true);
			}
		}, 2, object1);

		Kryo kryo = new Kryo();
		FieldSerializer complexSerializer = new FieldSerializer(kryo, ComplexNoDefaultConstructor.class) {
			public ComplexNoDefaultConstructor readObjectData (ByteBuffer buffer, Class type) {
				String name = StringSerializer.get(buffer);
				ComplexNoDefaultConstructor object = new ComplexNoDefaultConstructor(name);
				readObjectData(object, buffer, type);
				return object;
			}

			public void writeObjectData (ByteBuffer buffer, Object object) {
				ComplexNoDefaultConstructor complexObject = (ComplexNoDefaultConstructor)object;
				StringSerializer.put(buffer, complexObject.name);
				super.writeObjectData(buffer, complexObject);
			}
		};
		ComplexNoDefaultConstructor object2 = new ComplexNoDefaultConstructor("has no zero arg constructor!");
		object2.anotherField1 = 1234;
		object2.anotherField2 = "abcd";
		roundTrip(complexSerializer, 38, object2);
	}

	public void testArrayList () {
		Kryo kryo = new Kryo();
		kryo.register(ArrayList.class);
		ArrayList list = new ArrayList(Arrays.asList("1", "2", "3"));
		ByteBuffer buffer = ByteBuffer.allocate(1024);

		kryo.writeObject(buffer, list);
		buffer.flip();
		kryo.readObject(buffer, ArrayList.class);
	}

	public void testNulls () {
		Kryo kryo = new Kryo();
		kryo.register(ArrayList.class);
		ByteBuffer buffer = ByteBuffer.allocate(1024);

		kryo.writeObject(buffer, null);
		buffer.flip();
		Object object = kryo.readObject(buffer, ArrayList.class);
		assertNull(object);

		buffer.clear();
		kryo.writeClassAndObject(buffer, null);
		buffer.flip();
		object = kryo.readClassAndObject(buffer);
		assertNull(object);

		buffer.clear();
		kryo.writeClass(buffer, null);
		buffer.flip();
		object = kryo.readClass(buffer);
		assertNull(object);
	}

	public void testUnregisteredClassNames () {
		TestClass value = new TestClass();
		value.child = new TestClass();
		value.optional = 123;
		Kryo kryo = new Kryo();
		kryo.setAllowUnregisteredClasses(true);
		buffer.clear();
		kryo.writeClassAndObject(buffer, value);
		buffer.flip();
		TestClass value2 = (TestClass)kryo.readClassAndObject(buffer);
		assertEquals(value, value2);
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

	static public class NonNullTestClass {
		@NotNull
		public String nonNullText;
	}

	static public class StringTestClass {
		public String text = "something";
	}

	static public class TestClass {
		public String text = "something";
		public String nullField;
		TestClass child;
		private float abc = 1.2f;
		public int optional;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			TestClass other = (TestClass)obj;
			if (Float.floatToIntBits(abc) != Float.floatToIntBits(other.abc)) return false;
			if (child == null) {
				if (other.child != null) return false;
			} else if (!child.equals(other.child)) return false;
			if (nullField == null) {
				if (other.nullField != null) return false;
			} else if (!nullField.equals(other.nullField)) return false;
			if (text == null) {
				if (other.text != null) return false;
			} else if (!text.equals(other.text)) return false;
			return true;
		}
	}

	static public final class A {
		public int value;
		public B b;

		public int hashCode () {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((b == null) ? 0 : b.hashCode());
			result = prime * result + value;
			return result;
		}

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

		public int hashCode () {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((a == null) ? 0 : a.hashCode());
			result = prime * result + value;
			return result;
		}

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
}
