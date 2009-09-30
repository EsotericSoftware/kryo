
package com.esotericsoftware.kryo.compress;

import static com.esotericsoftware.minlog.Log.LEVEL_TRACE;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serialize.StringSerializer;
import com.esotericsoftware.minlog.Log;

public class DeltaCompressorTest extends TestCase {
	public void testDeltaCompressor () {
		Log.set(LEVEL_TRACE);

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
