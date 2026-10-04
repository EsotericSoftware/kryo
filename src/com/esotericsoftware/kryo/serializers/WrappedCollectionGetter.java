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
import com.esotericsoftware.kryo.unsafe.UnsafeUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.function.Function;

/** Gets the collection or map that is wrapped by an unmodifiable or synchronized wrapper of {@link java.util.Collections}, or
 * another private field of java.util, eg the access order of a LinkedHashMap. The field is read with a method handle if java.util
 * is open to Kryo, or else with Unsafe, if available. A primitive value is boxed. The field is resolved on first use. */
final class WrappedCollectionGetter {
	private final String className, fieldName;
	// Not volatile: the instances are shared, but a getter only has final fields, so another thread sees it fully initialized or
	// null, and then creates its own.
	private Function<Object, Object> getter;

	WrappedCollectionGetter (String className, String fieldName) {
		this.className = className;
		this.fieldName = fieldName;
	}

	Object get (Object wrapper) {
		if (getter == null) getter = getter();
		return getter.apply(wrapper);
	}

	private Function<Object, Object> getter () {
		Field field;
		try {
			field = Class.forName(className).getDeclaredField(fieldName);
		} catch (ReflectiveOperationException ex) {
			throw new KryoException("Unable to find field: " + className + "." + fieldName, ex);
		}
		if (!isAndroid && Object.class.getModule().isOpen("java.util", WrappedCollectionGetter.class.getModule())) {
			try {
				MethodHandle handle = MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup())
					.unreflectGetter(field).asType(MethodType.methodType(Object.class, Object.class));
				return wrapper -> {
					try {
						return (Object)handle.invokeExact(wrapper);
					} catch (Throwable t) {
						throw new KryoException(t);
					}
				};
			} catch (IllegalAccessException ex) {
				throw new KryoException("Unable to access field: " + className + "." + fieldName, ex);
			}
		}
		if (unsafe) {
			long offset = UnsafeUtil.objectFieldOffset(field);
			if (field.getType() == boolean.class) return object -> UnsafeUtil.getBoolean(object, offset);
			return wrapper -> UnsafeUtil.getObject(wrapper, offset);
		}
		if (isAndroid) {
			throw new KryoException("Unable to access field: " + className + "." + fieldName
				+ ". This is not supported on Android, register a serializer for the class instead.");
		}
		throw new KryoException("Unable to access field: " + className + "." + fieldName
			+ ". Allow it with --add-opens java.base/java.util=" + moduleName()
			+ ", or allow Unsafe with --sun-misc-unsafe-memory-access=allow.");
	}
}
