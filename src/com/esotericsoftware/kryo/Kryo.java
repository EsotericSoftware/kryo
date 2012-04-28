
package com.esotericsoftware.kryo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Map;

import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.InstantiatorStrategy;
import org.objenesis.strategy.SerializingInstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.ArraySerializer;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.BigDecimalSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.BigIntegerSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.BooleanSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ByteArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ByteSerializer;
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
import com.esotericsoftware.kryo.serializers.DefaultSerializers.FloatSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.IntSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.KryoSerializableSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.LongSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ShortSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringBufferSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringBuilderSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializer;
import com.esotericsoftware.kryo.util.IdentityMap;
import com.esotericsoftware.kryo.util.IdentityObjectIntMap;
import com.esotericsoftware.kryo.util.IntMap;
import com.esotericsoftware.kryo.util.ObjectMap;
import com.esotericsoftware.reflectasm.ConstructorAccess;

import static com.esotericsoftware.kryo.Util.*;
import static com.esotericsoftware.minlog.Log.*;

/** Maps classes to serializers so object graphs can be serialized automatically.
 * @author Nathan Sweet <misc@n4te.com> */
public class Kryo {
	static public final byte NAME = -1;
	static public final byte NULL = 0;
	static public final byte NOT_NULL = 1;

	private Class<? extends Serializer> defaultSerializer = FieldSerializer.class;
	private final ArrayList<DefaultSerializerEntry> defaultSerializers = new ArrayList(32);
	private int lowPriorityDefaultSerializerCount;
	private ArraySerializer arraySerializer = new ArraySerializer();
	private InstantiatorStrategy strategy;

	private int depth, nextRegisterID;
	private final IntMap<Registration> idToRegistration = new IntMap();
	private final ObjectMap<Class, Registration> classToRegistration = new ObjectMap();
	private Class memoizedType;
	private Registration memoizedRegistration;
	private ObjectMap context, graphContext;

	private boolean registrationRequired;
	private final IdentityObjectIntMap<Class> classToNameId = new IdentityObjectIntMap();
	private final IntMap<Class> nameIdToClass = new IntMap();
	private int nextNameId;
	private ClassLoader classLoader = getClass().getClassLoader();

	private boolean references = true;
	private final InstanceId instanceId = new InstanceId(null, 0);
	private final IdentityObjectIntMap<Class> classToNextInstanceId = new IdentityObjectIntMap();
	private final IdentityObjectIntMap objectToInstanceId = new IdentityObjectIntMap();
	private final ObjectMap<InstanceId, Object> instanceIdToObject = new ObjectMap();

	private boolean copyShallow;
	private IdentityMap originalToCopy;

	public Kryo () {
		addDefaultSerializer(byte[].class, ByteArraySerializer.class);
		addDefaultSerializer(BigInteger.class, BigIntegerSerializer.class);
		addDefaultSerializer(BigDecimal.class, BigDecimalSerializer.class);
		addDefaultSerializer(Class.class, ClassSerializer.class);
		addDefaultSerializer(Date.class, DateSerializer.class);
		addDefaultSerializer(Enum.class, EnumSerializer.class);
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
		addDefaultSerializer(Map.class, MapSerializer.class);
		addDefaultSerializer(KryoSerializable.class, KryoSerializableSerializer.class);
		lowPriorityDefaultSerializerCount = defaultSerializers.size();

		// Primitives and string. Primitive wrappers automatically use the same registration as primitives.
		register(boolean.class, new BooleanSerializer());
		register(byte.class, new ByteSerializer());
		register(char.class, new CharSerializer());
		register(short.class, new ShortSerializer());
		register(int.class, new IntSerializer());
		register(long.class, new LongSerializer());
		register(float.class, new FloatSerializer());
		register(double.class, new DoubleSerializer());
		register(String.class, new StringSerializer());
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
	 * <td>byte[]</td>
	 * <td>String</td>
	 * <td>BigInteger</td>
	 * <td>BigDecimal</td>
	 * </tr>
	 * <tr>
	 * <td>Collection</td>
	 * <td>Date</td>
	 * <td>Collections.emptyList</td>
	 * <td>Collections.singleton</td>
	 * <td>Map</td>
	 * </tr>
	 * <tr>
	 * <td>StringBuilder</td>
	 * <td>Enum</td>
	 * <td>Collections.emptyMap</td>
	 * <td>Collections.emptySet</td>
	 * <td>KryoSerializable</td>
	 * </tr>
	 * <tr>
	 * <td>StringBuffer</td>
	 * <td>Class</td>
	 * <td>Collections.singletonList</td>
	 * <td>Collections.singletonMap</td>
	 * <td>Currency</td>
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

		if (type.isArray()) return arraySerializer;

		return newDefaultSerializer(type);
	}

