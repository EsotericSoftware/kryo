
package com.esotericsoftware.kryo;

/** Allows implementing classes to perform their own copying. Hand written copying can be more efficient in some cases.
 * <p>
 * These methods are used instead of the registered serializer methods {@link Serializer#createCopy(Kryo, Object)} and
 * {@link Serializer#copy(Kryo, Object, Object)}.
 * @author Nathan Sweet <misc@n4te.com> */
public interface KryoCopyable<T> {
	/** Creates a copy of this object.
	 * @see Serializer#createCopy(Kryo, Object) */
	public T createCopy (Kryo kryo);

	/** Configures the copy to have the same values as this object.
	 * @see Serializer#copy(Kryo, Object, Object) */
	public void copy (Kryo kryo, T copy);
}
