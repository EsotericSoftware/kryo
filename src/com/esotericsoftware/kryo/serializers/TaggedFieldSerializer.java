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

import static com.esotericsoftware.kryo.Kryo.*;
import static com.esotericsoftware.kryo.util.Util.*;
import static com.esotericsoftware.minlog.Log.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.InputChunked;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.OutputChunked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;

/** Serializes objects using direct field assignment for fields that have a <code>@Tag(int)</code> annotation, providing backward
 * compatibility and optional forward compatibility. This means fields can be added or renamed and optionally removed without
 * invalidating previously serialized bytes. Changing the type of a field is not supported.
 * <p>
 * Fields are identified by the {@link Tag} annotation. Fields can be renamed without affecting serialization. Field tag values
 * must be unique, both within a class and all its super classes. An exception is thrown if duplicate tag values are encountered.
 * <p>
 * The forward and backward compatibility and serialization performance depend on
 * {@link TaggedFieldSerializerConfig#setReadUnknownTagData(boolean)} and
 * {@link TaggedFieldSerializerConfig#setChunkedEncoding(boolean)}. Additionally, a varint is written before each field for the
 * tag value.
 * <p>
 * If <code>readUnknownTagData</code> and <code>chunkedEncoding</code> are false, fields must not be removed but the
 * {@link Deprecated} annotation can be applied. Deprecated fields are read when reading old bytes but aren't written to new
 * bytes. Classes can evolve by reading the values of deprecated fields and writing them elsewhere. Fields can be renamed and/or
 * made private to reduce clutter in the class (eg, <code>ignored1</code>, <code>ignored2</code>).
 * <p>
 * Compared to {@link VersionFieldSerializer}, TaggedFieldSerializer allows renaming and deprecating fields, so has more
 * flexibility for classes to evolve. This comes at the cost of one varint per field.
 * @author Nathan Sweet */
public class TaggedFieldSerializer<T> extends FieldSerializer<T> {
	static private final Comparator<CachedField> tagComparator = new Comparator<CachedField>() {
		public int compare (CachedField o1, CachedField o2) {
			return o1.field.getAnnotation(Tag.class).value() - o2.field.getAnnotation(Tag.class).value();
		}
	};

	private int[] tags;
	private int writeFieldCount;
	private boolean[] deprecated;
	private final TaggedFieldSerializerConfig config;

	public TaggedFieldSerializer (Kryo kryo, Class type) {
		this(kryo, type, new TaggedFieldSerializerConfig());
	}

	public TaggedFieldSerializer (Kryo kryo, Class type, TaggedFieldSerializerConfig config) {
		super(kryo, type, config);
		this.config = config;
		setAcceptsNull(true);
	}

	protected void initializeCachedFields () {
		CachedField[] fields = cachedFields.fields;
		// Remove untagged fields.
		for (int i = 0, n = fields.length; i < n; i++) {
			Field field = fields[i].field;
			if (field.getAnnotation(Tag.class) == null) {
				if (TRACE) trace("kryo", "Ignoring field without tag: " + fields[i]);
				super.removeField(fields[i]);
			}
		}
		fields = cachedFields.fields; // removeField changes cached field array.
		Arrays.sort(fields, tagComparator); // fields are sorted to easily check for reused tag values

		// Cache tag values.
		int n = fields.length;
		writeFieldCount = n;
		tags = new int[n];
		deprecated = new boolean[n];
		for (int i = 0; i < n; i++) {
			Field field = fields[i].field;
			tags[i] = field.getAnnotation(Tag.class).value();
			if (i > 0 && tags[i] == tags[i - 1]) // Relies on fields having been sorted.
				throw new KryoException("Duplicate tag " + tags[i] + " on fields: " + field + " and " + fields[i - 1].field);
			if (field.getAnnotation(Deprecated.class) != null) {
				deprecated[i] = true;
				writeFieldCount--;
			}
		}
	}

	public void removeField (String fieldName) {
		super.removeField(fieldName);
		initializeCachedFields();
	}

	public void removeField (CachedField field) {
		super.removeField(field);
		initializeCachedFields();
	}

	public void write (Kryo kryo, Output output, T object) {
		if (object == null) {
			output.writeByte(NULL);
			return;
		}

		int pop = pushTypeVariables();

		output.writeVarInt(writeFieldCount + 1, true);
		writeHeader(kryo, output, object);

		CachedField[] fields = cachedFields.fields;

		boolean chunked = config.chunked, readUnknownTagData = config.readUnknownTagData;
		Output fieldOutput;
		OutputChunked outputChunked = null;
		if (chunked)
			fieldOutput = outputChunked = new OutputChunked(output, config.chunkSize);
		else
			fieldOutput = output;
		int[] tags = this.tags;
		boolean[] deprecated = this.deprecated;
		for (int i = 0, n = fields.length; i < n; i++) {
			if (deprecated[i]) continue;
			CachedField cachedField = fields[i];

			if (TRACE) log("Write", fields[i], output.position());
			output.writeVarInt(tags[i], true);

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
			}

			cachedField.write(fieldOutput, object);
			if (chunked) outputChunked.endChunk();
		}

