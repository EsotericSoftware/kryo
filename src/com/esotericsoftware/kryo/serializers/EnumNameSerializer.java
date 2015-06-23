package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Enum serialize with enum's name.
 * <p />
 * This serializer prevents reading wrong values when enum's order changed.
 *
 * @author KwonNam Son <kwon37xi@gmail.com>
 */
public class EnumNameSerializer extends Serializer<Enum> {
	{
		setImmutable(true);
		setAcceptsNull(true);
	}

	private Class<? extends Enum> enumType;

	public EnumNameSerializer(Class<? extends Enum> type) {
		this.enumType = type;
	}

	public void write(Kryo kryo, Output output, Enum object) {
		if (object == null) {
			output.writeString(null);
			return;
		}
		output.writeString(object.name());
	}

	public Enum read(Kryo kryo, Input input, Class<Enum> type) {
		String name = input.readString();
		if (name == null) {
			return null;
		}

		try {
			return Enum.valueOf(enumType, name);
		} catch (IllegalArgumentException e) {
			throw new KryoException("Invalid name for enum \"" + type.getName() + "\": " + name, e);
		}
	}
}