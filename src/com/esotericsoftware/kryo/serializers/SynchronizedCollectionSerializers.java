/* Copyright (c) 2008-2023, Nathan Sweet
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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.unsafe.UnsafeUtil;
import com.esotericsoftware.kryo.util.Tuple2;
import com.esotericsoftware.minlog.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

/** Serializer for synchronized Collections and Maps created via Collections. */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SynchronizedCollectionSerializers {

	private static class Offset {
		// Graalvm unsafe offset substitution support
		private static final long SOURCE_COLLECTION_FIELD_OFFSET;
		private static final long SOURCE_MAP_FIELD_OFFSET;

		static {
			String clsName = "java.util.Collections$SynchronizedCollection";
			try {
				SOURCE_COLLECTION_FIELD_OFFSET = UnsafeUtil.objectFieldOffset(Class.forName(clsName).getDeclaredField("c"));
			} catch (Exception e) {
				Log.info("Could not access source collection field in {}", clsName);
				throw new KryoException(e);
			}
			clsName = "java.util.Collections$SynchronizedMap";
			try {
				SOURCE_MAP_FIELD_OFFSET = UnsafeUtil.objectFieldOffset(Class.forName(clsName).getDeclaredField("m"));
			} catch (Exception e) {
				Log.info("Could not access source map field in {}", clsName);
				throw new KryoException(e);
			}
		}
	}

	static final class SynchronizedCollectionSerializer extends CollectionSerializer<Collection> {

		private final Function factory;
		private final long offset;

		public SynchronizedCollectionSerializer (Function factory, long offset) {
			setAcceptsNull(false);
			this.factory = factory;
			this.offset = offset;
		}

		@Override
		public void write (Kryo kryo, Output output, Collection collection) {
			Object unwrapped = UnsafeUtil.getObject(collection, offset);
			synchronized (collection) {
				kryo.writeClassAndObject(output, unwrapped);
			}
		}

		@Override
		public Collection read (Kryo kryo, Input input, Class<? extends Collection> type) {
			final Object sourceCollection = kryo.readClassAndObject(input);
			return (Collection)factory.apply(sourceCollection);
		}

		@Override
		public Collection copy (Kryo kryo, Collection original) {
			final Object collection = UnsafeUtil.getObject(original, offset);
			return (Collection)factory.apply(kryo.copy(collection));
		}
	}

	static final class SynchronizedMapSerializer extends MapSerializer<Map> {
		private final Function factory;
		private final long offset;

		public SynchronizedMapSerializer (Function factory, long offset) {
			setAcceptsNull(false);
			this.factory = factory;
			this.offset = offset;
		}

		@Override
		public void write (Kryo kryo, Output output, Map map) {
			Object unwrapped = UnsafeUtil.getObject(map, offset);
			synchronized (map) {
				kryo.writeClassAndObject(output, unwrapped);
			}
		}

		@Override
		public Map read (Kryo kryo, Input input, Class<? extends Map> type) {
			final Object sourceCollection = kryo.readClassAndObject(input);
			return (Map)factory.apply(sourceCollection);
		}

		@Override
		public Map copy (Kryo kryo, Map original) {
			final Object collection = UnsafeUtil.getObject(original, offset);
			return (Map)factory.apply(kryo.copy(collection));
		}
	}

	private static Serializer<?> createSerializer (Tuple2<Class<?>, Function> factory) {
		if (Collection.class.isAssignableFrom(factory.f0)) {
			return new SynchronizedCollectionSerializer(factory.f1, Offset.SOURCE_COLLECTION_FIELD_OFFSET);
		} else {
			return new SynchronizedMapSerializer(factory.f1, Offset.SOURCE_MAP_FIELD_OFFSET);
		}
	}

	static Tuple2<Class<?>, Function>[] synchronizedFactories () {
		return new Tuple2[] {
			Tuple2.<Class<?>, Function> of(
				Collections.synchronizedCollection(Arrays.asList("")).getClass(),
				o -> Collections.synchronizedCollection((Collection)o)),
			Tuple2.<Class<?>, Function> of(
				Collections.synchronizedList(new ArrayList<Void>()).getClass(),
				o1 -> Collections.synchronizedList((List<?>)o1)),
			Tuple2.<Class<?>, Function> of(
				Collections.synchronizedList(new LinkedList<Void>()).getClass(),
				o2 -> Collections.synchronizedList((List<?>)o2)),
			Tuple2.<Class<?>, Function> of(
				Collections.synchronizedSet(new HashSet<Void>()).getClass(),
				o3 -> Collections.synchronizedSet((Set<?>)o3)),
			Tuple2.<Class<?>, Function> of(
				Collections.synchronizedSortedSet(new TreeSet<>()).getClass(),
				o4 -> Collections.synchronizedSortedSet((TreeSet<?>)o4)),
			Tuple2.<Class<?>, Function> of(
				Collections.synchronizedMap(new HashMap<Void, Void>()).getClass(),
				o5 -> Collections.synchronizedMap((Map)o5)),
			Tuple2.<Class<?>, Function> of(
				Collections.synchronizedSortedMap(new TreeMap<>()).getClass(),
				o6 -> Collections.synchronizedSortedMap((SortedMap)o6))
		};
	}

	/** Registering serializers for synchronized Collections and Maps created via {@link Collections}.
	 *
	 * @see Collections#synchronizedCollection(Collection)
	 * @see Collections#synchronizedList(List)
	 * @see Collections#synchronizedSet(Set)
	 * @see Collections#synchronizedSortedSet(SortedSet)
	 * @see Collections#synchronizedMap(Map)
	 * @see Collections#synchronizedSortedMap(SortedMap) **/
	public static void registerSerializers (Kryo kryo) {
		try {
			for (Tuple2<Class<?>, Function> factory : synchronizedFactories()) {
				kryo.register(factory.f0, createSerializer(factory));
			}
		} catch (Throwable ignored) {
			// ignored
		}
	}

	/** Adding default serializers for synchronized Collections and Maps created via {@link Collections}.
	 *
	 * @see Collections#synchronizedCollection(Collection)
	 * @see Collections#synchronizedList(List)
	 * @see Collections#synchronizedSet(Set)
	 * @see Collections#synchronizedSortedSet(SortedSet)
	 * @see Collections#synchronizedMap(Map)
	 * @see Collections#synchronizedSortedMap(SortedMap) **/
	public static void addDefaultSerializers (Kryo kryo) {
		try {
			for (Tuple2<Class<?>, Function> factory : synchronizedFactories()) {
				kryo.addDefaultSerializer(factory.f0, createSerializer(factory));
			}
		} catch (Throwable ignored) {
			// ignored
		}
	}
}
