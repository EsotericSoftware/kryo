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
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.kryo.util.Generics.GenericType;
import com.esotericsoftware.kryo.util.Util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Read and write a non-primitive field using reflection.
 * @author Nathan Sweet
 * @author Roman Levenstein <romixlev@gmail.com> */
class ReflectField extends CachedField {
	final FieldSerializer fieldSerializer;
	final GenericType genericType;

	ReflectField (Field field, FieldSerializer fieldSerializer, GenericType genericType) {
		super(field);
		this.fieldSerializer = fieldSerializer;
		this.genericType = genericType;
	}

	public Object get (Object object) throws IllegalAccessException {
		return field.get(object);
	}

	public void set (Object object, Object value) throws IllegalAccessException {
		field.set(object, value);
	}

	public void write (Output output, Object object) {
		Object value;
		try {
			value = get(object);
		} catch (Throwable t) {
			KryoException ex = new KryoException("Error accessing field: " + name + " (" + object.getClass().getName() + ")", t);
			throw ex;
		}
		writeValue(output, object, value);
	}

	/** Writes the value of the field, which was already read from the object. */
	final void writeValue (Output output, Object object, Object value) {
		Kryo kryo = fieldSerializer.kryo;
		try {
			Serializer serializer = this.serializer;
			Class concreteType = resolveFieldClass();
			if (concreteType == null) {
				// The concrete type of the field is unknown, write the class first.
				if (value == null) {
					kryo.writeClass(output, null);
					return;
				}
				Registration registration = kryo.writeClass(output, value.getClass());
				if (serializer == null) serializer = registration.getSerializer();
				if (fieldSerializer.optimizeGenerics()) kryo.getGenerics().pushGenericType(genericType);
				kryo.writeObject(output, value, serializer);
			} else {
				if (serializer == null) {
					serializer = kryo.getSerializer(concreteType);
					// The concrete type of the field is known, always use the same serializer.
					if (valueClass != null && reuseSerializer) this.serializer = serializer;
				}
				if (fieldSerializer.optimizeGenerics()) kryo.getGenerics().pushGenericType(genericType);
				if (canBeNull) {
					kryo.writeObjectOrNull(output, value, serializer);
				} else {
					if (value == null) {
						throw new KryoException(
							"Field value cannot be null when canBeNull is false: " + name + " (" + object.getClass().getName() + ")");
					}
					kryo.writeObject(output, value, serializer);
				}
			}
		} catch (KryoException ex) {
			ex.addTrace(name + " (" + object.getClass().getName() + ")");
			throw ex;
		} catch (Throwable t) {
			if (isStackOverflow(t)) throw stackOverflow(kryo, value, t);
			KryoException ex = new KryoException(t);
			ex.addTrace(name + " (" + object.getClass().getName() + ")");
			throw ex;
		} finally {
			// Pop in a finally so an exception thrown by the nested write does not leave the generics stack unbalanced.
			kryo.getGenerics().popGenericType();
		}
	}

	/** Writes the class of the value, then the value without null marker, like CompatibleFieldSerializer with unknown field data.
	 * For generated code. */
	final void writeValueWithClass (Output output, Object object, Object value) {
		Kryo kryo = fieldSerializer.kryo;
		if (value == null) {
			kryo.writeClass(output, null);
			return;
		}
		boolean pushed = false;
		try {
			Class valueClass = value.getClass();
			kryo.writeClass(output, valueClass);
			Serializer serializer = this.serializer;
			if (serializer == null) serializer = kryo.getSerializer(valueClass);
			if (fieldSerializer.optimizeGenerics()) {
				kryo.getGenerics().pushGenericType(genericType);
				pushed = true;
			}
			kryo.writeObject(output, value, serializer);
		} catch (KryoException ex) {
			ex.addTrace(name + " (" + object.getClass().getName() + ")");
			throw ex;
		} catch (Throwable t) {
			if (isStackOverflow(t)) throw stackOverflow(kryo, value, t);
			KryoException ex = new KryoException(t);
			ex.addTrace(name + " (" + object.getClass().getName() + ")");
			throw ex;
		} finally {
			if (pushed) kryo.getGenerics().popGenericType();
		}
	}

