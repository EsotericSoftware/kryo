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

import com.esotericsoftware.kryo.Kryo;
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
 * Removing fields when {@link Kryo#setReferences(boolean) references} are enabled can cause compatibility issues. See
 * <a href="https://github.com/EsotericSoftware/kryo/issues/286#issuecomment-74870545">here</a>.
 * <p>
 * Note that the field data is identified by name. The situation where a super class has a field with the same name as a subclass
 * must be avoided.
 * @author Nathan Sweet <misc@n4te.com> */
public class CompatibleFieldSerializer<T> extends FieldSerializer<T> {
	/* For object with more than BINARY_SEARCH_THRESHOLD fields, use binary search instead of iterative search */
	private static final int THRESHOLD_BINARY_SEARCH = 32;

	public CompatibleFieldSerializer (Kryo kryo, Class type) {
		super(kryo, type);
	}

	public void write (Kryo kryo, Output output, T object) {
		CachedField[] fields = getFields();
		ObjectMap context = kryo.getGraphContext();
		if (!context.containsKey(this)) {
			context.put(this, null);
			if (TRACE) trace("kryo", "Write " + fields.length + " field names.");
			output.writeVarInt(fields.length, true);
			for (int i = 0, n = fields.length; i < n; i++)
				output.writeString(getCachedFieldName(fields[i]));
		}

		OutputChunked outputChunked = new OutputChunked(output, 1024);
		for (int i = 0, n = fields.length; i < n; i++) {
			fields[i].write(outputChunked, object);
			outputChunked.endChunks();
		}
	}

	public T read (Kryo kryo, Input input, Class<T> type) {
		T object = create(kryo, input, type);
		kryo.reference(object);
		ObjectMap context = kryo.getGraphContext();
		CachedField[] fields = (CachedField[])context.get(this);
		if (fields == null) {
			int length = input.readVarInt(true);
			if (TRACE) trace("kryo", "Read " + length + " field names.");
			String[] names = new String[length];
			for (int i = 0; i < length; i++)
				names[i] = input.readString();

			fields = new CachedField[length];
			CachedField[] allFields = getFields();

			if (length < THRESHOLD_BINARY_SEARCH) {
				outer:
				for (int i = 0; i < length; i++) {
					String schemaName = names[i];
					for (int ii = 0, nn = allFields.length; ii < nn; ii++) {
						if (getCachedFieldName(allFields[ii]).equals(schemaName)) {
							fields[i] = allFields[ii];
							continue outer;
						}
					}
					if (TRACE) trace("kryo", "Ignore obsolete field: " + schemaName);
				}
			} else {
				// binary search for schemaName
				int low, mid, high;
				int compare;
				outerBinarySearch:
				for (int i = 0; i < length; i++) {
					String schemaName = names[i];

					low = 0;
					high = length - 1;

					while (low <= high) {
						mid = (low + high) >>> 1;
						String midVal = getCachedFieldName(allFields[mid]);
						compare = schemaName.compareTo(midVal);

						if (compare < 0) {
							high = mid - 1;
						} else if (compare > 0) {
							low = mid + 1;
						} else {
							fields[i] = allFields[mid];
							continue outerBinarySearch;
						}
					}
					if (TRACE) trace("kryo", "Ignore obsolete field: " + schemaName);
				}
			}

			context.put(this, fields);
		}

		InputChunked inputChunked = new InputChunked(input, 1024);
		boolean hasGenerics = getGenerics() != null;
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];
			if (cachedField != null && hasGenerics) {
				// Generic type used to instantiate this field could have
				// been changed in the meantime. Therefore take the most
				// up-to-date definition of a field
				cachedField = getField(getCachedFieldName(cachedField));
			}
			if (cachedField == null) {
				if (TRACE) trace("kryo", "Skip obsolete field.");
				inputChunked.nextChunks();
				continue;
			}
			cachedField.read(inputChunked, object);
			inputChunked.nextChunks();
		}
		return object;
	}
}
