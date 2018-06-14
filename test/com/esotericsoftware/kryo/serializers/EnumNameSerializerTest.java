
package com.esotericsoftware.kryo.serializers;

import java.util.EnumSet;

import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestSupport;
import com.esotericsoftware.kryo.TestKryoFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** @author KwonNam Son <kwon37xi@gmail.com> */
public class EnumNameSerializerTest {
	private final Kryo kryo = new TestKryoFactory().create();
	private final KryoTestSupport support = new KryoTestSupport(kryo);

	@Test
	public void testEnumNameSerializer () {
		kryo.addDefaultSerializer(Enum.class, EnumNameSerializer.class);
		kryo.register(TestNameEnum.class);
		kryo.register(TestAnotherNameEnum.class);

		// 1 byte for identifying class name,
		// rest bytes for enum's name
		support.roundTrip(6, 6, TestNameEnum.HELLO);
		support.roundTrip(5, 5, TestNameEnum.KRYO);

		support.roundTrip(7, 7, TestAnotherNameEnum.SUNDAY);
		support.roundTrip(8, 8, TestAnotherNameEnum.TUESDAY);

		{
			Kryo kryo = new Kryo();
			kryo.addDefaultSerializer(Enum.class, EnumNameSerializer.class);
			kryo.setRegistrationRequired(false);
			KryoTestSupport support = new KryoTestSupport(kryo);

			support.roundTrip(84, 84, TestNameEnum.WORLD);
			support.roundTrip(92, 92, TestAnotherNameEnum.MONDAY);
		}

	}

	@Test
	public void testEnumSetSerializerWithEnumNameSerializer () throws Exception {
		kryo.addDefaultSerializer(Enum.class, EnumNameSerializer.class);
		kryo.register(EnumSet.class);
		kryo.register(TestNameEnum.class);

		// support.roundTrip for EnumSet with EnumNameSerializer does not work.

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
		support.roundTrip(3, 6, EnumSet.noneOf(TestNameEnum.class));
	}

	@Test
	public void testEnumNameSerializerWithMethods () {
		kryo.addDefaultSerializer(Enum.class, EnumNameSerializer.class);

		kryo.register(TestNameEnumWithMethods.class);
		support.roundTrip(6, 6, TestNameEnumWithMethods.ALPHA);
		support.roundTrip(5, 5, TestNameEnumWithMethods.BETA);

		{
			Kryo kryo = new Kryo();
			kryo.addDefaultSerializer(Enum.class, EnumNameSerializer.class);
			kryo.setRegistrationRequired(false);
			KryoTestSupport support = new KryoTestSupport(kryo);

			support.roundTrip(97, 97, TestNameEnumWithMethods.ALPHA);
			support.roundTrip(96, 96, TestNameEnumWithMethods.BETA);
		}

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
