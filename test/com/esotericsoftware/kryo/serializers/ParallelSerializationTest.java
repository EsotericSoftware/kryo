
package com.esotericsoftware.kryo.serializers;

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.util.Pool;

import java.util.Objects;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;

class ParallelSerializationTest {

	private final Pool<Kryo> pool = new Pool<Kryo>(true, false, 8) {
		@Override
		protected Kryo create () {
			Kryo kryo = new Kryo();
			kryo.setRegistrationRequired(false);
			kryo.setReferences(true);
			kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
			return kryo;
		}
	};

	@Test
	void serializeAndDeserialize () {
		IntStream.range(0, 100)
			.parallel()
			.forEach(it -> {
				for (int i = 0; i < 10; i++) {
					roundTrip(new TestClass(new GenericClass<>("test-" + it + "-" + i)));
				}
			});
	}

	private void roundTrip (Object target) {
		byte[] serialized = serialize(target);
		Object result = deserialize(serialized);
		assertEquals(result, target);
	}

	private byte[] serialize (Object sample) {
		Kryo kryo = pool.obtain();
		Output out = new Output(4096, -1);
		kryo.writeClassAndObject(out, sample);
		byte[] serialized = out.toBytes();
		out.close();
		pool.free(kryo);
		return serialized;
	}

	private Object deserialize (byte[] serialized) {
		Kryo kryo = pool.obtain();
		Input in = new Input(serialized);
		Object result = kryo.readClassAndObject(in);
		in.close();
		pool.free(kryo);
		return result;
	}

	static class TestClass {

		private final GenericSuperClass<String> value;

		public TestClass(GenericSuperClass<String> value) {
			this.value = value;
		}

		public GenericSuperClass<String> value () {
			return value;
		}

		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TestClass that = (TestClass)o;
			return Objects.equals(value, that.value);
		}

		@Override
		public int hashCode () {
			return Objects.hash(value);
		}
	}

	static abstract class GenericSuperClass<T> {
	}

	static class GenericClass<T> extends GenericSuperClass<T> {
		private final T value;

		public GenericClass(T value) {
			this.value = value;
		}

		public T value () {
			return value;
		}

		@Override
		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			GenericClass<?> genericClass = (GenericClass<?>)o;
			return Objects.equals(value, genericClass.value);
		}

		@Override
		public int hashCode () {
			return value.hashCode();
		}
	}
	
}
