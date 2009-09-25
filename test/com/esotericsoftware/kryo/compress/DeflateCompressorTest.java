
package com.esotericsoftware.kryo.compress;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.serialize.StringSerializer;
import com.esotericsoftware.log.Log;

public class DeflateCompressorTest extends TestCase {
	public void testDeflateCompressor () {
		Log.level = Log.TRACE;

		ByteBuffer buffer = ByteBuffer.allocateDirect(2048);
		String data = "this is some data this is some data this is some data this is some data this is some data "
			+ "this is some data this is some data this is some data this is some data this is some data this is some data"
			+ "this is some data this is some data this is some data this is some data this is some data this is some data"
			+ "this is some data this is some data this is some data this is some data this is some data this is some data";

		DeflateCompressor deflate = new DeflateCompressor(new StringSerializer());
		deflate.setContext(new Context());
		deflate.writeObjectData(buffer, data);
		buffer.flip();
		String newData = deflate.readObjectData(buffer, String.class);

		assertEquals(data, newData);
	}
}
