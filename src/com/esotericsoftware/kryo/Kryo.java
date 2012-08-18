
package com.esotericsoftware.kryo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.InstantiatorStrategy;
import org.objenesis.strategy.SerializingInstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
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
import com.esotericsoftware.kryo.serializers.DefaultSerializers.BigDecimalSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.BigIntegerSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.BooleanSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ByteSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CalendarSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CharSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ClassSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CollectionsEmptyListSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CollectionsEmptyMapSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CollectionsEmptySetSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CollectionsSingletonListSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CollectionsSingletonMapSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CollectionsSingletonSetSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CurrencySerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.DateSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.DoubleSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.EnumSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.EnumSetSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.FloatSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.IntSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.KryoSerializableSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.LongSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ShortSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringBufferSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringBuilderSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.TimeZoneSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.TreeMapSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializer;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import com.esotericsoftware.kryo.util.IdentityMap;
import com.esotericsoftware.kryo.util.IntArray;
import com.esotericsoftware.kryo.util.MapReferenceResolver;
import com.esotericsoftware.kryo.util.ObjectMap;
import com.esotericsoftware.kryo.util.Util;
import com.esotericsoftware.reflectasm.ConstructorAccess;

import static com.esotericsoftware.kryo.util.Util.*;
import static com.esotericsoftware.minlog.Log.*;

/** Maps classes to serializers so object graphs can be serialized automatically.
 * @author Nathan Sweet <misc@n4te.com> */
public class Kryo {
	static public final byte NULL = 0;
	static public final byte NOT_NULL = 1;

	static private final int REF = -1;
	static private final int NO_REF = -2;

	private Class<? extends Serializer> defaultSerializer = FieldSerializer.class;
	private final ArrayList<DefaultSerializerEntry> defaultSerializers = new ArrayList(32);
	private final int lowPriorityDefaultSerializerCount;

	private final ClassResolver classResolver;
	private int nextRegisterID;
	private Class memoizedClass;
	private Registration memoizedClassValue;
	private ClassLoader classLoader = getClass().getClassLoader();
	private InstantiatorStrategy strategy;
	private boolean registrationRequired;

	private int depth, maxDepth = Integer.MAX_VALUE;
	private boolean autoReset = true;
	private volatile Thread thread;
	private ObjectMap context, graphContext;

	private ReferenceResolver referenceResolver;
	private final IntArray readReferenceIds = new IntArray(0);
	private boolean references;
	private Object readObject;

	private int copyDepth;
	private boolean copyShallow;
	private IdentityMap originalToCopy;
	private Object needsCopyReference;

