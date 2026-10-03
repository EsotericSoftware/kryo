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

import java.util.function.Function;

/** Serializes an unmodifiable or synchronized wrapper of {@link java.util.Collections} by writing the wrapped collection or map,
 * which is wrapped again when reading or copying.
 * <p>
 * A wrapper that is contained in the collection it wraps, directly or indirectly, is read as null there, because the wrapper can
 * only be created after the wrapped collection was read. */
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
		Object wrapped = getter.get(wrapper);
		if (synchronize) {
			synchronized (wrapper) {
				kryo.writeClassAndObject(output, wrapped);
			}
		} else
			kryo.writeClassAndObject(output, wrapped);
	}

	public Object read (Kryo kryo, Input input, Class<?> type) {
		return factory.apply(kryo.readClassAndObject(input));
	}

	public Object copy (Kryo kryo, Object original) {
		Object wrapped = getter.get(original);
		if (synchronize) {
			synchronized (original) {
				return factory.apply(kryo.copy(wrapped));
			}
		}
		return factory.apply(kryo.copy(wrapped));
	}
}
