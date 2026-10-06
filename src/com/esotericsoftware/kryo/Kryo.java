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

import static com.esotericsoftware.kryo.util.Util.*;
import static com.esotericsoftware.kryo.util.Log.*;

import com.esotericsoftware.kryo.SerializerFactory.BaseSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.FieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.ReflectionSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.SingletonSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.ClosureSerializer;
import com.esotericsoftware.kryo.serializers.ClosureSerializer.Closure;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.BooleanArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.ByteArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.CharArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.DoubleArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.FloatArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.IntArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.LongArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.ObjectArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.ShortArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.StringArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ArraysAsListSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.AtomicBooleanSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.AtomicIntegerSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.AtomicLongSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.AtomicReferenceSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ArrayBlockingQueueSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.BigDecimalSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.BigIntegerSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.BitSetSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ByteBufferSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CaseInsensitiveOrderSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.BooleanSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ByteSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CalendarSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CharSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CharsetSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ClassSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CollectionsEmptyListSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CollectionsEmptyMapSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CollectionsEmptySetSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CollectionsSingletonListSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CollectionsSingletonMapSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CollectionsSingletonSetSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ConcurrentSkipListMapSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ConcurrentSkipListSetSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CurrencySerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.DateSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.DoubleSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.EnumSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.FileSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.EnumSetSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.FloatSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.InetAddressSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.InetSocketAddressSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.IntSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.KeySetViewSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.KryoSerializableSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.LinkedBlockingDequeSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.LinkedBlockingQueueSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.LocaleSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.LongSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.PatternSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.PriorityBlockingQueueSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.PriorityQueueSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ReverseOrderComparatorSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ReverseOrderSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ShortSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringBufferSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.SqlDateSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.SqlTimeSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringBuilderSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.TimeZoneSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.TimestampSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.TreeMapSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.TreeSetSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.URISerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.URLSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.UUIDSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.VoidSerializer;
import com.esotericsoftware.kryo.serializers.EnumMapSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.ImmutableCollectionsSerializers;
import com.esotericsoftware.kryo.serializers.MapSerializer;
import com.esotericsoftware.kryo.serializers.OptionalSerializers;
import com.esotericsoftware.kryo.serializers.SetFromMapSerializer;
import com.esotericsoftware.kryo.serializers.SynchronizedCollectionSerializers;
import com.esotericsoftware.kryo.serializers.TimeSerializers;
import com.esotericsoftware.kryo.serializers.UnmodifiableCollectionSerializers;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import com.esotericsoftware.kryo.util.DefaultGenerics;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.util.InstantiatorStrategy;
import com.esotericsoftware.kryo.util.ObjectInstantiator;
import com.esotericsoftware.kryo.util.SerializingInstantiatorStrategy;
import com.esotericsoftware.kryo.util.StdInstantiatorStrategy;
import com.esotericsoftware.kryo.util.Generics;
import com.esotericsoftware.kryo.util.Generics.GenericType;
import com.esotericsoftware.kryo.util.Generics.GenericsHierarchy;
import com.esotericsoftware.kryo.util.IdentityMap;
import com.esotericsoftware.kryo.util.IntArray;
import com.esotericsoftware.kryo.util.MapReferenceResolver;
import com.esotericsoftware.kryo.util.NoGenerics;
import com.esotericsoftware.kryo.util.ObjectMap;
import com.esotericsoftware.kryo.util.Util;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Currency;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** Maps classes to serializers so object graphs can be serialized automatically. The README describes how to configure and use
 * Kryo.
 * <p>
 * Kryo is not thread safe. Each thread should have its own Kryo, {@link Input} and {@link Output} instances, eg from a
 * {@link com.esotericsoftware.kryo.util.Pool}.
 * @author Nathan Sweet */
public class Kryo {
	public static final byte NULL = 0;
	public static final byte NOT_NULL = 1;

	private static final int REF = -1;
	private static final int NO_REF = -2;
	private static final int DEFAULT_SERIALIZER_SIZE = 68;

	SerializerFactory defaultSerializer = new FieldSerializerFactory();
	final ArrayList<DefaultSerializerEntry> defaultSerializers = new ArrayList(DEFAULT_SERIALIZER_SIZE);
	private final int lowPriorityDefaultSerializerCount;
	private ObjectMap<String, Class> defaultSerializerTypes;

	private final ClassResolver classResolver;
	private int nextRegisterID;
	private ClassLoader classLoader = getClass().getClassLoader();
	private InstantiatorStrategy strategy = new DefaultInstantiatorStrategy();
	private boolean registrationRequired = true;
	private boolean warnUnregisteredClasses;
	private boolean enumsFinal = true;
	private Predicate<Class> allowedUnregisteredClasses;

	private int depth, maxDepth = Integer.MAX_VALUE;
	private boolean autoReset = true;
	private volatile Thread thread;
	private ObjectMap context, graphContext;

	ReferenceResolver referenceResolver;
	private final IntArray readReferenceIds = new IntArray(0);
	private boolean references, copyReferences = true;
	/** Whether String fields of FieldSerializer use references: -1 until the first String field is created, then 0 or 1. The field
	 * decides when it is created, so the decision can't change afterward. */
	private int stringFieldReferences = -1;
	private Object readObject;

	private int copyDepth;
	private boolean copyShallow;
	private IdentityMap originalToCopy;
	private Object needsCopyReference;
	private Generics generics = new DefaultGenerics(this);

	/** Creates a new Kryo with a {@link DefaultClassResolver} and references disabled. */
	public Kryo () {
		this(new DefaultClassResolver(), null);
	}

	/** Creates a new Kryo with a {@link DefaultClassResolver}.
	 * @param referenceResolver May be null to disable references. */
	public Kryo (ReferenceResolver referenceResolver) {
		this(new DefaultClassResolver(), referenceResolver);
	}

