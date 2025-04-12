/* Copyright (c) 2008-2025, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryo.serializers;

import static com.esotericsoftware.kryo.Kryo.*;
import static com.esotericsoftware.kryo.util.Util.*;
import static java.lang.Long.numberOfLeadingZeros;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/** Contains many serializer classes that are provided by {@link Kryo#addDefaultSerializer(Class, Class) default}.
 * @author Nathan Sweet */
public class DefaultSerializers {
	public static class VoidSerializer extends ImmutableSerializer {
		public void write (Kryo kryo, Output output, Object object) {
		}

		public Object read (Kryo kryo, Input input, Class type) {
			return null;
		}
	}

	public static class BooleanSerializer extends ImmutableSerializer<Boolean> {
		public void write (Kryo kryo, Output output, Boolean object) {
			output.writeBoolean(object);
		}

		public Boolean read (Kryo kryo, Input input, Class<? extends Boolean> type) {
			return input.readBoolean();
		}
	}

	public static class ByteSerializer extends ImmutableSerializer<Byte> {
		public void write (Kryo kryo, Output output, Byte object) {
			output.writeByte(object);
		}

		public Byte read (Kryo kryo, Input input, Class<? extends Byte> type) {
			return input.readByte();
		}
	}

	public static class CharSerializer extends ImmutableSerializer<Character> {
		public void write (Kryo kryo, Output output, Character object) {
			output.writeChar(object);
		}

		public Character read (Kryo kryo, Input input, Class<? extends Character> type) {
			return input.readChar();
		}
	}

	public static class ShortSerializer extends ImmutableSerializer<Short> {
		public void write (Kryo kryo, Output output, Short object) {
			output.writeShort(object);
		}

		public Short read (Kryo kryo, Input input, Class<? extends Short> type) {
			return input.readShort();
		}
	}

	public static class IntSerializer extends ImmutableSerializer<Integer> {
		public void write (Kryo kryo, Output output, Integer object) {
			output.writeInt(object, false);
		}

		public Integer read (Kryo kryo, Input input, Class<? extends Integer> type) {
			return input.readInt(false);
		}
	}

	public static class LongSerializer extends ImmutableSerializer<Long> {
		public void write (Kryo kryo, Output output, Long object) {
			output.writeVarLong(object, false);
		}

		public Long read (Kryo kryo, Input input, Class<? extends Long> type) {
			return input.readVarLong(false);
		}
	}

	public static class FloatSerializer extends ImmutableSerializer<Float> {
		public void write (Kryo kryo, Output output, Float object) {
			output.writeFloat(object);
		}

		public Float read (Kryo kryo, Input input, Class<? extends Float> type) {
			return input.readFloat();
		}
	}

	public static class DoubleSerializer extends ImmutableSerializer<Double> {
		public void write (Kryo kryo, Output output, Double object) {
			output.writeDouble(object);
		}

		public Double read (Kryo kryo, Input input, Class<? extends Double> type) {
			return input.readDouble();
		}
	}

	/** @see Output#writeString(String) */
	public static class StringSerializer extends ImmutableSerializer<String> {
		{
			setAcceptsNull(true);
		}

		public void write (Kryo kryo, Output output, String object) {
			output.writeString(object);
		}

		public String read (Kryo kryo, Input input, Class<? extends String> type) {
			return input.readString();
		}
	}

	/** Serializer for {@link BigInteger} and any subclass.
	 * @author Tumi <serverperformance@gmail.com> (enhacements) */
	public static class BigIntegerSerializer extends ImmutableSerializer<BigInteger> {
		{
			setAcceptsNull(true);
		}

		public void write (Kryo kryo, Output output, BigInteger object) {
			if (object == null) {
				output.writeByte(NULL);
				return;
			}
			// fast-path optimizations for BigInteger.ZERO constant
			if (object == BigInteger.ZERO) {
				output.writeByte(2);
				output.writeByte(0);
				return;
			}
			// default behaviour
			byte[] bytes = object.toByteArray();
			output.writeVarInt(bytes.length + 1, true);
			output.writeBytes(bytes);
		}

		public BigInteger read (Kryo kryo, Input input, Class<? extends BigInteger> type) {
			int length = input.readVarInt(true);
			if (length == NULL) return null;
			byte[] bytes = input.readBytes(length - 1);
			if (type != BigInteger.class && type != null) {
				// Use reflection for subclasses.
				return newBigIntegerSubclass(type, bytes);
			}
			if (length == 2) {
				// Fast-path optimizations for BigInteger constants.
				switch (bytes[0]) {
				case 0:
					return BigInteger.ZERO;
				case 1:
					return BigInteger.ONE;
				case 10:
					return BigInteger.TEN;
				}
			}
			return new BigInteger(bytes);
		}

