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
import com.esotericsoftware.kryo.util.IntMap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;

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
	private CachedField[] writeTags;
	private IntMap<CachedField> readTags;
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

		// Cache tag values.
		ArrayList writeTags = new ArrayList(fields.length);
		readTags = new IntMap((int)(fields.length / 0.8f));
		for (CachedField cachedField : fields) {
			Field field = cachedField.field;
			int tag = field.getAnnotation(Tag.class).value();
			if (readTags.containsKey(tag))
				throw new KryoException(String.format("Duplicate tag %d on fields: %s and %s", tag, field, writeTags.get(tag)));
			readTags.put(tag, cachedField);
			if (field.getAnnotation(Deprecated.class) == null) writeTags.add(cachedField);
			cachedField.tag = tag;
		}
		this.writeTags = (CachedField[])writeTags.toArray(new CachedField[writeTags.size()]);
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

		CachedField[] writeTags = this.writeTags;
		output.writeVarInt(writeTags.length + 1, true);
		writeHeader(kryo, output, object);

		boolean chunked = config.chunked, readUnknownTagData = config.readUnknownTagData;
		Output fieldOutput;
		OutputChunked outputChunked = null;
		if (chunked)
			fieldOutput = outputChunked = new OutputChunked(output, config.chunkSize);
		else
			fieldOutput = output;

		for (int i = 0, n = writeTags.length; i < n; i++) {
			CachedField cachedField = writeTags[i];
			if (TRACE) log("Write", cachedField, output.position());
			output.writeVarInt(cachedField.tag, true);

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

		boolean chunked = config.chunked, readUnknownTagData = config.readUnknownTagData;
		Input fieldInput;
		InputChunked inputChunked = null;
		if (chunked)
			fieldInput = inputChunked = new InputChunked(input, config.chunkSize);
		else
			fieldInput = input;
		IntMap<CachedField> readTags = this.readTags;
		for (int i = 0; i < fieldCount; i++) {
			int tag = input.readVarInt(true);
			CachedField cachedField = readTags.get(tag);

			if (readUnknownTagData) {
				Registration registration;
				try {
					registration = kryo.readClass(fieldInput);
				} catch (KryoException ex) {
					String message = "Unable to read unknown tag " + tag + " data (unknown type). (" + getType().getName() + "#"
						+ cachedField + ")";
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
					// Read unknown tag data in case it is a reference.
					if (TRACE) trace("kryo", "Read unknown tag " + tag + " data, type: " + className(valueClass));
					try {
						kryo.readObject(fieldInput, valueClass);
					} catch (KryoException ex) {
						String message = "Unable to read unknown tag " + tag + " data, type: " + className(valueClass) + " ("
							+ getType().getName() + "#" + cachedField + ")";
						if (!chunked) throw new KryoException(message, ex);
						if (DEBUG) debug("kryo", message, ex);
					}
					if (chunked) inputChunked.nextChunk();
					continue;
				}
				cachedField.setCanBeNull(false);
				cachedField.setValueClass(valueClass);
				cachedField.setReuseSerializer(false);
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

		popTypeVariables(pop);
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
	public static class TaggedFieldSerializerConfig extends FieldSerializerConfig {
		boolean readUnknownTagData, chunked;
		int chunkSize = 1024;

		public TaggedFieldSerializerConfig clone () {
			return (TaggedFieldSerializerConfig)super.clone(); // Clone is ok as we have only primitive fields.
		}

		/** When false and encountering an unknown tag, an exception is thrown or, if {@link #setChunkedEncoding(boolean) chunked
		 * encoding} is enabled, the data is skipped.
		 * <p>
		 * When true, the type of each field value is written before the value. When an unknown tag is encountered, an attempt to
		 * read the data is made. This is used to skip the data and, if {@link Kryo#setReferences(boolean) references} are enabled,
		 * then any other values in the object graph referencing that data can still be deserialized. If reading the data fails (eg
		 * the class is unknown or has been removed) then an exception is thrown or, if {@link #setChunkedEncoding(boolean) chunked
		 * encoding} is enabled, the data is skipped.
		 * <p>
		 * In either case, if the data is skipped and {@link Kryo#setReferences(boolean) references} are enabled, then any
		 * references in the skipped data are not read and further deserialization receive the wrong references and fail.
		 * <p>
		 * Default is false. */
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
