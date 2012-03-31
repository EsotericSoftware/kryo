
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
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ShortSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringBufferSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringBuilderSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializer;
import com.esotericsoftware.kryo.util.IntMap;
import com.esotericsoftware.kryo.util.ObjectIntMap;
import com.esotericsoftware.kryo.util.ObjectMap;

public class Kryo {
	static private final byte NULL = 0;
	static private final byte NOT_NULL = 1;
	static private final int NAME = 16383;

	static public boolean isAndroid;
	static {
		try {
			Class.forName("android.os.Process");
			isAndroid = true;
		} catch (Exception ignored) {
		}
	}

	private Class<? extends Serializer> defaultSerializer = FieldSerializer.class;
	private final ArrayList<DefaultSerializer> defaultSerializers = new ArrayList(32);
	private ArraySerializer arraySerializer = new ArraySerializer();

	private int depth;
	private int nextID = 1;
	private final IntMap<Registration> idToRegistration = new IntMap();
	private final ObjectMap<Class, Registration> classToRegistration = new ObjectMap();

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

	/** Sets the serailzer to use when no serializers added with {@link #addDefaultSerializer(Class, Serializer)} match an object's
	 * type. Default is {@link FieldSerializer}. */
	public void setDefaultSerializer (Class<Serializer> serializer) {
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		defaultSerializer = serializer;
	}

