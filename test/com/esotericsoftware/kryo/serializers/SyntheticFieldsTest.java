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
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import com.esotericsoftware.kryo.util.StdInstantiatorStrategy;

/** The synthetic fields of inner classes are serialized by default: the outer instance and captured variables. */
class SyntheticFieldsTest {
	static Kryo newKryo () {
		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		return kryo;
	}

	static <T> T roundTrip (Kryo kryo, T object) {
		Output output = new Output(1024, -1);
		kryo.writeObject(output, object);
		return (T)kryo.readObject(new Input(output.toBytes()), object.getClass());
	}

	@Test
	void testInnerClasses () {
		Kryo kryo = newKryo();
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
		Kryo kryo = newKryo();
		Outer outer = new Outer("outer");
		outer.member = outer.new Member();
		KryoException ex = assertThrows(KryoException.class, () -> roundTrip(kryo, outer.member));
		assertTrue(ex.getMessage().contains("setIgnoreSyntheticFields"), ex.getMessage());

		kryo.setReferences(true);
		Outer.Member member = roundTrip(kryo, outer.member);
		assertEquals("outer", member.outerName());
		assertSame(member, member.outer().member);
	}

	@Test
	void testIgnoreSyntheticFields () {
		// Like Kryo 5: the outer instance is not serialized, it is null after reading.
		Kryo kryo = newKryo();
		FieldSerializerFactory factory = new FieldSerializerFactory();
		factory.getConfig().setIgnoreSyntheticFields(true);
		kryo.setDefaultSerializer(factory);
		Outer outer = new Outer("outer");
		outer.member = outer.new Member();
		Outer.Member member = roundTrip(kryo, outer.member);
		assertNull(member.outer());

		kryo = newKryo();
		Kryo5Compatibility.configure(kryo);
		assertEquals(Boolean.TRUE, ((FieldSerializer)kryo.getDefaultSerializer(Outer.Member.class)).getFieldSerializerConfig()
			.getIgnoreSyntheticFields());
		member = roundTrip(kryo, outer.member);
		assertNull(member.outer());

		// The default depends on the class.
		FieldSerializerConfig config = new FieldSerializerConfig();
		assertNull(config.getIgnoreSyntheticFields());
		assertFalse(config.ignoresSyntheticFields(Outer.Member.class));
		assertTrue(config.ignoresSyntheticFields(Outer.class));
		assertTrue(config.ignoresSyntheticFields(Outer.Nested.class));
		config.setIgnoreSyntheticFields(false);
		assertFalse(config.ignoresSyntheticFields(Outer.Nested.class));
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

		static public class Nested {
		}
	}
}
