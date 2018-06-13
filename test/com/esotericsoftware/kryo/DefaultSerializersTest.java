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
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

/** @author Nathan Sweet <misc@n4te.com> */
public class DefaultSerializersTest {
	private Kryo kryo = new TestKryoFactory().create();
	private final boolean supportsCopy = true;
	private KryoTestSupport support = new KryoTestSupport(kryo, supportsCopy);

	@Test
	public void testBoolean () {
		support.roundTrip(2, 2, true);
		support.roundTrip(2, 2, false);
	}

	@Test
	public void testByte () {
		support.roundTrip(2, 2, (byte)1);
		support.roundTrip(2, 2, (byte)125);
		support.roundTrip(2, 2, (byte)-125);
	}

	@Test
	public void testChar () {
		support.roundTrip(3, 3, 'a');
		support.roundTrip(3, 3, 'z');
	}

	@Test
	public void testDouble () {
		support.roundTrip(9, 9, 0d);
		support.roundTrip(9, 9, 1234d);
		support.roundTrip(9, 9, 1234.5678d);
	}

	@Test
	public void testFloat () {
		support.roundTrip(5, 5, 0f);
		support.roundTrip(5, 5, 123f);
		support.roundTrip(5, 5, 123.456f);
	}

	@Test
	public void testInt () {
		support.roundTrip(2, 5, 0);
		support.roundTrip(2, 5, 63);
		support.roundTrip(3, 5, 64);
		support.roundTrip(3, 5, 127);
		support.roundTrip(3, 5, 128);
		support.roundTrip(3, 5, 8191);
		support.roundTrip(4, 5, 8192);
		support.roundTrip(4, 5, 16383);
		support.roundTrip(4, 5, 16384);
		support.roundTrip(5, 5, 2097151);
		support.roundTrip(4, 5, 1048575);
		support.roundTrip(5, 5, 134217727);
		support.roundTrip(6, 5, 268435455);
		support.roundTrip(6, 5, 134217728);
		support.roundTrip(6, 5, 268435456);
		support.roundTrip(2, 5, -64);
		support.roundTrip(3, 5, -65);
		support.roundTrip(3, 5, -8192);
		support.roundTrip(4, 5, -1048576);
		support.roundTrip(5, 5, -134217728);
		support.roundTrip(6, 5, -134217729);
	}

	@Test
	public void testLong () {
		support.roundTrip(2, 9, 0l);
		support.roundTrip(2, 9, 63l);
		support.roundTrip(3, 9, 64l);
		support.roundTrip(3, 9, 127l);
		support.roundTrip(3, 9, 128l);
		support.roundTrip(3, 9, 8191l);
		support.roundTrip(4, 9, 8192l);
		support.roundTrip(4, 9, 16383l);
		support.roundTrip(4, 9, 16384l);
		support.roundTrip(5, 9, 2097151l);
		support.roundTrip(4, 9, 1048575l);
		support.roundTrip(5, 9, 134217727l);
		support.roundTrip(6, 9, 268435455l);
		support.roundTrip(6, 9, 134217728l);
		support.roundTrip(6, 9, 268435456l);
		support.roundTrip(2, 9, -64l);
		support.roundTrip(3, 9, -65l);
		support.roundTrip(3, 9, -8192l);
		support.roundTrip(4, 9, -1048576l);
		support.roundTrip(5, 9, -134217728l);
		support.roundTrip(6, 9, -134217729l);
		support.roundTrip(10, 9, 2368365495612416452l);
		support.roundTrip(10, 9, -2368365495612416452l);
	}

	@Test
	public void testShort () {
		support.roundTrip(3, 3, (short)0);
		support.roundTrip(3, 3, (short)123);
		support.roundTrip(3, 3, (short)123);
		support.roundTrip(3, 3, (short)-123);
		support.roundTrip(3, 3, (short)250);
		support.roundTrip(3, 3, (short)123);
		support.roundTrip(3, 3, (short)400);
	}

	@Test
	public void testString () {
		kryo = new Kryo();
		kryo.setRegistrationRequired(true);
		support = new KryoTestSupport(kryo, supportsCopy);

		support.roundTrip(6, 6, "meow");
		support.roundTrip(70, 70, "abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdef");

		kryo.setReferences(false);
		support.roundTrip(5, 5, "meow");

		support.roundTrip(3, 3, "a");
		support.roundTrip(3, 3, "\n");
		support.roundTrip(2, 2, "");
		support.roundTrip(100, 100,
			"ABCDEFGHIJKLMNOPQRSTUVWXYZ\rabcdefghijklmnopqrstuvwxyz\n1234567890\t\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*");

		support.roundTrip(21, 21, "abcdef\u00E1\u00E9\u00ED\u00F3\u00FA\u7C9F");
	}

