/* Copyright (c) 2008-2018, Nathan Sweet
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

import static org.junit.Assert.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.TimeZone;

import org.junit.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;

/** @author Nathan Sweet */
public class DefaultSerializersTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@Test
	public void testBoolean () {
		roundTrip(2, true);
		roundTrip(2, false);
	}

	@Test
	public void testByte () {
		roundTrip(2, (byte)1);
		roundTrip(2, (byte)125);
		roundTrip(2, (byte)-125);
	}

	@Test
	public void testChar () {
		roundTrip(3, 'a');
		roundTrip(3, 'z');
	}

	@Test
	public void testDouble () {
		roundTrip(9, 0d);
		roundTrip(9, 1234d);
		roundTrip(9, 1234.5678d);
	}

	@Test
	public void testFloat () {
		roundTrip(5, 0f);
		roundTrip(5, 123f);
		roundTrip(5, 123.456f);
	}

	@Test
	public void testInt () {
		roundTrip(2, 0);
		roundTrip(2, 63);
		roundTrip(3, 64);
		roundTrip(3, 127);
		roundTrip(3, 128);
		roundTrip(3, 8191);
		roundTrip(4, 8192);
		roundTrip(4, 16383);
		roundTrip(4, 16384);
		roundTrip(5, 2097151);
		roundTrip(4, 1048575);
		roundTrip(5, 134217727);
		roundTrip(6, 268435455);
		roundTrip(6, 134217728);
		roundTrip(6, 268435456);
		roundTrip(2, -64);
		roundTrip(3, -65);
		roundTrip(3, -8192);
		roundTrip(4, -1048576);
		roundTrip(5, -134217728);
		roundTrip(6, -134217729);
	}

	@Test
	public void testLong () {
		roundTrip(2, 0l);
		roundTrip(2, 63l);
		roundTrip(3, 64l);
		roundTrip(3, 127l);
		roundTrip(3, 128l);
		roundTrip(3, 8191l);
		roundTrip(4, 8192l);
		roundTrip(4, 16383l);
		roundTrip(4, 16384l);
		roundTrip(5, 2097151l);
		roundTrip(4, 1048575l);
		roundTrip(5, 134217727l);
		roundTrip(6, 268435455l);
		roundTrip(6, 134217728l);
		roundTrip(6, 268435456l);
		roundTrip(2, -64l);
		roundTrip(3, -65l);
		roundTrip(3, -8192l);
		roundTrip(4, -1048576l);
		roundTrip(5, -134217728l);
		roundTrip(6, -134217729l);
		roundTrip(10, 2368365495612416452l);
		roundTrip(10, -2368365495612416452l);
	}

	@Test
	public void testShort () {
		roundTrip(3, (short)0);
		roundTrip(3, (short)123);
		roundTrip(3, (short)123);
		roundTrip(3, (short)-123);
		roundTrip(3, (short)250);
		roundTrip(3, (short)123);
		roundTrip(3, (short)400);
	}

	@Test
	public void testString () {
		kryo = new Kryo();
		kryo.setReferences(true);
		roundTrip(6, "meow");
		roundTrip(70, "abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdef");

		kryo.setReferences(false);
		roundTrip(5, "meow");

		roundTrip(3, "a");
		roundTrip(3, "\n");
		roundTrip(2, "");
		roundTrip(100, "ABCDEFGHIJKLMNOPQRSTUVWXYZ\rabcdefghijklmnopqrstuvwxyz\n1234567890\t\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*");

		roundTrip(21, "abcdef\u00E1\u00E9\u00ED\u00F3\u00FA\u7C9F");
	}

	@Test
	public void testVoid () {
		roundTrip(1, (Void)null);
	}

	@Test
	public void testNull () {
		kryo = new Kryo();
		kryo.register(ArrayList.class);
		kryo.setReferences(true);
		roundTrip(1, null);
		testNull(Long.class);
		testNull(ArrayList.class);

		kryo.setReferences(false);
		roundTrip(1, null);
		testNull(Long.class);
		testNull(ArrayList.class);
	}

	private void testNull (Class type) {
		kryo.writeObjectOrNull(output, null, type);
		input = new Input(output.toBytes());
		Object object = kryo.readObjectOrNull(input, type);
		assertNull(object);
	}

	@Test
	public void testDateSerializer () {
		kryo.register(Date.class);
		roundTrip(10, new Date(-1234567));
		roundTrip(2, new Date(0));
		roundTrip(4, new Date(1234567));
		roundTrip(10, new Date(-1234567));

		kryo.register(java.sql.Date.class);
		roundTrip(10, new java.sql.Date(Long.MIN_VALUE));
		roundTrip(2, new java.sql.Date(0));
		roundTrip(4, new java.sql.Date(1234567));
		roundTrip(10, new java.sql.Date(Long.MAX_VALUE));
		roundTrip(10, new java.sql.Date(-1234567));

		kryo.register(java.sql.Time.class);
		roundTrip(10, new java.sql.Time(Long.MIN_VALUE));
		roundTrip(2, new java.sql.Time(0));
		roundTrip(4, new java.sql.Time(1234567));
		roundTrip(10, new java.sql.Time(Long.MAX_VALUE));
		roundTrip(10, new java.sql.Time(-1234567));

		kryo.register(java.sql.Timestamp.class);
		roundTrip(10, new java.sql.Timestamp(Long.MIN_VALUE));
		roundTrip(2, new java.sql.Timestamp(0));
		roundTrip(4, new java.sql.Timestamp(1234567));
		roundTrip(10, new java.sql.Timestamp(Long.MAX_VALUE));
		roundTrip(10, new java.sql.Timestamp(-1234567));
	}

	@Test
	public void testBigDecimalSerializer () {
		kryo.register(BigDecimal.class);
		kryo.register(BigDecimalSubclass.class);
		roundTrip(5, BigDecimal.valueOf(12345, 2));
		roundTrip(7, new BigDecimal("12345.12345"));
		roundTrip(4, BigDecimal.ZERO);
		roundTrip(4, BigDecimal.ONE);
		roundTrip(4, BigDecimal.TEN);
		roundTrip(5, new BigDecimalSubclass(new BigInteger("12345"), 2));
		roundTrip(7, new BigDecimalSubclass("12345.12345"));
	}

	@Test
	public void testBigIntegerSerializer () {
		kryo.register(BigInteger.class);
		kryo.register(BigIntegerSubclass.class);
		roundTrip(8, BigInteger.valueOf(1270507903945L));
		roundTrip(3, BigInteger.ZERO);
		roundTrip(3, BigInteger.ONE);
		roundTrip(3, BigInteger.TEN);
		roundTrip(8, new BigIntegerSubclass("1270507903945"));
	}

	@Test
	public void testEnumSerializer () {
		kryo.register(TestEnum.class);
		roundTrip(2, TestEnum.a);
		roundTrip(2, TestEnum.b);
		roundTrip(2, TestEnum.c);

		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		roundTrip(72, TestEnum.c);
	}

	@Test
	public void testEnumSetSerializer () {
		kryo.register(EnumSet.class);
		kryo.register(TestEnum.class);
		roundTrip(5, EnumSet.of(TestEnum.a, TestEnum.c));
		roundTrip(4, EnumSet.of(TestEnum.a));
		roundTrip(6, EnumSet.allOf(TestEnum.class));

		// Test empty EnumSet
		roundTrip(3, EnumSet.noneOf(TestEnum.class));

		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setReferences(true);
		roundTrip(101, EnumSet.of(TestEnum.a, TestEnum.c));
	}

	@Test
	public void testEnumSerializerWithMethods () {
		kryo.register(TestEnumWithMethods.class);
		roundTrip(2, TestEnumWithMethods.a);
		roundTrip(2, TestEnumWithMethods.b);
		roundTrip(2, TestEnumWithMethods.c);

		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		roundTrip(85, TestEnumWithMethods.c);
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
		roundTrip(249, test);
	}

	@Test
	public void testArraysAsListUnregistered () {
		kryo.setRegistrationRequired(false);
		roundTrip(29, Arrays.asList());
		roundTrip(37, Arrays.asList(new String[] {"A", "B", "C"}));
	}

	@Test
	public void testArraysAsList () {
		kryo.register(Arrays.asList().getClass());
		roundTrip(2, Arrays.asList());
		roundTrip(10, Arrays.asList(new String[] {"A", "B", "C"}));
	}

	@Test
	public void testDeepCollectionCloning () {
		kryo.setRegistrationRequired(false);
		Object contents = new Object();
		assertNotEquals(kryo.copy(Collections.singleton(contents)).iterator().next(), contents);
		assertNotEquals(kryo.copy(Collections.singletonList(contents)).iterator().next(), contents);
		assertNotEquals(kryo.copy(Collections.singletonMap(contents, contents)).values().iterator().next(), contents);
	}

	@Test
	public void testPriorityQueueCopy () {
		List<Integer> values = Arrays.asList(7, 0, 5, 123, 432);
		PriorityQueue<Integer> queue = new PriorityQueue(3, new Comparator<Integer>() {
			public int compare (Integer o1, Integer o2) {
				return o2 - o1;
			}
		});
		queue.addAll(values);

		kryo.register(PriorityQueue.class);
		PriorityQueue<Integer> copy = kryo.copy(queue);
		assertEquals(queue.peek(), copy.peek());
	}

	@Test
	public void testCalendar () {
		kryo.setRegistrationRequired(false);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		calendar.set(1980, 7, 26, 12, 22, 46);
		roundTrip(64, calendar);
	}

	@Test
	public void testBitSet () {
		kryo.register(BitSet.class);
		BitSet set = BitSet.valueOf(new long[] {1L, 2L, 99999L, 2345678987654L});
		roundTrip(34, set);
	}

	@Test
	public void testClassSerializer () {
		kryo.register(Class.class);
		kryo.register(ArrayList.class);
		kryo.register(void.class);
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
		kryo.register(Locale.class);

		roundTrip(5, Locale.ENGLISH);
		roundTrip(6, Locale.US);
		roundTrip(6, Locale.SIMPLIFIED_CHINESE);
		roundTrip(5, new Locale("es"));
		roundTrip(16, new Locale("es", "ES", "\u00E1\u00E9\u00ED\u00F3\u00FA"));
	}

	@Test
	public void testCharset () {
		List<String> css = Arrays.asList("ISO-8859-1", "US-ASCII", "UTF-8", "UTF-16", "UTF-16BE", "UTF-16LE");

		for (String cs : css) {
			Charset charset = Charset.forName(cs);
			kryo.register(charset.getClass());
			int expectedLength = 1 + cs.length();
			roundTrip(expectedLength, charset);
		}

		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setReferences(true);

		for (String cs : css) {
			Charset charset = Charset.forName(cs);
			int expectedLength = 3 + charset.getClass().getName().length() + cs.length();
			roundTrip(expectedLength, charset);
		}
	}

	@Test
	public void testURLSerializer () throws Exception {
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		kryo.register(URL.class);

		roundTrip(42, new URL("https://github.com/EsotericSoftware/kryo"));
		roundTrip(78, new URL("https://github.com:443/EsotericSoftware/kryo/pulls?utf8=%E2%9C%93&q=is%3Apr"));
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
