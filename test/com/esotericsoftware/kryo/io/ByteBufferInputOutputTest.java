/* Copyright (c) 2008-2018, Nathan Sweet
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

import static org.junit.Assert.*;

import com.esotericsoftware.kryo.KryoTestCase;

import java.io.ByteArrayInputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.junit.Test;

public class ByteBufferInputOutputTest extends KryoTestCase {
	@Test
	public void testByteBufferInputEnd () {
		ByteBufferInput in = new ByteBufferInput(new ByteArrayInputStream(new byte[] {123, 0, 0, 0}));
		assertFalse(in.end());
		in.setPosition(4);
		assertTrue(in.end());
	}

	@Test
	public void testByteBufferInputPosition () {
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4096);
		ByteBufferInput inputBuffer = new ByteBufferInput(byteBuffer);
		assertEquals(0, inputBuffer.position());
		assertEquals(0, inputBuffer.getByteBuffer().position());
		inputBuffer.setPosition(5);
		assertEquals(5, inputBuffer.position());
		assertEquals(5, inputBuffer.getByteBuffer().position());
	}

	@Test
	public void testByteBufferInputLimit () {
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4096);
		ByteBufferInput inputBuffer = new ByteBufferInput(byteBuffer);
		assertEquals(4096, inputBuffer.limit());
		assertEquals(4096, inputBuffer.getByteBuffer().limit());
		inputBuffer.setLimit(1000);
		assertEquals(1000, inputBuffer.limit());
		assertEquals(1000, inputBuffer.getByteBuffer().limit());
	}

	@Test
	public void testByteBufferInputSkip () {
		ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
		ByteBufferInput inputBuffer = new ByteBufferInput(buffer);
		assertEquals(0, inputBuffer.getByteBuffer().position());
		inputBuffer.skip(5);
		assertEquals(5, inputBuffer.getByteBuffer().position());
	}

	@Test
	public void testByteBufferOutputPosition () {
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
}
