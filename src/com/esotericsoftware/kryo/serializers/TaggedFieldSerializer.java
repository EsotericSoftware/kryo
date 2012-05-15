
package com.esotericsoftware.kryo.serializers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import static com.esotericsoftware.minlog.Log.*;

/** Serializes objects using direct field assignment for fields that have been {@link Tag tagged}. Fields without the {@link Tag}
 * annotation are not serialized. New tagged fields can be added without invalidating previously serialized bytes. If any tagged
 * field is removed, previously serialized bytes are invalidated. Instead of removing fields, apply the {@link Deprecated}
 * annotation and they will not be serialized. If fields are public, bytecode generation will be used instead of reflection.
 * @author Nathan Sweet <misc@n4te.com> */
public class TaggedFieldSerializer<T> extends FieldSerializer<T> {
	public TaggedFieldSerializer (Kryo kryo, Class type) {
		super(kryo, type);
	}

	protected CachedField newCachedField (Field field) {
		if (field.getAnnotation(Deprecated.class) != null) return null;
		Tag tag = field.getAnnotation(Tag.class);
		if (tag == null) return null;
		return new TaggedCachedField(field, tag.value());
	}

	public void write (Kryo kryo, Output output, T object) {
		CachedField[] fields = getFields();
		output.writeInt(fields.length, true);
		for (int i = 0, n = fields.length; i < n; i++) {
			CachedField cachedField = fields[i];
			try {
				if (TRACE) trace("kryo", "Write field: " + cachedField + " (" + object.getClass().getName() + ")");

				output.writeInt(((TaggedCachedField)fields[i]).tag, true);

				Object value = cachedField.get(object);

				Serializer serializer = cachedField.serializer;
				if (cachedField.fieldClass == null) {
					if (value == null) {
						kryo.writeClass(output, null);
						continue;
					}
					Registration registration = kryo.writeClass(output, value.getClass());
					if (serializer == null) serializer = registration.getSerializer();
					kryo.writeObject(output, value, serializer);
				} else {
					if (serializer == null) cachedField.serializer = serializer = kryo.getSerializer(cachedField.fieldClass);
					if (cachedField.canBeNull) {
						kryo.writeObjectOrNull(output, value, serializer);
					} else {
						if (value == null) {
							throw new KryoException("Field value is null but canBeNull is false: " + cachedField + " ("
								+ object.getClass().getName() + ")");
						}
						kryo.writeObject(output, value, serializer);
					}
				}
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field: " + cachedField + " (" + object.getClass().getName() + ")", ex);
			} catch (KryoException ex) {
				ex.addTrace(cachedField + " (" + object.getClass().getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(cachedField + " (" + object.getClass().getName() + ")");
				throw ex;
			}
		}
	}

	public void read (Kryo kryo, Input input, T object) {
		CachedField[] fields = getFields();
		int fieldCount = input.readInt(true);
		for (int i = 0, n = fieldCount; i < n; i++) {
			int tag = input.readInt(true);

			CachedField cachedField = null;
			for (int ii = 0, nn = fields.length; ii < nn; ii++) {
				TaggedCachedField f = (TaggedCachedField)fields[ii];
				if (f.tag == tag) {
					cachedField = f;
					break;
				}
			}
			if (cachedField == null) throw new KryoException("Unknown field tag: " + tag + " (" + getType().getName() + ")");

			try {
				if (TRACE) trace("kryo", "Read field: " + cachedField + " (" + getType().getName() + ")");

				Object value = null;

				Class concreteType = cachedField.fieldClass;
				Serializer serializer = cachedField.serializer;
				if (concreteType == null) {
					Registration registration = kryo.readClass(input);
					if (registration != null) { // Else value is null.
						if (serializer == null) serializer = registration.getSerializer();
						value = kryo.readObject(input, registration.getType(), serializer);
					}
				} else {
					if (serializer == null) cachedField.serializer = serializer = kryo.getSerializer(concreteType);
					if (cachedField.canBeNull)
						value = kryo.readObjectOrNull(input, concreteType, serializer);
					else
						value = kryo.readObject(input, concreteType, serializer);
				}

				cachedField.set(object, value);
			} catch (IllegalAccessException ex) {
				throw new KryoException("Error accessing field: " + cachedField + " (" + getType().getName() + ")", ex);
			} catch (KryoException ex) {
				ex.addTrace(cachedField + " (" + getType().getName() + ")");
				throw ex;
			} catch (RuntimeException runtimeEx) {
				KryoException ex = new KryoException(runtimeEx);
				ex.addTrace(cachedField + " (" + getType().getName() + ")");
				throw ex;
			}
		}
	}

	private class TaggedCachedField extends CachedField {
		final int tag;

		TaggedCachedField (Field field, int tag) {
			super(field);
			this.tag = tag;
		}
	}

	/** If true, this field will not be serialized. */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Tag {
		int value();
	}
}
