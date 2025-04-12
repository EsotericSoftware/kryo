/* Copyright (c) 2008-2025, Nathan Sweet
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

package com.esotericsoftware.kryo.io;

import static com.esotericsoftware.kryo.KryoAssert.*;
import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.Unsafe;
import com.esotericsoftware.kryo.unsafe.UnsafeByteBufferInput;
import com.esotericsoftware.kryo.unsafe.UnsafeByteBufferOutput;
import com.esotericsoftware.kryo.unsafe.UnsafeUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** @author Roman Levenstein <romixlev@gmail.com> */
@Unsafe
@SuppressWarnings("restriction")
class UnsafeByteBufferInputOutputTest {

	@Test
	void testByteBufferOutputWithPreallocatedMemory () {
		long bufAddress = UnsafeUtil.unsafe.allocateMemory(4096);
		try {
			ByteBufferOutput outputBuffer = new ByteBufferOutput(UnsafeUtil.newDirectBuffer(bufAddress, 4096));
			outputBuffer.writeInt(10);

			ByteBufferInput inputBuffer = new ByteBufferInput(outputBuffer.getByteBuffer());
			inputBuffer.readInt();

			UnsafeUtil.dispose(inputBuffer.getByteBuffer());
			UnsafeUtil.dispose(outputBuffer.getByteBuffer());

			outputBuffer = new UnsafeByteBufferOutput(bufAddress, 4096);
			outputBuffer.writeInt(10);

			inputBuffer = new UnsafeByteBufferInput(outputBuffer.getByteBuffer());
			inputBuffer.readInt();

			UnsafeUtil.dispose(inputBuffer.getByteBuffer());
			UnsafeUtil.dispose(outputBuffer.getByteBuffer());
		} catch (Throwable t) {
			System.err.println("Streams with preallocated direct memory are not supported on this JVM");
			t.printStackTrace();
		} finally {
			UnsafeUtil.unsafe.freeMemory(bufAddress);
		}
	}

