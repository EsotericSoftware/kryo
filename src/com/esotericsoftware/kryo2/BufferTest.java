
package com.esotericsoftware.kryo2;

import com.esotericsoftware.kryo.KryoTestCase;

public class BufferTest extends KryoTestCase {
	public void testStrings () {
		runStringTest(new WriteBuffer());
		runStringTest(new WriteBuffer(2, 200));
	}

	public void runStringTest (WriteBuffer write) {
		String value1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ\rabcdefghijklmnopqrstuvwxyz\n1234567890\t\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*";
		String value2 = "abcdef\u00E1\u00E9\u00ED\u00F3\u00FA\u1234";

		write.writeString("uno");
		write.writeString("dos");
		write.writeString("tres");
		write.writeString(value1);
		write.writeString(value2);

		ReadBuffer read = new ReadBuffer(write.toBytes());
		assertEquals("uno", read.readString());
		assertEquals("dos", read.readString());
		assertEquals("tres", read.readString());
		assertEquals(value1, read.readString());
		assertEquals(value2, read.readString());
	}

	public void testInts () {
		runIntTest(new WriteBuffer());
		runIntTest(new WriteBuffer(2, 200));
	}

	private void runIntTest (WriteBuffer write) {
		write.writeInt(0);
		write.writeInt(63);
		write.writeInt(64);
		write.writeInt(127);
		write.writeInt(128);
		write.writeInt(8192);
		write.writeInt(16384);
		write.writeInt(2097151);
		write.writeInt(1048575);
		write.writeInt(134217727);
		write.writeInt(268435455);
		write.writeInt(134217728);
		write.writeInt(268435456);
		write.writeInt(-2097151);
		write.writeInt(-1048575);
		write.writeInt(-134217727);
		write.writeInt(-268435455);
		write.writeInt(-134217728);
		write.writeInt(-268435456);
		assertEquals(1, write.writeInt(0, true));
		assertEquals(1, write.writeInt(0, false));
		assertEquals(1, write.writeInt(63, true));
		assertEquals(1, write.writeInt(63, false));
		assertEquals(1, write.writeInt(64, true));
		assertEquals(2, write.writeInt(64, false));
		assertEquals(1, write.writeInt(127, true));
		assertEquals(2, write.writeInt(127, false));
		assertEquals(2, write.writeInt(128, true));
		assertEquals(2, write.writeInt(128, false));
		assertEquals(2, write.writeInt(8191, true));
		assertEquals(2, write.writeInt(8191, false));
		assertEquals(2, write.writeInt(8192, true));
		assertEquals(3, write.writeInt(8192, false));
		assertEquals(2, write.writeInt(16383, true));
		assertEquals(3, write.writeInt(16383, false));
		assertEquals(3, write.writeInt(16384, true));
		assertEquals(3, write.writeInt(16384, false));
		assertEquals(3, write.writeInt(2097151, true));
		assertEquals(4, write.writeInt(2097151, false));
		assertEquals(3, write.writeInt(1048575, true));
		assertEquals(3, write.writeInt(1048575, false));
		assertEquals(4, write.writeInt(134217727, true));
		assertEquals(4, write.writeInt(134217727, false));
		assertEquals(4, write.writeInt(268435455, true));
		assertEquals(5, write.writeInt(268435455, false));
		assertEquals(4, write.writeInt(134217728, true));
		assertEquals(5, write.writeInt(134217728, false));
		assertEquals(5, write.writeInt(268435456, true));
		assertEquals(5, write.writeInt(268435456, false));
		assertEquals(1, write.writeInt(-64, false));
		assertEquals(5, write.writeInt(-64, true));
		assertEquals(2, write.writeInt(-65, false));
		assertEquals(5, write.writeInt(-65, true));
		assertEquals(2, write.writeInt(-8192, false));
		assertEquals(5, write.writeInt(-8192, true));
		assertEquals(3, write.writeInt(-1048576, false));
		assertEquals(5, write.writeInt(-1048576, true));
		assertEquals(4, write.writeInt(-134217728, false));
		assertEquals(5, write.writeInt(-134217728, true));
		assertEquals(5, write.writeInt(-134217729, false));
		assertEquals(5, write.writeInt(-134217729, true));

		ReadBuffer read = new ReadBuffer(write.toBytes());
		assertEquals(0, read.readInt());
		assertEquals(63, read.readInt());
		assertEquals(64, read.readInt());
		assertEquals(127, read.readInt());
		assertEquals(128, read.readInt());
		assertEquals(8192, read.readInt());
		assertEquals(16384, read.readInt());
		assertEquals(2097151, read.readInt());
		assertEquals(1048575, read.readInt());
		assertEquals(134217727, read.readInt());
		assertEquals(268435455, read.readInt());
		assertEquals(134217728, read.readInt());
		assertEquals(268435456, read.readInt());
		assertEquals(-2097151, read.readInt());
		assertEquals(-1048575, read.readInt());
		assertEquals(-134217727, read.readInt());
		assertEquals(-268435455, read.readInt());
		assertEquals(-134217728, read.readInt());
		assertEquals(-268435456, read.readInt());
		assertEquals(true, read.canReadInt());
		assertEquals(true, read.canReadInt());
		assertEquals(true, read.canReadInt());
		assertEquals(0, read.readInt(true));
		assertEquals(0, read.readInt(false));
		assertEquals(63, read.readInt(true));
		assertEquals(63, read.readInt(false));
		assertEquals(64, read.readInt(true));
		assertEquals(64, read.readInt(false));
		assertEquals(127, read.readInt(true));
		assertEquals(127, read.readInt(false));
		assertEquals(128, read.readInt(true));
		assertEquals(128, read.readInt(false));
		assertEquals(8191, read.readInt(true));
		assertEquals(8191, read.readInt(false));
		assertEquals(8192, read.readInt(true));
		assertEquals(8192, read.readInt(false));
		assertEquals(16383, read.readInt(true));
		assertEquals(16383, read.readInt(false));
		assertEquals(16384, read.readInt(true));
		assertEquals(16384, read.readInt(false));
		assertEquals(2097151, read.readInt(true));
		assertEquals(2097151, read.readInt(false));
		assertEquals(1048575, read.readInt(true));
		assertEquals(1048575, read.readInt(false));
		assertEquals(134217727, read.readInt(true));
		assertEquals(134217727, read.readInt(false));
		assertEquals(268435455, read.readInt(true));
		assertEquals(268435455, read.readInt(false));
		assertEquals(134217728, read.readInt(true));
		assertEquals(134217728, read.readInt(false));
		assertEquals(268435456, read.readInt(true));
		assertEquals(268435456, read.readInt(false));
		assertEquals(-64, read.readInt(false));
		assertEquals(-64, read.readInt(true));
		assertEquals(-65, read.readInt(false));
		assertEquals(-65, read.readInt(true));
		assertEquals(-8192, read.readInt(false));
		assertEquals(-8192, read.readInt(true));
		assertEquals(-1048576, read.readInt(false));
		assertEquals(-1048576, read.readInt(true));
		assertEquals(-134217728, read.readInt(false));
		assertEquals(-134217728, read.readInt(true));
		assertEquals(-134217729, read.readInt(false));
		assertEquals(-134217729, read.readInt(true));
		assertEquals(false, read.canReadInt());
	}
}
