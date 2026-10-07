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
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

/** Serializer for Java8 closures which implement Serializable. To serialize closures, use:
 * <p>
 * <code>kryo.register(Object[].class);
 * kryo.register(Class.class);
 * kryo.register(ClosureSerializer.Closure.class, new ClosureSerializer());</code>
 * <p>
 * Also, the closure's capturing class must be registered.
 * <p>
 * The closure is written as its {@link SerializedLambda} and read by calling the {@code $deserializeLambda$} method of the
 * capturing class, like Java serialization does. Only the closure's class and the capturing class are accessed reflectively, no
 * JDK internals.
 * @author Roman Levenstein {@literal <romixlev@gmail.com>}
 * @author Nathan Sweet */
public class ClosureSerializer extends Serializer {
	/** Marker class used to find the class {@link Registration} for closure instances.
	 * @see Kryo#isClosure(Class) */
	public static class Closure {
	}

	/** What is needed to write a closure, cached by the closure's class. */
	static private final class ClosureClass {
		final MethodHandle writeReplace; // (Object)Object, null if the closure isn't serializable.
		Class capturingClass; // Resolved at the first write.

		ClosureClass (MethodHandle writeReplace) {
			this.writeReplace = writeReplace;
		}
	}

	/** The caches are in a holder class, so ClassValue is only loaded when a closure is serialized. Android has ClassValue only
	 * since API level 34, but no serializable lambdas at all, so the serializer can be registered there like before. */
	static private final class Caches {
		/** The method the compiler generates in each class that contains a serializable lambda, see
		 * SerializedLambda#readResolve. */
		static final ClassValue<Method> deserializeMethods = new ClassValue<>() {
			protected Method computeValue (Class<?> capturingClass) {
				try {
					Method method = capturingClass.getDeclaredMethod("$deserializeLambda$", SerializedLambda.class);
					method.setAccessible(true);
					return method;
				} catch (Exception ex) {
					throw new KryoException(
						"Unable to access $deserializeLambda$ of the capturing class: " + className(capturingClass), ex);
				}
			}
		};

		static final ClassValue<ClosureClass> closureClasses = new ClassValue<>() {
			protected ClosureClass computeValue (Class<?> type) {
				Method writeReplace;
				try {
					writeReplace = type.getDeclaredMethod("writeReplace");
				} catch (NoSuchMethodException ex) {
					return new ClosureClass(null);
				}
				try {
					writeReplace.setAccessible(true);
					// A method handle, because on Java 17 Method.invoke is slow for the methods of a hidden class like a closure's.
					return new ClosureClass(
						MethodHandles.lookup().unreflect(writeReplace).asType(MethodType.methodType(Object.class, Object.class)));
				} catch (Exception ex) {
					throw new KryoException("Unable to access the writeReplace method of the closure: " + className(type), ex);
				}
			}
		};
	}

	public void write (Kryo kryo, Output output, Object object) {
		ClosureClass closureClass = Caches.closureClasses.get(object.getClass());
		SerializedLambda serializedLambda = toSerializedLambda(closureClass, object);
		int count = serializedLambda.getCapturedArgCount();
		output.writeVarInt(count, true);
		for (int i = 0; i < count; i++)
			kryo.writeClassAndObject(output, serializedLambda.getCapturedArg(i));
		kryo.writeClass(output, getCapturingClass(closureClass, object, serializedLambda));
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
		Object[] capturedArgs = new Object[input.validateArrayLength(count)];
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
		ClosureClass closureClass = Caches.closureClasses.get(original.getClass());
		SerializedLambda lambda = toSerializedLambda(closureClass, original);
		try {
			return readResolve(getCapturingClass(closureClass, original, lambda), lambda);
		} catch (Exception ex) {
			throw new KryoException("Error copying closure.", ex);
		}
	}

	/** Creates the closure like Java serialization does. */
	private Object readResolve (Class<?> capturingClass, SerializedLambda lambda) throws Exception {
		return Caches.deserializeMethods.get(capturingClass).invoke(null, lambda);
	}

	private SerializedLambda toSerializedLambda (ClosureClass closureClass, Object object) {
		if (closureClass.writeReplace == null) {
			if (object instanceof Serializable) throw new KryoException("Error serializing closure, no writeReplace method.");
			throw new KryoException("Closure must implement java.io.Serializable.");
		}
		Object replacement;
		try {
			replacement = (Object)closureClass.writeReplace.invokeExact(object);
		} catch (Throwable ex) {
			throw new KryoException("Error serializing closure.", ex);
		}
		try {
			return (SerializedLambda)replacement;
		} catch (Exception ex) {
			throw new KryoException("writeReplace must return a SerializedLambda: " + className(replacement.getClass()), ex);
		}
	}

	/** The closure's class is defined by the class loader of the capturing class, so the name resolves there. */
	private static Class<?> getCapturingClass (ClosureClass closureClass, Object closure, SerializedLambda serializedLambda) {
		Class capturingClass = closureClass.capturingClass;
		if (capturingClass == null) {
			try {
				capturingClass = Class.forName(serializedLambda.getCapturingClass().replace('/', '.'), false,
					closure.getClass().getClassLoader());
			} catch (ClassNotFoundException ex) {
				throw new KryoException("Error writing closure.", ex);
			}
			closureClass.capturingClass = capturingClass;
		}
		return capturingClass;
	}
}
