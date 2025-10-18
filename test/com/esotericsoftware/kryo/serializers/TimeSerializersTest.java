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

import com.esotericsoftware.kryo.KryoTestCase;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test for java 8 java.time.* serializers. */
class TimeSerializersTest extends KryoTestCase {

	@BeforeEach
	public void setUp () throws Exception {
		super.setUp();
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
	void testDuration () {
		roundTrip(14, Duration.ofSeconds(-42, -23));
		roundTrip(10, Duration.ofSeconds(42, 23));
		roundTrip(10, Duration.ofSeconds(60 * 60 * 24 * 1000, -999999999));
		roundTrip(10, Duration.ofSeconds(60 * 60 * 24 * 1000, 1000000001));
	}

	@Test
	void testInstant () {
		roundTrip(7, Instant.ofEpochSecond(42, -23));
		roundTrip(3, Instant.ofEpochSecond(42, 23));
		roundTrip(7, Instant.ofEpochSecond(1456662120, -999999999));
		roundTrip(7, Instant.ofEpochSecond(1456662120, 1000000001));
	}

	@Test
	void testLocalDate () {
		roundTrip(8, LocalDate.of(Year.MIN_VALUE, Month.JANUARY, 1));
		roundTrip(5, LocalDate.of(2015, 12, 31));
		roundTrip(8, LocalDate.of(Year.MAX_VALUE, Month.DECEMBER, 31));
	}

	@Test
	void testLocalTime () {
		roundTrip(2, LocalTime.of(0, 0, 0, 0));
		roundTrip(2, LocalTime.of(1, 0, 0, 0));
		roundTrip(3, LocalTime.of(1, 1, 0, 0));
		roundTrip(4, LocalTime.of(1, 1, 1, 0));
		roundTrip(5, LocalTime.of(1, 1, 1, 1));
		roundTrip(9, LocalTime.of(23, 59, 59, 999999999));
	}

	@Test
	void testLocalDateTime () {
		roundTrip(9, LocalDateTime.of(Year.MIN_VALUE, Month.JANUARY, 1, 0, 0, 0, 0));
		roundTrip(16, LocalDateTime.of(Year.MAX_VALUE, Month.DECEMBER, 31, 23, 59, 59, 999999999));
	}

	@Test
	void testZoneOffset () {
		roundTrip(2, ZoneOffset.UTC);
		roundTrip(2, ZoneOffset.MIN);
		roundTrip(2, ZoneOffset.MAX);
	}

	@Test
	void testZoneId () {
		// Type 1, ID is that from {@code ZoneOffset}
		// -> already tested with testZoneOffset

		// Type 2, offset-style IDs with some form of prefix, such as 'GMT+2' or 'UTC+01:00'.
		// The recognised prefixes are 'UTC', 'GMT' and 'UT'.
		roundTrip(10, ZoneId.of("UTC+01:00"));

		// Type 3, region-based IDs. A region-based ID must be of two or more characters, and not start with
		// 'UTC', 'GMT', 'UT' '+' or '-'.
		roundTrip(14, ZoneId.of("Europe/Berlin"));
	}

	@Test
	void testOffsetTime () {
		roundTrip(3, OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC));
		roundTrip(3, OffsetTime.of(1, 0, 0, 0, ZoneOffset.UTC));
		roundTrip(4, OffsetTime.of(1, 1, 0, 0, ZoneOffset.UTC));
		roundTrip(5, OffsetTime.of(1, 1, 1, 0, ZoneOffset.UTC));
		roundTrip(6, OffsetTime.of(1, 1, 1, 1, ZoneOffset.UTC));
		roundTrip(10, OffsetTime.of(23, 59, 59, 999999999, ZoneOffset.UTC));
	}

	@Test
	void testOffsetDateTime () {
		roundTrip(10, OffsetDateTime.of(Year.MIN_VALUE, Month.JANUARY.getValue(), 1, 0, 0, 0, 0, ZoneOffset.UTC));
		roundTrip(17, OffsetDateTime.of(Year.MAX_VALUE, Month.DECEMBER.getValue(), 31, 23, 59, 59, 999999999, ZoneOffset.UTC));
	}

	@Test
	void testZonedDateTime () {
		roundTrip(11, ZonedDateTime.of(Year.MIN_VALUE, Month.JANUARY.getValue(), 1, 0, 0, 0, 0, ZoneOffset.UTC));
		roundTrip(22, ZonedDateTime.of(Year.MIN_VALUE, Month.JANUARY.getValue(), 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")));
		roundTrip(29,
			ZonedDateTime.of(Year.MAX_VALUE, Month.DECEMBER.getValue(), 31, 23, 59, 59, 999999999, ZoneId.of("Europe/Berlin")));
	}

	@Test
	void testYear () {
		roundTrip(6, Year.of(Year.MIN_VALUE));
		roundTrip(6, Year.of(Year.MAX_VALUE));
		roundTrip(3, Year.of(2016));
	}

	@Test
	void testYearMonth () {
		roundTrip(7, YearMonth.of(Year.MIN_VALUE, Month.JANUARY));
		roundTrip(7, YearMonth.of(Year.MAX_VALUE, Month.DECEMBER));
		roundTrip(4, YearMonth.of(2016, Month.FEBRUARY));
	}

	@Test
	void testMonthDay () {
		roundTrip(3, MonthDay.of(Month.JANUARY, 1));
		roundTrip(3, MonthDay.of(Month.DECEMBER, 31));
	}

	@Test
	void testPeriod () {
		roundTrip(4, Period.ZERO);
		roundTrip(16, Period.of(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE));
		roundTrip(16, Period.of(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
	}

}
