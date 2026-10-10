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

package com.esotericsoftware.kryo.nativeimage;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Serializes and copies objects in a GraalVM native image. The reachability metadata in META-INF/native-image only covers the
 * classes of this test, so Kryo itself must work without metadata. Exits with 1 if anything fails. */
public class NativeImageTest {
	public static class Pojo {
		private int number;
		private String text;
		private List<String> list = new ArrayList<>();
		private Map<String, Integer> map = new HashMap<>();
		private Date date;

		public String toString () {
			return number + " " + text + " " + list + " " + map + " " + (date == null ? null : date.getTime());
		}
	}

	public static class WithFinal {
		private final long id;

		public WithFinal () {
			id = 0;
		}

		public WithFinal (long id) {
			this.id = id;
		}

		public String toString () {
			return "id=" + id;
		}
	}

	public record Point(int x, String name) {
	}

	public enum Color {
		RED, GREEN
	}

	static boolean failed;

	public static void main (String[] args) {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);

		Pojo pojo = new Pojo();
		pojo.number = 42;
		pojo.text = "hello";
		pojo.list.add("a");
		pojo.map.put("b", 1);
		pojo.date = new Date(1000);
		test(kryo, "String", () -> "text");
		test(kryo, "ArrayList", () -> new ArrayList<>(List.of(1, 2)));
		test(kryo, "List.of", () -> List.of(1, 2, 3));
		test(kryo, "Optional", () -> Optional.of("o"));
		test(kryo, "LocalDate", () -> LocalDate.of(2026, 10, 3));
		test(kryo, "java.sql.Date", () -> new java.sql.Date(1000));
		test(kryo, "java.sql.Time", () -> new java.sql.Time(1000));
		test(kryo, "UUID", () -> new UUID(1, 2));
		test(kryo, "enum", () -> Color.GREEN);
		test(kryo, "POJO", () -> pojo);
		test(kryo, "final field", () -> new WithFinal(7));
		test(kryo, "record", () -> new Point(1, "p"));
		if (failed) System.exit(1);
	}

	static void test (Kryo kryo, String name, Supplier<Object> supplier) {
		try {
			Object object = supplier.get();
			Output output = new Output(1024, -1);
			kryo.writeClassAndObject(output, object);
			Object read = kryo.readClassAndObject(new Input(output.toBytes()));
			Object copy = kryo.copy(object);
			if (!object.toString().equals(read.toString()) || !object.toString().equals(copy.toString()))
				throw new AssertionError("Different: " + object + ", read " + read + ", copy " + copy);
			System.out.println("OK   " + name + ": " + read);
		} catch (Throwable ex) {
			failed = true;
			System.out.println("FAIL " + name + ":");
			ex.printStackTrace(System.out);
		}
	}
}
