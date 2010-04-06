
package com.esotericsoftware.kryo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;

import com.esotericsoftware.kryo.serialize.LongSerializer;

public class KryoTest extends KryoTestCase {
	public void testNulls () {
		Kryo kryo = new Kryo();
		kryo.register(ArrayList.class);
		buffer.clear();
		kryo.writeObject(buffer, null);
		assertEquals("Incorrect length.", 1, buffer.position());
		buffer.flip();
		assertNull(kryo.readObject(buffer, ArrayList.class));

		buffer.clear();
		kryo.writeClassAndObject(buffer, null);
		assertEquals("Incorrect length.", 1, buffer.position());
		buffer.flip();
		assertNull(kryo.readClassAndObject(buffer));

		buffer.clear();
		kryo.writeClass(buffer, null);
		assertEquals("Incorrect length.", 1, buffer.position());
		buffer.flip();
		assertNull(kryo.readClass(buffer));
	}

	public void testOptionalRegistration () {
		Kryo kryo = new Kryo();
		kryo.setRegistrationOptional(true);
		roundTrip(kryo, 5, "abc");
		roundTrip(kryo, 32, toList("1", "2", "3"));
		roundTrip(kryo, 55, toList("1", "2", toList("3")));
	}

	public void testDefaultSerializerAnnotation () {
		Kryo kryo = new Kryo();
		kryo.register(HasDefaultSerializerAnnotation.class);
		HasDefaultSerializerAnnotation someClass = new HasDefaultSerializerAnnotation();
		someClass.setTime(12345);
		roundTrip(kryo, 3, someClass);
	}

	@DefaultSerializer(HasDefaultSerializerAnnotationSerializer.class)
	static public class HasDefaultSerializerAnnotation extends Date {
	}

	static public class HasDefaultSerializerAnnotationSerializer extends Serializer {
		private LongSerializer longSerializer = new LongSerializer(true);

		public Date readObjectData (ByteBuffer buffer, Class type) {
			HasDefaultSerializerAnnotation date = new HasDefaultSerializerAnnotation();
			date.setTime(LongSerializer.get(buffer, true));
			return date;
		}

		public void writeObjectData (ByteBuffer buffer, Object object) {
			LongSerializer.put(buffer, ((Date)object).getTime(), true);
		}
	}
}
