
package com.esotericsoftware.kryo.serializers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import static com.esotericsoftware.minlog.Log.*;

/** Serializes objects using direct field assignment for fields that have been {@link Tag tagged}. Fields without the {@link Tag}
 * annotation are not serialized. New tagged fields can be added without invalidating previously serialized bytes. If any tagged
 * field is removed, previously serialized bytes are invalidated. Instead of removing fields, apply the {@link Deprecated}
 * annotation and they will not be serialized. If fields are public, bytecode generation will be used instead of reflection.
 * @author Nathan Sweet <misc@n4te.com> */
public class TaggedFieldSerializer<T> extends FieldSerializer<T> {
	private int[] tags;

	public TaggedFieldSerializer (Kryo kryo, Class type) {
		super(kryo, type);
	}

	protected void initializeCachedFields () {
		CachedField[] fields = getFields();
		// Remove unwanted fields.
		for (int i = 0, n = fields.length; i < n; i++) {
			Field field = fields[i].getField();
			Tag tag = field.getAnnotation(Tag.class);
			Deprecated deprecated = field.getAnnotation(Deprecated.class);
			if (tag == null || deprecated != null) {
				if (TRACE) {
					if (tag == null)
						trace("kryo", "Ignoring field without tag: " + fields[i]);
					else
						trace("kryo", "Ignoring deprecated field: " + fields[i]);
				}
				super.removeField(field.getName());
			}
		}
		// Cache tags.
		fields = getFields();
		tags = new int[fields.length];
		for (int i = 0, n = fields.length; i < n; i++)
			tags[i] = fields[i].getField().getAnnotation(Tag.class).value();
	}

	public void removeField (String fieldName) {
		super.removeField(fieldName);
		initializeCachedFields();
	}

	public void write (Kryo kryo, Output output, T object) {
		CachedField[] fields = getFields();
		output.writeInt(fields.length, true);
		for (int i = 0, n = fields.length; i < n; i++) {
			output.writeInt(tags[i], true);
			fields[i].write(output, object);
		}
	}

	public T read (Kryo kryo, Input input, Class<T> type) {
		T object = kryo.newInstance(type);
		kryo.reference(object);
		int fieldCount = input.readInt(true);
		int[] tags = this.tags;
		CachedField[] fields = getFields();
		for (int i = 0, n = fieldCount; i < n; i++) {
			int tag = input.readInt(true);

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
