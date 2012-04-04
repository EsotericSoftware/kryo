
package com.esotericsoftware.kryo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class DefaultSerializersTest extends KryoTestCase {
	public void testBoolean () {
		roundTrip(2, true);
		roundTrip(2, false);
	}

	public void testByte () {
		roundTrip(2, (byte)1);
		roundTrip(2, (byte)125);
		roundTrip(2, (byte)-125);
	}

	public void testChar () {
		roundTrip(3, 'a');
		roundTrip(3, 'z');
	}

	public void testDouble () {
		roundTrip(9, 0d);
		roundTrip(9, 1234d);
		roundTrip(9, 1234.5678d);
	}

	public void testFloat () {
		roundTrip(5, 0f);
		roundTrip(5, 123f);
		roundTrip(5, 123.456f);
	}

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
	}

	public void testShort () {
		roundTrip(3, (short)0);
		roundTrip(3, (short)123);
		roundTrip(3, (short)123);
		roundTrip(3, (short)-123);
		roundTrip(3, (short)250);
		roundTrip(3, (short)123);
		roundTrip(3, (short)400);
	}

	public void testString () {
		kryo.setReferences(true);
		roundTrip(7, "meow");
		roundTrip(69, "abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdef");

		kryo.setReferences(false);
		roundTrip(6, "meow");

		roundTrip(3, "a");
		roundTrip(3, "\n");
		roundTrip(2, "");
		roundTrip(99, "ABCDEFGHIJKLMNOPQRSTUVWXYZ\rabcdefghijklmnopqrstuvwxyz\n1234567890\t\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*");

		roundTrip(21, "abcdef\u00E1\u00E9\u00ED\u00F3\u00FA\u7C9F");
	}

	public void testChars () {
		kryo.register(String.class, new Serializer<String>() {
			public void write (Kryo kryo, Output output, String object) {
				output.writeChars(object);
			}

			public String read (Kryo kryo, Input input, Class<String> type) {
				return input.readChars();
			}
		});

		kryo.setReferences(true);
		roundTrip(7, "meow");
		roundTrip(69, "abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdef");

		kryo.setReferences(false);
		roundTrip(6, "meow");

		roundTrip(3, "a");
		roundTrip(3, "\n");
		roundTrip(2, "");
		roundTrip(99, "ABCDEFGHIJKLMNOPQRSTUVWXYZ\rabcdefghijklmnopqrstuvwxyz\n1234567890\t\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*");
	}

	public void testNull () {
		kryo.setReferences(true);
		roundTrip(1, null);

		kryo.setReferences(false);
		roundTrip(1, null);
	}

	public void testDateSerializer () {
		kryo.register(Date.class);
		roundTrip(2, new Date(0));
		roundTrip(4, new Date(1234567));
		roundTrip(11, new Date(-1234567));
	}

	public void testBigDecimalSerializer () {
		kryo.register(BigDecimal.class);
		roundTrip(5, BigDecimal.valueOf(12345, 2));
	}

	public void testBigIntegerSerializer () {
		kryo.register(BigInteger.class);
		roundTrip(8, BigInteger.valueOf(1270507903945L));
	}

	public void testEnumSerializer () {
		kryo.register(TestEnum.class);
		roundTrip(2, TestEnum.a);
		roundTrip(2, TestEnum.b);
		roundTrip(2, TestEnum.c);

		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		// 1 byte identifying it's a class name
		// 1 byte for the class name id
		// 1 byte for the class name string length
		// 57 bytes for the class name characters
		// 1 byte for the reference id
		// 1 byte for the enum value
		roundTrip(62, TestEnum.c);
	}

	public void testEnumSerializerWithMethods () {
		kryo.register(TestEnumWithMethods.class);
		roundTrip(2, TestEnumWithMethods.a);
		roundTrip(2, TestEnumWithMethods.b);
		roundTrip(2, TestEnumWithMethods.c);

		kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		roundTrip(75, TestEnumWithMethods.c);
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
}
