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

package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.SerializationCompatTestData.Person.Gender;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Testdata for serialization compatibility check. */
@SuppressWarnings("unused")
class SerializationCompatTestData {

	static class TestDataJava8 extends TestData {
		private Optional<String> optionalString;
		private OptionalInt optionalInt;
		private OptionalLong optionalLong;
		private OptionalDouble optionalDouble;

		private Duration duration;
		private Instant instant;
		private LocalDate localDate;
		private LocalTime localTime;
		private LocalDateTime localDateTime;
		private ZoneOffset zoneOffset;
		private ZoneId zoneId;
		private OffsetTime offsetTime;
		private OffsetDateTime offsetDateTime;
		private ZonedDateTime zonedDateTime;
		private Year year;
		private YearMonth yearMonth;
		private Period period;

		TestDataJava8 () {
			optionalString = Optional.of("foo");
			optionalInt = OptionalInt.of(42);
			optionalLong = OptionalLong.of(42L);
			optionalDouble = OptionalDouble.of(42d);

			duration = Duration.ofSeconds(42, 23);
			instant = Instant.ofEpochSecond(42);
			localDate = LocalDate.of(2016, Month.MARCH, 1);
			localTime = LocalTime.of(11, 11, 11, 1111);
			localDateTime = LocalDateTime.of(localDate, localTime);
			zoneOffset = ZoneOffset.ofHours(1);
			zoneId = ZoneId.of("Europe/Berlin");
			offsetTime = OffsetTime.of(localTime, zoneOffset);
			offsetDateTime = OffsetDateTime.of(localDate, localTime, zoneOffset);
			zonedDateTime = ZonedDateTime.of(localDate, localTime, zoneId);
			year = Year.of(2016);
			yearMonth = YearMonth.of(2016, Month.MARCH);
			period = Period.of(11, 11, 11);
		}
	}

	public static class TestData implements Serializable {
		private boolean _boolean;
		private char _char;
		private byte _byte;
		private short _short;
		private int _int1;
		private int _int2;
		private long _long;
		private float _float;
		private double _double;

		private Boolean _Boolean;
		private Character _Character;
		private Byte _Byte;
		private Short _Short;
		private Integer _Integer;
		private Long _Long;
		private Float _Float;
		private Double _Double;

		private BigInteger _bigInteger;
		private BigDecimal _bigDecimal;
		private AtomicInteger _atomicInteger;
		private AtomicLong _atomicLong;

		private String _string;
		private StringBuilder _stringBuilder;
		private StringBuffer _stringBuffer;

		private Class _class;
		private Integer[] _integerArray;
		private Date _date;
		private TimeZone _timeZone;
		private Calendar _calendar;
		private Locale _locale;
		List<Charset> _charsets;
		private URL _url;

		private Gender _enum;
		private EnumSet<Gender> _enumSet;
		private Currency _currency;

		private List<String> _emptyList = Collections.emptyList();
		private Set<String> _emptySet = Collections.emptySet();
		private Map<String, String> _emptyMap = Collections.emptyMap();
		private List<String> _singletonList = Collections.singletonList("foo");
		private Set<String> _singletonSet = Collections.emptySet();
		private Map<String, String> _singletonMap;
		private TreeSet<String> _treeSet;
		private TreeMap<String, Integer> _treeMap;
		private List<String> _arrayList;
		private Set<String> _hashSet;
		private Map<String, Integer> _hashMap;
		private List<Integer> _asList = Arrays.asList(1, 2, 3);
		private int[] _intArray;
		private long[] _longArray;
		private short[] _shortArray;
		private float[] _floatArray;
		private double[] _doubleArray;
		private byte[] _byteArray;
		private char[] _charArray;
		private String[] _stringArray;
		private Person[] _personArray;
		private BitSet _bitSet;

		private Generic<String> _generic;
		private GenericList<String> _genericList;
		private GenericArray<String> _genericArray;
		private PublicClass _public;

