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

import static com.esotericsoftware.kryo.Kryo.*;
import static com.esotericsoftware.kryo.util.Util.*;
import static com.esotericsoftware.minlog.Log.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.IntMap;
import com.esotericsoftware.kryo.util.Util;

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
	}

	/** Generated code is used except with the chunked encoding of Kryo 5, which writes the tags outside the chunks. */
	boolean usesCodeGeneration () {
		// The super class config, because this is called by the super constructor.
		return !((TaggedFieldSerializerConfig)super.config).legacyChunks;
	}

	GeneratedFields generateCode () {
		// The super class config, because this is called by the super constructor.
		TaggedFieldSerializerConfig config = (TaggedFieldSerializerConfig)super.config;
		int[] tags = new int[writeTags.length];
		for (int i = 0; i < tags.length; i++)
			tags[i] = writeTags[i].tag;
		return GeneratedFields.generate(this, writeTags, config.readUnknownTagData, tags);
	}

	GeneratedFields generated () {
		GeneratedFields generated = this.generated;
		if (generated == null || config.legacyChunks) return null;
		// readUnknownTagData can be changed without updateFields.
		if (generated.writesClasses != config.readUnknownTagData) {
			regenerate();
			return this.generated;
		}
		return generated;
	}

	void cachedFieldsChanged () {
		// Cache tag values.
		CachedField[] fields = cachedFields.fields;
		ArrayList writeTags = new ArrayList(fields.length);
		readTags = new IntMap((int)(fields.length / 0.8f));
		for (CachedField cachedField : fields) {
			Field field = cachedField.field;
			int tag = field.getAnnotation(Tag.class).value();
			if (readTags.containsKey(tag))
				throw new KryoException(String.format("Duplicate tag %d on fields: %s and %s", tag, field, readTags.get(tag).field));
			readTags.put(tag, cachedField);
			if (field.getAnnotation(Deprecated.class) == null) writeTags.add(cachedField);
			cachedField.tag = tag;
		}
		this.writeTags = (CachedField[])writeTags.toArray(new CachedField[writeTags.size()]);
	}

	/** Field values must be readable without the field, so they don't depend on the field's generic type when
	 * {@link TaggedFieldSerializerConfig#setReadUnknownTagData(boolean) readUnknownTagData} is true, unless
	 * {@link TaggedFieldSerializerConfig#setOptimizeGenerics(boolean) optimizeGenerics} is set. */
	protected boolean optimizeGenerics () {
		return config.optimizeGenerics || !config.readUnknownTagData;
	}

	public void write (Kryo kryo, Output output, T object) {
		if (object == null) {
			output.writeByte(NULL);
			return;
		}

		CachedField[] writeTags = this.writeTags;
		output.writeVarInt(writeTags.length + 1, true);
		boolean readUnknownTagData = config.readUnknownTagData;
		ChunkedEncoding chunks = ChunkedEncoding.get(kryo, config.chunked, config.legacyChunks, config.chunkSize);
		boolean chunked = chunks != null;
		Output fieldOutput = chunked ? chunks.beginWrite(output) : output;
		// The chunked encoding of Kryo 5 writes the tags and the header outside the chunks.
		Output objectOutput = config.legacyChunks ? output : fieldOutput;
		int pop = pushTypeVariables();
		writeHeader(kryo, objectOutput, object);

		if (generated() != null) {
			writeGenerated(fieldOutput, object, chunks);
			popTypeVariables(pop);
			if (chunked) chunks.endWrite();
			return;
		}

		for (int i = 0, n = writeTags.length; i < n; i++) {
			CachedField cachedField = writeTags[i];
			if (TRACE) log("Write", cachedField, objectOutput.position());
			objectOutput.writeVarInt(cachedField.tag, true);
			long mark = chunked ? chunks.beginField(fieldOutput) : 0;

			// Write the value class so the field data can be read even if the field is removed.
			if (readUnknownTagData) {
				Class valueClass = null;
				try {
					Object value = cachedField.field.get(object);
					if (value != null) valueClass = value.getClass();
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

	/** Can be overidden to write data needed for {@link #create(Kryo, Input, Class)}. The default implementation does nothing. */
	protected void writeHeader (Kryo kryo, Output output, T object) {
	}

	public T read (Kryo kryo, Input input, Class<? extends T> type) {
		int fieldCount = input.readVarInt(true);
		if (fieldCount == NULL) return null;
		fieldCount--;

		boolean readUnknownTagData = config.readUnknownTagData;
		ChunkedEncoding chunks = ChunkedEncoding.get(kryo, config.chunked, config.legacyChunks, config.chunkSize);
		boolean chunked = chunks != null;
		int pop = pushTypeVariables();
		T object = null;
		try {
			// In the try block, so the read is ended if the data first written in the object can't be read.
			Input fieldInput = chunked ? chunks.beginRead(input) : input;
			Object[] values = null;
			if (recordConstructor == null) {
				object = create(kryo, input, type);
				kryo.reference(object);
			} else
				values = newRecordValues();

			// The generated code reads the tags this serializer writes, in its order, and falls back to readTag for other tags.
			if (values == null && fieldCount == writeTags.length && generated() != null) {
				readGenerated(fieldInput, object, chunks);
				return object;
			}

			IntMap<CachedField> readTags = this.readTags;
			for (int i = 0; i < fieldCount; i++) {
				int tag = input.readVarInt(true);
				CachedField cachedField = readTags.get(tag);
				long end = chunked ? chunks.beginField(fieldInput) : 0;

				if (readUnknownTagData) {
					Registration registration;
					try {
						registration = kryo.readClass(fieldInput);
					} catch (KryoException ex) {
						String message = "Unable to read unknown tag " + tag + " data (unknown type). (" + getType().getName() + "#"
							+ cachedField + ")";
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
							if (TRACE) trace("kryo", "Skip unknown tag " + tag + " data, type: " + className(valueClass));
							chunks.endField(fieldInput, end);
							continue;
						}
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
					if (!chunked) throw new KryoException("Unknown field tag: " + tag + " (" + getType().getName() + ")");
					if (TRACE) trace("kryo", "Skip unknown field tag: " + tag);
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
			if (chunked) chunks.endRead();
		}
		return object;
	}

	/** Reads the field for a tag, like {@link #read(Kryo, Input, Class)} does for one entry, inside the chunk if chunked. Called
	 * by the generated code when the tag is not the expected one.
	 * @param chunked If true, the data is skipped instead of throwing an exception, by the caller's endField. */
	void readTag (Input input, int tag, Object object, boolean chunked) {
		CachedField cachedField = readTags.get(tag);
		if (config.readUnknownTagData) {
			Registration registration;
			try {
				registration = kryo.readClass(input);
			} catch (KryoException ex) {
				String message = "Unable to read unknown tag " + tag + " data (unknown type). (" + getType().getName() + "#"
					+ cachedField
					+ ")";
				if (!chunked) throw new KryoException(message, ex);
				if (DEBUG) debug("kryo", message, ex);
				return;
			}
			if (registration == null) {
				if (cachedField != null) setNull(cachedField, object);
				return;
			}
			Class valueClass = registration.getType();
			if (cachedField == null) {
				if (chunked && config.optimizeGenerics) {
					// Without the generic type of the removed field, its data can't be read.
					if (TRACE) trace("kryo", "Skip unknown tag " + tag + " data, type: " + className(valueClass));
					return;
				}
				// Read unknown tag data in case it is a reference.
				if (TRACE) trace("kryo", "Read unknown tag " + tag + " data, type: " + className(valueClass));
				try {
					kryo.readObject(input, valueClass);
				} catch (KryoException ex) {
					String message = "Unable to read unknown tag " + tag + " data, type: " + className(valueClass) + " ("
						+ getType().getName() + "#" + cachedField + ")";
					if (!chunked) throw new KryoException(message, ex);
					if (DEBUG) debug("kryo", message, ex);
				}
				return;
			}

			// Ensure the type in the data is compatible with the field type.
			Class fieldType = cachedField.field.getType();
			if (!Util.isAssignableTo(valueClass, fieldType)) {
				String message = "Read type is incompatible with the field type: " + className(valueClass) + " -> "
					+ className(fieldType) + " (" + getType().getName() + "#" + cachedField + ")";
				if (!chunked) throw new KryoException(message);
				if (DEBUG) debug("kryo", message);
				return;
			}

			cachedField.setCanBeNull(false);
			cachedField.setValueClass(valueClass);
			cachedField.setReuseSerializer(false);
		} else if (cachedField == null) {
			if (!chunked) throw new KryoException("Unknown field tag: " + tag + " (" + getType().getName() + ")");
			if (TRACE) trace("kryo", "Skip unknown field tag: " + tag);
			return;
		}

		if (TRACE) log("Read", cachedField, input.position());
		readField(cachedField, input, object);
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
		boolean readUnknownTagData, chunked, legacyChunks, optimizeGenerics;
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
		 * If the data is skipped and {@link Kryo#setReferences(boolean) references} are enabled, references to objects in the
		 * skipped data are read as null. With {@link #setLegacyChunks(boolean) the chunked encoding of Kryo 5}, or a custom
		 * ReferenceResolver without {@link com.esotericsoftware.kryo.ReferenceResolver#getObjectCount() getObjectCount}, references
		 * in skipped data are not read and further deserialization receives the wrong references and fails.
		 * <p>
		 * Default is false. */
		public void setReadUnknownTagData (boolean readUnknownTagData) {
			this.readUnknownTagData = readUnknownTagData;
		}

		public boolean getReadUnknownTagData () {
			return readUnknownTagData;
		}

		/** When true, fields are written with chunked encoding to allow unknown field data to be skipped, eg when the class of a
		 * removed field no longer exists. Each field is written with its length. The outermost object with chunked encoding is
		 * buffered until it is written completely, it is not streamed. This impacts performance. Default is false.
		 * @see #setReadUnknownTagData(boolean)
		 * @see #setLegacyChunks(boolean) */
		public void setChunkedEncoding (boolean chunked) {
			this.chunked = chunked;
			if (TRACE) trace("kryo", "TaggedFieldSerializerConfig setChunked: " + chunked);
		}

		public boolean getChunkedEncoding () {
			return chunked;
		}

		/** When true, {@link #setChunkedEncoding(boolean) chunked encoding} uses the format of Kryo 5, see
		 * {@link CompatibleFieldSerializer.CompatibleFieldSerializerConfig#setLegacyChunks(boolean)}. Must be true to read data
		 * written by Kryo 5 with chunked encoding. Default is false.
		 * @deprecated Only needed to read data written by Kryo 5, see {@link com.esotericsoftware.kryo.Kryo5Compatibility}. Will be
		 *             removed in Kryo 7. */
		@Deprecated
		public void setLegacyChunks (boolean legacyChunks) {
			this.legacyChunks = legacyChunks;
			if (TRACE) trace("kryo", "TaggedFieldSerializerConfig setLegacyChunks: " + legacyChunks);
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
			if (TRACE) trace("kryo", "TaggedFieldSerializerConfig setChunkSize: " + chunkSize);
		}

		/** @deprecated See {@link #setChunkSize(int)}. */
		@Deprecated
		public int getChunkSize () {
			return chunkSize;
		}

		/** When true, the generic type of a field is used to optimize its value, eg to omit the class of collection elements, even
		 * if {@link #setReadUnknownTagData(boolean) readUnknownTagData} is true. Then the value can't be read anymore once the
		 * field is removed: an exception is thrown or, if chunked encoding is enabled, the data is skipped. Default is false.
		 * @deprecated Only needed to read data written by Kryo 5, see {@link com.esotericsoftware.kryo.Kryo5Compatibility}. The
		 *             collection and map serializers write the class of the elements once per collection anyway, so the
		 *             optimization saves almost nothing. Will be removed in Kryo 7. */
		@Deprecated
		public void setOptimizeGenerics (boolean optimizeGenerics) {
			this.optimizeGenerics = optimizeGenerics;
			if (TRACE) trace("kryo", "TaggedFieldSerializerConfig setOptimizeGenerics: " + optimizeGenerics);
		}

		/** @deprecated See {@link #setOptimizeGenerics(boolean)}. */
		@Deprecated
		public boolean getOptimizeGenerics () {
			return optimizeGenerics;
		}
	}
}
