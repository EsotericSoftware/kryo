
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

import static com.esotericsoftware.minlog.Log.*;

public class DefaultSerializers {
	static public class BooleanSerializer implements Serializer<Boolean> {
		public void write (Kryo kryo, Output output, Boolean object) {
			output.writeBoolean(object);
		}

		public Boolean read (Kryo kryo, Input input, Class<Boolean> type) {
			return input.readBoolean();
		}
	}

	static public class ByteSerializer implements Serializer<Byte> {
		public void write (Kryo kryo, Output output, Byte object) {
			output.writeByte(object);
		}

		public Byte read (Kryo kryo, Input input, Class<Byte> type) {
			return input.readByte();
		}
	}

	static public class CharSerializer implements Serializer<Character> {
		public void write (Kryo kryo, Output output, Character object) {
			output.writeChar(object);
		}

		public Character read (Kryo kryo, Input input, Class<Character> type) {
			return input.readChar();
		}
	}

	static public class ShortSerializer implements Serializer<Short> {
		public void write (Kryo kryo, Output output, Short object) {
			output.writeShort(object);
		}

		public Short read (Kryo kryo, Input input, Class<Short> type) {
			return input.readShort();
		}
	}

	static public class IntSerializer implements Serializer<Integer> {
		public void write (Kryo kryo, Output output, Integer object) {
			output.writeInt(object, false);
		}

		public Integer read (Kryo kryo, Input input, Class<Integer> type) {
			return input.readInt(false);
		}
	}

	static public class LongSerializer implements Serializer<Long> {
		public void write (Kryo kryo, Output output, Long object) {
			output.writeLong(object, false);
		}

		public Long read (Kryo kryo, Input input, Class<Long> type) {
			return input.readLong(false);
		}
	}

	static public class FloatSerializer implements Serializer<Float> {
		public void write (Kryo kryo, Output output, Float object) {
			output.writeFloat(object);
		}

		public Float read (Kryo kryo, Input input, Class<Float> type) {
			return input.readFloat();
		}
	}

	static public class DoubleSerializer implements Serializer<Double> {
		public void write (Kryo kryo, Output output, Double object) {
			output.writeDouble(object);
		}

		public Double read (Kryo kryo, Input input, Class<Double> type) {
			return input.readDouble();
		}
	}

	static public class StringSerializer implements Serializer<String> {
		public void write (Kryo kryo, Output output, String object) {
			output.writeString(object);
		}

		public String read (Kryo kryo, Input input, Class<String> type) {
			return input.readString();
		}
	}

	static public class ByteArraySerializer implements Serializer<byte[]> {
		public void write (Kryo kryo, Output output, byte[] object) {
			output.writeInt(object.length, true);
			output.writeBytes(object);
		}

		public byte[] read (Kryo kryo, Input input, Class<byte[]> type) {
			return input.readBytes(input.readInt(true));
		}
	}

	static public class BigIntegerSerializer implements Serializer<BigInteger> {
		public void write (Kryo kryo, Output output, BigInteger object) {
			BigInteger value = (BigInteger)object;
			byte[] bytes = value.toByteArray();
			output.writeInt(bytes.length, true);
			output.writeBytes(bytes);
			if (TRACE) trace("kryo", "Wrote BigInteger: " + value);
		}

		public BigInteger read (Kryo kryo, Input input, Class<BigInteger> type) {
			int length = input.readInt(true);
			byte[] bytes = input.readBytes(length);
			BigInteger value = new BigInteger(bytes);
			if (TRACE) trace("kryo", "Read BigInteger: " + value);
			return value;
		}
	}

	static public class BigDecimalSerializer implements Serializer<BigDecimal> {
		private BigIntegerSerializer bigIntegerSerializer = new BigIntegerSerializer();

