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

package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Reads and writes objects to and from bytes.
 * @author Nathan Sweet */
public abstract class Serializer<T> {
	private boolean acceptsNull, immutable;

	public Serializer () {
	}

	/** @see #setAcceptsNull(boolean) */
	public Serializer (boolean acceptsNull) {
		this.acceptsNull = acceptsNull;
	}

	/** @see #setAcceptsNull(boolean)
	 * @see #setImmutable(boolean) */
	public Serializer (boolean acceptsNull, boolean immutable) {
		this.acceptsNull = acceptsNull;
		this.immutable = immutable;
	}

	/** Writes the bytes for the object to the output.
	 * <p>
	 * This method should not be called directly, instead this serializer can be passed to {@link Kryo} write methods that accept a
	 * serialier.
	 * @param object May be null if {@link #getAcceptsNull()} is true. */
	public abstract void write (Kryo kryo, Output output, T object);

	/** Reads bytes and returns a new object of the specified concrete type.
	 * <p>
	 * Before Kryo can be used to read child objects, {@link Kryo#reference(Object)} must be called with the parent object to
	 * ensure it can be referenced by the child objects. Any serializer that uses {@link Kryo} to read a child object may need to
	 * be reentrant.
	 * <p>
	 * This method should not be called directly, instead this serializer can be passed to {@link Kryo} read methods that accept a
	 * serialier.
	 * @return May be null if {@link #getAcceptsNull()} is true. */
	public abstract T read (Kryo kryo, Input input, Class<? extends T> type);

	public boolean getAcceptsNull () {
		return acceptsNull;
	}

	/** If true, this serializer will handle writing and reading null values. If false, the Kryo framework handles null values and
	 * the serializer will never receive null.
	 * <p>
	 * This can be set to true on a serializer that does not accept nulls if it is known that the serializer will never encounter
	 * null. Doing this will prevent the framework from writing a byte to denote null. */
	public void setAcceptsNull (boolean acceptsNull) {
		this.acceptsNull = acceptsNull;
	}

	public boolean isImmutable () {
		return immutable;
	}

	/** If true, the type this serializer will be used for is considered immutable. This causes {@link #copy(Kryo, Object)} to
	 * return the original object. */
	public void setImmutable (boolean immutable) {
		this.immutable = immutable;
	}

	/** Returns a copy of the specified object. The default implementation returns the original if {@link #isImmutable()} is true,
	 * else throws {@link KryoException}. Subclasses can optionall override this method to support {@link Kryo#copy(Object)}.
	 * <p>
	 * Before Kryo can be used to copy child objects, {@link Kryo#reference(Object)} must be called with the copy to ensure it can
	 * be referenced by the child objects. A serializer that uses {@link Kryo} to copy a child object may need to be reentrant.
	 * <p>
	 * This method should not be called directly, instead this serializer can be passed to {@link Kryo} copy methods that accept a
	 * serialier. */
	public T copy (Kryo kryo, T original) {
		if (isImmutable()) return original;
		throw new KryoException("Serializer does not support copy: " + getClass().getName());
	}
}