	@Test
	void testOutputStream () {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		UnsafeByteBufferOutput output = new UnsafeByteBufferOutput(buffer, 2);
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
	void testInputStream () {
		byte[] bytes = new byte[] { //
			11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, //
			31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, //
			51, 52, 53, 54, 55, 56, 57, 58, //
			61, 62, 63, 64, 65};
		ByteArrayInputStream buffer = new ByteArrayInputStream(bytes);
		Input input = new UnsafeByteBufferInput(buffer, 2);
		byte[] temp = new byte[1024];
		int count = input.read(temp, 512, bytes.length);
		assertEquals(bytes.length, count);
		byte[] temp2 = new byte[count];
		System.arraycopy(temp, 512, temp2, 0, count);
		assertArrayEquals(bytes, temp2);

		input = new UnsafeByteBufferInput(bytes);
		count = input.read(temp, 512, 512);
		assertEquals(bytes.length, count);
		temp2 = new byte[count];
		System.arraycopy(temp, 512, temp2, 0, count);
		assertArrayEquals(bytes, temp2);
	}

	@Test
	void testWriteBytes () {
		UnsafeByteBufferOutput buffer = new UnsafeByteBufferOutput(512);
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
	void testStrings () {
		runStringTest(new UnsafeByteBufferOutput(4096));
		runStringTest(new UnsafeByteBufferOutput(897));
		runStringTest(new UnsafeByteBufferOutput(new ByteArrayOutputStream()));

		UnsafeByteBufferOutput write = new UnsafeByteBufferOutput(21);
		String value = "abcdef\u00E1\u00E9\u00ED\u00F3\u00FA\u1234";
		write.writeString(value);
		Input read = new UnsafeByteBufferInput(write.toBytes());
		assertEquals(value, read.readString());

		runStringTest(127);
		runStringTest(256);
		runStringTest(1024 * 1023);
		runStringTest(1024 * 1024);
		runStringTest(1024 * 1025);
		runStringTest(1024 * 1026);
		runStringTest(1024 * 1024 * 2);
	}

	private void runStringTest (int length) {
		UnsafeByteBufferOutput write = new UnsafeByteBufferOutput(1024, -1);
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < length; i++)
			buffer.append((char)i);

		String value = buffer.toString();
		write.writeString(value);
		write.writeString(value);
		Input read = new UnsafeByteBufferInput(write.toBytes());
		assertEquals(value, read.readString());
		assertEquals(value, read.readStringBuilder().toString());

		write.reset();
		write.writeString(buffer.toString());
		write.writeString(buffer.toString());
		read = new UnsafeByteBufferInput(write.toBytes());
		assertEquals(value, read.readStringBuilder().toString());
		assertEquals(value, read.readString());

		if (length <= 127) {
			write.reset();
			write.writeAscii(value);
			write.writeAscii(value);
			read = new UnsafeByteBufferInput(write.toBytes());
			assertEquals(value, read.readStringBuilder().toString());
			assertEquals(value, read.readString());
		}
	}

	private void runStringTest (UnsafeByteBufferOutput write) {
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

		Input read = new UnsafeByteBufferInput(write.toBytes());
		assertEquals("", read.readString());
		assertEquals("1", read.readString());
		assertEquals("22", read.readString());
		assertEquals("uno", read.readString());
		assertEquals("dos", read.readString());
		assertEquals("tres", read.readString());
		assertNull(read.readString());
		assertEquals(value1, read.readString());
		assertEquals(value2, read.readString());
		for (int i = 0; i < 127; i++)
			assertEquals(String.valueOf((char)i), read.readString());
		for (int i = 0; i < 127; i++)
			assertEquals(String.valueOf((char)i) + "abc", read.readString());

		read.reset();
		assertEquals("", read.readStringBuilder().toString());
		assertEquals("1", read.readStringBuilder().toString());
		assertEquals("22", read.readStringBuilder().toString());
		assertEquals("uno", read.readStringBuilder().toString());
		assertEquals("dos", read.readStringBuilder().toString());
		assertEquals("tres", read.readStringBuilder().toString());
		assertNull(read.readStringBuilder());
		assertEquals(value1, read.readStringBuilder().toString());
		assertEquals(value2, read.readStringBuilder().toString());
		for (int i = 0; i < 127; i++)
			assertEquals(String.valueOf((char)i), read.readStringBuilder().toString());
		for (int i = 0; i < 127; i++)
			assertEquals(String.valueOf((char)i) + "abc", read.readStringBuilder().toString());
	}

	@Test
	void testCanReadInt () {
		UnsafeByteBufferOutput write = new UnsafeByteBufferOutput(new ByteArrayOutputStream());

		Input read = new UnsafeByteBufferInput(write.toBytes());
		assertFalse(read.canReadVarInt());

		write.writeVarInt(400, true);

		read = new UnsafeByteBufferInput(write.toBytes());
		assertTrue(read.canReadVarInt());
		read.setLimit(read.limit() - 1);
		assertFalse(read.canReadVarInt());
	}

	@Test
	void testInts () {
		runIntTest(new UnsafeByteBufferOutput(4096));
		runIntTest(new UnsafeByteBufferOutput(new ByteArrayOutputStream()));
	}

	private void runIntTest (UnsafeByteBufferOutput write) {
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

		write.setVariableLengthEncoding(false);
		assertEquals(4, write.writeInt(0, true));
		assertEquals(4, write.writeInt(0, false));
		assertEquals(4, write.writeInt(63, true));
		assertEquals(4, write.writeInt(63, false));
		assertEquals(4, write.writeInt(64, true));
		assertEquals(4, write.writeInt(64, false));
		assertEquals(4, write.writeInt(127, true));
		assertEquals(4, write.writeInt(127, false));
		assertEquals(4, write.writeInt(128, true));
		assertEquals(4, write.writeInt(128, false));
		assertEquals(4, write.writeInt(8191, true));
		assertEquals(4, write.writeInt(8191, false));
		assertEquals(4, write.writeInt(8192, true));
		assertEquals(4, write.writeInt(8192, false));
		assertEquals(4, write.writeInt(16383, true));
		assertEquals(4, write.writeInt(16383, false));
		assertEquals(4, write.writeInt(16384, true));
		assertEquals(4, write.writeInt(16384, false));
		assertEquals(4, write.writeInt(2097151, true));
		assertEquals(4, write.writeInt(2097151, false));
		assertEquals(4, write.writeInt(1048575, true));
		assertEquals(4, write.writeInt(1048575, false));
		assertEquals(4, write.writeInt(134217727, true));
		assertEquals(4, write.writeInt(134217727, false));
		assertEquals(4, write.writeInt(268435455, true));
		assertEquals(4, write.writeInt(268435455, false));
		assertEquals(4, write.writeInt(134217728, true));
		assertEquals(4, write.writeInt(134217728, false));
		assertEquals(4, write.writeInt(268435456, true));
		assertEquals(4, write.writeInt(268435456, false));
		assertEquals(4, write.writeInt(-64, false));
		assertEquals(4, write.writeInt(-64, true));
		assertEquals(4, write.writeInt(-65, false));
		assertEquals(4, write.writeInt(-65, true));
		assertEquals(4, write.writeInt(-8192, false));
		assertEquals(4, write.writeInt(-8192, true));
		assertEquals(4, write.writeInt(-1048576, false));
		assertEquals(4, write.writeInt(-1048576, true));
		assertEquals(4, write.writeInt(-134217728, false));
		assertEquals(4, write.writeInt(-134217728, true));
		assertEquals(4, write.writeInt(-134217729, false));
		assertEquals(4, write.writeInt(-134217729, true));
		assertEquals(4, write.writeInt(1000000000, false));
		assertEquals(4, write.writeInt(1000000000, true));
		assertEquals(4, write.writeInt(Integer.MAX_VALUE - 1, false));
		assertEquals(4, write.writeInt(Integer.MAX_VALUE - 1, true));
		assertEquals(4, write.writeInt(Integer.MAX_VALUE, false));
		assertEquals(4, write.writeInt(Integer.MAX_VALUE, true));

		UnsafeByteBufferInput read = new UnsafeByteBufferInput(write.toBytes());
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
		assertTrue(read.canReadInt());
		assertTrue(read.canReadInt());
		assertTrue(read.canReadInt());

		read.setVariableLengthEncoding(false);
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
		assertFalse(read.canReadInt());

		Random random = new Random();
		for (int i = 0; i < 10000; i++) {
			int value = random.nextInt();
			write.reset();
			write.writeInt(value);
			write.writeVarInt(value, true);
			write.writeVarInt(value, false);

			read = new UnsafeByteBufferInput(write.toBytes());
			assertEquals(value, read.readInt());
			assertEquals(value, read.readVarInt(true));
			assertEquals(value, read.readVarInt(false));
		}
	}

	@Test
	void testLongs () {
		runLongTest(new UnsafeByteBufferOutput(4096));
		runLongTest(new UnsafeByteBufferOutput(new ByteArrayOutputStream()));
	}

	private void runLongTest (UnsafeByteBufferOutput write) {
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

		write.setVariableLengthEncoding(false);
		assertEquals(8, write.writeLong(0, true));
		assertEquals(8, write.writeLong(0, false));
		assertEquals(8, write.writeLong(63, true));
		assertEquals(8, write.writeLong(63, false));
		assertEquals(8, write.writeLong(64, true));
		assertEquals(8, write.writeLong(64, false));
		assertEquals(8, write.writeLong(127, true));
		assertEquals(8, write.writeLong(127, false));
		assertEquals(8, write.writeLong(128, true));
		assertEquals(8, write.writeLong(128, false));
		assertEquals(8, write.writeLong(8191, true));
		assertEquals(8, write.writeLong(8191, false));
		assertEquals(8, write.writeLong(8192, true));
		assertEquals(8, write.writeLong(8192, false));
		assertEquals(8, write.writeLong(16383, true));
		assertEquals(8, write.writeLong(16383, false));
		assertEquals(8, write.writeLong(16384, true));
		assertEquals(8, write.writeLong(16384, false));
		assertEquals(8, write.writeLong(2097151, true));
		assertEquals(8, write.writeLong(2097151, false));
		assertEquals(8, write.writeLong(1048575, true));
		assertEquals(8, write.writeLong(1048575, false));
		assertEquals(8, write.writeLong(134217727, true));
		assertEquals(8, write.writeLong(134217727, false));
		assertEquals(8, write.writeLong(268435455L, true));
		assertEquals(8, write.writeLong(268435455L, false));
		assertEquals(8, write.writeLong(134217728L, true));
		assertEquals(8, write.writeLong(134217728L, false));
		assertEquals(8, write.writeLong(268435456L, true));
		assertEquals(8, write.writeLong(268435456L, false));
		assertEquals(8, write.writeLong(-64, false));
		assertEquals(8, write.writeLong(-64, true));
		assertEquals(8, write.writeLong(-65, false));
		assertEquals(8, write.writeLong(-65, true));
		assertEquals(8, write.writeLong(-8192, false));
		assertEquals(8, write.writeLong(-8192, true));
		assertEquals(8, write.writeLong(-1048576, false));
		assertEquals(8, write.writeLong(-1048576, true));
		assertEquals(8, write.writeLong(-134217728, false));
		assertEquals(8, write.writeLong(-134217728, true));
		assertEquals(8, write.writeLong(-134217729, false));
		assertEquals(8, write.writeLong(-134217729, true));

		UnsafeByteBufferInput read = new UnsafeByteBufferInput(write.toBytes());
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

		read.setVariableLengthEncoding(false);
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
			write.reset();
			write.writeLong(value);
			write.writeVarLong(value, true);
			write.writeVarLong(value, false);

			read = new UnsafeByteBufferInput(write.toBytes());
			assertEquals(value, read.readLong(), "Element " + i);
			assertEquals(value, read.readVarLong(true), "Element " + i);
			assertEquals(value, read.readVarLong(false), "Element " + i);
		}
	}

