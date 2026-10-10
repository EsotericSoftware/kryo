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

package com.esotericsoftware.kryo.bytecode;

import static com.esotericsoftware.kryo.util.Util.*;

import com.esotericsoftware.kryo.KryoException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;

/** Writes the class file of a generated class, with the subset of bytecode that the code generation of the field serializers
 * emits. Implemented with the Class-File API on Java 24+ ({@link ClassFileWriter}) and with ASM ({@link AsmWriter}), which is an
 * optional dependency, on older Java versions.
 * <p>
 * Internal, used by {@code com.esotericsoftware.kryo.serializers.CodeGeneration}. Public only because it is in its own package,
 * which keeps the dependency on ASM out of the serializers. */
public abstract class Bytecode {
	static public final int ACC_PUBLIC = 0x0001, ACC_PRIVATE = 0x0002, ACC_STATIC = 0x0008, ACC_FINAL = 0x0010, ACC_SUPER = 0x0020;
	/** The condition opcodes of {@link Code#ifThen(int, Runnable)}: the top of the stack is not 0, the top two ints are equal. */
	static public final int IFNE = 154, IF_ICMPEQ = 159;

	/** True if the Class-File API is used: Java 24+, unless the system property "kryo.codeGeneration.backend" is "asm". */
	static public final boolean classFileApi = Runtime.version().feature() >= 24
		&& !"asm".equals(System.getProperty("kryo.codeGeneration.backend"));
	/** True if ASM is on the classpath. */
	static public final boolean asm;
	static {
		boolean available = false;
		try {
			Class.forName("org.objectweb.asm.ClassWriter", false, Bytecode.class.getClassLoader());
			available = true;
		} catch (Throwable ignored) {
		}
		asm = available;
	}

	/** Returns a writer for a final class with the internal name, which extends the super class. Both writers produce the same
	 * class file, except for the order of the constant pool.
	 * @throws KryoException if no implementation is available. */
	static public Bytecode create (String name, String superName) {
		if (classFileApi) return classFileWriter(name, superName);
		if (asm) return asmWriter(name, superName);
		throw new KryoException("Code generation needs Java 24+ or ASM on the classpath (org.ow2.asm:asm).");
	}

	/** Returns the writer that uses the Class-File API, which needs Java 24+. */
	static public Bytecode classFileWriter (String name, String superName) {
		try {
			return (Bytecode)ClassFileWriterFactory.create.invokeExact(name, superName);
		} catch (Throwable t) {
			throw new KryoException("Unable to create the Class-File API writer.", t);
		}
	}

	/** Returns the writer that uses ASM, which needs org.ow2.asm:asm on the classpath. */
	static public Bytecode asmWriter (String name, String superName) {
		return new AsmWriter(name, superName);
	}

	/** Loaded on first use, which only happens on Java 24+, because ClassFileWriter uses the Class-File API. */
	static private final class ClassFileWriterFactory {
		static final MethodHandle create;
		static {
			try {
				Class type = Class.forName(Bytecode.class.getPackageName() + ".ClassFileWriter");
				create = MethodHandles.lookup().findConstructor(type, MethodType.methodType(void.class, String.class, String.class))
					.asType(MethodType.methodType(Bytecode.class, String.class, String.class));
			} catch (ReflectiveOperationException ex) {
				throw new ExceptionInInitializerError(ex);
			}
		}
	}

	/** Adds a field. */
	abstract public void field (String name, String descriptor, int flags);

	/** Adds a method, with the body emitted by the consumer. */
	abstract public void method (String name, String descriptor, int flags, Consumer<Code> body);

	/** Returns the class file. */
	abstract public byte[] bytes ();

	/** Returns the internal name of a class, eg java/lang/Object. */
	static public String name (Class type) {
		return type.getName().replace('.', '/');
	}

	/** Returns the descriptor of a type, eg Ljava/lang/Object; or I. */
	static public String descriptor (Class type) {
		if (type.isArray()) return "[" + descriptor(type.getComponentType());
		if (!type.isPrimitive()) return "L" + name(type) + ";";
		if (type == int.class) return "I";
		if (type == long.class) return "J";
		if (type == double.class) return "D";
		if (type == float.class) return "F";
		if (type == boolean.class) return "Z";
		if (type == short.class) return "S";
		if (type == char.class) return "C";
		if (type == byte.class) return "B";
		return "V";
	}

	/** Returns the descriptor of a method, eg (Ljava/lang/Object;I)V. */
	static public String descriptor (Class returnType, Class... parameterTypes) {
		StringBuilder buffer = new StringBuilder("(");
		for (Class type : parameterTypes)
			buffer.append(descriptor(type));
		return buffer.append(')').append(descriptor(returnType)).toString();
	}

	/** Emits the bytecode of a method. */
	abstract static public class Code {
		abstract public void aload (int local);

		abstract public void iload (int local);

		abstract public void lload (int local);

		abstract public void istore (int local);

		abstract public void lstore (int local);

		/** Loads an int constant. */
		abstract public void iconst (int value);

		/** Loads a String constant. */
		abstract public void ldc (String value);

		/** Loads a Class constant. */
		abstract public void ldcClass (String name);

		abstract public void getstatic (String owner, String name, String descriptor);

		abstract public void putstatic (String owner, String name, String descriptor);

		abstract public void getfield (String owner, String name, String descriptor);

		abstract public void putfield (String owner, String name, String descriptor);

		abstract public void invokevirtual (String owner, String name, String descriptor);

		abstract public void invokestatic (String owner, String name, String descriptor);

		abstract public void invokeinterface (String owner, String name, String descriptor);

		abstract public void invokespecial (String owner, String name, String descriptor);

		abstract public void checkcast (String name);

		abstract public void aaload ();

		abstract public void pop ();

		abstract public void vreturn ();

		abstract public void athrow ();

		/** if (<condition>) block, see {@link Bytecode#IFNE}. */
		abstract public void ifThen (int condition, Runnable block);

		/** if (<condition>) thenBlock else elseBlock */
		abstract public void ifThenElse (int condition, Runnable thenBlock, Runnable elseBlock);

		/** try { body } catch (Throwable t) { handler }, with the Throwable on the stack in the handler, which must throw. */
		abstract public void trying (Runnable body, Runnable handler);
	}
}
