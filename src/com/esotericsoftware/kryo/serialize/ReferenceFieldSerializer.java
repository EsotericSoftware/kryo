
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

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
 * Note that serializing references can be convenient, but can sometimes be redundant information. If this is the case and
 * serialized size is a priority, references should not be serialized. Code can sometimes be hand written to reconstruct the
 * references after deserialization.
 * @see FieldSerializer
 * @author Nathan Sweet <misc@n4te.com>
 */
public class ReferenceFieldSerializer extends FieldSerializer {
	public ReferenceFieldSerializer (Kryo kryo, Class type) {
		super(kryo, type);
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

		buffer.put((byte)0);
		references.referenceCount++;
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

		int reference = IntSerializer.get(buffer, true);
		if (reference != 0) {
			T object = (T)references.referenceToObject.get(reference);
			if (object == null) throw new SerializationException("Invalid object reference: " + reference);
			if (TRACE) trace("kryo", "Read object reference " + reference + ": " + object);
			return object;
		}

		T object = newInstance(kryo, type);

		references.referenceCount++;
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
