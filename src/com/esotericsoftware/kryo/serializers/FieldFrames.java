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
import com.esotericsoftware.kryo.io.Output;

import java.util.ArrayList;

/** Chunked encoding of {@link CompatibleFieldSerializer} and {@link TaggedFieldSerializer}: each field is written with its
 * length, so it can be skipped, eg when the class of a removed field no longer exists.
 * <p>
 * Data that Kryo writes only the first time in an object graph, class names of unregistered classes and the field names of
 * CompatibleFieldSerializer, is not written inside the fields. The outermost object with chunked encoding is written to a buffer,
 * and the data first written in it is written before the buffer. So skipping a field never loses it. With references, the number
 * of objects in each field is written too, so the reference IDs stay in sync when a field is skipped.
 * <p>
 * Format of the outermost object: varint number of class names, each with varint name ID and name, varint number of field names,
 * each with class and field names, then the object data. Format of a field: varint length, with references varint number of
 * objects, then the field data. */
final class FieldFrames {
	private static final Object contextKey = new Object();

	private final Kryo kryo;
	private final ArrayList<Scope> scopes = new ArrayList();
	private int depth;
	private final Output scratch = new Output(32, -1);

	private FieldFrames (Kryo kryo) {
		this.kryo = kryo;
	}

	/** Returns the instance for the Kryo, which is kept in its {@link Kryo#getContext() context} so the buffers are reused. */
	static FieldFrames get (Kryo kryo) {
		FieldFrames frames = (FieldFrames)kryo.getContext().get(contextKey);
		if (frames == null) kryo.getContext().put(contextKey, frames = new FieldFrames(kryo));
		return frames;
	}

	private Scope current () {
		return depth == 0 ? null : scopes.get(depth - 1);
	}

	private Scope push () {
		if (depth == scopes.size()) scopes.add(new Scope());
		return scopes.get(depth++);
	}

	/** Returns the current scope, discarding scopes of a previous object graph, eg left over after an exception. */
	private Scope current (Object stream) {
		if (depth > 0 && !kryo.getGraphContext().containsKey(contextKey)) depth = 0;
		Scope scope = current();
		return scope != null && (scope.output == stream || scope.input == stream) ? scope : null;
	}

	private Scope push (Object stream) {
		kryo.getGraphContext().put(contextKey, Boolean.TRUE);
		Scope scope = push();
		scope.nested = 0;
		return scope;
	}

	// --- Writing ---

	/** Returns the output for the object data: the output itself if it is the buffer of the current scope, otherwise the buffer of
	 * a new scope. Must be followed by {@link #endWrite()}. */
	Output beginWrite (Output output) {
		Scope scope = current(output);
		if (scope != null) {
			scope.nested++;
			return output;
		}
		scope = push(output);
		if (scope.output == null) scope.output = new Output(256, -1);
		scope.output.reset();
		scope.parent = output;
		ClassResolver classResolver = kryo.getClassResolver();
		scope.names = classResolver.getWrittenNameCount();
		scope.deferNames = classResolver.deferNames(true);
		return scope.output;
	}

	/** For a new scope, writes the data first written in it and then the object data. */
	void endWrite () {
		Scope scope = current();
		if (scope.nested > 0) {
			scope.nested--;
			return;
		}
		Output parent = scope.parent, buffer = scope.output;
		ClassResolver classResolver = kryo.getClassResolver();
		ArrayList<CompatibleFieldSerializer> fieldNames = scope.fieldNames;
		// Assign name IDs to the classes with field names, so they are written with the class names.
		scratch.reset();
		for (int i = 0, n = fieldNames.size(); i < n; i++)
			kryo.writeClass(scratch, fieldNames.get(i).getType());
		classResolver.deferNames(scope.deferNames);
		if (classResolver.getWrittenNameCount() == scope.names)
			parent.writeVarInt(0, true);
		else
			classResolver.writeNames(parent, scope.names);
		parent.writeVarInt(fieldNames.size(), true);
		for (int i = 0, n = fieldNames.size(); i < n; i++) {
			CompatibleFieldSerializer serializer = fieldNames.get(i);
			kryo.writeClass(parent, serializer.getType());
			serializer.writeFieldNames(parent);
		}
		// The outer scope writes them too, in case the data of this scope is skipped. Class names are written again anyway.
		if (depth > 1) scopes.get(depth - 2).fieldNames.addAll(fieldNames);
		fieldNames.clear();
		parent.writeBytes(buffer.getBuffer(), 0, buffer.position());
		scope.parent = null;
		depth--;
	}

	/** Remembers the CompatibleFieldSerializer to write its field names before the object data. */
	void writeFieldNames (CompatibleFieldSerializer serializer) {
		current().fieldNames.add(serializer);
	}

	/** Reserves space for the field length. Returns the start of the field and the number of objects written before it, for
	 * {@link #endField(Output, long)}. */
	long beginField (Output output) {
		int start = output.position();
		if (!kryo.getReferences()) {
			output.writeByte(0);
			return start;
		}
		output.writeShort(0);
		return (long)kryo.getReferenceResolver().getWrittenCount() << 32 | start;
	}

	/** Writes the field length and the number of objects before the field data, moving the data if it needs more space. */
	void endField (Output output, long mark) {
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
	}

	// --- Reading ---

	/** Reads the data first written in the scope, if the input is not the input of the current scope. Must be followed by
	 * {@link #endRead()}. */
	void beginRead (Input input) {
		Scope scope = current(input);
		if (scope != null) {
			scope.nested++;
			return;
		}
		scope = push(input);
		scope.input = input;
		scope.firstFieldNames = null;
		kryo.getClassResolver().readNames(input);
		for (int i = 0, n = input.readVarInt(true); i < n; i++) {
			Registration registration = null;
			try {
				registration = kryo.readClass(input);
			} catch (KryoException ignored) { // Unknown class.
			}
			String[] names = CompatibleFieldSerializer.readFieldNames(input);
			if (i == 0) scope.firstFieldNames = names;
			if (registration != null && registration.getSerializer() instanceof CompatibleFieldSerializer serializer
				&& !kryo.getGraphContext().containsKey(serializer)) serializer.setFieldNames(kryo, names);
		}
	}

	/** Returns the field names of the outermost object of a new scope, which are written first, or null. This allows reading an
	 * object as a different class than it was written. */
	String[] outermostFieldNames () {
		Scope scope = current();
		return scope.nested == 0 ? scope.firstFieldNames : null;
	}

	void endRead () {
		Scope scope = current();
		if (scope.nested > 0)
			scope.nested--;
		else {
			scope.input = null;
			depth--;
		}
	}

	/** Skips the rest of a field and reserves the IDs of the objects in it that were not read.
	 * @param end The {@link Input#total()} where the field ends.
	 * @param objects The number of objects in the field.
	 * @param readObjects The number of objects read before the field. */
	void endField (Input input, long end, int objects, int readObjects) {
		long remaining = end - input.total();
		if (remaining > 0) input.skip(remaining);
		if (objects > 0) {
			ReferenceResolver referenceResolver = kryo.getReferenceResolver();
			for (int i = referenceResolver.getReadCount() - readObjects; i < objects; i++)
				referenceResolver.nextReadId(Object.class);
		}
	}

	static private class Scope {
		Output output, parent;
		Input input;
		int names, nested;
		boolean deferNames;
		final ArrayList<CompatibleFieldSerializer> fieldNames = new ArrayList();
		String[] firstFieldNames;
	}
}
