
package com.esotericsoftware.kryo;

import java.util.ArrayList;

public class CopyTest extends KryoTestCase {
	protected void setUp () throws Exception {
		super.setUp();
		kryo.setRegistrationRequired(false);
	}

	public void testBasic () {
		ArrayList test = new ArrayList();
		test.add("one");
		test.add("two");
		test.add("three");

		ArrayList copy = kryo.copy(test);
		assertTrue(test != copy);
		assertEquals(test, copy);
	}

	public void testNested () {
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
		assertTrue(test != copy);
		assertTrue(test.get(3) != copy.get(3));
		assertEquals(test, copy);
	}

	public void testReferences () {
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
		assertTrue(test != copy);
		assertEquals(test, copy);
		assertTrue(test.get(3) != copy.get(4));
		assertTrue(copy.get(3) == copy.get(4));
		assertTrue(copy.get(3) == copy.get(5));
	}

	public void testCircularReferences () {
		ArrayList test = new ArrayList();
		test.add("one");
		test.add("two");
		test.add("three");
		test.add(test);

		ArrayList copy = kryo.copy(test);
		assertTrue(test != copy);
		assertEquals(copy.get(0), "one");
		assertEquals(copy.get(1), "two");
		assertEquals(copy.get(2), "three");
		assertTrue(copy.get(3) == copy);
	}

	public void testShallow () {
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
		assertTrue(test != copy);
		assertTrue(test.get(3) == copy.get(3));
		assertEquals(test, copy);
	}
}
