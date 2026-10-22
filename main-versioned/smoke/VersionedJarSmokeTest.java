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

import com.esotericsoftware.kryo.kryo6.Kryo;
import com.esotericsoftware.kryo.kryo6.SerializerFactory.FieldSerializerFactory;
import com.esotericsoftware.kryo.kryo6.io.Input;
import com.esotericsoftware.kryo.kryo6.io.Output;
import com.esotericsoftware.kryo.kryo6.objenesis.strategy.StdInstantiatorStrategy;
import com.esotericsoftware.kryo.kryo6.util.Log;
import com.esotericsoftware.kryo.kryo6.util.ObjenesisStrategy;
import java.util.ArrayList;
import java.util.List;

/** Smoke test of the versioned jar, run by the build with the Java source launcher and only the jar on the class path: Kryo with
 * the relocated packages, code generation, also with the relocated ASM (-Dkryo.codeGeneration.backend=asm), and the relocated
 * Objenesis. */
public class VersionedJarSmokeTest {
	public static class Data {
		public int number;
		public String text;
		public List<String> list = new ArrayList<>();
	}

	public static class NoDefaultConstructor {
		public int value;

		public NoDefaultConstructor (int value) {
			this.value = value;
		}
	}

	public static void main (String[] args) {
		Data data = new Data();
		data.number = 42;
		data.text = "kryo";
		data.list.add("a");

		Kryo kryo = new Kryo();
		kryo.register(Data.class);
		kryo.register(ArrayList.class);
		check(roundTrip(kryo, data), data);

		// Code generation, with the Class-File API on Java 24+ or the relocated ASM.
		List<String> messages = new ArrayList<>();
		Log.setLogger(new Log.Logger() {
			public void log (int level, String category, String message, Throwable ex) {
				messages.add(message);
			}
		});
		Log.set(Log.LEVEL_TRACE);
		kryo = new Kryo();
		FieldSerializerFactory factory = new FieldSerializerFactory();
		factory.getConfig().setCodeGeneration(true);
		kryo.setDefaultSerializer(factory);
		kryo.register(Data.class);
		kryo.register(ArrayList.class);
		check(roundTrip(kryo, data), data);
		Log.set(Log.LEVEL_INFO);
		if (messages.stream().noneMatch(message -> message.startsWith("Generated code for the fields of")))
			throw new AssertionError("No code generated, Java " + Runtime.version());

		// Objenesis, for a class without a zero argument constructor.
		kryo = new Kryo();
		kryo.setInstantiatorStrategy(new ObjenesisStrategy(new StdInstantiatorStrategy()));
		kryo.register(NoDefaultConstructor.class);
		if (roundTrip(kryo, new NoDefaultConstructor(7)).value != 7) throw new AssertionError("Objenesis");

		System.out.println("Versioned jar works on Java " + Runtime.version());
	}

	@SuppressWarnings("unchecked")
	static <T> T roundTrip (Kryo kryo, T object) {
		Output output = new Output(1024, -1);
		kryo.writeObject(output, object);
		return (T)kryo.readObject(new Input(output.toBytes()), object.getClass());
	}

	static void check (Data read, Data data) {
		if (read.number != data.number || !read.text.equals(data.text) || !read.list.equals(data.list))
			throw new AssertionError("Round trip failed");
	}
}
