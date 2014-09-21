
package com.esotericsoftware.kryo.serializers;

import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.InputChunked;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.OutputChunked;

public class DeflateSerializer extends Serializer {
	private final Serializer serializer;
	private boolean noHeaders = true;
	private int compressionLevel = 4;

	public DeflateSerializer (Serializer serializer) {
		this.serializer = serializer;
	}

	public void write (Kryo kryo, Output output, Object object) {
		OutputChunked outputChunked = new OutputChunked(output, 256);
		Deflater deflater = new Deflater(compressionLevel, noHeaders);
		try {
			DeflaterOutputStream deflaterStream = new DeflaterOutputStream(outputChunked, deflater);
			Output deflaterOutput = new Output(deflaterStream, 256);
			serializer.write(kryo, deflaterOutput, object);
			deflaterOutput.flush();
			deflaterStream.finish();
		} catch (IOException ex) {
			throw new KryoException(ex);
		} finally {
			deflater.end();
		}
		outputChunked.endChunks();
	}

	public Object read (Kryo kryo, Input input, Class type) {
		// The inflater would read from input beyond the compressed bytes if chunked enoding wasn't used.
		Inflater inflater = new Inflater(noHeaders);
		try {
			InflaterInputStream inflaterStream = new InflaterInputStream(new InputChunked(input, 256), inflater);
			return serializer.read(kryo, new Input(inflaterStream, 256), type);
		} finally {
			inflater.end();
		}
	}

	public void setNoHeaders (boolean noHeaders) {
		this.noHeaders = noHeaders;
	}

	/** Default is 4.
	 * @see Deflater#setLevel(int) */
	public void setCompressionLevel (int compressionLevel) {
		this.compressionLevel = compressionLevel;
	}

	public Object copy (Kryo kryo, Object original) {
		return serializer.copy(kryo, original);
	}
}
