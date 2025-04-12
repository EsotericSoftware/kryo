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

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.SerializerFactory.FieldSerializerFactory;

import org.junit.jupiter.api.Test;

/** Created by phamrak on 8.6.2016. */
class FieldSerializerInheritanceTest extends KryoTestCase {
	@Test
	void testDefaultStrategyForDefaultClass () {
		TestDefault testDefault = new TestDefault();
		testDefault.a = "someDefaultValue";
		kryo.setDefaultSerializer(FieldSerializer.class);
		kryo.register(TestDefault.class);

		roundTrip(17, testDefault);

		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(TestDefault.class);
		assertNotNull(serializer.getField("a"));
		serializer.removeField("a");
		assertFieldRemoved(serializer, "a");
	}

	@Test
	void testDefaultStrategyForExtendedClass () {
		TestExtended testExtended = new TestExtended();
		((TestDefault)testExtended).a = "someDefaultValue";
		testExtended.a = "someExtendedValue";
		kryo.setDefaultSerializer(FieldSerializer.class);
		kryo.register(TestExtended.class);

		roundTrip(34, testExtended);

		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(TestExtended.class);

		// the "a" field needs to be removed 2x, once for TestDefault.a and once for TestExtended.a. You
		// can't remove the second one without removing the first one (in DEFAULT field name strategy)
		assertNotNull(serializer.getField("a"));
		serializer.removeField("a");
		assertNotNull(serializer.getField("a"));
		serializer.removeField("a");
		assertFieldRemoved(serializer, "a");
	}

	@Test
	void testExtendedStrategyForExtendedClass () {
		TestExtended testExtended = new TestExtended();
		((TestDefault)testExtended).a = "someDefaultValue";
		testExtended.a = "someExtendedValue";
		FieldSerializerFactory factory = new FieldSerializerFactory();
		factory.getConfig().setExtendedFieldNames(true);
		kryo.setDefaultSerializer(factory);
		kryo.register(TestExtended.class);

		roundTrip(34, testExtended);

		FieldSerializer serializer = (FieldSerializer)kryo.getSerializer(TestExtended.class);

		// Simple class name is part of field name in EXTENDED field name strategy.
		assertNotNull(serializer.getField("TestDefault.a"));
		serializer.removeField("TestDefault.a");
		assertFieldRemoved(serializer, "TestDefault.a");
		assertNotNull(serializer.getField("TestExtended.a"));
		serializer.removeField("TestExtended.a");
		assertFieldRemoved(serializer, "TestExtended.a");
	}

	private void assertFieldRemoved (FieldSerializer serializer, String fieldName) {
		try {
			assertNull(serializer.getField(fieldName));
			fail("Expected IllegalArgumentException to be thrown for serializer.getField(" + fieldName + ")");
		} catch (IllegalArgumentException iae) {
			assertTrue(true);
		}
	}

	public static class TestDefault {
		String a;

		public String getA () {
			return a;
		}

		public void setA (String a) {
			this.a = a;
		}

		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			TestDefault that = (TestDefault)o;

			return a != null ? a.equals(that.a) : that.a == null;

		}

		public int hashCode () {
			return a != null ? a.hashCode() : 0;
		}
	}

	public static class TestExtended extends TestDefault {
		String a;

		public String getA () {
			return a;
		}

		public void setA (String a) {
			this.a = a;
		}

		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			if (!super.equals(o)) return false;

			TestExtended that = (TestExtended)o;
			return a != null ? a.equals(that.a) : that.a == null;
		}

		public int hashCode () {
			return a != null ? a.hashCode() : 0;
		}
	}
}
