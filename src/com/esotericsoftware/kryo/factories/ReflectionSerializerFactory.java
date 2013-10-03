package com.esotericsoftware.kryo.factories;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

import static com.esotericsoftware.kryo.util.Util.className;

/**
 * This factory instantiates new serializers of a given class via reflection. The constructors of the given {@code serializerClass}
 * must either take an instance of {@link Kryo} and an instance of {@link Class} as its parameter, take only a {@link Kryo} or {@link Class}
 * as its only argument or take no arguments. If several of the described constructors are found, the first found constructor is used,
 * in the order as they were just described.
 *
 * @author Rafael Winterhalter <rafael.wth@web.de>
 */
public class ReflectionSerializerFactory implements SerializerFactory {

	private final Class<? extends Serializer> serializerClass;

	public ReflectionSerializerFactory (Class<? extends Serializer> serializerClass) {
		this.serializerClass = serializerClass;
	}

	@Override
	public Serializer makeSerializer (Kryo kryo, Class<?> type) {
		return makeSerializer(kryo, serializerClass, type);
	}

	/** Creates a new instance of the specified serializer for serializing the specified class. Serializers must have a zero
	 * argument constructor or one that takes (Kryo), (Class), or (Kryo, Class).
	*/
	public static Serializer makeSerializer (Kryo kryo, Class<? extends Serializer> serializerClass, Class<?> type) {
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
			throw new IllegalArgumentException("Unable to create serializer \"" + serializerClass.getName() + "\" for class: " + className(type), ex);
		}

	}
}
