
package com.esotericsoftware.kryo;

import static com.esotericsoftware.minlog.Log.*;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.LongSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

public class RegistrationTest extends TestCase {
	public void testReplaceRegistration () throws IOException {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(true);
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
}
