/* Copyright (c) 2008-2017, Nathan Sweet
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

public class ByteBufferInputOutputTest extends KryoTestCase {

	public void testByteBufferOutputResetEndiannessAfterException () {
		ByteBufferOutput outputBuffer = new ByteBufferOutput(10, 10);
		boolean exceptionTriggered = false;
		try {
			for (int i = 0; i < 10; i++) {
				outputBuffer.writeInt(1234, true);
			}
		} catch (KryoException exception) {
			exceptionTriggered = true;
			assertEquals(outputBuffer.order(), outputBuffer.getByteBuffer().order());
		}

		assertTrue(exceptionTriggered);

		exceptionTriggered = false;
		try {
			for (int i = 0; i < 10; i++) {
				outputBuffer.writeLong(1234L, true);
			}
		} catch (KryoException exception) {
			assertEquals(outputBuffer.order(), outputBuffer.getByteBuffer().order());
			exceptionTriggered = true;
		}
		assertTrue(exceptionTriggered);
	}

	public void testByteBufferInputPosition () {
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4096);
		ByteBufferInput inputBuffer = new ByteBufferInput(byteBuffer);
		assertEquals(0, inputBuffer.position());
		assertEquals(0, inputBuffer.getByteBuffer().position());
		inputBuffer.setPosition(5);
		assertEquals(5, inputBuffer.position());
		assertEquals(5, inputBuffer.getByteBuffer().position());
	}

	public void testByteBufferInputLimit () {
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4096);
		ByteBufferInput inputBuffer = new ByteBufferInput(byteBuffer);
		assertEquals(4096, inputBuffer.limit());
		assertEquals(4096, inputBuffer.getByteBuffer().limit());
		inputBuffer.setLimit(1000);
		assertEquals(1000, inputBuffer.limit());
		assertEquals(1000, inputBuffer.getByteBuffer().limit());
	}

	public void testByteBufferInputSetBufferEndianness () {
		ByteBufferInput inputBuffer = new ByteBufferInput();
		assertEquals(ByteOrder.BIG_ENDIAN, inputBuffer.order());

		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4096);
		assertEquals(ByteOrder.BIG_ENDIAN, byteBuffer.order());
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		assertEquals(ByteOrder.LITTLE_ENDIAN, byteBuffer.order());

		inputBuffer.setBuffer(byteBuffer);
		assertEquals(byteBuffer.order(), inputBuffer.order());
	}

	public void testByteBufferInputSkip () {
		ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
		ByteBufferInput inputBuffer = new ByteBufferInput(buffer);
		assertEquals(0, inputBuffer.getByteBuffer().position());
		inputBuffer.skip(5);
		assertEquals(5, inputBuffer.getByteBuffer().position());
	}

	public void testByteBufferOutputPosition () {
		ByteBufferOutput outputBuffer = new ByteBufferOutput(4096);
		assertEquals(0, outputBuffer.position());
		assertEquals(0, outputBuffer.getByteBuffer().position());
		outputBuffer.setPosition(5);
		assertEquals(5, outputBuffer.position());
		outputBuffer.writeInt(10);

		ByteBuffer byteBuffer = outputBuffer.getByteBuffer().duplicate();
		byteBuffer.flip();

		ByteBufferInput inputBuffer = new ByteBufferInput(byteBuffer);
		inputBuffer.skip(5);
		assertEquals(5, byteBuffer.position());
		assertEquals(10, inputBuffer.readInt());
		assertEquals(9, byteBuffer.position());
	}

	public void testByteBufferOutputSetOrder () {
		ByteBufferOutput outputBuffer = new ByteBufferOutput(4096);
		assertEquals(ByteOrder.BIG_ENDIAN, outputBuffer.order());
		assertEquals(ByteOrder.BIG_ENDIAN, outputBuffer.getByteBuffer().order());

		outputBuffer.order(ByteOrder.LITTLE_ENDIAN);
		assertEquals(ByteOrder.LITTLE_ENDIAN, outputBuffer.order());
		assertEquals(ByteOrder.LITTLE_ENDIAN, outputBuffer.getByteBuffer().order());
	}

	public void testByteBufferByteOrderTheSameAfterGrowingForVarInt () {
		final ByteBufferOutput outputBuffer = new ByteBufferOutput(1, -1);
		final ByteOrder byteOrder = outputBuffer.order();
		outputBuffer.writeInt(300, true);
		assertEquals(byteOrder, outputBuffer.order());
	}
}
