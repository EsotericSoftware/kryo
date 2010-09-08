
package com.esotericsoftware.kryo.compress;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

public class DeltaTest extends TestCase {
	private ByteBuffer oldData, newData;
	private Delta delta = new Delta();

	public DeltaTest () {
		oldData = ByteBuffer.allocateDirect(20 * 4);
		for (int i = 0; i < oldData.capacity() / 4; i++)
			oldData.putInt(i);
		oldData.flip();

		newData = ByteBuffer.allocateDirect(500);
	}

	public void testDeltas () {
		resetNewData();

		check("New data is same");

		resetNewData();
		newData.putInt(95);
		newData.putInt(96);
		check("New data is longer");

		resetNewData();
		newData.position(newData.position() - 8);
		check("New data is shorter");

		newData.clear();
		newData.putInt(95);
		newData.putInt(96);
		newData.put(oldData);
		oldData.rewind();
		check("New data has data inserted at beginning");

		newData.position(newData.limit() - 8);
		check("New data is shorter and has data inserted at beginning");

		resetNewData();
		newData.put(16, (byte)95);
		newData.put(64, (byte)96);
		check("New data is modified in the middle");

		resetNewData();
		newData.put(0, (byte)94);
		newData.put(45, (byte)95);
		newData.put(newData.position() - 1, (byte)96);
		check("New data is modified in the beginning, middle, and end");

		newData.clear();
		for (int i = 0; i < oldData.capacity(); i++)
			newData.put((byte)i);
		check("New data is completely different");
	}

	private void resetNewData () {
		newData.clear();
		newData.put(oldData);
		oldData.rewind();
	}

	private void check (String name) {
		System.out.println();
		System.out.println(name);

		newData.flip();

		dump(oldData);
		dump(newData);

		ByteBuffer deltaData = ByteBuffer.allocate(2048);
		delta.compress(oldData, newData, deltaData);
		deltaData.flip();
		oldData.rewind();
		newData.rewind();

		dump(deltaData);

		ByteBuffer reconstructed = ByteBuffer.allocate(2048);
		delta.decompress(oldData, deltaData, reconstructed);
		reconstructed.flip();
		oldData.rewind();

		dump(reconstructed);

		assertEquals("Data should be the same length.", newData.remaining(), reconstructed.remaining());
		while (newData.hasRemaining() && reconstructed.hasRemaining())
			assertEquals("Data should be the same.", newData.get(), reconstructed.get());
		newData.rewind();
	}

	private void dump (ByteBuffer bb) {
		bb.mark();
		StringBuilder sb = new StringBuilder();
		while (bb.hasRemaining()) {
			sb.append(bb.get());
			sb.append(',');
		}
		bb.reset();
		System.out.println(sb);
	}
}
