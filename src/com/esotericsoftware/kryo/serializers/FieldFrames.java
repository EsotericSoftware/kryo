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

import static com.esotericsoftware.kryo.util.Util.*;
import static com.esotericsoftware.minlog.Log.*;

import com.esotericsoftware.kryo.ClassResolver;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.ReferenceResolver;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.InputChunked;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.OutputChunked;

import java.util.ArrayList;

/** Chunked encoding of {@link CompatibleFieldSerializer} and {@link TaggedFieldSerializer}: each field is written with its
 * length, so it can be skipped, eg when the class of a removed field no longer exists.
 * <p>
 * Data that Kryo writes only the first time in an object graph, class names of unregistered classes and the field names of
 * CompatibleFieldSerializer, is not written inside the fields. The outermost object with chunked encoding starts a scope: it is
 * written to a buffer, and the data first written in it is written before the buffer. So skipping a field never loses it. Nested
 * objects are written to the same buffer, unless a serializer in between writes to its own output, eg DeflateSerializer, which
 * starts a nested scope. With references, the number of objects in each field is written too, so the reference IDs stay in sync
 * when a field is skipped.
 * <p>
 * Format of a scope: varint number of class names, each with varint name ID and name; varint number of field names shifted left
 * by 1, bit 1 if the first are those of the outermost object, each with the class and the field names; then the object data. A
 * class is written as varint registration ID + 1, or 0 and the class written by Kryo. Format of a field: varint length, with
 * references varint number of objects, then the field data.
 * <p>
 * The chunked encoding of Kryo 5 splits each field into chunks with {@link OutputChunked} and {@link InputChunked}, which hold
 * all its state. */
final class FieldFrames {
	private static final Object contextKey = new Object();
	/** Larger buffers are not kept for the next scope. */
	private static final int maxBufferSize = 1024 * 1024;

	private final Kryo kryo;
	private final ArrayList<WriteScope> writeScopes = new ArrayList();
	private final ArrayList<ReadScope> readScopes = new ArrayList();
	private int writeDepth, readDepth;
	/** The field names read in the current object graph and their classes. Usually there are only a few, so a list is faster than
	 * a map, which would be cleared for each object graph. */
	private final ArrayList<Class> fieldNameTypes = new ArrayList();
	private final ArrayList<String[]> fieldNames = new ArrayList();

	/** The number of objects read after the field started by {@link #beginField(Input, boolean)}. Nested fields overwrite it. */
	private int fieldObjects;

	private FieldFrames (Kryo kryo) {
		this.kryo = kryo;
	}

	/** Returns the instance for the Kryo, which is kept in its {@link Kryo#getContext() context} so the buffers are reused. */
	static FieldFrames get (Kryo kryo) {
		FieldFrames frames = (FieldFrames)kryo.getContext().get(contextKey);
		if (frames == null) kryo.getContext().put(contextKey, frames = new FieldFrames(kryo));
		return frames;
	}

	/** Returns true if the scopes are from a previous object graph, eg left over after an exception. The graph context is cleared
	 * when the object graph is reset. */
	private boolean newGraph () {
		boolean newGraph = !kryo.getGraphContext().containsKey(contextKey);
		if (newGraph) kryo.getGraphContext().put(contextKey, Boolean.TRUE);
		return newGraph;
	}

	/** Returns the output for the fields: for the format of Kryo 5 a chunked output, otherwise the buffer of the current scope if
	 * the output is that buffer, or the buffer of a new scope. Must be followed by {@link #endWrite(boolean)}. */
	Output beginWrite (Output output, boolean legacyChunks, int chunkSize) {
		if (legacyChunks) return new OutputChunked(output, chunkSize);
		if (writeDepth > 0) {
			WriteScope scope = writeScopes.get(writeDepth - 1);
			if (scope.buffer == output) {
				scope.nested++;
				return output;
			}
		}
		if (newGraph()) writeDepth = 0;
		if (writeDepth == writeScopes.size()) writeScopes.add(new WriteScope());
		WriteScope scope = writeScopes.get(writeDepth++);
		scope.parent = output;
		scope.nested = 0;
		if (scope.buffer == null)
			scope.buffer = new Output(256, -1);
		else
			scope.buffer.reset();
		scope.outermostFieldNames = false;
		ClassResolver classResolver = kryo.getClassResolver();
		scope.names = classResolver.getWrittenNameCount();
		scope.deferNames = classResolver.deferNames(true);
		return scope.buffer;
	}

