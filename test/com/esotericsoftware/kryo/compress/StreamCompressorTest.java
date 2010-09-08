
package com.esotericsoftware.kryo.compress;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.esotericsoftware.kryo.KryoTestCase;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serialize.StringSerializer;

public class StreamCompressorTest extends KryoTestCase {
	public void testDeflateCompressor () {
		// Log.TRACE();

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

		roundTrip(new DeflateStreamCompressor(new StringSerializer()), 47, data);
	}
}
