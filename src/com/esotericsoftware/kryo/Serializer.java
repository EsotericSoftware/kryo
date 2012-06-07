
package com.esotericsoftware.kryo;

import java.lang.reflect.Type;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Reads and writes objects to and from bytes.
 * @author Nathan Sweet <misc@n4te.com> */
public abstract class Serializer<T> {
	private boolean acceptsNull, immutable;

	public Serializer () {
	}

	/** @see #setAcceptsNull(boolean) */
	public Serializer (boolean acceptsNull) {
		this.acceptsNull = acceptsNull;
	}

	/** @see #setAcceptsNull(boolean)
	 * @see #setImmutable(boolean) */
	public Serializer (boolean acceptsNull, boolean immutable) {
		this.acceptsNull = acceptsNull;
		this.immutable = immutable;
	}

	/** Writes the bytes for the object to the output.
	 * <p>
	 * This method should not be called directly, instead this serializer can be passed to {@link Kryo} write methods that accept a
	 * serialier.
	 * @param object May be null if {@link #getAcceptsNull()} is true. */
	abstract public void write (Kryo kryo, Output output, T object);

	/** Creates a new object of the specified type. The object may be uninitialized. This method may read from input to populate the
	 * object, but it must not call {@link Kryo} methods to deserialize nested objects. That must be done in
	 * {@link #read(Kryo, Input, Object)}. The default implementation uses {@link Kryo#newInstance(Class)} to create a new object.
	 * <p>
	 * This method should not be called directly, instead this serializer can be passed to {@link Kryo} read methods that accept a
	 * serialier.
	 * @return May be null if {@link #getAcceptsNull()} is true. */
	public T create (Kryo kryo, Input input, Class<T> type) {
		return kryo.newInstance(type);
	}

	/** Populates the object. This method may call {@link Kryo} methods to deserialize nested objects, unlike
	 * {@link #create(Kryo, Input, Class)}. The default implementation is empty.
	 * <p>
	 * Any serializer that uses {@link Kryo} to serialize a nested object may need to be reentrant.
	 * <p>
	 * This method should not be called directly, instead this serializer can be passed to {@link Kryo} read methods that accept a
	 * serialier. */
	public void read (Kryo kryo, Input input, T object) {
	}

	public boolean getAcceptsNull () {
		return acceptsNull;
	}

	/** If true, this serializer will handle writing and reading null values. If false, the Kryo framework handles null values. */
	public void setAcceptsNull (boolean acceptsNull) {
		this.acceptsNull = acceptsNull;
	}

	public boolean isImmutable () {
		return immutable;
	}

	/** If true, {@link #createCopy(Kryo, Object)} will return the original object. */
	public void setImmutable (boolean immutable) {
		this.immutable = immutable;
	}

	/** Sets the generic types of the field this serializer will be used for. This only applies to the next call to read or write.
	 * The default implementation does nothing. Subclasses may use this method for more efficient serialization, eg to use the same
	 * type for all items in a list. */
	public void setGenerics (Kryo kryo, Type[] generics) {
	}

	/** Creates a copy of the specified object. The object may be uninitialized or this method may populate the copy, but it must
	 * not call {@link Kryo} methods to copy nested objects. That must be done in {@link #copy(Kryo, Object, Object)}. The default
	 * implementation returns the original if {@link #isImmutable()} is true, else throws {@link KryoException}. Subclasses should
	 * override this method if needed to support {@link Kryo#copy(Object)}.
	 * <p>
	 * This method should not be called directly, instead this serializer can be passed to {@link Kryo} copy methods that accept a
	 * serialier.
	 * @return May be null if {@link #getAcceptsNull()} is true. */
	public T createCopy (Kryo kryo, T original) {
		if (immutable) return original;
		throw new KryoException("Serializer does not support copy: " + getClass().getName());
	}

	/** Configures the copy to have the same values as the original. This method may call {@link Kryo} methods to copy nested
	 * objects, unlike {@link #createCopy(Kryo, Object)}. The default implementation is empty.
	 * <p>
	 * Any serializer that uses {@link Kryo} to copy a nested object may need to be reentrant.
	 * <p>
	 * This method should not be called directly, instead this serializer can be passed to {@link Kryo} copy methods that accept a
	 * serialier. */
	public void copy (Kryo kryo, T original, T copy) {
	}
}
