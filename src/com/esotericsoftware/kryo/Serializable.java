
package com.esotericsoftware.kryo;

import java.lang.reflect.Constructor;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Allows implementing classes to perform their own serialization. Hand written serialization can be more efficient in some cases.
 * <p>
 * {@link Kryo#newInstance(Class)} is used to construct the class, so a zero argument constructor is required. If this constructor
 * is private, an attempt to access it via {@link Constructor#setAccessible(boolean)} will be made. */
public interface Serializable {
	public void write (Kryo kryo, Output output);

	public void read (Kryo kryo, Input input);
}
