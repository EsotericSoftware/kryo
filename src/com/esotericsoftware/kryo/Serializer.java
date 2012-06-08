
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

	/** Reads bytes and returns a new object of the specified concrete type.
	 * <p>
	 * Before Kryo can be used to read child objects, {@link Kryo#reference(Object)} must be called with the parent object to
	 * ensure it can be referenced by the child objects. Any serializer that uses {@link Kryo} to read a child object may need to
	 * be reentrant.
	 * <p>
	 * This method should not be called directly, instead this serializer can be passed to {@link Kryo} read methods that accept a
	 * serialier.
	 * @return May be null if {@link #getAcceptsNull()} is true. */
	abstract public T read (Kryo kryo, Input input, Class<T> type);

	public boolean getAcceptsNull () {
		return acceptsNull;
	}

	/** If true, this serializer will handle writing and reading null values. If false, the Kryo framework handles null values and
	 * the serializer will never receive null. This can be set to true on a serializer that does not accept nulls if it is known
	 * that the serializer will never encounter null. This will prevent the framework from writing a byte to denote null. */
	public void setAcceptsNull (boolean acceptsNull) {
		this.acceptsNull = acceptsNull;
	}

	public boolean isImmutable () {
		return immutable;
	}

	/** If true, {@link #copy(Kryo, Object)} will return the original object. */
	public void setImmutable (boolean immutable) {
		this.immutable = immutable;
	}

	/** Sets the generic types of the field this serializer will be used for. This only applies to the next call to read or write.
	 * The default implementation does nothing. Subclasses may use this method for more efficient serialization, eg to use the same
	 * type for all items in a list. */
	public void setGenerics (Kryo kryo, Type[] generics) {
	}

	/** Returns a copy of the specified object. The default implementation returns the original if {@link #isImmutable()} is true,
	 * else throws {@link KryoException}. Subclasses should override this method if needed to support {@link Kryo#copy(Object)}.
	 * <p>
	 * Before Kryo can be used to copy child objects, {@link Kryo#reference(Object)} must be called with the copy to ensure it can
	 * be referenced by the child objects. Any serializer that uses {@link Kryo} to copy a child object may need to be reentrant.
	 * <p>
	 * This method should not be called directly, instead this serializer can be passed to {@link Kryo} copy methods that accept a
	 * serialier. */
	public T copy (Kryo kryo, T original) {
		if (immutable) return original;
		throw new KryoException("Serializer does not support copy: " + getClass().getName());
	}
}
