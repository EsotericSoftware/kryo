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
 * For example, for {@code class Nested { String name; Nested next; final int value; }} with FieldSerializer, the hidden class is
 * equivalent to:
 *
 * <pre>
 * final class Generated$Nested extends GeneratedFields {
 *    // The class data: the VarHandle of each field, then the setter of each final field.
 *    static final VarHandle f0, f1, f2;
 *    static final MethodHandle s2;
 *    static {
 *       f0 = (VarHandle)MethodHandles.classDataAt(MethodHandles.lookup(), "_", VarHandle.class, 0);
 *       ...
 *    }
 *    // Per serializer: the object fields are delegated to their ReflectField.
 *    final FieldSerializer serializer;
 *    final CachedField[] fields;
 *
 *    public void write (Output output, Object object) {
 *       int index = 0;
 *       try {
 *          output.writeString((String)f0.get(object));
 *          index = 1;
 *          ((ReflectField)fields[1]).writeValue(output, object, f1.get(object));
 *          index = 2;
 *          if (fields[2].varEncoding) output.writeVarInt((int)f2.get(object), false); else output.writeInt((int)f2.get(object));
 *       } catch (Throwable t) {
 *          throw GeneratedFields.writeError(t, fields[index], output);
 *       }
 *    }
 *
 *    public void read (Input input, Object object) {
 *       int index = 0;
 *       try {
 *          f0.set(object, input.readString());
 *          index = 1;
 *          f1.set(object, ((ReflectField)fields[1]).readValue(input));
 *          index = 2;
 *          s2.invokeExact(object, fields[2].varEncoding ? input.readVarInt(false) : input.readInt());
 *       } catch (Throwable t) {
 *          throw GeneratedFields.readError(t, fields[index], input);
 *       }
 *    }
 *
 *    // write and read with a ChunkedEncoding parameter wrap each field in beginField and endField.
 * }
 * </pre>
 *
 * With the classes written, like CompatibleFieldSerializer with unknown field data, the class is written before each value and
 * read with the helpers of {@link GeneratedFields}. With tags, like TaggedFieldSerializer, the tag is written before each field,
 * and read falls back to {@link TaggedFieldSerializer#readTag(Input, int, Object, boolean)} when the tag isn't the expected one.
 * Classes with more than {@link #batchSize} fields get a private method per batch.
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
	static private final ClassDesc CD_ChunkedEncoding = ClassDesc.of(ChunkedEncoding.class.getName());
	static private final ClassDesc CD_Kryo = ClassDesc.of(Kryo.class.getName());
	static private final ClassDesc CD_Registration = ClassDesc.of(Registration.class.getName());
	static private final ClassDesc CD_KryoException = ClassDesc.of(KryoException.class.getName());
	static private final ClassDesc CD_Output = ClassDesc.of(Output.class.getName());
	static private final ClassDesc CD_Input = ClassDesc.of(Input.class.getName());
	static private final MethodTypeDesc MTD_classDataAt = MethodTypeDesc.of(CD_Object, CD_MethodHandles_Lookup, CD_String,
		CD_Class,
		CD_int);
	static private final MethodTypeDesc MTD_writeValue = MethodTypeDesc.of(CD_void, CD_Output, CD_Object, CD_Object);
	static private final MethodTypeDesc MTD_readValue = MethodTypeDesc.of(CD_Object, CD_Input);
	static private final MethodTypeDesc MTD_readValueWithClass = MethodTypeDesc.of(CD_Object, CD_Input, CD_Object, CD_boolean);
	static private final MethodTypeDesc MTD_writeClass = MethodTypeDesc.of(CD_Registration, CD_Output, CD_Class);
	static private final MethodTypeDesc MTD_writeStringWithClass = MethodTypeDesc.of(CD_void, CD_Kryo, CD_Output, CD_String);
	static private final MethodTypeDesc MTD_readStringWithClass = MethodTypeDesc.of(CD_String, CD_FieldSerializer, CD_Input,
		CD_CachedField, CD_Object, CD_boolean);
	static private final MethodTypeDesc MTD_readPrimitiveClass = MethodTypeDesc.of(CD_boolean, CD_FieldSerializer, CD_Input,
		CD_CachedField, CD_boolean);
	static private final MethodTypeDesc MTD_beginFieldWrite = MethodTypeDesc.of(CD_long, CD_Output);
	static private final MethodTypeDesc MTD_endFieldWrite = MethodTypeDesc.of(CD_void, CD_Output, CD_long);
	static private final MethodTypeDesc MTD_beginFieldRead = MethodTypeDesc.of(CD_long, CD_Input);
	static private final MethodTypeDesc MTD_endFieldRead = MethodTypeDesc.of(CD_void, CD_Input, CD_long);
	static private final MethodTypeDesc MTD_writeVarInt = MethodTypeDesc.of(CD_int, CD_int, CD_boolean);
	static private final MethodTypeDesc MTD_writeVarLong = MethodTypeDesc.of(CD_int, CD_long, CD_boolean);
	static private final MethodTypeDesc MTD_readVarInt = MethodTypeDesc.of(CD_int, CD_boolean);
	static private final MethodTypeDesc MTD_readVarLong = MethodTypeDesc.of(CD_long, CD_boolean);
	static private final MethodTypeDesc MTD_readTag = MethodTypeDesc.of(CD_void, CD_Input, CD_int, CD_Object, CD_boolean);
	static private final MethodTypeDesc MTD_writeError = MethodTypeDesc.of(CD_KryoException, CD_Throwable, CD_CachedField,
		CD_Output);
	static private final MethodTypeDesc MTD_readError = MethodTypeDesc.of(CD_KryoException, CD_Throwable, CD_CachedField,
		CD_Input);

	/** The constructors of the hidden classes, by type and field signature. If defining a class failed, its KryoException, so it
	 * isn't tried again for each serializer. */
	static private final ClassValue<ConcurrentHashMap<String, Object>> constructors = new ClassValue<>() {
		protected ConcurrentHashMap<String, Object> computeValue (Class type) {
			return new ConcurrentHashMap<>();
		}
	};

	static private final MethodType constructorType = MethodType.methodType(void.class, FieldSerializer.class,
		CachedField[].class);

	/** The fields per generated method: the largest field code (tagged, chunked, with classes) is ~60 bytes, the JIT doesn't
	 * compile methods above 8000 bytes. */
	static int batchSize = 64;

	// From java.lang.classfile.ClassFile.
	static private final int ACC_PUBLIC = 0x0001, ACC_PRIVATE = 0x0002, ACC_STATIC = 0x0008, ACC_FINAL = 0x0010,
		ACC_SUPER = 0x0020;
	static private final String INIT_NAME = "<init>", CLASS_INIT_NAME = "<clinit>";

	/** Returns the generated code for the fields, or null if code can't be generated for them. Called by
	 * {@link GeneratedFields#generate(FieldSerializer, CachedField[], boolean, int[])} by name, because this class is compiled
	 * separately.
	 * @param writeClasses If true, the class of each value is written before the value, which is written without null marker, like
	 *           CompatibleFieldSerializer with unknown field data.
	 * @param tags If not null, the tag of each field is written before the field, like TaggedFieldSerializer. When a read tag is
	 *           not the expected one, the field is read with {@link TaggedFieldSerializer#readTag(Input, int, Object, boolean)}.
	 * @throws KryoException if the hidden class can't be defined. */
	static GeneratedFields generate (FieldSerializer serializer, CachedField[] fields, boolean writeClasses, int[] tags) {
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

		Object constructor = constructors.get(serializer.type).computeIfAbsent(signature.toString(), key -> {
			try {
				return define(serializer.type, fields, kinds, writeClasses, tags);
			} catch (KryoException ex) {
				return ex;
			}
		});
		if (constructor instanceof KryoException) throw (KryoException)constructor;
		try {
			GeneratedFields generated = (GeneratedFields)((MethodHandle)constructor).invokeExact(serializer, fields);
			generated.writesClasses = writeClasses;
			return generated;
		} catch (Throwable t) {
			throw new KryoException("Unable to create the generated fields for: " + className(serializer.type), t);
		}
	}

	/** The kind of a field for the generated code, or null if the field is not supported. */
	static private Kind kind (CachedField field) {
		if (field.finalSetter != null) return null;
		Class type = field.field.getType();
		if (type.isPrimitive()) {
			if (type == int.class) return Kind.int_;
			if (type == long.class) return Kind.long_;
			if (type == double.class) return Kind.double_;
			if (type == float.class) return Kind.float_;
			if (type == boolean.class) return Kind.boolean_;
			if (type == short.class) return Kind.short_;
			if (type == char.class) return Kind.char_;
			if (type == byte.class) return Kind.byte_;
			return null;
		}
		if (field instanceof ReflectField) return Kind.object;
		if (field.valueClass == String.class) return Kind.string; // A String field written without references, maybe a type
																						// variable.
		return null;
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
					MethodHandle setter = MethodHandles.lookup().unreflectSetter(field); // The field is accessible.
					// The type the generated code passes: String for a type variable resolved to String, else the field type.
					Class valueType = kinds[i] == Kind.object ? Object.class
						: kinds[i] == Kind.string ? String.class : field.getType();
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
					classData(code, i, CD_VarHandle).putstatic(thisClass, "f" + i, CD_VarHandle);
					if (setters[i] != -1) classData(code, setters[i], CD_MethodHandle).putstatic(thisClass, "s" + i, CD_MethodHandle);
				}
				code.return_();
			});

			// public Generated$Type (FieldSerializer serializer, CachedField[] fields)
			cb.withMethodBody(INIT_NAME, MethodTypeDesc.of(CD_void, CD_FieldSerializer, CD_CachedFieldArray), ACC_PUBLIC, code -> {
				code.aload(0).invokespecial(CD_GeneratedFields, INIT_NAME, MTD_void) //
					.aload(0).aload(1).putfield(thisClass, "serializer", CD_FieldSerializer) //
					.aload(0).aload(2).putfield(thisClass, "fields", CD_CachedFieldArray).return_();
			});

			// public void write (Output output, Object object) and write (Output output, Object object, ChunkedEncoding chunks)
			// public void read (Input input, Object object) and read (Input input, Object object, ChunkedEncoding chunks)
			for (boolean chunked : new boolean[] {false, true}) {
				MethodTypeDesc writeType = chunked ? MethodTypeDesc.of(CD_void, CD_Output, CD_Object, CD_ChunkedEncoding)
					: MethodTypeDesc.of(CD_void, CD_Output, CD_Object);
				MethodTypeDesc readType = chunked ? MethodTypeDesc.of(CD_void, CD_Input, CD_Object, CD_ChunkedEncoding)
					: MethodTypeDesc.of(CD_void, CD_Input, CD_Object);
				batches(cb, thisClass, "write", writeType, n, chunked,
					(code, from, to) -> write(code, thisClass, kinds, writeClasses, tags, chunked, from, to));
				batches(cb, thisClass, "read", readType, n, chunked,
					(code, from, to) -> read(code, thisClass, kinds, setters, writeClasses, tags, chunked, from, to));
			}
		});

		try {
			Lookup hidden = lookup.defineHiddenClassWithClassData(bytes, classData, true);
			return hidden.findConstructor(hidden.lookupClass(), constructorType)
				.asType(constructorType.changeReturnType(GeneratedFields.class));
		} catch (IllegalAccessException | NoSuchMethodException | RuntimeException ex) {
			throw new KryoException("Unable to define the generated fields for: " + className(type), ex);
		}
	}

	/** Emits the fields of a method in batches. The JIT doesn't compile methods above 8000 bytes, so a class with many fields gets
	 * a private method per batch of fields, which the public method calls. */
	static private void batches (java.lang.classfile.ClassBuilder cb, ClassDesc thisClass, String name, MethodTypeDesc type, int n,
		boolean chunked, Batch batch) {
		if (n <= batchSize) {
			cb.withMethodBody(name, type, ACC_PUBLIC, code -> batch.emit(code, 0, n));
			return;
		}
		cb.withMethodBody(name, type, ACC_PUBLIC, code -> {
			for (int from = 0; from < n; from += batchSize) {
				code.aload(0).aload(1).aload(2);
				if (chunked) code.aload(3);
				code.invokevirtual(thisClass, name + from, type);
			}
			code.return_();
		});
		for (int from = 0; from < n; from += batchSize) {
			int start = from, end = Math.min(from + batchSize, n);
			cb.withMethodBody(name + from, type, ACC_PRIVATE, code -> batch.emit(code, start, end));
		}
	}

	private interface Batch {
		void emit (java.lang.classfile.CodeBuilder code, int from, int to);
	}

	/** Emits the body of write for the fields from (inclusive) to (exclusive): locals 1 output, 2 object, 3 chunks if chunked. */
	static private void write (java.lang.classfile.CodeBuilder code, ClassDesc thisClass, Kind[] kinds, boolean writeClasses,
		int[] tags, boolean chunked, int from, int to) {
		int mark = chunked ? code.allocateLocal(java.lang.classfile.TypeKind.LONG) : -1;
		// int index = from; try { index = i; write field i; ... } catch (Throwable t) { throw writeError(t, fields[index], output);
		// }
		int index = code.allocateLocal(java.lang.classfile.TypeKind.INT);
		code.loadConstant(from).istore(index);
		code.trying(block -> {
			for (int i = from; i < to; i++) {
				if (i != from) block.loadConstant(i).istore(index);
				writeField(block, thisClass, kinds[i], i, writeClasses, tags, chunked, mark);
			}
		}, catches -> catches.catchingAll(handler -> handler.aload(0).getfield(thisClass, "fields", CD_CachedFieldArray)
			.iload(index).aaload().aload(1).invokestatic(CD_GeneratedFields, "writeError", MTD_writeError).athrow()));
		code.return_();
	}

	/** Emits the code that writes field i, see {@link #write}. */
	static private void writeField (java.lang.classfile.CodeBuilder code, ClassDesc thisClass, Kind kind, int i,
		boolean writeClasses, int[] tags, boolean chunked, int mark) {
		// output.writeVarInt(tag, true);
		if (tags != null)
			code.aload(1).loadConstant(tags[i]).iconst_1().invokevirtual(CD_Output, "writeVarInt", MTD_writeVarInt).pop();
		// long mark = chunks.beginField(output);
		if (chunked) code.aload(3).aload(1).invokeinterface(CD_ChunkedEncoding, "beginField", MTD_beginFieldWrite).lstore(mark);
		if (kind == Kind.object) {
			// ((ReflectField)fields[i]).writeValue(output, object, (Object)fi.get(object));
			field(code, thisClass, i).checkcast(CD_ReflectField).aload(1).aload(2);
			value(code, thisClass, i, CD_Object).invokevirtual(CD_ReflectField,
				writeClasses ? "writeValueWithClass" : "writeValue",
				MTD_writeValue);
		} else if (writeClasses && kind == Kind.string) {
			// GeneratedFields.writeStringWithClass(serializer.kryo, output, (String)fi.get(object));
			kryo(code, thisClass).aload(1);
			value(code, thisClass, i, CD_String).invokestatic(CD_GeneratedFields, "writeStringWithClass",
				MTD_writeStringWithClass);
		} else {
			// serializer.kryo.writeClass(output, Integer.class);
			if (writeClasses)
				kryo(code, thisClass).aload(1).ldc(kind.wrapper).invokevirtual(CD_Kryo, "writeClass", MTD_writeClass).pop();
			// output.writeX((X)fi.get(object)), or writeVarX((X)fi.get(object), false) if fields[i].varEncoding
			code.aload(1);
			value(code, thisClass, i, kind.type);
			if (kind.varEncodable) {
				field(code, thisClass, i).getfield(CD_CachedField, "varEncoding", CD_boolean).ifThenElse(
					java.lang.classfile.Opcode.IFNE, //
					block -> block.iconst_0().invokevirtual(CD_Output, kind == Kind.int_ ? "writeVarInt" : "writeVarLong",
						kind == Kind.int_ ? MTD_writeVarInt : MTD_writeVarLong).pop(), //
					block -> block.invokevirtual(CD_Output, kind.write, kind.writeType));
			} else
				code.invokevirtual(CD_Output, kind.write, kind.writeType);
		}
		// chunks.endField(output, mark);
		if (chunked) code.aload(3).aload(1).lload(mark).invokeinterface(CD_ChunkedEncoding, "endField", MTD_endFieldWrite);
	}

	/** Emits the body of read for the fields from (inclusive) to (exclusive): locals 1 input, 2 object, 3 chunks if chunked. */
	static private void read (java.lang.classfile.CodeBuilder code, ClassDesc thisClass, Kind[] kinds, int[] setters,
		boolean readClasses, int[] tags, boolean chunked, int from, int to) {
		int tag = tags != null ? code.allocateLocal(java.lang.classfile.TypeKind.INT) : -1;
		int end = chunked ? code.allocateLocal(java.lang.classfile.TypeKind.LONG) : -1;
		// int index = from; try { index = i; read field i; ... } catch (Throwable t) { throw readError(t, fields[index], input); }
		int index = code.allocateLocal(java.lang.classfile.TypeKind.INT);
		code.loadConstant(from).istore(index);
		code.trying(block -> {
			for (int i = from; i < to; i++) {
				if (i != from) block.loadConstant(i).istore(index);
				readEntry(block, thisClass, kinds, setters, readClasses, tags, chunked, tag, end, i);
			}
		}, catches -> catches.catchingAll(handler -> handler.aload(0).getfield(thisClass, "fields", CD_CachedFieldArray)
			.iload(index).aaload().aload(1).invokestatic(CD_GeneratedFields, "readError", MTD_readError).athrow()));
		code.return_();
	}

	/** Emits the code that reads field i, with its tag and chunk if any, see {@link #read}. */
	static private void readEntry (java.lang.classfile.CodeBuilder code, ClassDesc thisClass, Kind[] kinds, int[] setters,
		boolean readClasses, int[] tags, boolean chunked, int tag, int end, int i) {
		int index = i;
		// int tag = input.readVarInt(true);
		if (tags != null) code.aload(1).iconst_1().invokevirtual(CD_Input, "readVarInt", MTD_readVarInt).istore(tag);
		// long end = chunks.beginField(input);
		if (chunked) code.aload(3).aload(1).invokeinterface(CD_ChunkedEncoding, "beginField", MTD_beginFieldRead).lstore(end);
		if (tags == null)
			readField(code, thisClass, index, kinds[index], setters[index], readClasses, chunked);
		else {
			// if (tag == TAG) read the field, else ((TaggedFieldSerializer)serializer).readTag(input, tag, object, chunked);
			code.iload(tag).loadConstant(tags[i]).ifThenElse(java.lang.classfile.Opcode.IF_ICMPEQ, //
				block -> readField(block, thisClass, index, kinds[index], setters[index], readClasses, chunked), //
				block -> block.aload(0).getfield(thisClass, "serializer", CD_FieldSerializer).checkcast(CD_TaggedFieldSerializer) //
					.aload(1).iload(tag).aload(2).loadConstant(chunked ? 1 : 0)
					.invokevirtual(CD_TaggedFieldSerializer, "readTag", MTD_readTag));
		}
		// chunks.endField(input, end);
		if (chunked) code.aload(3).aload(1).lload(end).invokeinterface(CD_ChunkedEncoding, "endField", MTD_endFieldRead);
	}

	/** Emits: fi.set(object, value) or, for a final field, si.invokeExact(object, value). With readClass, the class is read first
	 * and a null class keeps the value of a primitive field. */
	static private void readField (java.lang.classfile.CodeBuilder code, ClassDesc thisClass, int i, Kind kind, int setter,
		boolean readClass, boolean chunked) {
		if (readClass && kind != Kind.object && kind != Kind.string) {
			// if (GeneratedFields.readPrimitiveClass(serializer, input, fields[i], chunked)) fi.set(object, input.readX());
			code.aload(0).getfield(thisClass, "serializer", CD_FieldSerializer).aload(1);
			field(code, thisClass, i).loadConstant(chunked ? 1 : 0) //
				.invokestatic(CD_GeneratedFields, "readPrimitiveClass", MTD_readPrimitiveClass) //
				.ifThen(java.lang.classfile.Opcode.IFNE, block -> readField(block, thisClass, i, kind, setter, false, chunked));
			return;
		}
		if (setter == -1)
			code.getstatic(thisClass, "f" + i, CD_VarHandle);
		else
			code.getstatic(thisClass, "s" + i, CD_MethodHandle);
		code.aload(2);
		if (kind == Kind.object) {
			// ((ReflectField)fields[i]).readValue(input) or readValueWithClass(input, object, chunked)
			field(code, thisClass, i).checkcast(CD_ReflectField).aload(1);
			if (readClass)
				code.aload(2).loadConstant(chunked ? 1 : 0).invokevirtual(CD_ReflectField, "readValueWithClass",
					MTD_readValueWithClass);
			else
				code.invokevirtual(CD_ReflectField, "readValue", MTD_readValue);
		} else if (readClass && kind == Kind.string) {
			// GeneratedFields.readStringWithClass(serializer, input, fields[i], object, chunked)
			code.aload(0).getfield(thisClass, "serializer", CD_FieldSerializer).aload(1);
			field(code, thisClass, i).aload(2).loadConstant(chunked ? 1 : 0) //
				.invokestatic(CD_GeneratedFields, "readStringWithClass", MTD_readStringWithClass);
		} else if (kind.varEncodable) {
			// fields[i].varEncoding ? input.readVarX(false) : input.readX()
			field(code, thisClass, i).getfield(CD_CachedField, "varEncoding", CD_boolean).ifThenElse(java.lang.classfile.Opcode.IFNE, //
				block -> block.aload(1).iconst_0().invokevirtual(CD_Input, kind == Kind.int_ ? "readVarInt" : "readVarLong",
					kind == Kind.int_ ? MTD_readVarInt : MTD_readVarLong), //
				block -> block.aload(1).invokevirtual(CD_Input, kind.read, kind.readType));
		} else
			code.aload(1).invokevirtual(CD_Input, kind.read, kind.readType); // input.readX()
		if (setter == -1)
			code.invokevirtual(CD_VarHandle, "set", MethodTypeDesc.of(CD_void, CD_Object, kind.type));
		else
			code.invokevirtual(CD_MethodHandle, "invokeExact", MethodTypeDesc.of(CD_void, CD_Object, kind.type));
	}

	/** Emits: (T)MethodHandles.classDataAt(MethodHandles.lookup(), "_", T.class, index) */
	static private java.lang.classfile.CodeBuilder classData (java.lang.classfile.CodeBuilder code, int index, ClassDesc type) {
		return code.invokestatic(CD_MethodHandles, "lookup", MethodTypeDesc.of(CD_MethodHandles_Lookup)) //
			.ldc("_").ldc(type).loadConstant(index) //
			.invokestatic(CD_MethodHandles, "classDataAt", MTD_classDataAt).checkcast(type);
	}

	/** Emits: fields[i] */
	static private java.lang.classfile.CodeBuilder field (java.lang.classfile.CodeBuilder code, ClassDesc thisClass, int i) {
		return code.aload(0).getfield(thisClass, "fields", CD_CachedFieldArray).loadConstant(i).aaload();
	}

	/** Emits: serializer.kryo */
	static private java.lang.classfile.CodeBuilder kryo (java.lang.classfile.CodeBuilder code, ClassDesc thisClass) {
		return code.aload(0).getfield(thisClass, "serializer", CD_FieldSerializer).getfield(CD_FieldSerializer, "kryo", CD_Kryo);
	}

	/** Emits: (T)fi.get(object) */
	static private java.lang.classfile.CodeBuilder value (java.lang.classfile.CodeBuilder code, ClassDesc thisClass, int i,
		ClassDesc type) {
		return code.getstatic(thisClass, "f" + i, CD_VarHandle).aload(2).invokevirtual(CD_VarHandle, "get",
			MethodTypeDesc.of(type, CD_Object));
	}

	/** How a field is written and read. */
	private enum Kind {
		int_(CD_int, CD_Integer, "writeInt", "readInt"), long_(CD_long, CD_Long, "writeLong", "readLong"), //
		double_(CD_double, CD_Double, "writeDouble", "readDouble"), float_(CD_float, CD_Float, "writeFloat", "readFloat"), //
		boolean_(CD_boolean, CD_Boolean, "writeBoolean", "readBoolean"), short_(CD_short, CD_Short, "writeShort", "readShort"), //
		char_(CD_char, CD_Character, "writeChar", "readChar"), byte_(CD_byte, CD_Byte, "writeByte", "readByte"), //
		string(CD_String, CD_String, "writeString", "readString"), object(CD_Object, null, null, null);

		final ClassDesc type, wrapper;
		final String write, read;
		final MethodTypeDesc writeType, readType;
		/** True for int and long, which are written with variable length if {@link CachedField#varEncoding}. */
		final boolean varEncodable;

		Kind (ClassDesc type, ClassDesc wrapper, String write, String read) {
			this.type = type;
			this.wrapper = wrapper;
			this.write = write;
			this.read = read;
			varEncodable = type == CD_int || type == CD_long;
			// writeShort(int) takes its value as int.
			writeType = MethodTypeDesc.of(CD_void, type == CD_short ? CD_int : type);
			readType = MethodTypeDesc.of(type);
		}
	}
}