		if (pop > 0) popTypeVariables(pop);
	}

	/** Can be overidden to write data needed for {@link #create(Kryo, Input, Class)}. The default implementation does nothing. */
	protected void writeHeader (Kryo kryo, Output output, T object) {
	}

	public T read (Kryo kryo, Input input, Class<? extends T> type) {
		int fieldCount = input.readVarInt(true);
		if (fieldCount == NULL) return null;
		fieldCount--;

		int pop = pushTypeVariables();

		T object = create(kryo, input, type);
		kryo.reference(object);

		CachedField[] fields = cachedFields.fields;

		boolean chunked = config.chunked, readUnknownTagData = config.readUnknownTagData;
		Input fieldInput;
		InputChunked inputChunked = null;
		if (chunked)
			fieldInput = inputChunked = new InputChunked(input, config.chunkSize);
		else
			fieldInput = input;
		int[] tags = this.tags;
		for (int i = 0, n = fieldCount; i < n; i++) {
			int tag = input.readVarInt(true);
			CachedField cachedField = null;
			for (int ii = 0, nn = tags.length; ii < nn; ii++) {
				if (tags[ii] == tag) {
					cachedField = fields[ii];
					break;
				}
			}

			if (readUnknownTagData) {
				Registration registration;
				try {
					registration = kryo.readClass(fieldInput);
				} catch (KryoException ex) {
					if (!chunked) throw new KryoException(
						"Unable to read unknown tag " + tag + " data (unknown type). (" + getType().getName() + ")", ex);
					if (DEBUG) debug("kryo", "Unable to read unknown tag " + tag + " data (unknown type).", ex);
					inputChunked.nextChunk();
					continue;
				}
				if (registration == null) {
					if (chunked) inputChunked.nextChunk();
					continue;
				}
				Class valueClass = registration.getType();
				if (cachedField == null) {
					// Read unknown tag data in case it is a reference.
					if (TRACE) trace("kryo", "Read unknown tag " + tag + " data, type: " + className(valueClass));
					try {
						kryo.readObject(fieldInput, valueClass);
					} catch (KryoException ex) {
						if (!chunked)
							throw new KryoException("Unable to read unknown tag " + tag + " data, type: " + className(valueClass), ex);
						if (DEBUG) debug("kryo", "Unable to read unknown tag " + tag + " data, type: " + className(valueClass), ex);
					}
					if (chunked) inputChunked.nextChunk();
					continue;
				}
				cachedField.setCanBeNull(false);
				cachedField.setValueClass(valueClass);
			} else if (cachedField == null) {
				if (!chunked) throw new KryoException("Unknown field tag: " + tag + " (" + getType().getName() + ")");
				if (TRACE) trace("kryo", "Skip unknown field tag: " + tag);
				inputChunked.nextChunk();
				continue;
			}

			if (TRACE) log("Read", cachedField, input.position());
			cachedField.read(fieldInput, object);
			if (chunked) inputChunked.nextChunk();
		}

		if (pop > 0) popTypeVariables(pop);
		return object;
	}

	public TaggedFieldSerializerConfig getTaggedFieldSerializerConfig () {
		return config;
	}

	/** Marks a field for serialization. */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Tag {
		int value();
	}

	/** Configuration for TaggedFieldSerializer instances. */
	static public class TaggedFieldSerializerConfig extends FieldSerializerConfig {
		boolean readUnknownTagData, chunked;
		int chunkSize = 1024;

		public TaggedFieldSerializerConfig clone () {
			return (TaggedFieldSerializerConfig)super.clone(); // Clone is ok as we have only primitive fields.
		}

		/** When false and encountering an unknown tag, an exception is thrown or, if {@link #setChunkedEncoding(boolean) chunked
		 * encoding} is enabled, the data is skipped. If {@link Kryo#setReferences(boolean) references} are enabled, then any other
		 * values in the object graph referencing that data cannot be deserialized. Default is false.
		 * <p>
		 * When true, the type of each field value is written before the value. When an unknown tag is encountered, an attempt to
		 * read the data is made. This is used to skip the data and, if {@link Kryo#setReferences(boolean) references} are enabled,
		 * then any other values in the object graph referencing that data can still be deserialized. If reading the data fails (eg
		 * the class is unknown or has been removed) then an exception is thrown or, if {@link #setChunkedEncoding(boolean) chunked
		 * encoding} is enabled, the data is skipped. */
		public void setReadUnknownTagData (boolean readUnknownTagData) {
			this.readUnknownTagData = readUnknownTagData;
		}

		public boolean getReadUnknownTagData () {
			return readUnknownTagData;
		}

		/** When true, fields are written with chunked encoding to allow unknown field data to be skipped. This impacts performance.
		 * @see #setReadUnknownTagData(boolean) */
		public void setChunkedEncoding (boolean chunked) {
			this.chunked = chunked;
			if (TRACE) trace("kryo", "TaggedFieldSerializerConfig setChunked: " + chunked);
		}

		public boolean getChunkedEncoding () {
			return chunked;
		}

		/** The maximum size of each chunk for chunked encoding. Default is 1024. */
		public void setChunkSize (int chunkSize) {
			this.chunkSize = chunkSize;
			if (TRACE) trace("kryo", "TaggedFieldSerializerConfig setChunkSize: " + chunkSize);
		}

		public int getChunkSize () {
			return chunkSize;
		}
	}
}
