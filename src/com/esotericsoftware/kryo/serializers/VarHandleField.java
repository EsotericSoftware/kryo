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

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.kryo.util.Generics.GenericType;
import com.esotericsoftware.kryo.util.IgnoreAndroid;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.invoke.VarHandle.AccessMode;
import java.lang.reflect.Field;

/** Read and write a non-primitive field using a {@link VarHandle}.
 * <p>
 * The VarHandle is converted to method handles with erased ({@link Object}) coordinates once per field, which all serializers and
 * Kryo instances share, see {@link CachedFields#varHandles(Field, Class)}, so that every access is an exact invocation without
 * type adaptation. */
@IgnoreAndroid
class VarHandleField extends ReflectField {
	final MethodHandle getter, setter;

	VarHandleField (Field field, FieldSerializer serializer, GenericType genericType) {
		super(field, serializer, genericType);
		MethodHandle[] handles = CachedFields.varHandles(field, Object.class);
		getter = handles[0];
		setter = handles[1];
	}

	public Object get (Object object) {
		try {
			return (Object)getter.invokeExact(object);
		} catch (Throwable t) {
			throw new KryoException(t);
		}
	}

	public void set (Object object, Object value) {
		try {
			if (isFinal)
				FieldSerializer.setFinal(this, object, value);
			else
				setter.invokeExact(object, value);
		} catch (Throwable t) {
			throw new KryoException(t);
		}
	}

