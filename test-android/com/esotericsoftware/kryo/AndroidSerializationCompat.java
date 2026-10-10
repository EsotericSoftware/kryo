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

package com.esotericsoftware.kryo;

import static com.esotericsoftware.kryo.ReflectionAssert.*;

import com.esotericsoftware.kryo.SerializationCompatTestData.TestData;
import com.esotericsoftware.kryo.SerializationCompatTestData.TestDataJava8;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.ImmutableCollectionsSerializers;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.util.StdInstantiatorStrategy;
import com.esotericsoftware.kryo.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** {@link SerializationCompatTest} between a JVM and Android, for each field of the test data separately, so one type that fails
 * doesn't hide the others: the JVM writes the field values ({@link #main(String[])} with "write"), Android reads and compares
 * them and writes them again ({@link #readAndWrite(File, File, int)}), and the JVM reads and compares those ("read"). Run by
 * android/test.sh. */
@SuppressWarnings({"unchecked", "rawtypes"})
class AndroidSerializationCompat {
	/** Test data, written with the immutable collections registered with
	 * {@link ImmutableCollectionsSerializers#registerSerializers(Kryo)} or with their class names. */
	record Data (Object testData, boolean registered) {
		String fileName () {
			return testData.getClass().getSimpleName() + (registered ? "-registered" : "") + ".ser";
		}
	}

	/** Returns the test data with the types the Android API level has: java.time since API level 26, the immutable collections by
	 * class name since 30, records since 34. Registered immutable collections can be read on all Android versions. */
	static List<Data> testData (int apiLevel) {
		ArrayList<Data> testData = new ArrayList<>();
		testData.add(new Data(new TestData(), false));
		testData.add(new Data(new TestDataJava8(), false));
		testData.add(new Data(new ImmutableCollections(), true));
		if (apiLevel >= 30) {
			testData.add(new Data(new TestDataJava11(), false));
			testData.add(new Data(new ImmutableCollections(), false));
		}
		if (apiLevel >= 34) testData.add(new Data(new TestDataJava17(), false));
		return testData;
	}

	/** The immutable collections that TestDataJava11 doesn't have, which are read as immutable collections. */
	static final class ImmutableCollections {
		List<String> list12 = List.of("a"), listN = List.of("a", "b", "c"), subList = List.of("a", "b", "c").subList(1, 3);
		// Only Stream#toList() creates an immutable list with null elements, which Android has only since API level 34.
		List<String> listWithNulls = Util.isAndroid ? Collections.unmodifiableList(Arrays.asList("a", null, "c"))
			: Stream.of("a", null, "c").toList();
		Set<String> set12 = Set.of("x"), setN = Set.of("x", "y", "z");
		Map<String, Integer> map1 = Map.of("k", 1), mapN = Map.of("k", 1, "l", 2);
	}

	/** Configured like {@link SerializationCompatTest}. */
	static Kryo kryo (boolean registered) {
		Kryo kryo = new Kryo();
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		kryo.setReferences(true);
		kryo.setRegistrationRequired(false);
		kryo.register(EnumSet.class);
		if (registered) ImmutableCollectionsSerializers.registerSerializers(kryo);
		return kryo;
	}

	/** The fields declared by the test data class, sorted by name, not by its super class, which is tested separately. */
	static List<Field> fields (Object testData) {
		ArrayList<Field> fields = new ArrayList<>();
		for (Field field : testData.getClass().getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
			field.setAccessible(true);
			fields.add(field);
		}
		fields.sort(Comparator.comparing(Field::getName)); // The order of getDeclaredFields differs on Android.
		return fields;
	}

	/** Writes the name and the value of each field, each value with its length, so a value that can't be read can be skipped. */
	static void write (File directory, Data data) throws Exception {
		Kryo kryo = kryo(data.registered());
		try (Output output = new Output(new FileOutputStream(new File(directory, data.fileName())))) {
			for (Field field : fields(data.testData())) {
				Output value = new Output(256, -1);
				kryo.writeClassAndObject(value, field.get(data.testData()));
				output.writeString(field.getName());
				output.writeVarInt(value.position(), true);
				output.writeBytes(value.getBuffer(), 0, value.position());
			}
		}
	}

	/** Reads and compares each field value, returns the fields that fail. Collections only need to be of the same kind, eg D8
	 * replaces List.of with an unmodifiable list below Android API level 30. */
	static List<String> read (File directory, Data data, int apiLevel) throws Exception {
		ArrayList<String> failures = new ArrayList<>();
		Kryo kryo = kryo(data.registered());
		try (Input input = new Input(new FileInputStream(new File(directory, data.fileName())))) {
			for (Field field : fields(data.testData())) {
				String name = input.readString();
				byte[] bytes = input.readBytes(input.readVarInt(true));
				check(name.equals(field.getName()), "Expected field " + field.getName() + ": " + name);
				// Android has ImmutableCollections$Set12 only since API level 34, see README.
				if (!data.registered() && (name.equals("singleImmutableSet") || name.equals("set12")) && apiLevel < 34) continue;
				try {
					Object actual = kryo.readClassAndObject(new Input(bytes)), expected = field.get(data.testData());
					// Sets of different classes, eg Set.of and LinkedHashSet, iterate in a different order.
					if (actual instanceof Set && expected instanceof Set && actual.getClass() != expected.getClass())
						check(actual.equals(expected), "Sets not equal: " + actual + ", " + expected);
					else
						assertReflectionEquals(actual, expected, false);
					if (data.testData() instanceof ImmutableCollections) checkImmutable(actual);
				} catch (Throwable ex) {
					failures.add(data.fileName() + " " + name + ": " + ex);
				}
			}
		}
		return failures;
	}

	static void checkImmutable (Object collection) {
		try {
			if (collection instanceof Map)
				((Map)collection).put("new", 0);
			else
				((Collection)collection).add("new");
		} catch (UnsupportedOperationException expected) {
			return;
		}
		throw new IllegalStateException("Mutable: " + collection.getClass().getName());
	}

	/** On Android: reads the files written on the JVM in the JVM directory and writes them again in the Android directory. */
	static List<String> readAndWrite (File jvmDirectory, File androidDirectory, int apiLevel) throws Exception {
		ArrayList<String> failures = new ArrayList<>();
		for (Data data : testData(apiLevel)) {
			failures.addAll(read(jvmDirectory, data, apiLevel));
			write(androidDirectory, data);
		}
		return failures;
	}

	static void check (boolean condition, String message) {
		if (!condition) throw new IllegalStateException(message);
	}

	/** On the JVM, with "write" and a directory, or "read", a directory and the Android API level. Exits with 1 if reading
	 * fails. */
	public static void main (String[] args) throws Exception {
		File directory = new File(args[1]);
		if (args[0].equals("write")) {
			directory.mkdirs();
			for (Data data : testData(Integer.MAX_VALUE))
				write(directory, data);
			return;
		}
		ArrayList<String> failures = new ArrayList<>();
		for (Data data : testData(Integer.parseInt(args[2])))
			failures.addAll(read(directory, data, Integer.MAX_VALUE));
		for (String failure : failures)
			System.out.println("FAILED on the JVM, written on Android: " + failure);
		if (!failures.isEmpty()) System.exit(1);
		System.out.println("The JVM read the test data written on Android.");
	}
}
