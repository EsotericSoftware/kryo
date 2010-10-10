
package com.esotericsoftware.kryo;

import static com.esotericsoftware.minlog.Log.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
	static public final String version = "1.03";

	static private final byte ID_NULL_OBJECT = 0;
	static private final int ID_CLASS_NAME = 16383;

	static private ThreadLocal<Context> contextThreadLocal = new ThreadLocal<Context>() {
		protected Context initialValue () {
			return new Context();
		}
	};

	private final ConcurrentHashMap<Integer, RegisteredClass> idToRegisteredClass = new ConcurrentHashMap(64);
	private final ConcurrentHashMap<Class, RegisteredClass> classToRegisteredClass = new ConcurrentHashMap(64);
	private AtomicInteger nextClassID = new AtomicInteger(1);
	private Object listenerLock = new Object();
	private volatile Listener[] listeners = {};
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
	 * If <tt>useClassNameString</tt> is true, the first time an object of the specified type is encountered, the class name String
	 * will be written to the serialized bytes. Each appearance in the graph after the first is stored as an integer ordinal.
	 * <p>
	 * If <tt>useClassNameString</tt> is false, the class is assigned an ordinal which will be written to the serialized bytes for
	 * objects of the specified type. This is more efficient than using the class name String, but has the drawback that the exact
	 * same classes must be registered in exactly the same order when the class is deserialized.
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
	 * Registers a class with the ordinal of the specified registered class. This is useful when many classes can be serialized
	 * with the same serializer instance, such as when code generation is being used to wrap the actual class being serialized.
	 */
	public void register (Class type, RegisteredClass registeredClass) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (registeredClass == null) throw new IllegalArgumentException("registeredClass cannot be null.");
		classToRegisteredClass.put(type, registeredClass);
		if (TRACE) {
			String name = type.getName();
			if (type.isArray()) {
				Class elementClass = ArraySerializer.getElementClass(type);
				StringBuilder buffer = new StringBuilder(16);
				for (int i = 0, n = ArraySerializer.getDimensionCount(type); i < n; i++)
					buffer.append("[]");
				name = elementClass.getName() + buffer;
			}
			if (registeredClass.id == ID_CLASS_NAME)
				trace("kryo", "Registered class name: " + name + " (" + registeredClass.serializer.getClass().getName() + ")");
			else
				trace("kryo", "Registered class ID " + registeredClass.id + ": " + name + " ("
					+ registeredClass.serializer.getClass().getName() + ")");
		}
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

	public ClassLoader getClassLoader () {
		return classLoader;
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
		if (type == null) {
			try {
				buffer.put(ID_NULL_OBJECT);
				if (TRACE) trace("kryo", "Wrote object: null");
				return null;
			} catch (BufferOverflowException ex) {
				throw new SerializationException("Buffer limit exceeded writing null object.", ex);
			}
		}
		try {
			RegisteredClass registeredClass = getRegisteredClass(type);
			IntSerializer.put(buffer, registeredClass.id, true);
			if (registeredClass.id == ID_CLASS_NAME) {
				Context context = Kryo.getContext();
				ClassReferences references = (ClassReferences)context.getTemp("classReferences");
				if (references == null) {
					// Use non-temporary storage to avoid repeated allocation.
					references = (ClassReferences)context.get("classReferences");
					if (references == null)
						context.put("classReferences", references = new ClassReferences());
					else
						references.reset();
					context.putTemp("classReferences", references);
				}
				Integer reference = references.classToReference.get(type);
				if (reference != null) {
					IntSerializer.put(buffer, reference, true);
					if (TRACE) trace("kryo", "Wrote class name reference " + reference + ": " + type.getName());
					return registeredClass;
				}
				buffer.put((byte)0);
				references.classToReference.put(type, references.referenceCount++);
				StringSerializer.put(buffer, type.getName());
				if (TRACE) trace("kryo", "Wrote class name: " + type.getName());
			} else {
				if (TRACE) trace("kryo", "Wrote class " + registeredClass.id + ": " + type.getName());
			}
			return registeredClass;
		} catch (SerializationException ex) {
			if (ex.causedBy(BufferOverflowException.class))
				throw new SerializationException("Buffer limit exceeded writing class ID: " + type, ex);
			throw ex;
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
				Context context = Kryo.getContext();
				ClassReferences references = (ClassReferences)context.getTemp("classReferences");
				if (references == null) {
					// Use non-temporary storage to avoid repeated allocation.
					references = (ClassReferences)context.get("classReferences");
					if (references == null)
						context.put("classReferences", references = new ClassReferences());
					else
						references.reset();
					context.putTemp("classReferences", references);
				}

				Class type;
				int reference = IntSerializer.get(buffer, true);
				if (reference != 0) {
					type = (Class)references.referenceToClass.get(reference);
					if (type == null) throw new SerializationException("Invalid class name reference: " + reference);
					if (TRACE) trace("kryo", "Read class name reference " + reference + ": " + type.getName());
				} else {
					String className = StringSerializer.get(buffer);
					if (TRACE) trace("kryo", "Read class name: " + className);
					try {
						type = Class.forName(className, false, classLoader);
					} catch (ClassNotFoundException ex) {
						throw new SerializationException("Unable to find class: " + className, ex);
					}
					references.referenceToClass.put(references.referenceCount++, type);
				}
				return getRegisteredClass(type);
			}
		} catch (SerializationException ex) {
			if (ex.causedBy(BufferUnderflowException.class))
				throw new SerializationException("Buffer limit exceeded reading class ID.", ex);
			throw ex;
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
	 * @throws SerializationException if an error occurred during serialization.
	 */
	public void writeClassAndObject (ByteBuffer buffer, Object object) {
		if (object == null) {
			try {
				buffer.put(ID_NULL_OBJECT);
				if (TRACE) trace("kryo", "Wrote object: null");
				return;
			} catch (BufferOverflowException ex) {
				throw new SerializationException("Buffer limit exceeded writing null object.", ex);
			}
		}
		RegisteredClass registeredClass = writeClass(buffer, object.getClass());
		if (registeredClass == null) return;
		Context context = getContext();
		context.objectGraphLevel++;
		try {
			registeredClass.serializer.writeObjectData(buffer, object);
		} catch (SerializationException ex) {
			if (ex.causedBy(BufferOverflowException.class))
				throw new SerializationException("Buffer limit exceeded writing object of type: " + object.getClass().getName(), ex);
			throw new SerializationException("Unable to serialize object of type: " + object.getClass().getName(), ex);
		} catch (BufferOverflowException ex) {
			throw new SerializationException("Buffer limit exceeded writing object of type: " + object.getClass().getName(), ex);
		} finally {
			context.objectGraphLevel--;
			if (context.objectGraphLevel == 0) context.reset();
		}
	}

	/**
	 * Uses the serializer registered for the object's class to write the object to the buffer.
	 * @param object Can be null (writes a special ID for a null object instead).
	 * @throws SerializationException if an error occurred during serialization.
	 */
	public void writeObject (ByteBuffer buffer, Object object) {
		if (object == null) {
			try {
				buffer.put(ID_NULL_OBJECT);
				if (TRACE) trace("kryo", "Wrote object: null");
				return;
			} catch (BufferOverflowException ex) {
				throw new SerializationException("Buffer limit exceeded writing null object.", ex);
			}
		}
		Context context = getContext();
		context.objectGraphLevel++;
		try {
			getRegisteredClass(object.getClass()).serializer.writeObject(buffer, object);
		} catch (SerializationException ex) {
			if (ex.causedBy(BufferOverflowException.class))
				throw new SerializationException("Buffer limit exceeded writing object of type: " + object.getClass().getName(), ex);
			throw new SerializationException("Unable to serialize object of type: " + object.getClass().getName(), ex);
		} catch (BufferOverflowException ex) {
			throw new SerializationException("Buffer limit exceeded writing object of type: " + object.getClass().getName(), ex);
		} finally {
			context.objectGraphLevel--;
			if (context.objectGraphLevel == 0) context.reset();
		}
	}

	/**
	 * Uses the serializer registered for the object's class to write the object to the buffer.
	 * @param object Cannot be null.
	 * @throws SerializationException if an error occurred during serialization.
	 */
	public void writeObjectData (ByteBuffer buffer, Object object) {
		Context context = getContext();
		context.objectGraphLevel++;
		try {
			getRegisteredClass(object.getClass()).serializer.writeObjectData(buffer, object);
		} catch (SerializationException ex) {
			if (ex.causedBy(BufferOverflowException.class))
				throw new SerializationException("Buffer limit exceeded writing object of type: " + object.getClass().getName(), ex);
			throw new SerializationException("Unable to serialize object of type: " + object.getClass().getName(), ex);
		} catch (BufferOverflowException ex) {
			throw new SerializationException("Buffer limit exceeded writing object of type: " + object.getClass().getName(), ex);
		} finally {
			context.objectGraphLevel--;
			if (context.objectGraphLevel == 0) context.reset();
		}
	}

	/**
	 * Reads a class from the buffer and uses the serializer registered for that class to read an object from the buffer.
	 * @return The deserialized object, or null if the object read from the buffer was null.
	 * @throws SerializationException if an error occurred during deserialization.
	 */
	public Object readClassAndObject (ByteBuffer buffer) {
		RegisteredClass registeredClass = readClass(buffer);
		if (registeredClass == null) return null;
		Context context = getContext();
		context.objectGraphLevel++;
		try {
			return registeredClass.serializer.readObjectData(buffer, registeredClass.type);
		} catch (SerializationException ex) {
			if (ex.causedBy(BufferUnderflowException.class))
				throw new SerializationException("Buffer limit exceeded reading object of type: " + registeredClass.type.getName(),
					ex);
			throw new SerializationException("Unable to deserialize object of type: " + registeredClass.type.getName(), ex);
		} catch (BufferUnderflowException ex) {
			throw new SerializationException("Buffer limit exceeded reading object of type: " + registeredClass.type.getName(), ex);
		} finally {
			context.objectGraphLevel--;
			if (context.objectGraphLevel == 0) context.reset();
		}
	}

	/**
	 * Uses the serializer registered for the specified class to read an object from the buffer.
	 * @return The deserialized object, or null if the object read from the buffer was null.
	 * @throws SerializationException if an error occurred during deserialization.
	 */
	public <T> T readObject (ByteBuffer buffer, Class<T> type) {
		Context context = getContext();
		context.objectGraphLevel++;
		try {
			return getRegisteredClass(type).serializer.readObject(buffer, type);
		} catch (SerializationException ex) {
			if (ex.causedBy(BufferUnderflowException.class))
				throw new SerializationException("Buffer limit exceeded reading object of type: " + type.getName(), ex);
			throw new SerializationException("Unable to deserialize object of type: " + type.getName(), ex);
		} catch (BufferUnderflowException ex) {
			throw new SerializationException("Buffer limit exceeded reading object of type: " + type.getName(), ex);
		} finally {
			context.objectGraphLevel--;
			if (context.objectGraphLevel == 0) context.reset();
		}
	}

	/**
	 * Uses the serializer registered for the specified class to read an object from the buffer.
	 * @return The deserialized object, never null.
	 * @throws SerializationException if an error occurred during deserialization.
	 */
	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		Context context = getContext();
		context.objectGraphLevel++;
		try {
			return getRegisteredClass(type).serializer.readObjectData(buffer, type);
		} catch (SerializationException ex) {
			if (ex.causedBy(BufferUnderflowException.class))
				throw new SerializationException("Buffer limit exceeded reading object of type: " + type.getName(), ex);
			throw new SerializationException("Unable to deserialize object of type: " + type.getName(), ex);
		} catch (BufferUnderflowException ex) {
			throw new SerializationException("Buffer limit exceeded reading object of type: " + type.getName(), ex);
		} finally {
			context.objectGraphLevel--;
			if (context.objectGraphLevel == 0) context.reset();
		}
	}

	public void addListener (Listener listener) {
		if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
		synchronized (listenerLock) {
			Listener[] listeners = this.listeners;
			int n = listeners.length;
			for (int i = 0; i < n; i++)
				if (listener == listeners[i]) return;
			Listener[] newListeners = new Listener[n + 1];
			newListeners[0] = listener;
			System.arraycopy(listeners, 0, newListeners, 1, n);
			this.listeners = newListeners;
		}
		if (TRACE) trace("kryo", "Kryo listener added: " + listener.getClass().getName());
	}

	public void removeListener (Listener listener) {
		if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
		synchronized (listenerLock) {
			Listener[] listeners = this.listeners;
			int n = listeners.length;
			if (n == 0) return;
			Listener[] newListeners = new Listener[n - 1];
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
	 * @see #addListener(Listener)
	 */
	public void removeRemoteEntity (int remoteEntityID) {
		Listener[] listeners = this.listeners;
		if (TRACE) trace("kryo", "Remote ID removed: " + remoteEntityID);
		for (int i = 0, n = listeners.length; i < n; i++)
			listeners[i].remoteEntityRemoved(remoteEntityID);
	}

	/**
	 * Returns an instance of the specified class. Serializers that want to allow object construction to be customized by a
	 * subclass should use {@link Serializer#newInstance(Kryo, Class)} instead of calling this method directly.
	 * @throws SerializationException if the class could not be constructed.
	 */
	public <T> T newInstance (Class<T> type) {
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
				throw new SerializationException("Class cannot be created (missing no-arg constructor): " + type.getName(), ex);
			} catch (Exception privateConstructorException) {
				ex = privateConstructorException;
			}
			throw new SerializationException("Error constructing instance of class: " + type.getName(), ex);
		}
	}

	/**
	 * Returns true if the specified type is final, or if it is an array of a final type.
	 */
	static public boolean isFinal (Class type) {
		if (type.isArray()) return Modifier.isFinal(ArraySerializer.getElementClass(type).getModifiers());
		return Modifier.isFinal(type.getModifiers());
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

	/**
	 * Provides notification of {@link Kryo} events.
	 */
	static public interface Listener {
		/**
		 * Called when a remote entity is no longer available. This allows, for example, a context to release any resources it may
		 * be storing for the entity.
		 * @see Context#getRemoteEntityID()
		 * @see Kryo#removeListener(Listener)
		 */
		public void remoteEntityRemoved (int id);
	}

	static class ClassReferences {
		public HashMap<Class, Integer> classToReference = new HashMap();
		public IntHashMap referenceToClass = new IntHashMap();
		public int referenceCount = 1;

		public void reset () {
			classToReference.clear();
			referenceToClass.clear();
			referenceCount = 1;
		}
	}
}