	@Test
	public void testVoid () throws InstantiationException, IllegalAccessException {
		support.roundTrip(1, 1, (Void)null);
	}

	@Test
	public void doTestNull () {
		kryo.setRegistrationRequired(true);
		kryo.register(ArrayList.class);
		support.roundTrip(1, 1, null);
		doTestNull(Long.class);
		doTestNull(ArrayList.class);

		kryo.setReferences(false);
		support.roundTrip(1, 1, null);
		doTestNull(Long.class);
		doTestNull(ArrayList.class);
	}

	private void doTestNull (Class type) {
		byte[] bytes;
		try (Output output = new Output(4096)) {
			kryo.writeObjectOrNull(output, null, type);
			output.flush();
			bytes = output.toBytes();
		}

		try (Input input = new Input(bytes)) {
			Object object = kryo.readObjectOrNull(input, type);
			assertNull(object);
		}
	}

	@Test
	public void testDateSerializer () {
		kryo.register(Date.class);
		support.roundTrip(10, 9, new Date(-1234567));
		support.roundTrip(2, 9, new Date(0));
		support.roundTrip(4, 9, new Date(1234567));
		support.roundTrip(10, 9, new Date(-1234567));

		kryo.register(java.sql.Date.class);
		support.roundTrip(10, 9, new java.sql.Date(Long.MIN_VALUE));
		support.roundTrip(2, 9, new java.sql.Date(0));
		support.roundTrip(4, 9, new java.sql.Date(1234567));
		support.roundTrip(10, 9, new java.sql.Date(Long.MAX_VALUE));
		support.roundTrip(10, 9, new java.sql.Date(-1234567));

		kryo.register(java.sql.Time.class);
		support.roundTrip(10, 9, new java.sql.Time(Long.MIN_VALUE));
		support.roundTrip(2, 9, new java.sql.Time(0));
		support.roundTrip(4, 9, new java.sql.Time(1234567));
		support.roundTrip(10, 9, new java.sql.Time(Long.MAX_VALUE));
		support.roundTrip(10, 9, new java.sql.Time(-1234567));

		kryo.register(java.sql.Timestamp.class);
		support.roundTrip(10, 9, new java.sql.Timestamp(Long.MIN_VALUE));
		support.roundTrip(2, 9, new java.sql.Timestamp(0));
		support.roundTrip(4, 9, new java.sql.Timestamp(1234567));
		support.roundTrip(10, 9, new java.sql.Timestamp(Long.MAX_VALUE));
		support.roundTrip(10, 9, new java.sql.Timestamp(-1234567));
	}

	@Test
	public void testBigDecimalSerializer () {
		kryo.register(BigDecimal.class);
		kryo.register(BigDecimalSubclass.class);
		support.roundTrip(5, 8, BigDecimal.valueOf(12345, 2));
		support.roundTrip(7, 10, new BigDecimal("12345.12345"));
		support.roundTrip(4, 7, BigDecimal.ZERO);
		support.roundTrip(4, 7, BigDecimal.ONE);
		support.roundTrip(4, 7, BigDecimal.TEN);
		support.roundTrip(5, 8, new BigDecimalSubclass(new BigInteger("12345"), 2));
		support.roundTrip(7, 10, new BigDecimalSubclass("12345.12345"));
	}

	@Test
	public void testBigIntegerSerializer () {
		kryo.register(BigInteger.class);
		kryo.register(BigIntegerSubclass.class);
		support.roundTrip(8, 8, BigInteger.valueOf(1270507903945L));
		support.roundTrip(3, 3, BigInteger.ZERO);
		support.roundTrip(3, 3, BigInteger.ONE);
		support.roundTrip(3, 3, BigInteger.TEN);
		support.roundTrip(8, 8, new BigIntegerSubclass("1270507903945"));
	}

	@Test
	public void testEnumSerializer () {
		kryo.register(TestEnum.class);
		support.roundTrip(2, 2, TestEnum.a);
		support.roundTrip(2, 2, TestEnum.b);
		support.roundTrip(2, 2, TestEnum.c);

		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		support = new KryoTestSupport(kryo, supportsCopy);
		// 1 byte identifying it's a class name
		// 1 byte for the class name id
		// 57 bytes for the class name characters
		// 1 byte for the reference id
		// 1 byte for the enum value
		support.roundTrip(61, 61, TestEnum.c);
	}

