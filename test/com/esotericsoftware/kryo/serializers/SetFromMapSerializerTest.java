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

import com.esotericsoftware.kryo.KryoTestCase;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SetFromMapSerializerTest extends KryoTestCase {
	{
		supportsCopy = true;
	}

	@BeforeEach
	public void setUp () throws Exception {
		super.setUp();
		kryo.register(Collections.newSetFromMap(new HashMap<>()).getClass());
		kryo.register(HashMap.class);
		kryo.register(LinkedHashMap.class);
		kryo.register(TreeMap.class);
		kryo.register(ConcurrentHashMap.class);
		kryo.register(Comparator.reverseOrder().getClass());
	}

	@Test
	void testSetFromMap () {
		roundTrip(14, setFromMap(new HashMap<>()));
		roundTrip(14, setFromMap(new ConcurrentHashMap<>()));

		// The map class, the order and the comparator are kept.
		Set<String> linked = roundTrip(14, setFromMap(new LinkedHashMap<>()));
		assertEquals(List.of("b", "a", "c"), List.copyOf(linked));
		Set<String> sorted = roundTrip(15, setFromMap(new TreeMap<>(Comparator.reverseOrder())));
		assertEquals(List.of("c", "b", "a"), List.copyOf(sorted));
		sorted.add("d");
		assertEquals("d", sorted.iterator().next());

		roundTrip(3, Collections.newSetFromMap(new HashMap<>()));
	}

	@Test
	void testCopyCycle () {
		// An element that refers to the set gets the copy of the set.
		kryo.register(Node.class);
		Set<Node> set = Collections.newSetFromMap(new LinkedHashMap<>());
		Node node = new Node();
		node.set = set;
		set.add(node);
		Set<Node> copy = kryo.copy(set);
		assertNotSame(set, copy);
		Node copiedNode = copy.iterator().next();
		assertNotSame(node, copiedNode);
		assertSame(copy, copiedNode.set);
	}

	@Test
	void testCopyShallow () {
		kryo.register(Node.class);
		Set<Node> set = Collections.newSetFromMap(new LinkedHashMap<>());
		Node node = new Node();
		set.add(node);
		Set<Node> copy = kryo.copyShallow(set);
		assertNotSame(set, copy);
		assertSame(node, copy.iterator().next()); // The elements are not copied.
	}

	static class Node {
		Set<Node> set;
	}

	private Set<String> setFromMap (Map<String, Boolean> map) {
		Set<String> set = Collections.newSetFromMap(map);
		Collections.addAll(set, "b", "a", "c");
		return set;
	}
}
