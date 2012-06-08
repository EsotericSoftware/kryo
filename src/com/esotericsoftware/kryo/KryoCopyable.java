
package com.esotericsoftware.kryo;

/** Allows implementing classes to perform their own copying. Hand written copying can be more efficient in some cases.
 * <p>
 * This method is used instead of the registered serializer {@link Serializer#copy(Kryo, Object)} method.
 * @author Nathan Sweet <misc@n4te.com> */
public interface KryoCopyable<T> {
	/** Returns a copy that has the same values as this object. Before Kryo can be used to copy child objects,
	 * {@link Kryo#reference(Object)} must be called with the copy to ensure it can be referenced by the child objects.
	 * @see Serializer#copy(Kryo, Object) */
	public T copy (Kryo kryo);
}
