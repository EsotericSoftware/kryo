
package com.esotericsoftware.kryo;

import static org.junit.Assert.*;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.LongSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;

import java.io.IOException;

import org.junit.Test;

public class RegistrationTest {
	@Test
	public void testDefaultSerializerOrder () {
		Kryo kryo = new Kryo();
		kryo.addDefaultSerializer(Fruit.class, new FieldSerializer(kryo, Fruit.class));
		FieldSerializer appleSerializer = new FieldSerializer(kryo, Apple.class);
		kryo.addDefaultSerializer(Apple.class, appleSerializer);
		assertSame(appleSerializer, kryo.getDefaultSerializer(Apple.class));
	}

	@Test
	public void testReplaceRegistration () throws IOException {
		Kryo kryo = new Kryo();
		kryo.register(double[].class, 7); // Replace long with double[].
		kryo.register(Some.class);
		kryo.register(long.class, new LongSerializer());

		{
			Some<Integer> s = new Some(2);
			Output output = new Output(1024);
			kryo.writeObject(output, s);
			output.flush();

			Input input = new Input(output.getBuffer());
			Some r = kryo.readObject(input, Some.class); // OK!
			assertEquals(r.value, 2);
		}

		{
			Some<Long> s = new Some(2L);
			Output output = new Output(1024);
			kryo.writeObject(output, s);
			output.flush();

			Input input = new Input(output.getBuffer());
			Some r = kryo.readObject(input, Some.class);
			assertEquals(r.value, 2L);
		}
	}

	static public class Some<T> {
		public T value;

		public Some () {
		}

		public Some (T t) {
			this.value = t;
		}
	}

	static public class Fruit {
	}

	static public class Apple extends Fruit {
	}
}
