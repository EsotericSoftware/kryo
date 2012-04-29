
package com.esotericsoftware.kryo;

import java.util.Arrays;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Holds bytes for a string, only deserializing to a string as needed. This can be useful for high performance processing to avoid
 * deserializing strings unless they are actually needed.
 * <p>
 * Serialized bytes for !KryoString and String are identical and interchangeable. Eg, the serialized bytes for a class with a
 * String field will be deserialized correctly if the field's type is changed to !KryoString, and vice versa. However, if a string
 * is written as null and read as a KryoString, the KryoString will not be null but will contain a null string. If a KryoString
 * containing a null string is written and read back as a string, the string will be null. */
public final class KryoString {
	public byte[] bytes;

	private transient String value;

	public KryoString () {
	}

	/** @param value May be null. */
	public KryoString (String value) {
		setValue(value);
	}

	public KryoString (KryoString value) {
		if (value == null) throw new IllegalArgumentException("value cannot be null.");
		this.bytes = value.bytes;
		this.value = value.value;
	}

	/** @param value May be null. */
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
