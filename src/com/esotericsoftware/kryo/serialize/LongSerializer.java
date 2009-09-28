
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Serializer;

// BOZO - Optimize?

/**
 * Writes an 8 byte long.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class LongSerializer extends Serializer {
	public Long readObjectData (ByteBuffer buffer, Class type) {
		long l = buffer.getLong();
		if (TRACE) trace("kryo", "Read long: " + l);
		return l;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		buffer.putLong((Long)object);
		if (TRACE) trace("kryo", "Wrote long: " + object);
	}
}
