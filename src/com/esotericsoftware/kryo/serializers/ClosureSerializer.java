package com.esotericsoftware.kryo.serializers;

import java.io.IOException;
import java.lang.reflect.Method;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Serializer for Java8 closures.
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
