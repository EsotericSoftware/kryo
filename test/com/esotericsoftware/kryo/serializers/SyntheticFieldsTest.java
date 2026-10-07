/* Copyright (c) 2008-2026, Nathan Sweet
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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Kryo5Compatibility;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.SerializerFactory.FieldSerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer.FieldSerializerConfig;
import com.esotericsoftware.kryo.serializers.FieldSerializerTest.LoggerStub;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.util.Log;
import com.esotericsoftware.kryo.util.Log.Logger;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import com.esotericsoftware.kryo.util.StdInstantiatorStrategy;

/** Synthetic fields are ignored by default like in Kryo 5, with a warning for inner classes. They can be serialized: the outer
 * instance and captured variables of inner classes. */
class SyntheticFieldsTest {
	static Kryo newKryo (Boolean ignoreSyntheticFields) {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		if (ignoreSyntheticFields != null) {
			FieldSerializerFactory factory = new FieldSerializerFactory();
			factory.getConfig().setIgnoreSyntheticFields(ignoreSyntheticFields);
			kryo.setDefaultSerializer(factory);
		}
		return kryo;
	}

	static <T> T roundTrip (Kryo kryo, T object) {
		Output output = new Output(1024, -1);
		kryo.writeObject(output, object);
		return (T)kryo.readObject(new Input(output.toBytes()), object.getClass());
	}

	/** Returns the warnings logged while the serializers for the objects are created. */
	static List<String> warnings (Kryo kryo, Object... objects) {
		LoggerStub logger = new LoggerStub();
		Log.setLogger(logger);
		CachedFields.syntheticFieldsWarned = false;
		try {
			for (Object object : objects)
				kryo.getSerializer(object.getClass());
		} finally {
			Log.setLogger(new Logger());
			CachedFields.syntheticFieldsWarned = true; // Keeps the test output quiet.
		}
		return logger.messages;
	}

	@Test
	void testIgnoredByDefault () {
		Kryo kryo = newKryo(null);
		assertTrue(new FieldSerializerConfig().getIgnoreSyntheticFields());
		Outer outer = new Outer("outer");
		Outer.Member member = outer.new Member();
		Supplier<String> anonymous = new Supplier<String>() {
			public String get () {
				return outer.name;
			}
		};

		// The first ignored synthetic field of an inner class is logged once.
		List<String> warnings = warnings(kryo, member, anonymous);
		assertEquals(1, warnings.size(), warnings.toString());
		assertTrue(warnings.get(0).contains(Outer.Member.class.getName()), warnings.get(0));
		assertTrue(warnings.get(0).contains("setIgnoreSyntheticFields(false)"), warnings.get(0));
		assertNull(roundTrip(kryo, member).outer());

		// Not logged if the setting was set.
		assertEquals(0, warnings(newKryo(true), member, anonymous).size());

		// Like Kryo 5, so Kryo5Compatibility doesn't need to change it.
		kryo = newKryo(null);
		Kryo5Compatibility.configure(kryo);
		assertNull(roundTrip(kryo, member).outer());
	}

	@Test
	void testInnerClasses () {
		Kryo kryo = newKryo(false);
		Outer outer = new Outer("outer");

		// A non-static member class refers to its outer instance.
		Outer.Member member = roundTrip(kryo, outer.new Member());
		assertEquals("outer", member.outerName());

		// An anonymous class captures the outer instance and local variables.
		String local = "local";
		Supplier<String> anonymous = roundTrip(kryo, new Supplier<String>() {
			public String get () {
				return outer.name + " " + local;
			}
		});
		assertEquals("outer local", anonymous.get());

		// A local class too.
		class Local implements Supplier<String> {
			public String get () {
				return outer.name + " " + local;
			}
		}
		assertEquals("outer local", roundTrip(kryo, new Local()).get());
	}

	@Test
	void testCycleThroughOuterInstance () {
		// The outer instance refers back to the inner object, which needs references. Without them, the error explains the setting.
		Kryo kryo = newKryo(false);
		Outer outer = new Outer("outer");
		outer.member = outer.new Member();
		KryoException ex = assertThrows(KryoException.class, () -> roundTrip(kryo, outer.member));
		assertTrue(ex.getMessage().contains("setIgnoreSyntheticFields"), ex.getMessage());

		kryo.setReferences(true);
		Outer.Member member = roundTrip(kryo, outer.member);
		assertEquals("outer", member.outerName());
		assertSame(member, member.outer().member);
	}

	static public class Outer {
		String name;
		Member member;

		public Outer () {
		}

		Outer (String name) {
			this.name = name;
		}

		public class Member {
			String outerName () {
				return name;
			}

			Outer outer () {
				return Outer.this;
			}
		}

	}
}
