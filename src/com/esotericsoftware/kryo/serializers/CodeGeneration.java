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
import static java.lang.constant.ConstantDescs.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.invoke.VarHandle.AccessMode;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/** Generates a hidden class per serialized class that writes and reads its fields with straight line code, using the Class-File
 * API (Java 24+). Primitive and String fields are accessed with VarHandles that are constants of the hidden class (its class
 * data) and written directly to the {@link Output}, object fields are delegated to their {@link ReflectField}, which holds the
 * serializer, value class and generic type. Final fields are set with a constant MethodHandle, because VarHandles can't set them.
 * The hidden class depends only on the field names, kinds and encodings, so it is shared by all serializers and Kryo instances
 * for a class.
 * <p>
 * Not supported, so the cached fields are used: records, fields set with a {@link FinalFieldSetter} and custom
 * {@link CachedField} implementations.
 * <p>
 * This is the only class that uses an API newer than Java 17: it is compiled with -source 17 on JDK 24+ and only loaded on Java
 * 24+. The Class-File API is referenced with qualified names and the IDE inspection for the language level is suppressed, see
 * "Building from source" in README.md. */
@SuppressWarnings("Since15")
final class CodeGeneration {
	static private final ClassDesc CD_GeneratedFields = ClassDesc.of(GeneratedFields.class.getName());
	static private final ClassDesc CD_ReflectField = ClassDesc.of(ReflectField.class.getName());
	static private final ClassDesc CD_CachedField = ClassDesc.of(CachedField.class.getName());
	static private final ClassDesc CD_CachedFieldArray = CD_CachedField.arrayType();
	static private final ClassDesc CD_FieldSerializer = ClassDesc.of(FieldSerializer.class.getName());
	static private final ClassDesc CD_TaggedFieldSerializer = ClassDesc.of(TaggedFieldSerializer.class.getName());
	static private final ClassDesc CD_Kryo = ClassDesc.of(Kryo.class.getName());
	static private final ClassDesc CD_Registration = ClassDesc.of(Registration.class.getName());
	static private final ClassDesc CD_CodeGeneration = ClassDesc.of(CodeGeneration.class.getName());
	static private final ClassDesc CD_Output = ClassDesc.of(Output.class.getName());
	static private final ClassDesc CD_Input = ClassDesc.of(Input.class.getName());
	static private final MethodTypeDesc MTD_classDataAt = MethodTypeDesc.of(CD_Object, CD_MethodHandles_Lookup, CD_String,
		CD_Class,
		CD_int);
	static private final MethodTypeDesc MTD_writeValue = MethodTypeDesc.of(CD_void, CD_Output, CD_Object, CD_Object);
	static private final MethodTypeDesc MTD_readValue = MethodTypeDesc.of(CD_Object, CD_Input);
	static private final MethodTypeDesc MTD_writeClass = MethodTypeDesc.of(CD_Registration, CD_Output, CD_Class);
	static private final MethodTypeDesc MTD_writeStringWithClass = MethodTypeDesc.of(CD_void, CD_Kryo, CD_Output, CD_String);
	static private final MethodTypeDesc MTD_readStringWithClass = MethodTypeDesc.of(CD_String, CD_FieldSerializer, CD_Input,
		CD_CachedField);
	static private final MethodTypeDesc MTD_readPrimitiveClass = MethodTypeDesc.of(CD_boolean, CD_FieldSerializer, CD_Input,
		CD_CachedField);
	static private final MethodTypeDesc MTD_writeVarInt = MethodTypeDesc.of(CD_int, CD_int, CD_boolean);
	static private final MethodTypeDesc MTD_readVarInt = MethodTypeDesc.of(CD_int, CD_boolean);
	static private final MethodTypeDesc MTD_readTag = MethodTypeDesc.of(CD_void, CD_Input, CD_int, CD_Object);

	/** The constructors of the hidden classes, by type and field signature. */
	static private final ClassValue<ConcurrentHashMap<String, MethodHandle>> constructors = new ClassValue<>() {
		protected ConcurrentHashMap<String, MethodHandle> computeValue (Class type) {
			return new ConcurrentHashMap<>();
		}
	};

	static private final MethodType constructorType = MethodType.methodType(void.class, FieldSerializer.class,
		CachedField[].class);

