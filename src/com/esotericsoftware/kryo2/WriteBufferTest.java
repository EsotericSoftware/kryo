
package com.esotericsoftware.kryo2;

import com.esotericsoftware.kryo.KryoTestCase;

import junit.framework.TestCase;

public class WriteBufferTest extends KryoTestCase {
	public void testGrowBuffers () {
		WriteBuffer buffer = new WriteBuffer(4, 8, 16);
		buffer.writeBytes(new byte[] {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26});
		buffer.writeBytes(new byte[] {31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46});
		buffer.writeBytes(new byte[] {51, 52, 53, 54, 55, 56, 57, 58});
		buffer.writeBytes(new byte[] {61, 62, 63, 64, 65});
		buffer.flush();

		assertEquals(buffer.toBytes(), new byte[] { //
			11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, //
				31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, //
				51, 52, 53, 54, 55, 56, 57, 58, //
				61, 62, 63, 64, 65});
	}

	public void testPopBuffers () {
		WriteBuffer buffer = new WriteBuffer(4, 8, 16);
		buffer.writeBytes(new byte[] {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26});
		buffer.writeBytes(new byte[] {31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46});
		buffer.writeBytes(new byte[] {51, 52, 53, 54, 55, 56, 57, 58});
		buffer.writeBytes(new byte[] {61, 62, 63, 64});
		buffer.flush();

		byte[] expected = new byte[] { //
		11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, //
			31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, //
			51, 52, 53, 54, 55, 56, 57, 58, //
			61, 62, 63, 64};
		assertEquals(expected.length, buffer.toBytes().length);
		for (int i = 0; i < expected.length;) {
			byte[] bytes = buffer.popBytes();
			assertEquals(4, bytes.length);
			assertEquals(expected[i++], bytes[0]);
			assertEquals(expected[i++], bytes[1]);
			assertEquals(expected[i++], bytes[2]);
			assertEquals(expected[i++], bytes[3]);
		}
	}

	public void testMarks () {
		WriteBuffer buffer = new WriteBuffer(512);
		buffer.writeBytes(new byte[] {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26});

		int start = buffer.mark();
		buffer.writeBytes(new byte[] {31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46});
		int end = buffer.mark();

		buffer.positionToMark(start);
		buffer.writeBytes(new byte[] {51, 52, 53, 54, 55, 56, 57, 58});
		buffer.positionToMark(end);

		buffer.writeBytes(new byte[] {61, 62, 63, 64, 65});
		buffer.flush();

		assertEquals(buffer.toBytes(), new byte[] { //
			11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, //
				51, 52, 53, 54, 55, 56, 57, 58, 39, 40, 41, 42, 43, 44, 45, 46, //
				61, 62, 63, 64, 65});
	}

	public void testBytes () {
		WriteBuffer buffer = new WriteBuffer(512);
		buffer.writeBytes(new byte[] {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26});
		buffer.writeBytes(new byte[] {31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46});
		buffer.writeByte(51);
		buffer.writeBytes(new byte[] {52, 53, 54, 55, 56, 57, 58});
		buffer.writeByte(61);
		buffer.writeByte(62);
		buffer.writeByte(63);
		buffer.writeByte(64);
		buffer.writeByte(65);
		buffer.flush();

		assertEquals(buffer.toBytes(), new byte[] { //
			11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, //
				31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, //
				51, 52, 53, 54, 55, 56, 57, 58, //
				61, 62, 63, 64, 65});
	}
}
