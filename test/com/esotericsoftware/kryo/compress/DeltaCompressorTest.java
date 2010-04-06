
package com.esotericsoftware.kryo.compress;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.serialize.StringSerializer;

public class DeltaCompressorTest extends KryoTestCase {
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
		roundTripOnce(delta, 419, data);
		data += "abc";
		roundTripOnce(delta, 25, data);
		data = "abc" + data;
		roundTripOnce(delta, 28, data);
		data = "something and something else";
		roundTripOnce(delta, 33, data);
		data = "something and moo something else";
		roundTripOnce(delta, 22, data);
		data = "moo something and something else moo";
		roundTripOnce(delta, 25, data);

		kryo.removeRemoteEntity(123);
	}
}
