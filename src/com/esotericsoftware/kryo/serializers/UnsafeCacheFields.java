
package com.esotericsoftware.kryo.serializers;

import static com.esotericsoftware.kryo.util.UnsafeUtil.unsafe;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.esotericsoftware.kryo.io.UnsafeMemoryInput;
import com.esotericsoftware.kryo.io.UnsafeMemoryOutput;
import com.esotericsoftware.kryo.io.UnsafeOutput;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.reflectasm.FieldAccess;

/*** Implementations of sun.misc.Unsafe-based serializers for fields.
 * 
 * @author Roman Levenstein <romixlev@gmail.com> */
class UnsafeCacheFields {

	abstract static class UnsafeCachedField extends CachedField {
		UnsafeCachedField (long offset) {
			this.offset = offset;
		}
	}

	final static class UnsafeIntField extends UnsafeCachedField {
		public UnsafeIntField (Field f) {
			super(unsafe().objectFieldOffset(f));
		}

		public void write (Output output, Object object) {
			if (varIntsEnabled)
				output.writeInt(unsafe().getInt(object, offset), false);
			else
				output.writeInt(unsafe().getInt(object, offset));
		}

		public void read (Input input, Object object) {
			if (varIntsEnabled)
				unsafe().putInt(object, offset, input.readInt(false));
			else
				unsafe().putInt(object, offset, input.readInt());
		}

		public void copy (Object original, Object copy) {
			unsafe().putInt(copy, offset, unsafe().getInt(original, offset));
		}
	}

	final static class UnsafeFloatField extends UnsafeCachedField {
		public UnsafeFloatField (Field f) {
			super(unsafe().objectFieldOffset(f));
		}

		public void write (Output output, Object object) {
			output.writeFloat(unsafe().getFloat(object, offset));
		}

		public void read (Input input, Object object) {
			unsafe().putFloat(object, offset, input.readFloat());
		}

		public void copy (Object original, Object copy) {
			unsafe().putFloat(copy, offset, unsafe().getFloat(original, offset));
		}
	}

	final static class UnsafeShortField extends UnsafeCachedField {
		public UnsafeShortField (Field f) {
			super(unsafe().objectFieldOffset(f));
		}

		public void write (Output output, Object object) {
			output.writeShort(unsafe().getShort(object, offset));
		}

		public void read (Input input, Object object) {
			unsafe().putShort(object, offset, input.readShort());
		}

		public void copy (Object original, Object copy) {
			unsafe().putShort(copy, offset, unsafe().getShort(original, offset));
		}
	}

	final static class UnsafeByteField extends UnsafeCachedField {
		public UnsafeByteField (Field f) {
			super(unsafe().objectFieldOffset(f));
		}

		public void write (Output output, Object object) {
			output.writeByte(unsafe().getByte(object, offset));
		}

		public void read (Input input, Object object) {
			unsafe().putByte(object, offset, input.readByte());
		}

		public void copy (Object original, Object copy) {
			unsafe().putByte(copy, offset, unsafe().getByte(original, offset));
		}
	}

	final static class UnsafeBooleanField extends UnsafeCachedField {
		public UnsafeBooleanField (Field f) {
			super(unsafe().objectFieldOffset(f));
		}

		public void write (Output output, Object object) {
			output.writeBoolean(unsafe().getBoolean(object, offset));
		}

		public void read (Input input, Object object) {
			unsafe().putBoolean(object, offset, input.readBoolean());
		}

		public void copy (Object original, Object copy) {
			unsafe().putBoolean(copy, offset, unsafe().getBoolean(original, offset));
		}
	}

	final static class UnsafeCharField extends UnsafeCachedField {
		public UnsafeCharField (Field f) {
			super(unsafe().objectFieldOffset(f));
		}

		public void write (Output output, Object object) {
			output.writeChar(unsafe().getChar(object, offset));
		}

		public void read (Input input, Object object) {
			unsafe().putChar(object, offset, input.readChar());
		}

		public void copy (Object original, Object copy) {
			unsafe().putChar(copy, offset, unsafe().getChar(original, offset));
		}
	}

	final static class UnsafeLongField extends UnsafeCachedField {
		public UnsafeLongField (Field f) {
			super(unsafe().objectFieldOffset(f));
		}

		public void write (Output output, Object object) {
			if (varIntsEnabled)
				output.writeLong(unsafe().getLong(object, offset), false);
			else
				output.writeLong(unsafe().getLong(object, offset));
		}

		public void read (Input input, Object object) {
			if (varIntsEnabled)
				unsafe().putLong(object, offset, input.readLong(false));
			else
				unsafe().putLong(object, offset, input.readLong());
		}

