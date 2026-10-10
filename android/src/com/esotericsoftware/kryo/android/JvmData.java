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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.ImmutableCollectionsSerializers;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** Writes data on a JVM, which AndroidTest reads on Android, run by android/test.sh. */
public class JvmData {
	public static void main (String[] args) throws Exception {
		try (Output output = new Output(new FileOutputStream(args[0]))) {
			// With registered classes, the immutable collections can be read on all Android versions.
			Kryo kryo = new Kryo();
			kryo.setRegistrationRequired(false);
			ImmutableCollectionsSerializers.registerSerializers(kryo);
			kryo.writeClassAndObject(output, immutableCollections());
			kryo.writeClassAndObject(output, Set.of("x"));

			// By class name. Only Stream#toList() creates an immutable list with null elements, which Android has since API level 34.
			kryo = new Kryo();
			kryo.setRegistrationRequired(false);
			kryo.writeClassAndObject(output, immutableCollections());
		}
	}

	/** The JDK's immutable collections, which Android has since API level 30, except Set12 before API level 34. */
	static ArrayList<Object> immutableCollections () {
		ArrayList<Object> collections = new ArrayList<>();
		collections.add(List.of("a"));
		collections.add(List.of("a", "b", "c"));
		collections.add(List.of("a", "b", "c").subList(1, 3));
		collections.add(Stream.of("a", null, "c").toList());
		collections.add(Set.of("x", "y", "z"));
		collections.add(Map.of("k", 1));
		collections.add(Map.of("k", 1, "l", 2));
		return collections;
	}
}