	/** Any objects that are of the type or super type of the specified class will use the specified serializer. Note that the order
	 * default serializers are added is important. The following are defaults:
	 * <p>
	 * <table border="1">
	 * <tr>
	 * <th>Type</th>
	 * <th>Serializer</th>
	 * </tr>
	 * <tr>
	 * <td>array (any number of dimensions)</td>
	 * <td>{@link ArraySerializer}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link Enum}</td>
	 * <td>{@link EnumSerializer}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link Collection}</td>
	 * <td>{@link CollectionSerializer}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link Map}</td>
	 * <td>{@link MapSerializer}</td>
	 * </tr>
	 * <tr>
	 * <td>{@link CustomSerialization}</td>
	 * <td>{@link CustomSerializer}</td>
	 * </tr>
	 * <tr>
	 * <td>class with {@link DefaultSerializer} annotation</td>
	 * <td>serializer specified in annotiation</td>
	 * </tr>
	 * </table> */
	public void addDefaultSerializer (Class type, Serializer serializer) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		DefaultSerializer entry = new DefaultSerializer();
		entry.type = type;
		entry.serializer = serializer;
		defaultSerializers.add(entry);
	}

	public void addDefaultSerializer (Class type, Class<? extends Serializer> serializerClass) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializerClass == null) throw new IllegalArgumentException("serializerClass cannot be null.");
		DefaultSerializer entry = new DefaultSerializer();
		entry.type = type;
		entry.serializerClass = serializerClass;
		defaultSerializers.add(entry);
	}

	/** Returns the best matching serializer for a class. */
	public Serializer getDefaultSerializer (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");

		if (type.isArray()) return arraySerializer;

		for (int i = 0, n = defaultSerializers.size(); i < n; i++) {
			DefaultSerializer entry = defaultSerializers.get(i);
			if (entry.type.isAssignableFrom(type)) {
				if (entry.serializer != null) return entry.serializer;
				return newSerializer(entry.serializerClass, type);
			}
		}
		return newSerializer(defaultSerializer, type);
	}

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
				+ type.getName(), ex);
		}
	}

	// --- Registration ---

	/** Registers the class using the next available, lowest integer ID and the {@link #getDefaultSerializer(Class) default
	 * serializer}. If the class is already registered, the existing entry is updated with the new serializer. Because the ID
	 * assigned is affected by the IDs registered before it, the order classes are registered is important when using this method.
	 * Registering a primitive also affects the corresponding primitive wrapper. */
	public Registration register (Class type) {
		return register(type, getDefaultSerializer(type));
	}

	/** Registers the class using the specified ID and the {@link #getDefaultSerializer(Class) default serializer}. If the ID is
	 * already in use by the same type, the old entry is overwritten. If the ID is already in use by a different type, a
	 * {@link KryoException} is thrown. IDs are written with {@link KryoOutput#writeInt(int, boolean)} called with true, so smaller
	 * positive integers use fewer bytes. Registering a primitive also affects the corresponding primitive wrapper.
	 * @param id Must not be 0 or {@value #NAME}. */
	public Registration register (Class type, int id) {
		return register(type, getDefaultSerializer(type), id);
	}

	/** Registers the class using the next available, lowest integer ID. If the class is already registered, the existing entry is
	 * updated with the new serializer. Because the ID assigned is affected by the IDs registered before it, the order classes are
	 * registered is important when using this method. Registering a primitive also affects the corresponding primitive wrapper. */
	public Registration register (Class type, Serializer serializer) {
		Registration registration = classToRegistration.get(type);
		if (registration != null) {
			registration.setSerializer(serializer);
			return registration;
		}
		int id;
		while (true) {
			id = nextID++;
			if (nextID == NAME) nextID++;
			if (!idToRegistration.containsKey(id)) break;
		}
		return registerInternal(type, serializer, id);
	}

	/** Registers the class using the specified ID. If the ID is already in use by the same type, the old entry is overwritten. If
	 * the ID is already in use by a different type, a {@link KryoException} is thrown. IDs are written with
	 * {@link KryoOutput#writeInt(int, boolean)} called with true, so smaller positive integers use fewer bytes. Registering a
	 * primitive also affects the corresponding primitive wrapper.
	 * @param id Must not be 0 or {@value #NAME}. */
	public Registration register (Class type, Serializer serializer, int id) {
		if (id == 0 || id == NAME) throw new IllegalArgumentException("id cannot be 0 or " + NAME);
		return registerInternal(type, serializer, id);
	}

	private Registration registerInternal (Class type, Serializer serializer, int id) {
		Registration registration = new Registration(type, id, serializer);
		classToRegistration.put(type, registration);
		idToRegistration.put(id, registration);
		if (type.isPrimitive()) {
			Class wrapperClass;
			if (type == boolean.class)
				wrapperClass = Boolean.class;
			else if (type == byte.class)
				wrapperClass = Byte.class;
			else if (type == char.class)
				wrapperClass = Character.class;
			else if (type == short.class)
				wrapperClass = Short.class;
			else if (type == int.class)
				wrapperClass = Integer.class;
			else if (type == long.class)
				wrapperClass = Long.class;
			else if (type == float.class)
				wrapperClass = Float.class;
			else
				wrapperClass = Double.class;
			classToRegistration.put(wrapperClass, registration);
		}
		return registration;
	}

	public Registration getRegistration (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");

		Registration registration = classToRegistration.get(type);
		if (registration != null) return registration;

		// If a Proxy class, treat it like an InvocationHandler because the concrete class for a proxy is generated.
		if (Proxy.isProxyClass(type)) return getRegistration(InvocationHandler.class);

		// This handles an enum value that is an inner class. Eg: enum A {b{}};
		if (!type.isEnum() && Enum.class.isAssignableFrom(type)) return getRegistration(type.getEnclosingClass());

		if (registrationRequired) throw new IllegalArgumentException("Class is not registered: " + type.getName());

		return registerInternal(type, getDefaultSerializer(type), NAME);
	}

	public Registration getRegistration (int classID) {
		Registration registration = idToRegistration.get(classID);
		if (registration == null) throw new IllegalArgumentException("Class ID is not registered: " + classID);
		return registration;
	}

	public Serializer getSerializer (Class type) {
		return getRegistration(type).getSerializer();
	}

	public void setRegistrationRequired (boolean registrationRequired) {
		this.registrationRequired = registrationRequired;
	}

	// --- Serialization ---

	/** Writes a class and returns its registration.
	 * @param type May be null.
	 * @return Will be null if type is null. */
	public Registration writeClass (KryoOutput output, Class type) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		try {
			if (type == null) {
				output.writeByte(Kryo.NULL);
				return null;
			}
			Registration registration = getRegistration(type);
			output.writeInt(registration.getId(), true);
			if (registration.getId() == NAME) {
				int nameId = classToNameId.get(type, -1);
				if (nameId != -1) {
					output.writeInt(nameId, true);
					return registration;
				}
				// Only write the class name the first time encountered in object graph.
				nameId = nextNameId++;
				classToNameId.put(type, nameId);
				output.write(nameId);
				output.writeString(type.getName());
			}
			return registration;
		} finally {
			if (depth == 0) reset();
		}
	}

	/** Writes an object using the registered serializer. */
	public void writeObject (KryoOutput output, Object object) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		if (object == null) throw new IllegalArgumentException("object cannot be null.");
		depth++;
		try {
			if (references && writeReference(output, object)) return;
			getRegistration(object.getClass()).getSerializer().write(this, output, object);
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Writes an object using the specified serializer. */
	public void writeObject (KryoOutput output, Object object, Serializer serializer) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		if (object == null) throw new IllegalArgumentException("object cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		depth++;
		try {
			if (references && writeReference(output, object)) return;
			serializer.write(this, output, object);
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Writes an object using the registered serializer.
	 * @param object May be null. */
	public void writeObjectOrNull (KryoOutput output, Object object) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		depth++;
		try {
			if (object == null) {
				output.writeByte(Kryo.NULL);
				return;
			}
			output.writeByte(Kryo.NOT_NULL);
			if (references && writeReference(output, object)) return;
			getRegistration(object.getClass()).getSerializer().write(this, output, object);
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Writes an object using the specified serializer.
	 * @param object May be null. */
	public void writeObjectOrNull (KryoOutput output, Object object, Serializer serializer) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		depth++;
		try {
			if (object == null) {
				output.writeByte(Kryo.NULL);
				return;
			}
			output.writeByte(Kryo.NOT_NULL);
			if (references && writeReference(output, object)) return;
			serializer.write(this, output, object);
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Writes the class and object using the registered serializer.
	 * @param object May be null. */
	public void writeClassAndObject (KryoOutput output, Object object) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		depth++;
		try {
			if (object == null) {
				writeClass(output, null);
				return;
			}
			Registration registration = writeClass(output, object.getClass());
			if (references && writeReference(output, object)) return;
			registration.getSerializer().write(this, output, object);
		} finally {
			if (--depth == 0) reset();
		}
	}

	private boolean writeReference (KryoOutput output, Object object) {
		int instanceId = objectToInstanceId.get(object, -1);
		if (instanceId != -1) {
			output.writeInt(instanceId, true);
			return true;
		}
		// Only write the object the first time encountered in object graph.
		instanceId = classToNextInstanceId.getAndIncrement(object.getClass(), 0, 1);
		objectToInstanceId.put(object, instanceId);
		output.writeInt(instanceId, true);
		return false;
	}

	/** Reads a class and returns its registration.
	 * @return May be null. */
	public Registration readClass (KryoInput input) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		try {
			int classID = input.readInt(true);
			if (classID == Kryo.NULL) return null;
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
				}
				return getRegistration(type);
			}
			Registration registration = idToRegistration.get(classID);
			if (registration == null) throw new KryoException("Encountered unregistered class ID: " + classID);
			return registration;
		} finally {
			if (depth == 0) reset();
		}
	}

	/** Reads an object using the registered serializer.
	 * @return May be null. */
	public <T> T readObject (KryoInput input, Class<T> type) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		depth++;
		try {
			if (references) {
				instanceId.type = type;
				instanceId.id = input.readInt(true);
				Object object = instanceIdToObject.get(instanceId);
				if (object != null) return (T)object;
			}
			T object = (T)getRegistration(type).getSerializer().read(this, input, type);
			if (references) instanceIdToObject.put(new InstanceId(type, instanceId.id), object);
			return object;
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Reads an object using the specified serializer.
	 * @return May be null. */
	public <T> T readObject (KryoInput input, Class<T> type, Serializer serializer) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		depth++;
		try {
			if (references) {
				instanceId.type = type;
				instanceId.id = input.readInt(true);
				Object object = instanceIdToObject.get(instanceId);
				if (object != null) return (T)object;
			}
			T object = (T)serializer.read(this, input, type);
			if (references) instanceIdToObject.put(new InstanceId(type, instanceId.id), object);
			return object;
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Reads an object using the registered serializer.
	 * @return May be null. */
	public <T> T readObjectOrNull (KryoInput input, Class<T> type) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		depth++;
		try {
			if (input.readByte() == Kryo.NULL) return null;
			if (references) {
				instanceId.type = type;
				instanceId.id = input.readInt(true);
				Object object = instanceIdToObject.get(instanceId);
				if (object != null) return (T)object;
			}
			T object = (T)getRegistration(type).getSerializer().read(this, input, type);
			if (references) instanceIdToObject.put(new InstanceId(type, instanceId.id), object);
			return object;
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Reads an object using the specified serializer.
	 * @return May be null. */
	public <T> T readObjectOrNull (KryoInput input, Class<T> type, Serializer serializer) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		depth++;
		try {
			if (input.readByte() == Kryo.NULL) return null;
			if (references) {
				instanceId.type = type;
				instanceId.id = input.readInt(true);
				Object object = instanceIdToObject.get(instanceId);
				if (object != null) return (T)object;
			}
			T object = (T)serializer.read(this, input, type);
			if (references) instanceIdToObject.put(new InstanceId(type, instanceId.id), object);
			return object;
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Reads the class and object using the registered serializer.
	 * @return May be null. */
	public Object readClassAndObject (KryoInput input) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		depth++;
		try {
			Registration registration = readClass(input);
			if (registration == null) return null;
			Class type = registration.getType();
			if (references) {
				instanceId.type = type;
				instanceId.id = input.readInt(true);
				Object object = instanceIdToObject.get(instanceId);
				if (object != null) return object;
			}
			Object object = registration.getSerializer().read(this, input, type);
			if (references) instanceIdToObject.put(new InstanceId(type, instanceId.id), object);
			return object;
		} finally {
			if (--depth == 0) reset();
		}
	}

	/** Called when an object graph has been completely serialized or deserialized, allowing any state only needed per object graph
	 * to be reset. */
	protected void reset () {
		depth = 0;
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
	}

	public void setClassLoader (ClassLoader classLoader) {
		if (classLoader == null) throw new IllegalArgumentException("classLoader cannot be null.");
		this.classLoader = classLoader;
	}

	/** If true, each appearance of an object in the graph after the first is stored as an integer ordinal. Default is false. */
	public void setReferences (boolean references) {
		this.references = references;
	}

	public void setArraySerializer (ArraySerializer arraySerializer) {
		this.arraySerializer = arraySerializer;
	}

	// --- Utility ---

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
					throw new KryoException("Class cannot be created (non-static member class): " + type.getName(), ex);
				else
					throw new KryoException("Class cannot be created (missing no-arg constructor): " + type.getName(), ex);
			} catch (Exception privateConstructorException) {
				ex = privateConstructorException;
			}
			throw new KryoException("Error constructing instance of class: " + type.getName(), ex);
		}
	}

	/** Returns true if the specified type is final, or if it is an array of a final type. */
	public boolean isFinal (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (type.isArray()) return Modifier.isFinal(ArraySerializer.getElementClass(type).getModifiers());
		return Modifier.isFinal(type.getModifiers());
	}

	static final class InstanceId {
		Class type;
		int id;

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

	static final class DefaultSerializer {
		Class type;
		Serializer serializer;
		Class<? extends Serializer> serializerClass;
	}
}
