
package com.esotericsoftware.kryo;

import java.util.Arrays;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Holds bytes for a string, only deserializing to a string as needed. This can be useful for high performance processing to avoid
 * deserializing strings, eg when data will just be written to another source. */
public class KryoString implements KryoSerializable {
	public byte[] bytes;

	private transient String value;

	public KryoString () {
	}

	public KryoString (String value) {
		setValue(value);
	}

	public KryoString (KryoString value) {
		this.bytes = value.bytes;
		this.value = value.value;
	}

	public void setValue (String value) {
		this.value = value;
		Output output = new Output(value.length() * 2, -1);
		output.writeString(value);
		bytes = output.toBytes();
	}

	public String getValue () {
		if (bytes == null) return null;
		if (value == null) value = new Input(bytes).readString();
		return value;
	}

	public String toString () {
		return getValue();
	}

	public void write (Kryo kryo, Output output) {
		if (bytes == null)
			output.writeByte(0);
		else
			output.writeBytes(bytes);
	}

	public void read (Kryo kryo, Input input) {
		int length = input.readInt(true);
		if (length == 0)
			bytes = null;
		else {
			int lengthLength = Output.intLength(length, true);
			bytes = new byte[lengthLength + length - 1];
			Output output = new Output(bytes);
			output.write(length);
			input.read(bytes, lengthLength, length - 1);
		}
	}

	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bytes);
		return result;
	}

	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		KryoString other = (KryoString)obj;
		if (!Arrays.equals(bytes, other.bytes)) return false;
		return true;
	}
}