		public void write (Kryo kryo, Output output, BigDecimal object) {
			BigDecimal value = (BigDecimal)object;
			bigIntegerSerializer.write(kryo, output, value.unscaledValue());
			output.writeInt(value.scale(), false);
			if (TRACE) trace("kryo", "Wrote BigDecimal: " + value);
		}

		public BigDecimal read (Kryo kryo, Input input, Class<BigDecimal> type) {
			BigInteger unscaledValue = bigIntegerSerializer.read(kryo, input, null);
			int scale = input.readInt(false);
			BigDecimal value = new BigDecimal(unscaledValue, scale);
			if (TRACE) trace("kryo", "Read BigDecimal: " + value);
			return value;
		}
	}

	static public class ClassSerializer implements Serializer<Class> {
		public void write (Kryo kryo, Output output, Class object) {
			kryo.writeClass(output, object);
		}

		public Class read (Kryo kryo, Input input, Class<Class> type) {
			return kryo.readClass(input).getType();
		}
	}

	static public class DateSerializer implements Serializer<Date> {
		public void write (Kryo kryo, Output output, Date object) {
			output.writeLong(object.getTime(), true);
			if (TRACE) trace("kryo", "Wrote date: " + object);
		}

		public Date read (Kryo kryo, Input input, Class<Date> type) {
			Date date = new Date(input.readLong(true));
			if (TRACE) trace("kryo", "Read date: " + date);
			return date;
		}
	}

	static public class EnumSerializer implements Serializer<Enum> {
		private Object[] enumConstants;

		public EnumSerializer (Kryo kryo, Class<? extends Enum> type) {
			enumConstants = type.getEnumConstants();
			if (enumConstants == null) throw new IllegalArgumentException("The type must be an enum: " + type);
		}

		public void write (Kryo kryo, Output output, Enum object) {
			output.writeInt(object.ordinal(), true);
			if (TRACE) trace("kryo", "Wrote enum: " + object);
		}

		public Enum read (Kryo kryo, Input input, Class<Enum> type) {
			int ordinal = input.readInt(true);
			if (ordinal < 0 || ordinal > enumConstants.length - 1)
				throw new KryoException("Invalid ordinal for enum \"" + type.getName() + "\": " + ordinal);
			Object constant = enumConstants[ordinal];
			if (TRACE) trace("kryo", "Read enum: " + constant);
			return (Enum)constant;
		}
	}

	/** @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	public class CurrencySerializer implements Serializer<Currency> {
		public void write (Kryo kryo, Output output, Currency object) {
			output.writeString(object.getCurrencyCode());
		}

		public Currency read (Kryo kryo, Input input, Class<Currency> type) {
			return Currency.getInstance(input.readString());
		}
	}

	/** @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	public class StringBufferSerializer implements Serializer<StringBuffer> {
		public void write (Kryo kryo, Output output, StringBuffer object) {
			output.writeString(object.toString());
		}

		public StringBuffer read (Kryo kryo, Input input, Class<StringBuffer> type) {
			return new StringBuffer(input.readString());
		}
	}

	/** @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	public class StringBuilderSerializer implements Serializer<StringBuilder> {
		public void write (Kryo kryo, Output output, StringBuilder object) {
			output.writeString(object.toString());
		}

		public StringBuilder read (Kryo kryo, Input input, Class<StringBuilder> type) {
			return new StringBuilder(input.readString());
		}
	}

	public class SerializableSerializer implements Serializer<Serializable> {
		public void write (Kryo kryo, Output output, Serializable object) {
			object.write(kryo, output);
		}

		public Serializable read (Kryo kryo, Input input, Class<Serializable> type) {
			Serializable object = newInstance(kryo, input, type);
			object.read(kryo, input);
			return object;
		}

		/** Instance creation can be customized by overridding this method. The default implementaion calls
		 * {@link Kryo#newInstance(Class)}. */
		public <T> T newInstance (Kryo kryo, Input input, Class<T> type) {
			return kryo.newInstance(type);
		}
	}
}
