/* Copyright (c) 2008-2018, Nathan Sweet
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

import static com.esotericsoftware.kryo.unsafe.UnsafeUtil.*;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.kryo.util.Generics.GenericType;

import java.lang.reflect.Field;

/** Read and write a non-primitive field using Unsafe.
 * @author Nathan Sweet */
@SuppressWarnings("restriction")
class UnsafeField extends ReflectField {
	public UnsafeField (Field field, FieldSerializer serializer, GenericType genericType) {
		super(field, serializer, genericType);
		offset = unsafe.objectFieldOffset(field);
	}

	public Object get (Object object) throws IllegalAccessException {
		return unsafe.getObject(object, offset);
	}

	public void set (Object object, Object value) throws IllegalAccessException {
		unsafe.putObject(object, offset, value);
	}

	public void copy (Object original, Object copy) {
		try {
			unsafe.putObject(copy, offset, fieldSerializer.kryo.copy(unsafe.getObject(original, offset)));
		} catch (KryoException ex) {
			ex.addTrace(this + " (" + fieldSerializer.type.getName() + ")");
			throw ex;
		} catch (Throwable t) {
			KryoException ex = new KryoException(t);
			ex.addTrace(this + " (" + fieldSerializer.type.getName() + ")");
			throw ex;
		}
	}

	final static class IntUnsafeField extends CachedField {
		public IntUnsafeField (Field field) {
			super(field);
			offset = unsafe.objectFieldOffset(field);
		}

		public void write (Output output, Object object) {
			if (varEncoding)
				output.writeVarInt(unsafe.getInt(object, offset), false);
			else
				output.writeInt(unsafe.getInt(object, offset));
		}

		public void read (Input input, Object object) {
			if (varEncoding)
				unsafe.putInt(object, offset, input.readVarInt(false));
			else
				unsafe.putInt(object, offset, input.readInt());
		}

		public void copy (Object original, Object copy) {
			unsafe.putInt(copy, offset, unsafe.getInt(original, offset));
		}
	}

	final static class FloatUnsafeField extends CachedField {
		public FloatUnsafeField (Field field) {
			super(field);
			offset = unsafe.objectFieldOffset(field);
		}

		public void write (Output output, Object object) {
			output.writeFloat(unsafe.getFloat(object, offset));
		}

		public void read (Input input, Object object) {
			unsafe.putFloat(object, offset, input.readFloat());
		}

		public void copy (Object original, Object copy) {
			unsafe.putFloat(copy, offset, unsafe.getFloat(original, offset));
		}
	}

	final static class ShortUnsafeField extends CachedField {
		public ShortUnsafeField (Field field) {
			super(field);
			offset = unsafe.objectFieldOffset(field);
		}

		public void write (Output output, Object object) {
			output.writeShort(unsafe.getShort(object, offset));
		}

		public void read (Input input, Object object) {
			unsafe.putShort(object, offset, input.readShort());
		}

		public void copy (Object original, Object copy) {
			unsafe.putShort(copy, offset, unsafe.getShort(original, offset));
		}
	}

	final static class ByteUnsafeField extends CachedField {
		public ByteUnsafeField (Field field) {
			super(field);
			offset = unsafe.objectFieldOffset(field);
		}

		public void write (Output output, Object object) {
			output.writeByte(unsafe.getByte(object, offset));
		}

		public void read (Input input, Object object) {
			unsafe.putByte(object, offset, input.readByte());
		}

		public void copy (Object original, Object copy) {
			unsafe.putByte(copy, offset, unsafe.getByte(original, offset));
		}
	}

	final static class BooleanUnsafeField extends CachedField {
		public BooleanUnsafeField (Field field) {
			super(field);
			offset = unsafe.objectFieldOffset(field);
		}

		public void write (Output output, Object object) {
			output.writeBoolean(unsafe.getBoolean(object, offset));
		}

		public void read (Input input, Object object) {
			unsafe.putBoolean(object, offset, input.readBoolean());
		}

		public void copy (Object original, Object copy) {
			unsafe.putBoolean(copy, offset, unsafe.getBoolean(original, offset));
		}
	}

	final static class CharUnsafeField extends CachedField {
		public CharUnsafeField (Field field) {
			super(field);
			offset = unsafe.objectFieldOffset(field);
		}

		public void write (Output output, Object object) {
			output.writeChar(unsafe.getChar(object, offset));
		}

		public void read (Input input, Object object) {
			unsafe.putChar(object, offset, input.readChar());
		}

		public void copy (Object original, Object copy) {
			unsafe.putChar(copy, offset, unsafe.getChar(original, offset));
		}
	}

	final static class LongUnsafeField extends CachedField {
		public LongUnsafeField (Field field) {
			super(field);
			offset = unsafe.objectFieldOffset(field);
		}

		public void write (Output output, Object object) {
			if (varEncoding)
				output.writeVarLong(unsafe.getLong(object, offset), false);
			else
				output.writeLong(unsafe.getLong(object, offset));
		}

		public void read (Input input, Object object) {
			if (varEncoding)
				unsafe.putLong(object, offset, input.readVarLong(false));
			else
				unsafe.putLong(object, offset, input.readLong());
		}

		public void copy (Object original, Object copy) {
			unsafe.putLong(copy, offset, unsafe.getLong(original, offset));
		}
	}

	final static class DoubleUnsafeField extends CachedField {
		public DoubleUnsafeField (Field field) {
			super(field);
			offset = unsafe.objectFieldOffset(field);
		}

		public void write (Output output, Object object) {
			output.writeDouble(unsafe.getDouble(object, offset));
		}

		public void read (Input input, Object object) {
			unsafe.putDouble(object, offset, input.readDouble());
		}

		public void copy (Object original, Object copy) {
			unsafe.putDouble(copy, offset, unsafe.getDouble(original, offset));
		}
	}

	final static class StringUnsafeField extends CachedField {
		public StringUnsafeField (Field field) {
			super(field);
			offset = unsafe.objectFieldOffset(field);
		}

		public void write (Output output, Object object) {
			output.writeString((String)unsafe.getObject(object, offset));
		}

		public void read (Input input, Object object) {
			unsafe.putObject(object, offset, input.readString());
		}

		public void copy (Object original, Object copy) {
			unsafe.putObject(copy, offset, unsafe.getObject(original, offset));
		}
	}
}
