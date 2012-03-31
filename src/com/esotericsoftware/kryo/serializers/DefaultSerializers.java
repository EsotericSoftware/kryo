
package com.esotericsoftware.kryo.serializers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Currency;
import java.util.Date;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoInput;
import com.esotericsoftware.kryo.KryoOutput;
import com.esotericsoftware.kryo.Serializer;

import static com.esotericsoftware.minlog.Log.*;

public class DefaultSerializers {
	static public class BooleanSerializer extends Serializer<Boolean> {
		public void write (Kryo kryo, KryoOutput output, Boolean object) {
			output.writeBoolean(object);
		}

		public Boolean read (Kryo kryo, KryoInput input, Class<Boolean> type) {
			return input.readBoolean();
		}
	}

	static public class ByteSerializer extends Serializer<Byte> {
		public void write (Kryo kryo, KryoOutput output, Byte object) {
			output.writeByte(object);
		}

		public Byte read (Kryo kryo, KryoInput input, Class<Byte> type) {
			return input.readByte();
		}
	}

	static public class CharSerializer extends Serializer<Character> {
		public void write (Kryo kryo, KryoOutput output, Character object) {
			output.writeChar(object);
		}

		public Character read (Kryo kryo, KryoInput input, Class<Character> type) {
			return input.readChar();
		}
	}

	static public class ShortSerializer extends Serializer<Short> {
		public void write (Kryo kryo, KryoOutput output, Short object) {
			output.writeShort(object);
		}

		public Short read (Kryo kryo, KryoInput input, Class<Short> type) {
			return input.readShort();
		}
	}

	static public class IntSerializer extends Serializer<Integer> {
		public void write (Kryo kryo, KryoOutput output, Integer object) {
			output.writeInt(object, false);
		}

		public Integer read (Kryo kryo, KryoInput input, Class<Integer> type) {
			return input.readInt(false);
		}
	}

	static public class LongSerializer extends Serializer<Long> {
		public void write (Kryo kryo, KryoOutput output, Long object) {
			output.writeLong(object, false);
		}

		public Long read (Kryo kryo, KryoInput input, Class<Long> type) {
			return input.readLong(false);
		}
	}

	static public class FloatSerializer extends Serializer<Float> {
		public void write (Kryo kryo, KryoOutput output, Float object) {
			output.writeFloat(object);
		}

		public Float read (Kryo kryo, KryoInput input, Class<Float> type) {
			return input.readFloat();
		}
	}

	static public class DoubleSerializer extends Serializer<Double> {
		public void write (Kryo kryo, KryoOutput output, Double object) {
			output.writeDouble(object);
		}

		public Double read (Kryo kryo, KryoInput input, Class<Double> type) {
			return input.readDouble();
		}
	}

	static public class StringSerializer extends Serializer<String> {
		public void write (Kryo kryo, KryoOutput output, String object) {
			output.writeString(object);
		}

		public String read (Kryo kryo, KryoInput input, Class<String> type) {
			return input.readString();
		}
	}

	static public class ByteArraySerializer extends Serializer<byte[]> {
		public void write (Kryo kryo, KryoOutput output, byte[] object) {
			output.writeInt(object.length, true);
			output.writeBytes(object);
		}

		public byte[] read (Kryo kryo, KryoInput input, Class<byte[]> type) {
			return input.readBytes(input.readInt(true));
		}
	}

	static public class BigIntegerSerializer extends Serializer<BigInteger> {
		public void write (Kryo kryo, KryoOutput output, BigInteger object) {
			BigInteger value = (BigInteger)object;
			byte[] bytes = value.toByteArray();
			output.writeInt(bytes.length, true);
			output.writeBytes(bytes);
			if (TRACE) trace("kryo", "Wrote BigInteger: " + value);
		}

