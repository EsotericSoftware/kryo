
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Handles class registration, writing class identifiers to bytes, and reading class identifiers from bytes.
 * @author Nathan Sweet <misc@n4te.com> */
public interface ClassResolver {
	/** Sets the Kryo instance that this ClassResolver will be used for. This is called automatically by Kryo. */
	public void setKryo (Kryo kryo);

	/** Stores the specified registration.
	 * @see Kryo#register(Registration) */
	public Registration register (Registration registration);

	/** Called when an unregistered type is encountered and {@link Kryo#setRegistrationRequired(boolean)} is false. */
	public Registration registerImplicit (Class type);

	/** Returns the registration for the specified class, or null if the class is not registered. */
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
