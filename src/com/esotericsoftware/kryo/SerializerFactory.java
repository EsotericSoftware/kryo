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

import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializerConfig;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializerConfig;

/** A serializer factory that allows the creation of serializers. This factory will be called when a {@link Kryo} serializer
 * discovers a new type for which no serializer is yet known. For example, when a factory is registered via
 * {@link Kryo#setDefaultSerializer(SerializerFactory)} a different serializer can be created dependent on the type of a class.
 * @author Rafael Winterhalter <rafael.wth@web.de> */
public interface SerializerFactory {
	/** Creates a new serializer
	 * @param kryo The serializer instance requesting the new serializer.
	 * @param type The type of the object that is to be serialized.
	 * @return An implementation of a serializer that is able to serialize an object of type {@code type}. */
	Serializer newSerializer (Kryo kryo, Class type);

	/** @param factoryClass Must have a constructor that takes a serializer class, or a zero argument constructor.
	 * @param serializerClass May be null if the factory alread knows the serializer class to create. */
	static public <T extends SerializerFactory> T newFactory (Class<T> factoryClass, Class<? extends Serializer> serializerClass) {
		if (serializerClass == Serializer.class) serializerClass = null; // Happens if not set in an annotation.
		try {
			if (serializerClass != null) {
				try {
					return factoryClass.getConstructor(Class.class).newInstance(serializerClass);
				} catch (NoSuchMethodException ex) {
				}
			}
			return factoryClass.newInstance();
		} catch (Exception ex) {
			if (serializerClass == null)
				throw new IllegalArgumentException("Unable to create serializer factory: " + factoryClass.getName(), ex);
			else {
				throw new IllegalArgumentException("Unable to create serializer factory \"" + factoryClass.getName()
					+ "\" for serializer class: " + className(serializerClass), ex);
			}
		}
	}

	/** This factory instantiates new serializers of a given class via reflection. The constructors of the given
	 * {@code serializerClass} must either take an instance of {@link Kryo} and an instance of {@link Class} as its parameter, take
	 * only a {@link Kryo} or {@link Class} as its only argument or take no arguments. If several of the described constructors are
	 * found, the first found constructor is used, in the order as they were just described.
	 * @author Rafael Winterhalter <rafael.wth@web.de> */
	static public class ReflectionSerializerFactory implements SerializerFactory {
		private final Class<? extends Serializer> serializerClass;

		public ReflectionSerializerFactory (Class<? extends Serializer> serializerClass) {
			this.serializerClass = serializerClass;
		}

		public Serializer newSerializer (Kryo kryo, Class type) {
			return newSerializer(kryo, serializerClass, type);
		}

		/** Creates a new instance of the specified serializer for serializing the specified class. Serializers must have a zero
		 * argument constructor or one that takes (Kryo), (Class), or (Kryo, Class). */
		static public Serializer newSerializer (Kryo kryo, Class<? extends Serializer> serializerClass, Class type) {
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
	 * be used when multiple types should be serialized by the same serializer. This also allows serializers to be shared among
	 * different {@link Kryo} instances.
	 * @author Rafael Winterhalter <rafael.wth@web.de> */
	static public class SingletonSerializerFactory implements SerializerFactory {
		private final Serializer serializer;

		public SingletonSerializerFactory (Serializer serializer) {
			this.serializer = serializer;
		}

		public Serializer newSerializer (Kryo kryo, Class type) {
			return serializer;
		}
	}

	/** A serializer factory that returns new, configured {@link FieldSerializer} instances.
	 * @author Nathan Sweet */
	static public class FieldSerializerFactory implements SerializerFactory {
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

		public Serializer newSerializer (Kryo kryo, Class type) {
			return new FieldSerializer(kryo, type, config.clone());
		}
	}

	/** A serializer factory that returns new, configured {@link TaggedFieldSerializer} instances.
	 * @author Nathan Sweet */
	static public class TaggedFieldSerializerFactory implements SerializerFactory {
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

		public Serializer newSerializer (Kryo kryo, Class type) {
			return new TaggedFieldSerializer(kryo, type, config.clone());
		}
	}
}
