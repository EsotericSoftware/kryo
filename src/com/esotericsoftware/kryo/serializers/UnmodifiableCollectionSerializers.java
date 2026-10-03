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
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.unsafe.UnsafeUtil;
import com.esotericsoftware.minlog.Log;

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

/** Serializer for unmodifiable Collections and Maps created via Collections. */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class UnmodifiableCollectionSerializers {

	private static class Offset {
		private static final long SOURCE_COLLECTION_FIELD_OFFSET;
		private static final long SOURCE_MAP_FIELD_OFFSET;

		static {
			String clsName = "java.util.Collections$UnmodifiableCollection";
			try {
				SOURCE_COLLECTION_FIELD_OFFSET = UnsafeUtil.objectFieldOffset(Class.forName(clsName).getDeclaredField("c"));
			} catch (Exception e) {
				Log.warn("Could not access source collection field in " + clsName);
				throw new KryoException(e);
			}
			clsName = "java.util.Collections$UnmodifiableMap";
			try {
				SOURCE_MAP_FIELD_OFFSET = UnsafeUtil.objectFieldOffset(Class.forName(clsName).getDeclaredField("m"));
			} catch (Exception e) {
				Log.warn("Could not access source map field in " + clsName);
				throw new KryoException(e);
			}
		}
	}

	static final class UnmodifiableCollectionSerializer extends CollectionSerializer<Collection> {
		private final Function factory;
		private final long offset;

		public UnmodifiableCollectionSerializer (Function factory, long offset) {
			setAcceptsNull(false);
			this.factory = factory;
			this.offset = offset;
		}

		@Override
		public void write (Kryo kryo, Output output, Collection collection) {
			final Object fieldValue = UnsafeUtil.getObject(collection, offset);
			kryo.writeClassAndObject(output, fieldValue);
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

	static final class UnmodifiableMapSerializer extends MapSerializer<Map> {
		private final Function factory;
		private final long offset;

		public UnmodifiableMapSerializer (Function factory, long offset) {
			setAcceptsNull(false);
			this.factory = factory;
			this.offset = offset;
		}

		@Override
		public void write (Kryo kryo, Output output, Map map) {
			Object fieldValue = UnsafeUtil.getObject(map, offset);
			kryo.writeClassAndObject(output, fieldValue);
		}

		@Override
		public Map read (Kryo kryo, Input input, Class<? extends Map> type) {
			final Object sourceMap = kryo.readClassAndObject(input);
			return (Map)factory.apply(sourceMap);
		}

		@Override
		public Map copy (Kryo kryo, Map original) {
			final Object map = UnsafeUtil.getObject(original, offset);
			return (Map)factory.apply(kryo.copy(map));
		}
	}

	private static Serializer<?> createSerializer (Map.Entry<Class<?>, Function> factory) {
		if (Collection.class.isAssignableFrom(factory.getKey())) {
			return new UnmodifiableCollectionSerializer(factory.getValue(), Offset.SOURCE_COLLECTION_FIELD_OFFSET);
		} else {
			return new UnmodifiableMapSerializer(factory.getValue(), Offset.SOURCE_MAP_FIELD_OFFSET);
		}
	}

	@SuppressWarnings("RedundantUnmodifiable")
	private static void putFactories (Map<Class<?>, Function> factories) {
		factories.put(
			Collections.unmodifiableCollection(Collections.singletonList("")).getClass(),
			o -> Collections.unmodifiableCollection((Collection)o));
		factories.put(
			Collections.unmodifiableList(new ArrayList<Void>()).getClass(),
			o1 -> Collections.unmodifiableList((List<?>)o1));
		factories.put(
			Collections.unmodifiableList(new LinkedList<Void>()).getClass(),
			o2 -> Collections.unmodifiableList((List<?>)o2));
		factories.put(
			Collections.unmodifiableSet(new HashSet<Void>()).getClass(),
			o3 -> Collections.unmodifiableSet((Set<?>)o3));
		factories.put(
			Collections.unmodifiableSortedSet(new TreeSet<>()).getClass(),
			o4 -> Collections.unmodifiableSortedSet((SortedSet<?>)o4));
		factories.put(
			Collections.unmodifiableMap(new HashMap<>()).getClass(),
			o5 -> Collections.unmodifiableMap((Map)o5));
		factories.put(
			Collections.unmodifiableSortedMap(new TreeMap<>()).getClass(),
			o6 -> Collections.unmodifiableSortedMap((SortedMap)o6));
	}

	/** Used by the deprecated {@link #registerSerializers(Kryo)}. The iteration order of this HashMap determines the registration
	 * IDs, so it must not change. */
	private static Map<Class<?>, Function> legacyFactories () {
		final Map<Class<?>, Function> factories = new HashMap<>();
		putFactories(factories);
		return factories;
	}

	/** The factories in a fixed order, which determines the IDs of {@link #registerSerializersOrdered(Kryo)}. Only classes that
	 * exist on all supported Java versions, so that the IDs don't change between Java versions. New ones must be added at the
	 * end. */
	static Map<Class<?>, Function> orderedFactories () {
		final Map<Class<?>, Function> factories = new LinkedHashMap<>();
		putFactories(factories);
		factories.put(
			Collections.unmodifiableNavigableSet(new TreeSet<>()).getClass(),
			o -> Collections.unmodifiableNavigableSet((NavigableSet<?>)o));
		factories.put(
			Collections.unmodifiableNavigableMap(new TreeMap<>()).getClass(),
			o -> Collections.unmodifiableNavigableMap((NavigableMap)o));
		return factories;
	}

	static Map<Class<?>, Function> defaultFactories () {
		final Map<Class<?>, Function> factories = orderedFactories();
		putSequencedFactory(factories, "unmodifiableSequencedCollection", "java.util.SequencedCollection", new ArrayList<>());
		putSequencedFactory(factories, "unmodifiableSequencedSet", "java.util.SequencedSet", new LinkedHashSet<>());
		putSequencedFactory(factories, "unmodifiableSequencedMap", "java.util.SequencedMap", new LinkedHashMap<>());
		return factories;
	}

	/** Adds a factory for a Java 21+ sequenced collection method, if available. */
	private static void putSequencedFactory (Map<Class<?>, Function> factories, String methodName, String parameterType,
		Object sample) {
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

	/** Registers serializers for unmodifiable Collections and Maps created via {@link Collections} in a fixed order, so the
	 * registration IDs are the same on all Java versions: unmodifiableCollection, unmodifiableList of a
	 * {@link java.util.RandomAccess} list, unmodifiableList of another list, unmodifiableSet, unmodifiableSortedSet,
	 * unmodifiableMap, unmodifiableSortedMap, unmodifiableNavigableSet and unmodifiableNavigableMap. The Java 21+ sequenced
	 * wrappers, eg unmodifiableSequencedCollection, are not registered, so that the same classes are registered on all Java
	 * versions. After {@link #addDefaultSerializers(Kryo)}, they can be registered with {@link Kryo#register(Class)}. */
	public static void registerSerializersOrdered (Kryo kryo) {
		for (Map.Entry<Class<?>, Function> factory : orderedFactories().entrySet())
			kryo.register(factory.getKey(), createSerializer(factory));
	}

	/** Registers serializers for unmodifiable Collections created via {@link Collections}, including {@link Map}s.
	 * <p>
	 * The registration IDs of these classes depend on the JVM, eg the Java version, so data written on one JVM may be read as a
	 * different collection type on another.
	 * @deprecated Use {@link #registerSerializersOrdered(Kryo)}, which registers the classes in a fixed order.
	 *
	 * @see Collections#unmodifiableCollection(Collection)
	 * @see Collections#unmodifiableList(List)
	 * @see Collections#unmodifiableSet(Set)
	 * @see Collections#unmodifiableSortedSet(SortedSet)
	 * @see Collections#unmodifiableMap(Map)
	 * @see Collections#unmodifiableSortedMap(SortedMap) */
	@Deprecated
	public static void registerSerializers (Kryo kryo) {
		try {
			for (Map.Entry<Class<?>, Function> factory : legacyFactories().entrySet()) {
				kryo.register(factory.getKey(), createSerializer(factory));
			}
		} catch (Throwable t) {
			Log.warn("Unable to register serializers for unmodifiable collections.", t);
		}
	}

	/** Adds default serializers for unmodifiable Collections created via {@link Collections}, including {@link Map}s.
	 *
	 * @see Collections#unmodifiableCollection(Collection)
	 * @see Collections#unmodifiableList(List)
	 * @see Collections#unmodifiableSet(Set)
	 * @see Collections#unmodifiableSortedSet(SortedSet)
	 * @see Collections#unmodifiableMap(Map)
	 * @see Collections#unmodifiableSortedMap(SortedMap)
	 * @see Collections#unmodifiableNavigableSet(NavigableSet)
	 * @see Collections#unmodifiableNavigableMap(NavigableMap) */
	public static void addDefaultSerializers (Kryo kryo) {
		try {
			for (Map.Entry<Class<?>, Function> factory : defaultFactories().entrySet()) {
				kryo.addDefaultSerializer(factory.getKey(), createSerializer(factory));
			}
		} catch (Throwable t) {
			Log.warn("Unable to add default serializers for unmodifiable collections.", t);
		}
	}
}
