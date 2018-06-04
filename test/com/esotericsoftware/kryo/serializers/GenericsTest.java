/* Copyright (c) 2008-2017, Nathan Sweet
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.io.Output;

public class GenericsTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@Before
	public void setUp () throws Exception {
		super.setUp();
	}

	@Test
	public void testGenericClassWithGenericFields () throws Exception {
		kryo.setReferences(true);
		kryo.setRegistrationRequired(false);
		kryo.register(BaseGeneric.class);

		List list = Arrays.asList(new SerializableObjectFoo("one"), new SerializableObjectFoo("two"),
			new SerializableObjectFoo("three"));
		BaseGeneric<SerializableObjectFoo> bg1 = new BaseGeneric(list);

		roundTrip(120, -1, bg1);
	}

	@Test
	public void testNonGenericClassWithGenericSuperclass () throws Exception {
		kryo.setReferences(true);
		kryo.setRegistrationRequired(false);
		kryo.register(BaseGeneric.class);
		kryo.register(ConcreteClass.class);

		List list = Arrays.asList(new SerializableObjectFoo("one"), new SerializableObjectFoo("two"),
			new SerializableObjectFoo("three"));
		ConcreteClass cc1 = new ConcreteClass(list);

		roundTrip(120, -1, cc1);
	}

	// Test for/from https://github.com/EsotericSoftware/kryo/issues/377
	@Test
	public void testDifferentTypeArguments () throws Exception {
		LongHolder o1 = new LongHolder(1L);
		LongListHolder o2 = new LongListHolder(Arrays.asList(1L));

		kryo.setRegistrationRequired(false);
		Output buffer = new Output(512, 4048);
		kryo.writeClassAndObject(buffer, o1);
		kryo.writeClassAndObject(buffer, o2);
	}

	private interface Holder<V> {
		V getValue ();
	}

	static private abstract class AbstractValueHolder<V> implements Holder<V> {
		private final V value;

		AbstractValueHolder (V value) {
			this.value = value;
		}

		public V getValue () {
			return value;
		}
	}

	static private abstract class AbstractValueListHolder<V> extends AbstractValueHolder<List<V>> {
		AbstractValueListHolder (List<V> value) {
			super(value);
		}
	}

	static private class LongHolder extends AbstractValueHolder<Long> {
		LongHolder (Long value) {
			super(value);
		}
	}

	static private class LongListHolder extends AbstractValueListHolder<Long> {
		LongListHolder (java.util.List<Long> value) {
			super(value);
		}
	}

	// A simple serializable class.
	static private class SerializableObjectFoo implements Serializable {
		String name;

		SerializableObjectFoo (String name) {
			this.name = name;
		}

		public SerializableObjectFoo () {
			name = "Default";
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			SerializableObjectFoo other = (SerializableObjectFoo)obj;
			if (name == null) {
				if (other.name != null) return false;
			} else if (!name.equals(other.name)) return false;
			return true;
		}
	}

	static private class BaseGeneric<T extends Serializable> {

		// The type of this field cannot be derived from the context.
		// Therefore, Kryo should consider it to be Object.
		private final List<T> listPayload;

		/** Kryo Constructor */
		protected BaseGeneric () {
			super();
			this.listPayload = null;
		}

		protected BaseGeneric (final List<T> listPayload) {
			super();
			// Defensive copy, listPayload is mutable
			this.listPayload = new ArrayList(listPayload);
		}

		public final List<T> getPayload () {
			return this.listPayload;
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			BaseGeneric other = (BaseGeneric)obj;
			if (listPayload == null) {
				if (other.listPayload != null) return false;
			} else if (!listPayload.equals(other.listPayload)) return false;
			return true;
		}

	}

	// This is a non-generic class with a generic superclass.
	static private class ConcreteClass2 extends BaseGeneric<SerializableObjectFoo> {
		/** Kryo Constructor */
		ConcreteClass2 () {
			super();
		}

		public ConcreteClass2 (final List listPayload) {
			super(listPayload);
		}
	}

	static private class ConcreteClass1 extends ConcreteClass2 {
		/** Kryo Constructor */
		ConcreteClass1 () {
			super();
		}

		public ConcreteClass1 (final List listPayload) {
			super(listPayload);
		}
	}

	static private class ConcreteClass extends ConcreteClass1 {
		/** Kryo Constructor */
		ConcreteClass () {
			super();
		}

		public ConcreteClass (final List listPayload) {
			super(listPayload);
		}
	}
}
