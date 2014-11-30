/* Copyright (c) 2008, Nathan Sweet
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

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.MapSerializer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ReferenceTest extends KryoTestCase {
	static public class Ordering {
		public String order;
	}

	static public class Stuff extends TreeMap {
		public Ordering ordering;

		public Stuff (Ordering ordering) {
			this.ordering = ordering;
		}
	}

	public void testChildObjectBeforeReference () {
		Ordering ordering = new Ordering();
		ordering.order = "assbackwards";
		Stuff stuff = new Stuff(ordering);
		stuff.put("key", "value");
		stuff.put("something", 456);
		stuff.put("self", stuff);

		Kryo kryo = new Kryo();
		kryo.addDefaultSerializer(Stuff.class, new MapSerializer() {
			public void write (Kryo kryo, Output output, Map object) {
				kryo.writeObjectOrNull(output, ((Stuff)object).ordering, Ordering.class);
				super.write(kryo, output, object);
			}

			protected Map create (Kryo kryo, Input input, Class<Map> type) {
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
		assertTrue(stuff.get("self") == stuff);
		assertTrue(stuff2.get("self") == stuff2);
	}

	public void testReadingNestedObjectsFirst () {
		ArrayList list = new ArrayList();
		list.add("1");
		list.add("1");
		list.add("2");
		list.add("1");
		list.add("1");
		List subList = list.subList(0, 5);

		kryo.setRegistrationRequired(false);
		kryo.register(ArrayList.class);
		Class<List> subListClass = (Class<List>)subList.getClass();
		if(subListClass.getName().equals("java.util.ArrayList$SubList")) {
			// This is JDK > = 1.7
			kryo.register(subList.getClass(), new ArraySubListSerializer());			
		} else {
			kryo.register(subList.getClass(), new SubListSerializer());
		    
		}
		roundTrip(26, 26,  subList);
	}

	static public class SubListSerializer extends Serializer<List> {
		private Field listField, offsetField, sizeField;

		public SubListSerializer () {
			try {
				Class sublistClass = Class.forName("java.util.SubList");
				listField = sublistClass.getDeclaredField("l");
				offsetField = sublistClass.getDeclaredField("offset");
				sizeField = sublistClass.getDeclaredField("size");
				listField.setAccessible(true);
				offsetField.setAccessible(true);
				sizeField.setAccessible(true);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		public void write (Kryo kryo, Output output, List list) {
			try {
				kryo.writeClassAndObject(output, listField.get(list));
				int fromIndex = offsetField.getInt(list);
				int count = sizeField.getInt(list);
				output.writeInt(fromIndex);
				output.writeInt(count);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		public List read (Kryo kryo, Input input, Class<List> type) {
			List list = (List)kryo.readClassAndObject(input);
			int fromIndex = input.readInt();
			int count = input.readInt();
			return list.subList(fromIndex, fromIndex + count);
		}
	}

	static public class ArraySubListSerializer extends Serializer<List> {
		private Field parentField, offsetField, sizeField;

		public ArraySubListSerializer () {
			try {
				Class sublistClass = Class.forName("java.util.ArrayList$SubList");
				parentField = sublistClass.getDeclaredField("parent");
				offsetField = sublistClass.getDeclaredField("offset");
				sizeField = sublistClass.getDeclaredField("size");
				parentField.setAccessible(true);
				offsetField.setAccessible(true);
				sizeField.setAccessible(true);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		public void write (Kryo kryo, Output output, List list) {
			try {
				kryo.writeClassAndObject(output, parentField.get(list));
				int offset = offsetField.getInt(list);
				int size = sizeField.getInt(list);
				output.writeInt(offset);
				output.writeInt(size);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		public List read (Kryo kryo, Input input, Class<List> type) {
			List list = (List)kryo.readClassAndObject(input);
			int offset = input.readInt();
			int size = input.readInt();
			return list.subList(offset, offset + size);
		}
	}
}
