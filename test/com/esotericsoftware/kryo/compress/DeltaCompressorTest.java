
package com.esotericsoftware.kryo.compress;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serialize.StringSerializer;

public class DeltaCompressorTest extends TestCase {
	public void testDeltaCompressor () {
		// Log.TRACE();

		ByteBuffer buffer = ByteBuffer.allocateDirect(2048);
		String data = "this is some data this is some data this is some data this is some data this is some data "
			+ "this is some data this is some data this is some data this is some data this is some data this is some data"
			+ "this is some data this is some data this is some data this is some data this is some data this is some data"
			+ "this is some data this is some data this is some data this is some data this is some data this is some data";

		Kryo kryo = new Kryo();
		Kryo.getContext().setRemoteEntityID(123);

		DeltaCompressor delta = new DeltaCompressor(kryo, new StringSerializer());
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

		kryo.removeRemoteEntity(123);
	}
}
