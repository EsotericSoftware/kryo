
package com.esotericsoftware.kryo.compress;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import junit.framework.TestCase;

import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serialize.StringSerializer;

public class StreamCompressorTest extends TestCase {
	public void testDeflateCompressor () {
		// Log.TRACE();

		ByteBuffer buffer = ByteBuffer.allocateDirect(2048);
		String data = "this is some data this is some data this is some data this is some data this is some data "
			+ "this is some data this is some data this is some data this is some data this is some data this is some data"
			+ "this is some data this is some data this is some data this is some data this is some data this is some data"
			+ "this is some data this is some data this is some data this is some data this is some data this is some data";

		class DeflateStreamCompressor extends StreamCompressor {
			public DeflateStreamCompressor (Serializer serializer) {
				super(serializer);
			}

			public FilterOutputStream getCompressionStream (OutputStream output) throws IOException {
				return new GZIPOutputStream(output);
			}

			public FilterInputStream getDecompressionStream (InputStream input) throws IOException {
				return new GZIPInputStream(input);
			}
		}

		DeflateStreamCompressor deflate = new DeflateStreamCompressor(new StringSerializer());
		deflate.writeObjectData(buffer, data);
		buffer.flip();
		String newData = deflate.readObjectData(buffer, String.class);
		assertEquals(data, newData);
	}
}
