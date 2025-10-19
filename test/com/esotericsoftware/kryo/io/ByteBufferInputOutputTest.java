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

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.KryoTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

@SuppressWarnings("all")
class ByteBufferInputOutputTest extends KryoTestCase {
	@Test
	void testByteBufferInputEnd () {
		ByteBufferInput in = new ByteBufferInput(new ByteArrayInputStream(new byte[] {123, 0, 0, 0}));
		assertFalse(in.end());
		in.setPosition(4);
		assertTrue(in.end());
	}

	@Test
	void testByteBufferInputPosition () {
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4096);
		ByteBufferInput inputBuffer = new ByteBufferInput(byteBuffer);
		assertEquals(0, inputBuffer.position());
		assertEquals(0, inputBuffer.getByteBuffer().position());
		inputBuffer.setPosition(5);
		assertEquals(5, inputBuffer.position());
		assertEquals(5, inputBuffer.getByteBuffer().position());
	}

	@Test
	void testByteBufferInputLimit () {
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4096);
		ByteBufferInput inputBuffer = new ByteBufferInput(byteBuffer);
		assertEquals(4096, inputBuffer.limit());
		assertEquals(4096, inputBuffer.getByteBuffer().limit());
		inputBuffer.setLimit(1000);
		assertEquals(1000, inputBuffer.limit());
		assertEquals(1000, inputBuffer.getByteBuffer().limit());
	}

	@Test
	void testByteBufferInputSkip () {
		ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
		ByteBufferInput inputBuffer = new ByteBufferInput(buffer);
		assertEquals(0, inputBuffer.getByteBuffer().position());
		inputBuffer.skip(5);
		assertEquals(5, inputBuffer.getByteBuffer().position());
	}

	@Test
	void testByteBufferOutputPosition () {
		ByteBufferOutput outputBuffer = new ByteBufferOutput(4096);
		assertEquals(0, outputBuffer.position());
		assertEquals(0, outputBuffer.getByteBuffer().position());
		outputBuffer.setPosition(5);
		assertEquals(5, outputBuffer.position());
		outputBuffer.writeInt(10);

		ByteBuffer byteBuffer = outputBuffer.getByteBuffer().duplicate();
		((Buffer) byteBuffer).flip();

		ByteBufferInput inputBuffer = new ByteBufferInput(byteBuffer);
		inputBuffer.skip(5);
		assertEquals(5, byteBuffer.position());
		assertEquals(10, inputBuffer.readInt());
		assertEquals(9, byteBuffer.position());
	}
	
	@Test
	void testOverflow () {
		ByteBufferOutput buffer = new ByteBufferOutput(1);
		buffer.writeByte(51);
		KryoBufferOverflowException thrown = assertThrows(
			KryoBufferOverflowException.class,
			() -> buffer.writeByte(65),
			"Exception expected but none thrown"
		);

		assertTrue(thrown.getMessage().startsWith("Buffer overflow"));
	}

	@Test
	void testUnderflow () {
		ByteBufferInput buffer = new ByteBufferInput(1);
	
		KryoBufferUnderflowException thrown = assertThrows(
			KryoBufferUnderflowException.class,
			() -> buffer.readBytes(2),
			"Exception expected but none thrown"
		);

		assertTrue(thrown.getMessage().equals("Buffer underflow."));
	}

	@Test
	void testStrings () throws IOException {
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

	private void runStringTest (int length) throws IOException {
		Output write = new ByteBufferOutput(1024, -1);
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < length; i++)
			buffer.append((char)i);

		String value = buffer.toString();
		write.writeString(value);
		write.writeString(value);
		Input read = new ByteBufferInput(write.toBytes());
		assertEquals(value, read.readString());
		assertEquals(value, read.readStringBuilder().toString());

		write.reset();
		write.writeString(buffer.toString());
		write.writeString(buffer.toString());
		read = new ByteBufferInput(write.toBytes());
		assertEquals(value, read.readString());
		assertEquals(value, read.readStringBuilder().toString());

		if (length <= 127) {
			write.reset();
			write.writeAscii(value);
			write.writeAscii(value);
			read = new ByteBufferInput(write.toBytes());
			assertEquals(value, read.readString());
			assertEquals(value, read.readStringBuilder().toString());
		}
	}

}
