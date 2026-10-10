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

package com.esotericsoftware.kryo;

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CopyTest extends KryoTestCase {
	@BeforeEach
	public void setUp () throws Exception {
		super.setUp();
		kryo.setRegistrationRequired(false);
	}

	@Test
	void testBasic () {
		ArrayList test = new ArrayList();
		test.add("one");
		test.add("two");
		test.add("three");

		ArrayList copy = kryo.copy(test);
		assertNotSame(test, copy);
		assertEquals(test, copy);
	}

	@Test
	void testMapReferences () {
		// A map that contains itself, and a map that is referenced twice, are copied once.
		for (Map map : new Map[] {new HashMap(), new TreeMap(), new LinkedHashMap(), new ConcurrentHashMap()}) {
			map.put("self", map);
			Map copy = kryo.copy(map);
			assertNotSame(map, copy);
			assertSame(copy, copy.get("self"));
		}
		HashMap shared = new HashMap();
		shared.put("a", "b");
		ArrayList copy = kryo.copy(new ArrayList(List.of(shared, shared)));
		assertNotSame(shared, copy.get(0));
		assertSame(copy.get(0), copy.get(1));
	}

	@Test
	void testImmutableCollectionReferences () {
		// An immutable collection that is referenced twice is copied once, as an immutable collection.
		for (Object shared : new Object[] {List.of("a", "b"), Map.of("a", "b"), Set.of("a", "b")}) {
			ArrayList copy = kryo.copy(new ArrayList(List.of(shared, shared)));
			assertEquals(shared, copy.get(0));
			assertSame(copy.get(0), copy.get(1));
			assertSame(shared.getClass(), copy.get(1).getClass());
		}
	}

	@Test
	void testSerializerWithoutReference () {
		// A serializer that copies nested objects first and doesn't call Kryo#reference still copies a shared object once.
		kryo.register(Box.class, new Serializer<Box>() {
			public void write (Kryo kryo, Output output, Box object) {
			}

			public Box read (Kryo kryo, Input input, Class<? extends Box> type) {
				return null;
			}

			public Box copy (Kryo kryo, Box original) {
				return new Box(kryo.copy(original.value));
			}
		});
		Box box = new Box(new ArrayList(List.of("a")));
		ArrayList copy = kryo.copy(new ArrayList(List.of(box, box)));
		assertNotSame(box, copy.get(0));
		assertSame(copy.get(0), copy.get(1));
	}

	@Test
	void testNestedShallowCopy () {
		// A serializer that makes a shallow copy of one value and a copy of another value, after referencing its copy.
		kryo.register(Pair.class, new Serializer<Pair>() {
			public void write (Kryo kryo, Output output, Pair object) {
			}

			public Pair read (Kryo kryo, Input input, Class<? extends Pair> type) {
				return null;
			}

			public Pair copy (Kryo kryo, Pair original) {
				Pair copy = new Pair();
				copy.a = kryo.copyShallow(original.a);
				kryo.reference(copy);
				copy.b = kryo.copy(original.b);
				return copy;
			}
		});

		// The nested shallow copy doesn't end the outer shallow copy.
		Pair pair = new Pair();
		pair.a = new ArrayList(List.of("a"));
		pair.b = new ArrayList(List.of("b"));
		Pair copy = kryo.copyShallow(pair);
		assertNotSame(pair.a, copy.a);
		assertSame(pair.b, copy.b);

		// The nested shallow copy doesn't drop the reference of the outer copy.
		pair.b = new ArrayList(List.of(pair));
		copy = kryo.copy(pair);
		assertNotSame(pair.b, copy.b);
		assertSame(copy, ((List)copy.b).get(0));
	}

	static class Pair {
		Object a, b;
	}

	static class Box {
		final Object value;

		Box (Object value) {
			this.value = value;
		}
	}

	@Test
	void testNested () {
		ArrayList test = new ArrayList();
		test.add("one");
		test.add("two");
		test.add("three");

		ArrayList test2 = new ArrayList();
		test2.add(1);
		test2.add(2f);
		test2.add(3d);
		test2.add((byte)4);
		test2.add((short)5);
		test.add(test2);

		ArrayList copy = kryo.copy(test);
		assertNotSame(test, copy);
		assertNotSame(test.get(3), copy.get(3));
		assertEquals(test, copy);

		kryo.setCopyReferences(false);
		copy = kryo.copy(test);
		assertNotSame(test, copy);
		assertNotSame(test.get(3), copy.get(3));
		assertEquals(test, copy);
	}

	@Test
	void testReferences () {
		ArrayList test = new ArrayList();
		test.add("one");
		test.add("two");
		test.add("three");

		ArrayList test2 = new ArrayList();
		test2.add(1);
		test2.add(2f);
		test2.add(3d);
		test2.add((byte)4);
		test2.add((short)5);
		test.add(test2);
		test.add(test2);
		test.add(test2);

		ArrayList copy = kryo.copy(test);
		assertNotSame(test, copy);
		assertEquals(test, copy);
		assertNotSame(test.get(3), copy.get(4));
		assertSame(copy.get(3), copy.get(4));
		assertSame(copy.get(3), copy.get(5));

		kryo.setCopyReferences(false);
		copy = kryo.copy(test);
		assertNotSame(test, copy);
		assertEquals(test, copy);
		assertNotSame(test.get(3), copy.get(4));
		assertNotSame(copy.get(3), copy.get(4));
		assertNotSame(copy.get(3), copy.get(5));
	}

	@Test
	void testCircularReferences () {
		ArrayList test = new ArrayList();
		test.add("one");
		test.add("two");
		test.add("three");
		test.add(test);

		ArrayList copy = kryo.copy(test);
		assertNotSame(test, copy);
		assertEquals(copy.get(0), "one");
		assertEquals(copy.get(1), "two");
		assertEquals(copy.get(2), "three");
		assertSame(copy.get(3), copy);

		Moo root = new Moo();
		Moo moo1 = new Moo();
		Moo moo2 = new Moo();
		Moo moo3 = new Moo();
		root.moo = moo1;
		moo1.moo = moo2;
		moo2.moo = moo3;
		moo3.moo = root;
		Moo root2 = kryo.copy(root);
		assertNotSame(root, root2);
		assertNotSame(root.moo, root2.moo);
		assertNotSame(root.moo.moo, root2.moo.moo);
		assertNotSame(root.moo.moo.moo, root2.moo.moo.moo);
		assertNotSame(root.moo.moo.moo.moo, root2.moo.moo.moo.moo);
		assertSame(root.moo.moo.moo.moo, root);
		assertSame(root2.moo.moo.moo.moo, root2);
	}

	@Test
	void testShallow () {
		ArrayList test = new ArrayList();
		test.add("one");
		test.add("two");
		test.add("three");

		ArrayList test2 = new ArrayList();
		test2.add(1);
		test2.add(2f);
		test2.add(3d);
		test2.add((byte)4);
		test2.add((short)5);
		test.add(test2);

		ArrayList copy = kryo.copyShallow(test);
		assertNotSame(test, copy);
		assertSame(test.get(3), copy.get(3));
		assertEquals(test, copy);
	}

	public static class Moo {
		Moo moo;
	}
}
