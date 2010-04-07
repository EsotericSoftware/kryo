
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializationException;
import com.esotericsoftware.kryo.util.IntHashMap;

/**
 * Serializes objects using direct field assignment, handling object references and cyclic graphs. Each object serialized requires
 * 1 byte more than FieldSerializer. Each appearance of an object in the graph after the first is stored as an integer ordinal.
 * <p>
 * This class supports serializing non-static inner classes, however doing this has special implications as described in <a
 * href="http://java.sun.com/javase/6/docs/platform/serialization/spec/serial-arch.html#7182">Java Object Serialization
 * Specification, chapter 1.10</a>.
 * <p>
 * Note that serializing references can be convenient, but can sometimes be redundant information. If this is the case and
 * serialized size is a priority, references should not be serialized. Code can sometimes be hand written to reconstruct the
 * references after deserialization.
 * @see FieldSerializer
 * @author Nathan Sweet <misc@n4te.com>
 */
public class ReferenceFieldSerializer extends FieldSerializer {
	static private final byte REGULAR_INSTANCE = 0;
	static private final int INNER_CLASS_INSTANCE = 16383;

	private Field enclosingInstanceField;

	public ReferenceFieldSerializer (Kryo kryo, Class type) {
		super(kryo, type);

		Field[] fields = type.getDeclaredFields();
		for (int i = 0, n = fields.length; i < n; i++) {
			if (fields[i].isSynthetic()) {
				enclosingInstanceField = fields[i];
				try {
					enclosingInstanceField.setAccessible(true);
				} catch (Exception ex) {
					throw new SerializationException(
						"Cannot access synthetic enclosing instance field, unable to serialize inner class: " + type.getName(), ex);
				}
				break;
			}
		}
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		Context context = Kryo.getContext();
		References references = (References)context.getTemp("references");
		if (references == null) {
			// Use non-temporary storage to avoid repeated allocation.
			references = (References)context.get("references");
			if (references == null)
				context.put("references", references = new References());
			else
				references.reset();
			context.putTemp("references", references);
		}
		Integer reference = references.objectToReference.get(object);
		if (reference != null) {
			IntSerializer.put(buffer, reference, true);
			if (TRACE) trace("kryo", "Wrote object reference " + reference + ": " + object);
			return;
		}

		if (enclosingInstanceField == null) {
			buffer.put(REGULAR_INSTANCE);
		} else {
			IntSerializer.put(buffer, INNER_CLASS_INSTANCE, true);
			Object enclosingInstance;
			try {
				enclosingInstance = enclosingInstanceField.get(object);
			} catch (IllegalAccessException ex) {
				throw new SerializationException("Error accessing enclosing instance field in class: " + type.getName(), ex);
			}
			if (TRACE) trace("kryo", "Writing enclosing instance.");
			kryo.writeObjectData(buffer, enclosingInstance);
			// If the enclosing instance had a reference to the inner instance, don't serialize the inner instance again.
			reference = references.objectToReference.get(object);
			if (reference != null) {
				IntSerializer.put(buffer, reference, true);
				return;
			}
			buffer.put(REGULAR_INSTANCE);
		}

		references.referenceCount++;
		if (references.referenceCount == INNER_CLASS_INSTANCE) references.referenceCount++;
		references.objectToReference.put(object, references.referenceCount);

		super.writeObjectData(buffer, object);
	}

	public <T> T readObjectData (ByteBuffer buffer, Class<T> type) {
		Context context = Kryo.getContext();
		References references = (References)context.getTemp("references");
		if (references == null) {
			// Use non-temporary storage to avoid repeated allocation.
			references = (References)context.get("references");
			if (references == null)
				context.put("references", references = new References());
			else
				references.reset();
			context.putTemp("references", references);
		}

		T object;

		int reference = IntSerializer.get(buffer, true);
		if (reference == REGULAR_INSTANCE)
			object = newInstance(kryo, type);
		else if (reference == INNER_CLASS_INSTANCE && enclosingInstanceField != null) {
			Class enclosingType = enclosingInstanceField.getType();
			if (TRACE) trace("kryo", "Reading enclosing instance.");
			Object enclosingInstance = kryo.readObjectData(buffer, enclosingType);
			try {
				object = type.getConstructor(enclosingType).newInstance(enclosingInstance);
			} catch (Exception ex) {
				throw new SerializationException("Error constructing inner class instance: " + type.getName(), ex);
			}
			// If the enclosing instance had a reference to the inner instance, just return the inner instance.
			reference = IntSerializer.get(buffer, true);
			if (reference != REGULAR_INSTANCE) {
				object = (T)references.referenceToObject.get(reference);
				if (object == null) throw new SerializationException("Invalid object reference: " + reference);
				return object;
			}
		} else {
			object = (T)references.referenceToObject.get(reference);
			if (object == null) throw new SerializationException("Invalid object reference: " + reference);
			if (TRACE) trace("kryo", "Read object reference " + reference + ": " + object);
			return object;
		}

		references.referenceCount++;
		if (references.referenceCount == INNER_CLASS_INSTANCE) references.referenceCount++;
		references.referenceToObject.put(references.referenceCount, object);

		return super.readObjectData(object, buffer, type);
	}

	static class References {
		public IdentityHashMap<Object, Integer> objectToReference = new IdentityHashMap();
		public IntHashMap referenceToObject = new IntHashMap();
		public int referenceCount = 1;

		public void reset () {
			objectToReference.clear();
			referenceToObject.clear();
			referenceCount = 1;
		}
	}
}
