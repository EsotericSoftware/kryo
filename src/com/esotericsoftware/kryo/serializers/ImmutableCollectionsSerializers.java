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

package com.esotericsoftware.kryo.serializers;

import static com.esotericsoftware.kryo.util.Util.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.util.IgnoreAndroid;
import com.esotericsoftware.kryo.util.Null;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Serializers for java.util.ImmutableCollections, Are added as default serializers for Java 9 or later. */
public final class ImmutableCollectionsSerializers {
	public static void addDefaultSerializers (Kryo kryo) {
		JdkImmutableListSerializer.addDefaultSerializers(kryo);
		JdkImmutableMapSerializer.addDefaultSerializers(kryo);
		JdkImmutableSetSerializer.addDefaultSerializers(kryo);
	}

	/** Creates new serializers for all types of java.util.ImmutableCollections and registers them. The registration IDs are the
	 * same on all platforms, also on Android, which doesn't have all of these classes: data of a missing class is read as the
	 * immutable collections of the platform.
	 *
	 * @param kryo the {@link Kryo} instance to register the serializers on. */
	public static void registerSerializers (Kryo kryo) {
		JdkImmutableListSerializer.registerSerializers(kryo);
		JdkImmutableMapSerializer.registerSerializers(kryo);
		JdkImmutableSetSerializer.registerSerializers(kryo);
	}

	/** Returns the class java.util.ImmutableCollections$name, or null if it doesn't exist. The class of the instance is used if it
	 * has that name, so a GraalVM native image needs no reflection metadata for it. On Android, D8 replaces {@code List.of} with
	 * an unmodifiable list below API level 30, and Android has these classes since API level 30, but not all of them: before API
	 * level 34 it has Set0, Set1 and Set2 instead of Set12, and Map0.
	 * @param instance May be null for the classes only Android has. */
	static private @Null Class immutableCollectionsClass (String name, @Null Object instance) {
		name = "java.util.ImmutableCollections$" + name;
		return instance != null ? classForName(name, instance) : classForName(name);
	}

	static private void addDefaultSerializer (Kryo kryo, Serializer serializer, Class... types) {
		for (Class type : types)
			if (type != null) kryo.addDefaultSerializer(type, serializer);
	}

	/** Registers the class, or the placeholder if the class doesn't exist, so the registration IDs are the same on all platforms.
	 * Data of a missing class is read as the immutable collections of the platform. */
	static private void register (Kryo kryo, Serializer serializer, @Null Class type, Class placeholder) {
		kryo.register(type != null ? type : placeholder, serializer);
	}

	static private final class MissingListN {
	}

	static private final class MissingList12 {
	}

	static private final class MissingSubList {
	}

	static private final class MissingMapN {
	}

	static private final class MissingMap1 {
	}

	static private final class MissingSetN {
	}

	static private final class MissingSet12 {
	}

	/** Serializer for the immutable lists created by {@code List.of} and {@code Stream.toList}, which can contain null elements.
	 * All list classes share one serializer. */
	public static final class JdkImmutableListSerializer extends CollectionSerializer<List<Object>> {
		private JdkImmutableListSerializer () {
		}

		@Override
		protected List<Object> create (Kryo kryo, Input input, Class<? extends List<Object>> type, int size) {
			return new ArrayList<>(size);
		}

		@Override
		protected List<Object> createCopy (Kryo kryo, List<Object> original) {
			return new ArrayList<>(original.size());
		}

		@Override
		public List<Object> read (Kryo kryo, Input input, Class<? extends List<Object>> type) {
			List<Object> list = super.read(kryo, input, type);
			if (list == null) {
				return null;
			}
			return immutableList(list);
		}

		@Override
		public List<Object> copy (Kryo kryo, List<Object> original) {
			// Not super.copy, which references the mutable list. Kryo references the immutable list, which can't contain itself.
			ArrayList<Object> copy = new ArrayList<>(original.size());
			for (Object element : original)
				copy.add(kryo.copy(element));
			return immutableList(copy);
		}

		private static List<Object> immutableList (List<Object> list) {
			// Fast paths without an array for small lists.
			int size = list.size();
			if (size == 0) return List.of();
			Object first = list.get(0);
			if (size == 1 && first != null) return List.of(first);
			if (size == 2 && first != null && list.get(1) != null) return List.of(first, list.get(1));
			if (!list.contains(null)) return List.of(list.toArray());
			return streamToList ? toList(list) : Collections.unmodifiableList(list);
		}

		/** Stream#toList() allows null elements. Android has it only since API level 34, like records. */
		static private final boolean streamToList = !isAndroid || isClassAvailable("java.lang.Record");