	/** Creates a new Kryo with a {@link DefaultClassResolver} and a {@link MapReferenceResolver}. */
	public Kryo () {
		this(new DefaultClassResolver(), new MapReferenceResolver());
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

		addDefaultSerializer(byte[].class, ByteArraySerializer.class);
		addDefaultSerializer(char[].class, CharArraySerializer.class);
		addDefaultSerializer(short[].class, ShortArraySerializer.class);
		addDefaultSerializer(int[].class, IntArraySerializer.class);
		addDefaultSerializer(long[].class, LongArraySerializer.class);
		addDefaultSerializer(float[].class, FloatArraySerializer.class);
		addDefaultSerializer(double[].class, DoubleArraySerializer.class);
		addDefaultSerializer(boolean[].class, BooleanArraySerializer.class);
		addDefaultSerializer(String[].class, StringArraySerializer.class);
		addDefaultSerializer(Object[].class, ObjectArraySerializer.class);
		addDefaultSerializer(BigInteger.class, BigIntegerSerializer.class);
		addDefaultSerializer(BigDecimal.class, BigDecimalSerializer.class);
		addDefaultSerializer(Class.class, ClassSerializer.class);
		addDefaultSerializer(Date.class, DateSerializer.class);
		addDefaultSerializer(Enum.class, EnumSerializer.class);
		addDefaultSerializer(EnumSet.class, EnumSetSerializer.class);
		addDefaultSerializer(Currency.class, CurrencySerializer.class);
		addDefaultSerializer(StringBuffer.class, StringBufferSerializer.class);
		addDefaultSerializer(StringBuilder.class, StringBuilderSerializer.class);
		addDefaultSerializer(Collections.EMPTY_LIST.getClass(), CollectionsEmptyListSerializer.class);
		addDefaultSerializer(Collections.EMPTY_MAP.getClass(), CollectionsEmptyMapSerializer.class);
		addDefaultSerializer(Collections.EMPTY_SET.getClass(), CollectionsEmptySetSerializer.class);
		addDefaultSerializer(Collections.singletonList(null).getClass(), CollectionsSingletonListSerializer.class);
		addDefaultSerializer(Collections.singletonMap(null, null).getClass(), CollectionsSingletonMapSerializer.class);
		addDefaultSerializer(Collections.singleton(null).getClass(), CollectionsSingletonSetSerializer.class);
		addDefaultSerializer(Collection.class, CollectionSerializer.class);
		addDefaultSerializer(TreeMap.class, TreeMapSerializer.class);
		addDefaultSerializer(Map.class, MapSerializer.class);
		addDefaultSerializer(KryoSerializable.class, KryoSerializableSerializer.class);
		addDefaultSerializer(TimeZone.class, TimeZoneSerializer.class);
		addDefaultSerializer(Calendar.class, CalendarSerializer.class);
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

	/** Sets the serailzer to use when no {@link #addDefaultSerializer(Class, Class) default serializers} match an object's type.
	 * Default is {@link FieldSerializer}.
	 * @see #newDefaultSerializer(Class) */
	public void setDefaultSerializer (Class<? extends Serializer> serializer) {
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		defaultSerializer = serializer;
	}

	/** Instances of the specified class will use the specified serializer.
	 * @see #setDefaultSerializer(Class) */
	public void addDefaultSerializer (Class type, Serializer serializer) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		DefaultSerializerEntry entry = new DefaultSerializerEntry();
		entry.type = type;
		entry.serializer = serializer;
		defaultSerializers.add(defaultSerializers.size() - lowPriorityDefaultSerializerCount, entry);
	}

