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

package com.esotericsoftware.kryo.io;

import static com.esotericsoftware.kryo.util.Util.*;

import com.esotericsoftware.kryo.util.IgnoreAndroid;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/** Reads and writes little endian ints and longs in a byte array with a single memory access, using VarHandles. On Android, where
 * VarHandles need API level 33, the bytes are accessed individually. */
final class Bytes {
	@IgnoreAndroid
	static void putInt (byte[] buffer, int p, int value) {
		if (isAndroid) {
			buffer[p] = (byte)value;
			buffer[p + 1] = (byte)(value >> 8);
			buffer[p + 2] = (byte)(value >> 16);
			buffer[p + 3] = (byte)(value >> 24);
		} else
			Handles.INT.set(buffer, p, value);
	}

	@IgnoreAndroid
	static void putLong (byte[] buffer, int p, long value) {
		if (isAndroid) {
			buffer[p] = (byte)value;
			buffer[p + 1] = (byte)(value >>> 8);
			buffer[p + 2] = (byte)(value >>> 16);
			buffer[p + 3] = (byte)(value >>> 24);
			buffer[p + 4] = (byte)(value >>> 32);
			buffer[p + 5] = (byte)(value >>> 40);
			buffer[p + 6] = (byte)(value >>> 48);
			buffer[p + 7] = (byte)(value >>> 56);
		} else
			Handles.LONG.set(buffer, p, value);
	}

	@IgnoreAndroid
	static int getInt (byte[] buffer, int p) {
		if (isAndroid) {
			return buffer[p] & 0xFF //
				| (buffer[p + 1] & 0xFF) << 8 //
				| (buffer[p + 2] & 0xFF) << 16 //
				| (buffer[p + 3] & 0xFF) << 24;
		}
		return (int)Handles.INT.get(buffer, p);
	}

	@IgnoreAndroid
	static long getLong (byte[] buffer, int p) {
		if (isAndroid) {
			return buffer[p] & 0xFF //
				| (buffer[p + 1] & 0xFF) << 8 //
				| (buffer[p + 2] & 0xFF) << 16 //
				| (long)(buffer[p + 3] & 0xFF) << 24 //
				| (long)(buffer[p + 4] & 0xFF) << 32 //
				| (long)(buffer[p + 5] & 0xFF) << 40 //
				| (long)(buffer[p + 6] & 0xFF) << 48 //
				| (long)buffer[p + 7] << 56;
		}
		return (long)Handles.LONG.get(buffer, p);
	}

	/** Separate class so VarHandle is not loaded on Android. */
	@IgnoreAndroid
	static private final class Handles {
		static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
		static final VarHandle LONG = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
	}
}