	/** If the object started the current scope, writes the data first written in it and then the object data. */
	void endWrite (boolean legacyChunks) {
		if (legacyChunks) return;
		WriteScope scope = writeScopes.get(writeDepth - 1);
		if (scope.nested > 0) {
			scope.nested--;
			return;
		}
		writeDepth--;
		ClassResolver classResolver = kryo.getClassResolver();
		classResolver.deferNames(scope.deferNames);
		Output parent = scope.parent;
		if (TRACE) {
			trace("kryo", "Write scope: " + (classResolver.getWrittenNameCount() - scope.names) + " class names, "
				+ scope.fieldNameTypes.size() + " field names, " + scope.buffer.position() + " bytes" + pos(parent.position()));
		}
		classResolver.writeNames(parent, scope.names);
		ArrayList<Class> types = scope.fieldNameTypes;
		ArrayList<String[]> fieldNames = scope.fieldNames;
		parent.writeVarInt(types.size() << 1 | (scope.outermostFieldNames ? 1 : 0), true);
		for (int i = 0, n = types.size(); i < n; i++) {
			writeClass(parent, types.get(i));
			writeStrings(parent, fieldNames.get(i));
		}
		// The outer scope writes the field names too, in case this scope is skipped. It writes the class names anyway.
		if (writeDepth > 0) {
			WriteScope outer = writeScopes.get(writeDepth - 1);
			outer.fieldNameTypes.addAll(types);
			outer.fieldNames.addAll(fieldNames);
		}
		types.clear();
		fieldNames.clear();

		Output buffer = scope.buffer;
		parent.writeBytes(buffer.getBuffer(), 0, buffer.position());
		scope.parent = null;
		if (buffer.getBuffer().length > maxBufferSize) scope.buffer = null;
	}

	/** Writes registered classes by ID, so reading doesn't throw an exception when the class is unknown. */
	private void writeClass (Output output, Class type) {
		int id = kryo.getRegistration(type).getId();
		if (id >= 0)
			output.writeVarInt(id + 1, true);
		else {
			output.writeVarInt(0, true);
			kryo.writeClass(output, type);
		}
	}

	/** Remembers the field names of CompatibleFieldSerializer for the class, to write them before the object data. */
	void writeFieldNames (Class type, String[] names) {
		WriteScope scope = writeScopes.get(writeDepth - 1);
		// The outermost object writes its field names before any nested object, so they are first.
		if (scope.nested == 0) scope.outermostFieldNames = true;
		scope.fieldNameTypes.add(type);
		scope.fieldNames.add(names);
	}

	private static void writeStrings (Output output, String[] values) {
		output.writeVarInt(values.length, true);
		for (String value : values)
			output.writeString(value);
	}

	private static String[] readStrings (Input input) {
		String[] values = new String[input.validateArrayLength(input.readVarInt(true))];
		for (int i = 0; i < values.length; i++)
			values[i] = input.readString();
		return values;
	}

	/** Starts a field, reserving space for its length. Returns the start of the field and the number of objects written before it,
	 * or -1 for the format of Kryo 5, for {@link #endField(Output, long)}. */
	long beginField (Output output, boolean legacyChunks) {
		if (legacyChunks) return -1;
		int start = output.position();
		if (!kryo.getReferences()) {
			output.writeByte(0);
			return start;
		}
		output.writeShort(0);
		return (long)kryo.getReferenceResolver().getWrittenCount() << 32 | start;
	}

	/** Ends a field: writes its length and the number of objects before the field data, moving the data if it needs more space.
	 * For the format of Kryo 5, ends the chunk. */
	void endField (Output output, long mark) {
		if (mark == -1) {
			((OutputChunked)output).endChunk();
			return;
		}
		int start = (int)mark;
		boolean references = kryo.getReferences();
		int reserved = references ? 2 : 1;
		int end = output.position(), length = end - start - reserved;
		int objects = references ? kryo.getReferenceResolver().getWrittenCount() - (int)(mark >>> 32) : 0;
		int header = Output.varIntLength(length, true) + (references ? Output.varIntLength(objects, true) : 0);
		if (header > reserved) {
			for (int i = reserved; i < header; i++)
				output.writeByte(0);
			byte[] buffer = output.getBuffer();
			System.arraycopy(buffer, start + reserved, buffer, start + header, length);
			end += header - reserved;
		}
		output.setPosition(start);
		output.writeVarInt(length, true);
		if (references) output.writeVarInt(objects, true);
		output.setPosition(end);
		if (TRACE)
			trace("kryo", "Write field: " + length + " bytes" + (references ? ", " + objects + " objects" : "") + pos(start));
	}