	// From java.lang.classfile.ClassFile.
	static private final int ACC_PUBLIC = 0x0001, ACC_PRIVATE = 0x0002, ACC_STATIC = 0x0008, ACC_FINAL = 0x0010,
		ACC_SUPER = 0x0020;
	static private final String INIT_NAME = "<init>", CLASS_INIT_NAME = "<clinit>";

	/** Returns the generated code for the fields, or null if code can't be generated for them.
	 * @param writeClasses If true, the class of each value is written before the value, which is written without null marker, like
	 *           CompatibleFieldSerializer with unknown field data.
	 * @param tags If not null, the tag of each field is written before the field, like TaggedFieldSerializer. When a read tag is
	 *           not the expected one, the field is read with {@link TaggedFieldSerializer#readTag(Input, int, Object)}.
	 * @throws KryoException if the hidden class can't be defined. */
	static GeneratedFields generate (FieldSerializer serializer, CachedField[] fields, boolean writeClasses, int[] tags) {
		if (serializer.recordConstructor != null) return null;
		Kind[] kinds = new Kind[fields.length];
		StringBuilder signature = new StringBuilder(writeClasses ? "classes;" : "");
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField field = fields[i];
			Kind kind = kind(field);
			if (kind == null) {
				if (DEBUG) debug("kryo",
					"Code generation is not supported for field: " + field.name + " (" + className(serializer.type) + ")");
				return null;
			}
			kinds[i] = kind;
			signature.append(field.field.getDeclaringClass().getName()).append('.').append(field.field.getName()).append(':')
				.append(kind);
			if (tags != null) signature.append(':').append(tags[i]);
			signature.append(';');
		}

