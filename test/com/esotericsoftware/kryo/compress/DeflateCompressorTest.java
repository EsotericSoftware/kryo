
package com.esotericsoftware.kryo.compress;

import static com.esotericsoftware.minlog.Log.*;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.serialize.StringSerializer;
import com.esotericsoftware.minlog.Log;

public class DeflateCompressorTest extends TestCase {
	public void testDeflateCompressor () {
		Log.set(LEVEL_TRACE);

		ByteBuffer buffer = ByteBuffer.allocateDirect(2048);
		String data = "this is some data this is some data this is some data this is some data this is some data "
			+ "this is some data this is some data this is some data this is some data this is some data this is some data"
			+ "this is some data this is some data this is some data this is some data this is some data this is some data"
			+ "this is some data this is some data this is some data this is some data this is some data this is some data";

		DeflateCompressor deflate = new DeflateCompressor(new StringSerializer());
		deflate.writeObjectData(buffer, data);
		buffer.flip();
		String newData = deflate.readObjectData(buffer, String.class);

		assertEquals(data, newData);
	}
}
