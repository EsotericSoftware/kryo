
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Allows implementing classes to perform their own serialization. Hand written serialization can be more efficient in some cases.
 * <p>
 * {@link Registration#getInstantiator()} is used to construct the class, which may be able to handle non-public and/or non-zero
 * argument constructors.
 * @author Nathan Sweet <misc@n4te.com> */
public interface KryoSerializable {
	public void write (Kryo kryo, Output output);

	public void read (Kryo kryo, Input input);
}
