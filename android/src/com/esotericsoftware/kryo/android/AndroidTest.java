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

package com.esotericsoftware.kryo.android;

import com.esotericsoftware.kryo.AndroidSerializationCompat;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Kryo5Compatibility;
import com.esotericsoftware.kryo.SerializerFactory.CompatibleFieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.TaggedFieldSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer.CompatibleFieldSerializerConfig;
import com.esotericsoftware.kryo.serializers.ExternalizableSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.TaggedFieldSerializerConfig;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;

import android.os.Build;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Serializes and copies objects on Android, and runs {@link AndroidSerializationCompat}, run with app_process by android/test.sh.
 * Exits with 1 if anything fails. */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AndroidTest {
	static boolean failed;

	public static void main (String[] args) {
		System.out.println("Android API level " + Build.VERSION.SDK_INT + ", " + System.getProperty("java.vm.name"));

		test("Output and Input", () -> {
			Output output = new Output(16, -1);
			output.writeInt(0x12345678);
			output.writeLong(0x123456789ABCDEF0L);
			output.writeFloat(1.5f);
			output.writeDouble(2.25);
			output.writeShort(-2);
			output.writeChar('x');
			output.writeBoolean(true);
			output.writeVarInt(-5, false);
			output.writeVarLong(1L << 40, true);
			output.writeString("ascii");
			output.writeString("unicode ä€");
			output.writeInts(new int[] {1, -2, 3}, 0, 3);
			output.writeLongs(new long[] {1L << 50, -2}, 0, 2);
			output.writeFloats(new float[] {1.5f}, 0, 1);
			output.writeDoubles(new double[] {2.5}, 0, 1);
			Input input = new Input(output.toBytes());
			check(input.readInt() == 0x12345678 && input.readLong() == 0x123456789ABCDEF0L && input.readFloat() == 1.5f
				&& input.readDouble() == 2.25 && input.readShort() == -2 && input.readChar() == 'x' && input.readBoolean()
				&& input.readVarInt(false) == -5 && input.readVarLong(true) == 1L << 40);
			check(input.readString().equals("ascii") && input.readString().equals("unicode ä€"));
			check(Arrays.equals(input.readInts(3), new int[] {1, -2, 3})
				&& Arrays.equals(input.readLongs(2), new long[] {1L << 50, -2}) && input.readFloats(1)[0] == 1.5f
				&& input.readDoubles(1)[0] == 2.5);
		});

		test("FieldSerializer", () -> {
			Pojo pojo = Pojo.create();
			check(roundTrip(kryo(), pojo).equals(pojo));
			check(kryo().copy(pojo).equals(pojo));
		});

		test("Final fields", () -> {
			Final object = roundTrip(kryo(), new Final(9, "nine"));
			check(object.number == 9 && object.text.equals("nine"));
		});

		test("Default serializers and references", () -> {
			Kryo kryo = kryo();
			kryo.setReferences(true);
			HashMap<Object, Object> map = new HashMap<>();
			map.put("list", new ArrayList<>(Arrays.asList(1, 2L, 3.0, "x", null)));
			map.put("tree", new TreeMap<>(Collections.singletonMap("k", "v")));
			map.put("enum", Thread.State.RUNNABLE);
			map.put("enumSet", EnumSet.of(Thread.State.NEW, Thread.State.BLOCKED));
			map.put("date", new Date(1234));
			map.put("bigInteger", new BigInteger("123456789012345678901234567890"));
			map.put("bigDecimal", new BigDecimal("1.25"));
			map.put("uuid", new UUID(1, 2));
			map.put("locale", Locale.GERMANY);
			map.put("bitSet", BitSet.valueOf(new long[] {5}));
			map.put("self", map);
			HashMap<Object, Object> read = roundTrip(kryo, map);
			check(read.remove("self") == read);
			map.remove("self");
			check(read.equals(map));
			check(kryo.copy(map).equals(map));
		});

		test("CompatibleFieldSerializer", () -> {
			Kryo kryo = kryo();
			kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
			Pojo pojo = Pojo.create();
			check(roundTrip(kryo, pojo).equals(pojo));
		});

		test("CompatibleFieldSerializer, chunked", () -> {
			CompatibleFieldSerializerConfig config = new CompatibleFieldSerializerConfig();
			config.setChunkedEncoding(true);
			Kryo kryo = kryo();
			kryo.setDefaultSerializer(new CompatibleFieldSerializerFactory(config));
			Pojo pojo = Pojo.create();
			check(roundTrip(kryo, pojo).equals(pojo));
		});

		test("TaggedFieldSerializer, chunked", () -> {
			TaggedFieldSerializerConfig config = new TaggedFieldSerializerConfig();
			config.setChunkedEncoding(true);
			Kryo kryo = kryo();
			kryo.setDefaultSerializer(new TaggedFieldSerializerFactory(config));
			Tagged tagged = new Tagged();
			tagged.number = 5;
			tagged.text = "five";
			Tagged read = roundTrip(kryo, tagged);
			check(read.number == 5 && read.text.equals("five"));
		});

		test("VersionFieldSerializer", () -> {
			Kryo kryo = kryo();
			kryo.setDefaultSerializer(VersionFieldSerializer.class);
			Pojo pojo = Pojo.create();
			check(roundTrip(kryo, pojo).equals(pojo));
		});

		test("Kryo5Compatibility", () -> {
			Kryo kryo = kryo();
			Kryo5Compatibility.configure(kryo);
			Pojo pojo = Pojo.create();
			check(roundTrip(kryo, pojo).equals(pojo));
		});

		test("ExternalizableSerializer", () -> {
			Kryo kryo = kryo();
			kryo.register(Externalized.class, new ExternalizableSerializer());
			Externalized object = new Externalized();
			object.number = 42;
			check(roundTrip(kryo, object).number == 42);
		});

		test("ExternalizableSerializer with JavaSerializer", () -> {
			Kryo kryo = kryo();
			kryo.register(ExternalizedWithReadResolve.class, new ExternalizableSerializer());
			ExternalizedWithReadResolve object = new ExternalizedWithReadResolve();
			object.number = 7;
			check(roundTrip(kryo, object).number == 7);
		});

		test("JavaSerializer", () -> {
			Kryo kryo = kryo();
			kryo.register(JavaSerialized.class, new JavaSerializer());
			JavaSerialized object = new JavaSerialized();
			object.text = "java";
			check(roundTrip(kryo, object).text.equals("java"));
		});

		test("ByteBuffer", () -> {
			ByteBuffer buffer = ByteBuffer.allocate(16);
			buffer.put(new byte[] {1, 2, 3, 4, 5});
			buffer.flip();
			buffer.position(2);
			for (ByteBuffer read : new ByteBuffer[] {roundTrip(kryo(), buffer), kryo().copy(buffer)})
				check(read.position() == 2 && read.limit() == 5 && read.get(4) == 5);
		});

		test("Copy of wrappers that contain themselves", () -> {
			Kryo kryo = kryo();
			kryo.setReferences(true);
			ArrayList<Object> list = new ArrayList<>();
			List<Object> wrapper = Collections.unmodifiableList(list);
			list.add("a");
			list.add(wrapper);
			List<Object> copy = kryo.copy(wrapper);
			check(copy.size() == 2 && copy.get(0).equals("a") && copy.get(1) == copy);
			ArrayList<Object> holder = new ArrayList<>();
			Set<Object> set = Collections.newSetFromMap(new HashMap<>());
			holder.add(set);
			set.add(holder);
			Set<Object> setCopy = kryo.copy(set);
			check(((List)setCopy.iterator().next()).get(0) == setCopy);
		});

		test("ConcurrentHashMap key set", () -> {
			ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
			map.put("a", 1);
			Set<String> keySet = ((Map<String, Integer>)map).keySet(); // ConcurrentHashMap#keySet() returns a Set on Android.
			check(roundTrip(kryo(), keySet).equals(keySet));
			check(kryo().copy(keySet).equals(keySet));
		});

		// The field values of the test data of SerializationCompatTest, written on a JVM, and written here for the JVM.
		File androidDirectory = new File(args[1]);
		androidDirectory.mkdirs();
		test("SerializationCompatTest test data from a JVM", () -> {
			List<String> failures = AndroidSerializationCompat.readAndWrite(new File(args[0]), androidDirectory,
				Build.VERSION.SDK_INT);
			for (String failure : failures)
				System.out.println("  " + failure);
			check(failures.isEmpty());
		});

		if (failed) System.exit(1);
		System.out.println("All tests passed.");
	}

	static Kryo kryo () {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		return kryo;
	}

	static <T> T roundTrip (Kryo kryo, T object) {
		Output output = new Output(1024, -1);
		kryo.writeClassAndObject(output, object);
		return (T)kryo.readClassAndObject(new Input(output.toBytes()));
	}

	static void check (boolean condition) {
		if (!condition) throw new AssertionError("Check failed.");
	}

	interface Test {
		void run () throws Exception;
	}

	static void test (String name, Test test) {
		try {
			test.run();
			System.out.println("Passed: " + name);
		} catch (Throwable ex) {
			failed = true;
			System.out.println("FAILED: " + name);
			ex.printStackTrace(System.out);
		}
	}

	public static class Pojo {
		int number;
		long large;
		String text;
		ArrayList<String> list;

		static Pojo create () {
			Pojo pojo = new Pojo();
			pojo.number = 123456789;
			pojo.large = 1234567890123L;
			pojo.text = "text";
			pojo.list = new ArrayList<>(Arrays.asList("a", null, "c"));
			return pojo;
		}

		public boolean equals (Object object) {
			Pojo other = (Pojo)object;
			return number == other.number && large == other.large && Objects.equals(text, other.text)
				&& Objects.equals(list, other.list);
		}
	}

	public static class Final {
		final int number;
		final String text;

		private Final () {
			this(0, null);
		}

		Final (int number, String text) {
			this.number = number;
			this.text = text;
		}
	}

	public static class Tagged {
		@Tag(1) int number;
		@Tag(2) String text;
	}

	public static class Externalized implements Externalizable {
		int number;

		public void writeExternal (ObjectOutput out) throws IOException {
			out.writeInt(number);
		}

		public void readExternal (ObjectInput in) throws IOException {
			number = in.readInt();
		}
	}

	public static class ExternalizedWithReadResolve extends Externalized {
		private Object readResolve () {
			return this;
		}
	}

	public static class JavaSerialized implements Serializable {
		String text;
	}
}