		@IgnoreAndroid
		static private List<Object> toList (List<Object> list) {
			return list.stream().toList();
		}

		static private final @Null Class listN = immutableCollectionsClass("ListN", List.of()),
			list12 = immutableCollectionsClass("List12", List.of(1)),
			subList = immutableCollectionsClass("SubList", List.of(1, 2, 3, 4).subList(0, 2));

		static void addDefaultSerializers (Kryo kryo) {
			addDefaultSerializer(kryo, new JdkImmutableListSerializer(), listN, list12, subList);
		}

		static void registerSerializers (Kryo kryo) {
			JdkImmutableListSerializer serializer = new JdkImmutableListSerializer();
			register(kryo, serializer, listN, MissingListN.class);
			register(kryo, serializer, list12, MissingList12.class);
			register(kryo, serializer, subList, MissingSubList.class);
		}
	}

	public static final class JdkImmutableMapSerializer extends MapSerializer<Map<Object, Object>> {

		private JdkImmutableMapSerializer () {
			setKeysCanBeNull(false);
			setValuesCanBeNull(false);
		}

		@Override
		protected Map<Object, Object> create (Kryo kryo, Input input, Class<? extends Map<Object, Object>> type, int size) {
			return new HashMap<>();
		}

		@Override
		protected Map<Object, Object> createCopy (Kryo kryo, Map<Object, Object> original) {
			return new HashMap<>();
		}

		@Override
		public Map<Object, Object> read (Kryo kryo, Input input, Class<? extends Map<Object, Object>> type) {
			Map<Object, Object> map = super.read(kryo, input, type);
			if (map == null) {
				return null;
			}
			return Map.copyOf(map);
		}

		@Override
		public Map<Object, Object> copy (Kryo kryo, Map<Object, Object> original) {
			// Not super.copy, which references the mutable map. Kryo references the immutable map, which can't contain itself.
			HashMap<Object, Object> copy = new HashMap<>();
			for (Map.Entry<Object, Object> entry : original.entrySet())
				copy.put(kryo.copy(entry.getKey()), kryo.copy(entry.getValue()));
			return Map.copyOf(copy);
		}

		static private final @Null Class mapN = immutableCollectionsClass("MapN", Map.of()),
			map1 = immutableCollectionsClass("Map1", Map.of(1, 2));

		static void addDefaultSerializers (Kryo kryo) {
			addDefaultSerializer(kryo, new JdkImmutableMapSerializer(), mapN, map1, immutableCollectionsClass("Map0", null));
		}

		static void registerSerializers (Kryo kryo) {
			JdkImmutableMapSerializer serializer = new JdkImmutableMapSerializer();
			register(kryo, serializer, mapN, MissingMapN.class);
			register(kryo, serializer, map1, MissingMap1.class);
		}
	}

	public static final class JdkImmutableSetSerializer extends CollectionSerializer<Set<Object>> {

		private JdkImmutableSetSerializer () {
			setElementsCanBeNull(false);
		}

		@Override
		protected Set<Object> create (Kryo kryo, Input input, Class<? extends Set<Object>> type, int size) {
			return new HashSet<>();
		}

		@Override
		protected Set<Object> createCopy (Kryo kryo, Set<Object> original) {
			return new HashSet<>();
		}

		@Override
		public Set<Object> read (Kryo kryo, Input input, Class<? extends Set<Object>> type) {
			Set<Object> set = super.read(kryo, input, type);
			if (set == null) {
				return null;
			}
			return Set.of(set.toArray());
		}

		@Override
		public Set<Object> copy (Kryo kryo, Set<Object> original) {
			// Not super.copy, which references the mutable set. Kryo references the immutable set, which can't contain itself.
			HashSet<Object> copy = new HashSet<>();
			for (Object element : original)
				copy.add(kryo.copy(element));
			return Set.copyOf(copy);
		}

		static private final @Null Class setN = immutableCollectionsClass("SetN", Set.of()),
			set12 = immutableCollectionsClass("Set12", Set.of(1));

		static void addDefaultSerializers (Kryo kryo) {
			addDefaultSerializer(kryo, new JdkImmutableSetSerializer(), setN, set12, immutableCollectionsClass("Set0", null),
				immutableCollectionsClass("Set1", null), immutableCollectionsClass("Set2", null));
		}

		static void registerSerializers (Kryo kryo) {
			JdkImmutableSetSerializer serializer = new JdkImmutableSetSerializer();
			register(kryo, serializer, setN, MissingSetN.class);
			register(kryo, serializer, set12, MissingSet12.class);
		}
	}

}
