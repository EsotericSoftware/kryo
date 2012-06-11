
package com.esotericsoftware.kryo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.IdentityObjectIntMap;
import com.esotericsoftware.kryo.util.IntMap;
import com.esotericsoftware.kryo.util.ObjectMap;

import static com.esotericsoftware.kryo.Util.*;
import static com.esotericsoftware.minlog.Log.*;

public class DefaultClassResolver implements ClassResolver {
	static public final byte NAME = -1;

	protected Kryo kryo;

	protected final IntMap<Registration> idToRegistration = new IntMap();
	protected final ObjectMap<Class, Registration> classToRegistration = new ObjectMap();
	protected int nextRegisterID;

	protected final IdentityObjectIntMap<Class> classToNameId = new IdentityObjectIntMap();
	protected final IntMap<Class> nameIdToClass = new IntMap();
	protected final ObjectMap<String, Class> nameToClass = new ObjectMap();
	protected int nextNameId;

	private Class memoizedType;
	private Registration memoizedRegistration;

	public void setKryo (Kryo kryo) {
		this.kryo = kryo;
	}

	/** Registers the class using the next available, lowest integer ID and the {@link Kryo#getDefaultSerializer(Class) default
	 * serializer}. Because the ID assigned is affected by the IDs registered before it, the order classes are registered is
	 * important when using this method. The order must be the same at deserialization as it was for serialization. */
	public Registration register (Class type) {
		return register(type, kryo.getDefaultSerializer(type));
	}

	/** Registers the class using the specified ID and the {@link Kryo#getDefaultSerializer(Class) default serializer}. IDs are
	 * written with {@link Output#writeInt(int, boolean)} called with true, so smaller positive integers use fewer bytes.
	 * @param id Must not be -1 or -2. */
	public Registration register (Class type, int id) {
		return register(type, kryo.getDefaultSerializer(type), id);
	}

	/** Registers the class using the next available, lowest integer ID. Because the ID assigned is affected by the IDs registered
	 * before it, the order classes are registered is important when using this method. The order must be the same at
	 * deserialization as it was for serialization. */
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

	/** IDs are written with {@link Output#writeInt(int, boolean)} called with true, so smaller positive integers use fewer bytes.
	 * @param id Must not be -1 or -2. */
	public Registration register (Class type, Serializer serializer, int id) {
		if (id == -1 || id == -2) throw new IllegalArgumentException("id cannot be -1 or -2");
		return register(new Registration(type, serializer, id));
	}

	/** IDs are written with {@link Output#writeInt(int, boolean)} called with true, so smaller positive integers use fewer bytes.
	 * @param registration The id must not be -1 or -2. */
	public Registration register (Registration registration) {
		if (registration == null) throw new IllegalArgumentException("registration cannot be null.");
		int id = registration.getId();
		if (id == -1 || id == -2) throw new IllegalArgumentException("id cannot be -1 or -2");

		try {
			Registration existing = getRegistration(registration.getType());
			if (existing != null && existing.getType() != registration.getType()) {
				throw new KryoException("An existing registration with a different type already uses ID: " + registration.getId()
					+ "\nExisting registration: " + existing + "\nUnable to set registration: " + registration);
			}
		} catch (Exception ignored) {
		}

		registerInternal(registration);
		return registration;
	}

	protected Registration registerInternal (Registration registration) {
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

	/** If the class is not registered and {@link Kryo#setRegistrationRequired(boolean)} is false, it is automatically registered
	 * using the {@link Kryo#addDefaultSerializer(Class, Class) default serializer}. */
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
			} else
				registration = registerImplicit(type);
		}
		memoizedType = type;
		memoizedRegistration = registration;
		return registration;
	}

	protected Registration registerImplicit (Class type) {
		if (kryo.isRegistrationRequired()) {
			throw new IllegalArgumentException("Class is not registered: " + className(type)
				+ "\nNote: To register this class use: kryo.register(" + className(type) + ".class);");
		}
		return registerInternal(new Registration(type, kryo.getDefaultSerializer(type), NAME));
	}

	public Registration getRegistration (int classID) {
		return idToRegistration.get(classID);
	}

	public Registration writeClass (Output output, Class type) {
		if (type == null) {
			if (TRACE || (DEBUG && kryo.getDepth() == 1)) log("Write", null);
			output.writeByte(Kryo.NULL);
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
	}

	public Registration readClass (Input input) {
		int classID = input.readInt(true);
		switch (classID) {
		case Kryo.NULL:
			if (TRACE || (DEBUG && kryo.getDepth() == 1)) log("Read", null);
			return null;
		case NAME + 2: // Offset for NAME and NULL.
			int nameId = input.readInt(true);
			Class type = nameIdToClass.get(nameId);
			if (type == null) {
				// Only read the class name the first time encountered in object graph.
				String className = input.readString();
				type = nameToClass.get(className);
				if (type == null) {
					try {
						type = Class.forName(className, false, kryo.getClassLoader());
					} catch (ClassNotFoundException ex) {
						throw new KryoException("Unable to find class: " + className, ex);
					}
					nameToClass.put(className, type);
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
	}

	public void reset () {
		if (!kryo.isRegistrationRequired()) {
			classToNameId.clear();
			nameIdToClass.clear();
			nextNameId = 0;
		}
	}
}