	/** Returns the input for the fields: for the format of Kryo 5 a chunked input, otherwise the input after reading the data
	 * first written in the scope, if the input starts a new scope. Must be followed by {@link #endRead(boolean)}. */
	Input beginRead (Input input, boolean legacyChunks, int chunkSize) {
		if (legacyChunks) return new InputChunked(input, chunkSize);
		if (readDepth > 0) {
			ReadScope scope = readScopes.get(readDepth - 1);
			if (scope.input == input) {
				scope.nested++;
				return input;
			}
		}
		if (newGraph()) {
			readDepth = 0;
			fieldNameTypes.clear();
			fieldNames.clear();
		}
		if (readDepth == readScopes.size()) readScopes.add(new ReadScope());
		ReadScope scope = readScopes.get(readDepth++);
		scope.input = input;
		scope.nested = 0;
		scope.outermostFieldNames = null;

		if (TRACE) trace("kryo", "Read scope" + pos(input.position()));
		kryo.getClassResolver().readNames(input);
		int fieldNames = input.readVarInt(true);
		for (int i = 0, n = fieldNames >>> 1; i < n; i++) {
			Registration registration = readClass(input);
			String[] names = readStrings(input);
			if (TRACE)
				trace("kryo", "Read field names: " + (registration == null ? "<unknown class>" : className(registration.getType())));
			if (i == 0 && (fieldNames & 1) != 0) scope.outermostFieldNames = names;
			if (registration != null) {
				fieldNameTypes.add(registration.getType());
				this.fieldNames.add(names);
			}
		}
		return input;
	}

	/** @return May be null if the class is unknown. */
	private Registration readClass (Input input) {
		int id = input.readVarInt(true);
		if (id > 0) return kryo.getClassResolver().getRegistration(id - 1);
		try {
			return kryo.readClass(input);
		} catch (KryoException ignored) { // Unknown class name.
			return null;
		}
	}

	/** Returns the field names of CompatibleFieldSerializer for the class, read in the current object graph. */
	String[] fieldNames (Class type) {
		for (int i = fieldNameTypes.size() - 1; i >= 0; i--)
			if (fieldNameTypes.get(i) == type) return fieldNames.get(i);
		// The object is read as a different class than it was written, if it started the scope and its field names were new.
		ReadScope scope = readScopes.get(readDepth - 1);
		if (scope.nested > 0 || scope.outermostFieldNames == null) {
			throw new KryoException("Field names not found for class: " + type.getName()
				+ ". With chunked encoding, an object can only be read as a different class than it was written if its class is written, "
				+ "eg if the type of a field changed, with readUnknownFieldData true.");
		}
		return scope.outermostFieldNames;
	}

	void endRead (boolean legacyChunks) {
		if (legacyChunks) return;
		ReadScope scope = readScopes.get(readDepth - 1);
		if (scope.nested > 0)
			scope.nested--;
		else {
			scope.input = null;
			readDepth--;
		}
	}

	/** Starts a field, reading its length and the number of objects in it. Returns the {@link Input#total()} where the field ends,
	 * or -1 for the format of Kryo 5, for {@link #endField(Input, long, int)}. {@link #fieldObjects()} must be called directly
	 * afterward, before reading the field data, which can start nested fields. */
	long beginField (Input input, boolean legacyChunks) {
		if (legacyChunks) return -1;
		int length = input.readVarInt(true);
		fieldObjects = kryo.getReferences() ? kryo.getReferenceResolver().getReadCount() + input.readVarInt(true) : 0;
		if (TRACE) trace("kryo", "Read field: " + length + " bytes" + pos(input.position()));
		return input.total() + length;
	}

	/** Returns the number of objects read after the field started by the last {@link #beginField(Input, boolean)}, for
	 * {@link #endField(Input, long, int)}. Must be called directly after beginField, because nested fields overwrite it. It is
	 * needed to reserve the IDs of objects that were not read, eg if reading an unknown field fails after reading nested
	 * objects. */
	int fieldObjects () {
		return fieldObjects;
	}

	/** Ends a field: skips the rest of it and reserves the IDs of the objects in it that were not read. For the format of Kryo 5,
	 * skips to the next chunk. */
	void endField (Input input, long end, int objects) {
		if (end == -1) {
			((InputChunked)input).nextChunk();
			return;
		}
		long remaining = end - input.total();
		if (remaining < 0) throw new KryoException("More data was read than the field contains: " + -remaining + " bytes");
		if (remaining > 0) {
			if (TRACE) trace("kryo", "Skip field: " + remaining + " bytes");
			input.skip(remaining);
		}
		if (objects > 0) {
			ReferenceResolver referenceResolver = kryo.getReferenceResolver();
			int read = referenceResolver.getReadCount();
			if (TRACE && read < objects) trace("kryo", "Skip field references: " + (objects - read));
			for (int i = read; i < objects; i++)
				referenceResolver.nextReadId(Object.class);
		}
	}

	static private class WriteScope {
		/** The object data is written to the buffer, then to the parent. */
		Output buffer, parent;
		/** The number of nested objects being written to the buffer. */
		int nested;
		/** The number of class names written before the scope. */
		int names;
		boolean deferNames, outermostFieldNames;
		final ArrayList<Class> fieldNameTypes = new ArrayList();
		final ArrayList<String[]> fieldNames = new ArrayList();
	}

	static private class ReadScope {
		Input input;
		/** The number of nested objects being read from the input. */
		int nested;
		String[] outermostFieldNames;
	}
}
