/* Copyright (c) 2008, Nathan Sweet
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

package com.esotericsoftware.kryo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import org.junit.Test;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.esotericsoftware.kryo.io.UnsafeOutput;

import static com.esotericsoftware.kryo.KryoTestUtil.assertDoubleEquals;
import static com.esotericsoftware.kryo.KryoTestUtil.assertFloatEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/** @author Nathan Sweet <misc@n4te.com> */
public class UnsafeInputOutputTest {
	@Test
	public void testOutputStream () throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		UnsafeOutput output = new UnsafeOutput(buffer, 2);
		output.writeBytes(new byte[] {11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26});
		output.writeBytes(new byte[] {31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46});
		output.writeBytes(new byte[] {51, 52, 53, 54, 55, 56, 57, 58});
		output.writeBytes(new byte[] {61, 62, 63, 64, 65});
		output.flush();

		assertArrayEquals(new byte[] { //
			11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, //
			31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, //
			51, 52, 53, 54, 55, 56, 57, 58, //
			61, 62, 63, 64, 65}, buffer.toByteArray());
	}

	@Test
	public void testInputStream () throws IOException {
		byte[] bytes = new byte[] { //
			11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, //
			31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, //
			51, 52, 53, 54, 55, 56, 57, 58, //
			61, 62, 63, 64, 65};
		ByteArrayInputStream buffer = new ByteArrayInputStream(bytes);
		Input input = new UnsafeInput(buffer, 2);
		byte[] temp = new byte[1024];
		int count = input.read(temp, 512, bytes.length);
		assertEquals(bytes.length, count);
		byte[] temp2 = new byte[count];
		System.arraycopy(temp, 512, temp2, 0, count);
		assertArrayEquals(bytes, temp2);

		input = new UnsafeInput(bytes);
		count = input.read(temp, 512, 512);
		assertEquals(bytes.length, count);
		temp2 = new byte[count];
		System.arraycopy(temp, 512, temp2, 0, count);
		assertArrayEquals(bytes, temp2);
	}

	@Test
	public void testWriteBytes () throws IOException {
		UnsafeOutput buffer = new UnsafeOutput(512);
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

		assertArrayEquals(new byte[] { //
			11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, //
			31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, //
			51, 52, 53, 54, 55, 56, 57, 58, //
			61, 62, 63, 64, 65}, buffer.toBytes());
	}

	@Test
	public void testStrings () throws IOException {
		runStringTest(new UnsafeOutput(4096));
		runStringTest(new UnsafeOutput(897));
		runStringTest(new UnsafeOutput(new ByteArrayOutputStream()));

		UnsafeOutput write = new UnsafeOutput(21);
		String value = "abcdef\u00E1\u00E9\u00ED\u00F3\u00FA\u1234";
		write.writeString(value);
		Input read = new UnsafeInput(write.toBytes());
		assertEquals(value, read.readString());

		runStringTest(127);
		runStringTest(256);
		runStringTest(1024 * 1023);
		runStringTest(1024 * 1024);
		runStringTest(1024 * 1025);
		runStringTest(1024 * 1026);
		runStringTest(1024 * 1024 * 2);
	}

	public void runStringTest (int length) throws IOException {
		UnsafeOutput write = new UnsafeOutput(1024, -1);
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < length; i++)
			buffer.append((char)i);

		String value = buffer.toString();
		write.writeString(value);
		write.writeString(value);
		Input read = new UnsafeInput(write.toBytes());
		assertEquals(value, read.readString());
		assertEquals(value, read.readStringBuilder().toString());

		write.clear();
		write.writeString(buffer);
		write.writeString(buffer);
		read = new UnsafeInput(write.toBytes());
		assertEquals(value, read.readStringBuilder().toString());
		assertEquals(value, read.readString());

		if (length <= 127) {
			write.clear();
			write.writeAscii(value);
			write.writeAscii(value);
			read = new UnsafeInput(write.toBytes());
			assertEquals(value, read.readStringBuilder().toString());
			assertEquals(value, read.readString());
		}
	}

	public void runStringTest (UnsafeOutput write) throws IOException {
		String value1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ\rabcdefghijklmnopqrstuvwxyz\n1234567890\t\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*";
		String value2 = "abcdef\u00E1\u00E9\u00ED\u00F3\u00FA\u1234";

		write.writeString("");
		write.writeString("1");
		write.writeString("22");
		write.writeString("uno");
		write.writeString("dos");
		write.writeString("tres");
		write.writeString(null);
		write.writeString(value1);
		write.writeString(value2);
		for (int i = 0; i < 127; i++)
			write.writeString(String.valueOf((char)i));
		for (int i = 0; i < 127; i++)
			write.writeString(String.valueOf((char)i) + "abc");

		Input read = new UnsafeInput(write.toBytes());
		assertEquals("", read.readString());
		assertEquals("1", read.readString());
		assertEquals("22", read.readString());
		assertEquals("uno", read.readString());
		assertEquals("dos", read.readString());
		assertEquals("tres", read.readString());
		assertEquals(null, read.readString());
		assertEquals(value1, read.readString());
		assertEquals(value2, read.readString());
		for (int i = 0; i < 127; i++)
			assertEquals(String.valueOf((char)i), read.readString());
		for (int i = 0; i < 127; i++)
			assertEquals(String.valueOf((char)i) + "abc", read.readString());

		read.rewind();
		assertEquals("", read.readStringBuilder().toString());
		assertEquals("1", read.readStringBuilder().toString());
		assertEquals("22", read.readStringBuilder().toString());
		assertEquals("uno", read.readStringBuilder().toString());
		assertEquals("dos", read.readStringBuilder().toString());
		assertEquals("tres", read.readStringBuilder().toString());
		assertEquals(null, read.readStringBuilder());
		assertEquals(value1, read.readStringBuilder().toString());
		assertEquals(value2, read.readStringBuilder().toString());
		for (int i = 0; i < 127; i++)
			assertEquals(String.valueOf((char)i), read.readStringBuilder().toString());
		for (int i = 0; i < 127; i++)
			assertEquals(String.valueOf((char)i) + "abc", read.readStringBuilder().toString());
	}

	@Test
	public void testCanReadInt () throws IOException {
		UnsafeOutput write = new UnsafeOutput(new ByteArrayOutputStream());

		Input read = new UnsafeInput(write.toBytes());
		assertEquals(false, read.canReadInt());

		write.writeVarInt(400, true);

		read = new UnsafeInput(write.toBytes());
		assertEquals(true, read.canReadInt());
		read.setLimit(read.limit() - 1);
		assertEquals(false, read.canReadInt());
	}

	@Test
	public void testInts () throws IOException {
		runIntTest(new UnsafeOutput(4096));
		runIntTest(new UnsafeOutput(new ByteArrayOutputStream()));
	}

	private void runIntTest (UnsafeOutput write) throws IOException {
		write.supportVarInts(false);

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

		write.supportVarInts(true);

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
		assertEquals(5, write.writeInt(1000000000, false));
		assertEquals(5, write.writeInt(1000000000, true));
		assertEquals(5, write.writeInt(Integer.MAX_VALUE - 1, false));
		assertEquals(5, write.writeInt(Integer.MAX_VALUE - 1, true));
		assertEquals(5, write.writeInt(Integer.MAX_VALUE, false));
		assertEquals(5, write.writeInt(Integer.MAX_VALUE, true));

		UnsafeInput read = new UnsafeInput(write.toBytes());
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

		read.setVarIntsEnabled(true);

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
		assertEquals(1000000000, read.readInt(false));
		assertEquals(1000000000, read.readInt(true));
		assertEquals(Integer.MAX_VALUE - 1, read.readInt(false));
		assertEquals(Integer.MAX_VALUE - 1, read.readInt(true));
		assertEquals(Integer.MAX_VALUE, read.readInt(false));
		assertEquals(Integer.MAX_VALUE, read.readInt(true));
		assertEquals(false, read.canReadInt());

		Random random = new Random();
		for (int i = 0; i < 10000; i++) {
			int value = random.nextInt();
			write.clear();
			write.writeInt(value);
			write.writeInt(value, true);
			write.writeInt(value, false);
			read.setBuffer(write.toBytes());
			assertEquals(value, read.readInt());
			assertEquals(value, read.readInt(true));
			assertEquals(value, read.readInt(false));
		}
	}

	@Test
	public void testLongs () throws IOException {
		runLongTest(new UnsafeOutput(4096));
		runLongTest(new UnsafeOutput(new ByteArrayOutputStream()));
	}

	private void runLongTest (UnsafeOutput write) throws IOException {
		write.supportVarInts(false);

		write.writeLong(0);
		write.writeLong(63);
		write.writeLong(64);
		write.writeLong(127);
		write.writeLong(128);
		write.writeLong(8192);
		write.writeLong(16384);
		write.writeLong(2097151);
		write.writeLong(1048575);
		write.writeLong(134217727);
		write.writeLong(268435455);
		write.writeLong(134217728);
		write.writeLong(268435456);
		write.writeLong(-2097151);
		write.writeLong(-1048575);
		write.writeLong(-134217727);
		write.writeLong(-268435455);
		write.writeLong(-134217728);
		write.writeLong(-268435456);

		write.supportVarInts(true);

		assertEquals(1, write.writeLong(0, true));
		assertEquals(1, write.writeLong(0, false));
		assertEquals(1, write.writeLong(63, true));
		assertEquals(1, write.writeLong(63, false));
		assertEquals(1, write.writeLong(64, true));
		assertEquals(2, write.writeLong(64, false));
		assertEquals(1, write.writeLong(127, true));
		assertEquals(2, write.writeLong(127, false));
		assertEquals(2, write.writeLong(128, true));
		assertEquals(2, write.writeLong(128, false));
		assertEquals(2, write.writeLong(8191, true));
		assertEquals(2, write.writeLong(8191, false));
		assertEquals(2, write.writeLong(8192, true));
		assertEquals(3, write.writeLong(8192, false));
		assertEquals(2, write.writeLong(16383, true));
		assertEquals(3, write.writeLong(16383, false));
		assertEquals(3, write.writeLong(16384, true));
		assertEquals(3, write.writeLong(16384, false));
		assertEquals(3, write.writeLong(2097151, true));
		assertEquals(4, write.writeLong(2097151, false));
		assertEquals(3, write.writeLong(1048575, true));
		assertEquals(3, write.writeLong(1048575, false));
		assertEquals(4, write.writeLong(134217727, true));
		assertEquals(4, write.writeLong(134217727, false));
		assertEquals(4, write.writeLong(268435455l, true));
		assertEquals(5, write.writeLong(268435455l, false));
		assertEquals(4, write.writeLong(134217728l, true));
		assertEquals(5, write.writeLong(134217728l, false));
		assertEquals(5, write.writeLong(268435456l, true));
		assertEquals(5, write.writeLong(268435456l, false));
		assertEquals(1, write.writeLong(-64, false));
		assertEquals(9, write.writeLong(-64, true));
		assertEquals(2, write.writeLong(-65, false));
		assertEquals(9, write.writeLong(-65, true));
		assertEquals(2, write.writeLong(-8192, false));
		assertEquals(9, write.writeLong(-8192, true));
		assertEquals(3, write.writeLong(-1048576, false));
		assertEquals(9, write.writeLong(-1048576, true));
		assertEquals(4, write.writeLong(-134217728, false));
		assertEquals(9, write.writeLong(-134217728, true));
		assertEquals(5, write.writeLong(-134217729, false));
		assertEquals(9, write.writeLong(-134217729, true));

		UnsafeInput read = new UnsafeInput(write.toBytes());
		assertEquals(0, read.readLong());
		assertEquals(63, read.readLong());
		assertEquals(64, read.readLong());
		assertEquals(127, read.readLong());
		assertEquals(128, read.readLong());
		assertEquals(8192, read.readLong());
		assertEquals(16384, read.readLong());
		assertEquals(2097151, read.readLong());
		assertEquals(1048575, read.readLong());
		assertEquals(134217727, read.readLong());
		assertEquals(268435455, read.readLong());
		assertEquals(134217728, read.readLong());
		assertEquals(268435456, read.readLong());
		assertEquals(-2097151, read.readLong());
		assertEquals(-1048575, read.readLong());
		assertEquals(-134217727, read.readLong());
		assertEquals(-268435455, read.readLong());
		assertEquals(-134217728, read.readLong());
		assertEquals(-268435456, read.readLong());

		read.setVarIntsEnabled(true);

		assertEquals(0, read.readLong(true));
		assertEquals(0, read.readLong(false));
		assertEquals(63, read.readLong(true));
		assertEquals(63, read.readLong(false));
		assertEquals(64, read.readLong(true));
		assertEquals(64, read.readLong(false));
		assertEquals(127, read.readLong(true));
		assertEquals(127, read.readLong(false));
		assertEquals(128, read.readLong(true));
		assertEquals(128, read.readLong(false));
		assertEquals(8191, read.readLong(true));
		assertEquals(8191, read.readLong(false));
		assertEquals(8192, read.readLong(true));
		assertEquals(8192, read.readLong(false));
		assertEquals(16383, read.readLong(true));
		assertEquals(16383, read.readLong(false));
		assertEquals(16384, read.readLong(true));
		assertEquals(16384, read.readLong(false));
		assertEquals(2097151, read.readLong(true));
		assertEquals(2097151, read.readLong(false));
		assertEquals(1048575, read.readLong(true));
		assertEquals(1048575, read.readLong(false));
		assertEquals(134217727, read.readLong(true));
		assertEquals(134217727, read.readLong(false));
		assertEquals(268435455, read.readLong(true));
		assertEquals(268435455, read.readLong(false));
		assertEquals(134217728, read.readLong(true));
		assertEquals(134217728, read.readLong(false));
		assertEquals(268435456, read.readLong(true));
		assertEquals(268435456, read.readLong(false));
		assertEquals(-64, read.readLong(false));
		assertEquals(-64, read.readLong(true));
		assertEquals(-65, read.readLong(false));
		assertEquals(-65, read.readLong(true));
		assertEquals(-8192, read.readLong(false));
		assertEquals(-8192, read.readLong(true));
		assertEquals(-1048576, read.readLong(false));
		assertEquals(-1048576, read.readLong(true));
		assertEquals(-134217728, read.readLong(false));
		assertEquals(-134217728, read.readLong(true));
		assertEquals(-134217729, read.readLong(false));
		assertEquals(-134217729, read.readLong(true));

		Random random = new Random();
		for (int i = 0; i < 10000; i++) {
			long value = random.nextLong();
			write.clear();
			write.writeLong(value);
			write.writeLong(value, true);
			write.writeLong(value, false);
			read.setBuffer(write.toBytes());
			assertEquals("Element " + i, value, read.readLong());
			assertEquals("Element " + i, value, read.readLong(true));
			assertEquals("Element " + i, value, read.readLong(false));
		}
	}

	@Test
	public void testShorts () throws IOException {
		runShortTest(new UnsafeOutput(4096));
		runShortTest(new UnsafeOutput(new ByteArrayOutputStream()));
	}

	private void runShortTest (UnsafeOutput write) throws IOException {
		write.writeShort(0);
		write.writeShort(63);
		write.writeShort(64);
		write.writeShort(127);
		write.writeShort(128);
		write.writeShort(8192);
		write.writeShort(16384);
		write.writeShort(32767);
		write.writeShort(-63);
		write.writeShort(-64);
		write.writeShort(-127);
		write.writeShort(-128);
		write.writeShort(-8192);
		write.writeShort(-16384);
		write.writeShort(-32768);

		Input read = new UnsafeInput(write.toBytes());
		assertEquals(0, read.readShort());
		assertEquals(63, read.readShort());
		assertEquals(64, read.readShort());
		assertEquals(127, read.readShort());
		assertEquals(128, read.readShort());
		assertEquals(8192, read.readShort());
		assertEquals(16384, read.readShort());
		assertEquals(32767, read.readShort());
		assertEquals(-63, read.readShort());
		assertEquals(-64, read.readShort());
		assertEquals(-127, read.readShort());
		assertEquals(-128, read.readShort());
		assertEquals(-8192, read.readShort());
		assertEquals(-16384, read.readShort());
		assertEquals(-32768, read.readShort());
	}

	@Test
	public void testFloats () throws IOException {
		runFloatTest(new UnsafeOutput(4096));
		runFloatTest(new UnsafeOutput(new ByteArrayOutputStream()));
	}

	private void runFloatTest (UnsafeOutput write) throws IOException {
		write.writeFloat(0);
		write.writeFloat(63);
		write.writeFloat(64);
		write.writeFloat(127);
		write.writeFloat(128);
		write.writeFloat(8192);
		write.writeFloat(16384);
		write.writeFloat(32767);
		write.writeFloat(-63);
		write.writeFloat(-64);
		write.writeFloat(-127);
		write.writeFloat(-128);
		write.writeFloat(-8192);
		write.writeFloat(-16384);
		write.writeFloat(-32768);
		assertEquals(4, write.writeFloat(0, 1000, true));
		assertEquals(4, write.writeFloat(0, 1000, false));
		assertEquals(4, write.writeFloat(63, 1000, true));
		assertEquals(4, write.writeFloat(63, 1000, false));
		assertEquals(4, write.writeFloat(64, 1000, true));
		assertEquals(4, write.writeFloat(64, 1000, false));
		assertEquals(4, write.writeFloat(127, 1000, true));
		assertEquals(4, write.writeFloat(127, 1000, false));
		assertEquals(4, write.writeFloat(128, 1000, true));
		assertEquals(4, write.writeFloat(128, 1000, false));
		assertEquals(4, write.writeFloat(8191, 1000, true));
		assertEquals(4, write.writeFloat(8191, 1000, false));
		assertEquals(4, write.writeFloat(8192, 1000, true));
		assertEquals(4, write.writeFloat(8192, 1000, false));
		assertEquals(4, write.writeFloat(16383, 1000, true));
		assertEquals(4, write.writeFloat(16383, 1000, false));
		assertEquals(4, write.writeFloat(16384, 1000, true));
		assertEquals(4, write.writeFloat(16384, 1000, false));
		assertEquals(4, write.writeFloat(32767, 1000, true));
		assertEquals(4, write.writeFloat(32767, 1000, false));
		assertEquals(4, write.writeFloat(-64, 1000, false));
		assertEquals(4, write.writeFloat(-64, 1000, true));
		assertEquals(4, write.writeFloat(-65, 1000, false));
		assertEquals(4, write.writeFloat(-65, 1000, true));
		assertEquals(4, write.writeFloat(-8192, 1000, false));
		assertEquals(4, write.writeFloat(-8192, 1000, true));

		Input read = new UnsafeInput(write.toBytes());
		assertFloatEquals(read.readFloat(), 0f);
		assertFloatEquals(read.readFloat(), 63f);
		assertFloatEquals(read.readFloat(), 64f);
		assertFloatEquals(read.readFloat(), 127f);
		assertFloatEquals(read.readFloat(), 128f);
		assertFloatEquals(read.readFloat(), 8192f);
		assertFloatEquals(read.readFloat(), 16384f);
		assertFloatEquals(read.readFloat(), 32767f);
		assertFloatEquals(read.readFloat(), -63f);
		assertFloatEquals(read.readFloat(), -64f);
		assertFloatEquals(read.readFloat(), -127f);
		assertFloatEquals(read.readFloat(), -128f);
		assertFloatEquals(read.readFloat(), -8192f);
		assertFloatEquals(read.readFloat(), -16384f);
		assertFloatEquals(read.readFloat(), -32768f);
		assertFloatEquals(read.readFloat(1000, true), 0f);
		assertFloatEquals(read.readFloat(1000, false), 0f);
		assertFloatEquals(read.readFloat(1000, true), 63f);
		assertFloatEquals(read.readFloat(1000, false), 63f);
		assertFloatEquals(read.readFloat(1000, true), 64f);
		assertFloatEquals(read.readFloat(1000, false), 64f);
		assertFloatEquals(read.readFloat(1000, true), 127f);
		assertFloatEquals(read.readFloat(1000, false), 127f);
		assertFloatEquals(read.readFloat(1000, true), 128f);
		assertFloatEquals(read.readFloat(1000, false), 128f);
		assertFloatEquals(read.readFloat(1000, true), 8191f);
		assertFloatEquals(read.readFloat(1000, false), 8191f);
		assertFloatEquals(read.readFloat(1000, true), 8192f);
		assertFloatEquals(read.readFloat(1000, false), 8192f);
		assertFloatEquals(read.readFloat(1000, true), 16383f);
		assertFloatEquals(read.readFloat(1000, false), 16383f);
		assertFloatEquals(read.readFloat(1000, true), 16384f);
		assertFloatEquals(read.readFloat(1000, false), 16384f);
		assertFloatEquals(read.readFloat(1000, true), 32767f);
		assertFloatEquals(read.readFloat(1000, false), 32767f);
		assertFloatEquals(read.readFloat(1000, false), -64f);
		assertFloatEquals(read.readFloat(1000, true), -64f);
		assertFloatEquals(read.readFloat(1000, false), -65f);
		assertFloatEquals(read.readFloat(1000, true), -65f);
		assertFloatEquals(read.readFloat(1000, false), -8192f);
		assertFloatEquals(read.readFloat(1000, true), -8192f);
	}

	@Test
	public void testDoubles () throws IOException {
		runDoubleTest(new UnsafeOutput(4096));
		runDoubleTest(new UnsafeOutput(new ByteArrayOutputStream()));
	}

	private void runDoubleTest (UnsafeOutput write) throws IOException {
		write.writeDouble(0);
		write.writeDouble(63);
		write.writeDouble(64);
		write.writeDouble(127);
		write.writeDouble(128);
		write.writeDouble(8192);
		write.writeDouble(16384);
		write.writeDouble(32767);
		write.writeDouble(-63);
		write.writeDouble(-64);
		write.writeDouble(-127);
		write.writeDouble(-128);
		write.writeDouble(-8192);
		write.writeDouble(-16384);
		write.writeDouble(-32768);
		assertEquals(8, write.writeDouble(0, 1000, true));
		assertEquals(8, write.writeDouble(0, 1000, false));
		assertEquals(8, write.writeDouble(63, 1000, true));
		assertEquals(8, write.writeDouble(63, 1000, false));
		assertEquals(8, write.writeDouble(64, 1000, true));
		assertEquals(8, write.writeDouble(64, 1000, false));
		assertEquals(8, write.writeDouble(127, 1000, true));
		assertEquals(8, write.writeDouble(127, 1000, false));
		assertEquals(8, write.writeDouble(128, 1000, true));
		assertEquals(8, write.writeDouble(128, 1000, false));
		assertEquals(8, write.writeDouble(8191, 1000, true));
		assertEquals(8, write.writeDouble(8191, 1000, false));
		assertEquals(8, write.writeDouble(8192, 1000, true));
		assertEquals(8, write.writeDouble(8192, 1000, false));
		assertEquals(8, write.writeDouble(16383, 1000, true));
		assertEquals(8, write.writeDouble(16383, 1000, false));
		assertEquals(8, write.writeDouble(16384, 1000, true));
		assertEquals(8, write.writeDouble(16384, 1000, false));
		assertEquals(8, write.writeDouble(32767, 1000, true));
		assertEquals(8, write.writeDouble(32767, 1000, false));
		assertEquals(8, write.writeDouble(-64, 1000, false));
		assertEquals(8, write.writeDouble(-64, 1000, true));
		assertEquals(8, write.writeDouble(-65, 1000, false));
		assertEquals(8, write.writeDouble(-65, 1000, true));
		assertEquals(8, write.writeDouble(-8192, 1000, false));
		assertEquals(8, write.writeDouble(-8192, 1000, true));
		write.writeDouble(1.23456d);

		Input read = new UnsafeInput(write.toBytes());
		assertDoubleEquals(read.readDouble(), 0d);
		assertDoubleEquals(read.readDouble(), 63d);
		assertDoubleEquals(read.readDouble(), 64d);
		assertDoubleEquals(read.readDouble(), 127d);
		assertDoubleEquals(read.readDouble(), 128d);
		assertDoubleEquals(read.readDouble(), 8192d);
		assertDoubleEquals(read.readDouble(), 16384d);
		assertDoubleEquals(read.readDouble(), 32767d);
		assertDoubleEquals(read.readDouble(), -63d);
		assertDoubleEquals(read.readDouble(), -64d);
		assertDoubleEquals(read.readDouble(), -127d);
		assertDoubleEquals(read.readDouble(), -128d);
		assertDoubleEquals(read.readDouble(), -8192d);
		assertDoubleEquals(read.readDouble(), -16384d);
		assertDoubleEquals(read.readDouble(), -32768d);
		assertDoubleEquals(read.readDouble(1000, true), 0d);
		assertDoubleEquals(read.readDouble(1000, false), 0d);
		assertDoubleEquals(read.readDouble(1000, true), 63d);
		assertDoubleEquals(read.readDouble(1000, false), 63d);
		assertDoubleEquals(read.readDouble(1000, true), 64d);
		assertDoubleEquals(read.readDouble(1000, false), 64d);
		assertDoubleEquals(read.readDouble(1000, true), 127d);
		assertDoubleEquals(read.readDouble(1000, false), 127d);
		assertDoubleEquals(read.readDouble(1000, true), 128d);
		assertDoubleEquals(read.readDouble(1000, false), 128d);
		assertDoubleEquals(read.readDouble(1000, true), 8191d);
		assertDoubleEquals(read.readDouble(1000, false), 8191d);
		assertDoubleEquals(read.readDouble(1000, true), 8192d);
		assertDoubleEquals(read.readDouble(1000, false), 8192d);
		assertDoubleEquals(read.readDouble(1000, true), 16383d);
		assertDoubleEquals(read.readDouble(1000, false), 16383d);
		assertDoubleEquals(read.readDouble(1000, true), 16384d);
		assertDoubleEquals(read.readDouble(1000, false), 16384d);
		assertDoubleEquals(read.readDouble(1000, true), 32767d);
		assertDoubleEquals(read.readDouble(1000, false), 32767d);
		assertDoubleEquals(read.readDouble(1000, false), -64d);
		assertDoubleEquals(read.readDouble(1000, true), -64d);
		assertDoubleEquals(read.readDouble(1000, false), -65d);
		assertDoubleEquals(read.readDouble(1000, true), -65d);
		assertDoubleEquals(read.readDouble(1000, false), -8192d);
		assertDoubleEquals(read.readDouble(1000, true), -8192d);
		assertDoubleEquals(1.23456d, read.readDouble());
	}

	@Test
	public void testBooleans () throws IOException {
		runBooleanTest(new UnsafeOutput(4096));
		runBooleanTest(new UnsafeOutput(new ByteArrayOutputStream()));
	}

	private void runBooleanTest (UnsafeOutput write) throws IOException {
		for (int i = 0; i < 100; i++) {
			write.writeBoolean(true);
			write.writeBoolean(false);
		}

		Input read = new UnsafeInput(write.toBytes());
		for (int i = 0; i < 100; i++) {
			assertEquals(true, read.readBoolean());
			assertEquals(false, read.readBoolean());
		}
	}

	@Test
	public void testChars () throws IOException {
		runCharTest(new UnsafeOutput(4096));
		runCharTest(new UnsafeOutput(new ByteArrayOutputStream()));
	}

	private void runCharTest (UnsafeOutput write) throws IOException {
		write.writeChar((char)0);
		write.writeChar((char)63);
		write.writeChar((char)64);
		write.writeChar((char)127);
		write.writeChar((char)128);
		write.writeChar((char)8192);
		write.writeChar((char)16384);
		write.writeChar((char)32767);
		write.writeChar((char)65535);

		Input read = new UnsafeInput(write.toBytes());
		assertEquals(0, read.readChar());
		assertEquals(63, read.readChar());
		assertEquals(64, read.readChar());
		assertEquals(127, read.readChar());
		assertEquals(128, read.readChar());
		assertEquals(8192, read.readChar());
		assertEquals(16384, read.readChar());
		assertEquals(32767, read.readChar());
		assertEquals(65535, read.readChar());
	}

	// Check if writing varInts may produce more bytes than expected
	@Test
	public void testWriteTooManyBytes () throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream(1);
		runVarIntTest(new UnsafeOutput(os, 4), os);
	}

	private void runVarIntTest (UnsafeOutput write, ByteArrayOutputStream os) throws IOException {
		write.writeVarInt(0, true);
		write.writeVarInt(63, true);
		write.writeVarInt(64, true);
		write.writeVarInt(65535, true);
		assertEquals(6, write.total());
		write.flush();
		assertEquals(6, os.size());

		Input read = new UnsafeInput(os.toByteArray());
		assertEquals(0, read.readVarInt(true));
		assertEquals(63, read.readVarInt(true));
		assertEquals(64, read.readVarInt(true));
		assertEquals(65535, read.readVarInt(true));
		assertEquals(true, read.eof());
	}

	@Test
	public void testSimpleVarInt () {
		final int value = 39117;
		final Output out = new UnsafeOutput(1024);
		out.writeVarInt(value, true);
		out.flush();
		final Input in = new UnsafeInput(out.toBytes());
		final int actualValue = in.readVarInt(true);
		assertEquals(value, actualValue);
	}
}
