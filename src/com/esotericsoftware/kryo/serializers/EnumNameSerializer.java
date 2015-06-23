
package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Serializes enums using the enum's name. This prevents invalidating previously serialized byts when the enum order changes.
 * @author KwonNam Son <kwon37xi@gmail.com> */
public class EnumNameSerializer extends Serializer<Enum> {
	private Class<? extends Enum> enumType;

	public EnumNameSerializer (Class<? extends Enum> type) {
		this.enumType = type;
		setImmutable(true);
		setAcceptsNull(true);
	}

	public void write (Kryo kryo, Output output, Enum object) {
		if (object == null) {
			output.writeString(null);
			return;
		}
		output.writeString(object.name());
	}

	public Enum read (Kryo kryo, Input input, Class<Enum> type) {
		String name = input.readString();
		if (name == null) return null;
		try {
			return Enum.valueOf(enumType, name);
		} catch (IllegalArgumentException e) {
			throw new KryoException("Invalid name for enum \"" + enumType.getName() + "\": " + name, e);
		}
	}
}
