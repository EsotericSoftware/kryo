
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;

public class KryoStringTest extends KryoTestCase {
	public void testKryoString () {
		kryo.register(Test.class);
		kryo.register(KryoString.class);

		Test test = new Test();
		test.string = new KryoString();
		roundTrip(2, test);

		test.string.setValue("abcdefg");
		roundTrip(9, test);

		test.string = new KryoString("abcdefg");
		roundTrip(9, test);

		assertEquals(test.string.toString(), "abcdefg");
	}

	public void testKryoStringToString () {
		kryo.register(Test.class);
		kryo.register(Test2.class);
		kryo.register(KryoString.class);

		// Serialize Test.
		Test test = new Test();
		test.string = new KryoString();
		test.string.setValue("abcdefg");
		roundTrip(9, test);

		// Deserialize Test2.
		Input input = new Input(output.toBytes());
		kryo.readClass(input);
		Test2 test2 = kryo.readObject(input, Test2.class);
		assertEquals(test.string.toString(), test2.string);
	}

	public void testStringToKryoString () {
		kryo.register(Test.class);
		kryo.register(Test2.class);
		kryo.register(KryoString.class);

		// Serialize Test2.
		Test2 test2 = new Test2();
		test2.string = "abcdefg";
		roundTrip(9, test2);

		// Deserialize Test.
		Input input = new Input(output.toBytes());
		kryo.readClass(input);
		Test2 test = kryo.readObject(input, Test2.class);
		assertEquals(test.string.toString(), test.string);
	}

	public void testUnicode () {
		kryo.register(KryoString.class);
		// FIXME - Unicode is currently FUBAR!
		roundTrip(12, new KryoString("abcdef\u00E1\u00E9\u00ED\u00F3\u00FA\u1234"));
	}
	
	static public class Test {
		KryoString string;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Test other = (Test)obj;
			if (string == null) {
				if (other.string != null) return false;
			} else if (!string.equals(other.string)) return false;
			return true;
		}
	}

	static public class Test2 {
		String string;

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Test2 other = (Test2)obj;
			if (string == null) {
				if (other.string != null) return false;
			} else if (!string.equals(other.string)) return false;
			return true;
		}
	}
}
