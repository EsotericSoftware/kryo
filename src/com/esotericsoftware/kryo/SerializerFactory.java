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

package com.esotericsoftware.kryo;

import static com.esotericsoftware.kryo.util.Util.*;

import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer.CompatibleFieldSerializerConfig;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer.FieldSerializerConfig;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.TaggedFieldSerializerConfig;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer.VersionFieldSerializerConfig;

/** Creates and configures serializers.
 * @author Rafael Winterhalter <rafael.wth@web.de> */
public interface SerializerFactory<T extends Serializer> {
	/** Creates and configures a new serializer.
	 * @param kryo The Kryo instance that will be used with the new serializer.
	 * @param type The type of the object that the serializer will serialize. */
	public T newSerializer (Kryo kryo, Class type);

	/** Returns true if this factory can create a serializer for the specified type. */
	public boolean isSupported (Class type);

	/** A serializer factory which always returns true for {@link #isSupported(Class)}. */
	static public abstract class BaseSerializerFactory<T extends Serializer> implements SerializerFactory<T> {
		public boolean isSupported (Class type) {
			return true;
		}
	}

	/** This factory instantiates new serializers of a given class via reflection. The constructors of the given serializer class
	 * must either take an instance of {@link Kryo} and an instance of {@link Class} as its parameter, take only a {@link Kryo} or
	 * {@link Class} as its only argument, or take no arguments. If several of the described constructors are found, the first
	 * found constructor is used, in the order they were just described.
	 * @author Rafael Winterhalter <rafael.wth@web.de> */
	static public class ReflectionSerializerFactory<T extends Serializer> extends BaseSerializerFactory<T> {
		private final Class<T> serializerClass;

		public ReflectionSerializerFactory (Class<T> serializerClass) {
			this.serializerClass = serializerClass;
		}

		public T newSerializer (Kryo kryo, Class type) {
			return newSerializer(kryo, serializerClass, type);
		}

		/** Creates a new instance of the specified serializer for serializing the specified class. Serializers must have a zero
		 * argument constructor or one that takes (Kryo), (Class), or (Kryo, Class). */
		static public <T extends Serializer> T newSerializer (Kryo kryo, Class<T> serializerClass, Class type) {
			try {
				try {
					return serializerClass.getConstructor(Kryo.class, Class.class).newInstance(kryo, type);
				} catch (NoSuchMethodException ex1) {
					try {
						return serializerClass.getConstructor(Kryo.class).newInstance(kryo);
					} catch (NoSuchMethodException ex2) {
						try {
							return serializerClass.getConstructor(Class.class).newInstance(type);
						} catch (NoSuchMethodException ex3) {
							return serializerClass.newInstance();
						}
					}
				}
			} catch (Exception ex) {
				throw new IllegalArgumentException(
					"Unable to create serializer \"" + serializerClass.getName() + "\" for class: " + className(type), ex);
			}
		}
	}

	/** A serializer factory that always returns a given serializer instance rather than creating new serializer instances. It can
	 * be used when multiple types should be serialized by the same serializer.
	 * @author Rafael Winterhalter <rafael.wth@web.de> */
	static public class SingletonSerializerFactory<T extends Serializer> extends BaseSerializerFactory<T> {
		private final T serializer;

		public SingletonSerializerFactory (T serializer) {
			this.serializer = serializer;
		}

		public T newSerializer (Kryo kryo, Class type) {
			return serializer;
		}
	}

	/** A serializer factory that returns new, configured {@link FieldSerializer} instances.
	 * @author Nathan Sweet */
	static public class FieldSerializerFactory extends BaseSerializerFactory<FieldSerializer> {
		private final FieldSerializerConfig config;

		public FieldSerializerFactory () {
			this.config = new FieldSerializerConfig();
		}

		public FieldSerializerFactory (FieldSerializerConfig config) {
			this.config = config;
		}

		public FieldSerializerConfig getConfig () {
			return config;
		}

		public FieldSerializer newSerializer (Kryo kryo, Class type) {
			return new FieldSerializer(kryo, type, config.clone());
		}
	}

	/** A serializer factory that returns new, configured {@link TaggedFieldSerializer} instances.
	 * @author Nathan Sweet */
	static public class TaggedFieldSerializerFactory extends BaseSerializerFactory<TaggedFieldSerializer> {
		private final TaggedFieldSerializerConfig config;

		public TaggedFieldSerializerFactory () {
			this.config = new TaggedFieldSerializerConfig();
		}

		public TaggedFieldSerializerFactory (TaggedFieldSerializerConfig config) {
			this.config = config;
		}

		public TaggedFieldSerializerConfig getConfig () {
			return config;
		}

		public TaggedFieldSerializer newSerializer (Kryo kryo, Class type) {
			return new TaggedFieldSerializer(kryo, type, config.clone());
		}
	}

	/** A serializer factory that returns new, configured {@link VersionFieldSerializer} instances.
	 * @author Nathan Sweet */
	static public class VersionFieldSerializerFactory extends BaseSerializerFactory<VersionFieldSerializer> {
		private final VersionFieldSerializerConfig config;

		public VersionFieldSerializerFactory () {
			this.config = new VersionFieldSerializerConfig();
		}

		public VersionFieldSerializerFactory (VersionFieldSerializerConfig config) {
			this.config = config;
		}

		public VersionFieldSerializerConfig getConfig () {
			return config;
		}

		public VersionFieldSerializer newSerializer (Kryo kryo, Class type) {
			return new VersionFieldSerializer(kryo, type, config.clone());
		}
	}

	/** A serializer factory that returns new, configured {@link CompatibleFieldSerializer} instances.
	 * @author Nathan Sweet */
	static public class CompatibleFieldSerializerFactory extends BaseSerializerFactory<CompatibleFieldSerializer> {
		private final CompatibleFieldSerializerConfig config;

		public CompatibleFieldSerializerFactory () {
			this.config = new CompatibleFieldSerializerConfig();
		}

		public CompatibleFieldSerializerFactory (CompatibleFieldSerializerConfig config) {
			this.config = config;
		}

		public CompatibleFieldSerializerConfig getConfig () {
			return config;
		}

		public CompatibleFieldSerializer newSerializer (Kryo kryo, Class type) {
			return new CompatibleFieldSerializer(kryo, type, config.clone());
		}
	}
}