	static VarHandle varHandle (Field field) {
		try {
			return MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup()).unreflectVarHandle(field);
		} catch (IllegalAccessException ex) {
			throw new KryoException("Unable to access field: " + field, ex);
		}
	}

	/** Returns the getter, setter and copier {@code (Object copy, Object original)} of a field, with erased ({@link Object})
	 * coordinates and the type as the type of the value. The setter and copier are null for a final field, which VarHandles can't
	 * set, it is set with {@link FieldSerializer#setFinal(CachedField, Object, Object)}.
	 * @throws KryoException if the field can't be accessed with a VarHandle. */
	static MethodHandle[] handles (Field field, Class type) {
		VarHandle handle = varHandle(field);
		MethodHandle getter = handle.toMethodHandle(AccessMode.GET).asType(MethodType.methodType(type, Object.class));
		if (!handle.isAccessModeSupported(AccessMode.SET)) return new MethodHandle[] {getter, null, null};
		MethodHandle setter = handle.toMethodHandle(AccessMode.SET)
			.asType(MethodType.methodType(void.class, Object.class, type));
		return new MethodHandle[] {getter, setter, MethodHandles.collectArguments(setter, 1, getter)};
	}

	@IgnoreAndroid
	static final class IntVarHandleField extends CachedField {
		final MethodHandle getter, setter, copier;

		IntVarHandleField (Field field) {
			super(field);
			MethodHandle[] handles = CachedFields.varHandles(field, int.class);
			getter = handles[0];
			setter = handles[1];
			copier = handles[2];
		}

		public void write (Output output, Object object) {
			try {
				if (varEncoding)
					output.writeVarInt((int)getter.invokeExact(object), false);
				else
					output.writeInt((int)getter.invokeExact(object));
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public void read (Input input, Object object) {
			try {
				if (isFinal)
					FieldSerializer.setFinal(this, object, read(input));
				else if (varEncoding)
					setter.invokeExact(object, input.readVarInt(false));
				else
					setter.invokeExact(object, input.readInt());
			} catch (Throwable t) {
				throw error(t);
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
				if (isFinal)
					FieldSerializer.setFinal(this, copy, (int)getter.invokeExact(original));
				else
					copier.invokeExact(copy, original);
			} catch (Throwable t) {
				throw error(t);
			}
		}
	}

	@IgnoreAndroid
	static final class LongVarHandleField extends CachedField {
		final MethodHandle getter, setter, copier;

		LongVarHandleField (Field field) {
			super(field);
			MethodHandle[] handles = CachedFields.varHandles(field, long.class);
			getter = handles[0];
			setter = handles[1];
			copier = handles[2];
		}

		public void write (Output output, Object object) {
			try {
				if (varEncoding)
					output.writeVarLong((long)getter.invokeExact(object), false);
				else
					output.writeLong((long)getter.invokeExact(object));
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public void read (Input input, Object object) {
			try {
				if (isFinal)
					FieldSerializer.setFinal(this, object, read(input));
				else if (varEncoding)
					setter.invokeExact(object, input.readVarLong(false));
				else
					setter.invokeExact(object, input.readLong());
			} catch (Throwable t) {
				throw error(t);
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
				if (isFinal)
					FieldSerializer.setFinal(this, copy, (long)getter.invokeExact(original));
				else
					copier.invokeExact(copy, original);
			} catch (Throwable t) {
				throw error(t);
			}
		}
	}

	@IgnoreAndroid
	static final class FloatVarHandleField extends CachedField {
		final MethodHandle getter, setter, copier;

		FloatVarHandleField (Field field) {
			super(field);
			MethodHandle[] handles = CachedFields.varHandles(field, float.class);
			getter = handles[0];
			setter = handles[1];
			copier = handles[2];
		}

		public void write (Output output, Object object) {
			try {
				output.writeFloat((float)getter.invokeExact(object));
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public void read (Input input, Object object) {
			try {
				if (isFinal)
					FieldSerializer.setFinal(this, object, read(input));
				else
					setter.invokeExact(object, input.readFloat());
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public Object read (Input input) {
			return input.readFloat();
		}

		public void copy (Object original, Object copy) {
			try {
				if (isFinal)
					FieldSerializer.setFinal(this, copy, (float)getter.invokeExact(original));
				else
					copier.invokeExact(copy, original);
			} catch (Throwable t) {
				throw error(t);
			}
		}
	}

	@IgnoreAndroid
	static final class DoubleVarHandleField extends CachedField {
		final MethodHandle getter, setter, copier;

		DoubleVarHandleField (Field field) {
			super(field);
			MethodHandle[] handles = CachedFields.varHandles(field, double.class);
			getter = handles[0];
			setter = handles[1];
			copier = handles[2];
		}

		public void write (Output output, Object object) {
			try {
				output.writeDouble((double)getter.invokeExact(object));
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public void read (Input input, Object object) {
			try {
				if (isFinal)
					FieldSerializer.setFinal(this, object, read(input));
				else
					setter.invokeExact(object, input.readDouble());
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public Object read (Input input) {
			return input.readDouble();
		}

		public void copy (Object original, Object copy) {
			try {
				if (isFinal)
					FieldSerializer.setFinal(this, copy, (double)getter.invokeExact(original));
				else
					copier.invokeExact(copy, original);
			} catch (Throwable t) {
				throw error(t);
			}
		}
	}

	@IgnoreAndroid
	static final class BooleanVarHandleField extends CachedField {
		final MethodHandle getter, setter, copier;

		BooleanVarHandleField (Field field) {
			super(field);
			MethodHandle[] handles = CachedFields.varHandles(field, boolean.class);
			getter = handles[0];
			setter = handles[1];
			copier = handles[2];
		}

		public void write (Output output, Object object) {
			try {
				output.writeBoolean((boolean)getter.invokeExact(object));
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public void read (Input input, Object object) {
			try {
				if (isFinal)
					FieldSerializer.setFinal(this, object, read(input));
				else
					setter.invokeExact(object, input.readBoolean());
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public Object read (Input input) {
			return input.readBoolean();
		}

		public void copy (Object original, Object copy) {
			try {
				if (isFinal)
					FieldSerializer.setFinal(this, copy, (boolean)getter.invokeExact(original));
				else
					copier.invokeExact(copy, original);
			} catch (Throwable t) {
				throw error(t);
			}
		}
	}

	@IgnoreAndroid
	static final class ByteVarHandleField extends CachedField {
		final MethodHandle getter, setter, copier;

		ByteVarHandleField (Field field) {
			super(field);
			MethodHandle[] handles = CachedFields.varHandles(field, byte.class);
			getter = handles[0];
			setter = handles[1];
			copier = handles[2];
		}

		public void write (Output output, Object object) {
			try {
				output.writeByte((byte)getter.invokeExact(object));
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public void read (Input input, Object object) {
			try {
				if (isFinal)
					FieldSerializer.setFinal(this, object, read(input));
				else
					setter.invokeExact(object, input.readByte());
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public Object read (Input input) {
			return input.readByte();
		}

		public void copy (Object original, Object copy) {
			try {
				if (isFinal)
					FieldSerializer.setFinal(this, copy, (byte)getter.invokeExact(original));
				else
					copier.invokeExact(copy, original);
			} catch (Throwable t) {
				throw error(t);
			}
		}
	}

	@IgnoreAndroid
	static final class ShortVarHandleField extends CachedField {
		final MethodHandle getter, setter, copier;

		ShortVarHandleField (Field field) {
			super(field);
			MethodHandle[] handles = CachedFields.varHandles(field, short.class);
			getter = handles[0];
			setter = handles[1];
			copier = handles[2];
		}

		public void write (Output output, Object object) {
			try {
				output.writeShort((short)getter.invokeExact(object));
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public void read (Input input, Object object) {
			try {
				if (isFinal)
					FieldSerializer.setFinal(this, object, read(input));
				else
					setter.invokeExact(object, input.readShort());
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public Object read (Input input) {
			return input.readShort();
		}

		public void copy (Object original, Object copy) {
			try {
				if (isFinal)
					FieldSerializer.setFinal(this, copy, (short)getter.invokeExact(original));
				else
					copier.invokeExact(copy, original);
			} catch (Throwable t) {
				throw error(t);
			}
		}
	}

	@IgnoreAndroid
	static final class CharVarHandleField extends CachedField {
		final MethodHandle getter, setter, copier;

		CharVarHandleField (Field field) {
			super(field);
			MethodHandle[] handles = CachedFields.varHandles(field, char.class);
			getter = handles[0];
			setter = handles[1];
			copier = handles[2];
		}

		public void write (Output output, Object object) {
			try {
				output.writeChar((char)getter.invokeExact(object));
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public void read (Input input, Object object) {
			try {
				if (isFinal)
					FieldSerializer.setFinal(this, object, read(input));
				else
					setter.invokeExact(object, input.readChar());
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public Object read (Input input) {
			return input.readChar();
		}

		public void copy (Object original, Object copy) {
			try {
				if (isFinal)
					FieldSerializer.setFinal(this, copy, (char)getter.invokeExact(original));
				else
					copier.invokeExact(copy, original);
			} catch (Throwable t) {
				throw error(t);
			}
		}
	}

	@IgnoreAndroid
	static final class StringVarHandleField extends CachedField {
		final MethodHandle getter, setter, copier;

		StringVarHandleField (Field field) {
			super(field);
			MethodHandle[] handles = CachedFields.varHandles(field, String.class);
			getter = handles[0];
			setter = handles[1];
			copier = handles[2];
		}

		public Object get (Object object) {
			try {
				return (String)getter.invokeExact(object);
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public void write (Output output, Object object) {
			try {
				output.writeString((String)getter.invokeExact(object));
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public void read (Input input, Object object) {
			try {
				if (isFinal)
					FieldSerializer.setFinal(this, object, read(input));
				else
					setter.invokeExact(object, input.readString());
			} catch (Throwable t) {
				throw error(t);
			}
		}

		public Object read (Input input) {
			return input.readString();
		}

		public void copy (Object original, Object copy) {
			try {
				if (isFinal)
					FieldSerializer.setFinal(this, copy, (String)getter.invokeExact(original));
				else
					copier.invokeExact(copy, original);
			} catch (Throwable t) {
				throw error(t);
			}
		}
	}
}
