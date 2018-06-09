/* Copyright (c) 2008-2018, Nathan Sweet
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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.InputChunked;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.OutputChunked;
import com.esotericsoftware.kryo.util.ObjectMap;

/** Serializes objects using direct field assignment, providing both forward and backward compatibility. This means fields can be
 * added or removed without invalidating previously serialized bytes. Changing the type of a field is not supported. Like
 * {@link FieldSerializer}, it can serialize most classes without needing annotations. The forward and backward compatibility
 * comes at a cost: the first time the class is encountered in the serialized bytes, a simple schema is written containing the
 * field name strings. Also, during serialization and deserialization buffers are allocated to perform chunked encoding. This is
 * what enables CompatibleFieldSerializer to skip bytes for fields it does not know about.
 * <p>
 * Note that the field data is identified by name. If a super class has a field with the same name as a subclass,
 * {@link CompatibleFieldSerializerConfig#setExtendedFieldNames(boolean)} must be true.
 * @author Nathan Sweet */
public class CompatibleFieldSerializer<T> extends FieldSerializer<T> {
	static private final int binarySearchThreshold = 32;

	private CompatibleFieldSerializerConfig config;

	public CompatibleFieldSerializer (Kryo kryo, Class type) {
		this(kryo, type, new CompatibleFieldSerializerConfig());
	}

	public CompatibleFieldSerializer (Kryo kryo, Class type, CompatibleFieldSerializerConfig config) {
		super(kryo, type, config);
		this.config = config;
	}

	public void write (Kryo kryo, Output output, T object) {
		if (TRACE) trace("kryo", "Writing fields for class: " + type.getName());

		CachedField[] fields = getFields();
		ObjectMap context = kryo.getGraphContext();
		if (!context.containsKey(this)) {
			context.put(this, null);
			output.writeVarInt(fields.length, true);
			for (int i = 0, n = fields.length; i < n; i++) {
				if (TRACE) trace("kryo", "Write field name: " + fields[i].name + pos(output.position()));
				output.writeString(fields[i].name);
			}
		}

		boolean chunked = config.chunked;
		boolean writeValueClass = !chunked || kryo.getReferences();
		Output fieldOutput;
		OutputChunked outputChunked = null;
		if (chunked)
			fieldOutput = outputChunked = new OutputChunked(output, 1024);
		else
			fieldOutput = output;
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];
			if (TRACE) log("Write", cachedField, output.position());

			// Write the concrete type in case the field is removed there was a reference to the field value.
			if (writeValueClass) {
				Class valueClass = null;
				try {
					if (object != null) {
						Object value = cachedField.getField().get(object);
						if (value != null) valueClass = value.getClass();
					}
				} catch (IllegalAccessException ex) {
				}
				kryo.writeClass(fieldOutput, valueClass);
				cachedField.setClass(valueClass);
			}

			cachedField.write(fieldOutput, object);
			if (chunked) outputChunked.endChunk();
		}
	}

	public T read (Kryo kryo, Input input, Class<? extends T> type) {
		if (TRACE) trace("kryo", "Reading fields for class: " + type.getName());

		T object = create(kryo, input, type);
		kryo.reference(object);
		CachedField[] fields = (CachedField[])kryo.getGraphContext().get(this);
		if (fields == null) fields = readFields(kryo, input);

		boolean chunked = config.chunked;
		boolean readValueClass = !chunked || kryo.getReferences();
		Input fieldInput;
		InputChunked inputChunked = null;
		if (chunked)
			fieldInput = inputChunked = new InputChunked(input, 1024);
		else
			fieldInput = input;
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];

			if (readValueClass) {
				Registration registration;
				try {
					registration = kryo.readClass(fieldInput);
				} catch (KryoException ex) {
					if (!chunked) throw new KryoException("Unable to read obsolete data (unknown type).", ex);
					if (DEBUG) debug("kryo", "Unable to read obsolete data (unknown type).", ex);
					inputChunked.nextChunk();
					continue;
				}
				Class valueClass = registration == null ? null : registration.getType();
				if (cachedField == null) {
					// Read obsolete data in case it is a reference.
					if (TRACE) trace("kryo", "Skip obsolete field, type: " + valueClass == null ? null : className(valueClass));
					try {
						if (valueClass != null) kryo.readObject(fieldInput, valueClass);
					} catch (KryoException ex) {
						if (!chunked) throw new KryoException("Unable to read obsolete data.", ex);
						if (DEBUG) debug("kryo", "Unable to read obsolete data.", ex);
					}
					if (chunked) inputChunked.nextChunk();
					continue;
				}
				cachedField.setClass(valueClass);
			} else if (cachedField == null) {
				if (TRACE) trace("kryo", "Skip obsolete field.");
				inputChunked.nextChunk();
				continue;
			}

			if (TRACE) log("Read", cachedField, input.position());
			cachedField.read(fieldInput, object);
			if (chunked) inputChunked.nextChunk();
		}
		return object;

	}

	private CachedField[] readFields (Kryo kryo, Input input) {
		int length = input.readVarInt(true);
		String[] names = new String[length];
		for (int i = 0; i < length; i++) {
			names[i] = input.readString();
			if (TRACE) trace("kryo", "Read field name: " + names[i]);
		}

		CachedField[] fields = new CachedField[length];
		CachedField[] allFields = getFields();
		if (length < binarySearchThreshold) {
			outer:
			for (int i = 0; i < length; i++) {
				String schemaName = names[i];
				for (int ii = 0, nn = allFields.length; ii < nn; ii++) {
					if (allFields[ii].name.equals(schemaName)) {
						fields[i] = allFields[ii];
						continue outer;
					}
				}
				if (TRACE) trace("kryo", "Ignore obsolete field name: " + schemaName);
			}
		} else {
			int low, mid, high, compare;
			int lastFieldIndex = allFields.length;
			outer:
			for (int i = 0; i < length; i++) {
				String schemaName = names[i];
				low = 0;
				high = lastFieldIndex;
				while (low <= high) {
					mid = (low + high) >>> 1;
					compare = schemaName.compareTo(allFields[mid].name);
					if (compare < 0)
						high = mid - 1;
					else if (compare > 0)
						low = mid + 1;
					else {
						fields[i] = allFields[mid];
						continue outer;
					}
				}
				if (TRACE) trace("kryo", "Ignore obsolete field name: " + schemaName);
			}
		}

		kryo.getGraphContext().put(this, fields);
		return fields;
	}

	public CompatibleFieldSerializerConfig getCompatibleFieldSerializerConfig () {
		return config;
	}

	/** Configuration for CompatibleFieldSerializer instances. */
	static public class CompatibleFieldSerializerConfig extends FieldSerializerConfig {
		boolean chunked;

		public CompatibleFieldSerializerConfig clone () {
			return (CompatibleFieldSerializerConfig)super.clone(); // Clone is ok as we have only primitive fields.
		}

		public void setChunked (boolean chunked) {
			this.chunked = chunked;
			if (TRACE) trace("kryo", "CompatibleFieldSerializerConfig setChunked: " + chunked);
		}

		public boolean getChunked () {
			return chunked;
		}
	}
}
