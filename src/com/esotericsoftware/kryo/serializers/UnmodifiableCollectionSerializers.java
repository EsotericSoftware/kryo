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
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

/** Serializers for unmodifiable Collections and Maps created via {@link Collections}. */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class UnmodifiableCollectionSerializers {
	private static final WrappedCollectionGetter collectionGetter = new WrappedCollectionGetter(
		"java.util.Collections$UnmodifiableCollection", "c");
	private static final WrappedCollectionGetter mapGetter = new WrappedCollectionGetter("java.util.Collections$UnmodifiableMap",
		"m");

	private static CollectionWrapperSerializer createSerializer (Map.Entry<Class<?>, Function<Object, Object>> factory) {
		WrappedCollectionGetter getter = Collection.class.isAssignableFrom(factory.getKey()) ? collectionGetter : mapGetter;
		return new CollectionWrapperSerializer(factory.getValue(), getter, false);
	}

	/** The factories in a fixed order, which determines the IDs of {@link #register(Kryo)}. Only classes that exist on all
	 * supported Java versions, so that the IDs don't change between Java versions. New ones must be added at the end. */
	@SuppressWarnings("RedundantUnmodifiable")
	static Map<Class<?>, Function<Object, Object>> orderedFactories () {
		Map<Class<?>, Function<Object, Object>> factories = new LinkedHashMap<>();
		factories.put(Collections.unmodifiableCollection(Collections.singletonList("")).getClass(),
			o -> Collections.unmodifiableCollection((Collection)o));
		factories.put(Collections.unmodifiableList(new ArrayList<>()).getClass(), o -> Collections.unmodifiableList((List)o));
		factories.put(Collections.unmodifiableList(new LinkedList<>()).getClass(), o -> Collections.unmodifiableList((List)o));
		factories.put(Collections.unmodifiableSet(new HashSet<>()).getClass(), o -> Collections.unmodifiableSet((Set)o));
		factories.put(Collections.unmodifiableSortedSet(new TreeSet<>()).getClass(),
			o -> Collections.unmodifiableSortedSet((SortedSet)o));
		factories.put(Collections.unmodifiableMap(new HashMap<>()).getClass(), o -> Collections.unmodifiableMap((Map)o));
		factories.put(Collections.unmodifiableSortedMap(new TreeMap<>()).getClass(),
			o -> Collections.unmodifiableSortedMap((SortedMap)o));
		factories.put(Collections.unmodifiableNavigableSet(new TreeSet<>()).getClass(),
			o -> Collections.unmodifiableNavigableSet((NavigableSet)o));
		factories.put(Collections.unmodifiableNavigableMap(new TreeMap<>()).getClass(),
			o -> Collections.unmodifiableNavigableMap((NavigableMap)o));
		return factories;
	}

	/** Computed once, because the Kryo constructor adds the default serializers. */
	private static final class DefaultFactories {
		static final Map<Class<?>, Function<Object, Object>> factories = defaultFactories();
	}

	static Map<Class<?>, Function<Object, Object>> defaultFactories () {
		Map<Class<?>, Function<Object, Object>> factories = orderedFactories();
		putSequencedFactory(factories, "unmodifiableSequencedCollection", "java.util.SequencedCollection", new ArrayList<>());
		putSequencedFactory(factories, "unmodifiableSequencedSet", "java.util.SequencedSet", new LinkedHashSet<>());
		putSequencedFactory(factories, "unmodifiableSequencedMap", "java.util.SequencedMap", new LinkedHashMap<>());
		return factories;
	}

	/** Adds a factory for a Java 21+ sequenced collection method, if available. In a GraalVM native image, the method is only
	 * found with reflection metadata for it. */
	private static void putSequencedFactory (Map<Class<?>, Function<Object, Object>> factories, String methodName,
		String parameterType, Object sample) {
		try {
			Method method = Collections.class.getMethod(methodName, Class.forName(parameterType));
			factories.put(method.invoke(null, sample).getClass(), o -> {
				try {
					return method.invoke(null, o);
				} catch (Exception ex) {
					throw new KryoException("Error creating " + methodName + ".", ex);
				}
			});
		} catch (Exception ignored) { // Before Java 21.
		}
	}

	/** Registers serializers for unmodifiable Collections and Maps created via {@link Collections} in a fixed order, so that the
	 * registration IDs are the same on all Java versions: unmodifiableCollection, unmodifiableList of a
	 * {@link java.util.RandomAccess} list, unmodifiableList of another list, unmodifiableSet, unmodifiableSortedSet,
	 * unmodifiableMap, unmodifiableSortedMap, unmodifiableNavigableSet and unmodifiableNavigableMap. The Java 21+ sequenced
	 * wrappers, eg unmodifiableSequencedCollection, are not registered, because they don't exist on older Java versions. They have
	 * default serializers and can be registered with {@link Kryo#register(Class)}. */
	public static void register (Kryo kryo) {
		for (Map.Entry<Class<?>, Function<Object, Object>> factory : orderedFactories().entrySet())
			kryo.register(factory.getKey(), createSerializer(factory));
	}

	/** Adds default serializers for unmodifiable Collections and Maps created via {@link Collections}, including the navigable and
	 * the Java 21+ sequenced wrappers. The Kryo constructor calls this, except on Android. */
	public static void addDefaultSerializers (Kryo kryo) {
		try {
			for (Map.Entry<Class<?>, Function<Object, Object>> factory : DefaultFactories.factories.entrySet())
				kryo.addDefaultSerializer(factory.getKey(), createSerializer(factory));
		} catch (Throwable t) {
			Log.warn("Unable to add default serializers for unmodifiable collections.", t);
		}
	}
}
