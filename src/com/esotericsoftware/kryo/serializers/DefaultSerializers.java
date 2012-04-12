
package com.esotericsoftware.kryo.serializers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Currency;
import java.util.Date;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializable;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class DefaultSerializers {
	static public class BooleanSerializer extends Serializer<Boolean> {
		public void write (Kryo kryo, Output output, Boolean object) {
			output.writeBoolean(object);
		}

		public Boolean create (Kryo kryo, Input input, Class<Boolean> type) {
			return input.readBoolean();
		}
	}

	static public class ByteSerializer extends Serializer<Byte> {
		public void write (Kryo kryo, Output output, Byte object) {
			output.writeByte(object);
		}

		public Byte create (Kryo kryo, Input input, Class<Byte> type) {
			return input.readByte();
		}
	}

	static public class CharSerializer extends Serializer<Character> {
		public void write (Kryo kryo, Output output, Character object) {
			output.writeChar(object);
		}

		public Character create (Kryo kryo, Input input, Class<Character> type) {
			return input.readChar();
		}
	}

	static public class ShortSerializer extends Serializer<Short> {
		public void write (Kryo kryo, Output output, Short object) {
			output.writeShort(object);
		}

		public Short create (Kryo kryo, Input input, Class<Short> type) {
			return input.readShort();
		}
	}

	static public class IntSerializer extends Serializer<Integer> {
		public void write (Kryo kryo, Output output, Integer object) {
			output.writeInt(object, false);
		}

		public Integer create (Kryo kryo, Input input, Class<Integer> type) {
			return input.readInt(false);
		}
	}

	static public class LongSerializer extends Serializer<Long> {
		public void write (Kryo kryo, Output output, Long object) {
			output.writeLong(object, false);
		}

		public Long create (Kryo kryo, Input input, Class<Long> type) {
			return input.readLong(false);
		}
	}

	static public class FloatSerializer extends Serializer<Float> {
		public void write (Kryo kryo, Output output, Float object) {
			output.writeFloat(object);
		}

		public Float create (Kryo kryo, Input input, Class<Float> type) {
			return input.readFloat();
		}
	}

	static public class DoubleSerializer extends Serializer<Double> {
		public void write (Kryo kryo, Output output, Double object) {
			output.writeDouble(object);
		}

		public Double create (Kryo kryo, Input input, Class<Double> type) {
			return input.readDouble();
		}
	}

	static public class StringSerializer extends Serializer<String> {
		public void write (Kryo kryo, Output output, String object) {
			output.writeString(object);
		}

		public String create (Kryo kryo, Input input, Class<String> type) {
			return input.readString();
		}
	}

	static public class ByteArraySerializer extends Serializer<byte[]> {
		public void write (Kryo kryo, Output output, byte[] object) {
			output.writeInt(object.length, true);
			output.writeBytes(object);
		}

		public byte[] create (Kryo kryo, Input input, Class<byte[]> type) {
			return input.readBytes(input.readInt(true));
		}
	}

	static public class BigIntegerSerializer extends Serializer<BigInteger> {
		public void write (Kryo kryo, Output output, BigInteger object) {
			BigInteger value = (BigInteger)object;
			byte[] bytes = value.toByteArray();
			output.writeInt(bytes.length, true);
			output.writeBytes(bytes);
		}

		public BigInteger create (Kryo kryo, Input input, Class<BigInteger> type) {
			int length = input.readInt(true);
			byte[] bytes = input.readBytes(length);
			return new BigInteger(bytes);
		}
	}

	static public class BigDecimalSerializer extends Serializer<BigDecimal> {
		private BigIntegerSerializer bigIntegerSerializer = new BigIntegerSerializer();

		public void write (Kryo kryo, Output output, BigDecimal object) {
			BigDecimal value = (BigDecimal)object;
			bigIntegerSerializer.write(kryo, output, value.unscaledValue());
			output.writeInt(value.scale(), false);
		}

		public BigDecimal create (Kryo kryo, Input input, Class<BigDecimal> type) {
			BigInteger unscaledValue = bigIntegerSerializer.create(kryo, input, null);
			int scale = input.readInt(false);
			return new BigDecimal(unscaledValue, scale);
		}
	}

	static public class ClassSerializer extends Serializer<Class> {
		public void write (Kryo kryo, Output output, Class object) {
			kryo.writeClass(output, object);
		}

		public Class create (Kryo kryo, Input input, Class<Class> type) {
			return kryo.readClass(input).getType();
		}
	}

	static public class DateSerializer extends Serializer<Date> {
		public void write (Kryo kryo, Output output, Date object) {
			output.writeLong(object.getTime(), true);
		}

		public Date create (Kryo kryo, Input input, Class<Date> type) {
			return new Date(input.readLong(true));
		}
	}

	static public class EnumSerializer extends Serializer<Enum> {
		private Object[] enumConstants;

		public EnumSerializer (Kryo kryo, Class<? extends Enum> type) {
			enumConstants = type.getEnumConstants();
			if (enumConstants == null) throw new IllegalArgumentException("The type must be an enum: " + type);
		}

		public void write (Kryo kryo, Output output, Enum object) {
			output.writeInt(object.ordinal(), true);
		}

		public Enum create (Kryo kryo, Input input, Class<Enum> type) {
			int ordinal = input.readInt(true);
			if (ordinal < 0 || ordinal > enumConstants.length - 1)
				throw new KryoException("Invalid ordinal for enum \"" + type.getName() + "\": " + ordinal);
			Object constant = enumConstants[ordinal];
			return (Enum)constant;
		}
	}

	/** @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	static public class CurrencySerializer extends Serializer<Currency> {
		public void write (Kryo kryo, Output output, Currency object) {
			output.writeString(object.getCurrencyCode());
		}

		public Currency create (Kryo kryo, Input input, Class<Currency> type) {
			return Currency.getInstance(input.readString());
		}
	}

	/** @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	static public class StringBufferSerializer extends Serializer<StringBuffer> {
		public void write (Kryo kryo, Output output, StringBuffer object) {
			output.writeString(object.toString());
		}

		public StringBuffer create (Kryo kryo, Input input, Class<StringBuffer> type) {
			return new StringBuffer(input.readString());
		}
	}

	/** @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	static public class StringBuilderSerializer extends Serializer<StringBuilder> {
		public void write (Kryo kryo, Output output, StringBuilder object) {
			output.writeString(object.toString());
		}

		public StringBuilder create (Kryo kryo, Input input, Class<StringBuilder> type) {
			return new StringBuilder(input.readString());
		}
	}

	static public class SerializableSerializer extends Serializer<Serializable> {
		public void write (Kryo kryo, Output output, Serializable object) {
			object.write(kryo, output);
		}

		public void read (Kryo kryo, Input input, Serializable object) {
			object.read(kryo, input);
		}
	}
}
