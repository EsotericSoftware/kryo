
package com.esotericsoftware.kryo.serializers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoCopyable;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.KryoString;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import static com.esotericsoftware.kryo.Kryo.*;

/** Contains many serializer classes that are provided by {@link Kryo#addDefaultSerializer(Class, Class) default}.
 * @author Nathan Sweet <misc@n4te.com> */
public class DefaultSerializers {
	static public class BooleanSerializer extends Serializer<Boolean> {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, Boolean object) {
			output.writeBoolean(object);
		}

		public Boolean create (Kryo kryo, Input input, Class<Boolean> type) {
			return input.readBoolean();
		}
	}

	static public class ByteSerializer extends Serializer<Byte> {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, Byte object) {
			output.writeByte(object);
		}

		public Byte create (Kryo kryo, Input input, Class<Byte> type) {
			return input.readByte();
		}
	}

	static public class CharSerializer extends Serializer<Character> {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, Character object) {
			output.writeChar(object);
		}

		public Character create (Kryo kryo, Input input, Class<Character> type) {
			return input.readChar();
		}
	}

	static public class ShortSerializer extends Serializer<Short> {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, Short object) {
			output.writeShort(object);
		}

		public Short create (Kryo kryo, Input input, Class<Short> type) {
			return input.readShort();
		}
	}

	static public class IntSerializer extends Serializer<Integer> {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, Integer object) {
			output.writeInt(object, false);
		}

		public Integer create (Kryo kryo, Input input, Class<Integer> type) {
			return input.readInt(false);
		}
	}

	static public class LongSerializer extends Serializer<Long> {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, Long object) {
			output.writeLong(object, false);
		}

		public Long create (Kryo kryo, Input input, Class<Long> type) {
			return input.readLong(false);
		}
	}

	static public class FloatSerializer extends Serializer<Float> {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, Float object) {
			output.writeFloat(object);
		}

		public Float create (Kryo kryo, Input input, Class<Float> type) {
			return input.readFloat();
		}
	}

	static public class DoubleSerializer extends Serializer<Double> {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, Double object) {
			output.writeDouble(object);
		}

		public Double create (Kryo kryo, Input input, Class<Double> type) {
			return input.readDouble();
		}
	}

	/** @see Output#writeString(String) */
	static public class StringSerializer extends Serializer<String> {
		{
			setImmutable(true);
			setAcceptsNull(true);
		}

		public void write (Kryo kryo, Output output, String object) {
			output.writeString(object);
		}

		public String create (Kryo kryo, Input input, Class<String> type) {
			return input.readString();
		}
	}

	static public class KryoStringSerializer extends Serializer<KryoString> {
		{
			setImmutable(true);
			setAcceptsNull(true);
		}

		public void write (Kryo kryo, Output output, KryoString object) {
			if (object == null || object.bytes == null)
				output.writeByte(0);
			else
				output.writeBytes(object.bytes);
		}

		public KryoString create (Kryo kryo, Input input, Class<KryoString> type) {
			KryoString string = new KryoString();
			int length = input.readInt(true);
			if (length == 0) return string;
			int lengthLength = Output.intLength(length, true);
			string.bytes = new byte[lengthLength + length - 1];
			Output output = new Output(string.bytes);
			output.write(length);
			input.read(string.bytes, lengthLength, length - 1);
			return string;
		}
	}

	// BOZO - Add string/int/float/double/boolean/char[] serializers.

	static public class ByteArraySerializer extends Serializer<byte[]> {
		{
			setAcceptsNull(true);
		}

		public void write (Kryo kryo, Output output, byte[] object) {
			if (object == null) {
				output.writeByte(NULL);
				return;
			}
			output.writeInt(object.length + 1, true);
			output.writeBytes(object);
		}

		public byte[] create (Kryo kryo, Input input, Class<byte[]> type) {
			int length = input.readInt(true);
			if (length == NULL) return null;
			return input.readBytes(length - 1);
		}

		public byte[] createCopy (Kryo kryo, byte[] original) {
			byte[] copy = new byte[original.length];
			System.arraycopy(original, 0, copy, 0, copy.length);
			return copy;
		}
	}

	static public class BigIntegerSerializer extends Serializer<BigInteger> {
		{
			setImmutable(true);
			setAcceptsNull(true);
		}

		public void write (Kryo kryo, Output output, BigInteger object) {
			if (object == null) {
				output.writeByte(NULL);
				return;
			}
			BigInteger value = (BigInteger)object;
			byte[] bytes = value.toByteArray();
			output.writeInt(bytes.length + 1, true);
			output.writeBytes(bytes);
		}

		public BigInteger create (Kryo kryo, Input input, Class<BigInteger> type) {
			int length = input.readInt(true);
			if (length == NULL) return null;
			byte[] bytes = input.readBytes(length - 1);
			return new BigInteger(bytes);
		}
	}

	static public class BigDecimalSerializer extends Serializer<BigDecimal> {
		private BigIntegerSerializer bigIntegerSerializer = new BigIntegerSerializer();

		{
			setImmutable(true);
		}

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
		{
			setImmutable(true);
			setAcceptsNull(true);
		}

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

		public Date createCopy (Kryo kryo, Date original) {
			return new Date(original.getTime());
		}
	}

	static public class EnumSerializer extends Serializer<Enum> {
		{
			setImmutable(true);
			setAcceptsNull(true);
		}

		private Object[] enumConstants;

		public EnumSerializer (Kryo kryo, Class<? extends Enum> type) {
			enumConstants = type.getEnumConstants();
			if (enumConstants == null) throw new IllegalArgumentException("The type must be an enum: " + type);
		}

		public void write (Kryo kryo, Output output, Enum object) {
			if (object == null) {
				output.writeByte(NULL);
				return;
			}
			output.writeInt(object.ordinal() + 1, true);
		}

		public Enum create (Kryo kryo, Input input, Class<Enum> type) {
			int ordinal = input.readInt(true);
			if (ordinal == NULL) return null;
			ordinal--;
			if (ordinal < 0 || ordinal > enumConstants.length - 1)
				throw new KryoException("Invalid ordinal for enum \"" + type.getName() + "\": " + ordinal);
			Object constant = enumConstants[ordinal];
			return (Enum)constant;
		}
	}

	/** @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	static public class CurrencySerializer extends Serializer<Currency> {
		{
			setImmutable(true);
			setAcceptsNull(true);
		}

		public void write (Kryo kryo, Output output, Currency object) {
			output.writeString(object == null ? null : object.getCurrencyCode());
		}

		public Currency create (Kryo kryo, Input input, Class<Currency> type) {
			String currencyCode = input.readString();
			if (currencyCode == null) return null;
			return Currency.getInstance(currencyCode);
		}
	}

	/** @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	static public class StringBufferSerializer extends Serializer<StringBuffer> {
		{
			setAcceptsNull(true);
		}

		public void write (Kryo kryo, Output output, StringBuffer object) {
			output.writeString(object == null ? null : object.toString());
		}

		public StringBuffer create (Kryo kryo, Input input, Class<StringBuffer> type) {
			String value = input.readString();
			if (value == null) return null;
			return new StringBuffer(value);
		}

		public StringBuffer createCopy (Kryo kryo, StringBuffer original) {
			return new StringBuffer(original);
		}
	}

	/** @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	static public class StringBuilderSerializer extends Serializer<StringBuilder> {
		{
			setAcceptsNull(true);
		}

		public void write (Kryo kryo, Output output, StringBuilder object) {
			output.writeString(object == null ? null : object.toString());
		}

		public StringBuilder create (Kryo kryo, Input input, Class<StringBuilder> type) {
			String value = input.readString();
			if (value == null) return null;
			return new StringBuilder(value);
		}

		public StringBuilder createCopy (Kryo kryo, StringBuilder original) {
			return new StringBuilder(original);
		}
	}

	static public class KryoSerializableSerializer extends Serializer<KryoSerializable> {
		public void write (Kryo kryo, Output output, KryoSerializable object) {
			object.write(kryo, output);
		}

		public void read (Kryo kryo, Input input, KryoSerializable object) {
			object.read(kryo, input);
		}

		public KryoSerializable createCopy (Kryo kryo, KryoSerializable original) {
			if (original instanceof KryoCopyable) return kryo.newInstance(original.getClass());
			return super.createCopy(kryo, original);
		}
	}

	/** Serializer for lists created via {@link Collections#emptyList()} or that were just assigned the
	 * {@link Collections#EMPTY_LIST}.
	 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	static public class CollectionsEmptyListSerializer extends Serializer {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, Object object) {
		}

		public Object create (Kryo kryo, Input input, Class type) {
			return Collections.EMPTY_LIST;
		}
	}

	/** Serializer for maps created via {@link Collections#emptyMap()} or that were just assigned the {@link Collections#EMPTY_MAP}.
	 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	static public class CollectionsEmptyMapSerializer extends Serializer {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, Object object) {
		}

		public Object create (Kryo kryo, Input input, Class type) {
			return Collections.EMPTY_MAP;
		}
	}

	/** Serializer for sets created via {@link Collections#emptySet()} or that were just assigned the {@link Collections#EMPTY_SET}.
	 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	static public class CollectionsEmptySetSerializer extends Serializer {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, Object object) {
		}

		public Object create (Kryo kryo, Input input, Class type) {
			return Collections.EMPTY_SET;
		}
	}

	/** Serializer for lists created via {@link Collections#singletonList(Object)}.
	 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	static public class CollectionsSingletonListSerializer extends Serializer<List> {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, List object) {
			kryo.writeClassAndObject(output, object.get(0));
		}

		public List create (Kryo kryo, Input input, Class type) {
			return Collections.singletonList(kryo.readClassAndObject(input));
		}
	}

	/** Serializer for maps created via {@link Collections#singletonMap(Object, Object)}.
	 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	static public class CollectionsSingletonMapSerializer extends Serializer<Map> {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, Map object) {
			Entry entry = (Entry)object.entrySet().iterator().next();
			kryo.writeClassAndObject(output, entry.getKey());
			kryo.writeClassAndObject(output, entry.getValue());
		}

		public Map create (Kryo kryo, Input input, Class type) {
			Object key = kryo.readClassAndObject(input);
			Object value = kryo.readClassAndObject(input);
			return Collections.singletonMap(key, value);
		}
	}

	/** Serializer for sets created via {@link Collections#singleton(Object)}.
	 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	static public class CollectionsSingletonSetSerializer extends Serializer<Set> {
		{
			setImmutable(true);
		}

		public void write (Kryo kryo, Output output, Set object) {
			kryo.writeClassAndObject(output, object.iterator().next());
		}

		public Set create (Kryo kryo, Input input, Class type) {
			return Collections.singleton(kryo.readClassAndObject(input));
		}
	}
}
