
package com.esotericsoftware.kryo.serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Date;

import junit.framework.TestCase;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.ObjectBuffer;

public class ObjectBufferTest extends TestCase {
	private TestObject testObject;
	private Kryo kryo;

	public ObjectBufferTest () {
		kryo = new Kryo();
		kryo.register(TestObject.class);
		kryo.register(TestEnum.class);
		kryo.register(BigInteger.class, new BigIntegerSerializer());
		kryo.register(BigDecimal.class, new BigDecimalSerializer());
		kryo.register(Date.class, new DateSerializer());
		kryo.register(Timestamp.class, new SimpleSerializer() {
			public Timestamp read (ByteBuffer buffer) {
				return new Timestamp(LongSerializer.get(buffer, true));
			}

			public void write (ByteBuffer buffer, Object object) {
				LongSerializer.put(buffer, ((Timestamp)object).getTime(), true);
			}
		});

		testObject = new TestObject();
		testObject.valid = true;
		testObject.intValue = Integer.MIN_VALUE;
		testObject.longWrapperValue = Long.valueOf(Long.MAX_VALUE);
		testObject.bigInteger = BigInteger.TEN;
		testObject.floatValue = Float.MIN_NORMAL;
		testObject.doubleWrapperValue = Double.valueOf(Double.MIN_NORMAL);
		testObject.bigDecimal = BigDecimal.TEN;
		testObject.date = new Date();
		testObject.enumValue = TestEnum.USD;
	}

	public void testStreams () {
		ObjectBuffer buffer = new ObjectBuffer(kryo, 1024);

		ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
		buffer.writeClassAndObject(output, testObject);

		ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
		Object resultObject = buffer.readClassAndObject(input);
		assertEquals(testObject, resultObject);

		input = new ByteArrayInputStream(output.toByteArray());
		resultObject = buffer.readClassAndObject(input);
		assertEquals(testObject, resultObject);
	}

	public void testByteArrays () {
		ObjectBuffer buffer = new ObjectBuffer(kryo, 1024);
		byte[] input = buffer.writeClassAndObject(testObject);
		Object resultObject = buffer.readClassAndObject(input);
		assertEquals(testObject, resultObject);

		resultObject = buffer.readClassAndObject(input);
		assertEquals(testObject, resultObject);
	}

	public void testBufferResize () {
		ObjectBuffer buffer = new ObjectBuffer(kryo, 2, 1024);
		byte[] input = buffer.writeClassAndObject(testObject);
		Object resultObject = buffer.readClassAndObject(input);
		assertEquals(testObject, resultObject);

		resultObject = buffer.readClassAndObject(input);
		assertEquals(testObject, resultObject);

		input = buffer.writeClassAndObject(testObject);
		resultObject = buffer.readClassAndObject(input);
		assertEquals(testObject, resultObject);
	}

	static public class TestObject {
		boolean valid;
		int intValue;
		Long longWrapperValue;
		BigInteger bigInteger;
		float floatValue;
		Double doubleWrapperValue;
		BigDecimal bigDecimal;
		Date date;
		TestEnum enumValue;
		String stringValue;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			TestObject other = (TestObject)obj;
			if (bigDecimal == null) {
				if (other.bigDecimal != null) return false;
			} else if (!bigDecimal.equals(other.bigDecimal)) return false;
			if (bigInteger == null) {
				if (other.bigInteger != null) return false;
			} else if (!bigInteger.equals(other.bigInteger)) return false;
			if (date == null) {
				if (other.date != null) return false;
			} else if (!date.equals(other.date)) return false;
			if (doubleWrapperValue == null) {
				if (other.doubleWrapperValue != null) return false;
			} else if (!doubleWrapperValue.equals(other.doubleWrapperValue)) return false;
			if (enumValue == null) {
				if (other.enumValue != null) return false;
			} else if (!enumValue.equals(other.enumValue)) return false;
			if (Float.floatToIntBits(floatValue) != Float.floatToIntBits(other.floatValue)) return false;
			if (intValue != other.intValue) return false;
			if (longWrapperValue == null) {
				if (other.longWrapperValue != null) return false;
			} else if (!longWrapperValue.equals(other.longWrapperValue)) return false;
			if (valid != other.valid) return false;
			return true;
		}
	}

	static enum TestEnum {
		EUR, GBP, USD, JPY, INR
	}
}
