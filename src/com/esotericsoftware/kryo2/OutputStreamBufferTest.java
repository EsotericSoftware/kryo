
package com.esotericsoftware.kryo2;

import java.io.ByteArrayOutputStream;

import com.esotericsoftware.kryo.KryoTestCase;

import junit.framework.TestCase;

public class OutputStreamBufferTest extends KryoTestCase {
	public void testOutputStream () {
		run(new OutputStreamBuffer(new ByteArrayOutputStream(), 512));
		run(new OutputStreamBuffer(new ByteArrayOutputStream(), 16));
		run(new OutputStreamBuffer(new ByteArrayOutputStream(), 4));
		run(new OutputStreamBuffer(new ByteArrayOutputStream(), 1));
		run(new OutputStreamBuffer(new ByteArrayOutputStream(), 17, 8));
	}

	private void run (OutputStreamBuffer buffer) {
		buffer.writeBytes(new byte[] {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26});
		buffer.writeBytes(new byte[] {31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46});
		buffer.writeByte(51);
		buffer.writeBytes(new byte[] {52, 53, 54, 55, 56, 57, 58});
		buffer.writeByte(61);
		buffer.writeByte(62);
		buffer.writeByte(63);
		buffer.writeByte(64);
		buffer.writeByte(65);
		buffer.close();

		assertEquals(((ByteArrayOutputStream)buffer.getOutputStream()).toByteArray(), new byte[] { //
			11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, //
				31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, //
				51, 52, 53, 54, 55, 56, 57, 58, //
				61, 62, 63, 64, 65});
	}
}
