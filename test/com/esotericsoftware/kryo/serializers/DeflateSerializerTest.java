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

import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.ByteArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** @author Nathan Sweet */
class DeflateSerializerTest extends KryoTestCase {
	@Test
	void testString () {
		kryo.register(String.class, new DeflateSerializer(new StringSerializer()));
		roundTrip(14, "abcdefabcdefabcdefabcdefabcdefabcdefabcdef");
	}

	@Test
	void testGraph () {
		kryo.register(Message.class);
		kryo.register(MessageType.class);
		kryo.register(ServerPhysicsUpdate.class, new DeflateSerializer(kryo.getDefaultSerializer(ServerPhysicsUpdate.class)));

		ServerPhysicsUpdate physicsUpdate = new ServerPhysicsUpdate();
		physicsUpdate.value = 1;
		Message message = new Message();
		message.type = MessageType.SERVER_UPDATE;
		message.data = physicsUpdate;

		roundTrip(8, message);
	}

	// The inflater doesn't read the last compressed byte if it only ends the last deflate block. That byte and the end of the
	// chunks must still be skipped, so the data after the compressed object is read correctly (#1312).
	@Test
	void testDataAfterCompressedObject () {
		DeflateSerializer serializer = new DeflateSerializer(new ByteArraySerializer());
		Random random = new Random(1);
		for (int size = 1; size <= 4000; size++) {
			StringBuilder text = new StringBuilder();
			while (text.length() < size)
				text.append("value-").append(random.nextInt(100000)).append(' ');
			byte[] payload = text.substring(0, size).getBytes(StandardCharsets.UTF_8);

			Output output = new Output(size + 100, -1);
			kryo.writeObject(output, payload, serializer);
			output.writeString("after");
			Input input = new Input(output.toBytes());
			assertArrayEquals(payload, kryo.readObject(input, byte[].class, serializer));
			assertEquals("after", input.readString(), "size " + size);
		}
	}

	public static class ServerPhysicsUpdate {
		public int value;

		public ServerPhysicsUpdate () {
		}

		public int hashCode () {
			final int prime = 31;
			int result = 1;
			result = prime * result + value;
			return result;
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ServerPhysicsUpdate other = (ServerPhysicsUpdate)obj;
			if (value != other.value) return false;
			return true;
		}
	}

	public static enum MessageType {
		SERVER_UPDATE
	}

	public static class Message {
		public MessageType type;
		public Object data;

		public Message () {
		}

		public int hashCode () {
			final int prime = 31;
			int result = 1;
			result = prime * result + (data == null ? 0 : data.hashCode());
			result = prime * result + (type == null ? 0 : type.hashCode());
			return result;
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Message other = (Message)obj;
			if (data == null) {
				if (other.data != null) return false;
			} else if (!data.equals(other.data)) return false;
			if (type != other.type) return false;
			return true;
		}
	}
}