		private static BigInteger newBigIntegerSubclass(Class<? extends BigInteger> type, byte[] bytes) {
			try {
				Constructor<? extends BigInteger> constructor = type.getConstructor(byte[].class);
				if (!constructor.isAccessible()) {
					try {
						constructor.setAccessible(true);
					} catch (SecurityException ignored) {
					}
				}
				return constructor.newInstance(bytes);
			} catch (Exception ex) {
				throw new KryoException(ex);
			}
		}
	}

	/** Serializer for {@link BigDecimal} and any subclass.
	 * @author Tumi <serverperformance@gmail.com> (enhacements) */
	public static class BigDecimalSerializer extends ImmutableSerializer<BigDecimal> {
		{
			setAcceptsNull(true);
		}

		public void write (Kryo kryo, Output output, BigDecimal object) {
			if (object == null) {
				output.writeByte(NULL);
				return;
			}
			if (object == BigDecimal.ZERO) {
				output.writeVarInt(2, true);
				output.writeByte((byte) 0);
				output.writeInt(0, false);
				return;
			}
			if (object == BigDecimal.ONE) {
				output.writeVarInt(2, true);
				output.writeByte((byte) 1);
				output.writeInt(0, false);
				return;
			}

			BigInteger unscaledBig = null; // avoid getting it from BigDecimal, as non-inflated BigDecimal will have to create it
			boolean compactForm = object.precision() < 19; // less than nineteen decimal digits for sure fits in a long
			if (!compactForm) {
				unscaledBig = object.unscaledValue(); // get and remember for possible use in non-compact form
				compactForm = unscaledBig.bitLength() <= 63; // check exactly if unscaled value will fit in a long
			}

			if (!compactForm) {
				byte[] bytes = unscaledBig.toByteArray();
				output.writeVarInt(bytes.length + 1, true);
				output.writeBytes(bytes);
			} else {
				long unscaledLong = object.scaleByPowerOfTen(object.scale()).longValue(); // best way to get unscaled long value without creating unscaled BigInteger on the way
				writeUnscaledLong(output, unscaledLong);
			}

			output.writeInt(object.scale(), false);
		}

		// compatible with writing unscaled value represented as BigInteger's bytes
		private static void writeUnscaledLong (Output output, long unscaledLong) {
			int insignificantBits = unscaledLong >= 0
					? numberOfLeadingZeros(unscaledLong)
					: numberOfLeadingZeros(~unscaledLong);
			int significantBits = (64 - insignificantBits) + 1; // one more bit is for the sign
			int length = (significantBits + (8 - 1)) >> 3; // how many bytes are needed (rounded up)

			output.writeByte(length + 1);
			output.writeLong(unscaledLong, length);
		}

		public BigDecimal read (Kryo kryo, Input input, Class<? extends BigDecimal> type) {
			BigInteger unscaledBig = null;
			long unscaledLong = 0;

			int length = input.readVarInt(true);
			if (length == NULL) return null;
			length--;

			if (length > 8) {
				byte[] bytes = input.readBytes(length);
				unscaledBig = new BigInteger(bytes);
			} else {
				unscaledLong = input.readLong(length);
			}

			int scale = input.readInt(false);
			if (type != BigDecimal.class && type != null) {
				// For subclasses, use reflection
				return newBigDecimalSubclass(type, unscaledBig != null ? unscaledBig : BigInteger.valueOf(unscaledLong), scale);
			} else {
				// For BigDecimal, if possible use factory methods to avoid instantiating BigInteger
				if (unscaledBig != null) {
					return new BigDecimal(unscaledBig, scale);
				} else {
					if (scale == 0) {
						if (unscaledLong == 0) return BigDecimal.ZERO;
						if (unscaledLong == 1) return BigDecimal.ONE;
					}
					return BigDecimal.valueOf(unscaledLong, scale);
				}
			}
		}

		private static BigDecimal newBigDecimalSubclass(Class<? extends BigDecimal> type, BigInteger unscaledValue, int scale) {
			try {
				Constructor<? extends BigDecimal> constructor = type.getConstructor(BigInteger.class, int.class);
				if (!constructor.isAccessible()) {
					try {
						constructor.setAccessible(true);
					} catch (SecurityException ignored) {
					}
				}
				return constructor.newInstance(unscaledValue, scale);
			} catch (Exception ex) {
				throw new KryoException(ex);
			}
		}
	}

	public static class ClassSerializer extends ImmutableSerializer<Class> {
		{
			setAcceptsNull(true);
		}

		public void write (Kryo kryo, Output output, Class type) {
			kryo.writeClass(output, type);
			if (type != null && (type.isPrimitive() || isWrapperClass(type))) output.writeBoolean(type.isPrimitive());
		}

