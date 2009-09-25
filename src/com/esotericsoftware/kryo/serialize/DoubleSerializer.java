
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.log.Log.*;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Serializer;

/**
 * Writes an 8 byte double.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class DoubleSerializer extends Serializer {
	public Double readObjectData (ByteBuffer buffer, Class type) {
		double d = buffer.getDouble();
		if (level <= TRACE) trace("kryo", "Read double: " + d);
		return d;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		buffer.putDouble((Double)object);
		if (level <= TRACE) trace("kryo", "Wrote double: " + object);
	}
}