	/** Reads the class, then the value without null marker, like CompatibleFieldSerializer with unknown field data. For generated
	 * code.
	 * @param chunked If true, the value is skipped if its class can't be read, then the current value of the field is returned.
	 * @return null if the class was null. */
	final Object readValueWithClass (Input input, Object object, boolean chunked) {
		Kryo kryo = fieldSerializer.kryo;
		Registration registration = GeneratedFields.readClass(fieldSerializer, input, this, chunked);
		if (registration == null) return null;
		if (registration == GeneratedFields.skip) return GeneratedFields.currentValue(this, object);
		Class valueClass = registration.getType();
		boolean pushed = false;
		try {
			Serializer serializer = this.serializer;
			if (serializer == null) serializer = kryo.getSerializer(valueClass);
			if (fieldSerializer.optimizeGenerics()) {
				kryo.getGenerics().pushGenericType(genericType);
				pushed = true;
			}
			return kryo.readObject(input, valueClass, serializer);
		} catch (KryoException ex) {
			ex.addTrace(name + " (" + fieldSerializer.type.getName() + ")");
			throw ex;
		} catch (Throwable t) {
			KryoException ex = new KryoException(t);
			ex.addTrace(name + " (" + fieldSerializer.type.getName() + ")");
			throw ex;
		} finally {
			if (pushed) kryo.getGenerics().popGenericType();
		}
	}

	/** Returns true for a stack overflow, also if it happened while a call site was linked, eg for a string concatenation in a
	 * catch block deeper in the stack, which throws a BootstrapMethodError instead. */
	static private boolean isStackOverflow (Throwable t) {
		return t instanceof StackOverflowError || (t instanceof BootstrapMethodError && t.getCause() instanceof StackOverflowError);
	}

	/** Returns the exception for a stack overflow, which is most likely a cycle in the data. */
	private KryoException stackOverflow (Kryo kryo, Object value, Throwable cause) {
		StringBuilder message = new StringBuilder(512);
		message.append("A StackOverflow occurred. The most likely cause is that your data has a circular reference resulting in ")
			.append("infinite recursion. Try enabling references with Kryo.setReferences(true). If your data structure ")
			.append("is really more than ").append(kryo.getDepth()).append(" levels deep then try increasing your Java stack size.");
		// The overflow can happen at any field of the cycle, so the declaring class and the value are checked.
		Class inner = isInnerClass(field.getDeclaringClass()) ? field.getDeclaringClass()
			: value != null && isInnerClass(value.getClass()) ? value.getClass() : null;
		if (inner != null) {
			message.append(" The inner class ").append(className(inner))
				.append(" is serialized with its outer instance and captured variables, which usually refer back to it. To omit ")
				.append("them, set FieldSerializerConfig#setIgnoreSyntheticFields(true), or make the class static.");
		}
		return new KryoException(message.toString(), cause);
	}

	public void read (Input input, Object object) {
		Object value = readValue(input);
		try {
			set(object, value);
		} catch (IllegalAccessException ex) {
			throw accessError(field, ex);
		} catch (Throwable t) {
			KryoException ex = new KryoException(t);
			ex.addTrace(name + " (" + fieldSerializer.type.getName() + ")");
			throw ex;
		}
	}

	/** Reads the value of the field, which the caller then sets on the object. */
	final Object readValue (Input input) {
		Kryo kryo = fieldSerializer.kryo;
		try {
			Object value;

			Serializer serializer = this.serializer;
			Class concreteType = resolveFieldClass();
			if (concreteType == null) {
				// The concrete type of the field is unknown, read the class first.
				Registration registration = kryo.readClass(input);
				if (registration == null) return null;
				if (serializer == null) serializer = registration.getSerializer();
				if (fieldSerializer.optimizeGenerics()) kryo.getGenerics().pushGenericType(genericType);
				value = kryo.readObject(input, registration.getType(), serializer);
			} else {
				if (serializer == null) {
					serializer = kryo.getSerializer(concreteType);
					// The concrete type of the field is known, always use the same serializer.
					if (valueClass != null && reuseSerializer) this.serializer = serializer;
				}
				if (fieldSerializer.optimizeGenerics()) kryo.getGenerics().pushGenericType(genericType);
				if (canBeNull)
					value = kryo.readObjectOrNull(input, concreteType, serializer);
				else
					value = kryo.readObject(input, concreteType, serializer);
			}
			return value;
		} catch (KryoException ex) {
			ex.addTrace(name + " (" + fieldSerializer.type.getName() + ")");
			throw ex;
		} catch (Throwable t) {
			KryoException ex = new KryoException(t);
			ex.addTrace(name + " (" + fieldSerializer.type.getName() + ")");
			throw ex;
		} finally {
			// Pop in a finally so an exception thrown by the nested read does not leave the generics stack unbalanced.
			kryo.getGenerics().popGenericType();
		}
	}

