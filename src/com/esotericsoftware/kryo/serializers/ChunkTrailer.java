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

import com.esotericsoftware.kryo.ClassResolver;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.ReferenceResolver;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.InputChunked;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.OutputChunked;
import com.esotericsoftware.kryo.util.Util;

import java.util.ArrayList;

/** Written after each field chunk of {@link CompatibleFieldSerializer} and {@link TaggedFieldSerializer} with chunked encoding.
 * Data that is written only the first time in an object graph is written again after the chunk in which it was first written:
 * class names of unregistered classes and the field names of CompatibleFieldSerializer. The number of objects written in the
 * chunk is written too, so the reference IDs stay in sync. If a reader skips the chunk, eg because the class of a removed field
 * no longer exists, it still knows this data. If nothing new was written in the chunk, the trailer is a single byte. There is no
 * trailer if the value class is known to both sides and its serialization can't write such data, eg for primitive wrappers.
 * <p>
 * Format: a varint with the number of objects written in the chunk shifted left by 2, bit 1 if class names follow and bit 2 if
 * field names follow. */
final class ChunkTrailer {
	/** Key in the graph context for the CompatibleFieldSerializers that wrote their field names, in order. */
	private static final Object fieldNamesKey = new Object();

	private final Kryo kryo;
	private final ClassResolver classResolver;
	private final ReferenceResolver referenceResolver;
	private final boolean references, stringReferences;
	private ArrayList<CompatibleFieldSerializer> fieldNamesList;
	private int names, fieldNames, objects;

	ChunkTrailer (Kryo kryo) {
		this.kryo = kryo;
		classResolver = kryo.getClassResolver();
		referenceResolver = kryo.getReferenceResolver();
		references = kryo.getReferences();
		stringReferences = references && referenceResolver.useReferences(String.class);
	}

	private ArrayList<CompatibleFieldSerializer> fieldNamesList () {
		if (fieldNamesList == null) fieldNamesList = (ArrayList)kryo.getGraphContext().get(fieldNamesKey);
		return fieldNamesList;
	}

	/** Remembers what was written before the next chunk. */
	void markWrite () {
		names = classResolver.getWrittenNameCount();
		ArrayList list = fieldNamesList();
		fieldNames = list == null ? 0 : list.size();
		objects = references ? referenceResolver.getWrittenCount() : 0;
	}

	/** Remembers what was read before the next chunk. */
	void markRead () {
		objects = references ? referenceResolver.getReadCount() : 0;
	}

	/** Returns true if there is no trailer after a chunk with a value of the specified class, which is known when reading. This is
	 * the case if the serialization of the value can't write class names, field names, or objects with references.
	 * @param valueClass May be null. */
	boolean omits (Class valueClass) {
		// Primitives and their wrappers have the same registration, so the reader may get the primitive class.
		return valueClass == null || valueClass.isPrimitive() || Util.isWrapperClass(valueClass)
			|| (valueClass == String.class && !stringReferences);
	}

	void endChunk (OutputChunked outputChunked, Output output, boolean omit) {
		outputChunked.endChunk();
		if (omit) return;
		int names = classResolver.getWrittenNameCount();
		ArrayList<CompatibleFieldSerializer> list = fieldNamesList();
		int fieldNames = list == null ? 0 : list.size();
		int objects = references ? referenceResolver.getWrittenCount() : 0;
		output.writeVarInt((objects - this.objects) << 2 | (fieldNames > this.fieldNames ? 2 : 0) | (names > this.names ? 1 : 0),
			true);
		if (names > this.names) classResolver.writeNames(output, this.names);
		if (fieldNames > this.fieldNames) {
			output.writeVarInt(fieldNames - this.fieldNames, true);
			for (int i = this.fieldNames; i < fieldNames; i++) {
				CompatibleFieldSerializer serializer = list.get(i);
				kryo.writeClass(output, serializer.getType());
				serializer.writeFieldNames(output);
			}
		}
	}

	void nextChunk (InputChunked inputChunked, Input input, boolean omit) {
		inputChunked.nextChunk();
		if (omit) return;
		int parts = input.readVarInt(true);
		if ((parts & 1) != 0) classResolver.readNames(input);
		if ((parts & 2) != 0) {
			for (int i = 0, n = input.readVarInt(true); i < n; i++) {
				Registration registration = null;
				try {
					registration = kryo.readClass(input);
				} catch (KryoException ignored) { // Unknown class.
				}
				String[] names = CompatibleFieldSerializer.readFieldNames(input);
				if (registration != null && registration.getSerializer() instanceof CompatibleFieldSerializer serializer
					&& !kryo.getGraphContext().containsKey(serializer)) serializer.setFieldNames(kryo, names);
			}
		}
		int objects = parts >>> 2;
		if (objects > 0) {
			// Reserve the IDs of the objects in the chunk that were not read, so the following IDs are correct.
			for (int i = referenceResolver.getReadCount() - this.objects; i < objects; i++)
				referenceResolver.nextReadId(Object.class);
		}
	}

	/** Called by CompatibleFieldSerializer when it writes its field names. */
	static void fieldNamesWritten (Kryo kryo, CompatibleFieldSerializer serializer) {
		ArrayList list = (ArrayList)kryo.getGraphContext().get(fieldNamesKey);
		if (list == null) kryo.getGraphContext().put(fieldNamesKey, list = new ArrayList());
		list.add(serializer);
	}
}