	/** Called by {@link #getDefaultSerializer(Class)} when no default serializers matched the type. Subclasses can override this
	 * method to customize behavior. The default implementation calls {@link #newSerializer(Class, Class)} using the
	 * {@link #setDefaultSerializer(Class) default serializer}. */
	protected Serializer newDefaultSerializer (Class type) {
		return newSerializer(defaultSerializer, type);
	}

	/** Creates a new instance of the specified serializer for serializing the specified class. Serializers */
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

	/** Registers the class using the next available, lowest integer ID and the {@link #getDefaultSerializer(Class) default
	 * serializer}. If the class is already registered, the existing entry is updated with the new serializer. Because the ID
	 * assigned is affected by the IDs registered before it, the order classes are registered is important when using this method.
	 * The order must be the same at deserialization as it was for serialization. Registering a primitive also affects the
	 * corresponding primitive wrapper. */
	public Registration register (Class type) {
		return register(type, getDefaultSerializer(type));
	}

	/** Registers the class using the specified ID and the {@link #getDefaultSerializer(Class) default serializer}. If the ID is
	 * already in use by the same type, the old entry is overwritten. If the ID is already in use by a different type, a
	 * {@link KryoException} is thrown. IDs are written with {@link Output#writeInt(int, boolean)} called with true, so smaller
	 * positive integers use fewer bytes. IDs must be the same at deserialization as they were for serialization. Registering a
	 * primitive also affects the corresponding primitive wrapper.
	 * @param id Must not be -1 or -2. */
	public Registration register (Class type, int id) {
		return register(type, getDefaultSerializer(type), id);
	}

	/** Registers the class using the next available, lowest integer ID. If the class is already registered, the existing entry is
	 * updated with the new serializer. Because the ID assigned is affected by the IDs registered before it, the order classes are
	 * registered is important when using this method. The order must be the same at deserialization as it was for serialization.
	 * Registering a primitive also affects the corresponding primitive wrapper. */
	public Registration register (Class type, Serializer serializer) {
		Registration registration = classToRegistration.get(type);
		if (registration != null) {
			registration.setSerializer(serializer);
			return registration;
		}
		int id;
		while (true) {
			id = nextRegisterID++;
			// Disallow -1 and -2, which are used for NAME and NULL (stored as id + 2 == 1 and 0).
			if (nextRegisterID == -2) nextRegisterID = 0;
			if (!idToRegistration.containsKey(id)) break;
		}
		return registerInternal(new Registration(type, serializer, id));
	}

	/** Registers the class using the specified ID. If the ID is already in use by the same type, the old entry is overwritten. If
	 * the ID is already in use by a different type, a {@link KryoException} is thrown. IDs are written with
	 * {@link Output#writeInt(int, boolean)} called with true, so smaller positive integers use fewer bytes. IDs must be the same
	 * at deserialization as they were for serialization. Registering a primitive also affects the corresponding primitive wrapper.
	 * @param id Must not be -1 or -2. */
	public Registration register (Class type, Serializer serializer, int id) {
		if (id == -1 || id == -2) throw new IllegalArgumentException("id cannot be -1 or -2");
		return register(new Registration(type, serializer, id));
	}

