/* Copyright (c) 2008-2018, Nathan Sweet
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

package com.esotericsoftware.kryo.unsafe;

import static com.esotericsoftware.minlog.Log.*;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.util.Util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

/** Utility methods for using {@link sun.misc.Unsafe}.
 * <p>
 * Not available on all JVMs. {@link Util#unsafe} can be checked before using this class.
 * @author Roman Levenstein <romixlev@gmail.com> */
@SuppressWarnings("restriction")
public class UnsafeUtil {
	/** The sun.misc.Unsafe instance, or null if Unsafe is unavailable. */
	static public final Unsafe unsafe;

	static public final long byteArrayBaseOffset;
	static public final long floatArrayBaseOffset;
	static public final long doubleArrayBaseOffset;
	static public final long intArrayBaseOffset;
	static public final long longArrayBaseOffset;
	static public final long shortArrayBaseOffset;
	static public final long charArrayBaseOffset;
	static public final long booleanArrayBaseOffset;
	static {
		Unsafe tempUnsafe = null;
		long tempByteArrayBaseOffset = 0;
		long tempFloatArrayBaseOffset = 0;
		long tempDoubleArrayBaseOffset = 0;
		long tempIntArrayBaseOffset = 0;
		long tempLongArrayBaseOffset = 0;
		long tempShortArrayBaseOffset = 0;
		long tempCharArrayBaseOffset = 0;
		long tempBooleanArrayBaseOffset = 0;

		try {
			if (!Util.isAndroid) {
				Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				field.setAccessible(true);
				tempUnsafe = (sun.misc.Unsafe)field.get(null);
				tempByteArrayBaseOffset = tempUnsafe.arrayBaseOffset(byte[].class);
				tempCharArrayBaseOffset = tempUnsafe.arrayBaseOffset(char[].class);
				tempShortArrayBaseOffset = tempUnsafe.arrayBaseOffset(short[].class);
				tempIntArrayBaseOffset = tempUnsafe.arrayBaseOffset(int[].class);
				tempFloatArrayBaseOffset = tempUnsafe.arrayBaseOffset(float[].class);
				tempLongArrayBaseOffset = tempUnsafe.arrayBaseOffset(long[].class);
				tempDoubleArrayBaseOffset = tempUnsafe.arrayBaseOffset(double[].class);
				tempBooleanArrayBaseOffset = tempUnsafe.arrayBaseOffset(boolean[].class);
			} else {
				if (DEBUG) debug("kryo", "Unsafe is not available on Android.");
			}
		} catch (Exception ex) {
			if (DEBUG) debug("kryo", "Unsafe is not available.", ex);
		}

		byteArrayBaseOffset = tempByteArrayBaseOffset;
		charArrayBaseOffset = tempCharArrayBaseOffset;
		shortArrayBaseOffset = tempShortArrayBaseOffset;
		intArrayBaseOffset = tempIntArrayBaseOffset;
		floatArrayBaseOffset = tempFloatArrayBaseOffset;
		longArrayBaseOffset = tempLongArrayBaseOffset;
		doubleArrayBaseOffset = tempDoubleArrayBaseOffset;
		booleanArrayBaseOffset = tempBooleanArrayBaseOffset;
		unsafe = tempUnsafe;
	}

	// Constructor to be used for creation of ByteBuffers that use preallocated memory regions.
	static private Constructor<? extends ByteBuffer> directByteBufferConstructor;
	static {
		ByteBuffer buffer = ByteBuffer.allocateDirect(1);
		try {
			directByteBufferConstructor = buffer.getClass().getDeclaredConstructor(long.class, int.class, Object.class);
			directByteBufferConstructor.setAccessible(true);
		} catch (Exception ex) {
			if (DEBUG) debug("kryo", "No direct ByteBuffer constructor is available.", ex);
			directByteBufferConstructor = null;
		}
	}

	static private Method cleanerMethod, cleanMethod;
	static {
		try {
			cleanerMethod = DirectBuffer.class.getMethod("cleaner");
			cleanerMethod.setAccessible(true);
			cleanMethod = cleanerMethod.getReturnType().getMethod("clean");
		} catch (Exception ex) {
			if (DEBUG) debug("kryo", "No direct ByteBuffer clean method is available.", ex);
			cleanerMethod = null;
		}
	}

	/** Create a ByteBuffer that uses the specified off-heap memory address instead of allocating a new one.
	 * @param address Address of the memory region to be used for a ByteBuffer.
	 * @param size Size in bytes of the memory region.
	 * @throws UnsupportedOperationException if creating a ByteBuffer this way is not available. */
	static public ByteBuffer newDirectBuffer (long address, int size) {
		if (directByteBufferConstructor == null)
			throw new UnsupportedOperationException("No direct ByteBuffer constructor is available.");
		try {
			return directByteBufferConstructor.newInstance(address, size, null);
		} catch (Exception ex) {
			throw new KryoException("Error creating a ByteBuffer at address: " + address, ex);
		}
	}

	/** Returns true if {@link #newDirectBuffer(long, int)} can be called. */
	static public boolean isNewDirectBufferAvailable () {
		return directByteBufferConstructor != null;
	}

	/** Release a direct buffer immediately rather than waiting for GC. */
	static public void dispose (ByteBuffer buffer) {
		if (!(buffer instanceof DirectBuffer)) return;
		if (cleanerMethod != null) {
			try {
				cleanMethod.invoke(cleanerMethod.invoke(buffer));
			} catch (Throwable ignored) {
			}
		}
	}
}
