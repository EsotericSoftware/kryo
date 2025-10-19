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

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.MapSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

class ReferenceTest extends KryoTestCase {
	public static class Ordering {
		public String order;
	}

	public static class Stuff extends TreeMap {
		public Ordering ordering;

		public Stuff (Ordering ordering) {
			this.ordering = ordering;
		}
	}

	@Test
	void testChildObjectBeforeReference () {
		Ordering ordering = new Ordering();
		ordering.order = "assbackwards";
		Stuff stuff = new Stuff(ordering);
		stuff.put("key", "value");
		stuff.put("something", 456);
		stuff.put("self", stuff);

		Kryo kryo = new Kryo();
		kryo.setRegistrationRequired(false);
		kryo.setReferences(true);
		kryo.addDefaultSerializer(Stuff.class, new MapSerializer<Stuff>() {
			protected void writeHeader (Kryo kryo, Output output, Stuff map) {
				kryo.writeObjectOrNull(output, map.ordering, Ordering.class);
			}

			protected Stuff create (Kryo kryo, Input input, Class<? extends Stuff> type, int size) {
				Ordering ordering = kryo.readObjectOrNull(input, Ordering.class);
				return new Stuff(ordering);
			}
		});

		Output output = new Output(512, -1);
		kryo.writeObject(output, stuff);

		Input input = new Input(output.getBuffer(), 0, output.position());
		Stuff stuff2 = kryo.readObject(input, Stuff.class);

		assertEquals(stuff.ordering.order, stuff2.ordering.order);
		assertEquals(stuff.get("key"), stuff2.get("key"));
		assertEquals(stuff.get("something"), stuff2.get("something"));
		assertSame(stuff.get("self"), stuff);
		assertSame(stuff2.get("self"), stuff2);
	}

	@Test
	void testReadingNestedObjectsFirst () {
		ArrayList list = new ArrayList();
		list.add("1");
		list.add("1");
		list.add("2");
		list.add("1");
		list.add("1");
		ArrayListHolder subList = new ArrayListHolder(list);

		kryo.setRegistrationRequired(false);
		kryo.register(ArrayList.class);
		kryo.register(ArrayListHolder.class);

		roundTrip(15, subList);
	}

	@Test
	void testReadInvalidReference() {
		kryo.setReferences(true);
		kryo.register(Ordering.class);
		final Input input = new Input(new byte[]{3});
		try {
			kryo.readObject(input, Ordering.class);
		} catch (KryoException ex) {
			assertTrue(ex.getMessage().contains("Unable to resolve reference"));
			return;
		}
		fail("Exception was expected");
	}

	public static class ArrayListHolder {
		private List<Object> parent;

		public ArrayListHolder () {
		}

		public ArrayListHolder (List<Object> parent) {
			this.parent = parent;
		}

		public boolean equals (Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ArrayListHolder that = (ArrayListHolder)o;
			return Objects.equals(parent, that.parent);
		}

		public int hashCode () {
			return Objects.hash(parent);
		}
	}
}
