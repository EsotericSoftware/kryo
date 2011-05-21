
package com.esotericsoftware.kryo2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.esotericsoftware.kryo.KryoTestCase;

import junit.framework.TestCase;

public class StreamBufferTest extends KryoTestCase {
	public void testOutputStream () {
		runOutputStreamTest(new OutputStreamBuffer(new ByteArrayOutputStream(), 512));
		runOutputStreamTest(new OutputStreamBuffer(new ByteArrayOutputStream(), 16));
		runOutputStreamTest(new OutputStreamBuffer(new ByteArrayOutputStream(), 4));
		runOutputStreamTest(new OutputStreamBuffer(new ByteArrayOutputStream(), 1));
		runOutputStreamTest(new OutputStreamBuffer(new ByteArrayOutputStream(), 17, 8));
	}

	private void runOutputStreamTest (OutputStreamBuffer buffer) {
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

	public void testRoundTripBytes () {
		byte[] array1 = new byte[] {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};
		byte[] array2 = new byte[] {31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46};
		byte[] array3 = new byte[] {52, 53, 54, 55, 56, 57, 58};

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		OutputStreamBuffer output = new OutputStreamBuffer(bytes);
		output.writeBytes(array1);
		output.writeBytes(array2);
		output.writeByte(51);
		output.writeBytes(array3);
		output.writeByte(61);
		output.writeByte(62);
		output.writeByte(63);
		output.writeByte(64);
		output.writeByte(65);
		output.writeByte(240);
		output.close();

		byte[] actual1 = new byte[16];
		byte[] actual2 = new byte[16];
		byte[] actual3 = new byte[7];
		InputStreamBuffer input = new InputStreamBuffer(new ByteArrayInputStream(bytes.toByteArray()));
		input.readBytes(actual1);
		assertEquals(array1, actual1);
		input.readBytes(actual2);
		assertEquals(array2, actual2);
		assertEquals(51, input.readByte());
		input.readBytes(actual3);
		assertEquals(array3, actual3);
		assertEquals(61, input.readByte());
		assertEquals(62, input.readByte());
		assertEquals(63, input.readByte());
		assertEquals(64, input.readByte());
		assertEquals(65, input.readByte());
		assertEquals(240, input.readUnsignedByte());
	}
}