		public Class read (Kryo kryo, Input input, Class<? extends Class> ignored) {
			Registration registration = kryo.readClass(input);
			if (registration == null) return null;
			Class type = registration.getType();
			if (!type.isPrimitive() || input.readBoolean()) return type;
			return getWrapperClass(type);
		}
	}

	/** Serializer for {@link Date}, {@link java.sql.Date}, {@link Time}, {@link Timestamp} and any other subclass.
	 * @author Tumi <serverperformance@gmail.com> */
	public static class DateSerializer extends Serializer<Date> {
		private Date create (Kryo kryo, Class<? extends Date> type, long time) throws KryoException {
			if (type == Date.class || type == null) {
				return new Date(time);
			}
			if (type == Timestamp.class) {
				return new Timestamp(time);
			}
			if (type == java.sql.Date.class) {
				return new java.sql.Date(time);
			}
			if (type == Time.class) {
				return new Time(time);
			}
			// other cases, reflection
			try {
				// Try to avoid invoking the no-args constructor
				// (which is expected to initialize the instance with the current time)
				Constructor<? extends Date> constructor = type.getConstructor(long.class);
				if (!constructor.isAccessible()) {
					try {
						constructor.setAccessible(true);
					} catch (SecurityException ignored) {
					}
				}
				return constructor.newInstance(time);
			} catch (Exception ex) {
				// default strategy
				Date d = kryo.newInstance(type);
				d.setTime(time);
				return d;
			}
		}

		public void write (Kryo kryo, Output output, Date object) {
			output.writeVarLong(object.getTime(), true);
		}

		public Date read (Kryo kryo, Input input, Class<? extends Date> type) {
			return create(kryo, type, input.readVarLong(true));
		}

		public Date copy (Kryo kryo, Date original) {
			return create(kryo, original.getClass(), original.getTime());
		}
	}

	/** Serializer for {@link Timestamp} which preserves the nanoseconds field. */
	public static class TimestampSerializer extends Serializer<Timestamp> {
		public void write (Kryo kryo, Output output, Timestamp object) {
			output.writeVarLong(integralTimeComponent(object), true);
			output.writeVarInt(object.getNanos(), true);
		}

		public Timestamp read (Kryo kryo, Input input, Class<? extends Timestamp> type) {
			return create(input.readVarLong(true), input.readVarInt(true));
		}

		public Timestamp copy (Kryo kryo, Timestamp original) {
			return create(integralTimeComponent(original), original.getNanos());
		}

		private long integralTimeComponent (Timestamp object) {
			return object.getTime() - (object.getNanos() / 1_000_000);
		}

		private Timestamp create (long time, int nanos) {
			Timestamp t = new Timestamp(time);
			t.setNanos(nanos);
			return t;
		}
	}

	public static class EnumSerializer extends ImmutableSerializer<Enum> {
		{
			setAcceptsNull(true);
		}

		private Object[] enumConstants;

		public EnumSerializer (Class<? extends Enum> type) {
			enumConstants = type.getEnumConstants();
			// We allow the serialization of the (abstract!) Enum.class (instead of an actual "user" enum),
			// which also creates an EnumSerializer instance during Kryo.writeClass with the following trace:
			// ClassSerializer.write -> Kryo.writeClass -> DefaultClassResolver.writeClass
			// -> Kryo.getDefaultSerializer -> ReflectionSerializerFactory.makeSerializer(kryo, EnumSerializer, Enum.class)
			// This EnumSerializer instance is expected to be never called for write/read.
			if (enumConstants == null && !Enum.class.equals(type))
				throw new IllegalArgumentException("The type must be an enum: " + type);
		}

		public void write (Kryo kryo, Output output, Enum object) {
			if (object == null) {
				output.writeVarInt(NULL, true);
				return;
			}
			output.writeVarInt(object.ordinal() + 1, true);
		}

		public Enum read (Kryo kryo, Input input, Class<? extends Enum> type) {
			int ordinal = input.readVarInt(true);
			if (ordinal == NULL) return null;
			ordinal--;
			if (ordinal < 0 || ordinal > enumConstants.length - 1)
				throw new KryoException("Invalid ordinal for enum \"" + type.getName() + "\": " + ordinal);
			Object constant = enumConstants[ordinal];
			return (Enum)constant;
		}
	}

	public static class EnumSetSerializer extends Serializer<EnumSet> {
		public void write (Kryo kryo, Output output, EnumSet object) {
			Serializer serializer;
			if (object.isEmpty()) {
				EnumSet tmp = EnumSet.complementOf(object);
				if (tmp.isEmpty()) throw new KryoException("An EnumSet must have a defined Enum to be serialized.");
				serializer = kryo.writeClass(output, tmp.iterator().next().getClass()).getSerializer();
			} else {
				serializer = kryo.writeClass(output, object.iterator().next().getClass()).getSerializer();
			}
			output.writeVarInt(object.size(), true);
			for (Object element : object)
				serializer.write(kryo, output, element);
		}

