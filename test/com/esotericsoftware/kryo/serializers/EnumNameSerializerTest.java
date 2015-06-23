
package com.esotericsoftware.kryo.serializers;

import java.util.EnumSet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** @author KwonNam Son <kwon37xi@gmail.com> */
public class EnumNameSerializerTest extends KryoTestCase {
	public void testEnumNameSerializer () {
		kryo.addDefaultSerializer(Enum.class, EnumNameSerializer.class);
		kryo.register(TestNameEnum.class);
		kryo.register(TestAnotherNameEnum.class);

		// 1 byte for identifying class name,
		// rest bytes for enum's name
		roundTrip(6, 6, TestNameEnum.HELLO);
		roundTrip(5, 5, TestNameEnum.KRYO);

		roundTrip(7, 7, TestAnotherNameEnum.SUNDAY);
		roundTrip(8, 8, TestAnotherNameEnum.TUESDAY);

		kryo = new Kryo();
		kryo.addDefaultSerializer(Enum.class, EnumNameSerializer.class);
		kryo.setRegistrationRequired(false);

		roundTrip(84, 84, TestNameEnum.WORLD);
		roundTrip(92, 92, TestAnotherNameEnum.MONDAY);
	}

	public void testEnumSetSerializerWithEnumNameSerializer () throws Exception {
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
		roundTrip(3, 6, EnumSet.noneOf(TestNameEnum.class));
	}

	public void testEnumNameSerializerWithMethods () {
		kryo.addDefaultSerializer(Enum.class, EnumNameSerializer.class);

		kryo.register(TestNameEnumWithMethods.class);
		roundTrip(6, 6, TestNameEnumWithMethods.ALPHA);
		roundTrip(5, 5, TestNameEnumWithMethods.BETA);

		kryo = new Kryo();
		kryo.addDefaultSerializer(Enum.class, EnumNameSerializer.class);
		kryo.setRegistrationRequired(false);

		roundTrip(97, 97, TestNameEnumWithMethods.ALPHA);
		roundTrip(96, 96, TestNameEnumWithMethods.BETA);
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
