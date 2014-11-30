package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;

import java.io.FileNotFoundException;

/** @author bohr.qiu <bohr.qiu@gmail.com> */
public class DuplicateFieldNameAcceptedCompatibleFieldSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}
	
	public void testCompatibleFieldSerializer() throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();
		object1.other = new AnotherClass();
		object1.other.value = "meow";
		kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
		kryo.register(TestClass.class);
		kryo.register(AnotherClass.class);
		roundTrip(100, 100, object1);
	}
	
	public void testAddedField() throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();
		object1.other = new AnotherClass();
		object1.other.value = "meow";
		
		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(
			kryo, TestClass.class);
		serializer.removeField("text");
		kryo.register(TestClass.class, serializer);
		kryo.register(AnotherClass.class, new CompatibleFieldSerializer(
			kryo, AnotherClass.class));
		roundTrip(74, 74, object1);
		
		kryo.register(TestClass.class, new CompatibleFieldSerializer(
			kryo, TestClass.class));
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}
	
	public void testRemovedField() throws FileNotFoundException {
		TestClass object1 = new TestClass();
		object1.child = new TestClass();
		
		kryo.register(TestClass.class, new CompatibleFieldSerializer(
			kryo, TestClass.class));
		roundTrip(88, 88, object1);
		
		CompatibleFieldSerializer serializer = new CompatibleFieldSerializer(
			kryo, TestClass.class);
		serializer.removeField("text");
		kryo.register(TestClass.class, serializer);
		Object object2 = kryo.readClassAndObject(input);
		assertEquals(object1, object2);
	}
	

	
	/**
	 * use CompatibleFieldSerializer,duplicate field's
	 * value will not missing
	 * @throws FileNotFoundException
	 */
	public void testDuplicateFieldNameSuccess() throws FileNotFoundException {
		PojoClass pojoClass = new PojoClass();
		pojoClass.setAnInt(1);
		pojoClass.setLongValue(2l);
		pojoClass.setText("text");
		pojoClass.setText1("text1");
		
		kryo.register(PojoClass.class, new CompatibleFieldSerializer(
			kryo, PojoClass.class));
		Output output = new Output(2048);
		kryo.writeClassAndObject(output, pojoClass);
		byte[] bytes = output.toBytes();
		Input input = new Input();
		input.setBuffer(bytes);
		PojoClass result = (PojoClass) kryo.readClassAndObject(input);
		assertEquals(pojoClass, result);
	}
	
	static public class TestClass {
		public String text = "something";
		public int moo = 120;
		public long moo2 = 1234120;
		public TestClass child;
		public int zzz = 123;
		public AnotherClass other;
		
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestClass other = (TestClass) obj;
			if (child == null) {
				if (other.child != null)
					return false;
			} else if (!child.equals(other.child))
				return false;
			if (moo != other.moo)
				return false;
			if (moo2 != other.moo2)
				return false;
			if (text == null) {
				if (other.text != null)
					return false;
			} else if (!text.equals(other.text))
				return false;
			if (zzz != other.zzz)
				return false;
			return true;
		}
	}
	
	static public class AnotherClass {
		String value;
	}
	
	static public class PojoClass extends SupPojoClass {
		private String text;
		private int anInt;
		private long longValue;
		private String text1;
		
		public String getText() {
			return text;
		}
		
		public void setText(String text) {
			this.text = text;
		}
		
		public int getAnInt() {
			return anInt;
		}
		
		public void setAnInt(int anInt) {
			this.anInt = anInt;
		}
		
		public long getLongValue() {
			return longValue;
		}
		
		public void setLongValue(long longValue) {
			this.longValue = longValue;
		}
		
		public String getText1() {
			return text1;
		}
		
		public void setText1(String text1) {
			this.text1 = text1;
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			if (!super.equals(o))
				return false;
			
			PojoClass pojoClass = (PojoClass) o;
			
			if (anInt != pojoClass.anInt)
				return false;
			if (longValue != pojoClass.longValue)
				return false;
			if (text != null ? !text.equals(pojoClass.text) : pojoClass.text != null)
				return false;
			if (text1 != null ? !text1.equals(pojoClass.text1) : pojoClass.text1 != null)
				return false;
			
			return true;
		}
		
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("PojoClass{");
			sb.append("text='").append(text).append('\'');
			sb.append(", anInt=").append(anInt);
			sb.append(", longValue=").append(longValue);
			sb.append(", text1='").append(text1).append('\'');
			sb.append('}');
			return sb.toString();
		}
	}
	
	static public class SupPojoClass {
		private String text;
		private int anInt;
		private long longValue;
		
		public String getText() {
			return text;
		}
		
		public void setText(String text) {
			this.text = text;
		}
		
		public int getAnInt() {
			return anInt;
		}
		
		public void setAnInt(int anInt) {
			this.anInt = anInt;
		}
		
		public long getLongValue() {
			return longValue;
		}
		
		public void setLongValue(long longValue) {
			this.longValue = longValue;
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			
			SupPojoClass that = (SupPojoClass) o;
			
			if (anInt != that.anInt)
				return false;
			if (longValue != that.longValue)
				return false;
			if (text != null ? !text.equals(that.text) : that.text != null)
				return false;
			
			return true;
		}
	}
}
