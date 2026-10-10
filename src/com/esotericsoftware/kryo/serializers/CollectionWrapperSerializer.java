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

import static com.esotericsoftware.kryo.util.Util.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

/** Serializes an unmodifiable or synchronized wrapper of {@link java.util.Collections} by writing the wrapped collection or map,
 * which is wrapped again when reading or copying. On Android, which doesn't allow access to the wrapped collection, a copy of the
 * elements is written in the same format, see {@link #copyElements(Object)}.
 * <p>
 * A wrapper that is contained in the collection it wraps, directly or indirectly, is read as null there, because the wrapper can
 * only be created after the wrapped collection was read. */
@SuppressWarnings({"rawtypes", "unchecked"})
final class CollectionWrapperSerializer extends Serializer<Object> {
	private final Function<Object, Object> factory;
	private final WrappedCollectionGetter getter;
	private final boolean synchronize;

	/** @param factory Creates the wrapper for a collection or map.
	 * @param synchronize If true, the wrapper is locked while the wrapped collection is written or copied. */
	CollectionWrapperSerializer (Function<Object, Object> factory, WrappedCollectionGetter getter, boolean synchronize) {
		setAcceptsNull(false);
		this.factory = factory;
		this.getter = getter;
		this.synchronize = synchronize;
	}

	public void write (Kryo kryo, Output output, Object wrapper) {
		if (synchronize) {
			synchronized (wrapper) {
				kryo.writeClassAndObject(output, wrapped(wrapper));
			}
		} else
			kryo.writeClassAndObject(output, wrapped(wrapper));
	}

	public Object read (Kryo kryo, Input input, Class<?> type) {
		return factory.apply(kryo.readClassAndObject(input));
	}

	public Object copy (Kryo kryo, Object original) {
		if (synchronize) {
			synchronized (original) {
				return factory.apply(kryo.copy(wrapped(original)));
			}
		}
		return factory.apply(kryo.copy(wrapped(original)));
	}

	private Object wrapped (Object wrapper) {
		return isAndroid ? copyElements(wrapper) : getter.get(wrapper);
	}

	/** Returns a collection or map with the elements of the wrapper, like Apache Fory does on Android: a list for a collection, a
	 * LinkedHashSet or LinkedHashMap, which keep the order, or a TreeSet or TreeMap with the comparator. The wrapper is read as a
	 * wrapper of that collection or map. */
	static Object copyElements (Object wrapper) {
		if (wrapper instanceof SortedMap) {
			TreeMap copy = new TreeMap(((SortedMap)wrapper).comparator());
			copy.putAll((Map)wrapper);
			return copy;
		}
		if (wrapper instanceof Map) return new LinkedHashMap((Map)wrapper);
		if (wrapper instanceof SortedSet) {
			TreeSet copy = new TreeSet(((SortedSet)wrapper).comparator());
			copy.addAll((SortedSet)wrapper);
			return copy;
		}
		if (wrapper instanceof Set) return new LinkedHashSet((Set)wrapper);
		return new ArrayList((Collection)wrapper);
	}
}
