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
import static com.esotericsoftware.kryo.util.Log.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.ReferenceResolver;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.ArrayList;
import java.util.Arrays;

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
 * class is written as varint registration ID + 1, or 0 and the class written by Kryo. Format of a field: varlong length, with
 * references varint number of objects, then the field data. The length is written as a varint, which has the same bytes as a
 * varlong for lengths that fit in an int, and read as a varlong, so data with longer fields stays readable.
 * <p>
 * The outermost object is buffered in memory until it is written completely, so it must be smaller than 2 GiB. */
final class DefaultChunkedEncoding implements ChunkedEncoding {
	private static final Object contextKey = new Object();
	/** The graph context keys for writing and reading, separate so reading doesn't hide stale write scopes and vice versa. */
	private static final Object writeGraphKey = new Object(), readGraphKey = new Object();
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
	/** The end and the number of objects after each field being read, by depth. Nested fields are started before the outer field
	 * ends. */
	private long[] fieldEnds = new long[8];
	private int[] fieldObjects = new int[8];
	private int fieldDepth;

	private DefaultChunkedEncoding (Kryo kryo) {
		this.kryo = kryo;
	}

	/** Returns the instance for the Kryo, which is kept in its {@link Kryo#getContext() context} so the buffers are reused. */
	static DefaultChunkedEncoding get (Kryo kryo) {
		DefaultChunkedEncoding encoding = (DefaultChunkedEncoding)kryo.getContext().get(contextKey);
		if (encoding == null) kryo.getContext().put(contextKey, encoding = new DefaultChunkedEncoding(kryo));
		return encoding;
	}

	/** Returns true if the scopes are from a previous object graph, eg left over after an exception. The graph context is cleared
	 * when the object graph is reset.
	 * @param key The graph context key for writing or reading. */
	private boolean newGraph (Object key) {
		boolean newGraph = !kryo.getGraphContext().containsKey(key);
		if (newGraph) kryo.getGraphContext().put(key, Boolean.TRUE);
		return newGraph;
	}

	/** Returns the buffer of the current scope if the output is that buffer, otherwise the buffer of a new scope. */
	public Output beginWrite (Output output) {
		if (writeDepth > 0) {
			WriteScope scope = writeScopes.get(writeDepth - 1);
			if (scope.buffer == output) {
				scope.nested++;
				return output;
			}
		}
		if (newGraph(writeGraphKey)) writeDepth = 0;
		if (writeDepth == writeScopes.size()) writeScopes.add(new WriteScope());
		WriteScope scope = writeScopes.get(writeDepth++);
		scope.parent = output;
		scope.nested = 0;
		if (scope.buffer == null)
			scope.buffer = new Output(256, -1);
		else
			scope.buffer.reset();
		// The object data is read from the input directly, which has the encoding of the output.
		scope.buffer.setVariableLengthEncoding(output.getVariableLengthEncoding());
		scope.outermostFieldNames = false;
		// Not empty if an exception was thrown while writing the previous object graph.
		scope.fieldNameTypes.clear();
		scope.fieldNames.clear();
		scope.namesMark = kryo.getClassResolver().beginDeferredNames();
		return scope.buffer;
	}