	public Object read (Input input) {
		return readValue(input);
	}

	Class resolveFieldClass () {
		if (valueClass == null) {
			Class fieldClass = genericType.resolve(fieldSerializer.kryo.getGenerics());
			if (fieldClass != null && fieldSerializer.kryo.isFinal(fieldClass)) {
				return field.getType().isArray() ? Util.getArrayType(fieldClass) : fieldClass;
			}
		}
		return valueClass;
	}

	public void copy (Object original, Object copy) {
		try {
			set(copy, fieldSerializer.kryo.copy(get(original)));
		} catch (IllegalAccessException ex) {
			throw accessError(field, ex);
		} catch (KryoException ex) {
			ex.addTrace(name + " (" + fieldSerializer.type.getName() + ")");
			throw ex;
		} catch (Throwable t) {
			KryoException ex = new KryoException(t);
			ex.addTrace(name + " (" + fieldSerializer.type.getName() + ")");
			throw ex;
		}
	}

	/** Returns an exception for a field that could not be accessed. Explains how to allow setting final fields, which is denied by
	 * default in future Java versions (JEP 500). */
	static KryoException accessError (Field field, Throwable cause) {
		if (!(cause instanceof IllegalAccessException)) return new KryoException(cause);
		if (Modifier.isFinal(field.getModifiers())) {
			return new KryoException("Unable to set final field: " + field.getDeclaringClass().getName() + "." + field.getName()
				+ ". Allow it with --enable-final-field-mutation=" + Util.moduleName()
				+ ", make the field non-final, use a record, or register a serializer for the class.", cause);
		}
		return new KryoException("Error accessing field: " + field.getName() + " (" + field.getDeclaringClass().getName() + ")",
			cause);
	}

	static final class IntReflectField extends CachedField {
		public IntReflectField (Field field) {
			super(field);
		}

		public void write (Output output, Object object) {
			try {
				if (varEncoding)
					output.writeVarInt(field.getInt(object), false);
				else
					output.writeInt(field.getInt(object));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (int)");
				throw ex;
			}
		}

		public void read (Input input, Object object) {
			try {
				if (varEncoding)
					field.setInt(object, input.readVarInt(false));
				else
					field.setInt(object, input.readInt());
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (int)");
				throw ex;
			}
		}

		public Object read (Input input) {
			if (varEncoding)
				return input.readVarInt(false);
			else
				return input.readInt();
		}

		public void copy (Object original, Object copy) {
			try {
				field.setInt(copy, field.getInt(original));
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (int)");
				throw ex;
			}
		}
	}

	static final class FloatReflectField extends CachedField {
		public FloatReflectField (Field field) {
			super(field);
		}

		public void write (Output output, Object object) {
			try {
				output.writeFloat(field.getFloat(object));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (float)");
				throw ex;
			}
		}

		public void read (Input input, Object object) {
			try {
				field.setFloat(object, input.readFloat());
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (float)");
				throw ex;
			}
		}

		public Object read (Input input) {
			return input.readFloat();
		}

		public void copy (Object original, Object copy) {
			try {
				field.setFloat(copy, field.getFloat(original));
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (float)");
				throw ex;
			}
		}
	}

	static final class ShortReflectField extends CachedField {
		public ShortReflectField (Field field) {
			super(field);
		}

		public void write (Output output, Object object) {
			try {
				output.writeShort(field.getShort(object));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (short)");
				throw ex;
			}
		}

		public void read (Input input, Object object) {
			try {
				field.setShort(object, input.readShort());
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (short)");
				throw ex;
			}
		}

