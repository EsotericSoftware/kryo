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

import static com.esotericsoftware.minlog.Log.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.InputChunked;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.OutputChunked;

/** Serializes objects using direct field assignment for fields that have a <code>@Tag(int)</code> annotation. This provides
 * backward compatibility so new fields can be added. TaggedFieldSerializer has two advantages over {@link VersionFieldSerializer}
 * : 1) fields can be renamed and 2) fields marked with the <code>@Deprecated</code> annotation will be read when reading old
 * bytes but won't be written to new bytes. Deprecation effectively removes the field from serialization, though the field and
 * <code>@Tag</code> annotation must remain in the class. Deprecated fields can optionally be made private and/or renamed so they
 * don't clutter the class (eg, <code>ignored</code>, <code>ignored2</code>). For these reasons, TaggedFieldSerializer generally
 * provides more flexibility for classes to evolve. The downside is that it has a small amount of additional overhead compared to
 * VersionFieldSerializer (an additional varint per field).
 * <p>
 * Forward compatibility is optionally supported by enabling {@link TaggedFieldSerializerConfig#setSkipUnknownTags(boolean)},
 * which allows it to skip reading unknown tagged fields, which are presumably new fields added in future versions of an
 * application. The data is only forward compatible for fields that have {@link TaggedFieldSerializer.Tag#annexed()} set to true,
 * which comes with the cost of chunked encoding: when annexed fields are encountered during the read or write process of an
 * object, a buffer is allocated to perform the chunked encoding.
 * <p>
 * Tag values must be entirely unique, both within a class and all its superclasses. An IllegalArgumentException will be thrown by
 * {@link Kryo#register(Class)} (and its overloads) if duplicate tag values are encountered.
 * @see VersionFieldSerializer
 * @author Nathan Sweet */
public class TaggedFieldSerializer<T> extends FieldSerializer<T> {
	static private final Comparator<CachedField> tagComparator = new Comparator<CachedField>() {
		public int compare (CachedField o1, CachedField o2) {
			return o1.getField().getAnnotation(Tag.class).value() - o2.getField().getAnnotation(Tag.class).value();
		}
	};

	private int[] tags;
	private int writeFieldCount;
	private boolean[] deprecated, annexed;
	private final TaggedFieldSerializerConfig config;

	public TaggedFieldSerializer (Kryo kryo, Class type) {
		this(kryo, type, new TaggedFieldSerializerConfig());
	}

	public TaggedFieldSerializer (Kryo kryo, Class type, TaggedFieldSerializerConfig config) {
		super(kryo, type, config);
		this.config = config;
	}

	protected void initializeCachedFields () {
		CachedField[] fields = getFields();
		// Remove untagged fields.
		for (int i = 0, n = fields.length; i < n; i++) {
			Field field = fields[i].getField();
			if (field.getAnnotation(Tag.class) == null) {
				if (TRACE) trace("kryo", "Ignoring field without tag: " + fields[i]);
				super.removeField(fields[i]);
			}
		}
		fields = getFields(); // removeField changes cached field array.
		Arrays.sort(fields, tagComparator); // fields are sorted to easily check for reused tag values

		// Cache tag values.
		int n = fields.length;
		writeFieldCount = n;
		tags = new int[n];
		deprecated = new boolean[n];
		annexed = new boolean[n];
		for (int i = 0; i < n; i++) {
			Field field = fields[i].getField();
			tags[i] = field.getAnnotation(Tag.class).value();
			if (i > 0 && tags[i] == tags[i - 1]) // Relies on fields having been sorted.
				throw new KryoException("Duplicate tag " + tags[i] + " on fields: " + field + " and " + fields[i - 1].getField());
			if (field.getAnnotation(Deprecated.class) != null) {
				deprecated[i] = true;
				writeFieldCount--;
			}
			if (field.getAnnotation(Tag.class).annexed()) annexed[i] = true;
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
		output.writeVarInt(writeFieldCount, true); // Can be used for null.

		CachedField[] fields = getFields();
		OutputChunked outputChunked = null; // Only instantiate if needed.
		int[] tags = this.tags;
		boolean[] annexed = this.annexed, deprecated = this.deprecated;
		for (int i = 0, n = fields.length; i < n; i++) {
			if (deprecated[i]) continue;
			output.writeVarInt(tags[i], true);
			if (annexed[i]) {
				if (TRACE) log("Write annexed", fields[i], output.position());
				if (outputChunked == null) outputChunked = new OutputChunked(output, 1024);
				fields[i].write(outputChunked, object);
				outputChunked.endChunk();
			} else {
				if (TRACE) log("Write", fields[i], output.position());
				fields[i].write(output, object);
			}
		}
	}

	public T read (Kryo kryo, Input input, Class<? extends T> type) {
		T object = create(kryo, input, type);
		kryo.reference(object);
		int fieldCount = input.readVarInt(true);

		CachedField[] fields = getFields();
		InputChunked inputChunked = null; // Only instantiate if needed.
		int[] tags = this.tags;
		boolean[] annexed = this.annexed;
		for (int i = 0, n = fieldCount; i < n; i++) {
			int tag = input.readVarInt(true);
			CachedField cachedField = null;
			boolean isAnnexed = false;
			for (int ii = 0, nn = tags.length; ii < nn; ii++) {
				if (tags[ii] == tag) {
					cachedField = fields[ii];
					isAnnexed = annexed[ii];
					break;
				}
			}
			if (cachedField == null) {
				if (config.skipUnknownTags) {
					if (inputChunked == null) inputChunked = new InputChunked(input, 1024);
					inputChunked.nextChunk(); // Assume future annexed field and skip.
					if (TRACE) trace(String.format("Unknown field tag: %d (%s) encountered. Assuming a future annexed "
						+ "tag with chunked encoding and skipping.", tag, getType().getName()));
					continue;
				} else
					throw new KryoException("Unknown field tag: " + tag + " (" + getType().getName() + ")");
			}

			if (isAnnexed) {
				if (TRACE) log("Read annexed", fields[i], input.position());
				if (inputChunked == null) inputChunked = new InputChunked(input, 1024);
				cachedField.read(inputChunked, object);
				inputChunked.nextChunk();
			} else {
				if (TRACE) log("Read", fields[i], input.position());
				cachedField.read(input, object);
			}
		}
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

		/** If true, the field is serialized with chunked encoding and is forward compatible, meaning safe to read in versions of
		 * the class without the field if {@link TaggedFieldSerializerConfig#getSkipUnknownTags()} is true. */
		boolean annexed() default false;
	}
}
