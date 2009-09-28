
package com.esotericsoftware.kryo.serialize;

import static com.esotericsoftware.minlog.Log.*;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.Serializer;

/**
 * Writes a 1 byte boolean.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class BooleanSerializer extends Serializer {
	public Boolean readObjectData (ByteBuffer buffer, Class type) {
		boolean b = buffer.get() == 1;
		if (TRACE) trace("kryo", "Read boolean: " + b);
		return b;
	}

	public void writeObjectData (ByteBuffer buffer, Object object) {
		buffer.put((Boolean)object ? (byte)1 : (byte)0);
		if (TRACE) trace("kryo", "Wrote boolean: " + object);
	}
}
