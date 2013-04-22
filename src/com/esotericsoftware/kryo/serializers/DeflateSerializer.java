
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
		Deflater deflater = new Deflater(compressionLevel, noHeaders);
		OutputChunked outputChunked = new OutputChunked(output, 256);
		DeflaterOutputStream deflaterStream = new DeflaterOutputStream(outputChunked, deflater);
		Output deflaterOutput = new Output(deflaterStream, 256);
		kryo.writeObject(deflaterOutput, object, serializer);
		deflaterOutput.flush();
		try {
			deflaterStream.finish();
		} catch (IOException ex) {
			throw new KryoException(ex);
		}
		outputChunked.endChunks();
	}

	public Object read (Kryo kryo, Input input, Class type) {
		// The inflater would read from input beyond the compressed bytes if chunked enoding wasn't used.
		InflaterInputStream inflaterStream = new InflaterInputStream(new InputChunked(input, 256), new Inflater(noHeaders));
		return kryo.readObject(new Input(inflaterStream, 256), type, serializer);
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
