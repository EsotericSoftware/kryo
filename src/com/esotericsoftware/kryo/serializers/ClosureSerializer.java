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

package com.esotericsoftware.kryo.serializers;

import static com.esotericsoftware.kryo.util.Util.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.minlog.Log;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Serializer for Java8 closures which implement Serializable. To serialize closures, use:
 * <p>
 * <code>kryo.register(Object[].class);
 * kryo.register(Class.class);
 * kryo.register(ClosureSerializer.Closure.class, new ClosureSerializer());</code>
 * <p>
 * Also, the closure's capturing class must be registered.
 * @author Roman Levenstein <romixlev@gmail.com>
 * @author Nathan Sweet */
public class ClosureSerializer extends Serializer {
	/** Marker class used to find the class {@link Registration} for closure instances.
	 * @see Kryo#isClosure(Class) */
	public static class Closure {
	}

	private static Method readResolve;
	private static Field capturingClass;

	public ClosureSerializer () {
		if (readResolve == null) {
			try {
				readResolve = SerializedLambda.class.getDeclaredMethod("readResolve");
				readResolve.setAccessible(true);
			} catch (Exception ex) {
				readResolve = null;
				Log.warn("Unable to obtain SerializedLambda#readResolve via reflection. " +
					"Falling back on resolving lambdas via capturing class.", ex);
			}
		}
		if (capturingClass == null) {
			try {
				capturingClass = SerializedLambda.class.getDeclaredField("capturingClass");
				capturingClass.setAccessible(true);
			} catch (Exception ex) {
				capturingClass = null;
				Log.warn("Unable to obtain SerializedLambda#capturingClass via reflection. " +
					"Falling back to resolving capturing class via Class.forName.", ex);
			}
		}
	}

	public void write (Kryo kryo, Output output, Object object) {
		SerializedLambda serializedLambda = toSerializedLambda(object);
		int count = serializedLambda.getCapturedArgCount();
		output.writeVarInt(count, true);
		for (int i = 0; i < count; i++)
			kryo.writeClassAndObject(output, serializedLambda.getCapturedArg(i));
		try {
			kryo.writeClass(output, getCapturingClass(serializedLambda));
		} catch (ClassNotFoundException ex) {
			throw new KryoException("Error writing closure.", ex);
		}
		output.writeString(serializedLambda.getFunctionalInterfaceClass());
		output.writeString(serializedLambda.getFunctionalInterfaceMethodName());
		output.writeString(serializedLambda.getFunctionalInterfaceMethodSignature());
		output.writeVarInt(serializedLambda.getImplMethodKind(), true);
		output.writeString(serializedLambda.getImplClass());
		output.writeString(serializedLambda.getImplMethodName());
		output.writeString(serializedLambda.getImplMethodSignature());
		output.writeString(serializedLambda.getInstantiatedMethodType());
	}

	public Object read (Kryo kryo, Input input, Class type) {
		int count = input.readVarInt(true);
		Object[] capturedArgs = new Object[count];
		for (int i = 0; i < count; i++)
			capturedArgs[i] = kryo.readClassAndObject(input);
		Class<?> capturingClass = kryo.readClass(input).getType();
		SerializedLambda serializedLambda = new SerializedLambda(capturingClass, input.readString(),
			input.readString(), input.readString(), input.readVarInt(true), input.readString(), input.readString(),
			input.readString(), input.readString(), capturedArgs);
		try {
			return readResolve(capturingClass, serializedLambda);
		} catch (Exception ex) {
			throw new KryoException("Error reading closure.", ex);
		}
	}

	public Object copy (Kryo kryo, Object original) {
		try {
			SerializedLambda lambda = toSerializedLambda(original);
			Class<?> capturingClass = getCapturingClass(lambda);
			return readResolve(capturingClass, lambda);
		} catch (Exception ex) {
			throw new KryoException("Error copying closure.", ex);
		}
	}

	private Object readResolve (Class<?> capturingClass, SerializedLambda lambda) throws Exception {
		if (readResolve != null) {
			return readResolve.invoke(lambda);
		}

		// See SerializedLambda#readResolve
		Method m = capturingClass.getDeclaredMethod("$deserializeLambda$", SerializedLambda.class);
		m.setAccessible(true);
		return m.invoke(null, lambda);
	}

	private SerializedLambda toSerializedLambda (Object object) {
		Object replacement;
		try {
			Method writeReplace = object.getClass().getDeclaredMethod("writeReplace");
			writeReplace.setAccessible(true);
			replacement = writeReplace.invoke(object);
		} catch (Exception ex) {
			if (object instanceof Serializable) throw new KryoException("Error serializing closure.", ex);
			throw new KryoException("Closure must implement java.io.Serializable.", ex);
		}
		try {
			return (SerializedLambda)replacement;
		} catch (Exception ex) {
			throw new KryoException("writeReplace must return a SerializedLambda: " + className(replacement.getClass()), ex);
		}
	}

	private static Class<?> getCapturingClass (SerializedLambda serializedLambda) throws ClassNotFoundException {
		if (capturingClass != null) {
			try {
				return (Class<?>)capturingClass.get(serializedLambda);
			} catch (IllegalAccessException ignored) {
				// ignore
			}
		}
		return Class.forName(serializedLambda.getCapturingClass().replace('/', '.'));
	}
}
