
package com.esotericsoftware.kryo.serialize;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;

public class BeanSerializerTest extends KryoTestCase {
	public void testBeanSerializer () {
		Kryo kryo = new Kryo();
		kryo.register(TestClass.class, new BeanSerializer(kryo, TestClass.class));

		TestClass test = new TestClass();
		test.setOptional(12);
		test.setNullField("value");
		test.setText("123");
		test.setChild(new TestClass());
		roundTrip(kryo, 39, test);
		test.setNullField(null);
		roundTrip(kryo, 33, test);
	}

	static public class TestClass {
		private String text = "something";
		private String nullField;
		private TestClass child;
		private TestClass child2;
		private float abc = 1.2f;
		private int optional;

		public String getText () {
			return text;
		}

		public void setText (String text) {
			this.text = text;
		}

		public String getNullField () {
			return nullField;
		}

		public void setNullField (String nullField) {
			this.nullField = nullField;
		}

		public TestClass getChild () {
			return child;
		}

		public void setChild (TestClass child) {
			this.child = child;
		}

		public TestClass getChild2 () {
			return child2;
		}

		public void setChild2 (TestClass child2) {
			this.child2 = child2;
		}

		public float getAbc () {
			return abc;
		}

		public void setAbc (float abc) {
			this.abc = abc;
		}

		public int getOptional () {
			return optional;
		}

		public void setOptional (int optional) {
			this.optional = optional;
		}

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
}