		public TestData () {
			_boolean = true;
			_char = 'c';
			_byte = "b".getBytes()[0];
			_short = 1;
			_int1 = -1;
			_int2 = 1;
			_long = 2L;
			_float = 1.0f;
			_double = 1.0d;

			_Boolean = Boolean.TRUE;
			_Character = 'c';
			_Byte = "b".getBytes()[0];
			_Short = (short)8;
			_Integer = 5;
			_Long = 4L;
			_Float = 7f;
			_Double = 6d;

			_bigInteger = new BigInteger("9");
			_bigDecimal = new BigDecimal(9);
			_atomicInteger = new AtomicInteger(10);
			_atomicLong = new AtomicLong(11);

			_string = "3";
			_stringBuffer = new StringBuffer("foo");
			_stringBuilder = new StringBuilder("foo");

			_class = String.class;
			_integerArray = new Integer[] {13};

			_date = new Date(42);
			_calendar = Calendar.getInstance(Locale.ENGLISH);
			_calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
			_calendar.set(2009, Calendar.JANUARY, 25, 10, 29, 0);
			_calendar.set(Calendar.MILLISECOND, 0);

			_timeZone = TimeZone.getTimeZone("America/Los_Angeles");
			_locale = Locale.ENGLISH;
			_charsets = new ArrayList(Arrays.asList(Charset.forName("ISO-8859-1"), Charset.forName("US-ASCII"),
				Charset.forName("UTF-8"), Charset.forName("UTF-16"), Charset.forName("UTF-16BE"), Charset.forName("UTF-16LE")));
			try {
				_url = new java.net.URL("https://github.com/EsotericSoftware/kryo");
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}

			_enum = Gender.FEMALE;
			_enumSet = EnumSet.allOf(Gender.class);

			_currency = Currency.getInstance("EUR");

			_emptyList = Collections.emptyList();
			_emptySet = Collections.emptySet();
			_emptyMap = Collections.emptyMap();
			_singletonList = Collections.singletonList("foo");
			_singletonSet = Collections.singleton("foo");
			_singletonMap = Collections.singletonMap("foo", "bar");
			_treeSet = new TreeSet(Arrays.asList("foo", "bar"));
			_treeMap = new TreeMap();
			_treeMap.put("foo", 23);
			_treeMap.put("bar", 42);
			_arrayList = new ArrayList(Arrays.asList("foo", "bar"));
			_hashSet = new HashSet();
			_hashSet.add("14");
			_hashMap = new HashMap();
			_hashMap.put("foo", 23);
			_hashMap.put("bar", 42);

			_intArray = new int[] {1, 2};
			_longArray = new long[] {1, 2};
			_shortArray = new short[] {1, 2};
			_floatArray = new float[] {1, 2};
			_doubleArray = new double[] {1, 2};
			_byteArray = "42".getBytes();
			_charArray = "42".toCharArray();
			_stringArray = new String[] {"23", "42"};
			_personArray = new Person[] {createPerson("foo", Gender.FEMALE, 42, "foo@example.org", "foo@example.com"),
				createPerson("bar", Gender.MALE, 43, "bar@example.org")};
			// cyclic references
			_personArray[0].addFriend(_personArray[1]);
			_personArray[1].addFriend(_personArray[0]);

			_bitSet = BitSet.valueOf(new long[] {1, 2, 99999, 2345678987654l});

			_generic = new Generic("foo");
			_genericList = new GenericList(new ArrayList(Arrays.asList(new Generic("foo"), new Generic("bar"))));
			_genericArray = new GenericArray(new Generic("foo"), new Generic("bar"));
			_public = new PublicClass(new PrivateClass("foo"));
		}

	}

	static class Generic<T> {
		T item;

		public Generic (final T item) {
			this.item = item;
		}

		public int hashCode () {
			return HashCodeBuilder.reflectionHashCode(this);
		}

		public boolean equals (Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}

		public String toString () {
			return "Generic [item=" + item + "]";
		}
	}

	static class GenericList<T> {
		List<Generic<T>> generics;

		public GenericList (final List<Generic<T>> holders) {
			this.generics = holders;
		}

		public int hashCode () {
			return HashCodeBuilder.reflectionHashCode(this);
		}

		public boolean equals (Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
	}

	static class GenericArray<T> {
		Generic<T>[] holders;

		public GenericArray (final Generic<T>... holders) {
			this.holders = holders;
		}

		public int hashCode () {
			return HashCodeBuilder.reflectionHashCode(this);
		}

		public boolean equals (Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
	}

	static Person createPerson (final String name, final Gender gender, final Integer age, final String... emailAddresses) {
		final Person person = new Person();
		person.setName(name);
		person.setGender(gender);
		person.setAge(age);
		final HashMap<String, Object> props = new HashMap();
		for (int i = 0; i < emailAddresses.length; i++) {
			final String emailAddress = emailAddresses[i];
			props.put("email" + i, new Email(name, emailAddress));
		}
		person.setProps(props);
		return person;
	}

