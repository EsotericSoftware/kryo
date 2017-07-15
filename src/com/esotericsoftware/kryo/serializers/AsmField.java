/* Copyright (c) 2008-2017, Nathan Sweet
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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.reflectasm.FieldAccess;

/*** Implementations of ASM-based serializers for fields.
 * @author Nathan Sweet */
class AsmField extends ReflectField {
	public AsmField (Kryo kryo, Class type) {
		super(kryo, type);
	}

	public Object getField (Object object) throws IllegalArgumentException, IllegalAccessException {
		if (accessIndex == -1) throw new KryoException("Unknown access index");
		return access.get(object, accessIndex);
	}

	public void setField (Object object, Object value) throws IllegalArgumentException, IllegalAccessException {
		if (accessIndex == -1) throw new KryoException("Unknown access index");
		((FieldAccess)access).set(object, accessIndex, value);
	}

	public void copy (Object original, Object copy) {
		try {
			if (accessIndex == -1) throw new KryoException("Unknown access index");
			access.set(copy, accessIndex, kryo.copy(access.get(original, accessIndex)));
		} catch (KryoException ex) {
			ex.addTrace(this + " (" + type.getName() + ")");
			throw ex;
		} catch (RuntimeException runtimeEx) {
			KryoException ex = new KryoException(runtimeEx);
			ex.addTrace(this + " (" + type.getName() + ")");
			throw ex;
		}
	}

	final static class IntAsmField extends CachedField {
		public void write (Output output, Object object) {
			if (varInt)
				output.writeInt(access.getInt(object, accessIndex), false);
			else
				output.writeInt(access.getInt(object, accessIndex));
		}

		public void read (Input input, Object object) {
			if (varInt)
				access.setInt(object, accessIndex, input.readInt(false));
			else
				access.setInt(object, accessIndex, input.readInt());
		}

		public void copy (Object original, Object copy) {
			access.setInt(copy, accessIndex, access.getInt(original, accessIndex));
		}
	}

	final static class FloatAsmField extends CachedField {
		public void write (Output output, Object object) {
			output.writeFloat(access.getFloat(object, accessIndex));
		}

		public void read (Input input, Object object) {
			access.setFloat(object, accessIndex, input.readFloat());
		}

		public void copy (Object original, Object copy) {
			access.setFloat(copy, accessIndex, access.getFloat(original, accessIndex));
		}
	}

	final static class ShortAsmField extends CachedField {
		public void write (Output output, Object object) {
			output.writeShort(access.getShort(object, accessIndex));
		}

		public void read (Input input, Object object) {
			access.setShort(object, accessIndex, input.readShort());
		}

		public void copy (Object original, Object copy) {
			access.setShort(copy, accessIndex, access.getShort(original, accessIndex));
		}
	}

	final static class ByteAsmField extends CachedField {
		public void write (Output output, Object object) {
			output.writeByte(access.getByte(object, accessIndex));
		}

		public void read (Input input, Object object) {
			access.setByte(object, accessIndex, input.readByte());
		}

		public void copy (Object original, Object copy) {
			access.setByte(copy, accessIndex, access.getByte(original, accessIndex));
		}
	}

	final static class BooleanAsmField extends CachedField {
		public void write (Output output, Object object) {
			output.writeBoolean(access.getBoolean(object, accessIndex));
		}

		public void read (Input input, Object object) {
			access.setBoolean(object, accessIndex, input.readBoolean());
		}

		public void copy (Object original, Object copy) {
			access.setBoolean(copy, accessIndex, access.getBoolean(original, accessIndex));
		}
	}

	final static class CharAsmField extends CachedField {
		public void write (Output output, Object object) {
			output.writeChar(access.getChar(object, accessIndex));
		}

		public void read (Input input, Object object) {
			access.setChar(object, accessIndex, input.readChar());
		}

		public void copy (Object original, Object copy) {
			access.setChar(copy, accessIndex, access.getChar(original, accessIndex));
		}
	}

	final static class LongAsmField extends CachedField {
		public void write (Output output, Object object) {
			if (varInt)
				output.writeLong(access.getLong(object, accessIndex), false);
			else
				output.writeLong(access.getLong(object, accessIndex));
		}

		public void read (Input input, Object object) {
			if (varInt)
				access.setLong(object, accessIndex, input.readLong(false));
			else
				access.setLong(object, accessIndex, input.readLong());
		}

		public void copy (Object original, Object copy) {
			access.setLong(copy, accessIndex, access.getLong(original, accessIndex));
		}
	}

	final static class DoubleAsmField extends CachedField {
		public void write (Output output, Object object) {
			output.writeDouble(access.getDouble(object, accessIndex));
		}

		public void read (Input input, Object object) {
			access.setDouble(object, accessIndex, input.readDouble());
		}

		public void copy (Object original, Object copy) {
			access.setDouble(copy, accessIndex, access.getDouble(original, accessIndex));
		}
	}

	final static class StringAsmField extends CachedField {
		public void write (Output output, Object object) {
			output.writeString(access.getString(object, accessIndex));
		}

		public void read (Input input, Object object) {
			access.set(object, accessIndex, input.readString());
		}

		public void copy (Object original, Object copy) {
			access.set(copy, accessIndex, access.getString(original, accessIndex));
		}
	}
}
