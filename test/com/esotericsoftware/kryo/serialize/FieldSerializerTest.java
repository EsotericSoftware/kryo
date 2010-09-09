
package com.esotericsoftware.kryo.serialize;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.NotNull;
import com.esotericsoftware.kryo.SerializationException;

public class FieldSerializerTest extends KryoTestCase {
	public void testFieldSerializer () {
		Kryo kryo = new Kryo();
		kryo.register(TestClass.class);
		kryo.register(HasStringField.class);

		HasStringField hasStringField = new HasStringField();
		hasStringField.text = "moo";
		roundTrip(kryo, 6, hasStringField);
		roundTrip(new FieldSerializer(kryo, HasStringField.class), 6, hasStringField);

		TestClass test = new TestClass();
		test.optional = 12;
		test.nullField = "value";
		test.text = "123";
		test.child = new TestClass();
		roundTrip(kryo, 39, test);
		test.nullField = null;
		roundTrip(kryo, 33, test);

		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(TestClass.class);
		serializer.removeField("optional");
		roundTrip(kryo, 31, test);
	}

	public void testNonNull () {
		Kryo kryo = new Kryo();
		kryo.register(HasNonNull.class);
		HasNonNull nonNullValue = new HasNonNull();
		nonNullValue.nonNullText = "moo";
		roundTrip(kryo, 5, nonNullValue);
	}

	public void testRegistrationOrder () {
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

	public void testSerializationExceptionTraceInfo () {
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

		buffer.clear();
		try {
			kryoWithoutF.writeClassAndObject(buffer, c);
			fail("Should have failed because F is not registered.");
		} catch (SerializationException ignored) {
		}

		Kryo kryo = new Kryo();
		kryo.register(A.class);
		kryo.register(B.class);
		kryo.register(C.class);
		kryo.register(D.class);
		kryo.register(E.class);
		kryo.register(F.class);

		buffer.clear();
		kryo.writeClassAndObject(buffer, c);
		buffer.flip();
		assertEquals(11, buffer.limit());

		try {
			kryoWithoutF.readClassAndObject(buffer);
			fail("Should have failed because F is not registered.");
		} catch (SerializationException ignored) {
		}
	}

	static public class HasStringField {
		public String text = "something";

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

	static public class HasNonNull {
		@NotNull
		public String nonNullText;

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

	static public class TestClass {
		public String text = "something";
		public String nullField;
		TestClass child;
		TestClass child2;
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
			} else if (child != this && !child.equals(other.child)) return false;
			if (child2 == null) {
				if (other.child2 != null) return false;
			} else if (child2 != this && !child2.equals(other.child2)) return false;
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
	}

	static public final class D {
		public E e;
	}

	static public final class E {
		public F f;
	}

	static public final class F {
		public int value;
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
}
