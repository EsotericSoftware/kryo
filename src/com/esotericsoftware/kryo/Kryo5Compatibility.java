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

import com.esotericsoftware.kryo.Kryo.DefaultSerializerEntry;
import com.esotericsoftware.kryo.SerializerFactory.BaseSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.CompatibleFieldSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.ReflectionSerializerFactory;
import com.esotericsoftware.kryo.SerializerFactory.TaggedFieldSerializerFactory;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.DateSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializer;
import com.esotericsoftware.kryo.serializers.RecordSerializer;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer;
import com.esotericsoftware.kryo.util.HashMapReferenceResolver;
import com.esotericsoftware.kryo.util.ListReferenceResolver;
import com.esotericsoftware.kryo.util.MapReferenceResolver;
import com.esotericsoftware.kryo.util.Null;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/** Configures Kryo to read data written by Kryo 5, see MIGRATION.md. Data written with this configuration can be read by Kryo 5,
 * unless it contains locales with a script or generic types that Kryo 6 resolves but Kryo 5 ignored.
 * <p>
 * Call {@link #configure(Kryo)} once, after {@link Kryo#setDefaultSerializer(SerializerFactory) setting the default serializer}.
 * It changes the default serializers, so serializers that are registered explicitly or added as default serializers later need
 * the Kryo 5 settings themselves, eg {@link MapSerializer#setWriteSameClassOnce(boolean)}. Records are serialized with
 * RecordSerializer like in Kryo 5, which is slower than FieldSerializer. Strings use references like in Kryo 5 with Kryo's
 * reference resolvers, which are replaced. Subclasses of them and custom reference resolvers decide in
 * {@link ReferenceResolver#useReferences(Class)}.
 * <p>
 * For the types that have new default serializers in Kryo 6, the serializers Kryo 5 used by default are configured. If the
 * serializers for these types that were already available in Kryo 5, eg UUIDSerializer, were registered with Kryo 5, they must
 * still be registered. */
public final class Kryo5Compatibility {
	private Kryo5Compatibility () {
	}

	/** Configures a Kryo instance to read and write the format of Kryo 5, see the class documentation. */
	public static void configure (Kryo kryo) {
		restoreEnumClasses(kryo);
		restoreStringReferences(kryo);
		restoreDefaultSerializers(kryo);
		restoreCollectionFormats(kryo);
		restoreFieldSerializerSettings(kryo);
	}

	/** Kryo 5 wrote the class of enums with constant bodies, because they are not final. */
	@SuppressWarnings("deprecation")
	private static void restoreEnumClasses (Kryo kryo) {
		kryo.setEnumsFinal(false);
	}

	/** Kryo 5 used references for strings. Kryo's reference resolvers are replaced by ones that do, without changing whether
	 * references are enabled. Without a reference resolver, {@link Kryo#setReferences(boolean)} uses the one set here. Subclasses
	 * and custom reference resolvers decide themselves. Throws if references are enabled and a String field was already created,
	 * see {@link Kryo#setReferences(boolean)}. */
	private static void restoreStringReferences (Kryo kryo) {
		ReferenceResolver referenceResolver = kryo.referenceResolver, kryo5Resolver = null;
		Class resolverClass = referenceResolver == null ? null : referenceResolver.getClass();
		if (referenceResolver == null) {
			kryo5Resolver = new MapReferenceResolver() {
				public boolean useReferences (Class type) {
					return kryo5UseReferences(type);
				}
			};
		} else if (resolverClass == MapReferenceResolver.class) {
			kryo5Resolver = new MapReferenceResolver(((MapReferenceResolver)referenceResolver).getMaximumCapacity()) {
				public boolean useReferences (Class type) {
					return kryo5UseReferences(type);
				}
			};
		} else if (resolverClass == ListReferenceResolver.class) {
			kryo5Resolver = new ListReferenceResolver() {
				public boolean useReferences (Class type) {
					return kryo5UseReferences(type);
				}
			};
		} else if (resolverClass == HashMapReferenceResolver.class) {
			kryo5Resolver = new HashMapReferenceResolver() {
				public boolean useReferences (Class type) {
					return kryo5UseReferences(type);
				}
			};
		}
		if (kryo5Resolver != null) {
			kryo5Resolver.setKryo(kryo);
			kryo.checkStringReferences(kryo.getReferences(), kryo5Resolver);
			kryo.referenceResolver = kryo5Resolver;
		}
	}

