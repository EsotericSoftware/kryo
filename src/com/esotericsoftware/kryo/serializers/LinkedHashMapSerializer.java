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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.LinkedHashMap;

/** Serializes {@link LinkedHashMap} like {@link MapSerializer}, and also whether its iteration order is the access order, eg for
 * an LRU cache created with {@code new LinkedHashMap<>(16, 0.75f, true)}. MapSerializer creates a LinkedHashMap with insertion
 * order. A subclass of LinkedHashMap is created with its no-arg constructor, which usually sets the access order.
 * <p>
 * This serializer is not used by default:
 * 
 * <pre>
 * kryo.register(LinkedHashMap.class, new LinkedHashMapSerializer());
 * </pre>
 * 
 * The access order is a private field of LinkedHashMap. It is read with a method handle if java.util is open to Kryo, eg with
 * {@code --add-opens java.base/java.util=ALL-UNNAMED}, or else with Unsafe, which warns on Java 24+. If neither is allowed, an
 * exception explains how to allow it. */
public class LinkedHashMapSerializer extends MapSerializer<LinkedHashMap> {
	private static final WrappedCollectionGetter accessOrder = new WrappedCollectionGetter("java.util.LinkedHashMap",
		"accessOrder");

	protected void writeHeader (Kryo kryo, Output output, LinkedHashMap map) {
		output.writeBoolean(accessOrder(map));
	}

	protected LinkedHashMap create (Kryo kryo, Input input, Class<? extends LinkedHashMap> type, int size) {
		boolean accessOrder = input.readBoolean();
		if (type != LinkedHashMap.class) return super.create(kryo, input, type, size);
		return new LinkedHashMap(capacity(size), 0.75f, accessOrder);
	}

	protected LinkedHashMap createCopy (Kryo kryo, LinkedHashMap original) {
		if (original.getClass() != LinkedHashMap.class) return super.createCopy(kryo, original);
		return new LinkedHashMap(capacity(original.size()), 0.75f, accessOrder(original));
	}

	/** Returns the access order of a LinkedHashMap, or false for a subclass, whose constructor sets it. */
	private static boolean accessOrder (LinkedHashMap map) {
		return map.getClass() == LinkedHashMap.class && (Boolean)accessOrder.get(map);
	}

	/** Returns the capacity for the size with the default load factor, like {@link MapSerializer} for HashMap. */
	private static int capacity (int size) {
		if (size < 3) return size + 1;
		if (size < 1073741824) return (int)(size / 0.75f + 1); // Max POT.
		return size;
	}
}
