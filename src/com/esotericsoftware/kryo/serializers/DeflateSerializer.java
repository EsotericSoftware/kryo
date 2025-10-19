/* Copyright (c) 2008-2025, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.InputChunked;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.OutputChunked;

import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

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
		outputChunked.endChunk();
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
