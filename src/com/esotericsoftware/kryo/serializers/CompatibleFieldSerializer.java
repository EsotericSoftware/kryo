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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
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
 * {@link CompatibleFieldSerializerConfig#setExtendedFieldNames(boolean)} must be true, otherwise an exception is thrown.
 * @author Nathan Sweet */
public class CompatibleFieldSerializer<T> extends FieldSerializer<T> {
	private static final int binarySearchThreshold = 32;

	private final CompatibleFieldSerializerConfig config;
	/** The graph context key for the field names written. The fields read are stored with this serializer as key. */
	private final Object writeKey = new Object();
	/** The error message if fields with the same name can't be distinguished, else null. */
	private String duplicateFieldName;
	/** The names of the cached fields in the same order, and as they are written inline. See {@link #updateFieldNames}. */
	private String[] fieldNames;
	private byte[] fieldNameBytes;
	private CachedField[] fieldNamesFields;

	public CompatibleFieldSerializer (Kryo kryo, Class type) {
		this(kryo, type, new CompatibleFieldSerializerConfig());
	}

	public CompatibleFieldSerializer (Kryo kryo, Class type, CompatibleFieldSerializerConfig config) {
		super(kryo, type, config);
		this.config = config;
	}

	boolean usesCodeGeneration () {
		return false;
	}

	void cachedFieldsChanged () {
		// Fields are sorted by name, so fields with the same name are adjacent. The exception is thrown when writing or reading,
		// so the config can still be changed and updateFields called after the serializer is constructed.
		duplicateFieldName = null;
		CachedField[] fields = cachedFields.fields;
		for (int i = 1, n = fields.length; i < n; i++) {
			CachedField field = fields[i], previous = fields[i - 1];
			if (field.name.equals(previous.name)) {
				duplicateFieldName = "Field \"" + field.name + "\" is declared in both "
					+ className(previous.field.getDeclaringClass())
					+ " and " + className(field.field.getDeclaringClass())
					+ ". CompatibleFieldSerializer identifies fields by name, so "
					+ "CompatibleFieldSerializerConfig#setExtendedFieldNames must be true for " + className(type) + ".";
				return;
			}
		}
	}

	/** Field values must be readable without the field, so they don't depend on the field's generic type when
	 * {@link CompatibleFieldSerializerConfig#setReadUnknownFieldData(boolean) readUnknownFieldData} is true, unless
	 * {@link CompatibleFieldSerializerConfig#setOptimizeGenerics(boolean) optimizeGenerics} is set. */
	protected boolean optimizeGenerics () {
		return config.optimizeGenerics || !config.readUnknownFieldData;
	}

	public void updateFields () {
		super.updateFields();
		// The fields may have changed, also if the array was reused. Not in initializeCachedFields, which subclasses can override.
		fieldNamesFields = null;
	}

	public void write (Kryo kryo, Output output, T object) {
		if (duplicateFieldName != null) throw new KryoException(duplicateFieldName);
		boolean readUnknownFieldData = config.readUnknownFieldData;
		ChunkedEncoding chunks = ChunkedEncoding.get(kryo, config.chunked, config.legacyChunks, config.chunkSize);
		boolean chunked = chunks != null;
		Output fieldOutput = chunked ? chunks.beginWrite(output) : output;
		int pop = pushTypeVariables();

		CachedField[] fields = cachedFields.fields;
		ObjectMap context = kryo.getGraphContext();
		if (!context.containsKey(writeKey)) {
			if (TRACE) trace("kryo", "Write fields for class: " + type.getName());
			context.put(writeKey, null);
			updateFieldNames(fields);
			if (!chunked || !chunks.writeFieldNames(type, fieldNames)) {
				if (TRACE) {
					for (String name : fieldNames)
						trace("kryo", "Write field name: " + name + pos(output.position()));
				}
				output.writeBytes(fieldNameBytes);
			}
		}

		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];
			if (TRACE) log("Write", cachedField, fieldOutput.position());
			long mark = chunked ? chunks.beginField(fieldOutput) : 0;

			// Write the value class so the field data can be read even if the field is removed.
			if (readUnknownFieldData) {
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
					if (chunked) chunks.endField(fieldOutput, mark);
					continue;
				}
				cachedField.setCanBeNull(false);
				cachedField.setValueClass(valueClass);
				cachedField.setReuseSerializer(false);
			}

			cachedField.write(fieldOutput, object);
			if (chunked) chunks.endField(fieldOutput, mark);
		}

		popTypeVariables(pop);
		if (chunked) chunks.endWrite();
	}

	public T read (Kryo kryo, Input input, Class<? extends T> type) {
		if (duplicateFieldName != null) throw new KryoException(duplicateFieldName);
		boolean readUnknownFieldData = config.readUnknownFieldData;
		int pop = pushTypeVariables();
		T object = null;
		ChunkedEncoding chunks = null;
		try {
			Object[] values = null;
			if (recordConstructor == null) {
				object = create(kryo, input, type);
				kryo.reference(object);
			} else
				values = newRecordValues();

			// After create, which can read data written before the object, eg by a subclass.
			chunks = ChunkedEncoding.get(kryo, config.chunked, config.legacyChunks, config.chunkSize);
			boolean chunked = chunks != null;
			Input fieldInput = chunked ? chunks.beginRead(input) : input;

			CachedField[] fields = (CachedField[])kryo.getGraphContext().get(this);
			if (fields == null) fields = readFields(kryo, input, chunks);

			for (int i = 0, n = fields.length; i < n; i++) {
				CachedField cachedField = fields[i];
				long end = chunked ? chunks.beginField(fieldInput) : 0;

				if (readUnknownFieldData) {
					Registration registration;
					try {
						registration = kryo.readClass(fieldInput);
					} catch (KryoException ex) {
						String message = "Unable to read unknown data (unknown type). (" + getType().getName() + "#" + cachedField
							+ ")";
						if (!chunked) throw new KryoException(message, ex);
						if (DEBUG) debug("kryo", message, ex);
						chunks.endField(fieldInput, end);
						continue;
					}
					if (registration == null) {
						// The value is null, overwrite the value set by the constructor. Record values are already null.
						if (cachedField != null && object != null) setNull(cachedField, object);
						if (chunked) chunks.endField(fieldInput, end);
						continue;
					}
					Class valueClass = registration.getType();
					if (cachedField == null) {
						if (chunked && config.optimizeGenerics && !config.legacyChunks) {
							// Without the generic type of the removed field, its data can't be read.
							if (TRACE) trace("kryo", "Skip unknown data, type: " + className(valueClass));
							chunks.endField(fieldInput, end);
							continue;
						}
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
						if (chunked) chunks.endField(fieldInput, end);
						continue;
					}

					// Ensure the type in the data is compatible with the field type.
					Class fieldType = cachedField.field.getType();
					if (!Util.isAssignableTo(valueClass, fieldType)) {
						String message = "Read type is incompatible with the field type: " + className(valueClass) + " -> "
							+ className(fieldType) + " (" + getType().getName() + "#" + cachedField + ")";
						if (!chunked) throw new KryoException(message);
						if (DEBUG) debug("kryo", message);
						chunks.endField(fieldInput, end);
						continue;
					}

					cachedField.setCanBeNull(false);
					cachedField.setValueClass(valueClass);
					cachedField.setReuseSerializer(false);
				} else if (cachedField == null) {
					if (!chunked) throw new KryoException("Unknown field. (" + getType().getName() + ")");
					if (TRACE) trace("kryo", "Skip unknown field.");
					chunks.endField(fieldInput, end);
					continue;
				}

				if (TRACE) log("Read", cachedField, input.position());
				if (values == null)
					readField(cachedField, fieldInput, object);
				else
					values[cachedField.index] = cachedField.read(fieldInput);
				if (chunked) chunks.endField(fieldInput, end);
			}

			if (values != null) object = createRecord(values);
		} finally {
			// Also after an exception, which is caught if reading an unknown field fails.
			popTypeVariables(pop);
			if (chunks != null) chunks.endRead();
		}
		return object;
	}

	/** Updates the field names if the fields changed, see {@link #updateFields()} and removeField, which creates a new array. The
	 * bytes are the number of fields, then the names. All outputs of Kryo write varints and strings the same way, so the bytes can
	 * be written to any output. */
	private void updateFieldNames (CachedField[] fields) {
		if (fieldNamesFields == fields) return;
		String[] names = new String[fields.length];
		Output output = new Output(64, -1);
		output.writeVarInt(fields.length, true);
		for (int i = 0, n = fields.length; i < n; i++) {
			names[i] = fields[i].name;
			output.writeString(names[i]);
		}
		fieldNames = names;
		fieldNameBytes = output.toBytes();
		fieldNamesFields = fields;
	}

	/** Returns the fields of the data for the current object graph, from the chunked encoding or read from the input. */
	private CachedField[] readFields (Kryo kryo, Input input, ChunkedEncoding chunks) {
		String[] names = chunks != null ? chunks.readFieldNames(type) : null;
		if (names != null) return fields(kryo, names);

		// The field names are usually the ones this serializer writes, which can be compared without reading them.
		CachedField[] allFields = cachedFields.fields;
		if (!(input instanceof ByteBufferInput) && allFields.length <= input.getMaxArraySize()) {
			updateFieldNames(allFields);
			byte[] bytes = fieldNameBytes;
			int position = input.position();
			if (input.limit() - position >= bytes.length && rangeEquals(input.getBuffer(), position, bytes, 0, bytes.length)) {
				input.setPosition(position + bytes.length);
				if (TRACE) {
					trace("kryo", "Read fields for class: " + type.getName());
					for (String name : fieldNames)
						trace("kryo", "Read field name: " + name);
				}
				kryo.getGraphContext().put(this, allFields);
				return allFields;
			}
		}
		return fields(kryo, readFieldNames(input));
	}

	private String[] readFieldNames (Input input) {
		String[] names = new String[input.validateArrayLength(input.readVarInt(true))];
		for (int i = 0; i < names.length; i++) {
			names[i] = input.readString();
			if (TRACE) trace("kryo", "Read field name: " + names[i]);
		}
		return names;
	}

	/** Returns the fields of the data for the current object graph from their names, which can contain unknown fields. */
	private CachedField[] fields (Kryo kryo, String[] names) {
		if (TRACE) trace("kryo", "Read fields for class: " + type.getName());
		CachedField[] allFields = cachedFields.fields;
		int length = names.length;
		CachedField[] fields = new CachedField[length];
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
		boolean readUnknownFieldData = true, chunked, legacyChunks, optimizeGenerics;
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
		 * If the data is skipped and {@link Kryo#setReferences(boolean) references} are enabled, references to objects in the
		 * skipped data are read as null. With {@link #setLegacyChunks(boolean) the chunked encoding of Kryo 5}, or a custom
		 * ReferenceResolver without {@link com.esotericsoftware.kryo.ReferenceResolver#getObjectCount() getObjectCount}, references
		 * in skipped data are not read and further deserialization receives the wrong references and fails.
		 * <p>
		 * Default is true. */
		public void setReadUnknownFieldData (boolean readUnknownTagData) {
			this.readUnknownFieldData = readUnknownTagData;
		}

		public boolean getReadUnknownTagData () {
			return readUnknownFieldData;
		}

		/** When true, fields are written with chunked encoding to allow unknown field data to be skipped, eg when the class of a
		 * removed field no longer exists. Each field is written with its length. The outermost object with chunked encoding is
		 * buffered until it is written completely, it is not streamed. This impacts performance. Default is false.
		 * @see #setReadUnknownFieldData(boolean)
		 * @see #setLegacyChunks(boolean) */
		public void setChunkedEncoding (boolean chunked) {
			this.chunked = chunked;
			if (TRACE) trace("kryo", "CompatibleFieldSerializerConfig setChunked: " + chunked);
		}

		public boolean getChunkedEncoding () {
			return chunked;
		}

		/** When true, {@link #setChunkedEncoding(boolean) chunked encoding} uses the format of Kryo 5, which splits each field into
		 * chunks. If a chunk is skipped, class names, field names, and objects first written in it are unknown afterwards, which
		 * can make reading the rest of the data fail. Kryo 6 writes each field with its length instead, and the class names and
		 * field names first written in an object before it. Must be true to read data written by Kryo 5 with chunked encoding.
		 * Default is false.
		 * @deprecated Only needed to read data written by Kryo 5, see {@link com.esotericsoftware.kryo.Kryo5Compatibility}. Will be
		 *             removed in Kryo 7. */
		@Deprecated
		public void setLegacyChunks (boolean legacyChunks) {
			this.legacyChunks = legacyChunks;
			if (TRACE) trace("kryo", "CompatibleFieldSerializerConfig setLegacyChunks: " + legacyChunks);
		}

		/** @deprecated See {@link #setLegacyChunks(boolean)}. */
		@Deprecated
		public boolean getLegacyChunks () {
			return legacyChunks;
		}

		/** The maximum size of each chunk for {@link #setLegacyChunks(boolean) the chunked encoding of Kryo 5}. Default is 1024.
		 * @deprecated Only needed with {@link #setLegacyChunks(boolean)}. Will be removed in Kryo 7. */
		@Deprecated
		public void setChunkSize (int chunkSize) {
			this.chunkSize = chunkSize;
			if (TRACE) trace("kryo", "CompatibleFieldSerializerConfig setChunkSize: " + chunkSize);
		}

		/** @deprecated See {@link #setChunkSize(int)}. */
		@Deprecated
		public int getChunkSize () {
			return chunkSize;
		}

		/** When true, the generic type of a field is used to optimize its value, eg to omit the class of collection elements, even
		 * if {@link #setReadUnknownFieldData(boolean) readUnknownFieldData} is true. Then the value can't be read anymore once the
		 * field is removed: an exception is thrown or, if chunked encoding is enabled, the data is skipped. Default is false.
		 * @deprecated Only needed to read data written by Kryo 5, see {@link com.esotericsoftware.kryo.Kryo5Compatibility}. The
		 *             collection and map serializers write the class of the elements once per collection anyway, so the
		 *             optimization saves almost nothing. Will be removed in Kryo 7. */
		@Deprecated
		public void setOptimizeGenerics (boolean optimizeGenerics) {
			this.optimizeGenerics = optimizeGenerics;
			if (TRACE) trace("kryo", "CompatibleFieldSerializerConfig setOptimizeGenerics: " + optimizeGenerics);
		}

		/** @deprecated See {@link #setOptimizeGenerics(boolean)}. */
		@Deprecated
		public boolean getOptimizeGenerics () {
			return optimizeGenerics;
		}
	}
}