	/** The types that have new default serializers in Kryo 6 get the serializers that Kryo 5 used by default: records are
	 * serialized with RecordSerializer, the types that had no default serializer in Kryo 5 with FieldSerializer, and the queues
	 * and sets with a comparator or capacity with CollectionSerializer, which loses them. */
	@SuppressWarnings("deprecation")
	private static void restoreDefaultSerializers (Kryo kryo) {
		// Android has records only since API level 34.
		if (!isAndroid || isClassAvailable("java.lang.Record")) {
			kryo.addDefaultSerializer(Record.class, new BaseSerializerFactory() {
				public Serializer newSerializer (Kryo kryo, Class type) {
					return new RecordSerializer(type);
				}
			});
		}

		// Kryo 5 had no default serializers for these types. ConcurrentHashMap.KeySetView is not needed: Kryo 5 wrote it with
		// CollectionSerializer, but couldn't read it back.
		try {
			kryo.addDefaultSerializer(Timestamp.class, DateSerializer::new);
		} catch (NoClassDefFoundError ignored) { // java.sql is not available in a named module that doesn't require it.
		}
		BaseSerializerFactory defaultSerializer = new BaseSerializerFactory() {
			public Serializer newSerializer (Kryo kryo, Class type) {
				return kryo.newDefaultSerializer(type);
			}
		};
		for (Class type : new Class[] {URI.class, UUID.class, Pattern.class, AtomicBoolean.class, AtomicInteger.class,
			AtomicLong.class, AtomicReference.class, File.class, InetAddress.class, InetSocketAddress.class, ByteBuffer.class})
			kryo.addDefaultSerializer(type, defaultSerializer);
		// Kryo 5 wrote these with CollectionSerializer, without the comparator.
		kryo.addDefaultSerializer(ConcurrentSkipListSet.class, CollectionSerializer::new);
		kryo.addDefaultSerializer(PriorityBlockingQueue.class, CollectionSerializer::new);
		// Kryo 5 wrote these with CollectionSerializer, without the capacity. It couldn't read ArrayBlockingQueue.
		kryo.addDefaultSerializer(LinkedBlockingQueue.class, CollectionSerializer::new);
		kryo.addDefaultSerializer(LinkedBlockingDeque.class, CollectionSerializer::new);
	}

	/** Kryo 5 wrote the class of each map key and value, and didn't support null elements in immutable lists. The default
	 * serializers and the registered serializers of the immutable collections are configured for that. */
	private static void restoreCollectionFormats (Kryo kryo) {
		// The factories of the default serializers are wrapped, so the more specific default serializers keep their priority.
		ArrayList<DefaultSerializerEntry> defaultSerializers = kryo.defaultSerializers;
		for (int i = 0, n = defaultSerializers.size(); i < n; i++) {
			DefaultSerializerEntry entry = defaultSerializers.get(i);
			SerializerFactory factory = entry.serializerFactory;
			defaultSerializers.set(i, new DefaultSerializerEntry(entry.type, new SerializerFactory() {
				public Serializer newSerializer (Kryo kryo, Class type) {
					Serializer serializer = factory.newSerializer(kryo, type);
					if (serializer instanceof MapSerializer mapSerializer) mapSerializer.setWriteSameClassOnce(false);
					return serializer;
				}

				public boolean isSupported (Class type) {
					return factory.isSupported(type);
				}
			}));
		}

		// Android has the immutable collections only since API level 30. By name, because D8 replaces List.of and Map.of with
		// unmodifiable collections below API level 30.
		Class mapN = classForName("java.util.ImmutableCollections$MapN");
		if (mapN != null) {
			// Registered serializers of immutable maps, the default serializers are configured above.
			Registration registration = kryo.getClassResolver().getRegistration(mapN);
			if (registration != null) ((MapSerializer)registration.getSerializer()).setWriteSameClassOnce(false);
		}
		Class listN = classForName("java.util.ImmutableCollections$ListN");
		if (listN != null) {
			// Kryo 5 didn't support null elements in immutable lists.
			Registration registration = kryo.getClassResolver().getRegistration(listN);
			Serializer listSerializer = registration != null ? registration.getSerializer() : kryo.getDefaultSerializer(listN);
			((CollectionSerializer)listSerializer).setElementsCanBeNull(false);
		}
	}

	static private @Null Class classForName (String name) {
		try {
			return Class.forName(name);
		} catch (ClassNotFoundException ex) {
			return null;
		}
	}

	/** Kryo 5 used the generic types of fields with CompatibleFieldSerializer and TaggedFieldSerializer, and wrote fields with
	 * chunked encoding in chunks. The default serializer is configured for that; a default serializer set as a class is replaced
	 * by the equivalent factory, which has the settings. Serializers that are registered explicitly need these settings
	 * themselves. */
	@SuppressWarnings("deprecation")
	private static void restoreFieldSerializerSettings (Kryo kryo) {
		if (kryo.defaultSerializer instanceof ReflectionSerializerFactory factory) {
			if (factory.serializerClass == CompatibleFieldSerializer.class)
				kryo.defaultSerializer = new CompatibleFieldSerializerFactory();
			else if (factory.serializerClass == TaggedFieldSerializer.class)
				kryo.defaultSerializer = new TaggedFieldSerializerFactory();
		}
		if (kryo.defaultSerializer instanceof CompatibleFieldSerializerFactory factory) {
			factory.getConfig().setOptimizeGenerics(true);
			factory.getConfig().setLegacyChunks(true);
		} else if (kryo.defaultSerializer instanceof TaggedFieldSerializerFactory factory) {
			factory.getConfig().setOptimizeGenerics(true);
			factory.getConfig().setLegacyChunks(true);
		}
	}

	/** Kryo 5 used references for all types except primitive wrappers and enums, also for strings. */
	private static boolean kryo5UseReferences (Class type) {
		return !isWrapperClass(type) && !isEnum(type);
	}
}