		public BigInteger read (Kryo kryo, KryoInput input, Class<BigInteger> type) {
			int length = input.readInt(true);
			byte[] bytes = input.readBytes(length);
			BigInteger value = new BigInteger(bytes);
			if (TRACE) trace("kryo", "Read BigInteger: " + value);
			return value;
		}
	}

	static public class BigDecimalSerializer extends Serializer<BigDecimal> {
		private BigIntegerSerializer bigIntegerSerializer = new BigIntegerSerializer();

		public void write (Kryo kryo, KryoOutput output, BigDecimal object) {
			BigDecimal value = (BigDecimal)object;
			bigIntegerSerializer.write(kryo, output, value.unscaledValue());
			output.writeInt(value.scale(), false);
			if (TRACE) trace("kryo", "Wrote BigDecimal: " + value);
		}

		public BigDecimal read (Kryo kryo, KryoInput input, Class<BigDecimal> type) {
			BigInteger unscaledValue = bigIntegerSerializer.read(kryo, input, null);
			int scale = input.readInt(false);
			BigDecimal value = new BigDecimal(unscaledValue, scale);
			if (TRACE) trace("kryo", "Read BigDecimal: " + value);
			return value;
		}
	}

	static public class ClassSerializer extends Serializer<Class> {
		public void write (Kryo kryo, KryoOutput output, Class object) {
			kryo.writeClass(output, object);
		}

		public Class read (Kryo kryo, KryoInput input, Class<Class> type) {
			return kryo.readClass(input).getType();
		}
	}

	static public class DateSerializer extends Serializer<Date> {
		public void write (Kryo kryo, KryoOutput output, Date object) {
			output.writeLong(((Date)object).getTime(), true);
			if (TRACE) trace("kryo", "Wrote date: " + object);
		}

		public Date read (Kryo kryo, KryoInput input, Class<Date> type) {
			Date date = new Date(input.readLong(true));
			if (TRACE) trace("kryo", "Read date: " + date);
			return date;
		}
	}

	static public class EnumSerializer extends Serializer<Enum> {
		private Object[] enumConstants;

		public EnumSerializer (Kryo kryo, Class<? extends Enum> type) {
			enumConstants = type.getEnumConstants();
			if (enumConstants == null) throw new IllegalArgumentException("The type must be an enum: " + type);
		}

		public void write (Kryo kryo, KryoOutput output, Enum object) {
			output.writeInt(object.ordinal(), true);
			if (TRACE) trace("kryo", "Wrote enum: " + object);
		}

		public Enum read (Kryo kryo, KryoInput input, Class<Enum> type) {
			int ordinal = input.readInt(true);
			if (ordinal < 0 || ordinal > enumConstants.length - 1)
				throw new KryoException("Invalid ordinal for enum \"" + type.getName() + "\": " + ordinal);
			Object constant = enumConstants[ordinal];
			if (TRACE) trace("kryo", "Read enum: " + constant);
			return (Enum)constant;
		}
	}

	/** @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	public class CurrencySerializer extends Serializer<Currency> {
		public void write (Kryo kryo, KryoOutput output, Currency object) {
			output.writeString(object.getCurrencyCode());
		}

		public Currency read (Kryo kryo, KryoInput input, Class<Currency> type) {
			return Currency.getInstance(input.readString());
		}
	}

	/** @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	public class StringBufferSerializer extends Serializer<StringBuffer> {
		public void write (Kryo kryo, KryoOutput output, StringBuffer object) {
			output.writeString(object.toString());
		}

		public StringBuffer read (Kryo kryo, KryoInput input, Class<StringBuffer> type) {
			return new StringBuffer(input.readString());
		}
	}

	/** @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	public class StringBuilderSerializer extends Serializer<StringBuilder> {
		public void write (Kryo kryo, KryoOutput output, StringBuilder object) {
			output.writeString(object.toString());
		}

		public StringBuilder read (Kryo kryo, KryoInput input, Class<StringBuilder> type) {
			return new StringBuilder(input.readString());
		}
	}
}
