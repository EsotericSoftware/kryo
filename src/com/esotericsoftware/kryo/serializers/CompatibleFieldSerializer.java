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
import com.esotericsoftware.kryo.util.Util;

/** Serializes objects using direct field assignment, providing both forward and backward compatibility. This means fields can be
 * added or removed without invalidating previously serialized bytes. Renaming or changing the type of a field is not supported.
 * Like {@link FieldSerializer}, it can serialize most classes without needing annotations.
 * <p>
 * The forward and backward compatibility and serialization performance depend on
 * {@link CompatibleFieldSerializerConfig#setReadUnknownFieldData(boolean)} and
 * {@link CompatibleFieldSerializerConfig#setChunkedEncoding(boolean)}. Additionally, the first time the class is encountered in
 * the serialized bytes, a simple schema is written containing the field name strings.
 * <p>
 * Note that the field data is identified by name. If a super class has a field with the same name as a subclass,
 * {@link CompatibleFieldSerializerConfig#setExtendedFieldNames(boolean)} must be true.
 * @author Nathan Sweet */
public class CompatibleFieldSerializer<T> extends FieldSerializer<T> {
	private static final int binarySearchThreshold = 32;

	private final CompatibleFieldSerializerConfig config;

	public CompatibleFieldSerializer (Kryo kryo, Class type) {
		this(kryo, type, new CompatibleFieldSerializerConfig());
	}

	public CompatibleFieldSerializer (Kryo kryo, Class type, CompatibleFieldSerializerConfig config) {
		super(kryo, type, config);
		this.config = config;
	}

	public void write (Kryo kryo, Output output, T object) {
		int pop = pushTypeVariables();

		CachedField[] fields = cachedFields.fields;
		ObjectMap context = kryo.getGraphContext();
		if (!context.containsKey(this)) {
			if (TRACE) trace("kryo", "Write fields for class: " + type.getName());
			context.put(this, null);
			output.writeVarInt(fields.length, true);
			for (int i = 0, n = fields.length; i < n; i++) {
				if (TRACE) trace("kryo", "Write field name: " + fields[i].name + pos(output.position()));
				output.writeString(fields[i].name);
			}
		}

		boolean chunked = config.chunked, readUnknownTagData = config.readUnknownFieldData;
		Output fieldOutput;
		OutputChunked outputChunked = null;
		if (chunked)
			fieldOutput = outputChunked = new OutputChunked(output, config.chunkSize);
		else
			fieldOutput = output;
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];
			if (TRACE) log("Write", cachedField, output.position());

			// Write the value class so the field data can be read even if the field is removed.
			if (readUnknownTagData) {
				Class valueClass = null;
				try {
					if (object != null) {
						Object value = cachedField.field.get(object);
						if (value != null) valueClass = value.getClass();
					}
				} catch (IllegalAccessException ex) {
				}
				kryo.writeClass(fieldOutput, valueClass);
				if (valueClass == null) {
					if (chunked) outputChunked.endChunk();
					continue;
				}
				cachedField.setCanBeNull(false);
				cachedField.setValueClass(valueClass);
				cachedField.setReuseSerializer(false);
			}

