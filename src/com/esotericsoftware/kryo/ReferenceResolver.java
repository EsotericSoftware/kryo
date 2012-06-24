
package com.esotericsoftware.kryo;

/** When references are enabled, this tracks objects that have already been read or written, provides an ID for objects that are
 * written, and looks up by ID objects that have been read.
 * @author Nathan Sweet <misc@n4te.com> */
public interface ReferenceResolver {
	/** Sets the Kryo instance that this ClassResolver will be used for. This is called automatically by Kryo. */
	public void setKryo (Kryo kryo);

	/** Returns an ID for the object if it has been written previously, otherwise returns -1 if the object has not been encountered
	 * before. */
	public int getWrittenId (Object object);

	/** Returns a new ID for an object that is being written for the first time. IDs must be sequential, the ID returned will be the
	 * number of times this method has been called since {@link #reset()}. */
	public int addWrittenObject (Object object);

	/** Returns the next ID for the next object that will be read. Called the first time an object is encountered.
	 * @param type The type of object that will be read.
	 * @return The ID, which should be position and must not be 0 or 1. */
	public int getReadId (Class type);

	/** Adds an object that has been read for the first time.
	 * @param id The ID from {@link #getReadId(Class)}. */
	public void addReadObject (int id, Object object);

	/** Returns the object for the specified ID. The ID and object are guaranteed to have been previously passed in a call to
	 * {@link #addReadObject(int, Object)}. */
	public Object getReadObject (Class type, int id);

	/** Called by {@link Kryo#reset()}. */
	public void reset ();

	/** Returns true if references will be written for the specified type.
	 * @param type Will never be a primitive type, but may be a primitive type wrapper. */
	public boolean useReferences (Class type);
}
