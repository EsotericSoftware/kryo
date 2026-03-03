/* Copyright (c) 2008-2022, Nathan Sweet
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

import java.lang.reflect.Array;

/** LUT by array.
 *
 * @apiNote don't specify large int to key by {@link #put(int, Object)}.
 * @author lifeinwild1@gmail.com */
public final class IntToObjArray<E> {
	private final Class<E> valueType;
	private final int initialCapacity;
	private final float expandRate;
	private E[] array;

	public final E get (int key) {
		if (key >= array.length || key < 0) {
			return null;
		}
		return array[key];
	}

	IntToObjArray (Class<E> valueType, int initialCapacity, float expansionRate) {
		if (expansionRate <= 1.0)
			throw new IllegalArgumentException("expansionRate <= 1.0");
		if (initialCapacity <= 0)
			throw new IllegalArgumentException("initialCapacity <= 0");
		this.valueType = valueType;
		array = (E[])Array.newInstance(valueType, initialCapacity);
		this.initialCapacity = initialCapacity;
		this.expandRate = expansionRate;
	}

	IntToObjArray (Class<E> valueType, int initialCapacity) {
		this(valueType, initialCapacity, 1.1f);
	}

	IntToObjArray (Class<E> valueType) {
		this(valueType, 1000);
	}

	public void clear () {
		array = (E[])Array.newInstance(valueType, initialCapacity);
	}

	public E remove (int classid) {
		if (classid >= array.length)
			return null;
		E r = array[classid];
		array[classid] = null;
		return r;
	}

	public E put (int classid, E v) {
		if (classid >= array.length && array.length < Integer.MAX_VALUE) {
			int nextSize = (int)(classid * expandRate);
			E[] next = (E[])Array.newInstance(valueType, nextSize);
			System.arraycopy(array, 0, next, 0, array.length);
			array = next;
		}

		E r = array[classid];
		array[classid] = v;
		return r;
	}
}