		public EnumSet read (Kryo kryo, Input input, Class<? extends EnumSet> type) {
			Registration registration = kryo.readClass(input);
			EnumSet object = EnumSet.noneOf(registration.getType());
			Serializer serializer = registration.getSerializer();
			int length = input.readVarInt(true);
			for (int i = 0; i < length; i++)
				object.add(serializer.read(kryo, input, null));
			return object;
		}

		public EnumSet copy (Kryo kryo, EnumSet original) {
			return EnumSet.copyOf(original);
		}
	}

	/** @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	public static class CurrencySerializer extends ImmutableSerializer<Currency> {
		{
			setAcceptsNull(true);
		}

		public void write (Kryo kryo, Output output, Currency object) {
			output.writeString(object == null ? null : object.getCurrencyCode());
		}

		public Currency read (Kryo kryo, Input input, Class<? extends Currency> type) {
			String currencyCode = input.readString();
			if (currencyCode == null) return null;
			return Currency.getInstance(currencyCode);
		}
	}

	/** @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	public static class StringBufferSerializer extends Serializer<StringBuffer> {
		{
			setAcceptsNull(true);
		}

		public void write (Kryo kryo, Output output, StringBuffer object) {
			output.writeString(object == null ? null : object.toString());
		}

		public StringBuffer read (Kryo kryo, Input input, Class<? extends StringBuffer> type) {
			String value = input.readString();
			if (value == null) return null;
			return new StringBuffer(value);
		}

		public StringBuffer copy (Kryo kryo, StringBuffer original) {
			return new StringBuffer(original);
		}
	}

	/** @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	public static class StringBuilderSerializer extends Serializer<StringBuilder> {
		{
			setAcceptsNull(true);
		}

		public void write (Kryo kryo, Output output, StringBuilder object) {
			output.writeString(object == null ? null : object.toString());
		}

		public StringBuilder read (Kryo kryo, Input input, Class<? extends StringBuilder> type) {
			return input.readStringBuilder();
		}

		public StringBuilder copy (Kryo kryo, StringBuilder original) {
			return new StringBuilder(original);
		}
	}

	public static class KryoSerializableSerializer extends Serializer<KryoSerializable> {
		public void write (Kryo kryo, Output output, KryoSerializable object) {
			object.write(kryo, output);
		}

		public KryoSerializable read (Kryo kryo, Input input, Class<? extends KryoSerializable> type) {
			KryoSerializable object = kryo.newInstance(type);
			kryo.reference(object);
			object.read(kryo, input);
			return object;
		}
	}

	/** Serializer for lists created via {@link Collections#emptyList()} or that were just assigned the
	 * {@link Collections#EMPTY_LIST}.
	 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	public static class CollectionsEmptyListSerializer extends ImmutableSerializer<Collection> {
		public void write (Kryo kryo, Output output, Collection object) {
		}

		public Collection read (Kryo kryo, Input input, Class<? extends Collection> type) {
			return Collections.EMPTY_LIST;
		}
	}

	/** Serializer for maps created via {@link Collections#emptyMap()} or that were just assigned the
	 * {@link Collections#EMPTY_MAP}.
	 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	public static class CollectionsEmptyMapSerializer extends ImmutableSerializer<Map> {
		public void write (Kryo kryo, Output output, Map object) {
		}

		public Map read (Kryo kryo, Input input, Class<? extends Map> type) {
			return Collections.EMPTY_MAP;
		}
	}

	/** Serializer for sets created via {@link Collections#emptySet()} or that were just assigned the
	 * {@link Collections#EMPTY_SET}.
	 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	public static class CollectionsEmptySetSerializer extends ImmutableSerializer<Set> {
		public void write (Kryo kryo, Output output, Set object) {
		}

		public Set read (Kryo kryo, Input input, Class<? extends Set> type) {
			return Collections.EMPTY_SET;
		}
	}

	/** Serializer for lists created via {@link Collections#singletonList(Object)}.
	 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	public static class CollectionsSingletonListSerializer extends Serializer<List> {
		public void write (Kryo kryo, Output output, List object) {
			kryo.writeClassAndObject(output, object.get(0));
		}

		public List read (Kryo kryo, Input input, Class<? extends List> type) {
			return Collections.singletonList(kryo.readClassAndObject(input));
		}

		public List copy (Kryo kryo, List original) {
			return Collections.singletonList(kryo.copy(original.get(0)));
		}
	}

	/** Serializer for maps created via {@link Collections#singletonMap(Object, Object)}.
	 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	public static class CollectionsSingletonMapSerializer extends Serializer<Map> {
		public void write (Kryo kryo, Output output, Map object) {
			Entry entry = (Entry)object.entrySet().iterator().next();
			kryo.writeClassAndObject(output, entry.getKey());
			kryo.writeClassAndObject(output, entry.getValue());
		}

		public Map read (Kryo kryo, Input input, Class<? extends Map> type) {
			Object key = kryo.readClassAndObject(input);
			Object value = kryo.readClassAndObject(input);
			return Collections.singletonMap(key, value);
		}

		public Map copy (Kryo kryo, Map original) {
			Entry entry = (Entry)original.entrySet().iterator().next();
			return Collections.singletonMap(kryo.copy(entry.getKey()), kryo.copy(entry.getValue()));
		}
	}

	/** Serializer for sets created via {@link Collections#singleton(Object)}.
	 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a> */
	public static class CollectionsSingletonSetSerializer extends Serializer<Set> {
		public void write (Kryo kryo, Output output, Set object) {
			kryo.writeClassAndObject(output, object.iterator().next());
		}

