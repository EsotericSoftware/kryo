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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.KryoBufferUnderflowException;
import com.esotericsoftware.kryo.io.KryoBufferOverflowException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** @author Nathan Sweet */
@SuppressWarnings("all")
class InputOutputTest extends KryoTestCase {
	@Test
	void testByteBufferInputEnd () {
		Input in = new Input(new ByteArrayInputStream(new byte[] {123, 0, 0, 0}));
		assertFalse(in.end());
		in.setPosition(4);
		assertTrue(in.end());
	}

	@Test
	void testOutputStream () throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		Output output = new Output(buffer, 2);
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
	void testInputStream () throws IOException {
		byte[] bytes = new byte[] { //
			11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, //
			31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, //
			51, 52, 53, 54, 55, 56, 57, 58, //
			61, 62, 63, 64, 65};
		ByteArrayInputStream buffer = new ByteArrayInputStream(bytes);
		Input input = new Input(buffer, 2);
		byte[] temp = new byte[1024];
		int count = input.read(temp, 512, bytes.length);
		assertEquals(bytes.length, count);
		byte[] temp2 = new byte[count];
		System.arraycopy(temp, 512, temp2, 0, count);
		assertArrayEquals(bytes, temp2);

		input = new Input(bytes);
		count = input.read(temp, 512, 512);
		assertEquals(bytes.length, count);
		temp2 = new byte[count];
		System.arraycopy(temp, 512, temp2, 0, count);
		assertArrayEquals(bytes, temp2);
	}

	@Test
	void testWriteBytes () throws IOException {
		Output buffer = new Output(512);
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
	void testOverflow () throws IOException {
		Output buffer = new Output(1);
		buffer.writeByte(51);
	
		KryoBufferOverflowException thrown = assertThrows(
			KryoBufferOverflowException.class,
			() -> buffer.writeByte(65),
			"Exception expected but none thrown"
		);

		assertTrue(thrown.getMessage().startsWith("Buffer overflow"));
	}

	@Test
	void testUnderflow () throws IOException {
		Input buffer = new Input(1);
		
		KryoBufferUnderflowException thrown = assertThrows(
			KryoBufferUnderflowException.class,
			() -> buffer.readBytes(2),
			"Exception expected but none thrown"
		);

		assertTrue(thrown.getMessage().equals("Buffer underflow."));
	}

	@Test
	void testStrings () throws IOException {
		runStringTest(new Output(4096));
		runStringTest(new Output(897));
		runStringTest(new Output(new ByteArrayOutputStream()));

		Output write = new Output(21);
		String value = "abcdef\u00E1\u00E9\u00ED\u00F3\u00FA\u1234";
		write.writeString(value);
		Input read = new Input(write.toBytes());
		assertEquals(value, read.readString());

		write.reset();
		write.writeString(null);
		read = new Input(write.toBytes());
		assertNull(read.readString());

		for (int i = 0; i <= 258; i++)
			runStringTest(i);
		runStringTest(1);
		runStringTest(2);
		runStringTest(127);
		runStringTest(256);
		runStringTest(1024 * 1023);
		runStringTest(1024 * 1024);
		runStringTest(1024 * 1025);
		runStringTest(1024 * 1026);
		runStringTest(1024 * 1024 * 2);
	}

	@Test
	void testGrowingBufferForAscii () {
		// Initial size of 0.
		final Output output = new Output(0, 1024);
		// Check that it is possible to write an ASCII string into the output buffer.
		output.writeString("node/read");
		assertEquals("node/read", new Input(output.getBuffer(), 0, output.position).readString());
	}

	private void runStringTest (int length) throws IOException {
		Output write = new Output(1024, -1);
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < length; i++)
			buffer.append((char)i);

		String value = buffer.toString();
		write.writeString(value);
		write.writeString(value);
		Input read = new Input(write.toBytes());
		assertEquals(value, read.readString());
		assertEquals(value, read.readStringBuilder().toString());

		write.reset();
		write.writeString(buffer.toString());
		write.writeString(buffer.toString());
		read = new Input(write.toBytes());
		assertEquals(value, read.readStringBuilder().toString());
		assertEquals(value, read.readString());

		if (length <= 127) {
			write.reset();
			write.writeAscii(value);
			write.writeAscii(value);
			read = new Input(write.toBytes());
			assertEquals(value, read.readStringBuilder().toString());
			assertEquals(value, read.readString());
		}
	}

