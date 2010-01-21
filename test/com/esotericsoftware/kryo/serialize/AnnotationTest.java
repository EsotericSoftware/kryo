
package com.esotericsoftware.kryo.serialize;

import java.nio.ByteBuffer;
import java.util.Date;

import junit.framework.TestCase;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

public class AnnotationTest extends TestCase {
	private ByteBuffer buffer = ByteBuffer.allocateDirect(500);

	public void testAnnotation () {
		Kryo kryo = new Kryo();
		kryo.register(SomeClass.class);
		SomeClass value1 = new SomeClass();
		kryo.writeObject(buffer, value1);
		buffer.flip();
		SomeClass value2 = kryo.readObject(buffer, SomeClass.class);
		assertEquals(value1, value2);
	}

	@DefaultSerializer(SomeClassSerializer.class)
	static public class SomeClass extends Date {
	}

	static public class SomeClassSerializer extends Serializer {
		private LongSerializer longSerializer = new LongSerializer(true);

		public Date readObjectData (ByteBuffer buffer, Class type) {
			long time = LongSerializer.get(buffer, true);
			SomeClass date = new SomeClass();
			date.setTime(time);
			return date;
		}

		public void writeObjectData (ByteBuffer buffer, Object object) {
			LongSerializer.put(buffer, ((Date)object).getTime(), true);
		}
	}
}
