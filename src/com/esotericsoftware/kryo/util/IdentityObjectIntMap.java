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

package com.esotericsoftware.kryo.util;

/** An unordered map where identity comparison is used for the objects keys and the values are unboxed ints. Null keys are not
 * allowed. No allocation is done except when growing the table size.
 * <p>
 * This class performs fast contains and remove (typically O(1), worst case O(n) but that is rare in practice). Add may be
 * slightly slower, depending on hash collisions. Load factors greater than 0.91 greatly increase the chances to resize to the
 * next higher POT size.
 * <p>
 * Unordered sets and maps are not designed to provide especially fast iteration.
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal. Linear probing continues to work even
 * when all hashCodes collide, just more slowly.
 * @author Nathan Sweet
 * @author Tommy Ettinger */
public class IdentityObjectIntMap<K> extends ObjectIntMap<K> {
	/** Creates a new map with an initial capacity of 51 and a load factor of 0.8. */
	public IdentityObjectIntMap () {
		super();
	}

	/** Creates a new map with a load factor of 0.8.
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two. */
	public IdentityObjectIntMap (int initialCapacity) {
		super(initialCapacity);
	}

	/** Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two. */
	public IdentityObjectIntMap (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/** Creates a new map identical to the specified map. */
	public IdentityObjectIntMap (IdentityObjectIntMap<K> map) {
		super(map);
	}

	protected int place (K item) {
		return System.identityHashCode(item) & mask;
	}

	public int get (K key, int defaultValue) {
		for (int i = place(key);; i = i + 1 & mask) {
			K other = keyTable[i];
			if (other == null) return defaultValue;
			if (other == key) return valueTable[i];
		}
	}

	int locateKey (K key) {
		if (key == null) throw new IllegalArgumentException("key cannot be null.");
		K[] keyTable = this.keyTable;
		for (int i = place(key);; i = i + 1 & mask) {
			K other = keyTable[i];
			if (other == null) return -(i + 1); // Empty space is available.
			if (other == key) return i; // Same key was found.
		}
	}

	public int hashCode () {
		int h = size;
		K[] keyTable = this.keyTable;
		int[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null) h += System.identityHashCode(key) + valueTable[i];
		}
		return h;
	}
}
