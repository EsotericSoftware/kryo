/* Copyright (c) 2008, Nathan Sweet
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
 * : 1) fields can be renamed and 2) fields marked with the <code>@Deprecated</code> annotation will be ignored when reading old
 * bytes and won't be written to new bytes. Deprecation effectively removes the field from serialization, though the field and
 * <code>@Tag</code> annotation must remain in the class. Deprecated fields can optionally be made private and/or renamed so they
 * don't clutter the class (eg, <code>ignored</code>, <code>ignored2</code>). For these reasons, TaggedFieldSerializer generally
 * provides more flexibility for classes to evolve. The downside is that it has a small amount of additional overhead compared to
 * VersionFieldSerializer (an additional varint per field). 
 * <p>
 * Forward compatibility is optionally supported by enabling {@link #setSkipUnknownTags(boolean)}, which allows it to
 * skip reading unknown tagged fields, which are presumably new fields added in future versions of an application. The
 * data is only forward compatible if the newly added fields are tagged with {@link TaggedFieldSerializer.Tag#annexed()}
 * set true, which comes with the cost of chunked encoding. When annexed fields are encountered during the read or write
 * process of an object, a buffer is allocated to perform the chunked encoding.
 * <p>
 * Tag values must be entirely unique, even among a class and its superclass(es). An IllegalArgumentException will be
 * thrown by {@link Kryo#register(Class)} (and its overloads) if duplicate Tag values are encountered.
 * @see VersionFieldSerializer
 * @author Nathan Sweet <misc@n4te.com> */
public class TaggedFieldSerializer<T> extends FieldSerializer<T> {
	private int[] tags;
	private int writeFieldCount;
	private boolean[] deprecated;
	private boolean[] annexed;

	public TaggedFieldSerializer (Kryo kryo, Class type) {
		super(kryo, type, null, kryo.getTaggedFieldSerializerConfig().clone());
	}

	/** Set whether TaggedFieldSerializer should attempt to skip reading the data of unknown tags, rather than throwing a
	 * KryoException. Data can be skipped if it is tagged with {@link Tag#annexed()} set true. This enables forward
	 * compatibility.
	 * <p>
	 * By default, this setting matches the value of {@link TaggedFieldSerializerConfig#isSkipUnknownTags()} in
	 * {@link Kryo#getTaggedFieldSerializerConfig()}, which is false by default.
	 * </p>
	 *
	 * @param skipUnknownTags If true, unknown field tags will be skipped, with the assumption that they are future
	 *                          tagged values with {@link Tag#annexed()} set true. If false KryoException will be thrown
	 *                          whenever unknown tags are encountered. */
	public void setSkipUnknownTags (boolean skipUnknownTags) {
		((TaggedFieldSerializerConfig)config).setSkipUnknownTags(skipUnknownTags);
		rebuildCachedFields();
	}

	/**
	 * @return Whether the TaggedFieldSerializers should attempt to skip reading the data of unknown tags, rather than
	 * throwing a KryoException. See {@link #setSkipUnknownTags(boolean)}.
	 */
	public boolean isSkipUnknownTags() {
		return ((TaggedFieldSerializerConfig)config).isSkipUnknownTags();
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
		// Cache tag values.
		fields = getFields();
		tags = new int[fields.length];
		deprecated = new boolean[fields.length];
		annexed = new boolean[fields.length];
		writeFieldCount = fields.length;

		Arrays.sort(fields, TAGGED_VALUE_COMPARATOR); // fields are sorted to easily check for reused tag values
		for (int i = 0, n = fields.length; i < n; i++) {
			Field field = fields[i].getField();
			tags[i] = field.getAnnotation(Tag.class).value();
			if (i > 0 && tags[i] == tags[i-1]) // This check relies on fields having been sorted
				throw new KryoException(String.format("The fields [%s] and [%s] both have a Tag value of %d.", field, fields[i-1].getField(), tags[i]));
			if (field.getAnnotation(Deprecated.class) != null) {
				deprecated[i] = true;
				writeFieldCount--;
			}
			if (field.getAnnotation(Tag.class).annexed())
				annexed[i] = true;
		}

		this.removedFields.clear();
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
		CachedField[] fields = getFields();
		output.writeVarInt(writeFieldCount, true); // Can be used for null.

		OutputChunked outputChunked = null; // only instantiate if needed
		for (int i = 0, n = fields.length; i < n; i++) {
			if (deprecated[i]) continue;
			output.writeVarInt(tags[i], true);
			if (annexed[i]){
				if (outputChunked == null)
					outputChunked = new OutputChunked(output, 1024);
				fields[i].write(outputChunked, object);
				outputChunked.endChunks();
			} else {
				fields[i].write(output, object);
			}
		}
	}

	public T read (Kryo kryo, Input input, Class<T> type) {
		T object = create(kryo, input, type);
		kryo.reference(object);
		int fieldCount = input.readVarInt(true);
		int[] tags = this.tags;
		InputChunked inputChunked = null; // only instantiate if needed
		CachedField[] fields = getFields();
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
				if (isSkipUnknownTags()) {
					if (inputChunked == null) inputChunked = new InputChunked(input, 1024);
					inputChunked.nextChunks(); // assume future annexed field and skip
					if (TRACE) trace(String.format("Unknown field tag: %d (%s) encountered. Assuming a future annexed " +
									"tag with chunked encoding and skipping.", tag, getType().getName()));
				} else
					throw new KryoException("Unknown field tag: " + tag + " (" + getType().getName() + ")");
			} else if (isAnnexed){
				if (inputChunked == null) inputChunked = new InputChunked(input, 1024);
				cachedField.read(inputChunked, object);
				inputChunked.nextChunks();
			} else {
				cachedField.read(input, object);
			}
		}
		return object;
	}

	private static final Comparator<CachedField> TAGGED_VALUE_COMPARATOR = new Comparator<CachedField>() {
		public int compare (CachedField o1, CachedField o2) {
			return o1.getField().getAnnotation(Tag.class).value() - o2.getField().getAnnotation(Tag.class).value();
		}
	};

	/** Marks a field for serialization. */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Tag {
		int value();
		/** If true, the field is serialized with chunked encoding and is forward compatible, meaning safe to read in
		 * iterations of the class without it if {@link #isSkipUnknownTags()}. */
		boolean annexed() default false;
	}

	/**
	 * @deprecated The {@code ignoreUnknownTags} feature is deprecated and the functionality is disabled, as it is an
	 * invalid means of preserving forward compatibility. See {@link #setSkipUnknownTags(boolean)} for an alternate means.
	 * @param ignoreUnknownTags This setting is now ignored.
	 */
	@Deprecated
	public void setIgnoreUnknownTags (boolean ignoreUnknownTags){
	}

	/**
	 * @deprecated See {@link #setIgnoreUnknownTags(boolean)} for information.
	 * @return Always returns false, as this feature has been disabled.
	 */
	@Deprecated
	public boolean isIgnoreUnkownTags() {
		return false;
	}
}
