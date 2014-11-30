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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.TimeZone;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.Locale;

/** @author Nathan Sweet <misc@n4te.com> */
public class DefaultSerializersTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	public void testBoolean () {
		roundTrip(2, 2, true);
		roundTrip(2, 2, false);
	}

	public void testByte () {
		roundTrip(2, 2, (byte)1);
		roundTrip(2, 2, (byte)125);
		roundTrip(2, 2, (byte)-125);
	}

	public void testChar () {
		roundTrip(3, 3, 'a');
		roundTrip(3, 3, 'z');
	}

	public void testDouble () {
		roundTrip(9, 9, 0d);
		roundTrip(9, 9, 1234d);
		roundTrip(9, 9, 1234.5678d);
	}

	public void testFloat () {
		roundTrip(5, 5, 0f);
		roundTrip(5, 5, 123f);
		roundTrip(5, 5, 123.456f);
	}

	public void testInt () {
		roundTrip(2, 5, 0);
		roundTrip(2, 5, 63);
		roundTrip(3, 5, 64);
		roundTrip(3, 5, 127);
		roundTrip(3, 5, 128);
		roundTrip(3, 5, 8191);
		roundTrip(4, 5, 8192);
		roundTrip(4, 5, 16383);
		roundTrip(4, 5, 16384);
		roundTrip(5, 5, 2097151);
		roundTrip(4, 5, 1048575);
		roundTrip(5, 5, 134217727);
		roundTrip(6, 5, 268435455);
		roundTrip(6, 5, 134217728);
		roundTrip(6, 5, 268435456);
		roundTrip(2, 5, -64);
		roundTrip(3, 5, -65);
		roundTrip(3, 5, -8192);
		roundTrip(4, 5, -1048576);
		roundTrip(5, 5, -134217728);
		roundTrip(6, 5, -134217729);
	}

	public void testLong () {
		roundTrip(2, 9, 0l);
		roundTrip(2, 9, 63l);
		roundTrip(3, 9, 64l);
		roundTrip(3, 9, 127l);
		roundTrip(3, 9, 128l);
		roundTrip(3, 9, 8191l);
		roundTrip(4, 9, 8192l);
		roundTrip(4, 9, 16383l);
		roundTrip(4, 9, 16384l);
		roundTrip(5, 9, 2097151l);
		roundTrip(4, 9, 1048575l);
		roundTrip(5, 9, 134217727l);
		roundTrip(6, 9, 268435455l);
		roundTrip(6, 9, 134217728l);
		roundTrip(6, 9, 268435456l);
		roundTrip(2, 9, -64l);
		roundTrip(3, 9, -65l);
		roundTrip(3, 9, -8192l);
		roundTrip(4, 9, -1048576l);
		roundTrip(5, 9, -134217728l);
		roundTrip(6, 9, -134217729l);
		roundTrip(10, 9, 2368365495612416452l);
		roundTrip(10, 9, -2368365495612416452l);
	}

	public void testShort () {
		roundTrip(3, 3, (short)0);
		roundTrip(3, 3, (short)123);
		roundTrip(3, 3, (short)123);
		roundTrip(3, 3, (short)-123);
		roundTrip(3, 3, (short)250);
		roundTrip(3, 3, (short)123);
		roundTrip(3, 3, (short)400);
	}

	public void testString () {
		kryo = new Kryo();
		kryo.setRegistrationRequired(true);
		roundTrip(6, 6, "meow");
		roundTrip(70, 70, "abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdef");

		kryo.setReferences(false);
		roundTrip(5, 5, "meow");

		roundTrip(3, 3, "a");
		roundTrip(3, 3, "\n");
		roundTrip(2, 2, "");
		roundTrip(100, 100,  "ABCDEFGHIJKLMNOPQRSTUVWXYZ\rabcdefghijklmnopqrstuvwxyz\n1234567890\t\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*");

		roundTrip(21, 21, "abcdef\u00E1\u00E9\u00ED\u00F3\u00FA\u7C9F");
	}

	public void testVoid () throws InstantiationException, IllegalAccessException {
		roundTrip(1, 1, (Void)null);
	}
	
	public void testNull () {
		kryo = new Kryo();
		kryo.setRegistrationRequired(true);
		kryo.register(ArrayList.class);
		roundTrip(1, 1, null);
		testNull(Long.class);
		testNull(ArrayList.class);

		kryo.setReferences(false);
		roundTrip(1, 1, null);
		testNull(Long.class);
		testNull(ArrayList.class);
	}

	private void testNull (Class type) {
		kryo.writeObjectOrNull(output, null, type);
		input.setBuffer(output.toBytes());
		Object object = kryo.readObjectOrNull(input, type);
		assertNull(object);
	}

	public void testDateSerializer () {
		kryo.register(Date.class);
		roundTrip(10, 9, new Date(-1234567));
		roundTrip(2, 9, new Date(0));
		roundTrip(4, 9, new Date(1234567));
		roundTrip(10, 9, new Date(-1234567));

		kryo.register(java.sql.Date.class);
		roundTrip(10, 9, new java.sql.Date(Long.MIN_VALUE));
		roundTrip(2, 9, new java.sql.Date(0));
		roundTrip(4, 9, new java.sql.Date(1234567));
		roundTrip(10, 9, new java.sql.Date(Long.MAX_VALUE));
		roundTrip(10, 9, new java.sql.Date(-1234567));

		kryo.register(java.sql.Time.class);
		roundTrip(10, 9, new java.sql.Time(Long.MIN_VALUE));
		roundTrip(2, 9, new java.sql.Time(0));
		roundTrip(4, 9, new java.sql.Time(1234567));
		roundTrip(10, 9, new java.sql.Time(Long.MAX_VALUE));
		roundTrip(10, 9, new java.sql.Time(-1234567));

		kryo.register(java.sql.Timestamp.class);
		roundTrip(10, 9, new java.sql.Timestamp(Long.MIN_VALUE));
		roundTrip(2, 9, new java.sql.Timestamp(0));
		roundTrip(4, 9, new java.sql.Timestamp(1234567));
		roundTrip(10, 9, new java.sql.Timestamp(Long.MAX_VALUE));
		roundTrip(10, 9, new java.sql.Timestamp(-1234567));
	}

	public void testBigDecimalSerializer () {
		kryo.register(BigDecimal.class);
		kryo.register(BigDecimalSubclass.class);
		roundTrip(5, 8, BigDecimal.valueOf(12345, 2));
		roundTrip(7, 10, new BigDecimal("12345.12345"));
		roundTrip(4, 7, BigDecimal.ZERO);
		roundTrip(4, 7, BigDecimal.ONE);
		roundTrip(4, 7, BigDecimal.TEN);
		roundTrip(5, 8, new BigDecimalSubclass(new BigInteger("12345"), 2));
		roundTrip(7, 10, new BigDecimalSubclass("12345.12345"));
	}

	public void testBigIntegerSerializer () {
		kryo.register(BigInteger.class);
		kryo.register(BigIntegerSubclass.class);
		roundTrip(8, 8, BigInteger.valueOf(1270507903945L));
		roundTrip(3, 3, BigInteger.ZERO);
		roundTrip(3, 3, BigInteger.ONE);
		roundTrip(3, 3, BigInteger.TEN);
		roundTrip(8, 8, new BigIntegerSubclass("1270507903945"));
	}

	public void testEnumSerializer () {
		kryo.register(TestEnum.class);
		roundTrip(2, 2, TestEnum.a);
		roundTrip(2, 2, TestEnum.b);
		roundTrip(2, 2, TestEnum.c);

		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		// 1 byte identifying it's a class name
		// 1 byte for the class name id
		// 57 bytes for the class name characters
		// 1 byte for the reference id
		// 1 byte for the enum value
		roundTrip(61, 61, TestEnum.c);
	}

	public void testEnumSetSerializer () {
		kryo.register(EnumSet.class);
		kryo.register(TestEnum.class);
		roundTrip(5, 8, EnumSet.of(TestEnum.a, TestEnum.c));
		roundTrip(4, 7, EnumSet.of(TestEnum.a));
		roundTrip(6, 9, EnumSet.allOf(TestEnum.class));

		// Test empty EnumSet
		roundTrip(3, 6, EnumSet.noneOf(TestEnum.class));

		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		roundTrip(89, 92, EnumSet.of(TestEnum.a, TestEnum.c));
	}

	public void testEnumSerializerWithMethods () {
		kryo.register(TestEnumWithMethods.class);
		roundTrip(2, 2, TestEnumWithMethods.a);
		roundTrip(2, 2, TestEnumWithMethods.b);
		roundTrip(2, 2, TestEnumWithMethods.c);

		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		roundTrip(76, 76, TestEnumWithMethods.c);
	}

	public void testCollectionsMethods () {
		kryo.setRegistrationRequired(false);
		ArrayList test = new ArrayList();
		test.add(Collections.EMPTY_LIST);
		test.add(Collections.EMPTY_MAP);
		test.add(Collections.EMPTY_SET);
		test.add(Collections.singletonList("meow"));
		test.add(Collections.singletonMap("moo", 1234));
		test.add(Collections.singleton(12.34));
		roundTrip(249, 251, test);
	}

	public void testCalendar () {
		kryo.setRegistrationRequired(false);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		calendar.set(1980, 7, 26, 12, 22, 46);
		roundTrip(64, 73, calendar);
	}
	
	public void testClassSerializer() {
		kryo.register(Class.class);
		kryo.register(ArrayList.class);
		kryo.setRegistrationRequired(false);
		final Output out = new Output(1024);

		kryo.writeObject(out, String.class);
		kryo.writeObject(out, Integer.class);
		kryo.writeObject(out, Short.class);
		kryo.writeObject(out, Long.class);
		kryo.writeObject(out, Double.class);
		kryo.writeObject(out, Float.class);
		kryo.writeObject(out, Boolean.class);
		kryo.writeObject(out, Character.class);
		kryo.writeObject(out, Void.class);

		kryo.writeObject(out, int.class);
		kryo.writeObject(out, short.class);
		kryo.writeObject(out, long.class);
		kryo.writeObject(out, double.class);
		kryo.writeObject(out, float.class);
		kryo.writeObject(out, boolean.class);
		kryo.writeObject(out, char.class);
		kryo.writeObject(out, void.class);
		kryo.writeObject(out, ArrayList.class);
		kryo.writeObject(out, TestEnum.class);

		final Input in = new Input(out.getBuffer());

		assertEquals(String.class, kryo.readObject(in, Class.class));
		assertEquals(Integer.class, kryo.readObject(in, Class.class));
		assertEquals(Short.class, kryo.readObject(in, Class.class));
		assertEquals(Long.class, kryo.readObject(in, Class.class));
		assertEquals(Double.class, kryo.readObject(in, Class.class));
		assertEquals(Float.class, kryo.readObject(in, Class.class));
		assertEquals(Boolean.class, kryo.readObject(in, Class.class));
		assertEquals(Character.class, kryo.readObject(in, Class.class));
		assertEquals(Void.class, kryo.readObject(in, Class.class));
		assertEquals(int.class, kryo.readObject(in, Class.class));
		assertEquals(short.class, kryo.readObject(in, Class.class));
		assertEquals(long.class, kryo.readObject(in, Class.class));
		assertEquals(double.class, kryo.readObject(in, Class.class));
		assertEquals(float.class, kryo.readObject(in, Class.class));
		assertEquals(boolean.class, kryo.readObject(in, Class.class));
		assertEquals(char.class, kryo.readObject(in, Class.class));
		assertEquals(void.class, kryo.readObject(in, Class.class));
		assertEquals(ArrayList.class, kryo.readObject(in, Class.class));
		assertEquals(TestEnum.class, kryo.readObject(in, Class.class));
	}

	public void testLocaleSerializer () {
		kryo.setRegistrationRequired(true);
		kryo.register(Locale.class);
		
		roundTrip(5, 5, Locale.ENGLISH);
		roundTrip(6, 6, Locale.US);
		roundTrip(6, 6, Locale.SIMPLIFIED_CHINESE);
		roundTrip(5, 5, new Locale("es"));		
		roundTrip(16, 16, new Locale("es", "ES", "áéíóú"));		
	}

	public enum TestEnum {
		a, b, c
	}

	public enum TestEnumWithMethods {
		a {
		},
		b {
		},
		c {
		}
	}
	
	static class BigDecimalSubclass extends BigDecimal {
		public BigDecimalSubclass(BigInteger unscaledVal, int scale) {
			super(unscaledVal, scale);
		}
		public BigDecimalSubclass(String val) {
			super(val);
		}
	}
	
	static class BigIntegerSubclass extends BigInteger {
		public BigIntegerSubclass(byte[] val) {
			super(val);
		}
		public BigIntegerSubclass(String val) {
			super(val);
		}
	}
	
}