	/** @param referenceResolver May be null to disable references. */
	public Kryo (ClassResolver classResolver, ReferenceResolver referenceResolver) {
		if (classResolver == null) throw new IllegalArgumentException("classResolver cannot be null.");

		this.classResolver = classResolver;
		classResolver.setKryo(this);

		this.referenceResolver = referenceResolver;
		if (referenceResolver != null) {
			referenceResolver.setKryo(this);
			references = true;
		}

		addDefaultSerializer(byte[].class, ByteArraySerializer::new);
		addDefaultSerializer(char[].class, CharArraySerializer::new);
		addDefaultSerializer(short[].class, ShortArraySerializer::new);
		addDefaultSerializer(int[].class, IntArraySerializer::new);
		addDefaultSerializer(long[].class, LongArraySerializer::new);
		addDefaultSerializer(float[].class, FloatArraySerializer::new);
		addDefaultSerializer(double[].class, DoubleArraySerializer::new);
		addDefaultSerializer(boolean[].class, BooleanArraySerializer::new);
		addDefaultSerializer(String[].class, StringArraySerializer::new);
		addDefaultSerializer(Object[].class, new BaseSerializerFactory() {
			public Serializer newSerializer (Kryo kryo, Class type) {
				return new ObjectArraySerializer(kryo, type);
			}
		});
		addDefaultSerializer(BigInteger.class, BigIntegerSerializer::new);
		addDefaultSerializer(BigDecimal.class, BigDecimalSerializer::new);
		addDefaultSerializer(Class.class, ClassSerializer::new);
		addDefaultSerializer(Date.class, DateSerializer::new);
		addDefaultSerializer(Enum.class, new BaseSerializerFactory() {
			public Serializer newSerializer (Kryo kryo, Class type) {
				return new EnumSerializer(type);
			}
		});
		addDefaultSerializer(EnumSet.class, EnumSetSerializer::new);
		addDefaultSerializer(Currency.class, CurrencySerializer::new);
		addDefaultSerializer(StringBuffer.class, StringBufferSerializer::new);
		addDefaultSerializer(StringBuilder.class, StringBuilderSerializer::new);
		addDefaultSerializer(Collections.EMPTY_LIST.getClass(), CollectionsEmptyListSerializer::new);
		addDefaultSerializer(Collections.EMPTY_MAP.getClass(), CollectionsEmptyMapSerializer::new);
		addDefaultSerializer(Collections.EMPTY_SET.getClass(), CollectionsEmptySetSerializer::new);
		addDefaultSerializer(Collections.singletonList(null).getClass(), CollectionsSingletonListSerializer::new);
		addDefaultSerializer(Collections.singletonMap(null, null).getClass(), CollectionsSingletonMapSerializer::new);
		addDefaultSerializer(Collections.singleton(null).getClass(), CollectionsSingletonSetSerializer::new);
		addDefaultSerializer(TreeSet.class, TreeSetSerializer::new);
		addDefaultSerializer(ConcurrentSkipListSet.class, ConcurrentSkipListSetSerializer::new);
		addDefaultSerializer(PriorityBlockingQueue.class, PriorityBlockingQueueSerializer::new);
		addDefaultSerializer(ArrayBlockingQueue.class, ArrayBlockingQueueSerializer::new);
		addDefaultSerializer(LinkedBlockingQueue.class, LinkedBlockingQueueSerializer::new);
		addDefaultSerializer(LinkedBlockingDeque.class, LinkedBlockingDequeSerializer::new);
		addDefaultSerializer(Collections.reverseOrder().getClass(), new ReverseOrderSerializer());
		addDefaultSerializer(Collections.reverseOrder(String.CASE_INSENSITIVE_ORDER).getClass(),
			new ReverseOrderComparatorSerializer());
		addDefaultSerializer(String.CASE_INSENSITIVE_ORDER.getClass(), new CaseInsensitiveOrderSerializer());
		addDefaultSerializer(Collection.class, CollectionSerializer::new);
		addDefaultSerializer(ConcurrentSkipListMap.class, ConcurrentSkipListMapSerializer::new);
		addDefaultSerializer(TreeMap.class, TreeMapSerializer::new);
		addDefaultSerializer(EnumMap.class, exactType(EnumMap.class, new EnumMapSerializer()));
		addDefaultSerializer(Map.class, MapSerializer::new);
		addDefaultSerializer(TimeZone.class, TimeZoneSerializer::new);
		addDefaultSerializer(Calendar.class, CalendarSerializer::new);
		addDefaultSerializer(Locale.class, LocaleSerializer::new);
		addDefaultSerializer(Charset.class, CharsetSerializer::new);
		addDefaultSerializer(URL.class, URLSerializer::new);
		addDefaultSerializer(File.class, exactType(File.class, new FileSerializer()));
		addDefaultSerializer(InetAddress.class, new InetAddressSerializer());
		addDefaultSerializer(InetSocketAddress.class, exactType(InetSocketAddress.class, new InetSocketAddressSerializer()));
		addDefaultSerializer(Arrays.asList().getClass(), ArraysAsListSerializer::new);
		addDefaultSerializer(void.class, new VoidSerializer());
		addDefaultSerializer(PriorityQueue.class, new PriorityQueueSerializer());
		addDefaultSerializer(BitSet.class, new BitSetSerializer());
		addDefaultSerializer(ByteBuffer.class, new ByteBufferSerializer());
		addDefaultSerializer(KryoSerializable.class, KryoSerializableSerializer::new);
		try {
			addDefaultSerializer(java.sql.Date.class, SqlDateSerializer::new);
			addDefaultSerializer(java.sql.Time.class, SqlTimeSerializer::new);
			addDefaultSerializer(Timestamp.class, TimestampSerializer::new);
		} catch (NoClassDefFoundError ignored) { // java.sql is not available in a named module that doesn't require it.
		}
		addDefaultSerializer(ConcurrentHashMap.KeySetView.class, KeySetViewSerializer::new);
		addDefaultSerializer(URI.class, URISerializer::new);
		addDefaultSerializer(UUID.class, UUIDSerializer::new);
		addDefaultSerializer(Pattern.class, PatternSerializer::new);
		addDefaultSerializer(AtomicBoolean.class, AtomicBooleanSerializer::new);
		addDefaultSerializer(AtomicInteger.class, AtomicIntegerSerializer::new);
		addDefaultSerializer(AtomicLong.class, AtomicLongSerializer::new);
		addDefaultSerializer(AtomicReference.class, AtomicReferenceSerializer::new);
		OptionalSerializers.addDefaultSerializers(this);
		TimeSerializers.addDefaultSerializers(this);
		ImmutableCollectionsSerializers.addDefaultSerializers(this);
		if (!isAndroid) { // The wrapped collection can't be accessed on Android.
			UnmodifiableCollectionSerializers.addDefaultSerializers(this);
			SynchronizedCollectionSerializers.addDefaultSerializers(this);
			addDefaultSerializer(Collections.newSetFromMap(new HashMap<>()).getClass(), new SetFromMapSerializer());
		}
		lowPriorityDefaultSerializerCount = defaultSerializers.size();

		// Primitives and string. Primitive wrappers automatically use the same registration as primitives.
		register(int.class, new IntSerializer());
		register(String.class, new StringSerializer());
		register(float.class, new FloatSerializer());
		register(boolean.class, new BooleanSerializer());
		register(byte.class, new ByteSerializer());
		register(char.class, new CharSerializer());
		register(short.class, new ShortSerializer());
		register(long.class, new LongSerializer());
		register(double.class, new DoubleSerializer());
	}

	// --- Default serializers ---

	/** Sets the serializer factory to use when no {@link #addDefaultSerializer(Class, Class) default serializers} match an
	 * object's type. Default is {@link FieldSerializerFactory}.
	 * @see #newDefaultSerializer(Class) */
	public void setDefaultSerializer (SerializerFactory serializer) {
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		defaultSerializer = serializer;
	}

	/** Sets the serializer to use when no {@link #addDefaultSerializer(Class, Class) default serializers} match an object's type.
	 * Default is {@link FieldSerializer}.
	 * @see #newDefaultSerializer(Class) */
	public void setDefaultSerializer (Class<? extends Serializer> serializer) {
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		defaultSerializer = new ReflectionSerializerFactory(serializer);
	}

