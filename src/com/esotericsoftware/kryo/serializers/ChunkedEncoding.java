/* Copyright (c) 2008-2026, Nathan Sweet
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
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Chunked encoding of {@link CompatibleFieldSerializer} and {@link TaggedFieldSerializer}, which allows skipping the data of a
 * field. Implemented by {@link DefaultChunkedEncoding} and, for the format of Kryo 5, by {@link LegacyChunkedEncoding}. */
interface ChunkedEncoding {
	/** Returns the chunked encoding for the serializer config, or null if chunked encoding is disabled. */
	static ChunkedEncoding get (Kryo kryo, boolean chunked, boolean legacyChunks, int chunkSize) {
		if (!chunked) return null;
		return legacyChunks ? LegacyChunkedEncoding.get(chunkSize) : DefaultChunkedEncoding.get(kryo);
	}

	/** Returns the output for the data of an object outside its fields, eg the tags of TaggedFieldSerializer. Must be followed by
	 * {@link #endWrite()}. */
	Output beginWrite (Output output);

	/** Returns the output for the field data.
	 * @param output The output returned by {@link #beginWrite(Output)}. */
	Output fieldOutput (Output output);

	void endWrite ();

	/** Takes the field names of CompatibleFieldSerializer for the class, which are written the first time in the object graph.
	 * @return false if the field names are not written by the chunked encoding, so the serializer writes them. */
	boolean writeFieldNames (Class type, String[] names);

	/** Starts a field. Returns a mark for {@link #endField(Output, long)}. */
	long beginField (Output output);

	void endField (Output output, long mark);

	/** Returns the input for the fields of an object. Must be followed by {@link #endRead()}. */
	Input beginRead (Input input);

	void endRead ();

	/** Returns the field names of CompatibleFieldSerializer for the class, which has no field names for the object graph yet.
	 * @return null if the field names are not written by the chunked encoding, so the serializer reads them. */
	String[] readFieldNames (Class type);

	/** Starts a field. Returns the end of the field for {@link #endField(Input, long, int)}. {@link #fieldObjects()} must be
	 * called directly afterward, before reading the field data, which can start nested fields. */
	long beginField (Input input);

	/** Returns the number of objects read after the field started by the last {@link #beginField(Input)}, for
	 * {@link #endField(Input, long, int)}. It is needed to reserve the IDs of objects that were not read, eg if reading an unknown
	 * field fails after reading nested objects. */
	int fieldObjects ();

	/** Ends a field, skipping the rest of it. */
	void endField (Input input, long end, int objects);
}
