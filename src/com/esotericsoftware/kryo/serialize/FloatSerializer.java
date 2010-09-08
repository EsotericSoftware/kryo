
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Serializer;

/**
 * Writes a 4 byte float.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class FloatSerializer extends Serializer {
	public Float readObjectData (ByteBuffer buffer, Class type) {
		float f = buffer.getFloat();
		if (TRACE) trace("kryo", "Read float: " + f);
		return f;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		buffer.putFloat((Float)object);
		if (TRACE) trace("kryo", "Wrote float: " + object);
	}
}
