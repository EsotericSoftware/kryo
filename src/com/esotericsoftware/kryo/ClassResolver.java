
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Handles class registration, writing class identifiers to bytes, and reading class identifiers from bytes.
 * @author Nathan Sweet <misc@n4te.com> */
public interface ClassResolver {
	/** Sets the Kryo instance that this ClassResolver will be used for. This is called automatically by Kryo. */
	public void setKryo (Kryo kryo);

	/** Registers the class using an automatically generated ID. The serializer is chosen automatically. If the class is already
	 * registered, the existing entry is updated with the new serializer. Registering a primitive also affects the corresponding
	 * primitive wrapper. */
	public Registration register (Class type);

	/** Registers the class using the specified ID. The serializer is chosen automatically. If the ID is already in use by the same
	 * type, the old entry is overwritten. If the ID is already in use by a different type, a {@link KryoException} is thrown. IDs
	 * must be the same at deserialization as they were for serialization. Registering a primitive also affects the corresponding
	 * primitive wrapper. */
	public Registration register (Class type, int id);

	/** Registers the class using an automatically generated ID. If the class is already registered, the existing entry is updated
	 * with the new serializer. Registering a primitive also affects the corresponding primitive wrapper. */
	public Registration register (Class type, Serializer serializer);

	/** Registers the class using the specified ID. If the ID is already in use by the same type, the old entry is overwritten. If
	 * the ID is already in use by a different type, a {@link KryoException} is thrown. IDs must be the same at deserialization as
	 * they were for serialization. Registering a primitive also affects the corresponding primitive wrapper. */
	public Registration register (Class type, Serializer serializer, int id);

	/** Stores the specified registration. This can be used to efficiently store per type information needed for serialization,
	 * accessible in serializers via {@link Kryo#getRegistration(Class)}. If the ID is already in use by the same type, the old
	 * entry is overwritten. If the ID is already in use by a different type, a {@link KryoException} is thrown. IDs must be the
	 * same at deserialization as they were for serialization. Registering a primitive also affects the corresponding primitive
	 * wrapper. */
	public Registration register (Registration registration);

	/** Returns the registration for the specified class.
	 * @throws IllegalArgumentException if the class is not registered and {@link Kryo#setRegistrationRequired(boolean)} is true. */
	public Registration getRegistration (Class type);

	/** Returns the registration for the specified ID, or null if no class is registered with that ID. */
	public Registration getRegistration (int classID);

	/** Writes a class and returns its registration.
	 * @param type May be null.
	 * @return Will be null if type is null. */
	public Registration writeClass (Output output, Class type);

	/** Reads a class and returns its registration.
	 * @return May be null. */
	public Registration readClass (Input input);

	/** Called by {@link Kryo#reset()}. */
	public void reset ();
}