	/** Stores the specified registration. This can be used to efficiently store per type information needed for serialization,
	 * accessible in serializers via {@link #getRegistration(Class)}. If the ID is already in use by the same type, the old entry
	 * is overwritten. If the ID is already in use by a different type, a {@link KryoException} is thrown. IDs are written with
	 * {@link Output#writeInt(int, boolean)} called with true, so smaller positive integers use fewer bytes. IDs must be the same
	 * at deserialization as they were for serialization. Registering a primitive also affects the corresponding primitive wrapper.
	 * @param registration The id must not be -1 or -2. */
	public Registration register (Registration registration) {
		if (registration == null) throw new IllegalArgumentException("registration cannot be null.");
		int id = registration.getId();
		if (id == -1 || id == -2) throw new IllegalArgumentException("id cannot be -1 or -2");

		Registration existing = getRegistration(registration.getType());
		if (existing != null && existing.getType() != registration.getType()) {
			throw new KryoException("An existing registration with a different type already uses ID: " + registration.getId()
				+ "\nExisting registration: " + existing + "\nUnable to set registration: " + registration);
		}

		registerInternal(registration);
		return registration;
	}

	private Registration registerInternal (Registration registration) {
		if (TRACE) {
			if (registration.getId() == NAME) {
				trace("kryo", "Register class name: " + className(registration.getType()) + " ("
					+ registration.getSerializer().getClass().getName() + ")");
			} else {
				trace("kryo", "Register class ID " + registration.getId() + ": " + className(registration.getType()) + " ("
					+ registration.getSerializer().getClass().getName() + ")");
			}
		}
		classToRegistration.put(registration.getType(), registration);
		idToRegistration.put(registration.getId(), registration);
		if (registration.getType().isPrimitive()) classToRegistration.put(getWrapperClass(registration.getType()), registration);
		return registration;
	}

	/** Returns the registration for the specified class. If the class is not registered {@link #setRegistrationRequired(boolean)}
	 * is false, it is automatically registered using the {@link #addDefaultSerializer(Class, Class) default serializer}.
	 * @throws IllegalArgumentException if the class is not registered and {@link #setRegistrationRequired(boolean)} is true. */
	public Registration getRegistration (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");

		if (type == memoizedType) return memoizedRegistration;
		Registration registration = classToRegistration.get(type);
		if (registration == null) {
			if (Proxy.isProxyClass(type)) {
				// If a Proxy class, treat it like an InvocationHandler because the concrete class for a proxy is generated.
				registration = getRegistration(InvocationHandler.class);
			} else if (!type.isEnum() && Enum.class.isAssignableFrom(type)) {
				// This handles an enum value that is an inner class. Eg: enum A {b{}};
				registration = getRegistration(type.getEnclosingClass());
			} else if (registrationRequired) {
				throw new IllegalArgumentException("Class is not registered: " + className(type)
					+ "\nNote: To register this class use: kryo.register(" + className(type) + ".class);");
			} else
				registration = registerInternal(new Registration(type, getDefaultSerializer(type), NAME));
		}
		memoizedType = type;
		memoizedRegistration = registration;
		return registration;
	}

	/** Returns the registration for the specified ID, or null if no class is registered with that ID. */
	public Registration getRegistration (int classID) {
		return idToRegistration.get(classID);
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
	 * @return Will be null if type is null. */
	public Registration writeClass (Output output, Class type) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		try {
			if (type == null) {
				if (TRACE || (DEBUG && depth == 1)) log("Write", null);
				output.writeByte(NULL);
				return null;
			}
			Registration registration = getRegistration(type);
			if (registration.getId() == NAME) {
				output.writeByte(NAME + 2);
				int nameId = classToNameId.get(type, -1);
				if (nameId != -1) {
					if (TRACE) trace("kryo", "Write class name reference " + nameId + ": " + className(type));
					output.writeInt(nameId, true);
					return registration;
				}
				// Only write the class name the first time encountered in object graph.
				if (TRACE) trace("kryo", "Write class name: " + className(type));
				nameId = nextNameId++;
				classToNameId.put(type, nameId);
				output.write(nameId);
				output.writeString(type.getName());
			} else {
				if (TRACE) trace("kryo", "Write class " + registration.getId() + ": " + className(type));
				output.writeInt(registration.getId() + 2, true);
			}
			return registration;
		} finally {
			if (depth == 0) reset();
		}
	}

