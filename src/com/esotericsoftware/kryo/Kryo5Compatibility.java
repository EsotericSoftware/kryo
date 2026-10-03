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

import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
 * RecordSerializer like in Kryo 5, which is slower than FieldSerializer.
 * <p>
 * For the types that have new default serializers in Kryo 6, the serializers Kryo 5 used by default are configured. If the
 * serializers for these types that were already available in Kryo 5, eg UUIDSerializer, were registered with Kryo 5, they must
 * still be registered. */
public final class Kryo5Compatibility {
	private Kryo5Compatibility () {
	}

	public static void configure (Kryo kryo) {
		// Kryo 5 serialized records with RecordSerializer. Android has records only since API level 34.
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
			AtomicLong.class, AtomicReference.class})
			kryo.addDefaultSerializer(type, defaultSerializer);

		// Kryo 5 wrote the class of each map key and value. The factories of the default serializers are wrapped, so the more
		// specific default serializers keep their priority.
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

		// Android has the immutable collections only since API level 30.
		if (!isAndroid || isClassAvailable("java.util.ImmutableCollections")) {
			// Registered serializers of immutable maps, the default serializers are configured above.
			Registration registration = kryo.getClassResolver().getRegistration(Map.of().getClass());
			if (registration != null) ((MapSerializer)registration.getSerializer()).setWriteSameClassOnce(false);
			// Kryo 5 didn't support null elements in immutable lists.
			registration = kryo.getClassResolver().getRegistration(List.of().getClass());
			Serializer listSerializer = registration != null ? registration.getSerializer()
				: kryo.getDefaultSerializer(List.of().getClass());
			((CollectionSerializer)listSerializer).setElementsCanBeNull(false);
		}

		// Kryo 5 used the generic types of fields with CompatibleFieldSerializer and TaggedFieldSerializer. A default serializer
		// set as a class is replaced by the equivalent factory, which has the setting.
		if (kryo.defaultSerializer instanceof ReflectionSerializerFactory factory) {
			if (factory.serializerClass == CompatibleFieldSerializer.class)
				kryo.defaultSerializer = new CompatibleFieldSerializerFactory();
			else if (factory.serializerClass == TaggedFieldSerializer.class) //
				kryo.defaultSerializer = new TaggedFieldSerializerFactory();
		}
		if (kryo.defaultSerializer instanceof CompatibleFieldSerializerFactory factory)
			factory.getConfig().setOptimizeGenerics(true);
		else if (kryo.defaultSerializer instanceof TaggedFieldSerializerFactory factory) //
			factory.getConfig().setOptimizeGenerics(true);
	}
}