	public static class Person {

		static enum Gender {
			MALE, FEMALE
		}

		private String _name;
		private Gender _gender;
		private Integer _age;
		private Map<String, Object> _props;
		private final Collection<Person> _friends = new ArrayList();

		public String getName () {
			return _name;
		}

		void addFriend (final Person p) {
			_friends.add(p);
		}

		public void setName (final String name) {
			_name = name;
		}

		public Map<String, Object> getProps () {
			return _props;
		}

		void setProps (final Map<String, Object> props) {
			_props = props;
		}

		public Gender getGender () {
			return _gender;
		}

		void setGender (final Gender gender) {
			_gender = gender;
		}

		public Integer getAge () {
			return _age;
		}

		void setAge (final Integer age) {
			_age = age;
		}

		public Collection<Person> getFriends () {
			return _friends;
		}

		private boolean flatEquals (final Collection c1, final Collection c2) {
			return c1 == c2 || c1 != null && c2 != null && c1.size() == c2.size();
		}

		public int hashCode () {
			final int prime = 31;
			int result = 1;
			result = prime * result + (_age == null ? 0 : _age.hashCode());
			result = prime * result + (_friends == null ? 0 : _friends.size());
			result = prime * result + (_gender == null ? 0 : _gender.hashCode());
			result = prime * result + (_name == null ? 0 : _name.hashCode());
			result = prime * result + (_props == null ? 0 : _props.hashCode());
			return result;
		}

		public boolean equals (final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Person other = (Person)obj;
			if (_age == null) {
				if (other._age != null) {
					return false;
				}
			} else if (!_age.equals(other._age)) {
				return false;
			}
			if (_friends == null) {
				if (other._friends != null) {
					return false;
				}
			} else if (!flatEquals(_friends, other._friends)) {
				return false;
			}
			if (_gender == null) {
				if (other._gender != null) {
					return false;
				}
			} else if (!_gender.equals(other._gender)) {
				return false;
			}
			if (_name == null) {
				if (other._name != null) {
					return false;
				}
			} else if (!_name.equals(other._name)) {
				return false;
			}
			if (_props == null) {
				if (other._props != null) {
					return false;
				}
			} else if (!_props.equals(other._props)) {
				return false;
			}
			return true;
		}

		public String toString () {
			return "Person [_age=" + _age + ", _friends.size=" + _friends.size() + ", _gender=" + _gender + ", _name=" + _name
				+ ", _props=" + _props + "]";
		}

	}

	public static class Email implements Serializable {

		private static final long serialVersionUID = 1L;

		private String _name;
		private String _email;

		public Email () {
		}

		public Email (final String name, final String email) {
			super();
			_name = name;
			_email = email;
		}

		public String getName () {
			return _name;
		}

		public void setName (final String name) {
			_name = name;
		}

		public String getEmail () {
			return _email;
		}

		public void setEmail (final String email) {
			_email = email;
		}

		public int hashCode () {
			final int prime = 31;
			int result = 1;
			result = prime * result + (_email == null ? 0 : _email.hashCode());
			result = prime * result + (_name == null ? 0 : _name.hashCode());
			return result;
		}

		public boolean equals (final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Email other = (Email)obj;
			if (_email == null) {
				if (other._email != null) {
					return false;
				}
			} else if (!_email.equals(other._email)) {
				return false;
			}
			if (_name == null) {
				if (other._name != null) {
					return false;
				}
			} else if (!_name.equals(other._name)) {
				return false;
			}
			return true;
		}

		public String toString () {
			return "Email [_email=" + _email + ", _name=" + _name + "]";
		}

	}

	public static class PublicClass {
		PrivateClass privateClass;

		public PublicClass () {
		}

		public PublicClass (final PrivateClass protectedClass) {
			this.privateClass = protectedClass;
		}

		public int hashCode () {
			return HashCodeBuilder.reflectionHashCode(this);
		}

		public boolean equals (Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
	}

	private static class PrivateClass {
		String foo;

		public PrivateClass (String foo) {
			this.foo = foo;
		}

		public int hashCode () {
			return HashCodeBuilder.reflectionHashCode(this);
		}

		public boolean equals (Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
	}

}
