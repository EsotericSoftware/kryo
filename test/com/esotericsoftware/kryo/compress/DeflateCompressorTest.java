
package com.esotericsoftware.kryo.compress;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.serialize.StringSerializer;

public class DeflateCompressorTest extends KryoTestCase {
	public void testDeflateCompressor () {
		// Log.TRACE();
		ByteBuffer buffer = ByteBuffer.allocateDirect(2048);
		String data = "this is some data this is some data this is some data this is some data this is some data "
			+ "this is some data this is some data this is some data this is some data this is some data this is some data"
			+ "this is some data this is some data this is some data this is some data this is some data this is some data"
			+ "this is some data this is some data this is some data this is some data this is some data this is some data";
		roundTrip(new DeflateCompressor(new StringSerializer()), 35, data);
	}
}
