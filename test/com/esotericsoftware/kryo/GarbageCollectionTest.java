/* Copyright (c) 2008-2020, Nathan Sweet
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

package com.esotericsoftware.kryo;

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.util.DefaultClassResolver;
import com.esotericsoftware.kryo.util.MapReferenceResolver;

import java.lang.ref.WeakReference;

import org.junit.jupiter.api.Test;

/** Tests for detecting PermGen memory leaks.
 * @author Tumi <serverperformance@gmail.com> */
class GarbageCollectionTest {
	@Test
	void test () {
		Kryo kryo = new Kryo(new DefaultClassResolver(), new MapReferenceResolver());
		WeakReference<Kryo> kryoWeakRef = new WeakReference(kryo);
		kryo = null; // remove strong ref, now kryo is only weak-reachable
		reclaim(kryoWeakRef);
	}

	private void reclaim (WeakReference<Kryo> kryoWeakRef) {
		// Forces GC
		System.gc();
		// Waits for recaim the weaked-reachable kryo instance
		int times = 0;
		while (kryoWeakRef.get() != null && times < 30) { // limit 3 seconds
			try {
				Thread.sleep(100);
			} catch (InterruptedException ignored) {
			}
			times++;
		}
		assertNull(kryoWeakRef.get());
	}
}
