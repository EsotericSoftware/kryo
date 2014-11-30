/* Copyright (c) 2008, Nathan Sweet
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

package com.esotericsoftware.kryo.factories;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

import static com.esotericsoftware.kryo.util.Util.className;

/**
 * This factory instantiates new serializers of a given class via reflection. The constructors of the given {@code serializerClass}
 * must either take an instance of {@link Kryo} and an instance of {@link Class} as its parameter, take only a {@link Kryo} or {@link Class}
 * as its only argument or take no arguments. If several of the described constructors are found, the first found constructor is used,
 * in the order as they were just described.
 *
 * @author Rafael Winterhalter <rafael.wth@web.de>
 */
public class ReflectionSerializerFactory implements SerializerFactory {

	private final Class<? extends Serializer> serializerClass;

	public ReflectionSerializerFactory (Class<? extends Serializer> serializerClass) {
		this.serializerClass = serializerClass;
	}

	@Override
	public Serializer makeSerializer (Kryo kryo, Class<?> type) {
		return makeSerializer(kryo, serializerClass, type);
	}

	/** Creates a new instance of the specified serializer for serializing the specified class. Serializers must have a zero
	 * argument constructor or one that takes (Kryo), (Class), or (Kryo, Class).
	*/
	public static Serializer makeSerializer (Kryo kryo, Class<? extends Serializer> serializerClass, Class<?> type) {
		try {
			try {
				return serializerClass.getConstructor(Kryo.class, Class.class).newInstance(kryo, type);
			} catch (NoSuchMethodException ex1) {
				try {
					return serializerClass.getConstructor(Kryo.class).newInstance(kryo);
				} catch (NoSuchMethodException ex2) {
					try {
						return serializerClass.getConstructor(Class.class).newInstance(type);
					} catch (NoSuchMethodException ex3) {
						return serializerClass.newInstance();
					}
				}
			}
		} catch (Exception ex) {
			throw new IllegalArgumentException("Unable to create serializer \"" + serializerClass.getName() + "\" for class: " + className(type), ex);
		}

	}
}
