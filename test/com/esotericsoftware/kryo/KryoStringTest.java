
package com.esotericsoftware.kryo;

public class KryoStringTest extends KryoTestCase {
	public void testKryotring () {
		kryo.register(Test.class);
		kryo.register(KryoString.class);

		Test test = new Test();
		roundTrip(2, test);

		test.string = new KryoString();
		roundTrip(3, test);

		test.string.setValue("abcdefg");
		roundTrip(10, test);

		test.string = new KryoString("abcdefg");
		roundTrip(10, test);

		assertEquals(test.string.toString(), "abcdefg");
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
}
