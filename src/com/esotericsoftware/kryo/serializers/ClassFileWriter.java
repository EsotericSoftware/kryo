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

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.function.Consumer;

/** {@link Bytecode} with the Class-File API, Java 24+. This is the only class that uses an API newer than Java 17: it is compiled
 * with -source 17 on JDK 24+ and only loaded on Java 24+. The Class-File API is referenced with qualified names and the IDE
 * inspection for the language level is suppressed, see "Building from source" in README.md. */
@SuppressWarnings("Since15")
final class ClassFileWriter extends Bytecode {
	private final ClassDesc thisClass, superClass;
	private final ArrayList<Consumer<java.lang.classfile.ClassBuilder>> members = new ArrayList<>();

	ClassFileWriter (String name, String superName) {
		thisClass = ClassDesc.ofInternalName(name);
		superClass = ClassDesc.ofInternalName(superName);
	}

	public void field (String name, String descriptor, int flags) {
		members.add(cb -> cb.withField(name, ClassDesc.ofDescriptor(descriptor), flags));
	}

	public void method (String name, String descriptor, int flags, Consumer<Code> body) {
		members.add(cb -> cb.withMethodBody(name, MethodTypeDesc.ofDescriptor(descriptor), flags,
			code -> body.accept(new ClassFileCode(code))));
	}

	public byte[] bytes () {
		return java.lang.classfile.ClassFile.of().build(thisClass, cb -> {
			// Class file version 61 (Java 17) like AsmWriter, so both writers produce the same class file.
			cb.withVersion(61, 0).withFlags(ACC_FINAL | ACC_SUPER).withSuperclass(superClass);
			for (Consumer<java.lang.classfile.ClassBuilder> member : members)
				member.accept(cb);
		});
	}

	static private final class ClassFileCode extends Code {
		/** The builder of the current block: the method, or a nested block of an if or try. */
		private java.lang.classfile.CodeBuilder code;

		ClassFileCode (java.lang.classfile.CodeBuilder code) {
			this.code = code;
		}

		/** Runs the block with its builder, then restores the enclosing one. */
		private void block (java.lang.classfile.CodeBuilder block, Runnable runnable) {
			java.lang.classfile.CodeBuilder enclosing = code;
			code = block;
			try {
				runnable.run();
			} finally {
				code = enclosing;
			}
		}

		public void aload (int local) {
			code.aload(local);
		}

		public void iload (int local) {
			code.iload(local);
		}

		public void lload (int local) {
			code.lload(local);
		}

		public void istore (int local) {
			code.istore(local);
		}

		public void astore (int local) {
			code.astore(local);
		}

		public void lstore (int local) {
			code.lstore(local);
		}

		public void iconst (int value) {
			code.loadConstant(value);
		}

		public void ldc (String value) {
			code.ldc(value);
		}

		public void ldcClass (String name) {
			code.ldc(ClassDesc.ofInternalName(name));
		}

		public void getstatic (String owner, String name, String descriptor) {
			code.getstatic(ClassDesc.ofInternalName(owner), name, ClassDesc.ofDescriptor(descriptor));
		}

		public void putstatic (String owner, String name, String descriptor) {
			code.putstatic(ClassDesc.ofInternalName(owner), name, ClassDesc.ofDescriptor(descriptor));
		}

		public void getfield (String owner, String name, String descriptor) {
			code.getfield(ClassDesc.ofInternalName(owner), name, ClassDesc.ofDescriptor(descriptor));
		}

		public void putfield (String owner, String name, String descriptor) {
			code.putfield(ClassDesc.ofInternalName(owner), name, ClassDesc.ofDescriptor(descriptor));
		}

		public void invokevirtual (String owner, String name, String descriptor) {
			code.invokevirtual(ClassDesc.ofInternalName(owner), name, MethodTypeDesc.ofDescriptor(descriptor));
		}

		public void invokestatic (String owner, String name, String descriptor) {
			code.invokestatic(ClassDesc.ofInternalName(owner), name, MethodTypeDesc.ofDescriptor(descriptor));
		}

		public void invokeinterface (String owner, String name, String descriptor) {
			code.invokeinterface(ClassDesc.ofInternalName(owner), name, MethodTypeDesc.ofDescriptor(descriptor));
		}

		public void invokespecial (String owner, String name, String descriptor) {
			code.invokespecial(ClassDesc.ofInternalName(owner), name, MethodTypeDesc.ofDescriptor(descriptor));
		}

		public void checkcast (String name) {
			code.checkcast(ClassDesc.ofInternalName(name));
		}

		public void aaload () {
			code.aaload();
		}

		public void pop () {
			code.pop();
		}

		public void vreturn () {
			code.return_();
		}

		public void areturn () {
			code.areturn();
		}

		public void athrow () {
			code.athrow();
		}

		public void ifThen (int condition, Runnable thenBlock) {
			code.ifThen(opcode(condition), block -> block(block, thenBlock));
		}

		public void ifThenElse (int condition, Runnable thenBlock, Runnable elseBlock) {
			code.ifThenElse(opcode(condition), block -> block(block, thenBlock), block -> block(block, elseBlock));
		}

		public void trying (Runnable body, Runnable handler) {
			code.trying(block -> block(block, body), catches -> catches.catchingAll(block -> block(block, handler)));
		}

		static private java.lang.classfile.Opcode opcode (int condition) {
			return condition == IFNE ? java.lang.classfile.Opcode.IFNE : java.lang.classfile.Opcode.IF_ICMPEQ;
		}
	}
}
