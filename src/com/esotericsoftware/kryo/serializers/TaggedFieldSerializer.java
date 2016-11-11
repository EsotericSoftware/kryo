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
import com.esotericsoftware.kryo.io.Output;

/** Serializes objects using direct field assignment for fields that have a <code>@Tag(int)</code> annotation. This provides
 * backward compatibility so new fields can be added. TaggedFieldSerializer has two advantages over {@link VersionFieldSerializer}
 * : 1) fields can be renamed and 2) fields marked with the <code>@Deprecated</code> annotation will be ignored when reading old
 * bytes and won't be written to new bytes. Deprecation effectively removes the field from serialization, though the field and
 * <code>@Tag</code> annotation must remain in the class. Deprecated fields can optionally be made private and/or renamed so they
 * don't clutter the class (eg, <code>ignored</code>, <code>ignored2</code>). For these reasons, TaggedFieldSerializer generally
 * provides more flexibility for classes to evolve. The downside is that it has a small amount of additional overhead compared to
 * VersionFieldSerializer (an additional varint per field). Forward compatibility is not supported.
 * @see VersionFieldSerializer
 * @author Nathan Sweet <misc@n4te.com> */
public class TaggedFieldSerializer<T> extends FieldSerializer<T> {
	private int[] tags;
	private int writeFieldCount;
	private boolean[] deprecated;

	public TaggedFieldSerializer (Kryo kryo, Class type) {
		super(kryo, type, null, kryo.getTaggedFieldSerializerConfig().clone());
	}

	/** Tells Kryo, if should ignore unknown field tags when using TaggedFieldSerializer. Already existing serializer instances are
	 * not affected by this setting.
	 *
	 * <p>
	 * By default, Kryo will throw KryoException if encounters unknown field tags.
	 * </p>
	 *
	 * @param ignoreUnknownTags if true, unknown field tags will be ignored. Otherwise KryoException will be thrown */
	public void setIgnoreUnknownTags (boolean ignoreUnknownTags) {
		((TaggedFieldSerializerConfig)config).setIgnoreUnknownTags(ignoreUnknownTags);
		rebuildCachedFields();
	}

	public boolean isIgnoreUnkownTags () {
		return ((TaggedFieldSerializerConfig)config).isIgnoreUnknownTags();
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
		writeFieldCount = fields.length;

		// fields are sorted to ensure write order: tag 0, tag 1, ... , tag N
		Arrays.sort(fields, new Comparator<CachedField>() {
			public int compare (CachedField o1, CachedField o2) {
				return o1.getField().getAnnotation(Tag.class).value() - o2.getField().getAnnotation(Tag.class).value();
			}
		});
		for (int i = 0, n = fields.length; i < n; i++) {
			Field field = fields[i].getField();
			tags[i] = field.getAnnotation(Tag.class).value();
			if (field.getAnnotation(Deprecated.class) != null) {
				deprecated[i] = true;
				writeFieldCount--;
			}
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
		for (int i = 0, n = fields.length; i < n; i++) {
			if (deprecated[i]) continue;
			output.writeVarInt(tags[i], true);
			fields[i].write(output, object);
		}
	}

	public T read (Kryo kryo, Input input, Class<T> type) {
		T object = create(kryo, input, type);
		kryo.reference(object);
		int fieldCount = input.readVarInt(true);
		int[] tags = this.tags;
		CachedField[] fields = getFields();
		for (int i = 0, n = fieldCount; i < n; i++) {
			int tag = input.readVarInt(true);

			CachedField cachedField = null;
			for (int ii = 0, nn = tags.length; ii < nn; ii++) {
				if (tags[ii] == tag) {
					cachedField = fields[ii];
					break;
				}
			}
			if (cachedField == null) {
				if (!isIgnoreUnkownTags()) throw new KryoException("Unknown field tag: " + tag + " (" + getType().getName() + ")");
			} else {
				cachedField.read(input, object);
			}
		}
		return object;
	}

	/** If true, this field will not be serialized. */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Tag {
		int value();
	}
}