	private void runStringTest (Output write) throws IOException {
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

		Input read = new Input(write.toBytes());
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
	void testCanReadInt () throws IOException {
		Output write = new Output(new ByteArrayOutputStream());

		Input read = new Input(write.toBytes());
		assertFalse(read.canReadVarInt());

		write.writeVarInt(400, true);

		read = new Input(write.toBytes());
		assertTrue(read.canReadVarInt());
		read.setLimit(read.limit() - 1);
		assertFalse(read.canReadVarInt());
	}

	@Test
	void testVarIntFlagOutput () throws IOException {
		Output output = new Output(4096);
		Input input = new Input(output.getBuffer());
		runVarIntFlagsTest(output, input);
	}

	@Test
	void testVarIntFlagByteBufferOutput () throws IOException {
		ByteBufferOutput output = new ByteBufferOutput(4096);
		ByteBufferInput input = new ByteBufferInput(output.getByteBuffer());
		runVarIntFlagsTest(output, input);
	}

	private void runVarIntFlagsTest (Output output, Input input) throws IOException {
		assertEquals(1, output.writeVarIntFlag(true, 63, true));
		assertEquals(2, output.writeVarIntFlag(true, 64, true));

		assertEquals(1, output.writeVarIntFlag(false, 63, true));
		assertEquals(2, output.writeVarIntFlag(false, 64, true));

		assertEquals(1, output.writeVarIntFlag(true, 31, false));
		assertEquals(2, output.writeVarIntFlag(true, 32, false));

		assertEquals(1, output.writeVarIntFlag(false, 31, false));
		assertEquals(2, output.writeVarIntFlag(false, 32, false));

		input.setPosition(0);
		input.setLimit(output.position());
		assertTrue(input.readVarIntFlag());
		assertEquals(63, input.readVarIntFlag(true));
		assertTrue(input.readVarIntFlag());
		assertEquals(64, input.readVarIntFlag(true));

		assertFalse(input.readVarIntFlag());
		assertEquals(63, input.readVarIntFlag(true));
		assertFalse(input.readVarIntFlag());
		assertEquals(64, input.readVarIntFlag(true));

		assertTrue(input.readVarIntFlag());
		assertEquals(31, input.readVarIntFlag(false));
		assertTrue(input.readVarIntFlag());
		assertEquals(32, input.readVarIntFlag(false));

		assertFalse(input.readVarIntFlag());
		assertEquals(31, input.readVarIntFlag(false));
		assertFalse(input.readVarIntFlag());
		assertEquals(32, input.readVarIntFlag(false));
	}

	@Test
	void testInts () throws IOException {
		runIntTest(new Output(4096));
		runIntTest(new Output(new ByteArrayOutputStream()));
	}

	private void runIntTest (Output write) throws IOException {
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
		write.writeInt(0, 1);
		write.writeInt(63, 1);
		write.writeInt(64, 1);
		write.writeInt(127, 1);
		write.writeInt(128, 2);
		write.writeInt(8192, 2);
		write.writeInt(16384, 2);
		write.writeInt(2097151, 3);
		write.writeInt(1048575, 3);
		write.writeInt(134217727, 4);
		write.writeInt(268435455, 4);
		write.writeInt(134217728, 4);
		write.writeInt(268435456, 4);
		write.writeInt(-2097151, 3);
		write.writeInt(-1048575, 3);
		write.writeInt(-134217727, 4);
		write.writeInt(-268435455, 4);
		write.writeInt(-134217728, 4);
		write.writeInt(-268435456, 4);
		assertEquals(1, write.writeVarInt(0, true));
		assertEquals(1, write.writeVarInt(0, false));
		assertEquals(1, write.writeVarInt(63, true));
		assertEquals(1, write.writeVarInt(63, false));
		assertEquals(1, write.writeVarInt(64, true));
		assertEquals(2, write.writeVarInt(64, false));
		assertEquals(1, write.writeVarInt(127, true));
		assertEquals(2, write.writeVarInt(127, false));
		assertEquals(2, write.writeVarInt(128, true));
		assertEquals(2, write.writeVarInt(128, false));
		assertEquals(2, write.writeVarInt(8191, true));
		assertEquals(2, write.writeVarInt(8191, false));
		assertEquals(2, write.writeVarInt(8192, true));
		assertEquals(3, write.writeVarInt(8192, false));
		assertEquals(2, write.writeVarInt(16383, true));
		assertEquals(3, write.writeVarInt(16383, false));
		assertEquals(3, write.writeVarInt(16384, true));
		assertEquals(3, write.writeVarInt(16384, false));
		assertEquals(3, write.writeVarInt(2097151, true));
		assertEquals(4, write.writeVarInt(2097151, false));
		assertEquals(3, write.writeVarInt(1048575, true));
		assertEquals(3, write.writeVarInt(1048575, false));
		assertEquals(4, write.writeVarInt(134217727, true));
		assertEquals(4, write.writeVarInt(134217727, false));
		assertEquals(4, write.writeVarInt(268435455, true));
		assertEquals(5, write.writeVarInt(268435455, false));
		assertEquals(4, write.writeVarInt(134217728, true));
		assertEquals(5, write.writeVarInt(134217728, false));
		assertEquals(5, write.writeVarInt(268435456, true));
		assertEquals(5, write.writeVarInt(268435456, false));
		assertEquals(1, write.writeVarInt(-64, false));
		assertEquals(5, write.writeVarInt(-64, true));
		assertEquals(2, write.writeVarInt(-65, false));
		assertEquals(5, write.writeVarInt(-65, true));
		assertEquals(2, write.writeVarInt(-8192, false));
		assertEquals(5, write.writeVarInt(-8192, true));
		assertEquals(3, write.writeVarInt(-1048576, false));
		assertEquals(5, write.writeVarInt(-1048576, true));
		assertEquals(4, write.writeVarInt(-134217728, false));
		assertEquals(5, write.writeVarInt(-134217728, true));
		assertEquals(5, write.writeVarInt(-134217729, false));
		assertEquals(5, write.writeVarInt(-134217729, true));
		assertEquals(5, write.writeVarInt(1000000000, false));
		assertEquals(5, write.writeVarInt(1000000000, true));
		assertEquals(5, write.writeVarInt(Integer.MAX_VALUE - 1, false));
		assertEquals(5, write.writeVarInt(Integer.MAX_VALUE - 1, true));
		assertEquals(5, write.writeVarInt(Integer.MAX_VALUE, false));
		assertEquals(5, write.writeVarInt(Integer.MAX_VALUE, true));

		Input read = new Input(write.toBytes());
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
		assertEquals(0, read.readInt(1));
		assertEquals(63, read.readInt(1));
		assertEquals(64, read.readInt(1));
		assertEquals(127, read.readInt(1));
		assertEquals(128, read.readInt(2));
		assertEquals(8192, read.readInt(2));
		assertEquals(16384, read.readInt(2));
		assertEquals(2097151, read.readInt(3));
		assertEquals(1048575, read.readInt(3));
		assertEquals(134217727, read.readInt(4));
		assertEquals(268435455, read.readInt(4));
		assertEquals(134217728, read.readInt(4));
		assertEquals(268435456, read.readInt(4));
		assertEquals(-2097151, read.readInt(3));
		assertEquals(-1048575, read.readInt(3));
		assertEquals(-134217727, read.readInt(4));
		assertEquals(-268435455, read.readInt(4));
		assertEquals(-134217728, read.readInt(4));
		assertEquals(-268435456, read.readInt(4));
		assertTrue(read.canReadVarInt());
		assertTrue(read.canReadVarInt());
		assertTrue(read.canReadVarInt());
		assertEquals(0, read.readVarInt(true));
		assertEquals(0, read.readVarInt(false));
		assertEquals(63, read.readVarInt(true));
		assertEquals(63, read.readVarInt(false));
		assertEquals(64, read.readVarInt(true));
		assertEquals(64, read.readVarInt(false));
		assertEquals(127, read.readVarInt(true));
		assertEquals(127, read.readVarInt(false));
		assertEquals(128, read.readVarInt(true));
		assertEquals(128, read.readVarInt(false));
		assertEquals(8191, read.readVarInt(true));
		assertEquals(8191, read.readVarInt(false));
		assertEquals(8192, read.readVarInt(true));
		assertEquals(8192, read.readVarInt(false));
		assertEquals(16383, read.readVarInt(true));
		assertEquals(16383, read.readVarInt(false));
		assertEquals(16384, read.readVarInt(true));
		assertEquals(16384, read.readVarInt(false));
		assertEquals(2097151, read.readVarInt(true));
		assertEquals(2097151, read.readVarInt(false));
		assertEquals(1048575, read.readVarInt(true));
		assertEquals(1048575, read.readVarInt(false));
		assertEquals(134217727, read.readVarInt(true));
		assertEquals(134217727, read.readVarInt(false));
		assertEquals(268435455, read.readVarInt(true));
		assertEquals(268435455, read.readVarInt(false));
		assertEquals(134217728, read.readVarInt(true));
		assertEquals(134217728, read.readVarInt(false));
		assertEquals(268435456, read.readVarInt(true));
		assertEquals(268435456, read.readVarInt(false));
		assertEquals(-64, read.readVarInt(false));
		assertEquals(-64, read.readVarInt(true));
		assertEquals(-65, read.readVarInt(false));
		assertEquals(-65, read.readVarInt(true));
		assertEquals(-8192, read.readVarInt(false));
		assertEquals(-8192, read.readVarInt(true));
		assertEquals(-1048576, read.readVarInt(false));
		assertEquals(-1048576, read.readVarInt(true));
		assertEquals(-134217728, read.readVarInt(false));
		assertEquals(-134217728, read.readVarInt(true));
		assertEquals(-134217729, read.readVarInt(false));
		assertEquals(-134217729, read.readVarInt(true));
		assertEquals(1000000000, read.readVarInt(false));
		assertEquals(1000000000, read.readVarInt(true));
		assertEquals(Integer.MAX_VALUE - 1, read.readVarInt(false));
		assertEquals(Integer.MAX_VALUE - 1, read.readVarInt(true));
		assertEquals(Integer.MAX_VALUE, read.readVarInt(false));
		assertEquals(Integer.MAX_VALUE, read.readVarInt(true));
		assertFalse(read.canReadVarInt());

		Random random = new Random();
		for (int i = 0; i < 10000; i++) {
			int value = random.nextInt();
			write.reset();
			write.writeInt(value);
			write.writeVarInt(value, true);
			write.writeVarInt(value, false);
			int numOfBytes = (i % 4) + 1;
			write.writeInt(value, numOfBytes);

			read.setBuffer(write.toBytes());
			assertEquals(value, read.readInt());
			assertEquals(value, read.readVarInt(true));
			assertEquals(value, read.readVarInt(false));
			int numOfBytesMask = numOfBytes == 4 ? -1 : (1 << numOfBytes * 8) - 1;
			assertEquals(value & numOfBytesMask, read.readInt(numOfBytes) & numOfBytesMask);
		}
	}

	@Test
	void testLongs () throws IOException {
		runLongTest(new Output(4096));
		runLongTest(new Output(new ByteArrayOutputStream()));
	}

	private void runLongTest (Output write) throws IOException {
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
		write.writeLong(0, 1);
		write.writeLong(63, 1);
		write.writeLong(64, 1);
		write.writeLong(127, 1);
		write.writeLong(128, 2);
		write.writeLong(8192, 2);
		write.writeLong(16384, 2);
		write.writeLong(2097151, 3);
		write.writeLong(1048575, 3);
		write.writeLong(134217727, 4);
		write.writeLong(268435455, 4);
		write.writeLong(134217728, 4);
		write.writeLong(268435456, 4);
		write.writeLong(-2097151, 3);
		write.writeLong(-1048575, 3);
		write.writeLong(-134217727, 4);
		write.writeLong(-268435455, 4);
		write.writeLong(-134217728, 4);
		write.writeLong(-268435456, 4);
		assertEquals(1, write.writeVarLong(0, true));
		assertEquals(1, write.writeVarLong(0, false));
		assertEquals(1, write.writeVarLong(63, true));
		assertEquals(1, write.writeVarLong(63, false));
		assertEquals(1, write.writeVarLong(64, true));
		assertEquals(2, write.writeVarLong(64, false));
		assertEquals(1, write.writeVarLong(127, true));
		assertEquals(2, write.writeVarLong(127, false));
		assertEquals(2, write.writeVarLong(128, true));
		assertEquals(2, write.writeVarLong(128, false));
		assertEquals(2, write.writeVarLong(8191, true));
		assertEquals(2, write.writeVarLong(8191, false));
		assertEquals(2, write.writeVarLong(8192, true));
		assertEquals(3, write.writeVarLong(8192, false));
		assertEquals(2, write.writeVarLong(16383, true));
		assertEquals(3, write.writeVarLong(16383, false));
		assertEquals(3, write.writeVarLong(16384, true));
		assertEquals(3, write.writeVarLong(16384, false));
		assertEquals(3, write.writeVarLong(2097151, true));
		assertEquals(4, write.writeVarLong(2097151, false));
		assertEquals(3, write.writeVarLong(1048575, true));
		assertEquals(3, write.writeVarLong(1048575, false));
		assertEquals(4, write.writeVarLong(134217727, true));
		assertEquals(4, write.writeVarLong(134217727, false));
		assertEquals(4, write.writeVarLong(268435455L, true));
		assertEquals(5, write.writeVarLong(268435455L, false));
		assertEquals(4, write.writeVarLong(134217728L, true));
		assertEquals(5, write.writeVarLong(134217728L, false));
		assertEquals(5, write.writeVarLong(268435456L, true));
		assertEquals(5, write.writeVarLong(268435456L, false));
		assertEquals(1, write.writeVarLong(-64, false));
		assertEquals(9, write.writeVarLong(-64, true));
		assertEquals(2, write.writeVarLong(-65, false));
		assertEquals(9, write.writeVarLong(-65, true));
		assertEquals(2, write.writeVarLong(-8192, false));
		assertEquals(9, write.writeVarLong(-8192, true));
		assertEquals(3, write.writeVarLong(-1048576, false));
		assertEquals(9, write.writeVarLong(-1048576, true));
		assertEquals(4, write.writeVarLong(-134217728, false));
		assertEquals(9, write.writeVarLong(-134217728, true));
		assertEquals(5, write.writeVarLong(-134217729, false));
		assertEquals(9, write.writeVarLong(-134217729, true));

		Input read = new Input(write.toBytes());
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
		assertEquals(0, read.readLong(1));
		assertEquals(63, read.readLong(1));
		assertEquals(64, read.readLong(1));
		assertEquals(127, read.readLong(1));
		assertEquals(128, read.readLong(2));
		assertEquals(8192, read.readLong(2));
		assertEquals(16384, read.readLong(2));
		assertEquals(2097151, read.readLong(3));
		assertEquals(1048575, read.readLong(3));
		assertEquals(134217727, read.readLong(4));
		assertEquals(268435455, read.readLong(4));
		assertEquals(134217728, read.readLong(4));
		assertEquals(268435456, read.readLong(4));
		assertEquals(-2097151, read.readLong(3));
		assertEquals(-1048575, read.readLong(3));
		assertEquals(-134217727, read.readLong(4));
		assertEquals(-268435455, read.readLong(4));
		assertEquals(-134217728, read.readLong(4));
		assertEquals(-268435456, read.readLong(4));
		assertEquals(0, read.readVarLong(true));
		assertEquals(0, read.readVarLong(false));
		assertEquals(63, read.readVarLong(true));
		assertEquals(63, read.readVarLong(false));
		assertEquals(64, read.readVarLong(true));
		assertEquals(64, read.readVarLong(false));
		assertEquals(127, read.readVarLong(true));
		assertEquals(127, read.readVarLong(false));
		assertEquals(128, read.readVarLong(true));
		assertEquals(128, read.readVarLong(false));
		assertEquals(8191, read.readVarLong(true));
		assertEquals(8191, read.readVarLong(false));
		assertEquals(8192, read.readVarLong(true));
		assertEquals(8192, read.readVarLong(false));
		assertEquals(16383, read.readVarLong(true));
		assertEquals(16383, read.readVarLong(false));
		assertEquals(16384, read.readVarLong(true));
		assertEquals(16384, read.readVarLong(false));
		assertEquals(2097151, read.readVarLong(true));
		assertEquals(2097151, read.readVarLong(false));
		assertEquals(1048575, read.readVarLong(true));
		assertEquals(1048575, read.readVarLong(false));
		assertEquals(134217727, read.readVarLong(true));
		assertEquals(134217727, read.readVarLong(false));
		assertEquals(268435455, read.readVarLong(true));
		assertEquals(268435455, read.readVarLong(false));
		assertEquals(134217728, read.readVarLong(true));
		assertEquals(134217728, read.readVarLong(false));
		assertEquals(268435456, read.readVarLong(true));
		assertEquals(268435456, read.readVarLong(false));
		assertEquals(-64, read.readVarLong(false));
		assertEquals(-64, read.readVarLong(true));
		assertEquals(-65, read.readVarLong(false));
		assertEquals(-65, read.readVarLong(true));
		assertEquals(-8192, read.readVarLong(false));
		assertEquals(-8192, read.readVarLong(true));
		assertEquals(-1048576, read.readVarLong(false));
		assertEquals(-1048576, read.readVarLong(true));
		assertEquals(-134217728, read.readVarLong(false));
		assertEquals(-134217728, read.readVarLong(true));
		assertEquals(-134217729, read.readVarLong(false));
		assertEquals(-134217729, read.readVarLong(true));

		Random random = new Random();
		for (int i = 0; i < 10000; i++) {
			long value = random.nextLong();
			write.reset();
			write.writeLong(value);
			write.writeVarLong(value, true);
			write.writeVarLong(value, false);
			int numOfBytes = (i % 8) + 1;
			write.writeLong(value, numOfBytes);

			read.setBuffer(write.toBytes());
			assertEquals(value, read.readLong());
			assertEquals(value, read.readVarLong(true));
			assertEquals(value, read.readVarLong(false));
			long numOfBytesMask = numOfBytes == 8 ? -1 : (1L << numOfBytes * 8) - 1;
			assertEquals(value & numOfBytesMask, read.readLong(numOfBytes) & numOfBytesMask);
		}
	}

	@Test
	void testShorts () throws IOException {
		runShortTest(new Output(4096));
		runShortTest(new Output(new ByteArrayOutputStream()));
	}

	private void runShortTest (Output write) throws IOException {
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

		Input read = new Input(write.toBytes());
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
	void testFloats () throws IOException {
		runFloatTest(new Output(4096));
		runFloatTest(new Output(new ByteArrayOutputStream()));
	}

	private void runFloatTest (Output write) throws IOException {
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

		Input read = new Input(write.toBytes());
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
	void testDoubles () throws IOException {
		runDoubleTest(new Output(4096));
		runDoubleTest(new Output(new ByteArrayOutputStream()));
	}

	private void runDoubleTest (Output write) throws IOException {
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

		Input read = new Input(write.toBytes());
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
	void testBooleans () throws IOException {
		runBooleanTest(new Output(4096));
		runBooleanTest(new Output(new ByteArrayOutputStream()));
	}

	private void runBooleanTest (Output write) throws IOException {
		for (int i = 0; i < 100; i++) {
			write.writeBoolean(true);
			write.writeBoolean(false);
		}

		Input read = new Input(write.toBytes());
		for (int i = 0; i < 100; i++) {
			assertTrue(read.readBoolean());
			assertFalse(read.readBoolean());
		}
	}

	@Test
	void testChars () throws IOException {
		runCharTest(new Output(4096));
		runCharTest(new Output(new ByteArrayOutputStream()));
	}

	private void runCharTest (Output write) throws IOException {
		write.writeChar((char)0);
		write.writeChar((char)63);
		write.writeChar((char)64);
		write.writeChar((char)127);
		write.writeChar((char)128);
		write.writeChar((char)8192);
		write.writeChar((char)16384);
		write.writeChar((char)32767);
		write.writeChar((char)65535);

		Input read = new Input(write.toBytes());
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

	@Test
	void testInputWithOffset () throws Exception {
		final byte[] buf = new byte[30];
		final Input in = new Input(buf, 10, 10);
		assertEquals(10, in.available());
	}

	@Test
	void testSmallBuffers () throws Exception {
		ByteBuffer buf = ByteBuffer.allocate(1024);
		ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(buf);
		Output testOutput = new Output(byteBufferOutputStream);
		testOutput.writeBytes(new byte[512]);
		testOutput.writeBytes(new byte[512]);
		testOutput.flush();

		ByteBufferInputStream testInputs = new ByteBufferInputStream();
		((Buffer) buf).flip();
		testInputs.setByteBuffer(buf);
		Input input = new Input(testInputs, 512);
		byte[] toRead = new byte[512];
		input.readBytes(toRead);

		input.readBytes(toRead);
	}

	@Test
	void testVerySmallBuffers () throws Exception {
		Output out1 = new Output(4, -1);
		Output out2 = new ByteBufferOutput(4, -1);

		for (int i = 0; i < 16; i++) {
			out1.writeVarInt(92, false);
		}

		for (int i = 0; i < 16; i++) {
			out2.writeVarInt(92, false);
		}

		assertArrayEquals(out1.toBytes(), out2.toBytes());
	}

	@Test
	void testZeroLengthOutputs () throws Exception {
		Output output = new Output(0, 10000);
		kryo.writeClassAndObject(output, "Test string");

		Output byteBufferOutput = new ByteBufferOutput(0, 10000);
		kryo.writeClassAndObject(byteBufferOutput, "Test string");
	}

	@Test
	void testFlushRoundTrip () throws Exception {

		Kryo kryo = new Kryo();

		String s1 = "12345";

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ObjectOutputStream objOutput = new ObjectOutputStream(os);
		Output output = new Output(objOutput);

		kryo.writeClass(output, s1.getClass());
		kryo.writeObject(output, s1);
		output.flush();
// objOutput.flush(); // this layer wasn't flushed prior to this bugfix, add it for a workaround

		byte[] b = os.toByteArray();
		System.out.println("size: " + b.length);

		ByteArrayInputStream in = new ByteArrayInputStream(b);
		ObjectInputStream objIn = new ObjectInputStream(in);
		Input input = new Input(objIn);

		Registration r = kryo.readClass(input);
		String s2 = (String)kryo.readObject(input, r.getType());

		assertEquals(s1, s2);

	}

	@Test
	void testNewOutputMaxBufferSizeLessThanBufferSize () {
		int bufferSize = 2;
		int maxBufferSize = 1;

		assertThrows(IllegalArgumentException.class, () -> new Output(bufferSize, maxBufferSize));
	}

	@Test
	void testSetOutputMaxBufferSizeLessThanBufferSize () {
		int bufferSize = 2;
		int maxBufferSize = 1;

		Output output = new Output(bufferSize, bufferSize);
		assertNotNull(output);

		assertThrows(IllegalArgumentException.class, () -> output.setBuffer(new byte[bufferSize], maxBufferSize));
	}

	@Test
	void testNewOutputMaxBufferSizeIsMinusOne () {
		int bufferSize = 2;
		int maxBufferSize = -1;

		Output output = new Output(bufferSize, maxBufferSize);
		assertNotNull(output);
		// This test should pass as long as no exception thrown
	}

	@Test
	void testSetOutputMaxBufferSizeIsMinusOne () {
		int bufferSize = 2;
		int maxBufferSize = -1;

		Output output = new Output(bufferSize, bufferSize);
		assertNotNull(output);
		output.setBuffer(new byte[bufferSize], maxBufferSize);
		// This test should pass as long as no exception thrown
	}

}