	/** Instances of the specified class will use the specified serializer when {@link #register(Class)} or
	 * {@link #register(Class, int)} are called.
	 * @see #setDefaultSerializer(Class) */
	public void addDefaultSerializer (Class type, Serializer serializer) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		insertDefaultSerializer(type, new SingletonSerializerFactory(serializer));
	}

	/** Instances of the specified class will use the specified factory to create a serializer when {@link #register(Class)} or
	 * {@link #register(Class, int)} are called.
	 * @see #setDefaultSerializer(Class) */
	public void addDefaultSerializer (Class type, SerializerFactory serializerFactory) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializerFactory == null) throw new IllegalArgumentException("serializerFactory cannot be null.");
		insertDefaultSerializer(type, serializerFactory);
	}

	/** Instances of the specified class will use a new serializer from the specified supplier, eg a constructor reference like
	 * {@code DateSerializer::new}, when {@link #register(Class)} or {@link #register(Class, int)} are called. Unlike
	 * {@link #addDefaultSerializer(Class, Class)}, this doesn't use reflection, which needs metadata in a GraalVM native image.
	 * @see #setDefaultSerializer(Class) */
	public void addDefaultSerializer (Class type, Supplier<? extends Serializer> serializerSupplier) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializerSupplier == null) throw new IllegalArgumentException("serializerSupplier cannot be null.");
		insertDefaultSerializer(type, new BaseSerializerFactory() {
			public Serializer newSerializer (Kryo kryo, Class type) {
				return serializerSupplier.get();
			}
		});
	}

	/** Instances of the specified class will use the specified serializer when {@link #register(Class)} or
	 * {@link #register(Class, int)} are called. Serializer instances are created as needed via
	 * {@link ReflectionSerializerFactory#newSerializer(Kryo, Class, Class)}.
	 * <p>
	 * Kryo has built-in default serializers for primitives and their wrappers, strings, arrays, enums, {@link Collection}s,
	 * {@link Map}s and more than 100 other JDK classes, which are listed in the README. The order default serializers are added is
	 * important for a class that may match multiple types: the built-in default serializers always have a lower priority than the
	 * default serializers that are added. */
	public void addDefaultSerializer (Class type, Class<? extends Serializer> serializerClass) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializerClass == null) throw new IllegalArgumentException("serializerClass cannot be null.");
		insertDefaultSerializer(type, new ReflectionSerializerFactory(serializerClass));
	}

	private int insertDefaultSerializer (Class type, SerializerFactory factory) {
		int lowest = 0;
		for (int i = 0, n = defaultSerializers.size() - lowPriorityDefaultSerializerCount; i < n; i++)
			if (type.isAssignableFrom(defaultSerializers.get(i).type)) lowest = i + 1;
		defaultSerializers.add(lowest, new DefaultSerializerEntry(type, factory));
		defaultSerializerTypes = null;
		return lowest;
	}

	/** Returns a factory for the serializer that is used for the type, but not its subclasses, which may have more state. */
	private static SerializerFactory exactType (Class type, Serializer serializer) {
		return new SingletonSerializerFactory(serializer) {
			public boolean isSupported (Class subtype) {
				return subtype == type;
			}
		};
	}

	/** Returns the type with the specified name if it has a default serializer. This finds classes without reflection, eg
	 * JDK-internal classes like the one returned by {@code List.of} in a GraalVM native image.
	 * @return May be null. */
	public Class getDefaultSerializerType (String className) {
		if (defaultSerializerTypes == null) {
			defaultSerializerTypes = new ObjectMap(defaultSerializers.size());
			for (DefaultSerializerEntry entry : defaultSerializers)
				defaultSerializerTypes.put(entry.type.getName(), entry.type);
		}
		return defaultSerializerTypes.get(className);
	}

	/** Returns the best matching serializer for a class. This method can be overridden to implement custom logic to choose a
	 * serializer. */
	public Serializer getDefaultSerializer (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");

		Serializer serializerForAnnotation = getDefaultSerializerForAnnotatedType(type);
		if (serializerForAnnotation != null) return serializerForAnnotation;

		for (int i = 0, n = defaultSerializers.size(); i < n; i++) {
			DefaultSerializerEntry entry = defaultSerializers.get(i);
			if (entry.type.isAssignableFrom(type) && entry.serializerFactory.isSupported(type))
				return entry.serializerFactory.newSerializer(this, type);
		}

		return newDefaultSerializer(type);
	}

	/** Returns a serializer created from the {@link DefaultSerializer} annotation of the type, or null if it has none. */
	protected Serializer getDefaultSerializerForAnnotatedType (Class type) {
		if (type.isAnnotationPresent(DefaultSerializer.class)) {
			DefaultSerializer annotation = (DefaultSerializer)type.getAnnotation(DefaultSerializer.class);
			return newFactory(annotation.serializerFactory(), annotation.value()).newSerializer(this, type);
		}
		return null;
	}

	/** Called by {@link #getDefaultSerializer(Class)} when no default serializers matched the type. Subclasses can override this
	 * method to customize behavior. The default implementation calls {@link SerializerFactory#newSerializer(Kryo, Class)} using
	 * the {@link #setDefaultSerializer(Class) default serializer}. */
	protected Serializer newDefaultSerializer (Class type) {
		return defaultSerializer.newSerializer(this, type);
	}

	// --- Registration ---

	/** Registers the class using the lowest, next available integer ID and the {@link Kryo#getDefaultSerializer(Class) default
	 * serializer}. If the class is already registered, no change will be made and the existing registration will be returned.
	 * Registering a primitive also affects the corresponding primitive wrapper.
	 * <p>
	 * Because the ID assigned is affected by the IDs registered before it, the order classes are registered is important when
	 * using this method. The order must be the same at deserialization as it was for serialization. */
	public Registration register (Class type) {
		Registration registration = classResolver.getRegistration(type);
		if (registration != null) return registration;
		return register(type, getDefaultSerializer(type));
	}

	/** Registers the class using the specified ID and the {@link Kryo#getDefaultSerializer(Class) default serializer}. If the
	 * class is already registered this has no effect and the existing registration is returned. Registering a primitive also
	 * affects the corresponding primitive wrapper.
	 * <p>
	 * IDs must be the same at deserialization as they were for serialization.
	 * @param id Must be {@code >= 0}. Smaller IDs are serialized more efficiently. IDs 0-8 are used by default for primitive types
	 *           and String, but these IDs can be repurposed. */
	public Registration register (Class type, int id) {
		Registration registration = classResolver.getRegistration(type);
		if (registration != null) return registration;
		return register(type, getDefaultSerializer(type), id);
	}

	/** Registers the class using the lowest, next available integer ID and the specified serializer. If the class is already
	 * registered, the existing entry is updated with the new serializer. Registering a primitive also affects the corresponding
	 * primitive wrapper.
	 * <p>
	 * Because the ID assigned is affected by the IDs registered before it, the order classes are registered is important when
	 * using this method. The order must be the same at deserialization as it was for serialization. */
	public Registration register (Class type, Serializer serializer) {
		Registration registration = classResolver.getRegistration(type);
		if (registration != null) {
			registration.setSerializer(serializer);
			return registration;
		}
		return classResolver.register(new Registration(type, serializer, getNextRegistrationId()));
	}

	/** Registers the class using the specified ID and serializer. Providing an ID that is already in use by the same type will
	 * cause the old entry to be overwritten. Registering a primitive also affects the corresponding primitive wrapper.
	 * <p>
	 * IDs must be the same at deserialization as they were for serialization.
	 * @param id Must be {@code >= 0}. Smaller IDs are serialized more efficiently. IDs 0-8 are used by default for primitive types
	 *           and String, but these IDs can be repurposed. */
	public Registration register (Class type, Serializer serializer, int id) {
		if (id < 0) throw new IllegalArgumentException("id must be >= 0: " + id);
		return register(new Registration(type, serializer, id));
	}

	/** Stores the specified registration. If the ID is already in use by the same type, the old entry is overwritten. Registering
	 * a primitive also affects the corresponding primitive wrapper.
	 * <p>
	 * IDs must be the same at deserialization as they were for serialization.
	 * <p>
	 * Registration can be subclassed to efficiently store per type information, accessible in serializers via
	 * {@link Kryo#getRegistration(Class)}. */
	public Registration register (Registration registration) {
		int id = registration.getId();
		if (id < 0) throw new IllegalArgumentException("id must be > 0: " + id);

		Registration existing = classResolver.unregister(id);
		if (DEBUG && existing != null && existing.getType() != registration.getType())
			debug("kryo", "Registration overwritten: " + existing + " -> " + registration);

		return classResolver.register(registration);
	}

	/** Returns the lowest, next available integer ID. */
	public int getNextRegistrationId () {
		while (nextRegisterID != -2) {
			if (classResolver.getRegistration(nextRegisterID) == null) return nextRegisterID;
			nextRegisterID++;
		}
		throw new KryoException("No registration IDs are available.");
	}

	/** If the class is not registered and {@link Kryo#setRegistrationRequired(boolean)} is false or
	 * {@link #setAllowedUnregisteredClasses(Predicate)} allows the class, it is automatically registered using the
	 * {@link Kryo#addDefaultSerializer(Class, Class) default serializer}.
	 * @throws IllegalArgumentException if the class is not registered, {@link Kryo#setRegistrationRequired(boolean)} is true and
	 *            the class is not allowed.
	 * @see ClassResolver#getRegistration(Class) */
	public Registration getRegistration (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");

		Registration registration = classResolver.getRegistration(type);
		if (registration == null) {
			if (isProxy(type)) {
				// If a Proxy class, treat it like an InvocationHandler because the concrete class for a proxy is generated.
				registration = getRegistration(InvocationHandler.class);
			} else if (!type.isEnum() && Enum.class.isAssignableFrom(type) && type != Enum.class) {
				// This handles an enum value that is an inner class, eg: enum A {b{}}
				while (true) {
					type = type.getSuperclass();
					if (type == null) break;
					if (type.isEnum()) {
						registration = classResolver.getRegistration(type);
						break;
					}
				}
			} else if (EnumSet.class.isAssignableFrom(type))
				registration = classResolver.getRegistration(EnumSet.class);
			else if (isClosure(type)) {
				registration = classResolver.getRegistration(ClosureSerializer.Closure.class);
				// The class of a closure can't be found by its name when reading, so it can't be registered implicitly.
				if (registration == null) {
					throw new IllegalArgumentException("Class is a closure, but ClosureSerializer.Closure is not registered: "
						+ className(type)
						+ "\nNote: To register it use: kryo.register(ClosureSerializer.Closure.class, new ClosureSerializer());");
				}
			}
			if (registration == null) {
				if (registrationRequired && !isAllowedUnregistered(type))
					throw new IllegalArgumentException(unregisteredClassMessage(type));
				if (WARN && warnUnregisteredClasses) warn(unregisteredClassMessage(type));
				registration = classResolver.registerImplicit(type);
			}
		}
		return registration;
	}

	/** Returns the message of the exception thrown and the warning logged for an unregistered class. This can be overridden to
	 * customize the message or take other actions.
	 * @see #setWarnUnregisteredClasses(boolean) */
	protected String unregisteredClassMessage (Class type) {
		return "Class is not registered: " + className(type) + "\nNote: To register this class use: kryo.register("
			+ canonicalName(type) + ".class);";
	}

	/** @see ClassResolver#getRegistration(int) */
	public Registration getRegistration (int classID) {
		return classResolver.getRegistration(classID);
	}

	/** Returns the serializer for the registration for the specified class.
	 * @see #getRegistration(Class)
	 * @see Registration#getSerializer() */
	public Serializer getSerializer (Class type) {
		return getRegistration(type).getSerializer();
	}

	// --- Serialization ---

	/** Writes a class and returns its registration.
	 * @param type May be null.
	 * @return Will be null if type is null.
	 * @see ClassResolver#writeClass(Output, Class) */
	public Registration writeClass (Output output, Class type) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		try {
			return classResolver.writeClass(output, type);
		} finally {
			if (depth == 0 && autoReset) reset();
		}
	}

	/** Writes an object using the registered serializer. */
	public void writeObject (Output output, Object object) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		if (object == null) throw new IllegalArgumentException("object cannot be null.");
		beginObject();
		try {
			if (references && writeReferenceOrNull(output, object, false)) return;
			if (TRACE || (DEBUG && depth == 1)) log("Write", object, output.position());
			getRegistration(object.getClass()).getSerializer().write(this, output, object);
		} finally {
			if (--depth == 0 && autoReset) reset();
		}
	}

	/** Writes an object using the specified serializer. The registered serializer is ignored. */
	public void writeObject (Output output, Object object, Serializer serializer) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		if (object == null) throw new IllegalArgumentException("object cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		beginObject();
		try {
			if (references && writeReferenceOrNull(output, object, false)) return;
			if (TRACE || (DEBUG && depth == 1)) log("Write", object, output.position());
			serializer.write(this, output, object);
		} finally {
			if (--depth == 0 && autoReset) reset();
		}
	}

	/** Writes an object or null using the registered serializer for the specified type.
	 * @param object May be null. */
	public void writeObjectOrNull (Output output, Object object, Class type) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		beginObject();
		try {
			Serializer serializer = getRegistration(type).getSerializer();
			if (references) {
				if (writeReferenceOrNull(output, object, true)) return;
			} else if (!serializer.getAcceptsNull()) {
				if (object == null) {
					if (TRACE || (DEBUG && depth == 1)) log("Write", object, output.position());
					output.writeByte(NULL);
					return;
				}
				if (TRACE) trace("kryo", "Write: <not null>" + pos(output.position()));
				output.writeByte(NOT_NULL);
			}
			if (TRACE || (DEBUG && depth == 1)) log("Write", object, output.position());
			serializer.write(this, output, object);
		} finally {
			if (--depth == 0 && autoReset) reset();
		}
	}

	/** Writes an object or null using the specified serializer. The registered serializer is ignored.
	 * @param object May be null. */
	public void writeObjectOrNull (Output output, Object object, Serializer serializer) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		beginObject();
		try {
			if (references) {
				if (writeReferenceOrNull(output, object, true)) return;
			} else if (!serializer.getAcceptsNull()) {
				if (object == null) {
					if (TRACE || (DEBUG && depth == 1)) log("Write", null, output.position());
					output.writeByte(NULL);
					return;
				}
				if (TRACE) trace("kryo", "Write: <not null>" + pos(output.position()));
				output.writeByte(NOT_NULL);
			}
			if (TRACE || (DEBUG && depth == 1)) log("Write", object, output.position());
			serializer.write(this, output, object);
		} finally {
			if (--depth == 0 && autoReset) reset();
		}
	}

	/** Writes the class and object or null using the registered serializer.
	 * @param object May be null. */
	public void writeClassAndObject (Output output, Object object) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		beginObject();
		try {
			if (object == null) {
				writeClass(output, null);
				return;
			}
			Registration registration = writeClass(output, object.getClass());
			if (references && writeReferenceOrNull(output, object, false)) return;
			if (TRACE || (DEBUG && depth == 1)) log("Write", object, output.position());
			registration.getSerializer().write(this, output, object);
		} finally {
			if (--depth == 0 && autoReset) reset();
		}
	}

	/** @param object May be null if mayBeNull is true.
	 * @return true if no bytes need to be written for the object. */
	boolean writeReferenceOrNull (Output output, Object object, boolean mayBeNull) {
		if (object == null) {
			if (TRACE || (DEBUG && depth == 1)) log("Write", null, output.position());
			output.writeByte(NULL);
			return true;
		}

		if (!referenceResolver.useReferences(object.getClass())) {
			if (mayBeNull) {
				if (TRACE) trace("kryo", "Write: <not null>" + pos(output.position()));
				output.writeByte(NOT_NULL);
			}
			return false;
		}

		// Determine if this object has already been seen in this object graph.
		int id = referenceResolver.getWrittenId(object);

		// If not the first time encountered, only write reference ID.
		if (id != -1) {
			if (DEBUG) debug("kryo", "Write reference " + id + ": " + string(object) + pos(output.position()));
			output.writeVarInt(id + 2, true); // + 2 because 0 and 1 are used for NULL and NOT_NULL.
			return true;
		}

		// Otherwise write NOT_NULL and then the object bytes.
		id = referenceResolver.addWrittenObject(object);
		if (TRACE) trace("kryo", "Write: <not null>" + pos(output.position()));
		output.writeByte(NOT_NULL);
		if (TRACE) trace("kryo", "Write initial reference " + id + ": " + string(object) + pos(output.position()));
		return false;
	}

	/** Reads a class and returns its registration.
	 * @return May be null.
	 * @see ClassResolver#readClass(Input) */
	public Registration readClass (Input input) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		try {
			return classResolver.readClass(input);
		} finally {
			if (depth == 0 && autoReset) reset();
		}
	}

	/** Reads an object using the registered serializer. */
	public <T> T readObject (Input input, Class<T> type) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		beginObject();
		try {
			T object;
			if (references) {
				int stackSize = readReferenceOrNull(input, type, false);
				if (stackSize == REF) return (T)readObject;
				object = (T)getRegistration(type).getSerializer().read(this, input, type);
				if (stackSize == readReferenceIds.size) reference(object);
			} else
				object = (T)getRegistration(type).getSerializer().read(this, input, type);
			if (TRACE || (DEBUG && depth == 1)) log("Read", object, input.position());
			return object;
		} finally {
			if (--depth == 0 && autoReset) reset();
		}
	}

	/** Reads an object using the specified serializer. The registered serializer is ignored. */
	public <T> T readObject (Input input, Class<T> type, Serializer serializer) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		beginObject();
		try {
			T object;
			if (references) {
				int stackSize = readReferenceOrNull(input, type, false);
				if (stackSize == REF) return (T)readObject;
				object = (T)serializer.read(this, input, type);
				if (stackSize == readReferenceIds.size) reference(object);
			} else
				object = (T)serializer.read(this, input, type);
			if (TRACE || (DEBUG && depth == 1)) log("Read", object, input.position());
			return object;
		} finally {
			if (--depth == 0 && autoReset) reset();
		}
	}

	/** Reads an object or null using the registered serializer.
	 * @return May be null. */
	public <T> T readObjectOrNull (Input input, Class<T> type) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		beginObject();
		try {
			T object;
			if (references) {
				int stackSize = readReferenceOrNull(input, type, true);
				if (stackSize == REF) return (T)readObject;
				object = (T)getRegistration(type).getSerializer().read(this, input, type);
				if (stackSize == readReferenceIds.size) reference(object);
			} else {
				Serializer serializer = getRegistration(type).getSerializer();
				if (!serializer.getAcceptsNull() && input.readByte() == NULL) {
					if (TRACE || (DEBUG && depth == 1)) log("Read", null, input.position());
					return null;
				}
				object = (T)serializer.read(this, input, type);
			}
			if (TRACE || (DEBUG && depth == 1)) log("Read", object, input.position());
			return object;
		} finally {
			if (--depth == 0 && autoReset) reset();
		}
	}

	/** Reads an object or null using the specified serializer. The registered serializer is ignored.
	 * @return May be null. */
	public <T> T readObjectOrNull (Input input, Class<T> type, Serializer serializer) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		beginObject();
		try {
			T object;
			if (references) {
				int stackSize = readReferenceOrNull(input, type, true);
				if (stackSize == REF) return (T)readObject;
				object = (T)serializer.read(this, input, type);
				if (stackSize == readReferenceIds.size) reference(object);
			} else {
				if (!serializer.getAcceptsNull() && input.readByte() == NULL) {
					if (TRACE || (DEBUG && depth == 1)) log("Read", null, input.position());
					return null;
				}
				object = (T)serializer.read(this, input, type);
			}
			if (TRACE || (DEBUG && depth == 1)) log("Read", object, input.position());
			return object;
		} finally {
			if (--depth == 0 && autoReset) reset();
		}
	}

	/** Reads the class and object or null using the registered serializer.
	 * @return May be null. */
	public Object readClassAndObject (Input input) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		beginObject();
		try {
			Registration registration = readClass(input);
			if (registration == null) return null;
			Class type = registration.getType();

			Object object;
			if (references) {
				int stackSize = readReferenceOrNull(input, type, false);
				if (stackSize == REF) return readObject;
				object = registration.getSerializer().read(this, input, type);
				if (stackSize == readReferenceIds.size) reference(object);
			} else
				object = registration.getSerializer().read(this, input, type);
			if (TRACE || (DEBUG && depth == 1)) log("Read", object, input.position());
			return object;
		} finally {
			if (--depth == 0 && autoReset) reset();
		}
	}

	/** Returns {@link #REF} if a reference to a previously read object was read, which is stored in {@link #readObject}. Returns a
	 * stack size (> 0) if a reference ID has been put on the stack. */
	int readReferenceOrNull (Input input, Class type, boolean mayBeNull) {
		if (type.isPrimitive()) type = getWrapperClass(type);
		boolean referencesSupported = referenceResolver.useReferences(type);
		int id;
		if (mayBeNull) {
			id = input.readVarInt(true);
			if (id == NULL) {
				if (TRACE || (DEBUG && depth == 1)) log("Read", null, input.position());
				readObject = null;
				return REF;
			}
			if (!referencesSupported) {
				readReferenceIds.add(NO_REF);
				return readReferenceIds.size;
			}
		} else {
			if (!referencesSupported) {
				readReferenceIds.add(NO_REF);
				return readReferenceIds.size;
			}
			id = input.readVarInt(true);
		}
		if (id == NOT_NULL) {
			if (TRACE) trace("kryo", "Read: <not null>" + pos(input.position()));
			// First time object has been encountered.
			id = referenceResolver.nextReadId(type);
			if (TRACE) trace("kryo", "Read initial reference " + id + ": " + className(type) + pos(input.position()));
			readReferenceIds.add(id);
			return readReferenceIds.size;
		}
		// The id is an object reference.
		id -= 2; // - 2 because 0 and 1 are used for NULL and NOT_NULL.
		try {
			readObject = referenceResolver.getReadObject(type, id);
		} catch (Exception e) {
			throw new KryoException("Unable to resolve reference for " + className(type) + " with id: " + id, e);
		}
		if (DEBUG) debug("kryo", "Read reference " + id + ": " + string(readObject) + pos(input.position()));
		return REF;
	}

	/** Called by {@link Serializer#read(Kryo, Input, Class)} and {@link Serializer#copy(Kryo, Object)} before Kryo can be used to
	 * deserialize or copy child objects. Calling this method is unnecessary if Kryo is not used to deserialize or copy child
	 * objects.
	 * @param object May be null, unless calling this method from {@link Serializer#copy(Kryo, Object)}. */
	public void reference (Object object) {
		if (copyDepth > 0) {
			if (needsCopyReference != null) {
				if (object == null) throw new IllegalArgumentException("object cannot be null.");
				originalToCopy.put(needsCopyReference, object);
				needsCopyReference = null;
			}
		} else if (references && object != null) {
			int id = readReferenceIds.pop();
			if (id != NO_REF) referenceResolver.setReadObject(id, object);
		}
	}

	/** Resets object graph state: unregistered class names, references to previously serialized or deserialized objects, the
	 * {@link #getOriginalToCopyMap() original to copy map}, the {@link #getGraphContext() graph context} and the
	 * {@link #getGenerics() generic type information} collected during serialization. If {@link #setAutoReset(boolean) auto reset}
	 * is true, this method is called automatically when an object graph has been completely serialized or deserialized. If
	 * overridden, the super method must be called. */
	public void reset () {
		depth = 0;
		if (graphContext != null) graphContext.clear(2048);
		classResolver.reset();
		generics.reset();
		if (references) {
			referenceResolver.reset();
			readObject = null;
		}

		copyDepth = 0;
		if (originalToCopy != null) originalToCopy.clear(2048);

		if (TRACE) trace("kryo", "Object graph complete.");
	}

	/** Returns a deep copy of the object. Serializers for the classes involved must support {@link Serializer#copy(Kryo, Object)}.
	 * @param object May be null. */
	public <T> T copy (T object) {
		if (object == null) return null;
		if (copyShallow) return object;
		copyDepth++;
		try {
			if (originalToCopy == null) originalToCopy = new IdentityMap();
			Object existingCopy = originalToCopy.get(object);
			if (existingCopy != null) return (T)existingCopy;

			// Nested copies must not replace the object of an outer copy that the serializer has not referenced yet.
			Object outerNeedsCopyReference = needsCopyReference;
			if (copyReferences) needsCopyReference = object;
			Object copy;
			if (object instanceof KryoCopyable)
				copy = ((KryoCopyable)object).copy(this);
			else
				copy = getSerializer(object.getClass()).copy(this, object);
			if (needsCopyReference != null) reference(copy);
			needsCopyReference = outerNeedsCopyReference;
			if (TRACE || (DEBUG && copyDepth == 1)) log("Copy", copy, -1);
			return (T)copy;
		} finally {
			if (--copyDepth == 0) reset();
		}
	}

	/** Returns a deep copy of the object using the specified serializer. Serializers for the classes involved must support
	 * {@link Serializer#copy(Kryo, Object)}.
	 * @param object May be null. */
	public <T> T copy (T object, Serializer serializer) {
		if (object == null) return null;
		if (copyShallow) return object;
		copyDepth++;
		try {
			if (originalToCopy == null) originalToCopy = new IdentityMap();
			Object existingCopy = originalToCopy.get(object);
			if (existingCopy != null) return (T)existingCopy;

			// Nested copies must not replace the object of an outer copy that the serializer has not referenced yet.
			Object outerNeedsCopyReference = needsCopyReference;
			if (copyReferences) needsCopyReference = object;
			Object copy;
			if (object instanceof KryoCopyable)
				copy = ((KryoCopyable)object).copy(this);
			else
				copy = serializer.copy(this, object);
			if (needsCopyReference != null) reference(copy);
			needsCopyReference = outerNeedsCopyReference;
			if (TRACE || (DEBUG && copyDepth == 1)) log("Copy", copy, -1);
			return (T)copy;
		} finally {
			if (--copyDepth == 0) reset();
		}
	}

	/** Returns a shallow copy of the object. Serializers for the classes involved must support
	 * {@link Serializer#copy(Kryo, Object)}.
	 * @param object May be null. */
	public <T> T copyShallow (T object) {
		if (object == null) return null;
		copyDepth++;
		boolean outerCopyShallow = copyShallow; // Restored, because a serializer can make a nested shallow copy.
		copyShallow = true;
		try {
			if (originalToCopy == null) originalToCopy = new IdentityMap();
			Object existingCopy = originalToCopy.get(object);
			if (existingCopy != null) return (T)existingCopy;

			Object outerNeedsCopyReference = needsCopyReference; // See copy(Object).
			if (copyReferences) needsCopyReference = object;
			Object copy;
			if (object instanceof KryoCopyable)
				copy = ((KryoCopyable)object).copy(this);
			else
				copy = getSerializer(object.getClass()).copy(this, object);
			if (needsCopyReference != null) reference(copy);
			needsCopyReference = outerNeedsCopyReference;
			if (TRACE || (DEBUG && copyDepth == 1)) log("Shallow copy", copy, -1);
			return (T)copy;
		} finally {
			copyShallow = outerCopyShallow;
			if (--copyDepth == 0) reset();
		}
	}

	/** Returns a shallow copy of the object using the specified serializer. Serializers for the classes involved must support
	 * {@link Serializer#copy(Kryo, Object)}.
	 * @param object May be null. */
	public <T> T copyShallow (T object, Serializer serializer) {
		if (object == null) return null;
		copyDepth++;
		boolean outerCopyShallow = copyShallow; // Restored, because a serializer can make a nested shallow copy.
		copyShallow = true;
		try {
			if (originalToCopy == null) originalToCopy = new IdentityMap();
			Object existingCopy = originalToCopy.get(object);
			if (existingCopy != null) return (T)existingCopy;

			Object outerNeedsCopyReference = needsCopyReference; // See copy(Object).
			if (copyReferences) needsCopyReference = object;
			Object copy;
			if (object instanceof KryoCopyable)
				copy = ((KryoCopyable)object).copy(this);
			else
				copy = serializer.copy(this, object);
			if (needsCopyReference != null) reference(copy);
			needsCopyReference = outerNeedsCopyReference;
			if (TRACE || (DEBUG && copyDepth == 1)) log("Shallow copy", copy, -1);
			return (T)copy;
		} finally {
			copyShallow = outerCopyShallow;
			if (--copyDepth == 0) reset();
		}
	}

	// --- Utility ---

	private void beginObject () {
		if (DEBUG) {
			if (depth == 0)
				thread = Thread.currentThread();
			else if (thread != Thread.currentThread())
				throw new ConcurrentModificationException("Kryo must not be accessed concurrently by multiple threads.");
		}
		if (depth == maxDepth) throw new KryoException("Max depth exceeded: " + depth);
		depth++;
	}

	public ClassResolver getClassResolver () {
		return classResolver;
	}

	/** @return May be null. */
	public ReferenceResolver getReferenceResolver () {
		return referenceResolver;
	}

	/** Sets the classloader to resolve unregistered class names to classes. The default is the loader that loaded the Kryo
	 * class. */
	public void setClassLoader (ClassLoader classLoader) {
		if (classLoader == null) throw new IllegalArgumentException("classLoader cannot be null.");
		this.classLoader = classLoader;
	}

	public ClassLoader getClassLoader () {
		return classLoader;
	}

	/** If true, an exception is thrown when an unregistered class is encountered. Default is true.
	 * <p>
	 * If false, when an unregistered class is encountered, its fully qualified class name will be serialized and the
	 * {@link #addDefaultSerializer(Class, Class) default serializer} for the class used to serialize the object. Subsequent
	 * appearances of the class within the same object graph are serialized as an int id.
	 * <p>
	 * Registered classes are serialized as an int id, avoiding the overhead of serializing the class name, but have the drawback
	 * of needing to know the classes to be serialized up front.
	 * <p>
	 * Requiring class registration controls which classes Kryo will instantiate. When false, during deserialization Kryo will
	 * invoke the constructor for whatever class name is found in the data. It can be a security problem to allow arbitrary classes
	 * to be instantiated (and later finalized). {@link #setAllowedUnregisteredClasses(Predicate)} can allow some unregistered
	 * classes while registration stays required. */
	public void setRegistrationRequired (boolean registrationRequired) {
		this.registrationRequired = registrationRequired;
		if (TRACE) trace("kryo", "Registration required: " + registrationRequired);
	}

	public boolean isRegistrationRequired () {
		return registrationRequired;
	}

	/** Allows classes that are not registered, even though {@link #setRegistrationRequired(boolean) registration is required}, if
	 * the predicate accepts them, eg the classes of a package:
	 * 
	 * <pre>
	 * kryo.setAllowedUnregisteredClasses(type -&gt; type.getName().startsWith("com.example."));
	 * </pre>
	 * 
	 * Like with registration not required, the class names of these classes are written, and other classes, including the classes
	 * in the serialized data when reading, must still be registered. For arrays, the predicate is called with the component type.
	 * @param allowedUnregisteredClasses May be null to allow no unregistered classes (default). */
	public void setAllowedUnregisteredClasses (Predicate<Class> allowedUnregisteredClasses) {
		this.allowedUnregisteredClasses = allowedUnregisteredClasses;
	}

	/** @return May be null. */
	public Predicate<Class> getAllowedUnregisteredClasses () {
		return allowedUnregisteredClasses;
	}

	private boolean isAllowedUnregistered (Class type) {
		if (allowedUnregisteredClasses == null) return false;
		return allowedUnregisteredClasses.test(type.isArray() ? getElementClass(type) : type);
	}

	/** If true, kryo writes a warn log entry when an unregistered class is encountered. Default is false. */
	public void setWarnUnregisteredClasses (boolean warnUnregisteredClasses) {
		this.warnUnregisteredClasses = warnUnregisteredClasses;
		if (TRACE) trace("kryo", "Warn unregistered classes: " + warnUnregisteredClasses);
	}

	public boolean getWarnUnregisteredClasses () {
		return warnUnregisteredClasses;
	}

	/** If true, each appearance of an object in the graph after the first is stored as an integer ordinal. This enables references
	 * to the same object and cyclic graphs to be serialized, but typically adds overhead of one byte per object. When set to true
	 * and no {@link #setReferenceResolver(ReferenceResolver) reference resolver} has been set, {@link MapReferenceResolver} is
	 * used. Default is false.
	 * <p>
	 * String fields of {@link FieldSerializer} and its subclasses decide when the serializer is created whether they use
	 * references, so if the reference resolver uses references for strings, this must be called before registering classes.
	 * @return The previous value.
	 * @throws KryoException if this changes whether strings use references after a String field was created. */
	public boolean setReferences (boolean references) {
		boolean old = this.references;
		if (references == old) return references;
		checkStringReferences(references, referenceResolver);
		if (old) {
			referenceResolver.reset();
			readObject = null;
		}
		this.references = references;
		if (references && referenceResolver == null) referenceResolver = new MapReferenceResolver();
		if (TRACE) trace("kryo", "References: " + references);
		return !references;
	}

	/** If true, when {@link #copy(Object)} and other copy methods encounter an object for the first time the object is copied and
	 * on subsequent encounters the copied object is used. If false, the overhead of tracking which objects have already been
	 * copied is avoided because each object is copied every time it is encountered, however a stack overflow will occur if an
	 * object graph is copied that contains a circular reference. Default is true. */
	public void setCopyReferences (boolean copyReferences) {
		this.copyReferences = copyReferences;
	}

	/** Sets the reference resolver and enables references.
	 * @throws KryoException if this changes whether strings use references after a String field was created, see
	 *            {@link #setReferences(boolean)}. */
	public void setReferenceResolver (ReferenceResolver referenceResolver) {
		if (referenceResolver == null) throw new IllegalArgumentException("referenceResolver cannot be null.");
		referenceResolver.setKryo(this); // Before the check, useReferences may need the Kryo instance.
		checkStringReferences(true, referenceResolver);
		this.references = true;
		this.referenceResolver = referenceResolver;
		if (TRACE) trace("kryo", "Reference resolver: " + referenceResolver.getClass().getName());
	}

	public boolean getReferences () {
		return references;
	}

	/** Returns true if strings are written with references: references are enabled and the reference resolver uses references for
	 * strings. {@link FieldSerializer} calls this when it creates a String field, after which the result can't change, see
	 * {@link #setReferences(boolean)}. */
	public boolean usesStringReferences () {
		if (stringFieldReferences == -1) stringFieldReferences = stringReferences(references, referenceResolver) ? 1 : 0;
		return stringFieldReferences == 1;
	}

	/** Throws if a String field was created and the specified settings change whether strings use references. */
	void checkStringReferences (boolean references, ReferenceResolver referenceResolver) {
		if (stringFieldReferences != -1 && stringReferences(references, referenceResolver) != (stringFieldReferences == 1)) {
			throw new KryoException(
				"Whether strings use references can't be changed after a FieldSerializer was created for a class "
					+ "with a String field, because the field decides when it is created whether it is written with references. Set "
					+ "references and the reference resolver before registering classes.");
		}
	}

	private static boolean stringReferences (boolean references, ReferenceResolver referenceResolver) {
		return references && referenceResolver != null && referenceResolver.useReferences(String.class);
	}

	/** Sets the strategy used by {@link #newInstantiator(Class)} for creating objects. Default is
	 * {@link DefaultInstantiatorStrategy}, which calls the zero argument constructor. See {@link StdInstantiatorStrategy} to
	 * create objects without calling any constructor. See {@link SerializingInstantiatorStrategy} to mimic Java's built-in
	 * serialization. */
	public void setInstantiatorStrategy (InstantiatorStrategy strategy) {
		this.strategy = strategy;
	}

	public InstantiatorStrategy getInstantiatorStrategy () {
		return strategy;
	}

	/** Returns a new instantiator for creating new instances of the specified type, using the
	 * {@link #setInstantiatorStrategy(InstantiatorStrategy) instantiator strategy}. By default, the instantiator calls the zero
	 * argument constructor of the class, or throws an exception if it has none. */
	protected ObjectInstantiator newInstantiator (Class type) {
		return strategy.newInstantiatorOf(type);
	}

	/** Creates a new instance of a class using {@link Registration#getInstantiator()}. If the registration's instantiator is null,
	 * a new one is set using {@link #newInstantiator(Class)}. */
	public <T> T newInstance (Class<T> type) {
		Registration registration = getRegistration(type);
		ObjectInstantiator instantiator = registration.getInstantiator();
		if (instantiator == null) {
			instantiator = newInstantiator(type);
			registration.setInstantiator(instantiator);
		}
		return (T)instantiator.newInstance();
	}

	/** Name/value pairs that are available to all serializers. */
	public ObjectMap getContext () {
		if (context == null) context = new ObjectMap();
		return context;
	}

	/** Name/value pairs that are available to all serializers and are cleared after each object graph is serialized or
	 * deserialized. */
	public ObjectMap getGraphContext () {
		// Small, because it usually has few entries and clearing it after each object graph is notable for small graphs.
		if (graphContext == null) graphContext = new ObjectMap(4);
		return graphContext;
	}

	/** Returns the number of child objects away from the object graph root. */
	public int getDepth () {
		return depth;
	}

	/** Returns the internal map of original to copy objects when a copy method is used. This can be used after a copy to map old
	 * objects to the copies, however it is cleared automatically by {@link #reset()} so this is only useful when
	 * {@link #setAutoReset(boolean)} is false. */
	public IdentityMap getOriginalToCopyMap () {
		return originalToCopy;
	}

	/** If true (the default), {@link #reset()} is called automatically after an entire object graph has been read or written. If
	 * false, {@link #reset()} must be called manually, which allows unregistered class names, references, and other information to
	 * span multiple object graphs. */
	public void setAutoReset (boolean autoReset) {
		this.autoReset = autoReset;
	}

	/** Sets the maximum depth of an object graph. This can be used to prevent malicious data from causing a stack overflow. It
	 * also limits the work that deeply nested malicious data can cause during deserialization, which for some object graphs grows
	 * exponentially with the depth. Default is {@link Integer#MAX_VALUE}. */
	public void setMaxDepth (int maxDepth) {
		if (maxDepth <= 0) throw new IllegalArgumentException("maxDepth must be > 0.");
		this.maxDepth = maxDepth;
	}

	/** Returns true if the specified type is final. Final types can be serialized more efficiently because they are
	 * non-polymorphic.
	 * <p>
	 * This can be overridden to force non-final classes to be treated as final. Eg, if an application uses ArrayList extensively
	 * but never uses an ArrayList subclass, treating ArrayList as final could allow FieldSerializer to save 1-2 bytes per
	 * ArrayList field. */
	public boolean isFinal (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (type.isArray()) type = Util.getElementClass(type);
		if (enumsFinal && type.isEnum()) return true; // Also with constant bodies, which are subclasses.
		return Modifier.isFinal(type.getModifiers());
	}

	/** If true, {@link #isFinal(Class)} returns true for enums, also if their constants have bodies, which are subclasses. Then
	 * the class of an enum value is not written if the enum is known, eg from the type of a field, also if a constant has a body,
	 * so adding or removing bodies doesn't change the serialized bytes. If false, the class is written for enums with constant
	 * bodies, like in Kryo 5. This must be set before classes are registered. Default is true.
	 * @deprecated Only needed to read data written by Kryo 5, see {@link Kryo5Compatibility}. Will be removed in Kryo 7. */
	@Deprecated
	public void setEnumsFinal (boolean enumsFinal) {
		this.enumsFinal = enumsFinal;
	}

	/** @deprecated See {@link #setEnumsFinal(boolean)}. */
	@Deprecated
	public boolean getEnumsFinal () {
		return enumsFinal;
	}

	/** Returns true if the specified type is a closure. When true, Kryo uses {@link Closure} instead of the specified type to find
	 * the class {@link Registration}.
	 * <p>
	 * This can be overridden to support alternative closure implementations. The default implementation returns true if the
	 * specified type is synthetic and the type's name contains '/' (to detect a Java 8+ closure). */
	public boolean isClosure (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		return type.isSynthetic() && type.getName().indexOf('/') >= 0;
	}

	/** Returns true if the specified type is a proxy. When true, Kryo uses {@link InvocationHandler} instead of the specified type
	 * to find the class {@link Registration}.
	 * <p>
	 * This can be overridden to support alternative proxy checks. The default implementation delegates to
	 * {@link Proxy#isProxyClass(Class)}. */
	public boolean isProxy (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		return Proxy.isProxyClass(type);
	}

	/** Tracks the generic type arguments and actual classes for type variables in the object graph during serialization.
	 * <p>
	 * When serializing a type with a single type parameter, {@link Generics#nextGenericClass() nextGenericClass} will return the
	 * generic class (or null) and must be followed by {@link Generics#popGenericType() popGenericType}. See
	 * {@link CollectionSerializer} for an example.
	 * <p>
	 * When serializing a type with multiple type parameters, {@link Generics#nextGenericTypes() nextGenericTypes} will return an
	 * array of {@link GenericType}, then for each of those {@link GenericType#resolve(Generics) resolve} returns the generic
	 * class. This must be followed by {@link Generics#popGenericType() popGenericType}. See {@link MapSerializer} for an example.
	 * <p>
	 * {@link GenericsHierarchy} stores the type parameters for a class.
	 * {@link Generics#pushTypeVariables(GenericsHierarchy, GenericType) pushTypeVariables} can be called before generic types are
	 * {@link GenericType#resolve(Generics) resolved} so the type parameters are tracked as serialization moves through the object
	 * graph. If {@code > 0} is returned, this must be followed by {@link Generics#popTypeVariables(int) popTypeVariables}. See
	 * {@link FieldSerializer} for an example. */
	public Generics getGenerics () {
		return generics;
	}

	/** If true (the default), Kryo attempts to use generic type information to optimize the serialized size. If an object's
	 * generic type can be inferred, serializers do not need to write the object's class.
	 * <p>
	 * Disabling generics optimization can increase performance at the cost of a larger serialized size.
	 * <p>
	 * Note that this setting affects the (de)serialization stream, i.e. the serializer and the deserializer need to use the same
	 * setting in order to be compatible.
	 * @param optimizedGenerics whether to optimize generics (default is true) */
	public void setOptimizedGenerics (boolean optimizedGenerics) {
		generics = optimizedGenerics ? new DefaultGenerics(this) : NoGenerics.INSTANCE;
	}

	static final class DefaultSerializerEntry {
		final Class type;
		final SerializerFactory serializerFactory;

		DefaultSerializerEntry (Class type, SerializerFactory serializerFactory) {
			this.type = type;
			this.serializerFactory = serializerFactory;
		}
	}
}
