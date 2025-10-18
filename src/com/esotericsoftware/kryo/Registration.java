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

import static com.esotericsoftware.kryo.util.Util.*;
import static com.esotericsoftware.minlog.Log.*;

import org.objenesis.instantiator.ObjectInstantiator;

/** Describes the {@link Serializer} and class ID to use for a class.
 * @author Nathan Sweet */
public class Registration {
	private final Class type;
	private final boolean typeNameAscii;
	private final int id;
	private Serializer serializer;
	private ObjectInstantiator instantiator;

	public Registration (Class type, Serializer serializer, int id) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		this.type = type;
		this.serializer = serializer;
		this.id = id;
		typeNameAscii = isAscii(type.getName());
	}

	public Class getType () {
		return type;
	}

	public boolean isTypeNameAscii () {
		return typeNameAscii;
	}

	/** Returns the registered class ID.
	 * @see Kryo#register(Class) */
	public int getId () {
		return id;
	}

	public Serializer getSerializer () {
		return serializer;
	}

	public void setSerializer (Serializer serializer) {
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		this.serializer = serializer;
		if (TRACE) trace("kryo", "Update registered serializer: " + type.getName() + " (" + serializer.getClass().getName() + ")");
	}

	/** @return May be null if not yet set. */
	public ObjectInstantiator getInstantiator () {
		return instantiator;
	}

	/** Sets the instantiator that will create a new instance of the type in {@link Kryo#newInstance(Class)}. */
	public void setInstantiator (ObjectInstantiator instantiator) {
		if (instantiator == null) throw new IllegalArgumentException("instantiator cannot be null.");
		this.instantiator = instantiator;
	}

	public String toString () {
		return "[" + id + ", " + className(type) + "]";
	}
}