		public Object read (Input input) {
			return input.readShort();
		}

		public void copy (Object original, Object copy) {
			try {
				field.setShort(copy, field.getShort(original));
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (short)");
				throw ex;
			}
		}
	}

	static final class ByteReflectField extends CachedField {
		public ByteReflectField (Field field) {
			super(field);
		}

		public void write (Output output, Object object) {
			try {
				output.writeByte(field.getByte(object));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (byte)");
				throw ex;
			}
		}

		public void read (Input input, Object object) {
			try {
				field.setByte(object, input.readByte());
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (byte)");
				throw ex;
			}
		}

		public Object read (Input input) {
			return input.readByte();
		}

		public void copy (Object original, Object copy) {
			try {
				field.setByte(copy, field.getByte(original));
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (byte)");
				throw ex;
			}
		}
	}

	static final class StringReflectField extends CachedField {
		public StringReflectField (Field field) {
			super(field);
		}

		public void write (Output output, Object object) {
			try {
				output.writeString((String)field.get(object));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (String)");
				throw ex;
			}
		}

		public void read (Input input, Object object) {
			try {
				field.set(object, input.readString());
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (String)");
				throw ex;
			}
		}

		public Object read (Input input) {
			return input.readString();
		}

		public void copy (Object original, Object copy) {
			try {
				field.set(copy, field.get(original));
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (String)");
				throw ex;
			}
		}
	}

	static final class BooleanReflectField extends CachedField {
		public BooleanReflectField (Field field) {
			super(field);
		}

		public void write (Output output, Object object) {
			try {
				output.writeBoolean(field.getBoolean(object));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (boolean)");
				throw ex;
			}
		}

		public void read (Input input, Object object) {
			try {
				field.setBoolean(object, input.readBoolean());
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (boolean)");
				throw ex;
			}
		}

		public Object read (Input input) {
			return input.readBoolean();
		}

		public void copy (Object original, Object copy) {
			try {
				field.setBoolean(copy, field.getBoolean(original));
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (boolean)");
				throw ex;
			}
		}
	}

	static final class CharReflectField extends CachedField {
		public CharReflectField (Field field) {
			super(field);
		}

		public void write (Output output, Object object) {
			try {
				output.writeChar(field.getChar(object));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (char)");
				throw ex;
			}
		}

		public void read (Input input, Object object) {
			try {
				field.setChar(object, input.readChar());
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (char)");
				throw ex;
			}
		}

		public Object read (Input input) {
			return input.readChar();
		}

		public void copy (Object original, Object copy) {
			try {
				field.setChar(copy, field.getChar(original));
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (char)");
				throw ex;
			}
		}
	}

	static final class LongReflectField extends CachedField {
		public LongReflectField (Field field) {
			super(field);
		}

		public void write (Output output, Object object) {
			try {
				if (varEncoding)
					output.writeVarLong(field.getLong(object), false);
				else
					output.writeLong(field.getLong(object));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (long)");
				throw ex;
			}
		}

		public void read (Input input, Object object) {
			try {
				if (varEncoding)
					field.setLong(object, input.readVarLong(false));
				else
					field.setLong(object, input.readLong());
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (long)");
				throw ex;
			}
		}

		public Object read (Input input) {
			if (varEncoding)
				return input.readVarLong(false);
			else
				return input.readLong();
		}

		public void copy (Object original, Object copy) {
			try {
				field.setLong(copy, field.getLong(original));
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (long)");
				throw ex;
			}
		}
	}

	static final class DoubleReflectField extends CachedField {
		public DoubleReflectField (Field field) {
			super(field);
		}

		public void write (Output output, Object object) {
			try {
				output.writeDouble(field.getDouble(object));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (double)");
				throw ex;
			}
		}

		public void read (Input input, Object object) {
			try {
				field.setDouble(object, input.readDouble());
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (double)");
				throw ex;
			}
		}

		public Object read (Input input) {
			return input.readDouble();
		}

		public void copy (Object original, Object copy) {
			try {
				field.setDouble(copy, field.getDouble(original));
			} catch (Throwable t) {
				KryoException ex = accessError(field, t);
				ex.addTrace(name + " (double)");
				throw ex;
			}
		}
	}
}
