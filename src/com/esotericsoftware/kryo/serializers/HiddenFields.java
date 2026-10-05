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

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;
import com.esotericsoftware.kryo.util.Generics.GenericType;

import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

/** Fields that are accessed with a {@link VarHandle} that is a constant. A VarHandle in an instance field, like in
 * {@link VarHandleField}, is not a constant for the JIT compiler, so each access is an indirect call that is not inlined. Each
 * template class below is defined once per field as a hidden class with the VarHandle of the field as class data, which the
 * template stores in a static final field. So the access is compiled like a direct field access, which is much faster.
 * <p>
 * The code that reads and writes the value is in the templates and not in a superclass, because a call from shared code to the
 * hidden class would be another indirect call. */
final class HiddenFields {
	/** Hidden classes can't be defined on Android or in a native image. Can be set by tests. */
	static boolean supported = !isAndroid && !isNativeImage;

	static private final MethodType fieldConstructor = MethodType.methodType(void.class, Field.class);
	static private final MethodType objectConstructor = MethodType.methodType(void.class, Field.class, FieldSerializer.class,
		GenericType.class);

	/** @param string True for a String field that is written without references.
	 * @throws KryoException if the hidden class can't be defined or the field can't be accessed with a VarHandle. */
	static CachedField create (Field field, Class fieldClass, boolean string, FieldSerializer serializer,
		GenericType genericType) {
		Class template;
		if (fieldClass == int.class)
			template = IntHiddenField.class;
		else if (fieldClass == long.class)
			template = LongHiddenField.class;
		else if (fieldClass == double.class)
			template = DoubleHiddenField.class;
		else if (fieldClass == boolean.class)
			template = BooleanHiddenField.class;
		else if (fieldClass == float.class)
			template = FloatHiddenField.class;
		else if (fieldClass == short.class)
			template = ShortHiddenField.class;
		else if (fieldClass == char.class)
			template = CharHiddenField.class;
		else if (fieldClass == byte.class)
			template = ByteHiddenField.class;
		else if (string)
			template = StringHiddenField.class;
		else
			return define(ObjectHiddenField.class, field, objectConstructor, field, serializer, genericType);
		return define(template, field, fieldConstructor, field);
	}

	/** The constructors of the hidden classes by template and field name, for the declaring class of the field. The hidden class
	 * of a field is shared by all serializers and Kryo instances, so it is defined and compiled only once. The map is held by the
	 * declaring class, so it doesn't prevent unloading it. */
	private static final ClassValue<ConcurrentHashMap<String, MethodHandle>> constructors = new ClassValue<>() {
		protected ConcurrentHashMap<String, MethodHandle> computeValue (Class type) {
			return new ConcurrentHashMap<>();
		}
	};

	/** The class files of the templates, which are read only once. */
	private static final ClassValue<byte[]> templateBytes = new ClassValue<>() {
		protected byte[] computeValue (Class template) {
			try (InputStream in = template.getResourceAsStream(template.getSimpleName() + ".class")) {
				return in.readAllBytes();
			} catch (Exception ex) {
				throw new KryoException("Unable to read class file: " + template.getName(), ex);
			}
		}
	};

	private static CachedField define (Class template, Field field, MethodType constructorType, Object... args) {
		try {
			ConcurrentHashMap<String, MethodHandle> map = constructors.get(field.getDeclaringClass());
			String key = template.getSimpleName() + ' ' + field.getName();
			MethodHandle constructor = map.get(key);
			if (constructor == null) {
				Lookup lookup = MethodHandles.lookup().defineHiddenClassWithClassData(templateBytes.get(template),
					VarHandleField.varHandle(field), false);
				constructor = lookup.findConstructor(lookup.lookupClass(), constructorType);
				MethodHandle existing = map.putIfAbsent(key, constructor);
				if (existing != null) constructor = existing;
			}
			return (CachedField)constructor.invokeWithArguments(args);
		} catch (Throwable t) {
			throw new KryoException("Unable to define a hidden class for field: " + field, t);
		}
	}

	static VarHandle classData (Lookup lookup) {
		try {
			return MethodHandles.classData(lookup, "_", VarHandle.class); // ConstantDescs.DEFAULT_NAME
		} catch (IllegalAccessException ex) {
			throw new KryoException(ex);
		}
	}
}

final class IntHiddenField extends CachedField {
	static final VarHandle handle = HiddenFields.classData(MethodHandles.lookup());

	IntHiddenField (Field field) {
		super(field);
	}

	public void write (Output output, Object object) {
		if (varEncoding)
			output.writeVarInt((int)handle.get(object), false);
		else
			output.writeInt((int)handle.get(object));
	}

	public void read (Input input, Object object) {
		if (varEncoding)
			handle.set(object, input.readVarInt(false));
		else
			handle.set(object, input.readInt());
	}

	public Object read (Input input) {
		return varEncoding ? input.readVarInt(false) : input.readInt();
	}

	public void copy (Object original, Object copy) {
		handle.set(copy, (int)handle.get(original));
	}
}

final class LongHiddenField extends CachedField {
	static final VarHandle handle = HiddenFields.classData(MethodHandles.lookup());

	LongHiddenField (Field field) {
		super(field);
	}

	public void write (Output output, Object object) {
		if (varEncoding)
			output.writeVarLong((long)handle.get(object), false);
		else
			output.writeLong((long)handle.get(object));
	}