			cachedField.write(fieldOutput, object);
			if (chunked) outputChunked.endChunk();
		}

		popTypeVariables(pop);
	}

	public T read (Kryo kryo, Input input, Class<? extends T> type) {
		int pop = pushTypeVariables();

		T object = create(kryo, input, type);
		kryo.reference(object);

		CachedField[] fields = (CachedField[])kryo.getGraphContext().get(this);
		if (fields == null) fields = readFields(kryo, input);

		boolean chunked = config.chunked, readUnknownTagData = config.readUnknownFieldData;
		Input fieldInput;
		InputChunked inputChunked = null;
		if (chunked)
			fieldInput = inputChunked = new InputChunked(input, config.chunkSize);
		else
			fieldInput = input;
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];

			if (readUnknownTagData) {
				Registration registration;
				try {
					registration = kryo.readClass(fieldInput);
				} catch (KryoException ex) {
					String message = "Unable to read unknown data (unknown type). (" + getType().getName() + "#" + cachedField + ")";
					if (!chunked) throw new KryoException(message, ex);
					if (DEBUG) debug("kryo", message, ex);
					inputChunked.nextChunk();
					continue;
				}
				if (registration == null) {
					if (chunked) inputChunked.nextChunk();
					continue;
				}
				Class valueClass = registration.getType();
				if (cachedField == null) {
					// Read unknown data in case it is a reference.
					if (TRACE) trace("kryo", "Read unknown data, type: " + className(valueClass) + pos(input.position()));
					try {
						kryo.readObject(fieldInput, valueClass);
					} catch (KryoException ex) {
						String message = "Unable to read unknown data, type: " + className(valueClass) + " (" + getType().getName()
							+ "#" + cachedField + ")";
						if (!chunked) throw new KryoException(message, ex);
						if (DEBUG) debug("kryo", message, ex);
					}
					if (chunked) inputChunked.nextChunk();
					continue;
				}

				// Ensure the type in the data is compatible with the field type.
				if (cachedField.valueClass != null && !Util.isAssignableTo(valueClass, cachedField.field.getType())) {
					String message = "Read type is incompatible with the field type: " + className(valueClass) + " -> "
						+ className(cachedField.valueClass) + " (" + getType().getName() + "#" + cachedField + ")";
					if (!chunked) throw new KryoException(message);
					if (DEBUG) debug("kryo", message);
					inputChunked.nextChunk();
					continue;
				}

				cachedField.setCanBeNull(false);
				cachedField.setValueClass(valueClass);
				cachedField.setReuseSerializer(false);
			} else if (cachedField == null) {
				if (!chunked) throw new KryoException("Unknown field. (" + getType().getName() + ")");
				if (TRACE) trace("kryo", "Skip unknown field.");
				inputChunked.nextChunk();
				continue;
			}

			if (TRACE) log("Read", cachedField, input.position());
			cachedField.read(fieldInput, object);
			if (chunked) inputChunked.nextChunk();
		}

		popTypeVariables(pop);
		return object;
	}

	private CachedField[] readFields (Kryo kryo, Input input) {
		if (TRACE) trace("kryo", "Read fields for class: " + type.getName());

		int length = input.readVarInt(true);
		String[] names = new String[length];
		for (int i = 0; i < length; i++) {
			names[i] = input.readString();
			if (TRACE) trace("kryo", "Read field name: " + names[i]);
		}

		CachedField[] fields = new CachedField[length];
		CachedField[] allFields = cachedFields.fields;
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
				if (TRACE) trace("kryo", "Unknown field will be skipped: " + schemaName);
			}
		} else {
			int low, mid, high, compare;
			int lastFieldIndex = allFields.length - 1;
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
				if (TRACE) trace("kryo", "Unknown field will be skipped: " + schemaName);
			}
		}

		kryo.getGraphContext().put(this, fields);
		return fields;
	}

	public CompatibleFieldSerializerConfig getCompatibleFieldSerializerConfig () {
		return config;
	}

	/** Configuration for CompatibleFieldSerializer instances. */
	public static class CompatibleFieldSerializerConfig extends FieldSerializerConfig {
		boolean readUnknownFieldData = true, chunked;
		int chunkSize = 1024;

		public CompatibleFieldSerializerConfig clone () {
			return (CompatibleFieldSerializerConfig)super.clone(); // Clone is ok as we have only primitive fields.
		}

		/** When false and encountering an unknown field, an exception is thrown or, if {@link #setChunkedEncoding(boolean) chunked
		 * encoding} is enabled, the data is skipped.
		 * <p>
		 * When true, the type of each field value is written before the value. When an unknown field is encountered, an attempt to
		 * read the data is made so if it is a reference then any other values in the object graph referencing that data can be
		 * deserialized. If reading the data fails (eg the class is unknown or has been removed) then an exception is thrown or, if
		 * {@link #setChunkedEncoding(boolean) chunked encoding} is enabled, the data is skipped.
		 * <p>
		 * In either case, if the data is skipped and {@link Kryo#setReferences(boolean) references} are enabled, then any
		 * references in the skipped data are not read and further deserialization receive the wrong references and fail.
		 * <p>
		 * Default is true. */
		public void setReadUnknownFieldData (boolean readUnknownTagData) {
			this.readUnknownFieldData = readUnknownTagData;
		}

		public boolean getReadUnknownTagData () {
			return readUnknownFieldData;
		}

		/** When true, fields are written with chunked encoding to allow unknown field data to be skipped. Default is false.
		 * @see #setReadUnknownFieldData(boolean) */
		public void setChunkedEncoding (boolean chunked) {
			this.chunked = chunked;
			if (TRACE) trace("kryo", "CompatibleFieldSerializerConfig setChunked: " + chunked);
		}

		public boolean getChunkedEncoding () {
			return chunked;
		}

		/** The maximum size of each chunk for chunked encoding. Default is 1024. */
		public void setChunkSize (int chunkSize) {
			this.chunkSize = chunkSize;
			if (TRACE) trace("kryo", "CompatibleFieldSerializerConfig setChunkSize: " + chunkSize);
		}

		public int getChunkSize () {
			return chunkSize;
		}
	}
}