	/** Writes an object using the registered serializer. */
	public void writeObject (Output output, Object object) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		if (object == null) throw new IllegalArgumentException("object cannot be null.");
		depth++;
		try {
			if (references && writeReferenceOrNull(output, object, false)) return;
			if (TRACE || (DEBUG && depth == 1)) log("Write", object);
			getRegistration(object.getClass()).getSerializer().write(this, output, object);
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Writes an object using the specified serializer. The registered serializer is ignored. */
	public void writeObject (Output output, Object object, Serializer serializer) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		if (object == null) throw new IllegalArgumentException("object cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		depth++;
		try {
			if (references && writeReferenceOrNull(output, object, false)) return;
			if (TRACE || (DEBUG && depth == 1)) log("Write", object);
			serializer.write(this, output, object);
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Writes an object or null using the registered serializer.
	 * @param object May be null. */
	public void writeObjectOrNull (Output output, Object object) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		depth++;
		try {
			Serializer serializer = getRegistration(object.getClass()).getSerializer();
			if (references) {
				if (writeReferenceOrNull(output, object, true)) return;
			} else if (!serializer.getAcceptsNull()) {
				if (object == null) {
					if (TRACE || (DEBUG && depth == 1)) log("Write", object);
					output.writeByte(NULL);
					return;
				}
				output.writeByte(Kryo.NOT_NULL);
			}
			if (TRACE || (DEBUG && depth == 1)) log("Write", object);
			serializer.write(this, output, object);
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Writes an object or null using the specified serializer. The registered serializer is ignored.
	 * @param object May be null. */
	public void writeObjectOrNull (Output output, Object object, Serializer serializer) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		depth++;
		try {
			if (references) {
				if (writeReferenceOrNull(output, object, true)) return;
			} else if (!serializer.getAcceptsNull()) {
				if (object == null) {
					if (TRACE || (DEBUG && depth == 1)) log("Write", null);
					output.writeByte(NULL);
					return;
				}
				output.writeByte(Kryo.NOT_NULL);
			}
			if (TRACE || (DEBUG && depth == 1)) log("Write", object);
			serializer.write(this, output, object);
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Writes the class and object or null using the registered serializer.
	 * @param object May be null. */
	public void writeClassAndObject (Output output, Object object) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		depth++;
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
			if (--depth == 0) reset();
		}
	}

	/** @param object May be null if mayBeNull is true. */
	private boolean writeReferenceOrNull (Output output, Object object, boolean mayBeNull) {
		if (object == null) {
			if (TRACE || (DEBUG && depth == 1)) log("Write", null);
			output.writeByte(NULL);
			return true;
		}
		Class type = object.getClass();
		if (!useReferences(type)) {
			if (mayBeNull) output.writeByte(Kryo.NOT_NULL);
			return false;
		}
		int instanceId = objectToInstanceId.get(object, -1);
		if (instanceId != -1) {
			if (DEBUG) debug("kryo", "Write object reference " + instanceId + ": " + string(object));
			output.writeInt(instanceId, true);
			return true;
		}
		// Only write the object the first time encountered in object graph.
		instanceId = classToNextInstanceId.getAndIncrement(type, 1, 1);
		if (TRACE) trace("kryo", "Write initial object reference " + instanceId + ": " + string(object));
		objectToInstanceId.put(object, instanceId);
		output.writeInt(instanceId, true);
		return false;
	}

	/** Reads a class and returns its registration.
	 * @return May be null. */
	public Registration readClass (Input input) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		try {
			int classID = input.readInt(true);
			switch (classID) {
			case NULL:
				if (TRACE || (DEBUG && depth == 1)) log("Read", null);
				return null;
			case NAME + 2: // Offset for NAME and NULL.
				int nameId = input.readInt(true);
				Class type = nameIdToClass.get(nameId);
				if (type == null) {
					// Only read the class name the first time encountered in object graph.
					String className = input.readString();
					try {
						type = Class.forName(className, false, classLoader);
					} catch (ClassNotFoundException ex) {
						throw new KryoException("Unable to find class: " + className, ex);
					}
					nameIdToClass.put(nameId, type);
					if (TRACE) trace("kryo", "Read class name: " + className);
				} else {
					if (TRACE) trace("kryo", "Read class name reference " + nameId + ": " + className(type));
				}
				return getRegistration(type);
			}
			Registration registration = idToRegistration.get(classID - 2);
			if (registration == null) throw new KryoException("Encountered unregistered class ID: " + (classID - 2));
			if (TRACE) trace("kryo", "Read class " + (classID - 2) + ": " + className(registration.getType()));
			return registration;
		} finally {
			if (depth == 0) reset();
		}
	}

	/** Reads an object using the registered serializer. */
	public <T> T readObject (Input input, Class<T> type) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		depth++;
		try {
			InstanceId instanceId = null;
			if (references) {
				instanceId = readReferenceOrNull(input, type, false);
				if (instanceId == this.instanceId) return (T)instanceId.object;
			}

			Serializer serializer = getRegistration(type).getSerializer();
			T object = (T)serializer.create(this, input, type);
			if (instanceId != null) instanceIdToObject.put(instanceId, object);
			if (object != null) serializer.read(this, input, object);
			if (TRACE || (DEBUG && depth == 1)) log("Read", object);
			return object;
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Reads an object using the specified serializer. The registered serializer is ignored. */
	public <T> T readObject (Input input, Class<T> type, Serializer serializer) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		depth++;
		try {
			InstanceId instanceId = null;
			if (references) {
				instanceId = readReferenceOrNull(input, type, false);
				if (instanceId == this.instanceId) return (T)instanceId.object;
			}

			T object = (T)serializer.create(this, input, type);
			if (instanceId != null) instanceIdToObject.put(instanceId, object);
			if (object != null) serializer.read(this, input, object);
			if (TRACE || (DEBUG && depth == 1)) log("Read", object);
			return object;
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Reads an object or null using the registered serializer.
	 * @return May be null. */
	public <T> T readObjectOrNull (Input input, Class<T> type) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		depth++;
		try {
			Serializer serializer = getRegistration(type).getSerializer();

			InstanceId instanceId = null;
			if (references) {
				instanceId = readReferenceOrNull(input, type, true);
				if (instanceId == this.instanceId) return (T)instanceId.object;
			} else if (!serializer.getAcceptsNull()) {
				if (input.readByte() == NULL) {
					if (TRACE || (DEBUG && depth == 1)) log("Read", null);
					return null;
				}
			}

			T object = (T)serializer.create(this, input, type);
			if (instanceId != null) instanceIdToObject.put(instanceId, object);
			if (object != null) serializer.read(this, input, object);
			if (TRACE || (DEBUG && depth == 1)) log("Read", object);
			return object;
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Reads an object or null using the specified serializer. The registered serializer is ignored.
	 * @return May be null. */
	public <T> T readObjectOrNull (Input input, Class<T> type, Serializer serializer) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		depth++;
		try {
			InstanceId instanceId = null;
			if (references) {
				instanceId = readReferenceOrNull(input, type, true);
				if (instanceId == this.instanceId) return (T)instanceId.object;
			} else if (!serializer.getAcceptsNull()) {
				if (input.readByte() == NULL) {
					if (TRACE || (DEBUG && depth == 1)) log("Read", null);
					return null;
				}
			}

			T object = (T)serializer.create(this, input, type);
			if (instanceId != null) instanceIdToObject.put(instanceId, object);
			if (object != null) serializer.read(this, input, object);
			if (TRACE || (DEBUG && depth == 1)) log("Read", object);
			return object;
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Reads the class and object or null using the registered serializer.
	 * @return May be null. */
	public Object readClassAndObject (Input input) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		depth++;
		try {
			Registration registration = readClass(input);
			if (registration == null) return null;
			Class type = registration.getType();

			InstanceId instanceId = null;
			if (references) {
				instanceId = readReferenceOrNull(input, type, false);
				if (instanceId == this.instanceId) return instanceId.object;
			}

			Serializer serializer = registration.getSerializer();
			Object object = serializer.create(this, input, type);
			if (instanceId != null) instanceIdToObject.put(instanceId, object);
			if (object != null) serializer.read(this, input, object);
			if (TRACE || (DEBUG && depth == 1)) log("Read", object);
			return object;
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** @return Null if references for the type is not supported. this.instanceId if the object field should be used. A new
	 *         InstanceId if this is the first time the object appears in the graph. */
	private InstanceId readReferenceOrNull (Input input, Class type, boolean mayBeNull) {
		if (type.isPrimitive()) type = getWrapperClass(type);
		boolean referencesSupported = useReferences(type);
		int id;
		if (mayBeNull) {
			id = input.readInt(true);
			if (id == NULL) {
				if (TRACE || (DEBUG && depth == 1)) log("Read", null);
				instanceId.object = null;
				return instanceId;
			}
			if (!referencesSupported) return null;
		} else {
			if (!referencesSupported) return null;
			id = input.readInt(true);
		}
		instanceId.id = id;
		instanceId.type = type;
		Object object = instanceIdToObject.get(instanceId);
		if (object != null) {
			if (DEBUG) debug("kryo", "Read object reference " + id + ": " + string(object));
			instanceId.object = object;
			return instanceId;
		}
		if (TRACE) trace("kryo", "Read initial object reference " + id + ": " + className(type));
		return new InstanceId(type, id);
	}

	/** Called when an object graph has been completely serialized or deserialized, allowing any state only needed per object graph
	 * to be reset. If overridden, the super method must be called. */
	protected void reset () {
		depth = 0;
		if (graphContext != null) graphContext.clear();
		if (!registrationRequired) {
			classToNameId.clear();
			nameIdToClass.clear();
			nextNameId = 0;
		}
		if (references) {
			objectToInstanceId.clear();
			instanceIdToObject.clear();
			classToNextInstanceId.clear();
		}
		if (originalToCopy != null) originalToCopy.clear();
		if (TRACE) trace("kryo", "Object graph complete.");
	}

	/** Returns a deep copy of the object. Serializers for the classes involved must support
	 * {@link Serializer#createCopy(Kryo, Object)}.
	 * @param object May be null. */
	public <T> T copy (T object) {
		if (object == null) return null;
		if (copyShallow) return object;
		depth++;
		try {
			if (originalToCopy == null) originalToCopy = new IdentityMap();
			Object existingCopy = originalToCopy.get(object);
			if (existingCopy != null) return (T)existingCopy;
			Serializer serializer = getRegistration(object.getClass()).getSerializer();
			Object copy = serializer.createCopy(this, object);
			originalToCopy.put(object, copy);
			serializer.copy(this, object, copy);
			if (TRACE || (DEBUG && depth == 1)) log("Copy", copy);
			return (T)copy;
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Returns a deep copy of the object using the specified serializer. Serializers for the classes involved must support
	 * {@link Serializer#createCopy(Kryo, Object)}.
	 * @param object May be null. */
	public <T> T copy (T object, Serializer serializer) {
		if (object == null) return null;
		if (copyShallow) return object;
		depth++;
		try {
			if (originalToCopy == null) originalToCopy = new IdentityMap();
			Object existingCopy = originalToCopy.get(object);
			if (existingCopy != null) return (T)existingCopy;
			Object copy = serializer.createCopy(this, object);
			originalToCopy.put(object, copy);
			serializer.copy(this, object, copy);
			if (TRACE || (DEBUG && depth == 1)) log("Copy", copy);
			return (T)copy;
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Returns a shallow copy of the object. Serializers for the classes involved must support
	 * {@link Serializer#createCopy(Kryo, Object)}.
	 * @param object May be null. */
	public <T> T copyShallow (T object) {
		if (object == null) return null;
		depth++;
		copyShallow = true;
		try {
			if (originalToCopy == null) originalToCopy = new IdentityMap();
			Object existingCopy = originalToCopy.get(object);
			if (existingCopy != null) return (T)existingCopy;
			Serializer serializer = getRegistration(object.getClass()).getSerializer();
			Object copy = serializer.createCopy(this, object);
			originalToCopy.put(object, copy);
			serializer.copy(this, object, copy);
			if (TRACE || (DEBUG && depth == 1)) log("Shallow copy", copy);
			return (T)copy;
		} finally {
			copyShallow = false;
			if (--depth == 0) reset();
		}
	}

	/** Returns a shallow copy of the object using the specified serializer. Serializers for the classes involved must support
	 * {@link Serializer#createCopy(Kryo, Object)}.
	 * @param object May be null. */
	public <T> T copyShallow (T object, Serializer serializer) {
		if (object == null) return null;
		depth++;
		copyShallow = true;
		try {
			if (originalToCopy == null) originalToCopy = new IdentityMap();
			Object existingCopy = originalToCopy.get(object);
			if (existingCopy != null) return (T)existingCopy;
			Object copy = serializer.createCopy(this, object);
			originalToCopy.put(object, copy);
			serializer.copy(this, object, copy);
			if (TRACE || (DEBUG && depth == 1)) log("Shallow copy", copy);
			return (T)copy;
		} finally {
			copyShallow = false;
			if (--depth == 0) reset();
		}
	}

	/** Sets the classloader to resolve unregistered class names to classes. */
	public void setClassLoader (ClassLoader classLoader) {
		if (classLoader == null) throw new IllegalArgumentException("classLoader cannot be null.");
		this.classLoader = classLoader;
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

	/** If true, each appearance of an object in the graph after the first is stored as an integer ordinal. This enables references
	 * to the same object and cyclic graphs to be serialized, but has the overhead of one byte per object. Default is true. */
	public void setReferences (boolean references) {
		this.references = references;
		if (TRACE) trace("kryo", "References: " + references);
	}

	/** Returns true if references will be written for the specified type when references are enabled. The default implementation
	 * returns false for Boolean, Byte, Character, and Short.
	 * @param type Will never be a primitive type, but may be a primitive type wrapper. */
	protected boolean useReferences (Class type) {
		return type != Boolean.class && type != Byte.class && type != Character.class && type != Short.class;
	}

	/** Sets the serializer to use for arrays. */
	public void setArraySerializer (ArraySerializer arraySerializer) {
		if (arraySerializer == null) throw new IllegalArgumentException("arraySerializer cannot be null.");
		this.arraySerializer = arraySerializer;
		if (TRACE) trace("kryo", "Array serializer set: " + arraySerializer.getClass().getName());
	}

	public ArraySerializer getArraySerializer () {
		return arraySerializer;
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

	// --- Utility ---

	/** Returns true if the specified type is final, or if it is an array of a final type. Final types can be serialized more
	 * efficiently because they are non-polymorphic.
	 * <p>
	 * This can be overridden to force non-final classes to be treated as final. Eg, if an application uses ArrayList extensively
	 * but never uses an ArrayList subclass, treating ArrayList as final would allow FieldSerializer to save 1-2 bytes per
	 * ArrayList field. */
	public boolean isFinal (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (type.isArray()) return Modifier.isFinal(ArraySerializer.getElementClass(type).getModifiers());
		return Modifier.isFinal(type.getModifiers());
	}

	static final class InstanceId {
		Class type;
		int id;
		Object object; // Set in readReferenceOrNull() for use as a return value.

		public InstanceId (Class type, int id) {
			this.type = type;
			this.id = id;
		}

		public int hashCode () {
			return 31 * (31 + id) + type.hashCode();
		}

		public boolean equals (Object obj) {
			if (obj == null) return false;
			InstanceId other = (InstanceId)obj;
			if (id != other.id) return false;
			if (type != other.type) return false;
			return true;
		}
	}

	static final class DefaultSerializerEntry {
		Class type;
		Serializer serializer;
		Class<? extends Serializer> serializerClass;
	}
}
