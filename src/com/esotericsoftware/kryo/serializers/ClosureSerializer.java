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

package com.esotericsoftware.kryo.serializers;

import java.lang.reflect.Method;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Serializer for Java8 closures. To serialize closures, use:
 * <p>
 * <code>
 * kryo.register(java.lang.invoke.SerializedLambda.class);<br>
 * kryo.register(Closure.class, new ClosureSerializer());</code>
 * @author Roman Levenstein <romixlev@gmail.com> */
public class ClosureSerializer extends Serializer {

	private static Method readResolve;
	private static Class serializedLambda;
	static {
		try {
			serializedLambda = Class.forName("java.lang.invoke.SerializedLambda");
			readResolve = serializedLambda.getDeclaredMethod("readResolve");
			readResolve.setAccessible(true);
		} catch (Exception e) {
			throw new RuntimeException("Could not obtain SerializedLambda or its methods via reflection", e);
		}
	}

	public ClosureSerializer () {
	}

	public void write (Kryo kryo, Output output, Object object) {
		try {
			Class type = object.getClass();
			Method writeReplace = type.getDeclaredMethod("writeReplace");
			writeReplace.setAccessible(true);
			Object replacement = writeReplace.invoke(object);
			if (serializedLambda.isInstance(replacement)) {
				// Serialize the representation of this lambda
				kryo.writeObject(output, replacement);
			} else
				throw new RuntimeException("Could not serialize lambda");
		} catch (Exception e) {
			throw new RuntimeException("Could not serialize lambda", e);
		}
	}

	public Object read (Kryo kryo, Input input, Class type) {
		try {
			Object object = kryo.readObject(input, serializedLambda);
			return readResolve.invoke(object);
		} catch (Exception e) {
			throw new RuntimeException("Could not serialize lambda", e);
		}
	}

	public Object copy (Kryo kryo, Object original) {
		try {
			Class type = original.getClass();
			Method writeReplace = type.getDeclaredMethod("writeReplace");
			writeReplace.setAccessible(true);
			Object replacement = writeReplace.invoke(original);
			if (serializedLambda.isInstance(replacement)) {
				return readResolve.invoke(replacement);
			} else
				throw new RuntimeException("Could not serialize lambda");
		} catch (Exception e) {
			throw new RuntimeException("Could not serialize lambda", e);
		}
	}
}
