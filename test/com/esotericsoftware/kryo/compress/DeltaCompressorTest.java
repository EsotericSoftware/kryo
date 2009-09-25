
package com.esotericsoftware.kryo.compress;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serialize.StringSerializer;
import com.esotericsoftware.log.Log;

public class DeltaCompressorTest extends TestCase {
	public void testDeflateCompressor () {
		Log.level = Log.TRACE;

		ByteBuffer buffer = ByteBuffer.allocateDirect(2048);
		String data = "this is some data this is some data this is some data this is some data this is some data "
			+ "this is some data this is some data this is some data this is some data this is some data this is some data"
			+ "this is some data this is some data this is some data this is some data this is some data this is some data"
			+ "this is some data this is some data this is some data this is some data this is some data this is some data";

		DeltaCompressor delta = new DeltaCompressor(new StringSerializer());
		delta.setContext(new Context());
		delta.writeObjectData(buffer, data);
		buffer.flip();
		String newData = delta.readObjectData(buffer, String.class);
		assertEquals(data, newData);

		data += "abc";
		buffer.clear();
		delta.writeObjectData(buffer, data);
		buffer.flip();
		newData = delta.readObjectData(buffer, String.class);
		assertEquals(data, newData);
	}
}
