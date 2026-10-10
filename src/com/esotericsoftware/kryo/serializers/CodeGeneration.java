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
import static com.esotericsoftware.kryo.util.Log.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.bytecode.Bytecode;
import com.esotericsoftware.kryo.bytecode.Bytecode.Code;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer.CachedField;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.VarHandle;
import java.lang.invoke.VarHandle.AccessMode;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

/** Generates a hidden class per serialized class that writes, reads and copies its fields with straight line code, see
 * {@link Bytecode}. Primitive and String fields are accessed with VarHandles that are constants of the hidden class (its class
 * data) and written directly to the {@link Output}, object fields are delegated to their {@link ReflectField}, which holds the
 * serializer, value class and generic type. Final fields are set with a MethodHandle, because VarHandles can't set them. The
 * hidden class depends only on the field names, kinds and encodings, so it is shared by all serializers and Kryo instances for a
 * class.
 * <p>
 * For example, for {@code class Nested { String name; Nested next; final int value; }} with FieldSerializer, the hidden class is
 * equivalent to:
 *
 * <pre>
 * final class Generated$Nested extends GeneratedFields {
 *    // The class data: the VarHandle of each field, then the call site invoker of each final field.
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
 *          s2.invokeExact(fields[2], object, fields[2].varEncoding ? input.readVarInt(false) : input.readInt());
 *       } catch (Throwable t) {
 *          throw GeneratedFields.readError(t, fields[index], input);
 *       }
 *    }
 *
 *    // write and read with a ChunkedEncoding parameter wrap each field in beginField and endField.
 *
 *    // For a record, which sets its fields with its canonical constructor, c, with the parameters in field order:
 *    public Object readRecord (Input input) {
 *       int index = 0;
 *       Object record;
 *       try {
 *          record = c.invokeExact(input.readString(), (index = 1, ((ReflectField)fields[1]).readValue(input)), ...);
 *       } catch (Throwable t) {
 *          throw GeneratedFields.readError(t, fields[index], input);
 *       }
 *       return record;
 *    }
 *    // copyRecord gets the values like copy.
 *
 *    public void copy (Object original, Object copy) {
 *       int index = 0;
 *       try {
 *          f0.set(copy, (String)f0.get(original));
 *          index = 1;
 *          f1.set(copy, serializer.kryo.copy(f1.get(original)));
 *          index = 2;
 *          s2.invokeExact(fields[2], copy, (int)f2.get(original));
 *       } catch (Throwable t) {
 *          throw GeneratedFields.copyError(t, fields[index], original);
 *       }
 *    }
 * }
 * </pre>
 *
 * With the classes written, like CompatibleFieldSerializer with unknown field data, the class is written before each value and
 * read with the helpers of {@link GeneratedFields}. With tags, like TaggedFieldSerializer, the tag is written before each field,
 * and read falls back to {@link TaggedFieldSerializer#readTag(Input, int, Object, boolean)} when the tag isn't the expected one.
 * Classes with more than {@link #batchSize} fields get a private method per batch.
 * <p>
 * The method handle that sets a final field is the invoker of a {@link MutableCallSite}. Its first call obtains the setter of the
 * field, because Java 26+ warns when that is done, so it only happens when a final field is set. If setting the field with
 * reflection is denied, the call site calls {@link FieldSerializer#setFinal(CachedField, Object, Object)} instead, which uses the
 * {@link FinalFieldSetter} of the cached field, or Unsafe for an Unsafe field.
 * <p>
 * Not supported, so the cached fields are used: records with more than {@link #batchSize} components, because their constructor
 * call can't be split, records with removed components, which the cached fields set to their defaults, and custom
 * {@link CachedField} implementations.
 * <p>
 * The class file is written with {@link Bytecode}: with the Class-File API on Java 24+, or with ASM, which is an optional
 * dependency, on older Java versions. */
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
	 * {@link GeneratedFields#generate(FieldSerializer, CachedField[], boolean, int[])}.
	 * @param writeClasses If true, the class of each value is written before the value, which is written without null marker, like
	 *           CompatibleFieldSerializer with unknown field data.
	 * @param tags If not null, the tag of each field is written before the field, like TaggedFieldSerializer. When a read tag is
	 *           not the expected one, the field is read with {@link TaggedFieldSerializer#readTag(Input, int, Object, boolean)}.
	 * @throws KryoException if the hidden class can't be defined. */
	static GeneratedFields generate (FieldSerializer serializer, CachedField[] fields, boolean writeClasses, int[] tags) {
		Kind[] kinds = kinds(serializer.type, fields);
		if (kinds == null) return null;
		Constructor recordConstructor = serializer.recordConstructor;
		if (recordConstructor != null && (fields.length > batchSize || fields.length != recordConstructor.getParameterCount())) {
			// The constructor call can't be split into batches, and the cached fields set the defaults of removed components.
			if (DEBUG) debug("kryo", "Code generation is not supported for a record with more than " + batchSize
				+ " components or removed components: " + className(serializer.type));
			return null;
		}
		StringBuilder signature = new StringBuilder(writeClasses ? "classes;" : "");
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField field = fields[i];
			signature.append(field.field.getDeclaringClass().getName()).append('.').append(field.field.getName()).append(':')
				.append(kinds[i]);
			if (tags != null) signature.append(':').append(tags[i]);
			signature.append(';');
		}

		Object constructor = constructors.get(serializer.type).computeIfAbsent(signature.toString(), key -> {
			try {
				return define(serializer.type, recordConstructor, fields, kinds, writeClasses, tags);
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

	/** The kinds of the fields for the generated code, or null if a field is not supported. */
	static private Kind[] kinds (Class type, CachedField[] fields) {
		Kind[] kinds = new Kind[fields.length];
		for (int i = 0, n = fields.length; i < n; i++) {
			kinds[i] = kind(fields[i]);
			if (kinds[i] == null) {
				if (DEBUG) debug("kryo",
					"Code generation is not supported for field: " + fields[i].name + " (" + className(type) + ")");
				return null;
			}
		}
		return kinds;
	}

	/** The kind of a field for the generated code, or null if the field is not supported. */
	static private Kind kind (CachedField field) {
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
	static private MethodHandle define (Class type, Constructor recordConstructor, CachedField[] fields, Kind[] kinds,
		boolean classes, int[] tags) {
		int n = fields.length;
		ArrayList<Object> classData = new ArrayList<>(n);
		String thisClass = name(type);
		byte[] bytes = write(Bytecode.create(thisClass, GeneratedFieldsName), thisClass, recordConstructor, fields, kinds, classes,
			tags, classData);
		try {
			Lookup hidden = MethodHandles.lookup().defineHiddenClassWithClassData(bytes, classData, true);
			if (TRACE) trace("kryo", "Generated code for the fields of: " + className(type) + " (" + n + " fields"
				+ (classes ? ", classes" : "") + (tags != null ? ", tags" : "")
				+ (n > batchSize ? ", " + (n + batchSize - 1) / batchSize + " batches" : "") + ")");
			return hidden.findConstructor(hidden.lookupClass(), constructorType)
				.asType(constructorType.changeReturnType(GeneratedFields.class));
		} catch (IllegalAccessException | NoSuchMethodException | RuntimeException ex) {
			throw new KryoException("Unable to define the generated fields for: " + className(type), ex);
		}
	}

	/** Returns the internal name of the hidden class for the type. */
	static private String name (Class type) {
		// The name without the package, not getSimpleName, which accesses the declaring class and can fail for another class
		// loader.
		String typeName = type.getName().substring(type.getName().lastIndexOf('.') + 1);
		return CodeGeneration.class.getPackageName().replace('.', '/') + "/Generated$" + typeName;
	}

	/** Returns the class file for the fields, written with the Class-File API or ASM. For tests that compare the writers.
	 * @param recordConstructor The canonical constructor if the type is a record, else null.
	 * @throws KryoException if code can't be generated for a field. */
	static byte[] classFile (boolean asm, Class type, Constructor recordConstructor, CachedField[] fields, boolean classes,
		int[] tags) {
		Kind[] kinds = kinds(type, fields);
		if (kinds == null) throw new KryoException("Code generation is not supported for a field of: " + className(type));
		String thisClass = name(type);
		Bytecode cb = asm ? Bytecode.asmWriter(thisClass, GeneratedFieldsName)
			: Bytecode.classFileWriter(thisClass, GeneratedFieldsName);
		return write(cb, thisClass, recordConstructor, fields, kinds, classes, tags, new ArrayList<>());
	}

	/** Writes the hidden class for the fields.
	 * @param recordConstructor The canonical constructor if the type is a record, else null.
	 * @param classData Receives the class data of the hidden class. */
	static private byte[] write (Bytecode cb, String thisClass, Constructor recordConstructor, CachedField[] fields, Kind[] kinds,
		boolean classes, int[] tags, ArrayList<Object> classData) {
		int n = fields.length;
		int[] setters = classData(fields, kinds, recordConstructor, classData);
		boolean record = recordConstructor != null;
		members(cb, thisClass, n, setters, record);
		constructor(cb, thisClass);
		// public void write (Output output, Object object) and write (Output output, Object object, ChunkedEncoding chunks)
		// public void read (Input input, Object object) and read (Input input, Object object, ChunkedEncoding chunks)
		for (boolean chunked : new boolean[] {false, true}) {
			String writeType = chunked ? descriptor(void.class, Output.class, Object.class, ChunkedEncoding.class)
				: descriptor(void.class, Output.class, Object.class);
			String readType = chunked ? descriptor(void.class, Input.class, Object.class, ChunkedEncoding.class)
				: descriptor(void.class, Input.class, Object.class);
			batches(cb, thisClass, "write", writeType, n, chunked,
				(code, from, to) -> new Emitter(code, thisClass, kinds, setters, tags, classes, chunked).write(from, to));
			batches(cb, thisClass, "read", readType, n, chunked,
				(code, from, to) -> new Emitter(code, thisClass, kinds, setters, tags, classes, chunked).read(from, to));
		}
		// public void copy (Object original, Object copy)
		batches(cb, thisClass, "copy", descriptor(void.class, Object.class, Object.class), n, false,
			(code, from, to) -> new Emitter(code, thisClass, kinds, setters, tags, classes, false).copy(from, to));
		if (record) {
			// public Object readRecord (Input input) and copyRecord (Object original)
			cb.method("readRecord", descriptor(Object.class, Input.class), ACC_PUBLIC, code -> {
				Emitter emitter = new Emitter(code, thisClass, kinds, setters, tags, classes, false);
				emitter.record(emitter::read, "readError", InputDesc);
			});
			cb.method("copyRecord", descriptor(Object.class, Object.class), ACC_PUBLIC, code -> {
				Emitter emitter = new Emitter(code, thisClass, kinds, setters, tags, classes, false);
				emitter.record(emitter::copyValue, "copyError", "Ljava/lang/Object;");
			});
		}
		return cb.bytes();
	}

	/** Collects the class data: the VarHandle of each field, then the call site invoker that sets each final field, which
	 * VarHandles can't set, or the canonical constructor of a record, which sets its fields.
	 * @return The class data index of the setter of each field, or -1. */
	static private int[] classData (CachedField[] fields, Kind[] kinds, Constructor recordConstructor,
		ArrayList<Object> classData) {
		int n = fields.length;
		for (int i = 0; i < n; i++)
			classData.add(VarHandleField.varHandle(fields[i].field));
		int[] setters = new int[n];
		for (int i = 0; i < n; i++) {
			setters[i] = -1;
			if (recordConstructor == null && !((VarHandle)classData.get(i)).isAccessModeSupported(AccessMode.SET)) {
				// The setter is obtained when the field is first set, until then the target of the call site is the resolver.
				MutableCallSite callSite = new MutableCallSite(
					MethodType.methodType(void.class, CachedField.class, Object.class, valueType(kinds[i], fields[i])));
				callSite.setTarget(MethodHandles.insertArguments(resolveSetter, 0, callSite).asType(callSite.type()));
				setters[i] = classData.size();
				classData.add(callSite.dynamicInvoker());
			}
		}
		if (recordConstructor != null) classData.add(recordConstructor(recordConstructor, fields, kinds));
		return setters;
	}

	/** The type of the values the generated code passes: String for a type variable resolved to String, else the field type. */
	static private Class valueType (Kind kind, CachedField field) {
		return kind == Kind.object ? Object.class : kind == Kind.string ? String.class : field.field.getType();
	}

	/** Returns the canonical constructor of a record as a method handle that takes the component values in field order, with the
	 * types the generated code passes, and returns Object. */
	static private MethodHandle recordConstructor (Constructor constructor, CachedField[] fields, Kind[] kinds) {
		Class type = constructor.getDeclaringClass();
		MethodHandle handle;
		try {
			handle = MethodHandles.privateLookupIn(type, MethodHandles.lookup()).unreflectConstructor(constructor);
		} catch (IllegalAccessException ex) {
			throw new KryoException("Unable to access the canonical constructor: " + className(type), ex);
		}
		int n = fields.length;
		Class[] fieldTypes = new Class[n], componentTypes = new Class[n];
		int[] reorder = new int[n]; // The field that provides each component.
		for (int i = 0; i < n; i++) {
			fieldTypes[i] = valueType(kinds[i], fields[i]);
			componentTypes[fields[i].index] = fieldTypes[i];
			reorder[fields[i].index] = i;
		}
		handle = handle.asType(MethodType.methodType(Object.class, componentTypes));
		return MethodHandles.permuteArguments(handle, MethodType.methodType(Object.class, fieldTypes), reorder);
	}

	static private final MethodHandle resolveSetter, setFinal;
	static {
		try {
			Lookup lookup = MethodHandles.lookup();
			resolveSetter = lookup.findStatic(CodeGeneration.class, "resolveSetter",
				MethodType.methodType(void.class, MutableCallSite.class, CachedField.class, Object.class, Object.class));
			setFinal = lookup.findStatic(FieldSerializer.class, "setFinal",
				MethodType.methodType(void.class, CachedField.class, Object.class, Object.class));
		} catch (IllegalAccessException | NoSuchMethodException ex) {
			throw new KryoException(ex);
		}
	}

	/** The initial target of the call site of a final field: obtains the setter of the field, which Java 26+ warns about, so it
	 * only happens when the field is first set. Makes the setter the target of the call site, or
	 * {@link FieldSerializer#setFinal(CachedField, Object, Object)} if setting the field with reflection is denied, then sets the
	 * field. */
	static private void resolveSetter (MutableCallSite callSite, CachedField field, Object object, Object value)
		throws Throwable {
		MethodHandle setter = setFinal;
		if (!FinalFieldSetter.force) {
			try {
				setter = MethodHandles.dropArguments(MethodHandles.lookup().unreflectSetter(field.field), 0, CachedField.class);
			} catch (IllegalAccessException denied) {
			}
		}
		callSite.setTarget(setter.asType(callSite.type()));
		callSite.getTarget().invoke(field, object, value);
	}

	/** Emits the fields: the VarHandle fi and setter si of each field and the constructor c of a record, initialized from the
	 * class data, and the serializer and fields of the serializer instance. */
	static private void members (Bytecode cb, String thisClass, int n, int[] setters, boolean record) {
		// static final VarHandle f0; static final MethodHandle s0; ... static final MethodHandle c; for a record
		for (int i = 0; i < n; i++) {
			cb.field("f" + i, VarHandleDesc, ACC_PRIVATE | ACC_STATIC | ACC_FINAL);
			if (setters[i] != -1) cb.field("s" + i, MethodHandleDesc, ACC_PRIVATE | ACC_STATIC | ACC_FINAL);
		}
		if (record) cb.field("c", MethodHandleDesc, ACC_PRIVATE | ACC_STATIC | ACC_FINAL);
		// final FieldSerializer serializer; final CachedField[] fields;
		cb.field("serializer", FieldSerializerDesc, ACC_PRIVATE | ACC_FINAL);
		cb.field("fields", CachedFieldArrayDesc, ACC_PRIVATE | ACC_FINAL);
		// static { f0 = (VarHandle)MethodHandles.classDataAt(MethodHandles.lookup(), "_", VarHandle.class, 0); ...
		// }
		cb.method("<clinit>", "()V", ACC_STATIC, code -> {
			for (int i = 0; i < n; i++) {
				loadClassData(code, i, VarHandleDesc, VarHandleName);
				code.putstatic(thisClass, "f" + i, VarHandleDesc);
				if (setters[i] != -1) {
					loadClassData(code, setters[i], MethodHandleDesc, MethodHandleName);
					code.putstatic(thisClass, "s" + i, MethodHandleDesc);
				}
			}
			if (record) {
				loadClassData(code, n, MethodHandleDesc, MethodHandleName);
				code.putstatic(thisClass, "c", MethodHandleDesc);
			}
			code.vreturn();
		});
	}

	/** Emits: (T)MethodHandles.classDataAt(MethodHandles.lookup(), "_", T.class, index) */
	static private void loadClassData (Code code, int index, String descriptor, String name) {
		code.invokestatic(MethodHandlesName, "lookup", "()" + LookupDesc);
		code.ldc("_");
		code.ldcClass(name);
		code.iconst(index);
		code.invokestatic(MethodHandlesName, "classDataAt",
			"(" + LookupDesc + "Ljava/lang/String;Ljava/lang/Class;I)Ljava/lang/Object;");
		code.checkcast(name);
	}

	/** Emits: public Generated$Type (FieldSerializer serializer, CachedField[] fields) */
	static private void constructor (Bytecode cb, String thisClass) {
		cb.method("<init>", "(" + FieldSerializerDesc + CachedFieldArrayDesc + ")V", ACC_PUBLIC, code -> {
			code.aload(0);
			code.invokespecial(GeneratedFieldsName, "<init>", "()V");
			code.aload(0);
			code.aload(1);
			code.putfield(thisClass, "serializer", FieldSerializerDesc);
			code.aload(0);
			code.aload(2);
			code.putfield(thisClass, "fields", CachedFieldArrayDesc);
			code.vreturn();
		});
	}

	/** Emits the fields of a method in batches. The JIT doesn't compile methods above 8000 bytes, so a class with many fields gets
	 * a private method per batch of fields, which the public method calls. */
	static private void batches (Bytecode cb, String thisClass, String name, String type, int n, boolean chunked, Batch batch) {
		if (n <= batchSize) {
			cb.method(name, type, ACC_PUBLIC, code -> batch.emit(code, 0, n));
			return;
		}
		cb.method(name, type, ACC_PUBLIC, code -> {
			for (int from = 0; from < n; from += batchSize) {
				code.aload(0);
				code.aload(1);
				code.aload(2);
				if (chunked) code.aload(3);
				code.invokevirtual(thisClass, name + from, type);
			}
			code.vreturn();
		});
		for (int from = 0; from < n; from += batchSize) {
			int start = from, end = Math.min(from + batchSize, n);
			cb.method(name + from, type, ACC_PRIVATE, code -> batch.emit(code, start, end));
		}
	}

	private interface Batch {
		void emit (Code code, int from, int to);
	}

	/** Emits the body of a write, read or copy method of the hidden class, see the class javadoc. The locals are 0 this, 1 the
	 * output, input or original, 2 the object or copy and 3 the chunks if chunked. Each method emits the statement or expression
	 * in its comment. */
	static private final class Emitter {
		final Code code;
		final String thisClass;
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

		Emitter (Code code, String thisClass, Kind[] kinds, int[] setters, int[] tags, boolean classes, boolean chunked) {
			this.code = code;
			this.thisClass = thisClass;
			this.kinds = kinds;
			this.setters = setters;
			this.tags = tags;
			this.classes = classes;
			this.chunked = chunked;
		}

		/** The body of write for the fields from (inclusive) to (exclusive). */
		void write (int from, int to) {
			int local = chunked ? 4 : 3; // After the parameters.
			if (chunked) {
				chunk = local;
				local += 2;
			}
			index = local;
			fields(from, to, this::writeField, "writeError", OutputDesc);
		}

		/** The body of read for the fields from (inclusive) to (exclusive). */
		void read (int from, int to) {
			int local = chunked ? 4 : 3; // After the parameters.
			if (tags != null) tag = local++;
			if (chunked) {
				chunk = local;
				local += 2;
			}
			index = local;
			fields(from, to, this::readField, "readError", InputDesc);
		}

		/** The body of copy for the fields from (inclusive) to (exclusive). */
		void copy (int from, int to) {
			index = 3; // After the parameters.
			fields(from, to, this::copyField, "copyError", "Ljava/lang/Object;");
		}

		/** The body of readRecord or copyRecord: int index = 0; Object record; try { record = c.invokeExact(<value 0>, index = 1,
		 * <value 1>, ...); } catch (Throwable t) { throw GeneratedFields.<error>(t, fields[index], <local 1>); } return record; */
		void record (IntConsumer value, String error, String local1Desc) {
			int n = kinds.length;
			if (n == 0) { // No fields to name in an error. return c.invokeExact();
				code.getstatic(thisClass, "c", MethodHandleDesc);
				code.invokevirtual(MethodHandleName, "invokeExact", "()Ljava/lang/Object;");
				code.areturn();
				return;
			}
			index = 2; // After the parameter.
			int record = 3;
			code.iconst(0);
			code.istore(index);
			code.trying( () -> {
				code.getstatic(thisClass, "c", MethodHandleDesc);
				StringBuilder type = new StringBuilder("(");
				for (int i = 0; i < n; i++) {
					if (i != 0) {
						code.iconst(i);
						code.istore(index);
					}
					value.accept(i);
					type.append(kinds[i].type);
				}
				code.invokevirtual(MethodHandleName, "invokeExact", type + ")Ljava/lang/Object;");
				code.astore(record);
			}, () -> throwError(error, "(Ljava/lang/Throwable;" + CachedFieldDesc + local1Desc + ")" + KryoExceptionDesc));
			code.aload(record);
			code.areturn();
		}

		/** int index = from; try { index = i; <field i> ... } catch (Throwable t) { throw GeneratedFields.<error>(t, fields[index],
		 * <local 1>); } */
		void fields (int from, int to, IntConsumer field, String error, String local1Desc) {
			if (from == to) { // No fields. An empty try block is not valid.
				code.vreturn();
				return;
			}
			code.iconst(from);
			code.istore(index);
			code.trying( () -> {
				for (int i = from; i < to; i++) {
					if (i != from) {
						code.iconst(i);
						code.istore(index);
					}
					field.accept(i);
				}
			}, () -> throwError(error, "(Ljava/lang/Throwable;" + CachedFieldDesc + local1Desc + ")" + KryoExceptionDesc));
			code.vreturn();
		}

		/** output.writeVarInt(TAG, true); long mark = chunks.beginField(output); <write the value>; chunks.endField(output,
		 * mark); */
		void writeField (int i) {
			if (tags != null) {
				code.aload(1);
				code.iconst(tags[i]);
				code.iconst(1);
				code.invokevirtual(OutputName, "writeVarInt", "(IZ)I");
				code.pop();
			}
			if (chunked) {
				code.aload(3);
				code.aload(1);
				code.invokeinterface(ChunkedEncodingName, "beginField", "(" + OutputDesc + ")J");
				code.lstore(chunk);
			}
			writeValue(i);
			if (chunked) {
				code.aload(3);
				code.aload(1);
				code.lload(chunk);
				code.invokeinterface(ChunkedEncodingName, "endField", "(" + OutputDesc + "J)V");
			}
		}

		/** Writes the value of field i, with its class first if classes are written. */
		void writeValue (int i) {
			Kind kind = kinds[i];
			if (kind == Kind.object) {
				// ((ReflectField)fields[i]).writeValue(output, object, (Object)fi.get(object)), or writeValueWithClass
				field(i);
				code.checkcast(ReflectFieldName);
				code.aload(1);
				code.aload(2);
				value(i, "Ljava/lang/Object;");
				code.invokevirtual(ReflectFieldName, classes ? "writeValueWithClass" : "writeValue",
					"(" + OutputDesc + "Ljava/lang/Object;Ljava/lang/Object;)V");
			} else if (classes && kind == Kind.string) {
				// GeneratedFields.writeStringWithClass(serializer.kryo, output, (String)fi.get(object))
				kryo();
				code.aload(1);
				value(i, "Ljava/lang/String;");
				code.invokestatic(GeneratedFieldsName, "writeStringWithClass", "(" + KryoDesc + OutputDesc + "Ljava/lang/String;)V");
			} else {
				// serializer.kryo.writeClass(output, Integer.class);
				if (classes) {
					kryo();
					code.aload(1);
					code.ldcClass(kind.wrapper);
					code.invokevirtual(KryoName, "writeClass", "(" + OutputDesc + "Ljava/lang/Class;)" + RegistrationDesc);
					code.pop();
				}
				// output.writeX((X)fi.get(object)), or output.writeVarX((X)fi.get(object), false) if fields[i].varEncoding
				code.aload(1);
				value(i, kind.type);
				if (kind.varEncodable) {
					varEncoding(i);
					code.ifThenElse(IFNE, () -> {
						code.iconst(0);
						code.invokevirtual(OutputName, kind.writeVar, kind.writeVarType);
						code.pop();
					}, () -> code.invokevirtual(OutputName, kind.write, kind.writeType));
				} else
					code.invokevirtual(OutputName, kind.write, kind.writeType);
			}
		}

		/** int tag = input.readVarInt(true); long end = chunks.beginField(input); if (tag == TAG) <read the value> else
		 * ((TaggedFieldSerializer)serializer).readTag(input, tag, object, chunked); chunks.endField(input, end); */
		void readField (int i) {
			if (tags != null) {
				code.aload(1);
				code.iconst(1);
				code.invokevirtual(InputName, "readVarInt", "(Z)I");
				code.istore(tag);
			}
			if (chunked) {
				code.aload(3);
				code.aload(1);
				code.invokeinterface(ChunkedEncodingName, "beginField", "(" + InputDesc + ")J");
				code.lstore(chunk);
			}
			if (tags == null)
				readValue(i);
			else {
				code.iload(tag);
				code.iconst(tags[i]);
				code.ifThenElse(IF_ICMPEQ, () -> readValue(i), () -> {
					code.aload(0);
					code.getfield(thisClass, "serializer", FieldSerializerDesc);
					code.checkcast(TaggedFieldSerializerName);
					code.aload(1);
					code.iload(tag);
					code.aload(2);
					code.iconst(chunked ? 1 : 0);
					code.invokevirtual(TaggedFieldSerializerName, "readTag", "(" + InputDesc + "ILjava/lang/Object;Z)V");
				});
			}
			if (chunked) {
				code.aload(3);
				code.aload(1);
				code.lload(chunk);
				code.invokeinterface(ChunkedEncodingName, "endField", "(" + InputDesc + "J)V");
			}
		}

		/** Reads the value of field i and sets it. With classes, the class of a primitive value is read first: if it is null or the
		 * value is skipped, the field keeps its value. */
		void readValue (int i) {
			if (classes && kinds[i].primitive) {
				// if (GeneratedFields.readPrimitiveClass(serializer, input, fields[i], chunked)) <set>
				serializer();
				code.aload(1);
				field(i);
				code.iconst(chunked ? 1 : 0);
				code.invokestatic(GeneratedFieldsName, "readPrimitiveClass",
					"(" + FieldSerializerDesc + InputDesc + CachedFieldDesc + "Z)Z");
				code.ifThen(IFNE, () -> set(i, () -> read(i)));
				return;
			}
			set(i, () -> read(i));
		}

		/** <set field i of the copy to> <copy value i> */
		void copyField (int i) {
			set(i, () -> copyValue(i));
		}

		/** (T)fi.get(original), or serializer.kryo.copy(fi.get(original)) for an object field. */
		void copyValue (int i) {
			Kind kind = kinds[i];
			if (kind != Kind.object)
				value(i, kind.type, 1);
			else {
				kryo();
				value(i, kind.type, 1);
				code.invokevirtual(KryoName, "copy", "(Ljava/lang/Object;)Ljava/lang/Object;");
			}
		}

		/** fi.set(object, <value>), or si.invokeExact(fields[i], object, <value>) for a final field. */
		void set (int i, Runnable value) {
			Kind kind = kinds[i];
			if (setters[i] == -1) {
				code.getstatic(thisClass, "f" + i, VarHandleDesc);
				code.aload(2);
				value.run();
				code.invokevirtual(VarHandleName, "set", "(Ljava/lang/Object;" + kind.type + ")V");
			} else {
				code.getstatic(thisClass, "s" + i, MethodHandleDesc);
				field(i);
				code.aload(2);
				value.run();
				code.invokevirtual(MethodHandleName, "invokeExact", "(" + CachedFieldDesc + "Ljava/lang/Object;" + kind.type + ")V");
			}
		}

		/** The value of field i from the input, with its class first if classes are written. */
		void read (int i) {
			Kind kind = kinds[i];
			if (kind == Kind.object) {
				// ((ReflectField)fields[i]).readValue(input), or readValueWithClass(input, object, chunked)
				field(i);
				code.checkcast(ReflectFieldName);
				code.aload(1);
				if (classes) {
					code.aload(2);
					code.iconst(chunked ? 1 : 0);
					code.invokevirtual(ReflectFieldName, "readValueWithClass",
						"(" + InputDesc + "Ljava/lang/Object;Z)Ljava/lang/Object;");
				} else
					code.invokevirtual(ReflectFieldName, "readValue", "(" + InputDesc + ")Ljava/lang/Object;");
			} else if (classes && kind == Kind.string) {
				// GeneratedFields.readStringWithClass(serializer, input, fields[i], object, chunked)
				serializer();
				code.aload(1);
				field(i);
				code.aload(2);
				code.iconst(chunked ? 1 : 0);
				code.invokestatic(GeneratedFieldsName, "readStringWithClass",
					"(" + FieldSerializerDesc + InputDesc + CachedFieldDesc + "Ljava/lang/Object;Z)Ljava/lang/String;");
			} else if (kind.varEncodable) {
				// fields[i].varEncoding ? input.readVarX(false) : input.readX()
				varEncoding(i);
				code.ifThenElse(IFNE, () -> {
					code.aload(1);
					code.iconst(0);
					code.invokevirtual(InputName, kind.readVar, kind.readVarType);
				}, () -> {
					code.aload(1);
					code.invokevirtual(InputName, kind.read, kind.readType);
				});
			} else {
				code.aload(1);
				code.invokevirtual(InputName, kind.read, kind.readType); // input.readX()
			}
		}

		/** throw GeneratedFields.<error>(t, fields[index], output or input), with the Throwable t on the stack. */
		void throwError (String error, String type) {
			code.aload(0);
			code.getfield(thisClass, "fields", CachedFieldArrayDesc);
			code.iload(index);
			code.aaload();
			code.aload(1);
			code.invokestatic(GeneratedFieldsName, error, type);
			code.athrow();
		}

		/** fields[i] */
		void field (int i) {
			code.aload(0);
			code.getfield(thisClass, "fields", CachedFieldArrayDesc);
			code.iconst(i);
			code.aaload();
		}

		/** fields[i].varEncoding */
		void varEncoding (int i) {
			field(i);
			code.getfield(CachedFieldName, "varEncoding", "Z");
		}

		/** serializer */
		void serializer () {
			code.aload(0);
			code.getfield(thisClass, "serializer", FieldSerializerDesc);
		}

		/** serializer.kryo */
		void kryo () {
			serializer();
			code.getfield(FieldSerializerName, "kryo", KryoDesc);
		}

		/** (T)fi.get(object) */
		void value (int i, String type) {
			value(i, type, 2);
		}

		/** (T)fi.get(<local>) */
		void value (int i, String type, int local) {
			code.getstatic(thisClass, "f" + i, VarHandleDesc);
			code.aload(local);
			code.invokevirtual(VarHandleName, "get", "(Ljava/lang/Object;)" + type);
		}
	}

	/** How a field is written and read. */
	private enum Kind {
		int_("I", "java/lang/Integer", "writeInt", "readInt"), long_("J", "java/lang/Long", "writeLong", "readLong"), //
		double_("D", "java/lang/Double", "writeDouble", "readDouble"), float_("F", "java/lang/Float", "writeFloat", "readFloat"), //
		boolean_("Z", "java/lang/Boolean", "writeBoolean", "readBoolean"), //
		short_("S", "java/lang/Short", "writeShort", "readShort"), char_("C", "java/lang/Character", "writeChar", "readChar"), //
		byte_("B", "java/lang/Byte", "writeByte", "readByte"), //
		string("Ljava/lang/String;", "java/lang/String", "writeString", "readString"), object("Ljava/lang/Object;", null, null,
			null);

		/** The descriptor of the type and the internal name of its wrapper class. */
		final String type, wrapper;
		final boolean primitive;
		/** The Output and Input methods and their descriptors. */
		final String write, read, writeType, readType;
		/** True for int and long, which are written with variable length if {@link CachedField#varEncoding}, with these methods. */
		final boolean varEncodable;
		final String writeVar, readVar, writeVarType, readVarType;

		Kind (String type, String wrapper, String write, String read) {
			this.type = type;
			this.wrapper = wrapper;
			primitive = type.length() == 1;
			this.write = write;
			this.read = read;
			// writeShort(int) takes its value as int.
			writeType = "(" + (type.equals("S") ? "I" : type) + ")V";
			readType = "()" + type;
			varEncodable = type.equals("I") || type.equals("J");
			writeVar = type.equals("I") ? "writeVarInt" : "writeVarLong";
			readVar = type.equals("I") ? "readVarInt" : "readVarLong";
			writeVarType = "(" + type + "Z)I";
			readVarType = "(Z)" + type;
		}
	}

	// The classes the generated code uses: internal names (Name) and descriptors.

	static private final String GeneratedFieldsName = Bytecode.name(GeneratedFields.class);
	static private final String ReflectFieldName = Bytecode.name(ReflectField.class);
	static private final String CachedFieldName = Bytecode.name(CachedField.class);
	static private final String CachedFieldDesc = Bytecode.descriptor(CachedField.class);
	static private final String CachedFieldArrayDesc = Bytecode.descriptor(CachedField[].class);
	static private final String FieldSerializerName = Bytecode.name(FieldSerializer.class);
	static private final String FieldSerializerDesc = Bytecode.descriptor(FieldSerializer.class);
	static private final String TaggedFieldSerializerName = Bytecode.name(TaggedFieldSerializer.class);
	static private final String ChunkedEncodingName = Bytecode.name(ChunkedEncoding.class);
	static private final String KryoName = Bytecode.name(Kryo.class);
	static private final String KryoDesc = Bytecode.descriptor(Kryo.class);
	static private final String RegistrationDesc = Bytecode.descriptor(Registration.class);
	static private final String KryoExceptionDesc = Bytecode.descriptor(KryoException.class);
	static private final String OutputName = Bytecode.name(Output.class);
	static private final String OutputDesc = Bytecode.descriptor(Output.class);
	static private final String InputName = Bytecode.name(Input.class);
	static private final String InputDesc = Bytecode.descriptor(Input.class);
	static private final String VarHandleName = Bytecode.name(VarHandle.class);
	static private final String VarHandleDesc = Bytecode.descriptor(VarHandle.class);
	static private final String MethodHandleName = Bytecode.name(MethodHandle.class);
	static private final String MethodHandleDesc = Bytecode.descriptor(MethodHandle.class);
	static private final String MethodHandlesName = Bytecode.name(MethodHandles.class);
	static private final String LookupDesc = Bytecode.descriptor(Lookup.class);

	static private final int ACC_PUBLIC = Bytecode.ACC_PUBLIC, ACC_PRIVATE = Bytecode.ACC_PRIVATE,
		ACC_STATIC = Bytecode.ACC_STATIC, ACC_FINAL = Bytecode.ACC_FINAL;
	static private final int IFNE = Bytecode.IFNE, IF_ICMPEQ = Bytecode.IF_ICMPEQ;

	static private String descriptor (Class returnType, Class... parameterTypes) {
		return Bytecode.descriptor(returnType, parameterTypes);
	}
}
