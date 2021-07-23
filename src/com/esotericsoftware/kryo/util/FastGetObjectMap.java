/* Copyright (c) 2008-2020, Nathan Sweet
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

/** this map extends from objectMap, optimized for better reading performance so there will be some tricky optimization **/
public class FastGetObjectMap<K, V> extends ObjectMap<K, V> {
	/** Creates a new map with an initial capacity of 51 and a load factor of 0.8. */
	public FastGetObjectMap () {
		this(51, 0.8f);
	}

	/** Creates a new map with a load factor of 0.8.
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two. */
	public FastGetObjectMap (int initialCapacity) {
		this(initialCapacity, 0.8f);
	}

	/** Creates a new map with a initial load factor of 0.8(which may be overloaded in resize funtion).
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two. */
	public FastGetObjectMap (int initialCapacity, float initialLoadFactor) {
		super(initialCapacity, initialLoadFactor);
	}

	/** Returns the value for the specified key, or null if the key is not in the map. unroll because of better performance
	 * (benchmark shows about 2% higher performance) */
	@Override
	@Null
	public <T extends K> V get (T key) {
		K[] keyTable = this.keyTable;
		for (int i = place(key);; i = i + 1 & mask) {
			K other = keyTable[i];
			if (key.equals(other)) return valueTable[i]; // Same key was found.
			if (other == null) return null; // Empty space is available.
		}
	}

	@Override
	/** Returns the value for the specified key, or the default value if the key is not in the map. unroll because of better
	 * performance */
	public V get (K key, @Null V defaultValue) {
		K[] keyTable = this.keyTable;
		for (int i = place(key);; i = i + 1 & mask) {
			K other = keyTable[i];
			if (key.equals(other)) return valueTable[i]; // Same key was found.
			if (other == null) return defaultValue; // Empty space is available.
		}
	}

	/** 1. remove magic number so that minimize the computation cost 2. with low loadFactor, we don't need to consider the hash
	 * collision **/
	@Override
	protected int place (K item) {
		return item.hashCode() & mask;
	}

	/* According to previous benchmark, different size have different best loadFactor **/
	@Override
	protected float computeLoadFactor (int newSize) {
		if (newSize <= 2048) {
			return 0.7f;
		} else {
			return 0.5f;
		}
	}
}
