
package com.esotericsoftware.kryo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.Map;

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
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CurrencySerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.DateSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.DoubleSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.EnumSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.FloatSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.IntSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.LongSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.SerializableSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ShortSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringBufferSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringBuilderSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializer;
import com.esotericsoftware.kryo.util.IntMap;
import com.esotericsoftware.kryo.util.ObjectIntMap;
import com.esotericsoftware.kryo.util.ObjectMap;

import static com.esotericsoftware.minlog.Log.*;

/** Maps classes to serializers so object graphs can be serialized automatically.
 * @author Nathan Sweet <misc@n4te.com> */
public class Kryo {
	static private final byte NULL = 0;
	static private final byte NOT_NULL = 1;
	static public final byte NAME = -1;

	static public boolean isAndroid;
	static {
		try {
			Class.forName("android.os.Process");
			isAndroid = true;
		} catch (Exception ignored) {
		}
	}

	private Class<? extends Serializer> defaultSerializer = FieldSerializer.class;
	private final ArrayList<DefaultSerializerEntry> defaultSerializers = new ArrayList(32);
	private ArraySerializer arraySerializer = new ArraySerializer();

	private int depth, nextRegisterID;
	private final IntMap<Registration> idToRegistration = new IntMap();
	private final ObjectMap<Class, Registration> classToRegistration = new ObjectMap();
	private ObjectMap context, graphContext;

	private boolean registrationRequired;
	private final ObjectIntMap<Class> classToNameId = new ObjectIntMap();
	private final IntMap<Class> nameIdToClass = new IntMap();
	private int nextNameId;
	private ClassLoader classLoader = getClass().getClassLoader();

	private boolean references = true;
	private final InstanceId instanceId = new InstanceId(null, 0);
	private final ObjectIntMap<Class> classToNextInstanceId = new ObjectIntMap();
	private final ObjectIntMap objectToInstanceId = new ObjectIntMap();
	private final ObjectMap<InstanceId, Object> instanceIdToObject = new ObjectMap();

	public Kryo () {
		addDefaultSerializer(boolean.class, BooleanSerializer.class);
		addDefaultSerializer(Boolean.class, BooleanSerializer.class);
		addDefaultSerializer(byte.class, ByteSerializer.class);
		addDefaultSerializer(Byte.class, ByteSerializer.class);
		addDefaultSerializer(char.class, CharSerializer.class);
		addDefaultSerializer(Character.class, CharSerializer.class);
		addDefaultSerializer(short.class, ShortSerializer.class);
		addDefaultSerializer(Short.class, ShortSerializer.class);
		addDefaultSerializer(int.class, IntSerializer.class);
		addDefaultSerializer(Integer.class, IntSerializer.class);
		addDefaultSerializer(long.class, LongSerializer.class);
		addDefaultSerializer(Long.class, LongSerializer.class);
		addDefaultSerializer(float.class, FloatSerializer.class);
		addDefaultSerializer(Float.class, FloatSerializer.class);
		addDefaultSerializer(double.class, DoubleSerializer.class);
		addDefaultSerializer(Double.class, DoubleSerializer.class);
		addDefaultSerializer(byte[].class, ByteArraySerializer.class);
		addDefaultSerializer(String.class, StringSerializer.class);
		addDefaultSerializer(BigInteger.class, BigIntegerSerializer.class);
		addDefaultSerializer(BigDecimal.class, BigDecimalSerializer.class);
		addDefaultSerializer(Class.class, ClassSerializer.class);
		addDefaultSerializer(Date.class, DateSerializer.class);
		addDefaultSerializer(Enum.class, EnumSerializer.class);
		addDefaultSerializer(Currency.class, CurrencySerializer.class);
		addDefaultSerializer(StringBuffer.class, StringBufferSerializer.class);
		addDefaultSerializer(StringBuilder.class, StringBuilderSerializer.class);
		addDefaultSerializer(Collection.class, CollectionSerializer.class);
		addDefaultSerializer(Map.class, MapSerializer.class);
		addDefaultSerializer(Serializable.class, SerializableSerializer.class);

		// Primitives and string. Primitive wrappers automatically use the same registration as primitives.
		register(boolean.class);
		register(byte.class);
		register(char.class);
		register(short.class);
		register(int.class);
		register(long.class);
		register(float.class);
		register(double.class);
		register(String.class);
	}

	// --- Default serializers ---

	/** Sets the serailzer to use when no {@link #addDefaultSerializer(Class, Class) default serializers} match an object's type.
	 * Default is {@link FieldSerializer}. */
	public void setDefaultSerializer (Class<Serializer> serializer) {
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
		defaultSerializers.add(entry);
	}

