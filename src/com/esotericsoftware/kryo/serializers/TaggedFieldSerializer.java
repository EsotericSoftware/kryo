
package com.esotericsoftware.kryo.serializers;

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

/** Serializes objects using direct field assignment for fields that have been {@link Tag tagged}. Fields without the {@link Tag}
 * annotation are not serialized. New tagged fields can be added without invalidating previously serialized bytes. If any tagged
 * field is removed, previously serialized bytes are invalidated. Instead of removing fields, apply the {@link Deprecated}
 * annotation and they will still be deserialized but won't be serialized. If fields are public, bytecode generation will be used
 * instead of reflection.
 * @author Nathan Sweet <misc@n4te.com> */
public class TaggedFieldSerializer<T> extends FieldSerializer<T> {
	private int[] tags;
	private int writeFieldCount;
	private boolean[] deprecated;

	public TaggedFieldSerializer (Kryo kryo, Class type) {
		super(kryo, type);
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
			if (cachedField == null) throw new KryoException("Unknown field tag: " + tag + " (" + getType().getName() + ")");
			cachedField.read(input, object);
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