	/** If the object started the current scope, writes the data first written in it and then the object data. */
	public void endWrite () {
		WriteScope scope = writeScopes.get(writeDepth - 1);
		if (scope.nested > 0) {
			scope.nested--;
			return;
		}
		writeDepth--;
		Output parent = scope.parent;
		if (TRACE) {
			trace("kryo", "Write scope: " + scope.fieldNameTypes.size() + " field names, " + scope.buffer.position() + " bytes"
				+ pos(parent.position()));
		}
		kryo.getClassResolver().endDeferredNames(parent, scope.namesMark);
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

	/** Remembers the field names to write them before the object data. */
	public boolean writeFieldNames (Class type, String[] names) {
		WriteScope scope = writeScopes.get(writeDepth - 1);
		// The outermost object writes its field names before any nested object, so they are first.
		if (scope.nested == 0) scope.outermostFieldNames = true;
		scope.fieldNameTypes.add(type);
		scope.fieldNames.add(names);
		return true;
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

	/** Starts a field, reserving space for its length. Returns the start of the field and the number of objects written before
	 * it. */
	public long beginField (Output output) {
		int start = output.position();
		if (!kryo.getReferences()) {
			output.writeByte(0);
			return start;
		}
		output.writeShort(0);
		return (long)kryo.getReferenceResolver().getObjectCount() << 32 | start;
	}

	/** Ends a field: writes its length and the number of objects before the field data, moving the data if it needs more space. */
	public void endField (Output output, long mark) {
		int start = (int)mark;
		boolean references = kryo.getReferences();
		int reserved = references ? 2 : 1;
		int end = output.position(), length = end - start - reserved;
		int objects = references ? kryo.getReferenceResolver().getObjectCount() - (int)(mark >>> 32) : 0;
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

	/** Reads the data first written in the scope, if the input starts a new scope. */
	public Input beginRead (Input input) {
		if (readDepth > 0) {
			ReadScope scope = readScopes.get(readDepth - 1);
			if (scope.input == input) {
				scope.nested++;
				return input;
			}
		}
		if (newGraph(readGraphKey)) {
			readDepth = 0;
			fieldDepth = 0;
			fieldNameTypes.clear();
			fieldNames.clear();
		}
		if (readDepth == readScopes.size()) readScopes.add(new ReadScope());
		ReadScope scope = readScopes.get(readDepth++);
		scope.input = input;
		scope.nested = 0;
		scope.outermostFieldNames = null;

		if (TRACE) trace("kryo", "Read scope" + pos(input.position()));
		kryo.getClassResolver().readDeferredNames(input);
		int entries = input.readVarInt(true);
		for (int i = 0, n = entries >>> 1; i < n; i++) {
			Registration registration = readClass(input);
			String[] names = readStrings(input);
			if (TRACE)
				trace("kryo", "Read field names: " + (registration == null ? "<unknown class>" : className(registration.getType())));
			if (i == 0 && (entries & 1) != 0) scope.outermostFieldNames = names;
			if (registration != null) {
				fieldNameTypes.add(registration.getType());
				fieldNames.add(names);
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

	/** Returns the field names for the class, read in the current object graph. */
	public String[] readFieldNames (Class type) {
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

	public void endRead () {
		ReadScope scope = readScopes.get(readDepth - 1);
		if (scope.nested > 0)
			scope.nested--;
		else {
			scope.input = null;
			readDepth--;
		}
	}

	/** Starts a field, reading its length and the number of objects in it. Returns the depth of the field, where its end and the
	 * number of objects read after it are kept until {@link #endField(Input, long)}. */
	public long beginField (Input input) {
		long length = input.readVarLong(true);
		if (length < 0) throw new KryoException("Invalid field length: " + length);
		int objects = 0;
		if (kryo.getReferences()) {
			// The IDs of the objects that are not read are reserved, so the number of objects is limited like an array length. It
			// can't be limited by the length of the field, which can contain compressed data.
			int count = input.readVarInt(true), read = kryo.getReferenceResolver().getObjectCount();
			if (count < 0 || count > input.getMaxArraySize() || read + count < read) {
				throw new KryoException(
					"Invalid number of objects in the field: " + count + " (maxArraySize: " + input.getMaxArraySize() + ")");
			}
			if (read >= 0) objects = read + count; // Else the number of objects is unknown and no IDs are reserved.
		}
		if (TRACE) trace("kryo", "Read field: " + length + " bytes" + pos(input.position()));
		int depth = fieldDepth++;
		if (depth == fieldEnds.length) {
			fieldEnds = Arrays.copyOf(fieldEnds, depth << 1);
			fieldObjects = Arrays.copyOf(fieldObjects, depth << 1);
		}
		fieldEnds[depth] = input.total() + length;
		fieldObjects[depth] = objects;
		return depth;
	}

	/** Ends a field: skips the rest of it and reserves the IDs of the objects in it that were not read. */
	public void endField (Input input, long mark) {
		int depth = (int)mark;
		fieldDepth = depth; // Also discards nested fields that weren't ended, eg after an exception that was caught.
		long remaining = fieldEnds[depth] - input.total();
		int objects = fieldObjects[depth];
		if (remaining < 0) throw new KryoException("More data was read than the field contains: " + -remaining + " bytes");
		if (remaining > 0) {
			if (TRACE) trace("kryo", "Skip field: " + remaining + " bytes");
			input.skip(remaining);
		}
		if (objects > 0) {
			ReferenceResolver referenceResolver = kryo.getReferenceResolver();
			int read = referenceResolver.getObjectCount();
			// The IDs of the objects after the field would be wrong.
			if (read > objects)
				throw new KryoException("More objects were read than the field contains: " + (read - objects) + " objects");
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
		/** The mark of {@link com.esotericsoftware.kryo.ClassResolver#beginDeferredNames()}. */
		int namesMark;
		boolean outermostFieldNames;
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
