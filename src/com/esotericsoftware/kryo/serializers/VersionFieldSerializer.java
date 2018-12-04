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
import static com.esotericsoftware.minlog.Log.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

/** Serializes objects using direct field assignment, providing backward compatibility with minimal overhead. This means fields
 * can be added without invalidating previously serialized bytes. Removing, renaming, or changing the type of a field is not
 * supported.
 * <p>
 * When a field is added, it must have the {@link Since} annotation to indicate the version it was added in order to be compatible
 * with previously serialized bytes. The annotation value must never change.
 * <p>
 * Compared to {@link FieldSerializer}, VersionFieldSerializer writes a single additional varint and requires annotations for
 * added fields, but provides backward compatibility so fields can be added. {@link TaggedFieldSerializer} provides more
 * flexibility for classes to evolve in exchange for a slightly larger serialized size.
 * @author Nathan Sweet */
public class VersionFieldSerializer<T> extends FieldSerializer<T> {
	private final VersionFieldSerializerConfig config;
	private int typeVersion; // Version of the type being serialized.
	private int[] fieldVersion; // Version of each field.

	public VersionFieldSerializer (Kryo kryo, Class type) {
		this(kryo, type, new VersionFieldSerializerConfig());
	}

	public VersionFieldSerializer (Kryo kryo, Class type, VersionFieldSerializerConfig config) {
		super(kryo, type, config);
		this.config = config;
		setAcceptsNull(true);
		// Make sure this is done before any read/write operations.
		initializeCachedFields();
	}

	protected void initializeCachedFields () {
		CachedField[] fields = cachedFields.fields;
		fieldVersion = new int[fields.length];
		for (int i = 0, n = fields.length; i < n; i++) {
			Field field = fields[i].field;
			Since since = field.getAnnotation(Since.class);
			if (since != null) {
				fieldVersion[i] = since.value();
				// Use the maximum version among fields as the entire type's version.
				typeVersion = Math.max(fieldVersion[i], typeVersion);
			} else {
				fieldVersion[i] = 0;
			}
		}
		if (DEBUG) debug("Version for type " + getType().getName() + " is " + typeVersion);
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

		CachedField[] fields = cachedFields.fields;
		// Write type version.
		output.writeVarInt(typeVersion + 1, true);
		// Write fields.
		for (int i = 0, n = fields.length; i < n; i++) {
			if (TRACE) log("Write", fields[i], output.position());
			fields[i].write(output, object);
		}

		if (pop > 0) popTypeVariables(pop);
	}

	public T read (Kryo kryo, Input input, Class<? extends T> type) {
		int version = input.readVarInt(true);
		if (version == NULL) return null;
		version--;
		if (!config.compatible && version != typeVersion)
			throw new KryoException("Version is not compatible: " + version + " != " + typeVersion);

		int pop = pushTypeVariables();

		T object = create(kryo, input, type);
		kryo.reference(object);

		CachedField[] fields = cachedFields.fields;
		for (int i = 0, n = fields.length; i < n; i++) {
			// Field is not present in input, skip it.
			if (fieldVersion[i] > version) {
				if (DEBUG) debug("Skip field: " + fields[i].field.getName());
				continue;
			}
			if (TRACE) log("Read", fields[i], input.position());
			fields[i].read(input, object);
		}

		if (pop > 0) popTypeVariables(pop);
		return object;
	}

	public VersionFieldSerializerConfig getVersionFieldSerializerConfig () {
		return config;
	}

	/** Incremental modification of serialized objects must add {@link Since} for new fields. */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Since {
		/** Version of annotated field, default is 0, and must be incremental to maintain compatibility. */
		int value() default 0;
	}

	/** Configuration for VersionFieldSerializer instances. */
	static public class VersionFieldSerializerConfig extends FieldSerializerConfig {
		boolean compatible = true;

		public VersionFieldSerializerConfig clone () {
			return (VersionFieldSerializerConfig)super.clone(); // Clone is ok as we have only primitive fields.
		}

		/** When false, an exception is thrown when reading an object with a different version. The version of an object is the
		 * maximum version of any field. Default is true. */
		public void setCompatible (boolean compatible) {
			this.compatible = compatible;
			if (TRACE) trace("kryo", "VersionFieldSerializerConfig setCompatible: " + compatible);
		}

		public boolean getCompatible () {
			return compatible;
		}
	}
}
