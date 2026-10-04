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
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/** Serializer for the set returned by {@link Collections#newSetFromMap(Map)}, which writes the map that backs it, so eg the map
 * class and comparator are kept. The JDK offers no public API to get the map, so it is read from a private JDK field, like for
 * the unmodifiable and synchronized collections. The Kryo constructor adds it as a default serializer, except on Android. When
 * registration is required, register {@code Collections.newSetFromMap(new HashMap<>()).getClass()}, which is the class for all
 * maps. */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class SetFromMapSerializer extends Serializer<Set> {
	private static final WrappedCollectionGetter mapGetter = new WrappedCollectionGetter("java.util.Collections$SetFromMap", "m");

	public SetFromMapSerializer () {
		setAcceptsNull(false);
	}

	public void write (Kryo kryo, Output output, Set set) {
		kryo.writeClassAndObject(output, mapGetter.get(set));
	}

	public Set read (Kryo kryo, Input input, Class<? extends Set> type) {
		return newSetFromMap((Map)kryo.readClassAndObject(input));
	}

	public Set copy (Kryo kryo, Set original) {
		// The set is created and referenced before the keys are copied, so a key that refers to the set gets the copy.
		Map map = kryo.copyShallow((Map)mapGetter.get(original));
		ArrayList keys = new ArrayList(map.keySet());
		map.clear();
		Set set = Collections.newSetFromMap(map);
		kryo.reference(set);
		for (Object key : keys)
			set.add(kryo.copy(key));
		return set;
	}

	/** {@link Collections#newSetFromMap(Map)} requires an empty map, so the keys are added again. */
	private Set newSetFromMap (Map map) {
		ArrayList keys = new ArrayList(map.keySet());
		map.clear();
		Set set = Collections.newSetFromMap(map);
		set.addAll(keys);
		return set;
	}
}
