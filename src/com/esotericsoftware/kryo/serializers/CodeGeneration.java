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

	// The hidden class.

	/** Defines the hidden class for the fields and returns its constructor. */
	static private MethodHandle define (Class type, CachedField[] fields, Kind[] kinds, boolean classes, int[] tags) {
		int n = fields.length;
		ArrayList<Object> classData = new ArrayList<>(n);
		int[] setters = classData(fields, kinds, classData);

		Lookup lookup = MethodHandles.lookup();
		ClassDesc thisClass = ClassDesc.of(CodeGeneration.class.getPackageName(), "Generated$" + type.getSimpleName());
		byte[] bytes = java.lang.classfile.ClassFile.of().build(thisClass, cb -> {
			cb.withFlags(ACC_FINAL | ACC_SUPER).withSuperclass(CD_GeneratedFields);
			members(cb, thisClass, n, setters);
			constructor(cb, thisClass);
			// public void write (Output output, Object object) and write (Output output, Object object, ChunkedEncoding chunks)
			// public void read (Input input, Object object) and read (Input input, Object object, ChunkedEncoding chunks)
			for (boolean chunked : new boolean[] {false, true}) {
				MethodTypeDesc writeType = chunked ? MethodTypeDesc.of(CD_void, CD_Output, CD_Object, CD_ChunkedEncoding)
					: MethodTypeDesc.of(CD_void, CD_Output, CD_Object);
				MethodTypeDesc readType = chunked ? MethodTypeDesc.of(CD_void, CD_Input, CD_Object, CD_ChunkedEncoding)
					: MethodTypeDesc.of(CD_void, CD_Input, CD_Object);
				batches(cb, thisClass, "write", writeType, n, chunked,
					(code, from, to) -> new Emitter(code, thisClass, kinds, setters, tags, classes, chunked).write(from, to));
				batches(cb, thisClass, "read", readType, n, chunked,
					(code, from, to) -> new Emitter(code, thisClass, kinds, setters, tags, classes, chunked).read(from, to));
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

	/** Collects the class data: the VarHandle of each field, then the setter MethodHandle of each final field, which VarHandles
	 * can't set.
	 * @return The class data index of the setter of each field, or -1. */
	static private int[] classData (CachedField[] fields, Kind[] kinds, ArrayList<Object> classData) {
		int n = fields.length;
		for (int i = 0; i < n; i++)
			classData.add(VarHandleField.varHandle(fields[i].field));
		int[] setters = new int[n];
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
		return setters;
	}

	/** Emits the fields: the VarHandle fi and setter si of each field, initialized from the class data, and the serializer and
	 * fields of the serializer instance. */
	static private void members (java.lang.classfile.ClassBuilder cb, ClassDesc thisClass, int n, int[] setters) {
		// static final VarHandle f0; static final MethodHandle s0; ...
		for (int i = 0; i < n; i++) {
			cb.withField("f" + i, CD_VarHandle, ACC_PRIVATE | ACC_STATIC | ACC_FINAL);
			if (setters[i] != -1) cb.withField("s" + i, CD_MethodHandle, ACC_PRIVATE | ACC_STATIC | ACC_FINAL);
		}
		// final FieldSerializer serializer; final CachedField[] fields;
		cb.withField("serializer", CD_FieldSerializer, ACC_PRIVATE | ACC_FINAL);
		cb.withField("fields", CD_CachedFieldArray, ACC_PRIVATE | ACC_FINAL);
		// static { f0 = (VarHandle)MethodHandles.classDataAt(MethodHandles.lookup(), "_", VarHandle.class, 0); ... }
		cb.withMethodBody(CLASS_INIT_NAME, MTD_void, ACC_STATIC, code -> {
			for (int i = 0; i < n; i++) {
				loadClassData(code, i, CD_VarHandle).putstatic(thisClass, "f" + i, CD_VarHandle);
				if (setters[i] != -1) loadClassData(code, setters[i], CD_MethodHandle).putstatic(thisClass, "s" + i, CD_MethodHandle);
			}
			code.return_();
		});
	}

	/** Emits: (T)MethodHandles.classDataAt(MethodHandles.lookup(), "_", T.class, index) */
	static private java.lang.classfile.CodeBuilder loadClassData (java.lang.classfile.CodeBuilder code, int index,
		ClassDesc type) {
		return code.invokestatic(CD_MethodHandles, "lookup", MethodTypeDesc.of(CD_MethodHandles_Lookup)) //
			.ldc("_").ldc(type).loadConstant(index) //
			.invokestatic(CD_MethodHandles, "classDataAt", MTD_classDataAt).checkcast(type);
	}

	/** Emits: public Generated$Type (FieldSerializer serializer, CachedField[] fields) */
	static private void constructor (java.lang.classfile.ClassBuilder cb, ClassDesc thisClass) {
		cb.withMethodBody(INIT_NAME, MethodTypeDesc.of(CD_void, CD_FieldSerializer, CD_CachedFieldArray), ACC_PUBLIC, code -> {
			code.aload(0).invokespecial(CD_GeneratedFields, INIT_NAME, MTD_void) //
				.aload(0).aload(1).putfield(thisClass, "serializer", CD_FieldSerializer) //
				.aload(0).aload(2).putfield(thisClass, "fields", CD_CachedFieldArray).return_();
		});
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

	/** Emits the body of a write or read method of the hidden class, see the class javadoc. The locals are 0 this, 1 the output or
	 * input, 2 the object and 3 the chunks if chunked. Each method emits the statement or expression in its comment. */
	static private final class Emitter {
		final java.lang.classfile.CodeBuilder code;
		final ClassDesc thisClass;
		final Kind[] kinds;
		/** The class data index of the setter of each final field, else -1. */
		final int[] setters;
		/** The tag of each field, or null if the fields have no tags. */
		final int[] tags;
		/** If true, the class of each value is written before the value. */
		final boolean classes;
		/** If true, each field is written in a chunk. */
		final boolean chunked;
		/** Locals: the index of the current field for the error, the tag read, the chunk mark when writing or end when reading. */
		int index, tag, chunk;

		Emitter (java.lang.classfile.CodeBuilder code, ClassDesc thisClass, Kind[] kinds, int[] setters, int[] tags,
			boolean classes, boolean chunked) {
			this.code = code;
			this.thisClass = thisClass;
			this.kinds = kinds;
			this.setters = setters;
			this.tags = tags;
			this.classes = classes;
			this.chunked = chunked;
		}

		/** Returns an emitter for a nested block, eg of an if. */
		Emitter with (java.lang.classfile.CodeBuilder block) {
			Emitter emitter = new Emitter(block, thisClass, kinds, setters, tags, classes, chunked);
			emitter.index = index;
			emitter.tag = tag;
			emitter.chunk = chunk;
			return emitter;
		}

		/** The body of write for the fields from (inclusive) to (exclusive). */
		void write (int from, int to) {
			if (chunked) chunk = code.allocateLocal(LONG);
			index = code.allocateLocal(INT);
			// int index = from; try { index = i; <write field i> ... } catch (Throwable t) { throw writeError(t, fields[index],
			// output); }
			code.loadConstant(from).istore(index);
			code.trying(block -> {
				Emitter emitter = with(block);
				for (int i = from; i < to; i++) {
					if (i != from) block.loadConstant(i).istore(index);
					emitter.writeField(i);
				}
			}, catches -> catches.catchingAll(handler -> with(handler).throwError("writeError", MTD_writeError)));
			code.return_();
		}

		/** The body of read for the fields from (inclusive) to (exclusive). */
		void read (int from, int to) {
			if (tags != null) tag = code.allocateLocal(INT);
			if (chunked) chunk = code.allocateLocal(LONG);
			index = code.allocateLocal(INT);
			// int index = from; try { index = i; <read field i> ... } catch (Throwable t) { throw readError(t, fields[index],
			// input); }
			code.loadConstant(from).istore(index);
			code.trying(block -> {
				Emitter emitter = with(block);
				for (int i = from; i < to; i++) {
					if (i != from) block.loadConstant(i).istore(index);
					emitter.readField(i);
				}
			}, catches -> catches.catchingAll(handler -> with(handler).throwError("readError", MTD_readError)));
			code.return_();
		}

		/** output.writeVarInt(TAG, true); long mark = chunks.beginField(output); <write the value>; chunks.endField(output,
		 * mark); */
		void writeField (int i) {
			if (tags != null)
				code.aload(1).loadConstant(tags[i]).iconst_1().invokevirtual(CD_Output, "writeVarInt", MTD_writeVarInt).pop();
			if (chunked) code.aload(3).aload(1).invokeinterface(CD_ChunkedEncoding, "beginField", MTD_beginFieldWrite).lstore(chunk);
			writeValue(i);
			if (chunked) code.aload(3).aload(1).lload(chunk).invokeinterface(CD_ChunkedEncoding, "endField", MTD_endFieldWrite);
		}

		/** Writes the value of field i, with its class first if classes are written. */
		void writeValue (int i) {
			Kind kind = kinds[i];
			if (kind == Kind.object) {
				// ((ReflectField)fields[i]).writeValue(output, object, (Object)fi.get(object)), or writeValueWithClass
				field(i).checkcast(CD_ReflectField).aload(1).aload(2);
				value(i, CD_Object).invokevirtual(CD_ReflectField, classes ? "writeValueWithClass" : "writeValue", MTD_writeValue);
			} else if (classes && kind == Kind.string) {
				// GeneratedFields.writeStringWithClass(serializer.kryo, output, (String)fi.get(object))
				kryo().aload(1);
				value(i, CD_String).invokestatic(CD_GeneratedFields, "writeStringWithClass", MTD_writeStringWithClass);
			} else {
				// serializer.kryo.writeClass(output, Integer.class);
				if (classes) kryo().aload(1).ldc(kind.wrapper).invokevirtual(CD_Kryo, "writeClass", MTD_writeClass).pop();
				// output.writeX((X)fi.get(object)), or output.writeVarX((X)fi.get(object), false) if fields[i].varEncoding
				code.aload(1);
				value(i, kind.type);
				if (kind.varEncodable) {
					varEncoding(i).ifThenElse(IFNE, //
						block -> block.iconst_0().invokevirtual(CD_Output, kind.writeVar, kind.writeVarType).pop(), //
						block -> block.invokevirtual(CD_Output, kind.write, kind.writeType));
				} else
					code.invokevirtual(CD_Output, kind.write, kind.writeType);
			}
		}

		/** int tag = input.readVarInt(true); long end = chunks.beginField(input); if (tag == TAG) <read the value> else
		 * ((TaggedFieldSerializer)serializer).readTag(input, tag, object, chunked); chunks.endField(input, end); */
		void readField (int i) {
			if (tags != null) code.aload(1).iconst_1().invokevirtual(CD_Input, "readVarInt", MTD_readVarInt).istore(tag);
			if (chunked) code.aload(3).aload(1).invokeinterface(CD_ChunkedEncoding, "beginField", MTD_beginFieldRead).lstore(chunk);
			if (tags == null)
				readValue(i);
			else {
				code.iload(tag).loadConstant(tags[i]).ifThenElse(IF_ICMPEQ, //
					block -> with(block).readValue(i), //
					block -> block.aload(0).getfield(thisClass, "serializer", CD_FieldSerializer).checkcast(CD_TaggedFieldSerializer) //
						.aload(1).iload(tag).aload(2).loadConstant(chunked ? 1 : 0)
						.invokevirtual(CD_TaggedFieldSerializer, "readTag", MTD_readTag));
			}
			if (chunked) code.aload(3).aload(1).lload(chunk).invokeinterface(CD_ChunkedEncoding, "endField", MTD_endFieldRead);
		}

		/** Reads the value of field i and sets it. With classes, the class of a primitive value is read first: if it is null or the
		 * value is skipped, the field keeps its value. */
		void readValue (int i) {
			if (classes && kinds[i].primitive) {
				// if (GeneratedFields.readPrimitiveClass(serializer, input, fields[i], chunked)) <set>
				serializer().aload(1);
				field(i).loadConstant(chunked ? 1 : 0).invokestatic(CD_GeneratedFields, "readPrimitiveClass", MTD_readPrimitiveClass) //
					.ifThen(IFNE, block -> with(block).set(i));
				return;
			}
			set(i);
		}

		/** fi.set(object, <read>), or si.invokeExact(object, <read>) for a final field. */
		void set (int i) {
			Kind kind = kinds[i];
			if (setters[i] == -1)
				code.getstatic(thisClass, "f" + i, CD_VarHandle);
			else
				code.getstatic(thisClass, "s" + i, CD_MethodHandle);
			code.aload(2);
			read(i);
			if (setters[i] == -1)
				code.invokevirtual(CD_VarHandle, "set", MethodTypeDesc.of(CD_void, CD_Object, kind.type));
			else
				code.invokevirtual(CD_MethodHandle, "invokeExact", MethodTypeDesc.of(CD_void, CD_Object, kind.type));
		}

		/** The value of field i from the input, with its class first if classes are written. */
		void read (int i) {
			Kind kind = kinds[i];
			if (kind == Kind.object) {
				// ((ReflectField)fields[i]).readValue(input), or readValueWithClass(input, object, chunked)
				field(i).checkcast(CD_ReflectField).aload(1);
				if (classes)
					code.aload(2).loadConstant(chunked ? 1 : 0).invokevirtual(CD_ReflectField, "readValueWithClass",
						MTD_readValueWithClass);
				else
					code.invokevirtual(CD_ReflectField, "readValue", MTD_readValue);
			} else if (classes && kind == Kind.string) {
				// GeneratedFields.readStringWithClass(serializer, input, fields[i], object, chunked)
				serializer().aload(1);
				field(i).aload(2).loadConstant(chunked ? 1 : 0) //
					.invokestatic(CD_GeneratedFields, "readStringWithClass", MTD_readStringWithClass);
			} else if (kind.varEncodable) {
				// fields[i].varEncoding ? input.readVarX(false) : input.readX()
				varEncoding(i).ifThenElse(IFNE, //
					block -> block.aload(1).iconst_0().invokevirtual(CD_Input, kind.readVar, kind.readVarType), //
					block -> block.aload(1).invokevirtual(CD_Input, kind.read, kind.readType));
			} else
				code.aload(1).invokevirtual(CD_Input, kind.read, kind.readType); // input.readX()
		}

		/** throw GeneratedFields.<error>(t, fields[index], output or input), with the Throwable t on the stack. */
		void throwError (String error, MethodTypeDesc type) {
			code.aload(0).getfield(thisClass, "fields", CD_CachedFieldArray).iload(index).aaload().aload(1)
				.invokestatic(CD_GeneratedFields, error, type).athrow();
		}

		/** fields[i] */
		java.lang.classfile.CodeBuilder field (int i) {
			return code.aload(0).getfield(thisClass, "fields", CD_CachedFieldArray).loadConstant(i).aaload();
		}

		/** fields[i].varEncoding */
		java.lang.classfile.CodeBuilder varEncoding (int i) {
			return field(i).getfield(CD_CachedField, "varEncoding", CD_boolean);
		}

		/** serializer */
		java.lang.classfile.CodeBuilder serializer () {
			return code.aload(0).getfield(thisClass, "serializer", CD_FieldSerializer);
		}

		/** serializer.kryo */
		java.lang.classfile.CodeBuilder kryo () {
			return serializer().getfield(CD_FieldSerializer, "kryo", CD_Kryo);
		}

		/** (T)fi.get(object) */
		java.lang.classfile.CodeBuilder value (int i, ClassDesc type) {
			return code.getstatic(thisClass, "f" + i, CD_VarHandle).aload(2).invokevirtual(CD_VarHandle, "get",
				MethodTypeDesc.of(type, CD_Object));
		}
	}

	/** How a field is written and read. */
	private enum Kind {
		int_(CD_int, CD_Integer, "writeInt", "readInt"), long_(CD_long, CD_Long, "writeLong", "readLong"), //
		double_(CD_double, CD_Double, "writeDouble", "readDouble"), float_(CD_float, CD_Float, "writeFloat", "readFloat"), //
		boolean_(CD_boolean, CD_Boolean, "writeBoolean", "readBoolean"), short_(CD_short, CD_Short, "writeShort", "readShort"), //
		char_(CD_char, CD_Character, "writeChar", "readChar"), byte_(CD_byte, CD_Byte, "writeByte", "readByte"), //
		string(CD_String, CD_String, "writeString", "readString"), object(CD_Object, null, null, null);

		final ClassDesc type, wrapper;
		final boolean primitive;
		/** The Output and Input methods and their descriptors. */
		final String write, read;
		final MethodTypeDesc writeType, readType;
		/** True for int and long, which are written with variable length if {@link CachedField#varEncoding}, with these methods. */
		final boolean varEncodable;
		final String writeVar, readVar;
		final MethodTypeDesc writeVarType, readVarType;

		Kind (ClassDesc type, ClassDesc wrapper, String write, String read) {
			this.type = type;
			this.wrapper = wrapper;
			primitive = type.isPrimitive();
			this.write = write;
			this.read = read;
			// writeShort(int) takes its value as int.
			writeType = MethodTypeDesc.of(CD_void, type == CD_short ? CD_int : type);
			readType = MethodTypeDesc.of(type);
			varEncodable = type == CD_int || type == CD_long;
			writeVar = type == CD_int ? "writeVarInt" : "writeVarLong";
			readVar = type == CD_int ? "readVarInt" : "readVarLong";
			writeVarType = MethodTypeDesc.of(CD_int, type, CD_boolean);
			readVarType = MethodTypeDesc.of(type, CD_boolean);
		}
	}

	// The classes and methods the generated code uses.

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
		CD_Class, CD_int);
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
	static private final MethodTypeDesc MTD_readVarInt = MethodTypeDesc.of(CD_int, CD_boolean);
	static private final MethodTypeDesc MTD_readTag = MethodTypeDesc.of(CD_void, CD_Input, CD_int, CD_Object, CD_boolean);
	static private final MethodTypeDesc MTD_writeError = MethodTypeDesc.of(CD_KryoException, CD_Throwable, CD_CachedField,
		CD_Output);
	static private final MethodTypeDesc MTD_readError = MethodTypeDesc.of(CD_KryoException, CD_Throwable, CD_CachedField,
		CD_Input);

	// From java.lang.classfile.
	static private final int ACC_PUBLIC = 0x0001, ACC_PRIVATE = 0x0002, ACC_STATIC = 0x0008, ACC_FINAL = 0x0010,
		ACC_SUPER = 0x0020;
	static private final String INIT_NAME = "<init>", CLASS_INIT_NAME = "<clinit>";
	static private final java.lang.classfile.Opcode IFNE = java.lang.classfile.Opcode.IFNE,
		IF_ICMPEQ = java.lang.classfile.Opcode.IF_ICMPEQ;
	static private final java.lang.classfile.TypeKind INT = java.lang.classfile.TypeKind.INT,
		LONG = java.lang.classfile.TypeKind.LONG;
}