	@Test
	void testShorts () {
		runShortTest(new UnsafeByteBufferOutput(4096));
		runShortTest(new UnsafeByteBufferOutput(new ByteArrayOutputStream()));
	}

	private void runShortTest (UnsafeByteBufferOutput write) {
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

		Input read = new UnsafeByteBufferInput(write.toBytes());
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
	void testFloats () {
		runFloatTest(new UnsafeByteBufferOutput(4096));
		runFloatTest(new UnsafeByteBufferOutput(new ByteArrayOutputStream()));
	}

	private void runFloatTest (UnsafeByteBufferOutput write) {
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

		assertEquals(1, write.writeVarFloat(0, 1000, true));
		assertEquals(1, write.writeVarFloat(0, 1000, false));
		assertEquals(3, write.writeVarFloat(63, 1000, true));
		assertEquals(3, write.writeVarFloat(63, 1000, false));
		assertEquals(3, write.writeVarFloat(64, 1000, true));
		assertEquals(3, write.writeVarFloat(64, 1000, false));
		assertEquals(3, write.writeVarFloat(127, 1000, true));
		assertEquals(3, write.writeVarFloat(127, 1000, false));
		assertEquals(3, write.writeVarFloat(128, 1000, true));
		assertEquals(3, write.writeVarFloat(128, 1000, false));
		assertEquals(4, write.writeVarFloat(8191, 1000, true));
		assertEquals(4, write.writeVarFloat(8191, 1000, false));
		assertEquals(4, write.writeVarFloat(8192, 1000, true));
		assertEquals(4, write.writeVarFloat(8192, 1000, false));
		assertEquals(4, write.writeVarFloat(16383, 1000, true));
		assertEquals(4, write.writeVarFloat(16383, 1000, false));
		assertEquals(4, write.writeVarFloat(16384, 1000, true));
		assertEquals(4, write.writeVarFloat(16384, 1000, false));
		assertEquals(4, write.writeVarFloat(32767, 1000, true));
		assertEquals(4, write.writeVarFloat(32767, 1000, false));
		assertEquals(3, write.writeVarFloat(-64, 1000, false));
		assertEquals(5, write.writeVarFloat(-64, 1000, true));
		assertEquals(3, write.writeVarFloat(-65, 1000, false));
		assertEquals(5, write.writeVarFloat(-65, 1000, true));
		assertEquals(4, write.writeVarFloat(-8192, 1000, false));
		assertEquals(5, write.writeVarFloat(-8192, 1000, true));

		Input read = new UnsafeByteBufferInput(write.toBytes());
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
		assertFloatEquals(read.readVarFloat(1000, true), 0f);
		assertFloatEquals(read.readVarFloat(1000, false), 0f);
		assertFloatEquals(read.readVarFloat(1000, true), 63f);
		assertFloatEquals(read.readVarFloat(1000, false), 63f);
		assertFloatEquals(read.readVarFloat(1000, true), 64f);
		assertFloatEquals(read.readVarFloat(1000, false), 64f);
		assertFloatEquals(read.readVarFloat(1000, true), 127f);
		assertFloatEquals(read.readVarFloat(1000, false), 127f);
		assertFloatEquals(read.readVarFloat(1000, true), 128f);
		assertFloatEquals(read.readVarFloat(1000, false), 128f);
		assertFloatEquals(read.readVarFloat(1000, true), 8191f);
		assertFloatEquals(read.readVarFloat(1000, false), 8191f);
		assertFloatEquals(read.readVarFloat(1000, true), 8192f);
		assertFloatEquals(read.readVarFloat(1000, false), 8192f);
		assertFloatEquals(read.readVarFloat(1000, true), 16383f);
		assertFloatEquals(read.readVarFloat(1000, false), 16383f);
		assertFloatEquals(read.readVarFloat(1000, true), 16384f);
		assertFloatEquals(read.readVarFloat(1000, false), 16384f);
		assertFloatEquals(read.readVarFloat(1000, true), 32767f);
		assertFloatEquals(read.readVarFloat(1000, false), 32767f);
		assertFloatEquals(read.readVarFloat(1000, false), -64f);
		assertFloatEquals(read.readVarFloat(1000, true), -64f);
		assertFloatEquals(read.readVarFloat(1000, false), -65f);
		assertFloatEquals(read.readVarFloat(1000, true), -65f);
		assertFloatEquals(read.readVarFloat(1000, false), -8192f);
		assertFloatEquals(read.readVarFloat(1000, true), -8192f);
	}

	@Test
	void testDoubles () {
		runDoubleTest(new UnsafeByteBufferOutput(4096));
		runDoubleTest(new UnsafeByteBufferOutput(new ByteArrayOutputStream()));
	}

	private void runDoubleTest (UnsafeByteBufferOutput write) {
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

		assertEquals(1, write.writeVarDouble(0, 1000, true));
		assertEquals(1, write.writeVarDouble(0, 1000, false));
		assertEquals(3, write.writeVarDouble(63, 1000, true));
		assertEquals(3, write.writeVarDouble(63, 1000, false));
		assertEquals(3, write.writeVarDouble(64, 1000, true));
		assertEquals(3, write.writeVarDouble(64, 1000, false));
		assertEquals(3, write.writeVarDouble(127, 1000, true));
		assertEquals(3, write.writeVarDouble(127, 1000, false));
		assertEquals(3, write.writeVarDouble(128, 1000, true));
		assertEquals(3, write.writeVarDouble(128, 1000, false));
		assertEquals(4, write.writeVarDouble(8191, 1000, true));
		assertEquals(4, write.writeVarDouble(8191, 1000, false));
		assertEquals(4, write.writeVarDouble(8192, 1000, true));
		assertEquals(4, write.writeVarDouble(8192, 1000, false));
		assertEquals(4, write.writeVarDouble(16383, 1000, true));
		assertEquals(4, write.writeVarDouble(16383, 1000, false));
		assertEquals(4, write.writeVarDouble(16384, 1000, true));
		assertEquals(4, write.writeVarDouble(16384, 1000, false));
		assertEquals(4, write.writeVarDouble(32767, 1000, true));
		assertEquals(4, write.writeVarDouble(32767, 1000, false));
		assertEquals(3, write.writeVarDouble(-64, 1000, false));
		assertEquals(9, write.writeVarDouble(-64, 1000, true));
		assertEquals(3, write.writeVarDouble(-65, 1000, false));
		assertEquals(9, write.writeVarDouble(-65, 1000, true));
		assertEquals(4, write.writeVarDouble(-8192, 1000, false));
		assertEquals(9, write.writeVarDouble(-8192, 1000, true));
		write.writeDouble(1.23456d);

		Input read = new UnsafeByteBufferInput(write.toBytes());
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

		assertDoubleEquals(read.readVarDouble(1000, true), 0d);
		assertDoubleEquals(read.readVarDouble(1000, false), 0d);
		assertDoubleEquals(read.readVarDouble(1000, true), 63d);
		assertDoubleEquals(read.readVarDouble(1000, false), 63d);
		assertDoubleEquals(read.readVarDouble(1000, true), 64d);
		assertDoubleEquals(read.readVarDouble(1000, false), 64d);
		assertDoubleEquals(read.readVarDouble(1000, true), 127d);
		assertDoubleEquals(read.readVarDouble(1000, false), 127d);
		assertDoubleEquals(read.readVarDouble(1000, true), 128d);
		assertDoubleEquals(read.readVarDouble(1000, false), 128d);
		assertDoubleEquals(read.readVarDouble(1000, true), 8191d);
		assertDoubleEquals(read.readVarDouble(1000, false), 8191d);
		assertDoubleEquals(read.readVarDouble(1000, true), 8192d);
		assertDoubleEquals(read.readVarDouble(1000, false), 8192d);
		assertDoubleEquals(read.readVarDouble(1000, true), 16383d);
		assertDoubleEquals(read.readVarDouble(1000, false), 16383d);
		assertDoubleEquals(read.readVarDouble(1000, true), 16384d);
		assertDoubleEquals(read.readVarDouble(1000, false), 16384d);
		assertDoubleEquals(read.readVarDouble(1000, true), 32767d);
		assertDoubleEquals(read.readVarDouble(1000, false), 32767d);
		assertDoubleEquals(read.readVarDouble(1000, false), -64d);
		assertDoubleEquals(read.readVarDouble(1000, true), -64d);
		assertDoubleEquals(read.readVarDouble(1000, false), -65d);
		assertDoubleEquals(read.readVarDouble(1000, true), -65d);
		assertDoubleEquals(read.readVarDouble(1000, false), -8192d);
		assertDoubleEquals(read.readVarDouble(1000, true), -8192d);
		assertDoubleEquals(1.23456d, read.readDouble());
	}

	@Test
	void testBooleans () {
		runBooleanTest(new UnsafeByteBufferOutput(200));
		runBooleanTest(new UnsafeByteBufferOutput(4096));
		runBooleanTest(new UnsafeByteBufferOutput(new ByteArrayOutputStream()));
	}

	private void runBooleanTest (UnsafeByteBufferOutput write) {
		for (int i = 0; i < 100; i++) {
			write.writeBoolean(true);
			write.writeBoolean(false);
		}

		Input read = new UnsafeByteBufferInput(write.toBytes());
		for (int i = 0; i < 100; i++) {
			assertTrue(read.readBoolean());
			assertFalse(read.readBoolean());
		}
	}

	@Test
	void testChars () {
		runCharTest(new UnsafeByteBufferOutput(4096));
		runCharTest(new UnsafeByteBufferOutput(new ByteArrayOutputStream()));
	}

	private void runCharTest (UnsafeByteBufferOutput write) {
		write.writeChar((char)0);
		write.writeChar((char)63);
		write.writeChar((char)64);
		write.writeChar((char)127);
		write.writeChar((char)128);
		write.writeChar((char)8192);
		write.writeChar((char)16384);
		write.writeChar((char)32767);
		write.writeChar((char)65535);

		Input read = new UnsafeByteBufferInput(write.toBytes());
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
}