	public void read (Input input, Object object) {
		if (varEncoding)
			handle.set(object, input.readVarLong(false));
		else
			handle.set(object, input.readLong());
	}

	public Object read (Input input) {
		return varEncoding ? input.readVarLong(false) : input.readLong();
	}

	public void copy (Object original, Object copy) {
		handle.set(copy, (long)handle.get(original));
	}
}

final class DoubleHiddenField extends CachedField {
	static final VarHandle handle = HiddenFields.classData(MethodHandles.lookup());

	DoubleHiddenField (Field field) {
		super(field);
	}

	public void write (Output output, Object object) {
		output.writeDouble((double)handle.get(object));
	}

	public void read (Input input, Object object) {
		handle.set(object, input.readDouble());
	}

	public Object read (Input input) {
		return input.readDouble();
	}

	public void copy (Object original, Object copy) {
		handle.set(copy, (double)handle.get(original));
	}
}

final class BooleanHiddenField extends CachedField {
	static final VarHandle handle = HiddenFields.classData(MethodHandles.lookup());

	BooleanHiddenField (Field field) {
		super(field);
	}

	public void write (Output output, Object object) {
		output.writeBoolean((boolean)handle.get(object));
	}

	public void read (Input input, Object object) {
		handle.set(object, input.readBoolean());
	}

	public Object read (Input input) {
		return input.readBoolean();
	}

	public void copy (Object original, Object copy) {
		handle.set(copy, (boolean)handle.get(original));
	}
}

final class FloatHiddenField extends CachedField {
	static final VarHandle handle = HiddenFields.classData(MethodHandles.lookup());

	FloatHiddenField (Field field) {
		super(field);
	}

	public void write (Output output, Object object) {
		output.writeFloat((float)handle.get(object));
	}

	public void read (Input input, Object object) {
		handle.set(object, input.readFloat());
	}

	public Object read (Input input) {
		return input.readFloat();
	}

	public void copy (Object original, Object copy) {
		handle.set(copy, (float)handle.get(original));
	}
}

final class ShortHiddenField extends CachedField {
	static final VarHandle handle = HiddenFields.classData(MethodHandles.lookup());

	ShortHiddenField (Field field) {
		super(field);
	}

	public void write (Output output, Object object) {
		output.writeShort((short)handle.get(object));
	}

	public void read (Input input, Object object) {
		handle.set(object, input.readShort());
	}

	public Object read (Input input) {
		return input.readShort();
	}

	public void copy (Object original, Object copy) {
		handle.set(copy, (short)handle.get(original));
	}
}

final class CharHiddenField extends CachedField {
	static final VarHandle handle = HiddenFields.classData(MethodHandles.lookup());

	CharHiddenField (Field field) {
		super(field);
	}

	public void write (Output output, Object object) {
		output.writeChar((char)handle.get(object));
	}

	public void read (Input input, Object object) {
		handle.set(object, input.readChar());
	}

	public Object read (Input input) {
		return input.readChar();
	}

	public void copy (Object original, Object copy) {
		handle.set(copy, (char)handle.get(original));
	}
}

final class ByteHiddenField extends CachedField {
	static final VarHandle handle = HiddenFields.classData(MethodHandles.lookup());

	ByteHiddenField (Field field) {
		super(field);
	}

	public void write (Output output, Object object) {
		output.writeByte((byte)handle.get(object));
	}

	public void read (Input input, Object object) {
		handle.set(object, input.readByte());
	}

	public Object read (Input input) {
		return input.readByte();
	}

	public void copy (Object original, Object copy) {
		handle.set(copy, (byte)handle.get(original));
	}
}

final class StringHiddenField extends CachedField {
	static final VarHandle handle = HiddenFields.classData(MethodHandles.lookup());

	StringHiddenField (Field field) {
		super(field);
	}

	public void write (Output output, Object object) {
		output.writeString((String)handle.get(object));
	}

	public void read (Input input, Object object) {
		handle.set(object, input.readString());
	}

	public Object read (Input input) {
		return input.readString();
	}

	public void copy (Object original, Object copy) {
		handle.set(copy, (String)handle.get(original));
	}
}

final class ObjectHiddenField extends ReflectField {
	static final VarHandle handle = HiddenFields.classData(MethodHandles.lookup());

	ObjectHiddenField (Field field, FieldSerializer serializer, GenericType genericType) {
		super(field, serializer, genericType);
	}

	public void write (Output output, Object object) {
		writeValue(output, object, (Object)handle.get(object));
	}

	public void read (Input input, Object object) {
		Object value = readValue(input);
		try {
			handle.set(object, value);
		} catch (Throwable t) { // Eg the value has the wrong type.
			KryoException ex = new KryoException(t);
			ex.addTrace(name + " (" + fieldSerializer.type.getName() + ")");
			throw ex;
		}
	}

	public void copy (Object original, Object copy) {
		try {
			handle.set(copy, fieldSerializer.kryo.copy((Object)handle.get(original)));
		} catch (KryoException ex) {
			ex.addTrace(name + " (" + fieldSerializer.type.getName() + ")");
			throw ex;
		} catch (Throwable t) {
			KryoException ex = new KryoException(t);
			ex.addTrace(name + " (" + fieldSerializer.type.getName() + ")");
			throw ex;
		}
	}

	public Object get (Object object) {
		return (Object)handle.get(object);
	}

	public void set (Object object, Object value) {
		handle.set(object, value);
	}
}
