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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

/** Serializers for synchronized Collections and Maps created via {@link Collections}. The wrapper is locked while the wrapped
 * collection is written or copied. */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class SynchronizedCollectionSerializers {
	private static final WrappedCollectionGetter collectionGetter = new WrappedCollectionGetter(
		"java.util.Collections$SynchronizedCollection", "c");
	private static final WrappedCollectionGetter mapGetter = new WrappedCollectionGetter("java.util.Collections$SynchronizedMap",
		"m");

	private static CollectionWrapperSerializer createSerializer (Map.Entry<Class<?>, Function<Object, Object>> factory) {
		WrappedCollectionGetter getter = Collection.class.isAssignableFrom(factory.getKey()) ? collectionGetter : mapGetter;
		return new CollectionWrapperSerializer(factory.getValue(), getter, true);
	}

	/** The factories in a fixed order, which determines the IDs of {@link #register(Kryo)}. New ones must be added at the end. */
	static Map<Class<?>, Function<Object, Object>> orderedFactories () {
		Map<Class<?>, Function<Object, Object>> factories = new LinkedHashMap<>();
		factories.put(Collections.synchronizedCollection(Collections.singletonList("")).getClass(),
			o -> Collections.synchronizedCollection((Collection)o));
		factories.put(Collections.synchronizedList(new ArrayList<>()).getClass(), o -> Collections.synchronizedList((List)o));
		factories.put(Collections.synchronizedList(new LinkedList<>()).getClass(), o -> Collections.synchronizedList((List)o));
		factories.put(Collections.synchronizedSet(new HashSet<>()).getClass(), o -> Collections.synchronizedSet((Set)o));
		factories.put(Collections.synchronizedSortedSet(new TreeSet<>()).getClass(),
			o -> Collections.synchronizedSortedSet((SortedSet)o));
		factories.put(Collections.synchronizedMap(new HashMap<>()).getClass(), o -> Collections.synchronizedMap((Map)o));
		factories.put(Collections.synchronizedSortedMap(new TreeMap<>()).getClass(),
			o -> Collections.synchronizedSortedMap((SortedMap)o));
		factories.put(Collections.synchronizedNavigableSet(new TreeSet<>()).getClass(),
			o -> Collections.synchronizedNavigableSet((NavigableSet)o));
		factories.put(Collections.synchronizedNavigableMap(new TreeMap<>()).getClass(),
			o -> Collections.synchronizedNavigableMap((NavigableMap)o));
		return factories;
	}

	/** Computed once, because the Kryo constructor adds the default serializers. */
	private static final class DefaultFactories {
		static final Map<Class<?>, Function<Object, Object>> factories = orderedFactories();
	}

	/** Registers serializers for synchronized Collections and Maps created via {@link Collections} in a fixed order, so that the
	 * registration IDs are the same on all Java versions: synchronizedCollection, synchronizedList of a
	 * {@link java.util.RandomAccess} list, synchronizedList of another list, synchronizedSet, synchronizedSortedSet,
	 * synchronizedMap, synchronizedSortedMap, synchronizedNavigableSet and synchronizedNavigableMap. */
	public static void register (Kryo kryo) {
		for (Map.Entry<Class<?>, Function<Object, Object>> factory : orderedFactories().entrySet())
			kryo.register(factory.getKey(), createSerializer(factory));
	}

	/** Adds default serializers for synchronized Collections and Maps created via {@link Collections}. The Kryo constructor calls
	 * this. */
	public static void addDefaultSerializers (Kryo kryo) {
		try {
			for (Map.Entry<Class<?>, Function<Object, Object>> factory : DefaultFactories.factories.entrySet())
				kryo.addDefaultSerializer(factory.getKey(), createSerializer(factory));
		} catch (Throwable t) {
			Log.warn("Unable to add default serializers for synchronized collections.", t);
		}
	}
}