	/** Instances of the specified class will use the specified serializer. Note that the order default serializers are added is
	 * important for a class with multiple super types registered. Serializer instances are created as needed via
	 * {@link #newSerializer(Class, Class)}. By default, the following classes have a default serializer set:
	 * <p>
	 * <table>
	 * <tr>
	 * <td>boolean</td>
	 * <td>Boolean</td>
	 * <td>byte</td>
	 * <td>Byte</td>
	 * <td>char</td>
	 * <td>Character</td>
	 * <tr>
	 * </tr>
	 * <td>short</td>
	 * <td>Short</td>
	 * <td>int</td>
	 * <td>Integer</td>
	 * <td>long</td>
	 * <td>Long</td>
	 * <tr>
	 * </tr>
	 * <td>float</td>
	 * <td>Float</td>
	 * <td>double</td>
	 * <td>Double</td>
	 * <td>byte[]</td>
	 * <td>String</td>
	 * <tr>
	 * </tr>
	 * <td>BigInteger</td>
	 * <td>BigDecimal</td>
	 * <td>Class</td>
	 * <td>Date</td>
	 * <td>Enum</td>
	 * <td>Currency</td>
	 * <tr>
	 * </tr>
	 * <td>StringBuffer</td>
	 * <td>StringBuilder</td>
	 * <td>Collection</td>
	 * <td>Map</td>
	 * <td>Serializable</td>
	 * </tr>
	 * </table> */
	public void addDefaultSerializer (Class type, Class<? extends Serializer> serializerClass) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializerClass == null) throw new IllegalArgumentException("serializerClass cannot be null.");
		DefaultSerializerEntry entry = new DefaultSerializerEntry();
		entry.type = type;
		entry.serializerClass = serializerClass;
		defaultSerializers.add(entry);
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
				+ toString(type), ex);
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
			if (nextRegisterID == -2) nextRegisterID = 0; // Disallow -1 and -2, which are used for NAME and NULL (stored as id + 2
