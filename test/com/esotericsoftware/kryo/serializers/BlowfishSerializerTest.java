/* Copyright (c) 2008-2026, Nathan Sweet
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

package com.esotericsoftware.kryo.serializers;

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.ByteArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;

import javax.crypto.KeyGenerator;

import org.junit.jupiter.api.Test;

/** @author Nathan Sweet */
class BlowfishSerializerTest extends KryoTestCase {
	@Test
	void testZip () throws Exception {
		byte[] key = KeyGenerator.getInstance("Blowfish").generateKey().getEncoded();
		kryo.register(String.class, new BlowfishSerializer(new StringSerializer(), key));
		roundTrip(49, "abcdefabcdefabcdefabcdefabcdefabcdefabcdef");
	}

	// The input the decrypted object is read from has the maxArraySize of the outer input.
	@Test
	void testMaxArraySize () throws Exception {
		byte[] key = KeyGenerator.getInstance("Blowfish").generateKey().getEncoded();
		BlowfishSerializer serializer = new BlowfishSerializer(new ByteArraySerializer(), key);
		Output output = new Output(64);
		kryo.writeObject(output, new byte[11], serializer);
		Input input = new Input(output.toBytes());
		input.setMaxArraySize(11);
		assertEquals(11, kryo.readObject(input, byte[].class, serializer).length);
		Input limited = new Input(output.toBytes());
		limited.setMaxArraySize(10);
		KryoException ex = assertThrows(KryoException.class, () -> kryo.readObject(limited, byte[].class, serializer));
		assertTrue(ex.getMessage().contains("maxArraySize"), ex.getMessage());
	}
}
