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

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

/** @author Robert DiFalco <robert.difalco@gmail.com> */
class ExternalizableSerializerTest extends KryoTestCase {
	@Test
	void testRegister () {
		kryo.register(TestClass.class, new ExternalizableSerializer());
		kryo.register(String.class, new DefaultSerializers.StringSerializer());
		TestClass test = new TestClass();
		test.stringField = "fubar";
		test.intField = 54321;

		roundTrip(11, test);
		roundTrip(11, test);
		roundTrip(11, test);
	}

	@Test
	void testDefault () {
		kryo.setRegistrationRequired(false);
		kryo.addDefaultSerializer(Externalizable.class, new ExternalizableSerializer());
		TestClass test = new TestClass();
		test.stringField = "fubar";
		test.intField = 54321;
		roundTrip(88, test);
		roundTrip(88, test);
		roundTrip(88, test);
	}

	@Test
	void testReadResolve () {
		kryo.setRegistrationRequired(false);
		kryo.addDefaultSerializer(Externalizable.class, ExternalizableSerializer.class);

		ReadResolvable test = new ReadResolvable("foobar");
		Output output = new Output(1024);
		kryo.writeClassAndObject(output, test);
		output.flush();

		Input input = new Input(output.getBuffer());
		Object result = kryo.readClassAndObject(input);
		input.close();

		// ensure read resolve happened!
		assertEquals(String.class, result.getClass());
		assertEquals(test.value, result);
	}

	@Test
	void testTwoClasses () {
		kryo.setRegistrationRequired(false);
		kryo.addDefaultSerializer(Externalizable.class, ExternalizableSerializer.class);

		ReadResolvable test1 = new ReadResolvable("foobar");
		TestClass test2 = new TestClass();
		test2.stringField = "fubar";
		test2.intField = 54321;

		List list = new ArrayList();
		list.add(test1);
		list.add(test2);
		Output output = new Output(1024);
		kryo.writeClassAndObject(output, list);
		output.flush();

		Input input = new Input(output.getBuffer());
		List result = (List)kryo.readClassAndObject(input);
		input.close();

		// ensure read resolve happened!
		assertEquals(result.get(0), test1.value);
		assertEquals(result.get(1), test2);
	}

	public static class TestClass implements Externalizable {
		String stringField;
		int intField;

		public boolean equals (Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			TestClass other = (TestClass)obj;
			if (intField != other.intField) {
				return false;
			}
			if (stringField == null) {
				if (other.stringField != null) {
					return false;
				}
			} else if (!stringField.equals(other.stringField)) {
				return false;
			}
			return true;
		}

		public void writeExternal (ObjectOutput out) throws IOException {
			out.writeObject(stringField);
			out.writeInt(intField);
		}

		public void readExternal (ObjectInput in) throws IOException, ClassNotFoundException {
			stringField = (String)in.readObject();
			intField = in.readInt();
		}
	}

	public static class AnotherTestClass implements Externalizable {
		private Date dateField;
		private long longField;

		public boolean equals (Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			AnotherTestClass other = (AnotherTestClass)obj;
			if (longField != other.longField) {
				return false;
			}
			if (dateField == null) {
				if (other.dateField != null) {
					return false;
				}
			} else if (!dateField.equals(other.dateField)) {
				return false;
			}
			return true;
		}

		public void writeExternal (ObjectOutput out) throws IOException {
			out.writeObject(dateField);
			out.writeLong(longField);
		}

		public void readExternal (ObjectInput in) throws IOException, ClassNotFoundException {
			dateField = (Date)in.readObject();
			longField = in.readInt();
		}
	}

	public static class ReadResolvable implements Externalizable {
		String value;
		private Object makeSureNullWorks;

		public ReadResolvable () {
		}

		public ReadResolvable (String value) {
			this.value = value;
		}

		public void writeExternal (ObjectOutput out) throws IOException {
			out.writeObject(value);
		}

		public void readExternal (ObjectInput in) throws IOException, ClassNotFoundException {
			value = (String)in.readObject();
		}

		private Object readResolve () {
			return value;
		}
	}
}