// == 1 and 0).
			if (!idToRegistration.containsKey(id)) break;
		}
		return registerInternal(type, serializer, id);
	}

	/** Registers the class using the specified ID. If the ID is already in use by the same type, the old entry is overwritten. If
	 * the ID is already in use by a different type, a {@link KryoException} is thrown. IDs are written with
	 * {@link Output#writeInt(int, boolean)} called with true, so smaller positive integers use fewer bytes. IDs must be the same
	 * at deserialization as they were for serialization. Registering a primitive also affects the corresponding primitive wrapper.
	 * @param id Must not be -1 or -2. */
	public Registration register (Class type, Serializer serializer, int id) {
		if (id == -1 || id == -2) throw new IllegalArgumentException("id cannot be -1 or -2");
		return registerInternal(type, serializer, id);
	}

	private Registration registerInternal (Class type, Serializer serializer, int id) {
		if (TRACE) {
			if (id == NAME)
				trace("kryo", "Register class name: " + toString(type) + " (" + serializer.getClass().getName() + ")");
			else
				trace("kryo", "Register class ID " + id + ": " + toString(type) + " (" + serializer.getClass().getName() + ")");
		}
		Registration registration = new Registration(type, id, serializer);
		classToRegistration.put(type, registration);
		idToRegistration.put(id, registration);
		if (type.isPrimitive()) classToRegistration.put(getWrapperClass(type), registration);
		return registration;
	}

	/** Returns the registration for the specified class. If the class is not registered {@link #setRegistrationRequired(boolean)}
	 * is false, it is automatically registered using the {@link #addDefaultSerializer(Class, Class) default serializer}.
	 * @throws IllegalArgumentException if the class is not registered and {@link #setRegistrationRequired(boolean)} is true. */
	public Registration getRegistration (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");

		Registration registration = classToRegistration.get(type);
		if (registration != null) return registration;

		// If a Proxy class, treat it like an InvocationHandler because the concrete class for a proxy is generated.
		if (Proxy.isProxyClass(type)) return getRegistration(InvocationHandler.class);

		// This handles an enum value that is an inner class. Eg: enum A {b{}};
		if (!type.isEnum() && Enum.class.isAssignableFrom(type)) return getRegistration(type.getEnclosingClass());

		if (registrationRequired) {
			throw new IllegalArgumentException("Class is not registered: " + toString(type)
				+ "\nNote: To register this class use: kryo.register(" + toString(type) + ".class);");
		}

		return registerInternal(type, getDefaultSerializer(type), NAME);
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
				if (DEBUG) log("Write", null);
				output.writeByte(NULL);
				return null;
			}
			Registration registration = getRegistration(type);
			if (registration.getId() == NAME) {
				output.writeByte(NAME + 2);
				int nameId = classToNameId.get(type, -1);
				if (nameId != -1) {
					if (TRACE) trace("kryo", "Write class name reference " + nameId + ": " + toString(type));
					output.writeInt(nameId, true);
					return registration;
				}
				// Only write the class name the first time encountered in object graph.
				if (TRACE) trace("kryo", "Write class name: " + toString(type));
				nameId = nextNameId++;
				classToNameId.put(type, nameId);
				output.write(nameId);
				output.writeString(type.getName());
			} else {
				if (TRACE) trace("kryo", "Write class " + registration.getId() + ": " + toString(type));
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
			if (references && writeReference(output, object)) return;
			if (DEBUG) log("Write", object);
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
			if (references && writeReference(output, object)) return;
			if (DEBUG) log("Write", object);
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
			if (object == null) {
				if (DEBUG) log("Write", object);
				output.writeByte(NULL);
				return;
			}
			output.writeByte(Kryo.NOT_NULL);
			if (references && writeReference(output, object)) return;
			if (DEBUG) log("Write", object);
			getRegistration(object.getClass()).getSerializer().write(this, output, object);
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
			if (object == null) {
				if (DEBUG) log("Write", null);
				output.writeByte(NULL);
				return;
			}
			output.writeByte(Kryo.NOT_NULL);
			if (references && writeReference(output, object)) return;
			if (DEBUG) log("Write", object);
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
			if (references && writeReference(output, object)) return;
			if (DEBUG) log("Write", object);
			registration.getSerializer().write(this, output, object);
		} finally {
			if (--depth == 0) reset();
		}
	}

	private boolean writeReference (Output output, Object object) {
		Class type = object.getClass();
		if (type == Boolean.class || type == Byte.class || type == Character.class || type == Short.class) return false;
		int instanceId = objectToInstanceId.get(object, -1);
		if (instanceId != -1) {
			if (DEBUG) debug("kryo", "Write object reference " + instanceId + ": " + toString(object));
			output.writeInt(instanceId, true);
			return true;
		}
		// Only write the object the first time encountered in object graph.
		instanceId = classToNextInstanceId.getAndIncrement(type, 0, 1);
		if (TRACE) trace("kryo", "Write initial object reference " + instanceId + ": " + toString(object));
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
			if (classID == NULL) {
				if (DEBUG) log("Read", null);
				return null;
			}
			classID -= 2;
			if (classID == NAME) {
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
					if (TRACE) trace("kryo", "Read class name reference " + nameId + ": " + toString(type));
				}
				return getRegistration(type);
			}
			Registration registration = idToRegistration.get(classID);
			if (registration == null) throw new KryoException("Encountered unregistered class ID: " + classID);
			if (TRACE) trace("kryo", "Read class " + classID + ": " + toString(registration.getType()));
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
				instanceId = readReference(input, type);
				if (instanceId != null && instanceId.object != null) return (T)instanceId.object;
			}

			Serializer serializer = getRegistration(type).getSerializer();
			T object = (T)serializer.create(this, input, type);
			if (instanceId != null) instanceIdToObject.put(instanceId, object);

			serializer.read(this, input, object);
			if (DEBUG) log("Read", object);
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
				instanceId = readReference(input, type);
				if (instanceId != null && instanceId.object != null) return (T)instanceId.object;
			}

			T object = (T)serializer.create(this, input, type);
			if (instanceId != null) instanceIdToObject.put(instanceId, object);

			serializer.read(this, input, object);
			if (DEBUG) log("Read", object);
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
			if (input.readByte() == NULL) {
				if (DEBUG) log("Read", null);
				return null;
			}

			InstanceId instanceId = null;
			if (references) {
				instanceId = readReference(input, type);
				if (instanceId != null && instanceId.object != null) return (T)instanceId.object;
			}

			Serializer serializer = getRegistration(type).getSerializer();
			T object = (T)serializer.create(this, input, type);
			if (instanceId != null) instanceIdToObject.put(instanceId, object);

			serializer.read(this, input, object);
			if (DEBUG) log("Read", object);
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
			if (input.readByte() == NULL) {
				if (DEBUG) log("Read", null);
				return null;
			}

			InstanceId instanceId = null;
			if (references) {
				instanceId = readReference(input, type);
				if (instanceId != null && instanceId.object != null) return (T)instanceId.object;
			}

			T object = (T)serializer.create(this, input, type);
			if (instanceId != null) instanceIdToObject.put(instanceId, object);

			serializer.read(this, input, object);
			if (DEBUG) log("Read", object);
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
				instanceId = readReference(input, type);
				if (instanceId != null && instanceId.object != null) return instanceId.object;
			}

			Serializer serializer = registration.getSerializer();
			Object object = serializer.create(this, input, type);
			if (instanceId != null) instanceIdToObject.put(instanceId, object);

			serializer.read(this, input, object);
			if (DEBUG) log("Read", object);
			return object;
		} finally {
			if (--depth == 0) reset();
		}
	}

	private InstanceId readReference (Input input, Class type) {
		if (type.isPrimitive()) type = getWrapperClass(type);
		if (type == Boolean.class || type == Byte.class || type == Character.class || type == Short.class) return null;
		InstanceId newInstanceId = null;
		instanceId.type = type;
		instanceId.id = input.readInt(true);
		Object object = instanceIdToObject.get(instanceId);
		if (object != null) {
			if (DEBUG) debug("kryo", "Read object reference " + instanceId.id + ": " + toString(object));
			instanceId.object = object;
			return instanceId;
		}
		if (TRACE) trace("kryo", "Read initial object reference " + instanceId.id + ": " + toString(type));
		return new InstanceId(type, instanceId.id);
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
		if (TRACE) trace("kryo", "Object graph complete.");
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

	/** Sets the serializer to use for arrays. */
	public void setArraySerializer (ArraySerializer arraySerializer) {
		this.arraySerializer = arraySerializer;
		if (TRACE) trace("kryo", "Array serializer set: " + arraySerializer.getClass().getName());
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

	/** Returns a new instance of the specified class. Generally serializers should use this method to create an new object instance
	 * via reflection, which by default calls this method. This allows object creation to be customized globally. */
	public <T> T newInstance (Class<T> type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		try {
			return type.newInstance();
		} catch (Exception ex) {
			try {
				// Try a private constructor.
				Constructor<T> constructor = type.getDeclaredConstructor();
				constructor.setAccessible(true);
				return constructor.newInstance();
			} catch (SecurityException ignored) {
			} catch (NoSuchMethodException ignored) {
				if (type.isMemberClass() && !Modifier.isStatic(type.getModifiers()))
					throw new KryoException("Class cannot be created (non-static member class): " + toString(type), ex);
				else
					throw new KryoException("Class cannot be created (missing no-arg constructor): " + toString(type), ex);
			} catch (Exception privateConstructorException) {
				ex = privateConstructorException;
			}
			throw new KryoException("Error constructing instance of class: " + toString(type), ex);
		}
	}

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

	private Class getWrapperClass (Class type) {
		if (type == boolean.class)
			return Boolean.class;
		else if (type == byte.class)
			return Byte.class;
		else if (type == char.class)
			return Character.class;
		else if (type == short.class)
			return Short.class;
		else if (type == int.class)
			return Integer.class;
		else if (type == long.class)
			return Long.class;
		else if (type == float.class) //
			return Float.class;
		return Double.class;
	}

	static private void log (String message, Object object) {
		if (object == null) {
			if (TRACE) trace("kryo", message + ": null");
			return;
		}
		Class type = object.getClass();
		if (type.isPrimitive() || type == Boolean.class || type == Byte.class || type == Character.class || type == Short.class
			|| type == Integer.class || type == Long.class || type == Float.class || type == Double.class) {
			if (TRACE) trace("kryo", message + ": " + type.getSimpleName());
			return;
		}
		debug("kryo", message + ": " + toString(object));
	}

	static private String toString (Object object) {
		if (object == null) return "null";
		Class type = object.getClass();
		if (type.isArray()) return toString(type);
		try {
			if (type.getMethod("toString", new Class[0]).getDeclaringClass() == Object.class) return type.getName();
		} catch (Exception ignored) {
		}
		return String.valueOf(object);
	}

	static private String toString (Class type) {
		if (type.isArray()) {
			Class elementClass = ArraySerializer.getElementClass(type);
			StringBuilder buffer = new StringBuilder(16);
			for (int i = 0, n = ArraySerializer.getDimensionCount(type); i < n; i++)
				buffer.append("[]");
			return elementClass.getName() + buffer;
		}
		return type.getName();
	}

	static final class InstanceId {
		Class type;
		int id;
		Object object; // Only used in readReference().

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