	/** Instances of the specified class will use the specified serializer. Serializer instances are created as needed via
	 * {@link #newSerializer(Class, Class)}. By default, the following classes have a default serializer set:
	 * <p>
	 * <table>
	 * <tr>
	 * <td>boolean</td>
	 * <td>Boolean</td>
	 * <td>byte</td>
	 * <td>Byte</td>
	 * <td>char</td>
	 * <tr>
	 * </tr>
	 * <td>Character</td>
	 * <td>short</td>
	 * <td>Short</td>
	 * <td>int</td>
	 * <td>Integer</td>
	 * <tr>
	 * </tr>
	 * <td>long</td>
	 * <td>Long</td>
	 * <td>float</td>
	 * <td>Float</td>
	 * <td>double</td>
	 * <tr>
	 * </tr>
	 * <td>Double</td>
	 * <td>String</td>
	 * <td>byte[]</td>
	 * <td>char[]</td>
	 * <td>short[]</td>
	 * <tr>
	 * </tr>
	 * <td>int[]</td>
	 * <td>long[]</td>
	 * <td>float[]</td>
	 * <td>double[]</td>
	 * <td>String[]</td>
	 * <tr>
	 * </tr>
	 * <td>Object[]</td>
	 * <td>Map</td>
	 * <td>BigInteger</td>
	 * <td>BigDecimal</td>
	 * <td>KryoSerializable</td>
	 * </tr>
	 * <tr>
	 * <td>Collection</td>
	 * <td>Date</td>
	 * <td>Collections.emptyList</td>
	 * <td>Collections.singleton</td>
	 * <td>Currency</td>
	 * </tr>
	 * <tr>
	 * <td>StringBuilder</td>
	 * <td>Enum</td>
	 * <td>Collections.emptyMap</td>
	 * <td>Collections.emptySet</td>
	 * <td>Calendar</td>
	 * </tr>
	 * <tr>
	 * <td>StringBuffer</td>
	 * <td>Class</td>
	 * <td>Collections.singletonList</td>
	 * <td>Collections.singletonMap</td>
	 * <td>TimeZone</td>
	 * </tr>
	 * <tr>
	 * <td>TreeMap</td>
	 * <td>EnumSet</td>
	 * </tr>
	 * </table>
	 * <p>
	 * Note that the order default serializers are added is important for a class that may match multiple types. The above default
	 * serializers always have a lower priority than subsequent default serializers that are added. */
	public void addDefaultSerializer (Class type, Class<? extends Serializer> serializerClass) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializerClass == null) throw new IllegalArgumentException("serializerClass cannot be null.");
		DefaultSerializerEntry entry = new DefaultSerializerEntry();
		entry.type = type;
		entry.serializerClass = serializerClass;
		defaultSerializers.add(defaultSerializers.size() - lowPriorityDefaultSerializerCount, entry);
	}

	/** Returns the best matching serializer for a class. This method can be overridden to implement custom logic to choose a
	 * serializer. */
	public Serializer getDefaultSerializer (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");

		if (type.isAnnotationPresent(DefaultSerializer.class))
			return newSerializer(((DefaultSerializer)type.getAnnotation(DefaultSerializer.class)).value(), type);

		for (int i = 0, n = defaultSerializers.size(); i < n; i++) {
			DefaultSerializerEntry entry = defaultSerializers.get(i);
			if (entry.type.isAssignableFrom(type)) {
				if (entry.serializer != null) return entry.serializer;
				return newSerializer(entry.serializerClass, type);
			}
		}

		return newDefaultSerializer(type);
	}

	/** Called by {@link #getDefaultSerializer(Class)} when no default serializers matched the type. Subclasses can override this
	 * method to customize behavior. The default implementation calls {@link #newSerializer(Class, Class)} using the
	 * {@link #setDefaultSerializer(Class) default serializer}. */
	protected Serializer newDefaultSerializer (Class type) {
		return newSerializer(defaultSerializer, type);
	}

	/** Creates a new instance of the specified serializer for serializing the specified class. Serializers must have a zero
	 * argument constructor or one that takes (Kryo), (Class), or (Kryo, Class). */
	public Serializer newSerializer (Class<? extends Serializer> serializerClass, Class type) {
		try {
			try {
				return serializerClass.getConstructor(Kryo.class, Class.class).newInstance(this, type);
			} catch (NoSuchMethodException ex1) {
				try {
					return serializerClass.getConstructor(Kryo.class).newInstance(this);
				} catch (NoSuchMethodException ex2) {
					try {
						return serializerClass.getConstructor(Class.class).newInstance(type);
					} catch (NoSuchMethodException ex3) {
						return serializerClass.newInstance();
					}
				}
			}
		} catch (Exception ex) {
			throw new IllegalArgumentException("Unable to create serializer \"" + serializerClass.getName() + "\" for class: "
				+ className(type), ex);
		}
	}

	// --- Registration ---

	/** Registers the class using the lowest, next available integer ID and the {@link Kryo#getDefaultSerializer(Class) default
	 * serializer}. If the class is already registered, the existing entry is updated with the new serializer. Registering a
	 * primitive also affects the corresponding primitive wrapper.
	 * <p>
	 * Because the ID assigned is affected by the IDs registered before it, the order classes are registered is important when
	 * using this method. The order must be the same at deserialization as it was for serialization. */
	public Registration register (Class type) {
		Registration registration = classResolver.getRegistration(type);
		if (registration != null) return registration;
		return register(type, getDefaultSerializer(type));
	}

	/** Registers the class using the specified ID and the {@link Kryo#getDefaultSerializer(Class) default serializer}. If the ID is
	 * already in use by the same type, the old entry is overwritten. If the ID is already in use by a different type, a
	 * {@link KryoException} is thrown. Registering a primitive also affects the corresponding primitive wrapper.
	 * <p>
	 * IDs must be the same at deserialization as they were for serialization.
	 * @param id Must be >= 0. Smaller IDs are serialized more efficiently. */
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

	/** Registers the class using the specified ID and serializer. If the ID is already in use by the same type, the old entry is
	 * overwritten. If the ID is already in use by a different type, a {@link KryoException} is thrown. Registering a primitive
	 * also affects the corresponding primitive wrapper.
	 * <p>
	 * IDs must be the same at deserialization as they were for serialization.
	 * @param id Must be >= 0. Smaller IDs are serialized more efficiently. */
	public Registration register (Class type, Serializer serializer, int id) {
		if (id < 0) throw new IllegalArgumentException("id must be >= 0: " + id);
		return register(new Registration(type, serializer, id));
	}

	/** Stores the specified registration. If the ID is already in use by the same type, the old entry is overwritten. If the ID is
	 * already in use by a different type, a {@link KryoException} is thrown. Registering a primitive also affects the
	 * corresponding primitive wrapper.
	 * <p>
	 * IDs must be the same at deserialization as they were for serialization.
	 * <p>
	 * Registration can be suclassed to efficiently store per type information, accessible in serializers via
	 * {@link Kryo#getRegistration(Class)}. */
	public Registration register (Registration registration) {
		int id = registration.getId();
		if (id < 0) throw new IllegalArgumentException("id must be > 0: " + id);

		Registration existing = getRegistration(registration.getId());
		if (existing != null && existing.getType() != registration.getType()) {
			throw new KryoException("An existing registration with a different type already uses ID: " + registration.getId()
				+ "\nExisting registration: " + existing + "\nUnable to set registration: " + registration);
		}

		return classResolver.register(registration);
	}

	/** Returns the lowest, next available integer ID. */
	public int getNextRegistrationId () {
		int id = nextRegisterID;
		while (true) {
			if (classResolver.getRegistration(id) == null) return id;
			id++;
		}
	}

	/** @throws IllegalArgumentException if the class is not registered and {@link Kryo#setRegistrationRequired(boolean)} is true.
	 * @see ClassResolver#getRegistration(Class) */
	public Registration getRegistration (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");

		if (type == memoizedClass) return memoizedClassValue;
		Registration registration = classResolver.getRegistration(type);
		if (registration == null) {
			if (Proxy.isProxyClass(type)) {
				// If a Proxy class, treat it like an InvocationHandler because the concrete class for a proxy is generated.
				registration = getRegistration(InvocationHandler.class);
			} else if (!type.isEnum() && Enum.class.isAssignableFrom(type)) {
				// This handles an enum value that is an inner class. Eg: enum A {b{}};
				registration = getRegistration(type.getEnclosingClass());
			} else if (EnumSet.class.isAssignableFrom(type)) {
				registration = classResolver.getRegistration(EnumSet.class);
			}
			if (registration == null) {
				if (registrationRequired) {
					throw new IllegalArgumentException("Class is not registered: " + className(type)
						+ "\nNote: To register this class use: kryo.register(" + className(type) + ".class);");
				}
				registration = classResolver.registerImplicit(type);
			}
		}
		memoizedClass = type;
		memoizedClassValue = registration;
		return registration;
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
			if (TRACE || (DEBUG && depth == 1)) log("Write", object);
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
			if (TRACE || (DEBUG && depth == 1)) log("Write", object);
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
					if (TRACE || (DEBUG && depth == 1)) log("Write", object);
					output.writeByte(NULL);
					return;
				}
				output.writeByte(NOT_NULL);
			}
			if (TRACE || (DEBUG && depth == 1)) log("Write", object);
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
					if (TRACE || (DEBUG && depth == 1)) log("Write", null);
					output.writeByte(NULL);
					return;
				}
				output.writeByte(NOT_NULL);
			}
			if (TRACE || (DEBUG && depth == 1)) log("Write", object);
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
			if (TRACE || (DEBUG && depth == 1)) log("Write", object);
			registration.getSerializer().write(this, output, object);
		} finally {
			if (--depth == 0 && autoReset) reset();
		}
	}

	/** @param object May be null if mayBeNull is true.
	 * @return true if no bytes need to be written for the object. */
	boolean writeReferenceOrNull (Output output, Object object, boolean mayBeNull) {
		if (object == null) {
			if (TRACE || (DEBUG && depth == 1)) log("Write", null);
			output.writeByte(Kryo.NULL);
			return true;
		}
		if (!referenceResolver.useReferences(object.getClass())) {
			if (mayBeNull) output.writeByte(Kryo.NOT_NULL);
			return false;
		}

		// Determine if this object has already been seen in this object graph.
		int id = referenceResolver.getWrittenId(object);

		// If not the first time encountered, only write reference ID.
		if (id != -1) {
			if (DEBUG) debug("kryo", "Write object reference " + id + ": " + string(object));
			output.writeInt(id + 2, true); // + 2 because 0 and 1 are used for NULL and NOT_NULL.
			return true;
		}

		// Otherwise write NOT_NULL and then the object bytes.
		id = referenceResolver.addWrittenObject(object);
		output.writeByte(NOT_NULL);
		if (TRACE) trace("kryo", "Write initial object reference " + id + ": " + string(object));
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
			if (TRACE || (DEBUG && depth == 1)) log("Read", object);
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
			if (TRACE || (DEBUG && depth == 1)) log("Read", object);
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
					if (TRACE || (DEBUG && depth == 1)) log("Read", null);
					return null;
				}
				object = (T)serializer.read(this, input, type);
			}
			if (TRACE || (DEBUG && depth == 1)) log("Read", object);
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
					if (TRACE || (DEBUG && depth == 1)) log("Read", null);
					return null;
				}
				object = (T)serializer.read(this, input, type);
			}
			if (TRACE || (DEBUG && depth == 1)) log("Read", object);
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
			if (TRACE || (DEBUG && depth == 1)) log("Read", object);
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
			id = input.readInt(true);
			if (id == Kryo.NULL) {
				if (TRACE || (DEBUG && depth == 1)) log("Read", null);
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
			id = input.readInt(true);
		}
		if (id == NOT_NULL) {
			// First time object has been encountered.
			id = referenceResolver.nextReadId(type);
			if (TRACE) trace("kryo", "Read initial object reference " + id + ": " + className(type));
			readReferenceIds.add(id);
			return readReferenceIds.size;
		}
		// The id is an object reference.
		id -= 2; // - 2 because 0 and 1 are used for NULL and NOT_NULL.
		readObject = referenceResolver.getReadObject(type, id);
		if (DEBUG) debug("kryo", "Read object reference " + id + ": " + string(readObject));
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
			if (id != NO_REF) referenceResolver.addReadObject(id, object);
		}
	}

	/** Resets unregistered class names. references to previously serialized or deserialized objects. and the
	 * {@link #getGraphContext() graph context}. If {@link #setAutoReset(boolean) auto reset} is true, this method is called
	 * automatically when an object graph has been completely serialized or deserialized. If overridden, the super method must be
	 * called. */
	public void reset () {
		depth = 0;
		if (graphContext != null) graphContext.clear();
		classResolver.reset();
		if (references) {
			referenceResolver.reset();
			readObject = null;
		}

		copyDepth = 0;
		if (originalToCopy != null) originalToCopy.clear();

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

			needsCopyReference = object;
			Object copy;
			if (object instanceof KryoCopyable)
				copy = ((KryoCopyable)object).copy(this);
			else
				copy = getSerializer(object.getClass()).copy(this, object);
			if (needsCopyReference != null) reference(copy);
			if (TRACE || (DEBUG && copyDepth == 1)) log("Copy", copy);
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

			needsCopyReference = object;
			Object copy;
			if (object instanceof KryoCopyable)
				copy = ((KryoCopyable)object).copy(this);
			else
				copy = serializer.copy(this, object);
			if (needsCopyReference != null) reference(copy);
			if (TRACE || (DEBUG && copyDepth == 1)) log("Copy", copy);
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
		copyShallow = true;
		try {
			if (originalToCopy == null) originalToCopy = new IdentityMap();
			Object existingCopy = originalToCopy.get(object);
			if (existingCopy != null) return (T)existingCopy;

			needsCopyReference = object;
			Object copy;
			if (object instanceof KryoCopyable)
				copy = ((KryoCopyable)object).copy(this);
			else
				copy = getSerializer(object.getClass()).copy(this, object);
			if (needsCopyReference != null) reference(copy);
			if (TRACE || (DEBUG && copyDepth == 1)) log("Shallow copy", copy);
			return (T)copy;
		} finally {
			copyShallow = false;
			if (--copyDepth == 0) reset();
		}
	}

	/** Returns a shallow copy of the object using the specified serializer. Serializers for the classes involved must support
	 * {@link Serializer#copy(Kryo, Object)}.
	 * @param object May be null. */
	public <T> T copyShallow (T object, Serializer serializer) {
		if (object == null) return null;
		copyDepth++;
		copyShallow = true;
		try {
			if (originalToCopy == null) originalToCopy = new IdentityMap();
			Object existingCopy = originalToCopy.get(object);
			if (existingCopy != null) return (T)existingCopy;

			needsCopyReference = object;
			Object copy;
			if (object instanceof KryoCopyable)
				copy = ((KryoCopyable)object).copy(this);
			else
				copy = serializer.copy(this, object);
			if (needsCopyReference != null) reference(copy);
			if (TRACE || (DEBUG && copyDepth == 1)) log("Shallow copy", copy);
			return (T)copy;
		} finally {
			copyShallow = false;
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

	public ReferenceResolver getReferenceResolver () {
		return referenceResolver;
	}

	/** Sets the classloader to resolve unregistered class names to classes. */
	public void setClassLoader (ClassLoader classLoader) {
		if (classLoader == null) throw new IllegalArgumentException("classLoader cannot be null.");
		this.classLoader = classLoader;
	}

	public ClassLoader getClassLoader () {
		return classLoader;
	}

	/** If true, an exception is thrown when an unregistered class is encountered. Default is false.
	 * <p>
	 * If false, when an unregistered class is encountered, its fully qualified class name will be serialized and the
	 * {@link #addDefaultSerializer(Class, Class) default serializer} for the class used to serialize the object. Subsequent
	 * appearances of the class within the same object graph are serialized as an int id.
	 * <p>
	 * Registered classes are serialized as an int id, avoiding the overhead of serializing the class name, but have the drawback
	 * of needing to know the classes to be serialized up front. */
	public void setRegistrationRequired (boolean registrationRequired) {
		this.registrationRequired = registrationRequired;
		if (TRACE) trace("kryo", "Registration required: " + registrationRequired);
	}

	public boolean isRegistrationRequired () {
		return registrationRequired;
	}

	/** If true, each appearance of an object in the graph after the first is stored as an integer ordinal. When set to true,
	 * {@link MapReferenceResolver} is used. This enables references to the same object and cyclic graphs to be serialized, but
	 * typically adds overhead of one byte per object. Default is true. */
	public void setReferences (boolean references) {
		this.references = references;
		if (!references)
			referenceResolver = null;
		else
			referenceResolver = new MapReferenceResolver();
		if (TRACE) trace("kryo", "References: " + references);
	}

	/** Sets the reference resolver and enables references. */
	public void setReferenceResolver (ReferenceResolver referenceResolver) {
		if (referenceResolver == null) throw new IllegalArgumentException("referenceResolver cannot be null.");
		this.references = true;
		this.referenceResolver = referenceResolver;
		if (TRACE) trace("kryo", "Reference resolver: " + referenceResolver.getClass().getName());
	}

	public boolean getReferences () {
		return references;
	}

	/** Sets the strategy used by {@link #newInstantiator(Class)} for creating objects. See {@link StdInstantiatorStrategy} to
	 * create objects via without calling any constructor. See {@link SerializingInstantiatorStrategy} to mimic Java's built-in
	 * serialization.
	 * @param strategy May be null. */
	public void setInstantiatorStrategy (InstantiatorStrategy strategy) {
		this.strategy = strategy;
	}

	/** Returns a new instantiator for creating new instances of the specified type. By default, an instantiator is returned that
	 * uses reflection if the class has a zero argument constructor, an exception is thrown. If a
	 * {@link #setInstantiatorStrategy(InstantiatorStrategy) strategy} is set, it will be used instead of throwing an exception. */
	protected ObjectInstantiator newInstantiator (final Class type) {
		if (!Util.isAndroid) {
			// ReflectASM.
			try {
				final ConstructorAccess access = ConstructorAccess.get(type);
				return new ObjectInstantiator() {
					public Object newInstance () {
						try {
							return access.newInstance();
						} catch (Exception ex) {
							throw new KryoException("Error constructing instance of class: " + className(type), ex);
						}
					}
				};
			} catch (Exception ignored) {
			}
		}
		// Reflection.
		try {
			Constructor ctor;
			try {
				ctor = type.getConstructor((Class[])null);
			} catch (Exception ex) {
				ctor = type.getDeclaredConstructor((Class[])null);
				ctor.setAccessible(true);
			}
			final Constructor constructor = ctor;
			return new ObjectInstantiator() {
				public Object newInstance () {
					try {
						return constructor.newInstance();
					} catch (Exception ex) {
						throw new KryoException("Error constructing instance of class: " + className(type), ex);
					}
				}
			};
		} catch (Exception ignored) {
		}
		if (strategy == null) {
			if (type.isMemberClass() && !Modifier.isStatic(type.getModifiers()))
				throw new KryoException("Class cannot be created (non-static member class): " + className(type));
			else
				throw new KryoException("Class cannot be created (missing no-arg constructor): " + className(type));
		}
		// InstantiatorStrategy.
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
		if (graphContext == null) graphContext = new ObjectMap();
		return graphContext;
	}

	/** Returns the number of child objects away from the object graph root. */
	public int getDepth () {
		return depth;
	}

	/** If true (the default), {@link #reset()} is called automatically after an entire object graph has been read or written. If
	 * false, {@link #reset()} must be called manually, which allows unregistered class names, references, and other information to
	 * span multiple object graphs. */
	public void setAutoReset (boolean autoReset) {
		this.autoReset = autoReset;
	}

	/** Sets the maxiumum depth of an object graph. This can be used to prevent malicious data from causing a stack overflow.
	 * Default is {@link Integer#MAX_VALUE}. */
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
		if (type.isArray()) return Modifier.isFinal(Util.getElementClass(type).getModifiers());
		return Modifier.isFinal(type.getModifiers());
	}

	/** Returns the first level of classes or interfaces for a generic type.
	 * @return null if the specified type is not generic or its generic types are not classes. */
	static public Class[] getGenerics (Type genericType) {
		if (!(genericType instanceof ParameterizedType)) return null;
		Type[] actualTypes = ((ParameterizedType)genericType).getActualTypeArguments();
		Class[] generics = new Class[actualTypes.length];
		int count = 0;
		for (int i = 0, n = actualTypes.length; i < n; i++) {
			Type actualType = actualTypes[i];
			if (actualType instanceof Class)
				generics[i] = (Class)actualType;
			else if (actualType instanceof ParameterizedType)
				generics[i] = (Class)((ParameterizedType)actualType).getRawType();
			else
				continue;
			count++;
		}
		if (count == 0) return null;
		return generics;
	}

	static final class DefaultSerializerEntry {
		Class type;
		Serializer serializer;
		Class<? extends Serializer> serializerClass;
	}
}
