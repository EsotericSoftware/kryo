
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

	/** Returns a new ID for an object that is being written for the first time. */
	public int addWrittenObject (Object object);

	/** Adds an object that has been read for the first time. */
	public void addReadObject (int id, Object object);

	/** Returns the object for the specified ID, or null no object with that ID has been read previously. */
	public Object getReadObject (int id);

	/** Called by {@link Kryo#reset()}. */
	public void reset ();

	/** Returns true if references will be written for the specified type.
	 * @param type Will never be a primitive type, but may be a primitive type wrapper. */
	public boolean useReferences (Class type);
}
