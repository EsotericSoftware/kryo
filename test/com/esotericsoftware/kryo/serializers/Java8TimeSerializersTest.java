/* Copyright (c) 2016, Martin Grotzke
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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Before;
import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestSupport;
import com.esotericsoftware.kryo.TestKryoFactory;

/** Test for java 8 java.time.* serializers. Excluded from surefire tests via the "until-java8" profile in pom.xml which excludes
 * "Java8*Tests". */
public class Java8TimeSerializersTest {

	private final Kryo kryo = new TestKryoFactory().create();
	private final KryoTestSupport support = new KryoTestSupport(kryo);

	@Before
	public void setUp () throws Exception {
		kryo.register(Duration.class);
		kryo.register(Instant.class);
		kryo.register(LocalDate.class);
		kryo.register(LocalTime.class);
		kryo.register(LocalDateTime.class);
		kryo.register(ZoneOffset.class);
		kryo.register(ZoneId.of("UTC+01:00").getClass()); // ZoneRegion
		kryo.register(ZoneId.class);
		kryo.register(OffsetTime.class);
		kryo.register(OffsetDateTime.class);
		kryo.register(ZonedDateTime.class);
		kryo.register(Year.class);
		kryo.register(YearMonth.class);
		kryo.register(MonthDay.class);
		kryo.register(Period.class);
	}

	@Test
	public void testDuration () {
		support.roundTrip(14, 13, Duration.ofSeconds(-42, -23));
		support.roundTrip(10, 13, Duration.ofSeconds(42, 23));
		support.roundTrip(10, 13, Duration.ofSeconds(60 * 60 * 24 * 1000, -999999999));
		support.roundTrip(10, 13, Duration.ofSeconds(60 * 60 * 24 * 1000, 1000000001));
	}

	@Test
	public void testInstant () {
		support.roundTrip(7, 13, Instant.ofEpochSecond(42, -23));
		support.roundTrip(3, 13, Instant.ofEpochSecond(42, 23));
		support.roundTrip(7, 13, Instant.ofEpochSecond(1456662120, -999999999));
		support.roundTrip(7, 13, Instant.ofEpochSecond(1456662120, 1000000001));
	}

	@Test
	public void testLocalDate () {
		support.roundTrip(8, 7, LocalDate.of(Year.MIN_VALUE, Month.JANUARY, 1));
		support.roundTrip(5, 7, LocalDate.of(2015, 12, 31));
		support.roundTrip(8, 7, LocalDate.of(Year.MAX_VALUE, Month.DECEMBER, 31));
	}

	@Test
	public void testLocalTime () {
		support.roundTrip(2, 2, LocalTime.of(0, 0, 0, 0));
		support.roundTrip(2, 2, LocalTime.of(1, 0, 0, 0));
		support.roundTrip(3, 3, LocalTime.of(1, 1, 0, 0));
		support.roundTrip(4, 4, LocalTime.of(1, 1, 1, 0));
		support.roundTrip(5, 8, LocalTime.of(1, 1, 1, 1));
		support.roundTrip(9, 8, LocalTime.of(23, 59, 59, 999999999));
	}

	@Test
	public void testLocalDateTime () {
		support.roundTrip(9, 8, LocalDateTime.of(Year.MIN_VALUE, Month.JANUARY, 1, 0, 0, 0, 0));
		support.roundTrip(16, 14, LocalDateTime.of(Year.MAX_VALUE, Month.DECEMBER, 31, 23, 59, 59, 999999999));
	}

	@Test
	public void testZoneOffset () {
		support.roundTrip(2, 2, ZoneOffset.UTC);
		support.roundTrip(2, 2, ZoneOffset.MIN);
		support.roundTrip(2, 2, ZoneOffset.MAX);
	}

	@Test
	public void testZoneId () {
		// Type 1, ID is that from {@code ZoneOffset}
		// -> already tested with testZoneOffset

		// Type 2, offset-style IDs with some form of prefix, such as 'GMT+2' or 'UTC+01:00'.
		// The recognised prefixes are 'UTC', 'GMT' and 'UT'.
		support.roundTrip(10, 10, ZoneId.of("UTC+01:00"));

		// Type 3, region-based IDs. A region-based ID must be of two or more characters, and not start with
		// 'UTC', 'GMT', 'UT' '+' or '-'.
		support.roundTrip(14, 14, ZoneId.of("Europe/Berlin"));
	}

	@Test
	public void testOffsetTime () {
		support.roundTrip(3, 3, OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC));
		support.roundTrip(3, 3, OffsetTime.of(1, 0, 0, 0, ZoneOffset.UTC));
		support.roundTrip(4, 4, OffsetTime.of(1, 1, 0, 0, ZoneOffset.UTC));
		support.roundTrip(5, 5, OffsetTime.of(1, 1, 1, 0, ZoneOffset.UTC));
		support.roundTrip(6, 9, OffsetTime.of(1, 1, 1, 1, ZoneOffset.UTC));
		support.roundTrip(10, 9, OffsetTime.of(23, 59, 59, 999999999, ZoneOffset.UTC));
	}

	@Test
	public void testOffsetDateTime () {
		support.roundTrip(10, 9, OffsetDateTime.of(Year.MIN_VALUE, Month.JANUARY.getValue(), 1, 0, 0, 0, 0, ZoneOffset.UTC));
		support.roundTrip(17, 15,
			OffsetDateTime.of(Year.MAX_VALUE, Month.DECEMBER.getValue(), 31, 23, 59, 59, 999999999, ZoneOffset.UTC));
	}

	@Test
	public void testZonedDateTime () {
		support.roundTrip(11, 10, ZonedDateTime.of(Year.MIN_VALUE, Month.JANUARY.getValue(), 1, 0, 0, 0, 0, ZoneOffset.UTC));
		support.roundTrip(22, 21,
			ZonedDateTime.of(Year.MIN_VALUE, Month.JANUARY.getValue(), 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")));
		support.roundTrip(29, 27,
			ZonedDateTime.of(Year.MAX_VALUE, Month.DECEMBER.getValue(), 31, 23, 59, 59, 999999999, ZoneId.of("Europe/Berlin")));
	}

	@Test
	public void testYear () {
		support.roundTrip(6, 5, Year.of(Year.MIN_VALUE));
		support.roundTrip(6, 5, Year.of(Year.MAX_VALUE));
		support.roundTrip(3, 5, Year.of(2016));
	}

	@Test
	public void testYearMonth () {
		support.roundTrip(7, 6, YearMonth.of(Year.MIN_VALUE, Month.JANUARY));
		support.roundTrip(7, 6, YearMonth.of(Year.MAX_VALUE, Month.DECEMBER));
		support.roundTrip(4, 6, YearMonth.of(2016, Month.FEBRUARY));
	}

	@Test
	public void testMonthDay () {
		support.roundTrip(3, 3, MonthDay.of(Month.JANUARY, 1));
		support.roundTrip(3, 3, MonthDay.of(Month.DECEMBER, 31));
	}

	@Test
	public void testPeriod () {
		support.roundTrip(4, 13, Period.ZERO);
		support.roundTrip(16, 13, Period.of(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE));
		support.roundTrip(16, 13, Period.of(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
	}


}
