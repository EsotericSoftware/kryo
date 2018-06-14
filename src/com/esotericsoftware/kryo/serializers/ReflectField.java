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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.kryo.util.Generics.GenericType;

import java.lang.reflect.Field;

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
		Kryo kryo = fieldSerializer.kryo;
		try {
			Object value = get(object);

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
				kryo.getGenerics().pushGenericType(genericType);
				kryo.writeObject(output, value, serializer);
			} else {
				// The concrete type of the field is known, always use the same serializer.
				if (serializer == null) serializer = kryo.getSerializer(concreteType);
				kryo.getGenerics().pushGenericType(genericType);
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
			kryo.getGenerics().popGenericType();
		} catch (IllegalAccessException ex) {
			throw new KryoException("Error accessing field: " + name + " (" + object.getClass().getName() + ")", ex);
		} catch (KryoException ex) {
			ex.addTrace(name + " (" + object.getClass().getName() + ")");
			throw ex;
		} catch (Throwable t) {
			KryoException ex = new KryoException(t);
			ex.addTrace(name + " (" + object.getClass().getName() + ")");
			throw ex;
		}
	}

	public void read (Input input, Object object) {
		Kryo kryo = fieldSerializer.kryo;
		try {
			Object value;

			Serializer serializer = this.serializer;
			Class concreteType = resolveFieldClass();
			if (concreteType == null) {
				// The concrete type of the field is unknown, read the class first.
				Registration registration = kryo.readClass(input);
				if (registration == null) {
					set(object, null);
					return;
				}
				if (serializer == null) serializer = registration.getSerializer();
				kryo.getGenerics().pushGenericType(genericType);
				value = kryo.readObject(input, registration.getType(), serializer);
			} else {
				// The concrete type of the field is known, always use the same serializer.
				if (serializer == null) serializer = kryo.getSerializer(concreteType);
				kryo.getGenerics().pushGenericType(genericType);
				if (canBeNull)
					value = kryo.readObjectOrNull(input, concreteType, serializer);
				else
					value = kryo.readObject(input, concreteType, serializer);
			}
			kryo.getGenerics().popGenericType();

			set(object, value);
		} catch (IllegalAccessException ex) {
			throw new KryoException("Error accessing field: " + name + " (" + fieldSerializer.type.getName() + ")", ex);
		} catch (KryoException ex) {
			ex.addTrace(name + " (" + fieldSerializer.type.getName() + ")");
			throw ex;
		} catch (Throwable t) {
			KryoException ex = new KryoException(t);
			ex.addTrace(name + " (" + fieldSerializer.type.getName() + ")");
			throw ex;
		}
	}

	Class resolveFieldClass () {
		if (valueClass == null) {
			Class fieldClass = genericType.resolve(fieldSerializer.kryo.getGenerics());
			if (fieldClass != null && fieldSerializer.kryo.isFinal(fieldClass)) return fieldClass;
		}
		return valueClass;
	}

	public void copy (Object original, Object copy) {
		try {
			set(copy, fieldSerializer.kryo.copy(get(original)));
		} catch (IllegalAccessException ex) {
			throw new KryoException("Error accessing field: " + name + " (" + fieldSerializer.type.getName() + ")", ex);
		} catch (KryoException ex) {
			ex.addTrace(name + " (" + fieldSerializer.type.getName() + ")");
			throw ex;
		} catch (Throwable t) {
			KryoException ex = new KryoException(t);
			ex.addTrace(name + " (" + fieldSerializer.type.getName() + ")");
			throw ex;
		}
	}

	final static class IntReflectField extends CachedField {
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
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (int)");
				throw ex;
			}
		}

		public void copy (Object original, Object copy) {
			try {
				field.setInt(copy, field.getInt(original));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (int)");
				throw ex;
			}
		}
	}

	final static class FloatReflectField extends CachedField {
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
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (float)");
				throw ex;
			}
		}

		public void copy (Object original, Object copy) {
			try {
				field.setFloat(copy, field.getFloat(original));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (float)");
				throw ex;
			}
		}
	}

	final static class ShortReflectField extends CachedField {
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
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (short)");
				throw ex;
			}
		}

		public void copy (Object original, Object copy) {
			try {
				field.setShort(copy, field.getShort(original));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (short)");
				throw ex;
			}
		}
	}

	final static class ByteReflectField extends CachedField {
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
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (byte)");
				throw ex;
			}
		}

		public void copy (Object original, Object copy) {
			try {
				field.setByte(copy, field.getByte(original));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (byte)");
				throw ex;
			}
		}
	}

	final static class BooleanReflectField extends CachedField {
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
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (boolean)");
				throw ex;
			}
		}

		public void copy (Object original, Object copy) {
			try {
				field.setBoolean(copy, field.getBoolean(original));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (boolean)");
				throw ex;
			}
		}
	}

	final static class CharReflectField extends CachedField {
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
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (char)");
				throw ex;
			}
		}

		public void copy (Object original, Object copy) {
			try {
				field.setChar(copy, field.getChar(original));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (char)");
				throw ex;
			}
		}
	}

	final static class LongReflectField extends CachedField {
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
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (long)");
				throw ex;
			}
		}

		public void copy (Object original, Object copy) {
			try {
				field.setLong(copy, field.getLong(original));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (long)");
				throw ex;
			}
		}
	}

	final static class DoubleReflectField extends CachedField {
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
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (double)");
				throw ex;
			}
		}

		public void copy (Object original, Object copy) {
			try {
				field.setDouble(copy, field.getDouble(original));
			} catch (Throwable t) {
				KryoException ex = new KryoException(t);
				ex.addTrace(name + " (double)");
				throw ex;
			}
		}
	}
}
