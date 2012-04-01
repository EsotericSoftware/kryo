
package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public interface Serializable {
	public void write (Kryo kryo, Output output);

	public void read (Kryo kryo, Input input);
}