		public Set read (Kryo kryo, Input input, Class<? extends Set> type) {
			return Collections.singleton(kryo.readClassAndObject(input));
		}

		public Set copy (Kryo kryo, Set original) {
			return Collections.singleton(kryo.copy(original.iterator().next()));
		}
	}

	/** Serializer for {@link TimeZone}. Assumes the timezones are immutable.
	 * @author Tumi <serverperformance@gmail.com> */
	public static class TimeZoneSerializer extends ImmutableSerializer<TimeZone> {
		public void write (Kryo kryo, Output output, TimeZone object) {
			output.writeString(object.getID());
		}

		public TimeZone read (Kryo kryo, Input input, Class<? extends TimeZone> type) {
			return TimeZone.getTimeZone(input.readString());
		}
	}

	/** Serializer for {@link GregorianCalendar}, java.util.JapaneseImperialCalendar, and sun.util.BuddhistCalendar.
	 * @author Tumi <serverperformance@gmail.com> */
	public static class CalendarSerializer extends Serializer<Calendar> {
		// The default value of gregorianCutover.
		private static final long DEFAULT_GREGORIAN_CUTOVER = -12219292800000L;

		TimeZoneSerializer timeZoneSerializer = new TimeZoneSerializer();

		public void write (Kryo kryo, Output output, Calendar object) {
			timeZoneSerializer.write(kryo, output, object.getTimeZone()); // can't be null
			output.writeVarLong(object.getTimeInMillis(), true);
			output.writeBoolean(object.isLenient());
			output.writeInt(object.getFirstDayOfWeek(), true);
			output.writeInt(object.getMinimalDaysInFirstWeek(), true);
			if (object instanceof GregorianCalendar)
				output.writeVarLong(((GregorianCalendar)object).getGregorianChange().getTime(), false);
			else
				output.writeVarLong(DEFAULT_GREGORIAN_CUTOVER, false);
		}

		public Calendar read (Kryo kryo, Input input, Class<? extends Calendar> type) {
			Calendar result = Calendar.getInstance(timeZoneSerializer.read(kryo, input, TimeZone.class));
			result.setTimeInMillis(input.readVarLong(true));
			result.setLenient(input.readBoolean());
			result.setFirstDayOfWeek(input.readInt(true));
			result.setMinimalDaysInFirstWeek(input.readInt(true));
			long gregorianChange = input.readVarLong(false);
			if (gregorianChange != DEFAULT_GREGORIAN_CUTOVER)
				if (result instanceof GregorianCalendar) ((GregorianCalendar)result).setGregorianChange(new Date(gregorianChange));
			return result;
		}

		public Calendar copy (Kryo kryo, Calendar original) {
			return (Calendar)original.clone();
		}
	}

	/** Serializer for {@link TreeMap} and any subclass.
	 * @author Tumi <serverperformance@gmail.com> (enhacements) */
	public static class TreeMapSerializer extends MapSerializer<TreeMap> {
		protected void writeHeader (Kryo kryo, Output output, TreeMap treeSet) {
			kryo.writeClassAndObject(output, treeSet.comparator());
		}

		protected TreeMap create (Kryo kryo, Input input, Class<? extends TreeMap> type, int size) {
			return createTreeMap(type, (Comparator)kryo.readClassAndObject(input));
		}

		protected TreeMap createCopy (Kryo kryo, TreeMap original) {
			return createTreeMap(original.getClass(), original.comparator());
		}

		private TreeMap createTreeMap (Class<? extends TreeMap> type, Comparator comparator) {
			if (type == TreeMap.class || type == null) return new TreeMap(comparator);
			// Use reflection for subclasses.
			try {
				Constructor constructor = type.getConstructor(Comparator.class);
				if (!constructor.isAccessible()) {
					try {
						constructor.setAccessible(true);
					} catch (SecurityException ignored) {
					}
				}
				return (TreeMap)constructor.newInstance(comparator);
			} catch (Exception ex) {
				throw new KryoException(ex);
			}
		}
	}

