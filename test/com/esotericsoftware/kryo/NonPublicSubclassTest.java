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


package com.esotericsoftware.kryo;

import static org.junit.jupiter.api.Assertions.*;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.PriorityBlockingQueue;

import org.junit.jupiter.api.Test;

/** Subclasses that are not public, in another package than the serializers, created with a public constructor. */
class NonPublicSubclassTest extends KryoTestCase {
	@Test
	void testComparatorCollections () {
		kryo.register(SkipListSet.class);
		kryo.register(BlockingQueue.class);
		kryo.register(ReverseComparator.class);

		SkipListSet set = new SkipListSet(new ReverseComparator());
		set.addAll(List.of(1, 3, 2));
		assertEquals(List.of(3, 2, 1), new ArrayList<>(writeRead(set)));

		BlockingQueue queue = new BlockingQueue(1, new ReverseComparator());
		queue.addAll(List.of(1, 3, 2));
		assertEquals(3, writeRead(queue).poll());
	}

	private <T> T writeRead (T object) {
		Output output = new Output(1024);
		kryo.writeObject(output, object);
		return (T)kryo.readObject(new Input(output.toBytes()), object.getClass());
	}

	private static class SkipListSet extends ConcurrentSkipListSet<Integer> {
		public SkipListSet (Comparator comparator) {
			super(comparator);
		}
	}

	private static class BlockingQueue extends PriorityBlockingQueue<Integer> {
		public BlockingQueue (int initialCapacity, Comparator comparator) {
			super(initialCapacity, comparator);
		}
	}

	private static class ReverseComparator implements Comparator<Integer> {
		public int compare (Integer o1, Integer o2) {
			return o2 - o1;
		}
	}
}
