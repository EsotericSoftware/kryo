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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

/** @author KwonNam Son <kwon37xi@gmail.com> */
class EnumNameSerializerTest extends KryoTestCase {
	@Test
	void testEnumNameSerializer () {
		kryo.addDefaultSerializer(Enum.class, EnumNameSerializer.class);
		kryo.register(TestNameEnum.class);
		kryo.register(TestAnotherNameEnum.class);

		// 1 byte for identifying class name,
		// rest bytes for enum's name
		roundTrip(6, TestNameEnum.HELLO);
		roundTrip(5, TestNameEnum.KRYO);

		roundTrip(7, TestAnotherNameEnum.SUNDAY);
		roundTrip(8, TestAnotherNameEnum.TUESDAY);

		kryo = new Kryo();
		kryo.addDefaultSerializer(Enum.class, EnumNameSerializer.class);
		kryo.setRegistrationRequired(false);

		roundTrip(80, TestNameEnum.WORLD);
		roundTrip(88, TestAnotherNameEnum.MONDAY);
	}

	@Test
	void testEnumSetSerializerWithEnumNameSerializer () {
		kryo.addDefaultSerializer(Enum.class, EnumNameSerializer.class);
		kryo.register(EnumSet.class);
		kryo.register(TestNameEnum.class);

		// roundTrip for EnumSet with EnumNameSerializer does not work.

		// test directly
		Output output = new Output(1024);
		kryo.writeClassAndObject(output, EnumSet.of(TestNameEnum.HELLO, TestNameEnum.WORLD));
		byte[] bytes = output.toBytes();

		EnumSet<TestNameEnum> enumSet = (EnumSet<TestNameEnum>)kryo.readClassAndObject(new Input(bytes));
		assertEquals(enumSet.size(), 2);
		assertTrue(enumSet.contains(TestNameEnum.HELLO));
		assertTrue(enumSet.contains(TestNameEnum.WORLD));
		assertFalse(enumSet.contains(TestNameEnum.KRYO));

		// empty EnumSet
		roundTrip(3, EnumSet.noneOf(TestNameEnum.class));
	}

	@Test
	void testEnumNameSerializerWithMethods () {
		kryo.addDefaultSerializer(Enum.class, EnumNameSerializer.class);

		kryo.register(TestNameEnumWithMethods.class);
		roundTrip(6, TestNameEnumWithMethods.ALPHA);
		roundTrip(5, TestNameEnumWithMethods.BETA);

		kryo = new Kryo();
		kryo.addDefaultSerializer(Enum.class, EnumNameSerializer.class);
		kryo.setRegistrationRequired(false);

		roundTrip(93, TestNameEnumWithMethods.ALPHA);
		roundTrip(92, TestNameEnumWithMethods.BETA);
	}

	public enum TestNameEnum {
		HELLO, KRYO, WORLD
	}

	public enum TestAnotherNameEnum {
		SUNDAY, MONDAY, TUESDAY
	}

	public enum TestNameEnumWithMethods {
		ALPHA {
		},
		BETA {
		}
	}
}
