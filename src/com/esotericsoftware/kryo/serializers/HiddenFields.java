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

import java.io.InputStream;
import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;

/** PROTOTYPE: each template class below is defined once per field as a hidden class, with the field's VarHandle as class data.
 * The template stores it in a static final field, which the JIT treats as a constant, so the VarHandle access is inlined. */
final class HiddenFields {
	static final boolean ENABLED = Boolean.getBoolean("kryo.hidden");

	static CachedField intField (Field field) {
		return define(IntHiddenField.class, field, MethodType.methodType(void.class, Field.class), field);
	}

	static CachedField longField (Field field) {
		return define(LongHiddenField.class, field, MethodType.methodType(void.class, Field.class), field);
	}

	static CachedField doubleField (Field field) {
		return define(DoubleHiddenField.class, field, MethodType.methodType(void.class, Field.class), field);
	}

	static CachedField booleanField (Field field) {
		return define(BooleanHiddenField.class, field, MethodType.methodType(void.class, Field.class), field);
	}

	static CachedField stringField (Field field) {
		return define(StringHiddenField.class, field, MethodType.methodType(void.class, Field.class), field);
	}

	static CachedField objectField (Field field, FieldSerializer serializer, GenericType genericType) {
		return define(ObjectHiddenField.class, field,
			MethodType.methodType(void.class, Field.class, FieldSerializer.class, GenericType.class), field, serializer,
			genericType);
	}

	private static CachedField define (Class template, Field field, MethodType constructor, Object... args) {
		try {
			byte[] bytes;
			try (InputStream in = template.getResourceAsStream(template.getSimpleName() + ".class")) {
				bytes = in.readAllBytes();
			}
			Lookup lookup = MethodHandles.lookup().defineHiddenClassWithClassData(bytes, VarHandleField.varHandle(field), false);
			return (CachedField)lookup.findConstructor(lookup.lookupClass(), constructor).invokeWithArguments(args);
		} catch (Throwable t) {
			throw new KryoException("Unable to define hidden field accessor: " + field, t);
		}
	}

	static VarHandle classData (Lookup lookup) {
		try {
			return MethodHandles.classData(lookup, ConstantDescs.DEFAULT_NAME, VarHandle.class);
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
		handle.set(object, readValue(input));
	}

	public Object get (Object object) {
		return (Object)handle.get(object);
	}

	public void set (Object object, Object value) {
		handle.set(object, value);
	}
}