		MethodHandle constructor = constructors.get(serializer.type).computeIfAbsent(signature.toString(),
			key -> define(serializer.type, fields, kinds, writeClasses, tags));
		try {
			return (GeneratedFields)constructor.invokeExact(serializer, fields);
		} catch (Throwable t) {
			throw new KryoException("Unable to create the generated fields for: " + className(serializer.type), t);
		}
	}

	/** The kind of a field for the generated code, or null if the field is not supported. */
	static private Kind kind (CachedField field) {
		if (field.finalSetter != null) return null;
		Class type = field.field.getType();
		if (type.isPrimitive()) {
			if (type == int.class) return field.varEncoding ? Kind.varInt : Kind.int_;
			if (type == long.class) return field.varEncoding ? Kind.varLong : Kind.long_;
			if (type == double.class) return Kind.double_;
			if (type == float.class) return Kind.float_;
			if (type == boolean.class) return Kind.boolean_;
			if (type == short.class) return Kind.short_;
			if (type == char.class) return Kind.char_;
			if (type == byte.class) return Kind.byte_;
			return null;
		}
		if (field instanceof ReflectField) return Kind.object;
		if (type == String.class) return Kind.string; // A String field written without references.
		return null;
	}

	// Called by the generated code when the classes are written:

	@SuppressWarnings("unused")
	static void writeStringWithClass (Kryo kryo, Output output, String value) {
		if (value == null) {
			kryo.writeClass(output, null);
			return;
		}
		kryo.writeClass(output, String.class);
		output.writeString(value);
	}

	/** @return null if the class was null. */
	@SuppressWarnings("unused")
	static String readStringWithClass (FieldSerializer serializer, Input input, CachedField field) {
		if (readClass(serializer, input, field) == null) return null;
		return input.readString();
	}

	/** Reads the class of a primitive field value.
	 * @return false if the class was null, then the field keeps its value. */
	@SuppressWarnings("unused")
	static boolean readPrimitiveClass (FieldSerializer serializer, Input input, CachedField field) {
		return readClass(serializer, input, field) != null;
	}

	/** Reads the class of a field value and ensures it is compatible with the field type, like CompatibleFieldSerializer with
	 * unknown field data.
	 * @return null if the class was null. */
	static Registration readClass (FieldSerializer serializer, Input input, CachedField field) {
		Registration registration;
		try {
			registration = serializer.kryo.readClass(input);
		} catch (KryoException ex) {
			throw new KryoException("Unable to read unknown data (unknown type). (" + serializer.type.getName() + "#" + field + ")",
				ex);
		}
		if (registration == null) return null;
		Class valueClass = registration.getType(), fieldType = field.field.getType();
		if (!isAssignableTo(valueClass, fieldType)) {
			throw new KryoException("Read type is incompatible with the field type: " + className(valueClass) + " -> "
				+ className(fieldType) + " (" + serializer.type.getName() + "#" + field + ")");
		}
		return registration;
	}

	/** Defines the hidden class and returns its constructor. */
	static private MethodHandle define (Class type, CachedField[] fields, Kind[] kinds, boolean writeClasses, int[] tags) {
		// The class data: the VarHandle of each field, then the setter MethodHandle of each final field.
		int n = fields.length;
		ArrayList<Object> classData = new ArrayList<>(n);
		for (int i = 0; i < n; i++)
			classData.add(VarHandleField.varHandle(fields[i].field));
		int[] setters = new int[n]; // The class data index of the setter for a final field, else -1.
		for (int i = 0; i < n; i++) {
			Field field = fields[i].field;
			setters[i] = -1;
			if (!((VarHandle)classData.get(i)).isAccessModeSupported(AccessMode.SET)) {
				try {
					MethodHandle setter = MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup())
						.unreflectSetter(field);
					Class valueType = kinds[i] == Kind.object ? Object.class : field.getType();
					setters[i] = classData.size();
					classData.add(setter.asType(MethodType.methodType(void.class, Object.class, valueType)));
				} catch (IllegalAccessException ex) {
					throw new KryoException("Unable to set field: " + field, ex);
				}
			}
		}

		Lookup lookup = MethodHandles.lookup();
		ClassDesc thisClass = ClassDesc.of(CodeGeneration.class.getPackageName(), "Generated$" + type.getSimpleName());
		byte[] bytes = java.lang.classfile.ClassFile.of().build(thisClass, cb -> {
			cb.withFlags(ACC_FINAL | ACC_SUPER).withSuperclass(CD_GeneratedFields);
			for (int i = 0; i < n; i++) {
				cb.withField("f" + i, CD_VarHandle, ACC_PRIVATE | ACC_STATIC | ACC_FINAL);
				if (setters[i] != -1) cb.withField("s" + i, CD_MethodHandle, ACC_PRIVATE | ACC_STATIC | ACC_FINAL);
			}
			cb.withField("serializer", CD_FieldSerializer, ACC_PRIVATE | ACC_FINAL);
			cb.withField("fields", CD_CachedFieldArray, ACC_PRIVATE | ACC_FINAL);

			// static { f0 = (VarHandle)MethodHandles.classDataAt(MethodHandles.lookup(), "_", VarHandle.class, 0); ... }
			cb.withMethodBody(CLASS_INIT_NAME, MTD_void, ACC_STATIC, code -> {
				for (int i = 0; i < n; i++) {
					code.invokestatic(CD_MethodHandles, "lookup", MethodTypeDesc.of(CD_MethodHandles_Lookup)) //
						.ldc("_").ldc(CD_VarHandle).loadConstant(i) //
						.invokestatic(CD_MethodHandles, "classDataAt", MTD_classDataAt) //
						.checkcast(CD_VarHandle).putstatic(thisClass, "f" + i, CD_VarHandle);
					if (setters[i] != -1) {
						code.invokestatic(CD_MethodHandles, "lookup", MethodTypeDesc.of(CD_MethodHandles_Lookup)) //
							.ldc("_").ldc(CD_MethodHandle).loadConstant(setters[i]) //
							.invokestatic(CD_MethodHandles, "classDataAt", MTD_classDataAt) //
							.checkcast(CD_MethodHandle).putstatic(thisClass, "s" + i, CD_MethodHandle);
					}
				}
				code.return_();
			});

			// public Generated$Type (FieldSerializer serializer, CachedField[] fields)
			cb.withMethodBody(INIT_NAME, MethodTypeDesc.of(CD_void, CD_FieldSerializer, CD_CachedFieldArray), ACC_PUBLIC, code -> {
				code.aload(0).invokespecial(CD_GeneratedFields, INIT_NAME, MTD_void) //
					.aload(0).aload(1).putfield(thisClass, "serializer", CD_FieldSerializer) //
					.aload(0).aload(2).putfield(thisClass, "fields", CD_CachedFieldArray).return_();
			});

			// public void write (Output output, Object object)
			cb.withMethodBody("write", MethodTypeDesc.of(CD_void, CD_Output, CD_Object), ACC_PUBLIC, code -> {
				for (int i = 0; i < n; i++) {
					Kind kind = kinds[i];
					// output.writeVarInt(tag, true);
					if (tags != null)
						code.aload(1).loadConstant(tags[i]).iconst_1().invokevirtual(CD_Output, "writeVarInt", MTD_writeVarInt).pop();
					if (kind == Kind.object) {
						// ((ReflectField)fields[i]).writeValue(output, object, (Object)fi.get(object));
						code.aload(0).getfield(thisClass, "fields", CD_CachedFieldArray).loadConstant(i).aaload()
							.checkcast(CD_ReflectField) //
							.aload(1).aload(2) //
							.getstatic(thisClass, "f" + i, CD_VarHandle).aload(2)
							.invokevirtual(CD_VarHandle, "get", MethodTypeDesc.of(CD_Object, CD_Object)) //
							.invokevirtual(CD_ReflectField, writeClasses ? "writeValueWithClass" : "writeValue", MTD_writeValue);
						continue;
					}
					if (writeClasses) {
						if (kind == Kind.string) {
							// CodeGeneration.writeStringWithClass(serializer.kryo, output, (String)fi.get(object));
							code.aload(0).getfield(thisClass, "serializer", CD_FieldSerializer)
								.getfield(CD_FieldSerializer, "kryo", CD_Kryo) //
								.aload(1).getstatic(thisClass, "f" + i, CD_VarHandle).aload(2)
								.invokevirtual(CD_VarHandle, "get", MethodTypeDesc.of(CD_String, CD_Object)) //
								.invokestatic(CD_CodeGeneration, "writeStringWithClass", MTD_writeStringWithClass);
							continue;
						}
						// serializer.kryo.writeClass(output, Integer.class);
						code.aload(0).getfield(thisClass, "serializer", CD_FieldSerializer)
							.getfield(CD_FieldSerializer, "kryo", CD_Kryo) //
							.aload(1).ldc(kind.wrapper).invokevirtual(CD_Kryo, "writeClass", MTD_writeClass).pop();
					}
					// output.writeX((X)fi.get(object));
					code.aload(1).getstatic(thisClass, "f" + i, CD_VarHandle).aload(2)
						.invokevirtual(CD_VarHandle, "get", MethodTypeDesc.of(kind.type, CD_Object));
					if (kind.varEncoding) code.iconst_0(); // optimizePositive
					code.invokevirtual(CD_Output, kind.write, kind.writeType);
					if (kind.varEncoding) code.pop(); // The number of bytes written.
				}
				code.return_();
			});

			// public void read (Input input, Object object)
			cb.withMethodBody("read", MethodTypeDesc.of(CD_void, CD_Input, CD_Object), ACC_PUBLIC, code -> {
				for (int i = 0; i < n; i++) {
					int index = i;
					if (tags == null) {
						readField(code, thisClass, index, kinds[index], setters[index], writeClasses);
						continue;
					}
					// int tag = input.readVarInt(true);
					// if (tag == TAG) read the field, else ((TaggedFieldSerializer)serializer).readTag(input, tag, object);
					int tag = code.allocateLocal(java.lang.classfile.TypeKind.INT);
					code.aload(1).iconst_1().invokevirtual(CD_Input, "readVarInt", MTD_readVarInt).istore(tag) //
						.iload(tag).loadConstant(tags[i]) //
						.ifThenElse(java.lang.classfile.Opcode.IF_ICMPEQ, //
							block -> readField(block, thisClass, index, kinds[index], setters[index], writeClasses), //
							block -> block.aload(0).getfield(thisClass, "serializer", CD_FieldSerializer)
								.checkcast(CD_TaggedFieldSerializer) //
								.aload(1).iload(tag).aload(2).invokevirtual(CD_TaggedFieldSerializer, "readTag", MTD_readTag));
				}
				code.return_();
			});
		});

		try {
			Lookup hidden = lookup.defineHiddenClassWithClassData(bytes, classData, true);
			return hidden.findConstructor(hidden.lookupClass(), constructorType)
				.asType(constructorType.changeReturnType(GeneratedFields.class));
		} catch (IllegalAccessException | NoSuchMethodException | RuntimeException ex) {
			throw new KryoException("Unable to define the generated fields for: " + className(type), ex);
		}
	}

	/** Emits: fi.set(object, value) or, for a final field, si.invokeExact(object, value). */
	static private void readField (java.lang.classfile.CodeBuilder code, ClassDesc thisClass, int i, Kind kind, int setter,
		boolean readClass) {
		if (readClass && kind != Kind.object && kind != Kind.string) {
			// if (CodeGeneration.readPrimitiveClass(serializer, input, fields[i])) fi.set(object, input.readX());
			code.aload(0).getfield(thisClass, "serializer", CD_FieldSerializer).aload(1) //
				.aload(0).getfield(thisClass, "fields", CD_CachedFieldArray).loadConstant(i).aaload() //
				.invokestatic(CD_CodeGeneration, "readPrimitiveClass", MTD_readPrimitiveClass) //
				.ifThen(java.lang.classfile.Opcode.IFNE, block -> readField(block, thisClass, i, kind, setter, false));
			return;
		}
		if (setter == -1)
			code.getstatic(thisClass, "f" + i, CD_VarHandle);
		else
			code.getstatic(thisClass, "s" + i, CD_MethodHandle);
		code.aload(2);
		if (kind == Kind.object) {
			// ((ReflectField)fields[i]).readValue(input)
			code.aload(0).getfield(thisClass, "fields", CD_CachedFieldArray).loadConstant(i).aaload().checkcast(CD_ReflectField) //
				.aload(1).invokevirtual(CD_ReflectField, readClass ? "readValueWithClass" : "readValue", MTD_readValue);
		} else if (readClass && kind == Kind.string) {
			// CodeGeneration.readStringWithClass(serializer, input, fields[i])
			code.aload(0).getfield(thisClass, "serializer", CD_FieldSerializer).aload(1) //
				.aload(0).getfield(thisClass, "fields", CD_CachedFieldArray).loadConstant(i).aaload() //
				.invokestatic(CD_CodeGeneration, "readStringWithClass", MTD_readStringWithClass);
		} else {
			// input.readX()
			code.aload(1);
			if (kind.varEncoding) code.iconst_0();
			code.invokevirtual(CD_Input, kind.read, kind.readType);
		}
		if (setter == -1)
			code.invokevirtual(CD_VarHandle, "set", MethodTypeDesc.of(CD_void, CD_Object, kind.type));
		else
			code.invokevirtual(CD_MethodHandle, "invokeExact", MethodTypeDesc.of(CD_void, CD_Object, kind.type));
	}

	/** How a field is written and read. */
	private enum Kind {
		varInt(CD_int, CD_Integer, "writeVarInt", "readVarInt", true), int_(CD_int, CD_Integer, "writeInt", "readInt", false), //
		varLong(CD_long, CD_Long, "writeVarLong", "readVarLong", true), long_(CD_long, CD_Long, "writeLong", "readLong", false), //
		double_(CD_double, CD_Double, "writeDouble", "readDouble", false), float_(CD_float, CD_Float, "writeFloat", "readFloat",
			false), //
		boolean_(CD_boolean, CD_Boolean, "writeBoolean", "readBoolean", false), short_(CD_short, CD_Short, "writeShort",
			"readShort", false), //
		char_(CD_char, CD_Character, "writeChar", "readChar", false), byte_(CD_byte, CD_Byte, "writeByte", "readByte", false), //
		string(CD_String, CD_String, "writeString", "readString", false), object(CD_Object, null, null, null, false);

		final ClassDesc type, wrapper;
		final String write, read;
		final boolean varEncoding;
		final MethodTypeDesc writeType, readType;

		Kind (ClassDesc type, ClassDesc wrapper, String write, String read, boolean varEncoding) {
			this.type = type;
			this.wrapper = wrapper;
			this.write = write;
			this.read = read;
			this.varEncoding = varEncoding;
			if (varEncoding) {
				writeType = MethodTypeDesc.of(CD_int, type, CD_boolean);
				readType = MethodTypeDesc.of(type, CD_boolean);
			} else {
				// writeShort(int) and writeChar(char) take their value as int and char, readShort() returns short.
				writeType = MethodTypeDesc.of(CD_void, type == CD_short ? CD_int : type);
				readType = MethodTypeDesc.of(type);
			}
		}
	}
}
