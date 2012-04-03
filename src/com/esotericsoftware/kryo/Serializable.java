
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Allows implementing classes to perform their own serialization. Hand written serialization can be more efficient in some cases. */
public interface Serializable {
	public void write (Kryo kryo, Output output);

	public void read (Kryo kryo, Input input);
}