	/** Serializer for {@link ConcurrentSkipListMap} and any subclass.
	 * @author Mr14huashao <mr11huashao@gmail.com> (enhacements) */
	public static class ConcurrentSkipListMapSerializer extends MapSerializer<ConcurrentSkipListMap> {
		@Override
		protected void writeHeader (Kryo kryo, Output output, ConcurrentSkipListMap concurrentSkipListMap) {
			kryo.writeClassAndObject(output, concurrentSkipListMap.comparator());
		}

		@Override
		protected ConcurrentSkipListMap create (Kryo kryo, Input input, Class<? extends ConcurrentSkipListMap> type,
			int size) {
			return createConcurrentSkipListMap(type, (Comparator)kryo.readClassAndObject(input));
		}

		@Override
		protected ConcurrentSkipListMap createCopy (Kryo kryo, ConcurrentSkipListMap original) {
			return createConcurrentSkipListMap(original.getClass(), original.comparator());
		}

		private ConcurrentSkipListMap createConcurrentSkipListMap (Class<? extends ConcurrentSkipListMap> type,
			Comparator comparator) {
			if (type == ConcurrentSkipListMap.class || type == null) {
				return new ConcurrentSkipListMap(comparator);
			}
			// Use reflection for subclasses.
			try {
				Constructor constructor = type.getConstructor(Comparator.class);
				if (!constructor.isAccessible()) {
					try {
						constructor.setAccessible(true);
					} catch (SecurityException ignored) {
					}
				}
				return (ConcurrentSkipListMap)constructor.newInstance(comparator);
			} catch (Exception ex) {
				throw new KryoException(ex);
			}
		}
	}

	/** Serializer for {@link TreeMap} and any subclass.
	 * @author Tumi <serverperformance@gmail.com> (enhacements) */
	public static class TreeSetSerializer extends CollectionSerializer<TreeSet> {
		protected void writeHeader (Kryo kryo, Output output, TreeSet treeSet) {
			kryo.writeClassAndObject(output, treeSet.comparator());
		}

		protected TreeSet create (Kryo kryo, Input input, Class<? extends TreeSet> type, int size) {
			return createTreeSet(type, (Comparator)kryo.readClassAndObject(input));
		}

		protected TreeSet createCopy (Kryo kryo, TreeSet original) {
			return createTreeSet(original.getClass(), original.comparator());
		}

		private TreeSet createTreeSet (Class<? extends Collection> type, Comparator comparator) {
			if (type == TreeSet.class || type == null) return new TreeSet(comparator);
			// Use reflection for subclasses.
			try {
				Constructor constructor = type.getConstructor(Comparator.class);
				if (!constructor.isAccessible()) {
					try {
						constructor.setAccessible(true);
					} catch (SecurityException ignored) {
					}
				}
				return (TreeSet)constructor.newInstance(comparator);
			} catch (Exception ex) {
				throw new KryoException(ex);
			}
		}
	}

	/** Serializer for {@link PriorityQueue} and any subclass.
	 * @author Nathan Sweet */
	public static class PriorityQueueSerializer extends CollectionSerializer<PriorityQueue> {
		protected void writeHeader (Kryo kryo, Output output, PriorityQueue queue) {
			kryo.writeClassAndObject(output, queue.comparator());
		}

		protected PriorityQueue create (Kryo kryo, Input input, Class<? extends PriorityQueue> type, int size) {
			return createPriorityQueue(type, size, (Comparator)kryo.readClassAndObject(input));
		}

		protected PriorityQueue createCopy (Kryo kryo, PriorityQueue original) {
			return createPriorityQueue(original.getClass(), original.size(), original.comparator());
		}

		private PriorityQueue createPriorityQueue (Class<? extends Collection> type, int size, Comparator comparator) {
			final int initialCapacity = Math.max(size, 1);
			if (type == PriorityQueue.class || type == null) return new PriorityQueue(initialCapacity, comparator);
			// Use reflection for subclasses.
			try {
				Constructor constructor = type.getConstructor(int.class, Comparator.class);
				if (!constructor.isAccessible()) {
					try {
						constructor.setAccessible(true);
					} catch (SecurityException ignored) {
					}
				}
				return (PriorityQueue)constructor.newInstance(initialCapacity, comparator);
			} catch (Exception ex) {
				throw new KryoException(ex);
			}
		}
	}

	/** Serializer for {@link Locale} (immutables).
	 * @author Tumi <serverperformance@gmail.com> */
	public static class LocaleSerializer extends ImmutableSerializer<Locale> {
		// Missing constants in j.u.Locale for common locale
		public static final Locale SPANISH = new Locale("es", "", "");
		public static final Locale SPAIN = new Locale("es", "ES", "");

