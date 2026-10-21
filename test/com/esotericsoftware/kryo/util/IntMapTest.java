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


package com.esotericsoftware.kryo.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IntMapTest {
	// A removed value is not kept in the map, also if it is removed with an iterator or its key is 0.
	@Test
	void testRemove () {
		IntMap<String> map = new IntMap<>();
		map.put(1, "one");
		assertEquals("one", map.remove(1));
		assertFalse(map.containsValue("one", false));
		assertEquals(-1, map.findKey("one", false, -1));

		map.put(0, "zero");
		map.put(1, "one");
		for (IntMap.Entries<String> entries = map.entries(); entries.hasNext();) {
			entries.next();
			entries.remove();
		}
		assertEquals(0, map.size);
		assertFalse(map.containsValue("zero", true));
		assertFalse(map.containsValue("one", false));
		assertNull(map.put(0, "new"));
	}

	@Test
	void testGetDefaultValue () {
		IntMap<String> map = new IntMap<>();
		assertEquals("default", map.get(0, "default"));
		assertEquals("default", map.get(1, "default"));
		map.put(0, null);
		map.put(1, null);
		assertNull(map.get(0, "default"));
		assertNull(map.get(1, "default"));
	}
}
