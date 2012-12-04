
package com.esotericsoftware.kryo;

/** When references are enabled, this tracks objects that have already been read or written, provides an ID for objects that are
 * written, and looks up by ID objects that have been read.
 * @author Nathan Sweet <misc@n4te.com> */
public interface ReferenceResolver {
	/** Sets the Kryo instance that this ClassResolver will be used for. This is called automatically by Kryo. */
	public void setKryo (Kryo kryo);

	/** Returns an ID for the object if it has been written previously, otherwise returns -1. */
	public int getWrittenId (Object object);

	/** Returns a new ID for an object that is being written for the first time.
	 * @return The ID, which is stored more efficiently if it is positive and must not be -1 or -2. */
	public int addWrittenObject (Object object);

	/** Reserves the ID for the next object that will be read. This is called only the first time an object is encountered.
	 * @param type The type of object that will be read.
	 * @return The ID, which is stored more efficiently if it is positive and must not be -1 or -2. */
	public int nextReadId (Class type);

	/** Sets the ID for an object that has been read.
	 * @param id The ID from {@link #nextReadId(Class)}. */
	public void setReadObject (int id, Object object);

	/** Returns the object for the specified ID. The ID and object are guaranteed to have been previously passed in a call to
	 * {@link #setReadObject(int, Object)}. */
	public Object getReadObject (Class type, int id);

	/** Called by {@link Kryo#reset()}. */
	public void reset ();

	/** Returns true if references will be written for the specified type.
	 * @param type Will never be a primitive type, but may be a primitive type wrapper. */
	public boolean useReferences (Class type);
}
