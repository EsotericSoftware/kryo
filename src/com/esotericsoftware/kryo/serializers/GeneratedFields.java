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
import static com.esotericsoftware.minlog.Log.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/** The super class of the hidden classes defined by {@link CodeGeneration}, which write and read the fields of one class with
 * straight line code: the VarHandles of the fields are constants and the object fields are delegated to their
 * {@link ReflectField}. */
abstract class GeneratedFields {
	/** True if the class of each value is written before the value, see {@link CodeGeneration#generate}. */
	boolean writesClasses;

	/** Writes all fields of the object. */
	abstract public void write (Output output, Object object);

	/** Writes all fields of the object, each in a chunk. */
	abstract public void write (Output output, Object object, ChunkedEncoding chunks);

	/** Reads all fields into the object. */
	abstract public void read (Input input, Object object);

	/** Reads all fields into the object, each from a chunk. */
	abstract public void read (Input input, Object object, ChunkedEncoding chunks);

	/** Returns the generated code for the fields, or null if code can't be generated for them, see
	 * {@link CodeGeneration#generate(FieldSerializer, CachedField[], boolean, int[])}. CodeGeneration uses the Java 24+ Class-File
	 * API, so it is compiled separately and referenced by name, which keeps the other classes compiled against the Java 17 API. */
	static GeneratedFields generate (FieldSerializer serializer, CachedField[] fields, boolean writeClasses, int[] tags) {
		try {
			return (GeneratedFields)Generator.generate.invokeExact(serializer, fields, writeClasses, tags);
		} catch (KryoException ex) {
			throw ex;
		} catch (Throwable t) {
			throw new KryoException("Unable to generate code for the fields of: " + className(serializer.type), t);
		}
	}

	/** Loaded on first use, which only happens on Java 24+. */
	static private final class Generator {
		static final MethodHandle generate;
		static {
			try {
				Class codeGeneration = Class.forName(GeneratedFields.class.getPackageName() + ".CodeGeneration");
				generate = MethodHandles.lookup().findStatic(codeGeneration, "generate",
					MethodType.methodType(GeneratedFields.class, FieldSerializer.class, CachedField[].class, boolean.class,
						int[].class));
			} catch (ReflectiveOperationException ex) {
				throw new ExceptionInInitializerError(ex);
			}
		}
	}

	/** Returned by {@link #readClass(FieldSerializer, Input, CachedField, boolean)} when the value is skipped. */
	static final Registration skip = new Registration(Void.class, new DefaultSerializers.VoidSerializer(), -1);

	// Called by the generated code when the classes are written. Also the helpers of CodeGeneration that don't need the Class-File
	// API, so the serializers don't reference that class.

	static void writeStringWithClass (Kryo kryo, Output output, String value) {
		if (value == null) {
			kryo.writeClass(output, null);
			return;
		}
		kryo.writeClass(output, String.class);
		output.writeString(value);
	}

	/** @param chunked If true, the value is skipped if its class can't be read, then the current value of the field is returned.
	 * @return null if the class was null. */
	static String readStringWithClass (FieldSerializer serializer, Input input, CachedField field, Object object,
		boolean chunked) {
		Registration registration = readClass(serializer, input, field, chunked);
		if (registration == null) return null;
		if (registration == skip) return (String)currentValue(field, object);
		return input.readString();
	}

	/** Returns the type a value in the data must be compatible with: the field type, or String for a String field that is written
	 * directly, which may be a type variable resolved to String. */
	static Class readType (CachedField field) {
		return field.valueClass == String.class && !(field instanceof ReflectField) ? String.class : field.field.getType();
	}

	/** Called by the generated code when writing a field fails, to name the field like the loop over the cached fields. */
	static KryoException writeError (Throwable t, CachedField field, Output output) {
		if (t instanceof KryoException) return (KryoException)t;
		if (t instanceof Error) throw (Error)t;
		return new KryoException("Error writing " + field + " at position " + output.position(), t);
	}

	/** Called by the generated code when reading a field fails, to name the field like the loop over the cached fields. */
	static KryoException readError (Throwable t, CachedField field, Input input) {
		if (t instanceof KryoException) return (KryoException)t;
		if (t instanceof Error) throw (Error)t;
		return new KryoException("Error reading " + field + " at position " + input.position(), t);
	}

	/** Returns the value of the field, to set it again when its value in the data is skipped. */
	static Object currentValue (CachedField field, Object object) {
		try {
			return field.get(object);
		} catch (IllegalAccessException ex) {
			throw ReflectField.accessError(field.field, ex);
		}
	}

	/** Reads the class of a primitive field value.
	 * @param chunked If true, the value is skipped if its class can't be read.
	 * @return false if the class was null or the value is skipped, then the field keeps its value. */
	static boolean readPrimitiveClass (FieldSerializer serializer, Input input, CachedField field, boolean chunked) {
		Registration registration = readClass(serializer, input, field, chunked);
		return registration != null && registration != skip;
	}

	/** Reads the class of a field value and ensures it is compatible with the field type, like CompatibleFieldSerializer with
	 * unknown field data.
	 * @param chunked If true, {@link #skip} is returned instead of throwing an exception, the caller's endField skips the data.
	 * @return null if the class was null. */
	static Registration readClass (FieldSerializer serializer, Input input, CachedField field, boolean chunked) {
		Registration registration;
		try {
			registration = serializer.kryo.readClass(input);
		} catch (KryoException ex) {
			String message = "Unable to read unknown data (unknown type). (" + serializer.type.getName() + "#" + field + ")";
			if (!chunked) throw new KryoException(message, ex);
			if (DEBUG) debug("kryo", message, ex);
			return skip;
		}
		if (registration == null) return null;
		Class valueClass = registration.getType(), fieldType = readType(field);
		if (!isAssignableTo(valueClass, fieldType)) {
			String message = "Read type is incompatible with the field type: " + className(valueClass) + " -> "
				+ className(fieldType)
				+ " (" + serializer.type.getName() + "#" + field + ")";
			if (!chunked) throw new KryoException(message);
			if (DEBUG) debug("kryo", message);
			return skip;
		}
		return registration;
	}

}
