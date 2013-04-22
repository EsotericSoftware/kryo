
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.kryo.serializers.DeflateSerializer;

/** @author Nathan Sweet <misc@n4te.com> */
public class DeflateSerializerTest extends KryoTestCase {
	public void testString () {
		kryo.register(String.class, new DeflateSerializer(new StringSerializer()));
		roundTrip(15, 15, "abcdefabcdefabcdefabcdefabcdefabcdefabcdef");
	}

	public void testGraph () {
		kryo.register(Message.class);
		kryo.register(MessageType.class);
		kryo.register(ServerPhysicsUpdate.class, new DeflateSerializer(kryo.getDefaultSerializer(ServerPhysicsUpdate.class)));

		ServerPhysicsUpdate physicsUpdate = new ServerPhysicsUpdate();
		physicsUpdate.value = 1;
		Message message = new Message();
		message.type = MessageType.SERVER_UPDATE;
		message.data = physicsUpdate;

		roundTrip(8, 8, message);
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
			result = prime * result + ((data == null) ? 0 : data.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
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