	@Test
	public void testEnumSetSerializer () {
		kryo.register(EnumSet.class);
		kryo.register(TestEnum.class);
		support.roundTrip(5, 8, EnumSet.of(TestEnum.a, TestEnum.c));
		support.roundTrip(4, 7, EnumSet.of(TestEnum.a));
		support.roundTrip(6, 9, EnumSet.allOf(TestEnum.class));

		// Test empty EnumSet
		support.roundTrip(3, 6, EnumSet.noneOf(TestEnum.class));

		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		support = new KryoTestSupport(kryo, supportsCopy);

		support.roundTrip(89, 92, EnumSet.of(TestEnum.a, TestEnum.c));
	}

	@Test
	public void testEnumSerializerWithMethods () {
		kryo.register(TestEnumWithMethods.class);
		support.roundTrip(2, 2, TestEnumWithMethods.a);
		support.roundTrip(2, 2, TestEnumWithMethods.b);
		support.roundTrip(2, 2, TestEnumWithMethods.c);

		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		support = new KryoTestSupport(kryo, supportsCopy);
		support.roundTrip(76, 76, TestEnumWithMethods.c);
	}

	@Test
	public void testCollectionsMethods () {
		kryo.setRegistrationRequired(false);
		ArrayList test = new ArrayList();
		test.add(Collections.EMPTY_LIST);
		test.add(Collections.EMPTY_MAP);
		test.add(Collections.EMPTY_SET);
		test.add(Collections.singletonList("meow"));
		test.add(Collections.singletonMap("moo", 1234));
		test.add(Collections.singleton(12.34));
		support.roundTrip(249, 251, test);
	}

	@Test
	public void testDeepCollectionCloning() {
		kryo.setRegistrationRequired(false);
		Object contents = new Object();
		Assert.assertNotEquals(kryo.copy(Collections.singleton(contents)).iterator().next(), contents);
		Assert.assertNotEquals(kryo.copy(Collections.singletonList(contents)).iterator().next(), contents);
		Assert.assertNotEquals(kryo.copy(Collections.singletonMap(contents, contents)).values().iterator().next(), contents);
	}

	@Test
	public void testCalendar () {
		kryo.setRegistrationRequired(false);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		calendar.set(1980, 7, 26, 12, 22, 46);
		support.roundTrip(64, 73, calendar);
	}

	@Test
	public void testClassSerializer () {
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
		kryo.writeObject(out, Enum.class);

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
		assertEquals(Enum.class, kryo.readObject(in, Class.class));
	}

	@Test
	public void testLocaleSerializer () {
		kryo.setRegistrationRequired(true);
		kryo.register(Locale.class);

		support.roundTrip(5, 5, Locale.ENGLISH);
		support.roundTrip(6, 6, Locale.US);
		support.roundTrip(6, 6, Locale.SIMPLIFIED_CHINESE);
		support.roundTrip(5, 5, new Locale("es"));
		support.roundTrip(16, 16, new Locale("es", "ES", "áéíóú"));
	}

	@Test
	public void testCharset () {
		List<String> css = Arrays.asList("ISO-8859-1", "US-ASCII", "UTF-8", "UTF-16", "UTF-16BE", "UTF-16LE");

		for (String cs : css) {
			Charset charset = Charset.forName(cs);
			kryo.register(charset.getClass());
			int expectedLength = 1 + cs.length();
			support.roundTrip(expectedLength, expectedLength, charset);
		}

		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		support = new KryoTestSupport(kryo, supportsCopy);

		for (String cs : css) {
			Charset charset = Charset.forName(cs);
			int expectedLength = 3 + charset.getClass().getName().length() + cs.length();
			support.roundTrip(expectedLength, expectedLength, charset);
		}
	}

	@Test
	public void testURLSerializer () throws Exception {
		kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		kryo.setRegistrationRequired(true);
		kryo.register(URL.class);

		support.roundTrip(41, 41, new URL("https://github.com/EsotericSoftware/kryo"));
		support.roundTrip(78, 78, new URL("https://github.com:443/EsotericSoftware/kryo/pulls?utf8=%E2%9C%93&q=is%3Apr"));
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
		public BigDecimalSubclass (BigInteger unscaledVal, int scale) {
			super(unscaledVal, scale);
		}

		public BigDecimalSubclass (String val) {
			super(val);
		}
	}

	static class BigIntegerSubclass extends BigInteger {
		public BigIntegerSubclass (byte[] val) {
			super(val);
		}

		public BigIntegerSubclass (String val) {
			super(val);
		}
	}

}