		protected Locale create (String language, String country, String variant) {
			// Fast-path for default locale in this system (may not be in the Locale constants list)
			Locale defaultLocale = Locale.getDefault();
			if (isSameLocale(defaultLocale, language, country, variant)) return defaultLocale;
			// Fast-paths for constants declared in java.util.Locale :
			// 1. "US" locale (typical forced default in many applications)
			if (defaultLocale != Locale.US && isSameLocale(Locale.US, language, country, variant)) return Locale.US;
			// 2. Language-only constant locales
			if (isSameLocale(Locale.ENGLISH, language, country, variant)) return Locale.ENGLISH;
			if (isSameLocale(Locale.GERMAN, language, country, variant)) return Locale.GERMAN;
			if (isSameLocale(SPANISH, language, country, variant)) return SPANISH;
			if (isSameLocale(Locale.FRENCH, language, country, variant)) return Locale.FRENCH;
			if (isSameLocale(Locale.ITALIAN, language, country, variant)) return Locale.ITALIAN;
			if (isSameLocale(Locale.JAPANESE, language, country, variant)) return Locale.JAPANESE;
			if (isSameLocale(Locale.KOREAN, language, country, variant)) return Locale.KOREAN;
			if (isSameLocale(Locale.SIMPLIFIED_CHINESE, language, country, variant)) return Locale.SIMPLIFIED_CHINESE;
			if (isSameLocale(Locale.CHINESE, language, country, variant)) return Locale.CHINESE;
			if (isSameLocale(Locale.TRADITIONAL_CHINESE, language, country, variant)) return Locale.TRADITIONAL_CHINESE;
			// 2. Language with Country constant locales
			if (isSameLocale(Locale.UK, language, country, variant)) return Locale.UK;
			if (isSameLocale(Locale.GERMANY, language, country, variant)) return Locale.GERMANY;
			if (isSameLocale(SPAIN, language, country, variant)) return SPAIN;
			if (isSameLocale(Locale.FRANCE, language, country, variant)) return Locale.FRANCE;
			if (isSameLocale(Locale.ITALY, language, country, variant)) return Locale.ITALY;
			if (isSameLocale(Locale.JAPAN, language, country, variant)) return Locale.JAPAN;
			if (isSameLocale(Locale.KOREA, language, country, variant)) return Locale.KOREA;
			// if (isSameLocale(Locale.CHINA, language, country, variant)) // CHINA==SIMPLIFIED_CHINESE, see Locale.java
			// return Locale.CHINA;
			// if (isSameLocale(Locale.PRC, language, country, variant)) // PRC==SIMPLIFIED_CHINESE, see Locale.java
			// return Locale.PRC;
			// if (isSameLocale(Locale.TAIWAN, language, country, variant)) // TAIWAN==SIMPLIFIED_CHINESE, see Locale.java
			// return Locale.TAIWAN;
			if (isSameLocale(Locale.CANADA, language, country, variant)) return Locale.CANADA;
			if (isSameLocale(Locale.CANADA_FRENCH, language, country, variant)) return Locale.CANADA_FRENCH;

			return new Locale(language, country, variant);
		}

		public void write (Kryo kryo, Output output, Locale l) {
			output.writeAscii(l.getLanguage());
			output.writeAscii(l.getCountry());
			output.writeString(l.getVariant());
		}

		public Locale read (Kryo kryo, Input input, Class<? extends Locale> type) {
			String language = input.readString();
			String country = input.readString();
			String variant = input.readString();
			return create(language, country, variant);
		}

		protected static boolean isSameLocale (Locale locale, String language, String country, String variant) {
			return (locale.getLanguage().equals(language) && locale.getCountry().equals(country)
				&& locale.getVariant().equals(variant));
		}
	}

	/** Serializer for {@link Charset}. */
	public static class CharsetSerializer extends ImmutableSerializer<Charset> {
		public void write (Kryo kryo, Output output, Charset object) {
			output.writeString(object.name());
		}

		public Charset read (Kryo kryo, Input input, Class<? extends Charset> type) {
			return Charset.forName(input.readString());
		}
	}

	/** Serializer for {@link URL}. */
	public static class URLSerializer extends ImmutableSerializer<URL> {
		public void write (Kryo kryo, Output output, URL object) {
			output.writeString(object.toExternalForm());
		}

		public URL read (Kryo kryo, Input input, Class<? extends URL> type) {
			try {
				return new java.net.URL(input.readString());
			} catch (MalformedURLException ex) {
				throw new KryoException(ex);
			}
		}
	}

	/** Serializer for {@link Arrays#asList(Object...)}. */
	public static class ArraysAsListSerializer extends CollectionSerializer<List> {
		protected List create (Kryo kryo, Input input, Class type, int size) {
			return new ArrayList(size);
		}

