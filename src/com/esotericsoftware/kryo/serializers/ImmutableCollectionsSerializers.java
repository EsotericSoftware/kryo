/* Copyright (c) 2008-2018, Nathan Sweet
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

package com.esotericsoftware.kryo.serializers;

import static com.esotericsoftware.kryo.util.Util.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.*;

/** Serializers for {@link java.util.ImmutableCollections}, Are added as default serializers for java >= 9. */
public final class ImmutableCollectionsSerializers {
	static public void addDefaultSerializers (Kryo kryo) {
		if (isClassAvailable("java.util.ImmutableCollections")) {
			JdkImmutableListSerializer.addDefaultSerializers(kryo);
			JdkImmutableMapSerializer.addDefaultSerializers(kryo);
			JdkImmutableSetSerializer.addDefaultSerializers(kryo);
		}
	}

	static public class JdkImmutableListSerializer extends Serializer<List<Object>> {

		private JdkImmutableListSerializer () {
			super(false, true);
		}

		@Override
		public void write (Kryo kryo, Output output, List<Object> object) {
			output.writeInt(object.size(), true);
			for (final Object elm : object) {
				kryo.writeClassAndObject(output, elm);
			}
		}

		@Override
		public List<Object> read (Kryo kryo, Input input, Class<? extends List<Object>> type) {
			final int size = input.readInt(true);
			final Object[] list = new Object[size];
			for (int i = 0; i < size; ++i) {
				list[i] = kryo.readClassAndObject(input);
			}
			return List.of(list);
		}

		@Override
		public List<Object> copy(Kryo kryo, List<Object> original) {
			List<Object> copy = new ArrayList<>(original.size());
			kryo.reference(copy);
			for (Object element : original) {
				copy.add(kryo.copy(element));
			}
			return List.copyOf(copy);
		}

		static void addDefaultSerializers (Kryo kryo) {
			final JdkImmutableListSerializer serializer = new JdkImmutableListSerializer();
			kryo.addDefaultSerializer(List.of().getClass(), serializer);
			kryo.addDefaultSerializer(List.of(1).getClass(), serializer);
			kryo.addDefaultSerializer(List.of(1, 2, 3, 4).getClass(), serializer);
			kryo.addDefaultSerializer(List.of(1, 2, 3, 4).subList(0, 2).getClass(), serializer);
		}
	}

	static public class JdkImmutableMapSerializer extends Serializer<Map<Object, Object>> {

		private JdkImmutableMapSerializer () {
			super(false, true);
		}

		@Override
		public void write (Kryo kryo, Output output, Map<Object, Object> object) {
			kryo.writeObject(output, new HashMap<>(object));
		}

		@Override
		public Map<Object, Object> read (Kryo kryo, Input input, Class<? extends Map<Object, Object>> type) {
			final Map<?, ?> map = kryo.readObject(input, HashMap.class);
			return Map.copyOf(map);
		}

		@Override
		public Map<Object, Object> copy(Kryo kryo, Map<Object, Object> original) {
			final HashMap<Object, Object> copy = new HashMap<>(original.size());
			for (Map.Entry<Object, Object> entry : original.entrySet()) {
				copy.put(kryo.copy(entry.getKey()), kryo.copy(entry.getValue()));
			}
			return Map.copyOf(copy);
		}

		static void addDefaultSerializers (Kryo kryo) {
			final JdkImmutableMapSerializer serializer = new JdkImmutableMapSerializer();
			kryo.addDefaultSerializer(Map.of().getClass(), serializer);
			kryo.addDefaultSerializer(Map.of(1, 2).getClass(), serializer);
			kryo.addDefaultSerializer(Map.of(1, 2, 3, 4).getClass(), serializer);
		}
	}

	static public class JdkImmutableSetSerializer extends Serializer<Set<Object>> {

		private JdkImmutableSetSerializer () {
			super(false, true);
		}

		@Override
		public void write (Kryo kryo, Output output, Set<Object> object) {
			output.writeInt(object.size(), true);
			for (final Object elm : object) {
				kryo.writeClassAndObject(output, elm);
			}
		}

		@Override
		public Set<Object> read(Kryo kryo, Input input, Class<? extends Set<Object>> type) {
			final int size = input.readInt(true);
			final Object[] objects = new Object[size];
			for (int i = 0; i < size; ++i) {
				objects[i] = kryo.readClassAndObject(input);
			}
			return Set.of(objects);
		}

		@Override
		public Set<Object> copy(Kryo kryo, Set<Object> original) {
			Set<Object> copy = new HashSet<>(original.size());
			kryo.reference(copy);
			for (Object element : original) {
				copy.add(kryo.copy(element));
			}
			return Set.copyOf(copy);
		}

		static void addDefaultSerializers (Kryo kryo) {
			final JdkImmutableSetSerializer serializer = new JdkImmutableSetSerializer();
			kryo.addDefaultSerializer(Set.of().getClass(), serializer);
			kryo.addDefaultSerializer(Set.of(1).getClass(), serializer);
			kryo.addDefaultSerializer(Set.of(1, 2, 3, 4).getClass(), serializer);
		}
	}

}
