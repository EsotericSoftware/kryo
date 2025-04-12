/* Copyright (c) 2008-2025, Nathan Sweet
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
import com.esotericsoftware.kryo.io.Input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Serializers for {@link java.util.ImmutableCollections}, Are added as default serializers for java >= 9. */
public final class ImmutableCollectionsSerializers {
	public static void addDefaultSerializers (Kryo kryo) {
		if (isClassAvailable("java.util.ImmutableCollections")) {
			JdkImmutableListSerializer.addDefaultSerializers(kryo);
			JdkImmutableMapSerializer.addDefaultSerializers(kryo);
			JdkImmutableSetSerializer.addDefaultSerializers(kryo);
		}
	}

	/** Creates new serializers for all types of {@link java.util.ImmutableCollections}s and registers them.
	 *
	 * @param kryo the {@link Kryo} instance to register the serializers on. */
	public static void registerSerializers (Kryo kryo) {
		JdkImmutableListSerializer.registerSerializers(kryo);
		JdkImmutableMapSerializer.registerSerializers(kryo);
		JdkImmutableSetSerializer.registerSerializers(kryo);
	}

	public static final class JdkImmutableListSerializer extends CollectionSerializer<List<Object>> {

		private JdkImmutableListSerializer () {
			setElementsCanBeNull(false);
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
			return List.of(list.toArray());
		}

		@Override
		public List<Object> copy (Kryo kryo, List<Object> original) {
			List<Object> copy = super.copy(kryo, original);
			return List.copyOf(copy);
		}

		static void addDefaultSerializers (Kryo kryo) {
			final JdkImmutableListSerializer serializer = new JdkImmutableListSerializer();
			kryo.addDefaultSerializer(List.of().getClass(), serializer);
			kryo.addDefaultSerializer(List.of(1).getClass(), serializer);
			kryo.addDefaultSerializer(List.of(1, 2, 3, 4).getClass(), serializer);
			kryo.addDefaultSerializer(List.of(1, 2, 3, 4).subList(0, 2).getClass(), serializer);
		}

		static void registerSerializers (Kryo kryo) {
			final JdkImmutableListSerializer serializer = new JdkImmutableListSerializer();
			kryo.register(List.of().getClass(), serializer);
			kryo.register(List.of(1).getClass(), serializer);
			kryo.register(List.of(1, 2, 3, 4).getClass(), serializer);
			kryo.register(List.of(1, 2, 3, 4).subList(0, 2).getClass(), serializer);
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
			final Map<Object, Object> copy = super.copy(kryo, original);
			return Map.copyOf(copy);
		}

		static void addDefaultSerializers (Kryo kryo) {
			final JdkImmutableMapSerializer serializer = new JdkImmutableMapSerializer();
			kryo.addDefaultSerializer(Map.of().getClass(), serializer);
			kryo.addDefaultSerializer(Map.of(1, 2).getClass(), serializer);
			kryo.addDefaultSerializer(Map.of(1, 2, 3, 4).getClass(), serializer);
		}

		static void registerSerializers (Kryo kryo) {
			final JdkImmutableMapSerializer serializer = new JdkImmutableMapSerializer();
			kryo.register(Map.of().getClass(), serializer);
			kryo.register(Map.of(1, 2).getClass(), serializer);
			kryo.register(Map.of(1, 2, 3, 4).getClass(), serializer);
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
			final Set<Object> copy = super.copy(kryo, original);
			return Set.copyOf(copy);
		}

		static void addDefaultSerializers (Kryo kryo) {
			final JdkImmutableSetSerializer serializer = new JdkImmutableSetSerializer();
			kryo.addDefaultSerializer(Set.of().getClass(), serializer);
			kryo.addDefaultSerializer(Set.of(1).getClass(), serializer);
			kryo.addDefaultSerializer(Set.of(1, 2, 3, 4).getClass(), serializer);
		}

		static void registerSerializers (Kryo kryo) {
			final JdkImmutableSetSerializer serializer = new JdkImmutableSetSerializer();
			kryo.register(Set.of().getClass(), serializer);
			kryo.register(Set.of(1).getClass(), serializer);
			kryo.register(Set.of(1, 2, 3, 4).getClass(), serializer);
		}
	}

}