		public List read (Kryo kryo, Input input, Class type) {
			List list = super.read(kryo, input, type);
			if (list == null) return null;
			return Arrays.asList(list.toArray());
		}

		public List copy (Kryo kryo, List original) {
			Object[] copyArr = new Object[original.size()];
			List<Object> copy = Arrays.asList(copyArr);
			kryo.reference(copy);
			for (int i = 0; i < original.size(); i++) {
				copyArr[i] = kryo.copy(original.get(i));
			}
			return copy;
		}
	}

	/** Serializer for {@link BitSet} */
	public static class BitSetSerializer extends Serializer<BitSet> {
		public void write (Kryo kryo, Output output, BitSet set) {
			long[] values = set.toLongArray();
			output.writeVarInt(values.length, true);
			output.writeLongs(values, 0, values.length);
		}

		public BitSet read (Kryo kryo, Input input, Class type) {
			int length = input.readVarInt(true);
			long[] values = input.readLongs(length);
			return BitSet.valueOf(values);
		}

		public BitSet copy (Kryo kryo, BitSet original) {
			return BitSet.valueOf(original.toLongArray());
		}
	}

	/** Serializer for {@link Pattern} */
	public static class PatternSerializer extends ImmutableSerializer<Pattern> {
		public void write (final Kryo kryo, final Output output, final Pattern pattern) {
			output.writeString(pattern.pattern());
			output.writeInt(pattern.flags(), true);
		}

		public Pattern read (final Kryo kryo, final Input input, final Class<? extends Pattern> patternClass) {
			String regex = input.readString();
			int flags = input.readInt(true);
			return Pattern.compile(regex, flags);
		}
	}

	/** Serializer for {@link URI} */
	public static class URISerializer extends ImmutableSerializer<java.net.URI> {
		public void write (Kryo kryo, Output output, URI uri) {
			output.writeString(uri.toString());
		}

		public URI read (Kryo kryo, Input input, Class<? extends URI> uriClass) {
			try {
				return new URI(input.readString());
			} catch (URISyntaxException ex) {
				throw new KryoException(ex);
			}
		}
	}

	/** Serializer for {@link UUID} */
	public static class UUIDSerializer extends ImmutableSerializer<UUID> {
		public void write (Kryo kryo, Output output, UUID uuid) {
			output.writeLong(uuid.getMostSignificantBits());
			output.writeLong(uuid.getLeastSignificantBits());
		}

		public UUID read (final Kryo kryo, final Input input, final Class<? extends UUID> uuidClass) {
			return new UUID(input.readLong(), input.readLong());
		}
	}

	/** Serializer for {@link AtomicBoolean} */
	public static class AtomicBooleanSerializer extends Serializer<AtomicBoolean> {
		public void write (Kryo kryo, Output output, AtomicBoolean object) {
			output.writeBoolean(object.get());
		}

		public AtomicBoolean read (Kryo kryo, Input input, Class<? extends AtomicBoolean> type) {
			return new AtomicBoolean(input.readBoolean());
		}

		public AtomicBoolean copy (Kryo kryo, AtomicBoolean original) {
			return new AtomicBoolean(original.get());
		}
	}

	/** Serializer for {@link AtomicInteger} */
	public static class AtomicIntegerSerializer extends Serializer<AtomicInteger> {
		public void write (Kryo kryo, Output output, AtomicInteger object) {
			output.writeInt(object.get());
		}

		public AtomicInteger read (Kryo kryo, Input input, Class<? extends AtomicInteger> type) {
			return new AtomicInteger(input.readInt());
		}

		public AtomicInteger copy (Kryo kryo, AtomicInteger original) {
			return new AtomicInteger(original.get());
		}
	}

	/** Serializer for {@link AtomicLong} */
	public static class AtomicLongSerializer extends Serializer<AtomicLong> {
		public void write (Kryo kryo, Output output, AtomicLong object) {
			output.writeLong(object.get());
		}

		public AtomicLong read (Kryo kryo, Input input, Class<? extends AtomicLong> type) {
			return new AtomicLong(input.readLong());
		}

		public AtomicLong copy (Kryo kryo, AtomicLong original) {
			return new AtomicLong(original.get());
		}
	}

	/** Serializer for {@link AtomicReference} */
	public static class AtomicReferenceSerializer extends Serializer<AtomicReference> {
		public void write (Kryo kryo, Output output, AtomicReference object) {
			kryo.writeClassAndObject(output, object.get());
		}

		public AtomicReference read (Kryo kryo, Input input, Class<? extends AtomicReference> type) {
			final Object value = kryo.readClassAndObject(input);
			return new AtomicReference(value);
		}

		public AtomicReference copy (Kryo kryo, AtomicReference original) {
			return new AtomicReference<>(kryo.copy(original.get()));
		}
	}
}
