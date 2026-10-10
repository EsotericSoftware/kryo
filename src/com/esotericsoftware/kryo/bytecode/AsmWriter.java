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

import java.util.function.Consumer;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/** {@link Bytecode} with ASM, which is an optional dependency, for Java versions before 24, which have no Class-File API. */
final class AsmWriter extends Bytecode {
	private final ClassWriter writer;

	AsmWriter (String name, String superName) {
		writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		writer.visit(Opcodes.V17, ACC_FINAL | ACC_SUPER, name, null, superName, null);
	}

	public void field (String name, String descriptor, int flags) {
		writer.visitField(flags, name, descriptor, null, null).visitEnd();
	}

	public void method (String name, String descriptor, int flags, Consumer<Code> body) {
		MethodVisitor method = writer.visitMethod(flags, name, descriptor, null, null);
		method.visitCode();
		body.accept(new AsmCode(method));
		method.visitMaxs(0, 0); // Computed.
		method.visitEnd();
	}

	public byte[] bytes () {
		writer.visitEnd();
		return writer.toByteArray();
	}

	static private final class AsmCode extends Code {
		private final MethodVisitor code;

		AsmCode (MethodVisitor code) {
			this.code = code;
		}

		public void aload (int local) {
			code.visitVarInsn(Opcodes.ALOAD, local);
		}

		public void iload (int local) {
			code.visitVarInsn(Opcodes.ILOAD, local);
		}

		public void lload (int local) {
			code.visitVarInsn(Opcodes.LLOAD, local);
		}

		public void istore (int local) {
			code.visitVarInsn(Opcodes.ISTORE, local);
		}

		public void astore (int local) {
			code.visitVarInsn(Opcodes.ASTORE, local);
		}

		public void lstore (int local) {
			code.visitVarInsn(Opcodes.LSTORE, local);
		}

		public void iconst (int value) {
			if (value >= -1 && value <= 5)
				code.visitInsn(Opcodes.ICONST_0 + value);
			else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE)
				code.visitIntInsn(Opcodes.BIPUSH, value);
			else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE)
				code.visitIntInsn(Opcodes.SIPUSH, value);
			else
				code.visitLdcInsn(value);
		}

		public void ldc (String value) {
			code.visitLdcInsn(value);
		}

		public void ldcClass (String name) {
			code.visitLdcInsn(Type.getObjectType(name));
		}

		public void getstatic (String owner, String name, String descriptor) {
			code.visitFieldInsn(Opcodes.GETSTATIC, owner, name, descriptor);
		}

		public void putstatic (String owner, String name, String descriptor) {
			code.visitFieldInsn(Opcodes.PUTSTATIC, owner, name, descriptor);
		}

		public void getfield (String owner, String name, String descriptor) {
			code.visitFieldInsn(Opcodes.GETFIELD, owner, name, descriptor);
		}

		public void putfield (String owner, String name, String descriptor) {
			code.visitFieldInsn(Opcodes.PUTFIELD, owner, name, descriptor);
		}

		public void invokevirtual (String owner, String name, String descriptor) {
			code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, descriptor, false);
		}

		public void invokestatic (String owner, String name, String descriptor) {
			code.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, descriptor, false);
		}

		public void invokeinterface (String owner, String name, String descriptor) {
			code.visitMethodInsn(Opcodes.INVOKEINTERFACE, owner, name, descriptor, true);
		}

		public void invokespecial (String owner, String name, String descriptor) {
			code.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, name, descriptor, false);
		}

		public void checkcast (String name) {
			code.visitTypeInsn(Opcodes.CHECKCAST, name);
		}

		public void aaload () {
			code.visitInsn(Opcodes.AALOAD);
		}

		public void pop () {
			code.visitInsn(Opcodes.POP);
		}

		public void vreturn () {
			code.visitInsn(Opcodes.RETURN);
		}

		public void areturn () {
			code.visitInsn(Opcodes.ARETURN);
		}

		public void athrow () {
			code.visitInsn(Opcodes.ATHROW);
		}

		public void ifThen (int condition, Runnable thenBlock) {
			Label end = new Label();
			code.visitJumpInsn(negate(condition), end);
			thenBlock.run();
			code.visitLabel(end);
		}

		public void ifThenElse (int condition, Runnable thenBlock, Runnable elseBlock) {
			Label elseLabel = new Label(), end = new Label();
			code.visitJumpInsn(negate(condition), elseLabel);
			thenBlock.run();
			code.visitJumpInsn(Opcodes.GOTO, end);
			code.visitLabel(elseLabel);
			elseBlock.run();
			code.visitLabel(end);
		}

		public void trying (Runnable body, Runnable handler) {
			Label start = new Label(), end = new Label(), handlerLabel = new Label(), after = new Label();
			code.visitTryCatchBlock(start, end, handlerLabel, null);
			code.visitLabel(start);
			body.run();
			code.visitLabel(end);
			code.visitJumpInsn(Opcodes.GOTO, after);
			code.visitLabel(handlerLabel);
			handler.run();
			code.visitLabel(after);
		}

		/** The jump that skips a block when the condition is false. */
		static private int negate (int condition) {
			return condition == IFNE ? Opcodes.IFEQ : Opcodes.IF_ICMPNE;
		}
	}
}
