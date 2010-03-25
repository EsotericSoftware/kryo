
package com.esotericsoftware.kryo;

import static com.esotericsoftware.minlog.Log.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.esotericsoftware.kryo.serialize.ArraySerializer;
import com.esotericsoftware.kryo.serialize.BooleanSerializer;
import com.esotericsoftware.kryo.serialize.ByteSerializer;
import com.esotericsoftware.kryo.serialize.CharSerializer;
import com.esotericsoftware.kryo.serialize.CollectionSerializer;
import com.esotericsoftware.kryo.serialize.CustomSerializer;
import com.esotericsoftware.kryo.serialize.DoubleSerializer;
import com.esotericsoftware.kryo.serialize.EnumSerializer;
import com.esotericsoftware.kryo.serialize.FieldSerializer;
import com.esotericsoftware.kryo.serialize.FloatSerializer;
import com.esotericsoftware.kryo.serialize.IntSerializer;
import com.esotericsoftware.kryo.serialize.LongSerializer;
import com.esotericsoftware.kryo.serialize.MapSerializer;
import com.esotericsoftware.kryo.serialize.ShortSerializer;
import com.esotericsoftware.kryo.serialize.StringSerializer;
import com.esotericsoftware.kryo.util.IntHashMap;

/**
 * Maps classes to serializers so object graphs can be serialized automatically.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class Kryo {
	static public final String version = "1.01a";

	static private final byte ID_NULL_OBJECT = 0;
	static private final int ID_CLASS_NAME = 16383;

	static private ThreadLocal<Context> contextThreadLocal = new ThreadLocal<Context>() {
		protected Context initialValue () {
			return new Context();
		}
	};

	private final IntHashMap<RegisteredClass> idToRegisteredClass = new IntHashMap(64);
	private final HashMap<Class, RegisteredClass> classToRegisteredClass = new HashMap(64);
	private AtomicInteger nextClassID = new AtomicInteger(1);
	private Object listenerLock = new Object();
	private KryoListener[] listeners = {};
	private boolean registrationOptional;
	private ClassLoader classLoader = getClass().getClassLoader();

	private final CustomSerializer customSerializer = new CustomSerializer(this);
	private final ArraySerializer arraySerializer = new ArraySerializer(this);
	private final CollectionSerializer collectionSerializer = new CollectionSerializer(this);
	private final MapSerializer mapSerializer = new MapSerializer(this);

	public Kryo () {
		Serializer serializer;
		// Primitives.
		register(boolean.class, new BooleanSerializer());
		register(byte.class, new ByteSerializer());
		register(char.class, new CharSerializer());
		register(short.class, new ShortSerializer());
		register(int.class, new IntSerializer());
		register(long.class, new LongSerializer());
		register(float.class, new FloatSerializer());
		register(double.class, new DoubleSerializer());
		// Primitive wrappers.
		register(Boolean.class, new BooleanSerializer());
		register(Byte.class, new ByteSerializer());
		register(Character.class, new CharSerializer());
		register(Short.class, new ShortSerializer());
		register(Integer.class, new IntSerializer());
		register(Long.class, new LongSerializer());
		register(Float.class, new FloatSerializer());
		register(Double.class, new DoubleSerializer());
		// Other.
		register(String.class, new StringSerializer());
	}

	/**
	 * When true, classes that have not been {@link #register(Class, Serializer) registered} will not throw an exception. Instead,
	 * {@link #handleUnregisteredClass(Class)} will be called. Default is false.
	 */
	public void setRegistrationOptional (boolean registrationOptional) {
		this.registrationOptional = registrationOptional;
	}

	/**
	 * Registers a class for serialization.
	 * <p>
	 * If <tt>useClassNameString</tt> is true, the class name String will be written to the serialized bytes for objects of the
	 * specified type.
	 * <p>
	 * If <tt>useClassNameString</tt> is false, the class is assigned an ordinal which will be written to the serialized bytes for
	 * objects of the specified type. This is much more efficient than using the class name String, but has the drawback that the
	 * exact same classes must be registered in exactly the same order when the class is deserialized.
	 * <p>
	 * By default, primitive types, primitive wrappers, and java.lang.String are registered. All other classes must be registered
	 * before they can be serialized. Note that JDK classes such as ArrayList, HashMap, etc and even array classes such as
	 * "int[].class" or "short[][].class" must be registered. {@link #setRegistrationOptional(boolean) Optional registration} can
	 * be enabled to handle unregistered classes as they are encountered.
	 * <p>
	 * The {@link Serializer} specified will be used to serialize and deserialize objects of the specified type. Note that a
	 * serializer can be wrapped with a {@link Compressor} for compression and/or encoding.
	 * <p>
	 * If the class is already registered, the serializer will be changed.
	 * @see #register(Class)
	 */
	public RegisteredClass register (Class type, Serializer serializer, boolean useClassNameString) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		if (type.isPrimitive()) serializer.setCanBeNull(false);

		int id;
		RegisteredClass existingRegisteredClass = classToRegisteredClass.get(type);
		if (useClassNameString)
			id = ID_CLASS_NAME;
		else if (existingRegisteredClass != null)
			id = existingRegisteredClass.id;
		else {
			id = nextClassID.getAndIncrement();
			if (id == ID_CLASS_NAME) id = nextClassID.getAndIncrement();
		}

		RegisteredClass registeredClass = new RegisteredClass(type, id, serializer);
		if (!useClassNameString) idToRegisteredClass.put(id, registeredClass);
		classToRegisteredClass.put(type, registeredClass);

		if (TRACE && id > 17) {
			String name = type.getName();
			if (type.isArray()) {
				Class elementClass = ArraySerializer.getElementClass(type);
				StringBuilder buffer = new StringBuilder(16);
				for (int i = 0, n = ArraySerializer.getDimensionCount(type); i < n; i++)
					buffer.append("[]");
				name = elementClass.getName() + buffer;
			}
			if (useClassNameString)
				trace("kryo", "Registered class name: " + name + " (" + serializer.getClass().getName() + ")");
			else
				trace("kryo", "Registered class ID " + id + ": " + name + " (" + serializer.getClass().getName() + ")");
		}
		return registeredClass;
	}

	/**
	 * Registers a class with an ordinal.
	 * @see #register(Class, Serializer, boolean)
	 */
	public RegisteredClass register (Class type, Serializer serializer) {
		return register(type, serializer, false);
	}

	/**
	 * Registers a class with an ordinal, automatically determining the serializer to use. The serializer returned by
	 * {@link #newSerializer(Class)} is used.
	 * <p>
	 * Note that some serializers allow additional information to be specified to make serialization more efficient in some cases
	 * (eg, {@link ArraySerializer#setElementsCanBeNull(boolean)}). To use these features, call
	 * {@link #register(Class, Serializer)} with the configured serializer.
	 * @see #register(Class, Serializer, boolean)
	 */
	public RegisteredClass register (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		RegisteredClass existingRegisteredClass = classToRegisteredClass.get(type);
		if (existingRegisteredClass != null && existingRegisteredClass.id >= 1 && existingRegisteredClass.id <= 17)
			throw new IllegalArgumentException("Class is registered by default: " + type.getName());
		return register(type, newSerializer(type));
	}

	/**
	 * Returns a serializer for the specified type, determined according to this table:
	 * <p>
	 * <table>
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
	 * <tr>
	 * <td>any other class</td>
	 * <td>serializer returned by {@link #newDefaultSerializer(Class)}</td>
	 * </tr>
	 * </table>
	 * @see #register(Class)
	 */
	public Serializer newSerializer (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		Serializer serializer;
		if (type.isArray()) return arraySerializer;
		if (CustomSerialization.class.isAssignableFrom(type)) return customSerializer;
		if (Collection.class.isAssignableFrom(type)) return collectionSerializer;
		if (Map.class.isAssignableFrom(type)) return mapSerializer;
		if (Enum.class.isAssignableFrom(type)) return new EnumSerializer(type);
		if (type.isAnnotationPresent(DefaultSerializer.class)) {
			Class<? extends Serializer> serializerClass = ((DefaultSerializer)type.getAnnotation(DefaultSerializer.class)).value();
			try {
				try {
					return serializerClass.getConstructor(Kryo.class, Class.class).newInstance(this, type);
				} catch (NoSuchMethodException ex1) {
					try {
						return serializerClass.getConstructor(Kryo.class).newInstance(this);
					} catch (NoSuchMethodException ex2) {
						try {
							return serializerClass.getConstructor(Class.class).newInstance(this, type);
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
		return newDefaultSerializer(type);
	}

	/**
	 * Called by {@link #newSerializer(Class)} when a serializer could not otherwise be determined. The default implementation
	 * returns a new {@link FieldSerializer}.
	 */
	protected Serializer newDefaultSerializer (Class type) {
		return new FieldSerializer(this, type);
	}

	/**
	 * Returns the registration information for the specified class. If {@link #setRegistrationOptional(boolean) optional
	 * registration} is true, {@link #handleUnregisteredClass(Class)} will be called if the class is not registered. Otherwise
	 * IllegalArgumentException is thrown.
	 */
	public RegisteredClass getRegisteredClass (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		RegisteredClass registeredClass = classToRegisteredClass.get(type);
		if (registeredClass != null) return registeredClass;

		// If a Proxy class, treat it like an InvocationHandler because the concrete class for a proxy is generated.
		if (Proxy.isProxyClass(type)) return getRegisteredClass(InvocationHandler.class);

		if (registrationOptional) {
			handleUnregisteredClass(type);
			registeredClass = classToRegisteredClass.get(type);
			if (registeredClass != null) return registeredClass;
		}

		// Failed to find registered class.
		if (type.isArray()) {
			Class elementClass = ArraySerializer.getElementClass(type);
			StringBuilder buffer = new StringBuilder(16);
			for (int i = 0, n = ArraySerializer.getDimensionCount(type); i < n; i++)
				buffer.append("[]");
			throw new IllegalArgumentException("Class is not registered: " + type.getName()
				+ "\nNote: To register this class use: kryo.register(" + elementClass.getName() + buffer + ".class);");
		}
		throw new IllegalArgumentException("Class is not registered: " + type.getName());
	}

	/**
	 * If {@link #setRegistrationOptional(boolean) optional registration} is true, this method is called the first time an
	 * unregistered class is encountered. The default implementation registers the class to use the class name String in the
	 * serialized bytes.
	 * @see #register(Class, Serializer, boolean)
	 */
	protected void handleUnregisteredClass (Class type) {
		register(type, newSerializer(type), true);
	}

	public RegisteredClass getRegisteredClass (int classID) {
		RegisteredClass registeredClass = idToRegisteredClass.get(classID);
		if (registeredClass == null) throw new IllegalArgumentException("Class ID is not registered: " + classID);
		return registeredClass;
	}

	/**
	 * Sets the class loader used to resolve class names when class name Strings are encountered in the serialized bytes.
	 */
	public void setClassLoader (ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public Serializer getSerializer (Class type) {
		return getRegisteredClass(type).serializer;
	}

	public void setSerializer (Class type, Serializer serializer) {
		getRegisteredClass(type).serializer = serializer;
	}

	/**
	 * Writes the specified class to the buffer. Either a String or an int will be written, depending on how the class was
	 * {@link #register(Class, Serializer, boolean) registered}.
	 * @param type Can be null (writes a special ID for a null object).
	 * @return The registered information for the class that was written, or null of the specified class was null.
	 */
	public RegisteredClass writeClass (ByteBuffer buffer, Class type) {
		try {
			if (type == null) {
				buffer.put(ID_NULL_OBJECT);
				if (TRACE) trace("kryo", "Wrote object: null");
				return null;
			}
			RegisteredClass registeredClass = getRegisteredClass(type);
			IntSerializer.put(buffer, registeredClass.id, true);
			if (registeredClass.id == ID_CLASS_NAME) {
				StringSerializer.put(buffer, type.getName());
				if (TRACE) trace("kryo", "Wrote class name: " + type.getName());
			} else {
				if (TRACE) trace("kryo", "Wrote class " + registeredClass.id + ": " + type.getName());
			}
			return registeredClass;
		} catch (BufferOverflowException ex) {
			throw new SerializationException("Buffer limit exceeded writing class ID: " + type, ex);
		}
	}

	/**
	 * Reads the class from the buffer.
	 * @return The registered information for the class that was read, or null if the data read from the buffer represented a null
	 *         object.
	 */
	public RegisteredClass readClass (ByteBuffer buffer) {
		int classID;
		try {
			classID = IntSerializer.get(buffer, true);
			if (classID == ID_NULL_OBJECT) {
				if (TRACE) trace("kryo", "Read object: null");
				return null;
			}
			if (classID == ID_CLASS_NAME) {
				String className = StringSerializer.get(buffer);
				try {
					RegisteredClass registeredClass = getRegisteredClass(Class.forName(className, false, classLoader));
					if (TRACE) trace("kryo", "Read class name: " + className);
					return registeredClass;
				} catch (ClassNotFoundException ex) {
					throw new SerializationException("Unable to find class: " + className, ex);
				}
			}
		} catch (BufferUnderflowException ex) {
			throw new SerializationException("Buffer limit exceeded reading class ID.", ex);
		}
		RegisteredClass registeredClass = idToRegisteredClass.get(classID);
		if (registeredClass == null) throw new SerializationException("Encountered unregistered class ID: " + classID);
		if (TRACE) trace("kryo", "Read class " + classID + ": " + registeredClass.type.getName());
		return registeredClass;
	}

	/**
	 * Writes the object's class to the buffer, then uses the serializer registered for that class to write the object to the
	 * buffer.
	 * @param object Can be null (writes a special ID for a null object instead).
	 */
	public void writeClassAndObject (ByteBuffer buffer, Object object) {
		try {
			if (object == null) {
				buffer.put(ID_NULL_OBJECT);
				if (TRACE) trace("kryo", "Wrote object: null");
				return;
			}
			RegisteredClass registeredClass = writeClass(buffer, object.getClass());
			if (registeredClass == null) return;
			registeredClass.serializer.writeObjectData(buffer, object);
		} catch (SerializationException ex) {
			throw new SerializationException("Unable to serialize object of type: " + object.getClass().getName(), ex);
		} catch (BufferOverflowException ex) {
			throw new SerializationException("Buffer limit exceeded serializing object of type: " + object.getClass().getName(), ex);
		}
	}

	/**
	 * Uses the serializer registered for the object's class to write the object to the buffer.
	 * @param object Can be null (writes a special ID for a null object instead).
	 */
	public void writeObject (ByteBuffer buffer, Object object) {
		try {
			if (object == null) {
				buffer.put(ID_NULL_OBJECT);
				if (TRACE) trace("kryo", "Wrote object: null");
				return;
			}
			getRegisteredClass(object.getClass()).serializer.writeObject(buffer, object);
		} catch (SerializationException ex) {
			throw new SerializationException("Unable to serialize object of type: " + object.getClass().getName(), ex);
		} catch (BufferOverflowException ex) {
			throw new SerializationException("Buffer limit exceeded serializing object of type: " + object.getClass().getName(), ex);
		}
	}

	/**
	 * Uses the serializer registered for the object's class to write the object to the buffer.
	 * @param object Cannot be null.
	 */
	public void writeObjectData (ByteBuffer buffer, Object object) {
		try {
			getRegisteredClass(object.getClass()).serializer.writeObjectData(buffer, object);
		} catch (SerializationException ex) {
			throw new SerializationException("Unable to serialize object of type: " + object.getClass().getName(), ex);
		} catch (BufferOverflowException ex) {
			throw new SerializationException("Buffer limit exceeded serializing object of type: " + object.getClass().getName(), ex);
		}
	}

	/**
	 * Reads a class from the buffer and uses the serializer registered for that class to read an object from the buffer.
	 * @return The deserialized object, or null if the object read from the buffer was null.
	 */
	public Object readClassAndObject (ByteBuffer buffer) {
		RegisteredClass registeredClass = readClass(buffer);
		if (registeredClass == null) return null;
		try {
			return registeredClass.serializer.readObjectData(buffer, registeredClass.type);
		} catch (SerializationException ex) {
			throw new SerializationException("Unable to deserialize object of type: " + registeredClass.type.getName(), ex);
		} catch (BufferUnderflowException ex) {
			throw new SerializationException(
				"Buffer limit exceeded deserializing object of type: " + registeredClass.type.getName(), ex);
		}
	}

	/**
	 * Uses the serializer registered for the specified class to read an object from the buffer.
	 * @return The deserialized object, or null if the object read from the buffer was null.
	 */
	public <T> T readObject (ByteBuffer buffer, Class<T> type) {
		try {
			return getRegisteredClass(type).serializer.readObject(buffer, type);
		} catch (SerializationException ex) {
			throw new SerializationException("Unable to deserialize object of type: " + type.getName(), ex);
		} catch (BufferUnderflowException ex) {
			throw new SerializationException("Buffer limit exceeded deserializing object of type: " + type.getName(), ex);
		}
	}

	/**
	 * Uses the serializer registered for the specified class to read an object from the buffer.
	 * @return The deserialized object, never null.
	 */
	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		try {
			return getRegisteredClass(type).serializer.readObjectData(buffer, type);
		} catch (SerializationException ex) {
			throw new SerializationException("Unable to deserialize object of type: " + type.getName(), ex);
		} catch (BufferUnderflowException ex) {
			throw new SerializationException("Buffer limit exceeded deserializing object of type: " + type.getName(), ex);
		}
	}

	public void addListener (KryoListener listener) {
		if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
		synchronized (listenerLock) {
			KryoListener[] listeners = this.listeners;
			int n = listeners.length;
			for (int i = 0; i < n; i++)
				if (listener == listeners[i]) return;
			KryoListener[] newListeners = new KryoListener[n + 1];
			newListeners[0] = listener;
			System.arraycopy(listeners, 0, newListeners, 1, n);
			this.listeners = newListeners;
		}
		if (TRACE) trace("kryo", "Kryo listener added: " + listener.getClass().getName());
	}

	public void removeListener (KryoListener listener) {
		if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
		synchronized (listenerLock) {
			KryoListener[] listeners = this.listeners;
			int n = listeners.length;
			KryoListener[] newListeners = new KryoListener[n - 1];
			for (int i = 0, ii = 0; i < n; i++) {
				if (listener == listeners[i]) continue;
				if (ii == n - 1) return;
				newListeners[ii++] = listener;
			}
			System.arraycopy(listeners, 0, newListeners, 1, n);
			this.listeners = newListeners;
		}
		if (TRACE) trace("kryo", "Kryo listener removed: " + listener.getClass().getName());
	}

	/**
	 * Notifies all listeners that the remote entity with the specified ID will no longer be available.
	 * @see Context#getRemoteEntityID()
	 * @see #addListener(KryoListener)
	 */
	public void removeRemoteEntity (int remoteEntityID) {
		KryoListener[] listeners = this.listeners;
		if (TRACE) trace("kryo", "Remote ID removed: " + remoteEntityID);
		for (int i = 0, n = listeners.length; i < n; i++)
			listeners[i].remoteEntityRemoved(remoteEntityID);
	}

	/**
	 * Returns the thread local context for serialization and deserialization.
	 * @see Context
	 */
	static public Context getContext () {
		return contextThreadLocal.get();
	}

	/**
	 * Holds the registration information for a class.
	 */
	static public class RegisteredClass {
		final Class type;
		final int id;
		Serializer serializer;

		RegisteredClass (Class type, int id, Serializer serializer) {
			this.type = type;
			this.id = id;
			this.serializer = serializer;
		}

		public Class getType () {
			return type;
		}

		public Serializer getSerializer () {
			return serializer;
		}

		public void setSerializer (Serializer serializer) {
			this.serializer = serializer;
		}

		public int getID () {
			return id;
		}
	}
}
