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

import com.esotericsoftware.kryo.KryoException;
import org.junit.jupiter.api.Test;

/** @author Nathan Sweet */
class ChunkedTest {
	@Test
	void testChunks () {
		Output output = new Output(512);
		output.writeInt(1234);
		OutputChunked outputChunked = new OutputChunked(output);
		outputChunked.writeInt(1);
		outputChunked.endChunk();
		outputChunked.writeInt(2);
		outputChunked.endChunk();
		outputChunked.writeInt(3);
		outputChunked.endChunk();
		outputChunked.writeInt(4);
		outputChunked.endChunk();
		outputChunked.writeInt(5);
		outputChunked.endChunk();
		output.writeInt(5678);
		output.close();

		Input input = new Input(output.getBuffer());
		assertEquals(1234, input.readInt());
		InputChunked inputChunked = new InputChunked(input);
		assertEquals(1, inputChunked.readInt());
		inputChunked.nextChunk();
		inputChunked.nextChunk(); // skip 3
		assertEquals(3, inputChunked.readInt());
		inputChunked.nextChunk();
		inputChunked.nextChunk(); // skip 4
		assertEquals(5, inputChunked.readInt());
		assertEquals(5678, input.readInt());
		input.close();
	}

	@Test
	void testSkipAfterUnderFlow () {
		Output output = new Output(512);
		OutputChunked outputChunked = new OutputChunked(output);
		outputChunked.writeInt(1);
		outputChunked.endChunk();
		outputChunked.writeInt(2);
		outputChunked.endChunk();
		output.close();

		Input input = new Input(output.getBuffer());
		InputChunked inputChunked = new InputChunked(input);
		assertEquals(1, inputChunked.readInt());
		// trigger buffer underflow
		assertThrows(KryoException.class, inputChunked::readInt);
		inputChunked.nextChunk();
		assertEquals(2, inputChunked.readInt());
		input.close();
	}
}