		public void copy (Object original, Object copy) {
			unsafe().putLong(copy, offset, unsafe().getLong(original, offset));
		}
	}

	final static class UnsafeDoubleField extends UnsafeCachedField {
		public UnsafeDoubleField (Field f) {
			super(unsafe().objectFieldOffset(f));
		}

		public void write (Output output, Object object) {
			output.writeDouble(unsafe().getDouble(object, offset));
		}

		public void read (Input input, Object object) {
			unsafe().putDouble(object, offset, input.readDouble());
		}

		public void copy (Object original, Object copy) {
			unsafe().putDouble(copy, offset, unsafe().getDouble(original, offset));
		}
	}

	final static class UnsafeStringField extends UnsafeCachedField {
		public UnsafeStringField (Field f) {
			super(unsafe().objectFieldOffset(f));
		}

		public void write (Output output, Object object) {
			output.writeString((String)unsafe().getObject(object, offset));
		}

		public void read (Input input, Object object) {
			unsafe().putObject(object, offset, input.readString());
		}

		public void copy (Object original, Object copy) {
			unsafe().putObject(copy, offset, unsafe().getObject(original, offset));
		}
	}

	/** Helper class for doing bulk copies of memory regions containing adjacent primitive fields. Should be normally used only with
	 * Unsafe streams to deliver best performance. */
	final static class UnsafeRegionField extends UnsafeCachedField {
		final long len;
		static final boolean bulkReadsSupported = false;

		public UnsafeRegionField (long offset, long len) {
			super(offset);
			this.len = len;
		}

		final public void write (Output output, Object object) {
			if (output instanceof UnsafeOutput) {
				UnsafeOutput unsafeOutput = (UnsafeOutput)output;
				unsafeOutput.writeBytes(object, offset, len);
			} else if (output instanceof UnsafeMemoryOutput) {
				UnsafeMemoryOutput unsafeOutput = (UnsafeMemoryOutput)output;
				unsafeOutput.writeBytes(object, offset, len);
			} else {
				long off;
				Unsafe unsafe = unsafe();
				for (off = offset; off < offset + len - 8; off += 8) {
					output.writeLong(unsafe.getLong(object, off));
				}

				if (off < offset + len) {
					for (; off < offset + len; ++off) {
						output.write(unsafe.getByte(object, off));
					}
				}
			}
		}

		final public void read (Input input, Object object) {
			if (bulkReadsSupported && input instanceof UnsafeInput) {
				UnsafeInput unsafeInput = (UnsafeInput)input;
				unsafeInput.readBytes(object, offset, len);
			} else if (bulkReadsSupported && input instanceof UnsafeMemoryInput) {
				UnsafeMemoryInput unsafeInput = (UnsafeMemoryInput)input;
				unsafeInput.readBytes(object, offset, len);
			} else {
				readSlow(input, object);
			}
		}

		/*** This is a fall-back solution for the case that bulk reading of bytes into object memory is not supported. Unfortunately,
		 * current Oracle JDKs do not allow for bulk reading in this style due to problems with GC.
		 * 
		 * @param input
		 * @param object */
		private void readSlow (Input input, Object object) {
			long off;
			Unsafe unsafe = unsafe();
			for (off = offset; off < offset + len - 8; off += 8) {
				unsafe.putLong(object, off, input.readLong());
			}

			if (off < offset + len) {
				for (; off < offset + len; ++off) {
					unsafe.putByte(object, off, input.readByte());
				}
			}
		}

		public void copy (Object original, Object copy) {
			unsafe().copyMemory(original, offset, copy, offset, len);
		}
	}

	final static class UnsafeObjectField extends ObjectField {
		public UnsafeObjectField (FieldSerializer fieldSerializer) {
			super(fieldSerializer);
		}

		public Object getField (Object object) throws IllegalArgumentException, IllegalAccessException {
			if (offset >= 0) {
				return unsafe().getObject(object, offset);
			} else
				throw new KryoException("Unknown offset");
		}

		public void setField (Object object, Object value) throws IllegalArgumentException, IllegalAccessException {
			if (offset != -1)
				unsafe().putObject(object, offset, value);
			else
				throw new KryoException("Unknown offset");
		}

		public void copy (Object original, Object copy) {
			try {
				if (offset != -1) {
					unsafe().putObject(copy, offset, kryo.copy(unsafe().getObject(original, offset)));
				} else
					throw new KryoException("Unknown offset");
			} catch (KryoException ex) {
				ex.addTrace(this + " (" + type.getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(this + " (" + type.getName() + ")");
				throw ex;
			}
		}
	}
}
